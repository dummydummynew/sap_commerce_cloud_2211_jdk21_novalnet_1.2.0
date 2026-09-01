/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.payment;

import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.commands.request.FollowOnRefundRequest;
import de.hybris.platform.returns.model.ReturnRequestModel;

import org.json.JSONObject;

import com.novalnet.dto.NovalnetTransactionResult;
import com.novalnet.dto.OrderPaymentCommentsData;


public interface NovalnetTransactionService
{
	OrderPaymentCommentsData buildOrderAndPaymentComments(String currentPayment, JSONObject transactionJsonObject,
			CartData cartData);

	NovalnetTransactionResult cancelOrder(OrderModel order);

	boolean canCancel(OrderModel order);

	NovalnetTransactionResult captureOrder(OrderModel order);

	boolean canCapture(OrderModel order);

	boolean canCreateReturnRequest(OrderModel order);

	boolean isReturnable(OrderModel order);

	boolean isRefundable(OrderModel order);

	boolean isFullyRefunded(OrderModel order);

	NovalnetTransactionResult refund(ReturnRequestModel returnRequest);

	NovalnetTransactionResult processRefund(FollowOnRefundRequest request);

}
