/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.order.impl;

import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.orderhistory.model.OrderHistoryEntryModel;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.util.localization.Localization;

import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.novalnet.dao.NovalnetCallbackDao;
import com.novalnet.enums.NovalnetPaymentGatewayStatus;
import com.novalnet.model.NovalnetCallbackInfoModel;
import com.novalnet.model.NovalnetPaymentInfoModel;
import com.novalnet.service.order.NovalnetCallbackOrderService;

import jakarta.annotation.Resource;


public class DefaultNovalnetCallbackOrderService implements NovalnetCallbackOrderService
{
	private static final String BR = "<br/>";
	private static final String STATUS_ON_HOLD = "ON_HOLD";
	private static final String QR_IMAGE = "qr_image";

	@Resource
	private ModelService modelService;

	@Resource(name = "novalnetCallbackDao")
	private NovalnetCallbackDao novalnetCallbackDao;

	@Resource
	private CommonI18NService commonI18NService;

	@Override
	public void updateOrderStatus(String orderCode, NovalnetPaymentInfoModel paymentInfo, OrderModel order)
	{
		requireNotBlank(orderCode, "orderCode");
		requireNotNull(paymentInfo, "paymentInfo");

		NovalnetPaymentGatewayStatus status = NovalnetPaymentGatewayStatus.fromValue(paymentInfo.getPaymentGatewayStatus())
				.orElseThrow(() -> new RequestParameterException("Invalid payment gateway status", RequestParameterException.INVALID,
						"paymentGatewayStatus"));

		switch (status)
		{
			case PENDING:
				order.setStatus(OrderStatus.PAYMENT_NOT_CAPTURED);
				order.setPaymentStatus(PaymentStatus.NOTPAID);
				break;

			case ON_HOLD:
				order.setStatus(OrderStatus.PAYMENT_AUTHORIZED);
				order.setPaymentStatus(PaymentStatus.NOTPAID);
				break;

			case CONFIRMED:
				order.setStatus(OrderStatus.COMPLETED);
				order.setPaymentStatus(PaymentStatus.PAID);
				break;

			case FAILURE:
			case DEACTIVATED:
				order.setStatus(OrderStatus.CANCELLED);
				order.setPaymentStatus(PaymentStatus.NOTPAID);
				break;

			default:
				break;
		}

		modelService.save(order);
	}

	@Override
	public void updateCancelStatus(String orderCode)
	{
		requireNotBlank(orderCode, "orderCode");

		OrderModel order = novalnetCallbackDao.findOrderByCode(orderCode);
		order.setStatus(OrderStatus.CANCELLED);

		modelService.save(order);
	}

	@Override
	public void updatePartPaidStatus(String orderCode)
	{
		requireNotBlank(orderCode, "orderCode");

		OrderModel order = novalnetCallbackDao.findOrderByCode(orderCode);
		order.setPaymentStatus(PaymentStatus.PARTPAID);

		modelService.save(order);
	}

	@Override
	public void updatePaymentInfo(String orderCode, String paymentGatewayStatus)
	{
		requireNotBlank(orderCode, "orderCode");
		requireNotBlank(paymentGatewayStatus, "paymentGatewayStatus");

		NovalnetPaymentInfoModel paymentInfo = novalnetCallbackDao.getLatestNovalnetPaymentInfo(orderCode);

		paymentInfo.setPaymentGatewayStatus(paymentGatewayStatus);

		modelService.save(paymentInfo);
	}

	@Override
	public void updateCallbackInfo(long callbackTid, String originalTid, int paidAmount)
	{
		requireNotBlank(originalTid, "originalTid");

		NovalnetCallbackInfoModel callback = novalnetCallbackDao.findCallbackInfoByOriginalTid(originalTid);

		callback.setCallbackTid(callbackTid);
		callback.setPaidAmount(paidAmount);

		modelService.save(callback);
	}

	@Override
	public void updateCallbackComments(String comments, String orderCode, String transactionStatus, String entryComment)
	{
		requireNotBlank(orderCode, "orderCode");
		requireNotBlank(transactionStatus, "transactionStatus");

		NovalnetPaymentInfoModel paymentInfo = novalnetCallbackDao.getLatestNovalnetPaymentInfo(orderCode);

		String existing = paymentInfo.getOrderHistoryNotes() == null ? "" : paymentInfo.getOrderHistoryNotes();

		paymentInfo.setOrderHistoryNotes(existing + "<br/><br/>" + comments);
		paymentInfo.setPaymentGatewayStatus(transactionStatus);

		OrderModel order = novalnetCallbackDao.findOrderByCode(orderCode);

		OrderHistoryEntryModel entry = modelService.create(OrderHistoryEntryModel.class);

		entry.setOrder(order);
		entry.setTimestamp(new Date());
		entry.setDescription(entryComment);

		modelService.saveAll(paymentInfo, entry);
	}

