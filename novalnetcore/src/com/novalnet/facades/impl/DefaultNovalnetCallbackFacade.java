/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */
package com.novalnet.facades.impl;

import com.novalnet.facades.NovalnetCallbackFacade;
import com.novalnet.service.payment.NovalnetCallbackService;

import de.novalnet.beans.NnCallbackRequestData;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;


public class DefaultNovalnetCallbackFacade implements NovalnetCallbackFacade
{
	@Resource(name = "novalnetCallbackService")
	private NovalnetCallbackService novalnetCallbackService;

	@Override
	public String processCallback(NnCallbackRequestData request, HttpServletRequest httpRequest)
	{
		return novalnetCallbackService.processCallback(request, httpRequest);
	}

	@Override
	public String handleTransactionCapture(NnCallbackRequestData request)
	{
		return novalnetCallbackService.handleTransactionCapture(request);
	}

	@Override
	public String handleTransactionCancel(NnCallbackRequestData request)
	{
		return novalnetCallbackService.handleTransactionCancel(request);
	}

	@Override
	public String handleTransactionUpdate(NnCallbackRequestData request)
	{
		return novalnetCallbackService.handleTransactionUpdate(request);
	}

	@Override
	public String handlePayment(NnCallbackRequestData request)
	{
		return novalnetCallbackService.handlePayment(request);
	}

	@Override
	public String handleCredit(NnCallbackRequestData request)
	{
		return novalnetCallbackService.handleCredit(request);
	}

	@Override
	public String handleRefund(NnCallbackRequestData request)
	{
		return novalnetCallbackService.handleRefund(request);
	}

	@Override
	public String handleReminder(NnCallbackRequestData request)
	{
		return novalnetCallbackService.handleReminder(request);
	}

	@Override
	public String handleCollection(NnCallbackRequestData request)
	{
		return novalnetCallbackService.handleCollection(request);
	}
}