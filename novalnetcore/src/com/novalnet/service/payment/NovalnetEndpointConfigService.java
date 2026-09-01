/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.payment;


public interface NovalnetEndpointConfigService
{
	String getPaymentUrl();

	String getPaymentHostedUrl();

	String getAuthorizeUrl();

	String getAuthorizeHostedUrl();

	String getTransactionCaptureUrl();

	String getTransactionCancelUrl();

	String getTransactionRefundUrl();

	String getTransactionDetailsUrl();

	String getTransactionUpdateUrl();

	String getWebhookConfigureUrl();

	String getMerchantDetailsUrl();

}