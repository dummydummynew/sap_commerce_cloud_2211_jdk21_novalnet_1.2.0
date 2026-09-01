/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.payment.impl;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.novalnet.dao.NovalnetCallbackDao;
import com.novalnet.model.NovalnetCallbackInfoModel;
import com.novalnet.service.order.NovalnetCallbackOrderService;
import com.novalnet.service.payment.NovalnetCallbackService;
import com.novalnet.util.NovalnetUtils;

import de.novalnet.beans.NnCallbackCollectionData;
import de.novalnet.beans.NnCallbackCustomData;
import de.novalnet.beans.NnCallbackEventData;
import de.novalnet.beans.NnCallbackRefundData;
import de.novalnet.beans.NnCallbackRequestData;
import de.novalnet.beans.NnCallbackTransactionData;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;



public class DefaultNovalnetCallbackService implements NovalnetCallbackService
{
	private static final Logger LOG = Logger.getLogger(DefaultNovalnetCallbackService.class);

	private static final Set<String> UPDATE_WITH_DATE_AMOUNT = Set.of("DUE_DATE", "AMOUNT_DUE_DATE");

	private static final Set<String> BANK_DETAILS_PAYMENT_TYPES = Set.of("INVOICE", "GUARANTEED_INVOICE");

	private static final Set<String> CREDIT_PAYMENT_TYPES = Set.of("INVOICE_CREDIT", "CASHPAYMENT_CREDIT", "MULTIBANCO_CREDIT");

	private String currentDate;

	@Resource(name = "objectMapper")
	private ObjectMapper objectMapper;

	@Resource(name = "novalnetCallbackDao")
	private NovalnetCallbackDao novalnetCallbackDao;

	@Resource(name = "novalnetCallbackOrderService")
	private NovalnetCallbackOrderService orderService;

	@Resource(name = "baseStoreService")
	private BaseStoreService baseStoreService;

	@Override
	public String processCallback(NnCallbackRequestData request, HttpServletRequest httpRequest)
	{
		BaseStoreModel baseStore = baseStoreService.getCurrentBaseStore();

		NovalnetUtils.validateCallbackIp(httpRequest, baseStore);
		NovalnetUtils.validateMandatoryParams(request);
		NovalnetUtils.validateCallbackChecksum(request, baseStore);

		currentDate = getCurrentDate();

		String eventType = request.getEvent().getType();

		logEvent("CALLBACK_RECEIVED", request);

		switch (eventType)
		{
			case "TRANSACTION_CAPTURE":
				return handleTransactionCapture(request);

			case "TRANSACTION_CANCEL":
				return handleTransactionCancel(request);

			case "TRANSACTION_UPDATE":
				return handleTransactionUpdate(request);

			case "PAYMENT":
				return handlePayment(request);

			case "CREDIT":
				return handleCredit(request);

			case "TRANSACTION_REFUND":
				return handleRefund(request);

			case "CHARGEBACK":
				return handleRefund(request);

			case "PAYMENT_REMINDER_1", "PAYMENT_REMINDER_2":
				return handleReminder(request);

			case "SUBMISSION_TO_COLLECTION_AGENCY":
				return handleCollection(request);

			default:
				return "Unsupported callback event type: " + eventType;
		}
	}

	@Override
	public String handleTransactionCapture(NnCallbackRequestData request)
	{
		NnCallbackTransactionData txn = request.getTransaction();
		NnCallbackCustomData custom = request.getCustom();

		String orderCode = txn.getOrder_no();

		OrderModel order = novalnetCallbackDao.findOrderByCode(orderCode);

		orderService.setSessionLanguage(order);
		orderService.updatePaymentInfo(orderCode, txn.getStatus());
		orderService.updateOrderStatus(orderCode, novalnetCallbackDao.getLatestNovalnetPaymentInfo(orderCode), order);

		String paymentType = txn.getPayment_type();

		boolean isInvoice = "GUARANTEED_INVOICE".equals(paymentType) || "INVOICE".equals(paymentType);

		String msg = isInvoice
				? String.format(orderService.getLabel("novalnet.callbackCaptureUpdateText"), txn.getTid(), txn.getDue_date())
				: String.format(orderService.getLabel("novalnet.callbackConfirmUpdateText"), currentDate);

		String shortComment = msg;

		msg = buildInitialComments(msg, txn, custom, order);

		orderService.updateCallbackComments(msg, orderCode, txn.getStatus(), shortComment);

		logEvent("TRANSACTION_CAPTURE", request);

		return msg;
	}

	private String buildInitialComments(String msg, final NnCallbackTransactionData transaction, final NnCallbackCustomData custom,
			final OrderModel order)
	{

		if (BANK_DETAILS_PAYMENT_TYPES.contains(transaction.getPayment_type()))
		{
			ObjectNode transactionObject = convertToJson(transaction);
			JSONObject transactionJson = new JSONObject(transactionObject.toString());
			String initialComments = orderService.buildOrderHistoryNotes(transactionJson,
					formatAmount(transaction.getAmount(), transaction.getCurrency(), order), custom.getInputval1());
			msg = msg + "<br>" + initialComments;
		}

		return msg;
	}

