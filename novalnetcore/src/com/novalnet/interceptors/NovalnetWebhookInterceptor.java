package com.novalnet.interceptors;

import de.hybris.platform.servicelayer.interceptor.InterceptorContext;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;
import de.hybris.platform.servicelayer.interceptor.PrepareInterceptor;
import de.hybris.platform.store.BaseStoreModel;

import org.apache.log4j.Logger;

import com.novalnet.exception.NovalnetInterceptorException;
import com.novalnet.facades.NovalnetWebhookConfigFacade;
import com.novalnet.service.http.NovalnetApiService;

import jakarta.annotation.Resource;


public class NovalnetWebhookInterceptor implements PrepareInterceptor<BaseStoreModel>
{
	private static final Logger LOG = Logger.getLogger(NovalnetWebhookInterceptor.class);

	@Resource
	private NovalnetApiService novalnetApiService;

	@Resource(name = "novalnetWebhookConfigFacade")
	private NovalnetWebhookConfigFacade novalnetWebhookConfigFacade;

	@Override
	public void onPrepare(BaseStoreModel baseStore, InterceptorContext ctx) throws InterceptorException
	{
		if (!ctx.isModified(baseStore, BaseStoreModel.NOTIFICATIONWEBHOOKURL))
		{
			return;
		}

		if (baseStore.getNotificationWebhookUrl() == null)
		{
			return;
		}

		try
		{
			String productKey = baseStore.getNovalnetAPIKey();
			String paymentKey = baseStore.getNovalnetPaymentAccessKey();
			String webhookUrl = baseStore.getNotificationWebhookUrl();

			if (isEmpty(productKey) || isEmpty(paymentKey) || isEmpty(webhookUrl))
			{
				LOG.warn("Missing required webhook configuration values. Skipping API call.");
				return;
			}

			LOG.info("Calling Novalnet Webhook API");
			novalnetWebhookConfigFacade.configureWebhook(productKey, paymentKey, webhookUrl, baseStore);
		}
		catch (Exception e)
		{
			throw new NovalnetInterceptorException("Unable to configure Novalnet webhook for BaseStore: " + baseStore.getUid(), e);
		}
	}

	private boolean isEmpty(String val)
	{
		return val == null || val.trim().isEmpty();
	}
}