	@Override
	public void setSessionLanguage(OrderModel order)
	{
		LanguageModel language = order.getLanguage();
		commonI18NService.setCurrentLanguage(language);
	}

	@Override
	public Locale getLocale(OrderModel order)
	{
		return commonI18NService.getLocaleForLanguage(order.getLanguage());
	}

	public CurrencyModel getCurrency(String currency)
	{
		return commonI18NService.getCurrency(currency);
	}

	private void requireNotBlank(String value, String field)
	{
		if (StringUtils.isBlank(value))
		{
			throw new RequestParameterException(field + " must not be empty", RequestParameterException.INVALID, field);
		}
	}

	private void requireNotNull(Object value, String field)
	{
		if (value == null)
		{
			throw new RequestParameterException(field + " must not be null", RequestParameterException.INVALID, field);
		}
	}

	@Override
	public String getLabel(String key)
	{
		return Localization.getLocalizedString(key);
	}

	@Override
	public String buildOrderHistoryNotes(JSONObject transaction, String amount, String paymentName)
	{
		StringBuilder notes = new StringBuilder(256);

		appendPaymentHeader(notes, transaction, paymentName);
		appendTestMode(notes, transaction);
		appendBankDetails(notes, transaction, amount);
		appendPartnerPaymentReference(notes, transaction, amount);

		return notes.toString();
	}

	private void appendPaymentHeader(StringBuilder notes, JSONObject transaction, String paymentName)
	{
		notes.append(getLabel("novalnet.paymentname")).append(" : ").append(paymentName).append(BR)
				.append(getLabel("novalnet.transactionID")).append(" ").append(transaction.optString("tid")).append(BR);

		if ("0".equals(transaction.optString("amount")))
		{
			notes.append(getLabel("novalnet.zeroAmountTransactionText")).append(BR);
		}
	}

	private void appendTestMode(StringBuilder notes, JSONObject transaction)
	{
		if ("1".equals(transaction.optString("test_mode")))
		{
			notes.append(getLabel("novalnet.testOrderText"));
		}
	}

	private void appendBankDetails(StringBuilder notes, JSONObject transaction, String amount)
	{
		JSONObject bankDetails = transaction.optJSONObject("bank_details");
		String status = transaction.optString("status");

		if (!"75".equals(transaction.optString("status_code")))
		{
			if (bankDetails != null)
			{
				notes.append(BR).append(BR).append(String.format(getLabel("novalnet.bankDetailsComments1"), amount));

				if (transaction.has("due_date") && !STATUS_ON_HOLD.equals(status))
				{
					notes.append(" ")
							.append(String.format(getLabel("novalnet.bankDetailsComments2"), transaction.optString("due_date")));
				}

				appendLabelValue(notes, "novalnet.bankDetailsAccountHolder", bankDetails.optString("account_holder"));

				appendLabelValue(notes, "novalnet.bankDetailsIban", bankDetails.optString("iban"));

				appendLabelValue(notes, "novalnet.bankDetailsBic", bankDetails.optString("bic"));

				appendLabelValue(notes, "novalnet.bankDetailsBank", bankDetails.optString("bank_name"));

				appendLabelValue(notes, "novalnet.bankPlace", bankDetails.optString("bank_place"));

				notes.append(BR).append(getLabel("novalnet.bankDetailspaymentRefernceMulti")).append(BR)
						.append(getLabel("novalnet.bankDetailsPaymentReference")).append(" : TID ").append(transaction.optString("tid"))
						.append(BR);

				if (StringUtils.isNotBlank(bankDetails.optString(QR_IMAGE)))
				{
					notes.append(getLabel("novalnet.qrCodeComments")).append(BR).append(BR).append("<img alt='nn_qr_code' src='")
							.append(bankDetails.optString(QR_IMAGE)).append("'>").append(BR);
				}
			}
		}
		else
		{
			notes.append(getLabel("novalnet.guaranteePendingNote"));
		}
	}

	private void appendPartnerPaymentReference(StringBuilder notes, JSONObject transaction, String amount)
	{
		if (!transaction.has("partner_payment_reference"))
		{
			return;
		}

		notes.append(BR).append(BR).append(getLabel("novalnet.multibancocomments1")).append(" ").append(amount).append(" ")
				.append(getLabel("novalnet.multibancocomments2")).append(BR).append(getLabel("novalnet.bankDetailsPaymentReference"))
				.append(" : ").append(transaction.optString("partner_payment_reference"));
	}

	private void appendLabelValue(StringBuilder sb, String labelKey, String value)
	{
		if (StringUtils.isNotBlank(value))
		{
			sb.append(BR).append(getLabel(labelKey)).append(" ").append(value);
		}
	}
}