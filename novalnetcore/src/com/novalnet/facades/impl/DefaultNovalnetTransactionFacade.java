/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.facades.impl;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.commands.request.FollowOnRefundRequest;
import de.hybris.platform.returns.model.ReturnRequestModel;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;

import com.novalnet.dto.NovalnetTransactionResult;
import com.novalnet.facades.NovalnetTransactionFacade;
import com.novalnet.service.payment.NovalnetTransactionBookingService;
import com.novalnet.service.payment.NovalnetTransactionService;

import jakarta.annotation.Resource;


public class DefaultNovalnetTransactionFacade implements NovalnetTransactionFacade
{
	@Resource(name = "novalnetTransactionService")
	private NovalnetTransactionService novalnetTransactionService;

	@Resource(name = "novalnetTransactionBookingService")
	private NovalnetTransactionBookingService novalnetTransactionBookingService;

	@Override
	public NovalnetTransactionResult cancelOrder(OrderModel order)
	{
		return novalnetTransactionService.cancelOrder(order);
	}

	@Override
	public boolean canCancel(OrderModel order)
	{
		return novalnetTransactionService.canCancel(order);
	}

	@Override
	public NovalnetTransactionResult captureOrder(OrderModel order)
	{
		return novalnetTransactionService.captureOrder(order);
	}

	@Override
	public boolean canCapture(OrderModel order)
	{
		return novalnetTransactionService.canCapture(order);
	}

	@Override
	public boolean canCreateReturnRequest(OrderModel order)
	{
		return novalnetTransactionService.canCreateReturnRequest(order);
	}

	@Override
	public boolean isReturnable(OrderModel order)
	{
		return novalnetTransactionService.isReturnable(order);
	}

	@Override
	public boolean isRefundable(OrderModel order)
	{
		return novalnetTransactionService.isRefundable(order);
	}

	@Override
	public boolean isFullyRefunded(OrderModel order)
	{
		return novalnetTransactionService.isFullyRefunded(order);
	}

	@Override
	public NovalnetTransactionResult refund(ReturnRequestModel returnRequest)
	{
		return novalnetTransactionService.refund(returnRequest);
	}

	@Override
	public NovalnetTransactionResult processRefund(FollowOnRefundRequest request)
	{
		return novalnetTransactionService.processRefund(request);
	}

	@Override
	public void bookTransaction(OrderModel order) throws InterceptorException
	{
		novalnetTransactionBookingService.bookTransaction(order);
	}
}