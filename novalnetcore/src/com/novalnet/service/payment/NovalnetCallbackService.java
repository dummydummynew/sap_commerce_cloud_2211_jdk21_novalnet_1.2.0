/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.payment;

import de.novalnet.beans.NnCallbackRequestData;

import jakarta.servlet.http.HttpServletRequest;


public interface NovalnetCallbackService
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