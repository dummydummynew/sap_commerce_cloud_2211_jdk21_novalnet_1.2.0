/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.order.impl;

import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.orderhistory.model.OrderHistoryEntryModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.novalnet.dao.NovalnetDao;
import com.novalnet.model.NovalnetAliPayPaymentModeModel;
import com.novalnet.model.NovalnetApplePayPaymentModeModel;
import com.novalnet.model.NovalnetBancontactPaymentModeModel;
import com.novalnet.model.NovalnetBlikPaymentModeModel;
import com.novalnet.model.NovalnetCreditCardPaymentModeModel;
import com.novalnet.model.NovalnetDirectDebitSepaPaymentModeModel;
import com.novalnet.model.NovalnetEpsPaymentModeModel;
import com.novalnet.model.NovalnetGooglePayPaymentModeModel;
import com.novalnet.model.NovalnetGuaranteedDirectDebitSepaPaymentModeModel;
import com.novalnet.model.NovalnetGuaranteedInvoicePaymentModeModel;
import com.novalnet.model.NovalnetIdealPaymentModeModel;
import com.novalnet.model.NovalnetInvoicePaymentModeModel;
import com.novalnet.model.NovalnetMbWayPaymentModeModel;
import com.novalnet.model.NovalnetMultibancoPaymentModeModel;
import com.novalnet.model.NovalnetOnlineBankTransferPaymentModeModel;
import com.novalnet.model.NovalnetPayPalPaymentModeModel;
import com.novalnet.model.NovalnetPaymentInfoModel;
import com.novalnet.model.NovalnetPostFinanceCardPaymentModeModel;
import com.novalnet.model.NovalnetPostFinancePaymentModeModel;
import com.novalnet.model.NovalnetPrepaymentPaymentModeModel;
import com.novalnet.model.NovalnetPrzelewy24PaymentModeModel;
import com.novalnet.model.NovalnetTrustlyPaymentModeModel;
import com.novalnet.model.NovalnetTwintPaymentModeModel;
import com.novalnet.model.NovalnetWechatPayPaymentModeModel;
import com.novalnet.service.order.NovalnetOrderService;

import jakarta.annotation.Resource;


public class DefaultNovalnetOrderService implements NovalnetOrderService
{
	private static final String STATUS_ON_HOLD = "ON_HOLD";
	private static final String STATUS_PENDING = "PENDING";
	private static final String PAYMENT_NOVALNET_INVOICE = "novalnetInvoice";
	private static final String PAYMENT_NOVALNET_PREPAYMENT = "novalnetPrepayment";

	@Resource
	private ModelService modelService;

	@Resource
	private FlexibleSearchService flexibleSearchService;

	@Resource
	private BaseStoreService baseStoreService;

	@Resource
	private PaymentModeService paymentModeService;

	@Resource
	private NovalnetDao novalnetDao;

	@Override
	public void updateOrderStatus(String orderCode, NovalnetPaymentInfoModel paymentInfoModel)
	{
		List<OrderModel> orderInfoModel = novalnetDao.getOrderInfoModel(orderCode);
		OrderModel orderModel = modelService.get(orderInfoModel.get(0).getPk());
		BaseStoreModel baseStore = baseStoreService.getCurrentBaseStore();

		orderModel.setStatus(getOrderStatus(paymentInfoModel, baseStore));

		String paymentMethod = paymentInfoModel.getPaymentProvider();

		String[] bankPayments =
		{ PAYMENT_NOVALNET_INVOICE, PAYMENT_NOVALNET_PREPAYMENT };

		boolean isInvoicePrepayment = Arrays.asList(bankPayments).contains(paymentMethod);

		String[] pendingStatusCode =
		{ STATUS_ON_HOLD, STATUS_PENDING };

		if (isInvoicePrepayment || Arrays.asList(pendingStatusCode).contains(paymentInfoModel.getPaymentGatewayStatus()))
		{
			orderModel.setPaymentStatus(PaymentStatus.NOTPAID);
		}
		else
		{
			orderModel.setPaymentStatus(PaymentStatus.PAID);
		}

		modelService.save(orderModel);
	}

	@Override
	public void updateCancelStatus(String orderCode)
	{
		List<OrderModel> orderInfoModel = novalnetDao.getOrderInfoModel(orderCode);
		OrderModel orderModel = modelService.get(orderInfoModel.get(0).getPk());

		OrderStatus orderStatus = OrderStatus.CANCELLED;
		orderModel.setStatus(orderStatus);

		modelService.save(orderModel);
	}

