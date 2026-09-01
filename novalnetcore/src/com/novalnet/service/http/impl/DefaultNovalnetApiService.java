/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.http.impl;

import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novalnet.service.http.NovalnetApiService;
import com.novalnet.service.payment.NovalnetEndpointConfigService;

import jakarta.annotation.Resource;


public class DefaultNovalnetApiService implements NovalnetApiService
{
	private static final Logger LOGGER = Logger.getLogger(DefaultNovalnetApiService.class);
	private static final int CONNECT_TIMEOUT_MS = 20000;
	private static final int READ_TIMEOUT_MS = 60000;

	private static final String HEADER_CONTENT_TYPE = "Content-Type";
	private static final String HEADER_ACCEPT = "Accept";
	private static final String HEADER_ACCESS_KEY = "X-NN-Access-Key";
	private static final String CONTENT_TYPE_JSON = "application/json";
	private static final String ERROR_CALLING_NOVALNET_API = "Error while calling Novalnet API";

	@Resource
	private BaseStoreService baseStoreService;

	@Resource
	private ConfigurationService configurationService;

	@Resource
	private NovalnetEndpointConfigService novalnetEndpointConfigService;

	private static String encodeAccessKey(String accessKey)
	{
		return Base64.getEncoder().encodeToString(accessKey.trim().getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public StringBuilder sendRequest(String url, String jsonString)
	{
		BaseStoreModel baseStore = baseStoreService.getCurrentBaseStore();

		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS)).build();

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofMillis(READ_TIMEOUT_MS))
				.header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON).header(HEADER_ACCEPT, CONTENT_TYPE_JSON)
				.header(HEADER_ACCESS_KEY, encodeAccessKey(baseStore.getNovalnetPaymentAccessKey()))
				.POST(HttpRequest.BodyPublishers.ofString(jsonString)).build();

		try
		{
			LOGGER.info("Novalnet Request Body : " + jsonString);
			LOGGER.info("Novalnet Request URL : " + url);

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			return new StringBuilder(response.body());
		}
		catch (IOException | InterruptedException ex)
		{
			if (ex instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}

			LOGGER.error(ERROR_CALLING_NOVALNET_API, ex);
			return new StringBuilder();
		}
	}


	@Override
	public StringBuilder followupSendRequest(String url, String jsonString, BaseStoreModel baseStore)
	{
		if (baseStore == null)
		{
			throw new IllegalArgumentException("BaseStore cannot be null");
		}

		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS)).build();

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofMillis(READ_TIMEOUT_MS))
				.header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON).header(HEADER_ACCEPT, CONTENT_TYPE_JSON)
				.header(HEADER_ACCESS_KEY, encodeAccessKey(baseStore.getNovalnetPaymentAccessKey()))
				.POST(HttpRequest.BodyPublishers.ofString(jsonString)).build();

		try
		{
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			return new StringBuilder(response.body());
		}
		catch (IOException | InterruptedException ex)
		{
			if (ex instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}
			LOGGER.error(ERROR_CALLING_NOVALNET_API, ex);
			return new StringBuilder();
		}
	}

	@Override
	public StringBuilder bookTransactionAmount(String url, String jsonString, BaseStoreModel baseStore)
	{
		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS)).build();

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofMillis(READ_TIMEOUT_MS))
				.header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON).header(HEADER_ACCEPT, CONTENT_TYPE_JSON)
				.header(HEADER_ACCESS_KEY, encodeAccessKey(baseStore.getNovalnetPaymentAccessKey()))
				.POST(HttpRequest.BodyPublishers.ofString(jsonString)).build();

		try
		{
			LOGGER.info("Novalnet Request Body : " + jsonString);
			LOGGER.info("Novalnet Request URL : " + url);

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			return new StringBuilder(response.body());
		}
		catch (IOException | InterruptedException ex)
		{
			if (ex instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}

			LOGGER.error(ERROR_CALLING_NOVALNET_API, ex);
			return new StringBuilder();
		}
	}


	@Override
	public String fetchMerchantDetails(String url, String jsonBody, BaseStoreModel baseStore)
	{
		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS)).build();

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofMillis(READ_TIMEOUT_MS))
				.header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON).header(HEADER_ACCEPT, CONTENT_TYPE_JSON)
				.header(HEADER_ACCESS_KEY, encodeAccessKey(baseStore.getNovalnetPaymentAccessKey()))
				.POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();

		try
		{
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			LOGGER.info("Novalnet API Response Code : " + response.statusCode());

			return response.body();
		}
		catch (IOException | InterruptedException ex)
		{
			if (ex instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}

			LOGGER.error(ERROR_CALLING_NOVALNET_API, ex);
			return "";
		}
	}

	@Override
	public String configureWebhookUrl(String productActivationKey, String paymentAccessKey, String webhookUrl,
			BaseStoreModel baseStore) throws Exception
	{
		String languageIso = baseStore.getDefaultLanguage().getIsocode().toUpperCase();

		Map<String, Object> requestMap = new HashMap<>();

		Map<String, Object> merchantMap = new HashMap<>();
		merchantMap.put("signature", productActivationKey);

		Map<String, Object> webhookMap = new HashMap<>();
		webhookMap.put("url", webhookUrl);

		Map<String, Object> customMap = new HashMap<>();
		customMap.put("lang", languageIso);

		requestMap.put("merchant", merchantMap);
		requestMap.put("webhook", webhookMap);
		requestMap.put("custom", customMap);

		ObjectMapper mapper = new ObjectMapper();
		String jsonBody = mapper.writeValueAsString(requestMap);

		LOGGER.debug("Webhook Request Body: " + jsonBody);

		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS)).build();

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(novalnetEndpointConfigService.getWebhookConfigureUrl()))
				.timeout(Duration.ofMillis(READ_TIMEOUT_MS)).header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
				.header(HEADER_ACCEPT, CONTENT_TYPE_JSON)
				.header(HEADER_ACCESS_KEY, Base64.getEncoder().encodeToString(paymentAccessKey.getBytes(StandardCharsets.UTF_8)))
				.POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();

		try
		{
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			LOGGER.info("Novalnet API Response Code : " + response.statusCode());

			return response.body();
		}
		catch (IOException | InterruptedException ex)
		{
			if (ex instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}

			LOGGER.error(ERROR_CALLING_NOVALNET_API, ex);
			return "";
		}
	}

}