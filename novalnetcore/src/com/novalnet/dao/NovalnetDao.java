/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.dao;

import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.OrderModel;

import java.util.List;

import com.novalnet.model.NovalnetCallbackInfoModel;
import com.novalnet.model.NovalnetPaymentInfoModel;
import com.novalnet.model.NovalnetPaymentRefInfoModel;


public interface NovalnetDao
{
	List<OrderModel> getOrderInfoModel(String orderCode);

	List<NovalnetPaymentRefInfoModel> getPaymentRefInfo(String customerNo, String paymentType);

	List<NovalnetPaymentInfoModel> getNovalnetPaymentInfo(String orderCode);

	List<NovalnetCallbackInfoModel> getCallbackInfo(String transactionId);

	List<NovalnetCallbackInfoModel> getPaymentDetailsInfo(String orderCode);

	OrderModel getOrderByTid(String tid);

	String getStoredPaymentToken(OrderModel order);

	CurrencyModel getCurrencyForIsoCode(String currencyIsoCode);

}