	@Override
	public void updateCallbackOrderStatus(String orderCode, String paymentMethod)
	{
		List<OrderModel> orderInfoModel = novalnetDao.getOrderInfoModel(orderCode);
		OrderModel orderModel = modelService.get(orderInfoModel.get(0).getPk());
		PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(paymentMethod);

		if (PAYMENT_NOVALNET_INVOICE.equals(paymentMethod))
		{
			NovalnetInvoicePaymentModeModel novalnetPaymentMethod = (NovalnetInvoicePaymentModeModel) paymentModeModel;

			orderModel.setStatus(novalnetPaymentMethod.getNovalnetCallbackOrderStatus());
		}
		else if ("novalnetMultibanco".equals(paymentMethod))
		{
			NovalnetMultibancoPaymentModeModel novalnetPaymentMethod = (NovalnetMultibancoPaymentModeModel) paymentModeModel;

			orderModel.setStatus(novalnetPaymentMethod.getNovalnetCallbackOrderStatus());
		}
		else if (PAYMENT_NOVALNET_PREPAYMENT.equals(paymentMethod))
		{
			NovalnetPrepaymentPaymentModeModel novalnetPaymentMethod = (NovalnetPrepaymentPaymentModeModel) paymentModeModel;

			orderModel.setStatus(novalnetPaymentMethod.getNovalnetCallbackOrderStatus());
		}
		else if ("novalnetPayPal".equals(paymentMethod))
		{
			NovalnetPayPalPaymentModeModel novalnetPaymentMethod = (NovalnetPayPalPaymentModeModel) paymentModeModel;

			orderModel.setStatus(novalnetPaymentMethod.getNovalnetOrderSuccessStatus());
		}
		else if ("novalnetPrzelewy24".equals(paymentMethod))
		{
			NovalnetPrzelewy24PaymentModeModel novalnetPaymentMethod = (NovalnetPrzelewy24PaymentModeModel) paymentModeModel;

			orderModel.setStatus(novalnetPaymentMethod.getNovalnetOrderSuccessStatus());
		}

		orderModel.setPaymentStatus(PaymentStatus.PAID);
		modelService.save(orderModel);
	}

	@Override
	public void updatePartPaidStatus(String orderCode)
	{
		List<OrderModel> orderInfoModel = novalnetDao.getOrderInfoModel(orderCode);
		OrderModel orderModel = modelService.get(orderInfoModel.get(0).getPk());

		orderModel.setPaymentStatus(PaymentStatus.PARTPAID);
		modelService.save(orderModel);
	}

