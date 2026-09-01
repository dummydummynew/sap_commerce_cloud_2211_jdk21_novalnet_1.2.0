/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.http;

import de.hybris.platform.store.BaseStoreModel;


public interface NovalnetApiService
{
	StringBuilder sendRequest(String url, String requestBody);

	StringBuilder followupSendRequest(String url, String requestBody, BaseStoreModel baseStore);

	StringBuilder bookTransactionAmount(String url, String requestBody, BaseStoreModel baseStore);

	String fetchMerchantDetails(String url, String requestBody, BaseStoreModel baseStore);

	String configureWebhookUrl(String productActivationKey, String paymentAccessKey, String webhookUrl, BaseStoreModel baseStore)
			throws Exception;
}