	public ObjectNode convertToJson(NnCallbackTransactionData data)
	{
		return objectMapper.valueToTree(data);
	}

	@Override
	public String handleTransactionCancel(NnCallbackRequestData request)
	{
		String orderCode = request.getTransaction().getOrder_no();

		orderService.updateCancelStatus(orderCode);

		OrderModel order = novalnetCallbackDao.findOrderByCode(orderCode);

		orderService.setSessionLanguage(order);

		String msg = String.format(orderService.getLabel("novalnet.callbackCancelUpdateText"), currentDate);

		orderService.updateCallbackComments(msg, orderCode, request.getTransaction().getStatus(), msg);

		logEvent("TRANSACTION_CANCEL", request);

		return msg;
	}

	@Override
	public String handleTransactionUpdate(NnCallbackRequestData request)
	{
		NnCallbackTransactionData transaction = request.getTransaction();
		NnCallbackCustomData custom = request.getCustom();

		String tid = transaction.getTid();
		String status = transaction.getStatus();
		String updateType = transaction.getUpdate_type();
		String orderCode = transaction.getOrder_no();

		OrderModel order = novalnetCallbackDao.findOrderByCode(orderCode);

		orderService.setSessionLanguage(order);
		orderService.updatePaymentInfo(orderCode, status);

		String msg = "";
		String shortComment = "";

		if (UPDATE_WITH_DATE_AMOUNT.contains(updateType))
		{
			msg = String.format(orderService.getLabel("novalnet.callbackAmountDueDateUpdateText"),
					formatAmount(transaction.getAmount(), transaction.getCurrency(), order), transaction.getDue_date());
		}
		else if ("AMOUNT".equals(updateType))
		{
			msg = String.format(orderService.getLabel("novalnet.callbackAmountUpdateText"), tid,
					formatAmount(transaction.getAmount(), transaction.getCurrency(), order), currentDate);
		}
		else if ("STATUS".equals(updateType))
		{
			switch (status)
			{
				case "DEACTIVATED":
					msg = String.format(orderService.getLabel("novalnet.callbackCancelUpdateText"), currentDate);
					break;

				case "ON_HOLD":
					msg = String.format(orderService.getLabel("novalnet.callbackOnhodUpdateText"), tid, currentDate);

					shortComment = msg;
					msg = buildInitialComments(msg, transaction, custom, order);
					break;

				case "CONFIRMED":
					msg = String.format(orderService.getLabel("novalnet.callbackConfirmUpdateText"), currentDate);

					shortComment = msg;
					msg = buildInitialComments(msg, transaction, custom, order);
					break;

				default:
					msg = String.format("Transaction status updated for TID %s to %s", tid, status);
					break;
			}

			orderService.updateOrderStatus(orderCode, novalnetCallbackDao.getLatestNovalnetPaymentInfo(orderCode), order);
		}

		if ("".equals(shortComment))
		{
			shortComment = msg;
		}

		orderService.updateCallbackComments(msg, orderCode, status, shortComment);

		logEvent("TRANSACTION_UPDATE", request);

		return msg;
	}

	@Override
	public String handlePayment(NnCallbackRequestData request)
	{
		NnCallbackTransactionData txn = request.getTransaction();

		String status = txn.getStatus();
		String orderCode = txn.getOrder_no();

		OrderModel order = novalnetCallbackDao.findOrderByCode(orderCode);

		orderService.updatePaymentInfo(orderCode, status);

		orderService.updateOrderStatus(orderCode, novalnetCallbackDao.getLatestNovalnetPaymentInfo(orderCode), order);

		logEvent("PAYMENT", request);

		return "Novalnet webhook script executed. Status updated for initial transaction";
	}

	public String formatAmount(String amount, String currency, OrderModel order)
	{
		double orderAmount = new BigDecimal(amount).movePointLeft(2).doubleValue();

		Locale locale = orderService.getLocale(order);

		NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(locale != null ? locale : Locale.getDefault());

		currencyFormat.setCurrency(Currency.getInstance(currency));

		return currencyFormat.format(orderAmount);
	}

	public String handleCredit(NnCallbackRequestData request)
	{
		NnCallbackTransactionData txn = request.getTransaction();

		String parentTid = request.getEvent().getParent_tid();

		NovalnetCallbackInfoModel ref = novalnetCallbackDao.findCallbackInfoByOriginalTid(parentTid);

		String orderCode = txn.getOrder_no();

		OrderModel order = novalnetCallbackDao.findOrderByCode(orderCode);

		orderService.setSessionLanguage(order);

		if (CREDIT_PAYMENT_TYPES.contains(txn.getPayment_type()))
		{
			int currentPaid = Math.toIntExact(ref.getPaidAmount());

			int receivedAmount = Integer.parseInt(txn.getAmount());

			int totalPaid = currentPaid + receivedAmount;

			orderService.updateCallbackInfo(Long.parseLong(txn.getTid()), parentTid, totalPaid);

			if (totalPaid >= ref.getOrderAmount())
			{
				orderService.updatePaymentInfo(orderCode, txn.getStatus());

				orderService.updateOrderStatus(orderCode, novalnetCallbackDao.getLatestNovalnetPaymentInfo(orderCode), order);
			}
		}

		String msg = buildCreditMessage(txn, parentTid, order);

		orderService.updateCallbackComments(msg, orderCode, txn.getStatus(), msg);

		logEvent("CREDIT", request);

		return msg;
	}