	@Override
	public OrderStatus getOrderStatus(NovalnetPaymentInfoModel paymentInfoModel, BaseStoreModel baseStore)
	{
		String paymentMethod = paymentInfoModel.getPaymentProvider();
		PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(paymentMethod);
		String gatewayStatus = paymentInfoModel.getPaymentGatewayStatus();

		switch (paymentMethod)
		{
			case "novalnetCreditCard":
				return resolveWithOnHoldCheck(gatewayStatus,
						((NovalnetCreditCardPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

			case "novalnetDirectDebitSepa":
				return resolveWithOnHoldCheck(gatewayStatus,
						((NovalnetDirectDebitSepaPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

			case "novalnetGuaranteedDirectDebitSepa":
				return resolveWithPendingAndOnHoldCheck(gatewayStatus,
						((NovalnetGuaranteedDirectDebitSepaPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

			case PAYMENT_NOVALNET_INVOICE:
				return resolveWithOnHoldCheck(gatewayStatus,
						((NovalnetInvoicePaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

			case "novalnetGuaranteedInvoice":
				return resolveWithPendingAndOnHoldCheck(gatewayStatus,
						((NovalnetGuaranteedInvoicePaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

			case PAYMENT_NOVALNET_PREPAYMENT:
				return ((NovalnetPrepaymentPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus();

			case "novalnetMultibanco":
				return ((NovalnetMultibancoPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus();

			case "novalnetPayPal":
				return resolveWithPendingAndOnHoldCheck(gatewayStatus,
						((NovalnetPayPalPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

			case "novalnetOnlineBankTransfer":
				return ((NovalnetOnlineBankTransferPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus();

			case "novalnetBancontact":
				return ((NovalnetBancontactPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus();

			case "novalnetPostFinanceCard":
				return ((NovalnetPostFinanceCardPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus();

			case "novalnetPostFinance":
				return ((NovalnetPostFinancePaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus();

			case "novalnetIdeal":
				return ((NovalnetIdealPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus();

			case "novalnetGooglePay":
				return resolveWithOnHoldCheck(gatewayStatus,
						((NovalnetGooglePayPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

			case "novalnetApplePay":
				return resolveWithOnHoldCheck(gatewayStatus,
						((NovalnetApplePayPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

			case "novalnetTwint":
				return ((NovalnetTwintPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus();

			case "novalnetMbWay":
				return ((NovalnetMbWayPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus();

			case "novalnetTrustly":
				return ((NovalnetTrustlyPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus();

			case "novalnetBlik":
				return ((NovalnetBlikPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus();

			case "novalnetWechatPay":
				return ((NovalnetWechatPayPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus();

			case "novalnetAlipay":
				return ((NovalnetAliPayPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus();

			case "novalnetEps":
				return ((NovalnetEpsPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus();

			case "novalnetPrzelewy24":
				return resolveWithPendingCheck(gatewayStatus,
						((NovalnetPrzelewy24PaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

			default:
				return OrderStatus.COMPLETED;
		}
	}

	private OrderStatus resolveWithOnHoldCheck(String gatewayStatus, OrderStatus successStatus)
	{
		if (STATUS_ON_HOLD.equals(gatewayStatus))
		{
			return OrderStatus.PAYMENT_AUTHORIZED;
		}

		return successStatus;
	}

	private OrderStatus resolveWithPendingAndOnHoldCheck(String gatewayStatus, OrderStatus successStatus)
	{
		if (STATUS_PENDING.equals(gatewayStatus))
		{
			return OrderStatus.PAYMENT_NOT_CAPTURED;
		}

		if (STATUS_ON_HOLD.equals(gatewayStatus))
		{
			return OrderStatus.PAYMENT_AUTHORIZED;
		}

		return successStatus;
	}

	private OrderStatus resolveWithPendingCheck(String gatewayStatus, OrderStatus successStatus)
	{
		if (STATUS_PENDING.equals(gatewayStatus))
		{
			return OrderStatus.PAYMENT_NOT_CAPTURED;
		}

		return successStatus;
	}

	@Override
	public void updateCallbackComments(String comments, String orderCode, String transactionStatus)
	{
		List<NovalnetPaymentInfoModel> paymentInfo = novalnetDao.getNovalnetPaymentInfo(orderCode);
		NovalnetPaymentInfoModel paymentInfoModel = modelService.get(paymentInfo.get(0).getPk());

		String previousComments = paymentInfoModel.getOrderHistoryNotes();
		paymentInfoModel.setOrderHistoryNotes(previousComments + "<br><br>" + comments);
		paymentInfoModel.setPaymentGatewayStatus(transactionStatus);

		List<OrderModel> orderInfoModel = novalnetDao.getOrderInfoModel(orderCode);
		OrderModel orderModel = modelService.get(orderInfoModel.get(0).getPk());

		OrderHistoryEntryModel orderEntry = modelService.create(OrderHistoryEntryModel.class);
		orderEntry.setTimestamp(new Date());
		orderEntry.setOrder(orderModel);
		orderEntry.setDescription(comments);

		modelService.saveAll(paymentInfoModel, orderEntry);
	}

	@Override
	public void updateGuaranteedInvoiceCallbackComments(String callbackComments, String shortComment, String orderNo,
			String transactionStatus)
	{
		List<NovalnetPaymentInfoModel> paymentInfo = novalnetDao.getNovalnetPaymentInfo(orderNo);
		NovalnetPaymentInfoModel paymentInfoModel = modelService.get(paymentInfo.get(0).getPk());

		String previousComments = paymentInfoModel.getOrderHistoryNotes();
		paymentInfoModel.setOrderHistoryNotes(previousComments + "<br><br>" + callbackComments);
		paymentInfoModel.setPaymentGatewayStatus(transactionStatus);

		List<OrderModel> orderInfoModel = novalnetDao.getOrderInfoModel(orderNo);
		OrderModel orderModel = modelService.get(orderInfoModel.get(0).getPk());

		OrderHistoryEntryModel orderEntry = modelService.create(OrderHistoryEntryModel.class);
		orderEntry.setTimestamp(new Date());
		orderEntry.setOrder(orderModel);
		orderEntry.setDescription(shortComment);

		modelService.saveAll(paymentInfoModel, orderEntry);
	}

	@Override
	public OrderModel getOrder(String orderCode)
	{
		List<OrderModel> orderInfoModel = novalnetDao.getOrderInfoModel(orderCode);
		return modelService.get(orderInfoModel.get(0).getPk());
	}
}