/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.facades;

import de.novalnet.beans.NnCallbackRequestData;

import jakarta.servlet.http.HttpServletRequest;


public interface NovalnetCallbackFacade
{

	String processCallback(NnCallbackRequestData request, HttpServletRequest httpRequest);

	String handleTransactionCapture(NnCallbackRequestData request);

	String handleTransactionCancel(NnCallbackRequestData request);

	String handleTransactionUpdate(NnCallbackRequestData request);

	String handlePayment(NnCallbackRequestData request);

	String handleCredit(NnCallbackRequestData request);

	String handleRefund(NnCallbackRequestData request);

	String handleReminder(NnCallbackRequestData request);

	String handleCollection(NnCallbackRequestData request);
}