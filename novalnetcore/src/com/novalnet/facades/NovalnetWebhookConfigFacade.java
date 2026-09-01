/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.facades;

import de.hybris.platform.store.BaseStoreModel;


public interface NovalnetWebhookConfigFacade
{
	void configureWebhook(String productKey, String paymentKey, String webhookUrl, BaseStoreModel baseStore) throws Exception;

}