	private String buildCreditMessage(NnCallbackTransactionData txn, String parentTid, OrderModel order)
	{
		return String.format(orderService.getLabel("novalnet.callbackCreditText"), parentTid,
				formatAmount(txn.getAmount(), txn.getCurrency(), order), getCurrentDate(), txn.getTid());
	}

	@Override
	public String handleRefund(NnCallbackRequestData request)
	{
		NnCallbackTransactionData transaction = request.getTransaction();

		NnCallbackEventData event = request.getEvent();

		String orderCode = transaction.getOrder_no();

		OrderModel order = novalnetCallbackDao.findOrderByCode(orderCode);

		orderService.setSessionLanguage(order);

		String transactionStatus = transaction.getStatus();

		String msg = buildRefundMessage(transaction, event, order);

		if ("DEACTIVATED".equals(transactionStatus))
		{
			orderService.updateCancelStatus(orderCode);
		}

		orderService.updateCallbackComments(msg, orderCode, transactionStatus, msg);

		logEvent("REFUND", request);

		return msg;
	}

	private String buildRefundMessage(NnCallbackTransactionData transaction, NnCallbackEventData event, OrderModel order)
	{
		String eventType = (event != null) ? event.getType() : null;

		String currency = transaction.getCurrency();

		String originalTid = getOriginalTid(event, transaction);

		if (!"TRANSACTION_REFUND".equals(eventType))
		{
			return String.format(orderService.getLabel("novalnet.callbackChargebackText"), originalTid,
					formatAmount(transaction.getAmount(), currency, order), getCurrentDate(), transaction.getTid());
		}

		NnCallbackRefundData refund = transaction.getRefund();

		if (refund == null || refund.getTid() == null)
		{
			String refundAmount = (refund != null) ? refund.getAmount() : "0";

			return String.format(orderService.getLabel("novalnet.callbackRefundTextOrgTid"), transaction.getTid(),
					formatAmount(refundAmount, currency, order));
		}

		String newTid = (event.getTid() != null) ? event.getTid() : refund.getTid();

		return String.format(orderService.getLabel("novalnet.callbackRefundText"), originalTid,
				formatAmount(refund.getAmount(), currency, order), newTid);
	}

	private String getOriginalTid(NnCallbackEventData event, NnCallbackTransactionData transaction)
	{
		return (event != null && event.getParent_tid() != null) ? event.getParent_tid() : transaction.getTid();
	}

	@Override
	public String handleReminder(NnCallbackRequestData request)
	{
		NnCallbackTransactionData transaction = request.getTransaction();

		NnCallbackEventData event = request.getEvent();

		String orderCode = transaction.getOrder_no();

		OrderModel order = novalnetCallbackDao.findOrderByCode(orderCode);

		orderService.setSessionLanguage(order);

		String eventType = event != null ? event.getType() : null;

		String count = "PAYMENT_REMINDER_1".equals(eventType) ? "1" : "2";

		String msg = String.format(orderService.getLabel("novalnet.callbackReminderText"), count);

		orderService.updateCallbackComments(msg, orderCode, transaction.getStatus(), msg);

		logEvent("REMINDER", request);

		return msg;
	}

	@Override
	public String handleCollection(NnCallbackRequestData request)
	{
		NnCallbackTransactionData transaction = request.getTransaction();

		NnCallbackCollectionData collection = request.getCollection();

		String orderCode = transaction.getOrder_no();

		OrderModel order = novalnetCallbackDao.findOrderByCode(orderCode);

		orderService.setSessionLanguage(order);

		String label = orderService.getLabel("novalnet.callbackCollectionText");

		StringBuilder msgBuilder = new StringBuilder(label);

		if (collection != null && collection.getReference() != null)
		{
			String reference = collection.getReference();

			String refLabel = orderService.getLabel("novalnet.callbackCollectionReferenceText");

			msgBuilder.append(' ').append(String.format(refLabel, reference));
		}

		String msg = msgBuilder.toString();

		orderService.updateCallbackComments(msg, orderCode, transaction.getStatus(), msg);

		logEvent("COLLECTION", request);

		return msg;
	}

	private void logEvent(String type, NnCallbackRequestData request)
	{
		LOG.info(
				String.format("{ \"event\":\"%s\", \"tid\":\"%s\", \"parentTid\":\"%s\", \"status\":\"%s\", \"timestamp\":\"%s\" }",
						type, request.getTransaction().getTid(), request.getEvent().getParent_tid(),
						request.getTransaction().getStatus(), new Date()));
	}

	private String getCurrentDate()
	{
		Calendar cal = Calendar.getInstance();

		cal.setTime(new Date());

		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy, HH:mm:ss");

		return formatter.format(cal.getTime());
	}
}