/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.dao;

import de.hybris.platform.core.model.order.OrderModel;

import com.novalnet.model.NovalnetCallbackInfoModel;
import com.novalnet.model.NovalnetPaymentInfoModel;


public interface NovalnetCallbackDao
{
	OrderModel findOrderByCode(String orderCode);

	NovalnetPaymentInfoModel getLatestNovalnetPaymentInfo(String orderCode);

	NovalnetCallbackInfoModel findCallbackInfoByOriginalTid(String originalTid);

	boolean isOrderCreatedForCart(String cartCode);

}
