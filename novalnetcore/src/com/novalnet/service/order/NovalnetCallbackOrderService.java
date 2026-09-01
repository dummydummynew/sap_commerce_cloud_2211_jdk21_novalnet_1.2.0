/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.order;

import de.hybris.platform.core.model.order.OrderModel;

import java.util.Locale;

import org.json.JSONObject;

import com.novalnet.model.NovalnetPaymentInfoModel;


public interface NovalnetCallbackOrderService
{
	void updateOrderStatus(String orderCode, NovalnetPaymentInfoModel paymentInfoModel, OrderModel order);

	void updateCancelStatus(String orderCode);

	void updatePartPaidStatus(String orderCode);

	void updatePaymentInfo(String orderCode, String paymentGatewayStatus);

	void updateCallbackInfo(long callbackTid, String originalTid, int paidAmount);

	void updateCallbackComments(String comments, String orderCode, String transactionStatus, String entryComment);

	void setSessionLanguage(OrderModel order);

	Locale getLocale(OrderModel order);

	String getLabel(String key);

	String buildOrderHistoryNotes(JSONObject transactionJson, String formattedAmount, String inputValue);

}