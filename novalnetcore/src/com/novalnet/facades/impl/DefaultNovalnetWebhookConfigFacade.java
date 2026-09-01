/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.facades.impl;

import de.hybris.platform.store.BaseStoreModel;

import com.novalnet.facades.NovalnetWebhookConfigFacade;
import com.novalnet.service.payment.NovalnetWebhookService;

import jakarta.annotation.Resource;


public class DefaultNovalnetWebhookConfigFacade implements NovalnetWebhookConfigFacade
{
	@Resource(name = "novalnetWebhookService")
	private NovalnetWebhookService novalnetWebhookService;

	@Override
	public void configureWebhook(String productKey, String paymentKey, String webhookUrl, BaseStoreModel baseStore)
			throws Exception
	{
		novalnetWebhookService.configureWebhook(productKey, paymentKey, webhookUrl, baseStore);
	}
}