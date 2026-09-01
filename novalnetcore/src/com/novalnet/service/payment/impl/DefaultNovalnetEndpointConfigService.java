package com.novalnet.service.payment.impl;

import de.hybris.platform.servicelayer.config.ConfigurationService;

import com.novalnet.service.payment.NovalnetEndpointConfigService;

import jakarta.annotation.Resource;


public class DefaultNovalnetEndpointConfigService implements NovalnetEndpointConfigService
{
	@Resource
	private ConfigurationService configurationService;

	@Override
	public String getPaymentUrl()
	{
		return configurationService.getConfiguration().getString("novalnet.payment.url");
	}

	@Override
	public String getPaymentHostedUrl()
	{
		return configurationService.getConfiguration().getString("novalnet.payment.hosted.url");
	}

	@Override
	public String getAuthorizeUrl()
	{
		return configurationService.getConfiguration().getString("novalnet.authorize.url");
	}

	@Override
	public String getAuthorizeHostedUrl()
	{
		return configurationService.getConfiguration().getString("novalnet.authorize.hosted.url");
	}

	@Override
	public String getTransactionCaptureUrl()
	{
		return configurationService.getConfiguration().getString("novalnet.transaction.capture.url");
	}

	@Override
	public String getTransactionCancelUrl()
	{
		return configurationService.getConfiguration().getString("novalnet.transaction.cancel.url");
	}

	@Override
	public String getTransactionRefundUrl()
	{
		return configurationService.getConfiguration().getString("novalnet.transaction.refund.url");
	}

	@Override
	public String getTransactionDetailsUrl()
	{
		return configurationService.getConfiguration().getString("novalnet.transaction.details.url");
	}

	@Override
	public String getTransactionUpdateUrl()
	{
		return configurationService.getConfiguration().getString("novalnet.transaction.update.url");
	}

	@Override
	public String getWebhookConfigureUrl()
	{
		return configurationService.getConfiguration().getString("novalnet.webhook.configure.url");
	}

	@Override
	public String getMerchantDetailsUrl()
	{
		return configurationService.getConfiguration().getString("novalnet.merchant.details.url");
	}
}