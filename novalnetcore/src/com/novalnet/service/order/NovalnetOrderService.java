/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.order;



import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.store.BaseStoreModel;

import com.novalnet.model.NovalnetPaymentInfoModel;

public interface NovalnetOrderService
{
	void updateOrderStatus(String orderCode, NovalnetPaymentInfoModel paymentInfoModel);

	void updateCancelStatus(String orderCode);

	void updateCallbackOrderStatus(String orderCode, String paymentMethod);

	void updatePartPaidStatus(String orderCode);

	OrderModel getOrder(String orderCode);

	OrderStatus getOrderStatus(NovalnetPaymentInfoModel paymentInfoModel, BaseStoreModel baseStore);

	void updateCallbackComments(String comments, String orderCode, String transactionStatus);

	void updateGuaranteedInvoiceCallbackComments(String callbackComments, String shortComment, String orderNo,
			String transactionStatus);

}

