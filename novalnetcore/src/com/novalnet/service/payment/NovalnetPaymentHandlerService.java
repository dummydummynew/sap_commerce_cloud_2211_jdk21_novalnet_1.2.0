/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.payment;

import de.hybris.platform.core.model.order.payment.PaymentModeModel;

import com.novalnet.dto.PaymentConfigResult;
import com.novalnet.dto.payment.request.Customer;
import com.novalnet.dto.payment.request.HostedPage;
import com.novalnet.dto.payment.request.PaymentData;
import com.novalnet.dto.payment.request.Transaction;

import jakarta.servlet.http.HttpServletRequest;


public interface NovalnetPaymentHandlerService
{
	PaymentConfigResult handlePayment(String currentPayment, PaymentModeModel paymentModeModel, Transaction transaction,
			PaymentData paymentData, Customer customer, Integer orderAmountCent, HttpServletRequest request, HostedPage hostedPage);
}