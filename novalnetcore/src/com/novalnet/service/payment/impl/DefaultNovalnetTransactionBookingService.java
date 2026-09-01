/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.payment.impl;

import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;
import de.hybris.platform.util.localization.Localization;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.novalnet.dao.NovalnetDao;
import com.novalnet.model.NovalnetPaymentInfoModel;
import com.novalnet.service.http.NovalnetApiService;
import com.novalnet.service.order.NovalnetOrderService;
import com.novalnet.service.payment.NovalnetTransactionBookingService;

import jakarta.annotation.Resource;


public class DefaultNovalnetTransactionBookingService implements NovalnetTransactionBookingService
{
	private static Logger LOG = Logger.getLogger(DefaultNovalnetTransactionBookingService.class);

	private static final int SUCCESS_STATUS_CODE = 100;
	private static final String GENDER_UNKNOWN = "u";
	private static final String CUSTOM_SHOP_INVOKED = "1";
	private static final String DEFAULT_LANG = "EN";

	@Resource
	private NovalnetApiService novalnetApiService;

	@Resource
	private ConfigurationService configurationService;

	@Resource
	private NovalnetDao novalnetDao;

	@Resource
	private NovalnetOrderService novalnetOrderService;

	public void bookTransaction(OrderModel order) throws InterceptorException
	{
		try
		{
			BigDecimal bookAmount = BigDecimal.valueOf(order.getBookAmount());
			int amountCent = bookAmount.intValue();

			if (amountCent <= 0)
			{
				throw new InterceptorException("Invalid booking amount for order " + order.getCode());
			}

			NovalnetPaymentInfoModel paymentInfo = (NovalnetPaymentInfoModel) order.getPaymentInfo();

			String paymentType = getPaymentType(paymentInfo.getPaymentProvider());

			AddressModel billingAddress = resolveUsableBillingAddress(order);

			Map<String, Object> merchant = new HashMap<>();
			merchant.put("signature", order.getStore().getNovalnetAPIKey());
			merchant.put("tariff", order.getStore().getNovalnetTariffId());

			String paymentToken = getToken(order);

			if (paymentToken == null || paymentToken.isEmpty())
			{
				throw new InterceptorException("No stored Novalnet payment token found on order " + order.getCode());
			}

			Map<String, Object> paymentData = new HashMap<>();
			paymentData.put("token", paymentToken);

			Map<String, Object> transaction = new HashMap<>();
			transaction.put("payment_type", paymentType);
			transaction.put("amount", amountCent);
			transaction.put("currency", order.getCurrency().getIsocode());
			transaction.put("test_mode", 1);
			transaction.put("order_no", order.getCode());
			transaction.put("payment_data", paymentData);

			Map<String, Object> customer = new HashMap<>();
			customer.put("first_name", billingAddress.getFirstname());
			customer.put("last_name", billingAddress.getLastname());
			customer.put("email", resolveEmail(order, billingAddress));
			customer.put("customer_no", order.getUser().getPk().toString());
			customer.put("gender", GENDER_UNKNOWN);

			Map<String, Object> billing = new HashMap<>();

			if (billingAddress.getStreetname() != null)
			{
				billing.put("street", billingAddress.getStreetname()
						+ (billingAddress.getStreetnumber() != null ? " " + billingAddress.getStreetnumber() : ""));
			}

			billing.put("city", billingAddress.getTown());
			billing.put("zip", billingAddress.getPostalcode());

			if (billingAddress.getCountry() != null)
			{
				billing.put("country_code", billingAddress.getCountry().getIsocode());
			}

			customer.put("billing", billing);

			Map<String, Object> custom = new HashMap<>();
			custom.put("lang", getOrderLanguage(order));
			custom.put("shop_invoked", CUSTOM_SHOP_INVOKED);

			Map<String, Object> request = new HashMap<>();
			request.put("merchant", merchant);
			request.put("customer", customer);
			request.put("transaction", transaction);
			request.put("custom", custom);

			Gson gson = new GsonBuilder().create();
			String json = gson.toJson(request);

			LOG.info("Novalnet book-amount request for order " + order.getCode() + ": " + json);

			String paymentApiUrl = configurationService.getConfiguration().getString("novalnet.payment.url");

			StringBuilder response = novalnetApiService.bookTransactionAmount(paymentApiUrl, json, order.getStore());

			LOG.info("Novalnet Response : " + response);

			JSONObject responseJson = new JSONObject(response.toString());
			JSONObject result = responseJson.getJSONObject("result");
			int statusCode = result.getInt("status_code");

			if (statusCode != SUCCESS_STATUS_CODE)
			{
				String statusText = result.has("status_text") ? result.getString("status_text")
						: result.optString("status_desc", "Unknown error");

				throw new InterceptorException(
						"Novalnet update booking failed for order " + order.getCode() + ". Reason: " + statusText);
			}

			JSONObject transactionResponse = responseJson.getJSONObject("transaction");

			String newTid = String.valueOf(transactionResponse.get("tid"));
			String amount = BigDecimal.valueOf(amountCent).movePointLeft(2).toPlainString();

			String currency = order.getCurrency().getIsocode();

			String comments = String.format(Localization.getLocalizedString("novalnet.booking.success"), amount, currency, newTid);

			novalnetOrderService.updateCallbackComments(comments, order.getCode(), paymentInfo.getPaymentGatewayStatus());

			order.setPaidAmount(amountCent);
			order.setBookAmount(null);

			LOG.info("Successfully updated paid amount to " + amountCent + " cents for order " + order.getCode());
		}
		catch (InterceptorException ie)
		{
			throw ie;
		}
		catch (Exception e)
		{
			throw new InterceptorException("Novalnet API call failed for order " + order.getCode(), e);
		}
	}

	private AddressModel resolveUsableBillingAddress(OrderModel order) throws InterceptorException
	{
		AddressModel paymentAddress = order.getPaymentAddress();

		if (isUsableAddress(paymentAddress))
		{
			return paymentAddress;
		}

		LOG.warn("Payment address on order " + order.getCode()
				+ (paymentAddress == null ? " is null" : " (PK " + paymentAddress.getPk() + ") is missing required fields")
				+ "; attempting fallback. This typically means the checkout " + "billing-address selection "
				+ "(same-as-delivery vs. a separately chosen/saved address) " + "did not populate the address correctly.");

		AddressModel deliveryAddress = order.getDeliveryAddress();

		if (isUsableAddress(deliveryAddress))
		{
			LOG.warn("Falling back to delivery address for order " + order.getCode());

			return deliveryAddress;
		}

		AddressModel customerDefault = findUsableCustomerAddress(order);

		if (customerDefault != null)
		{
			LOG.warn("Falling back to a saved customer address for order " + order.getCode());

			return customerDefault;
		}

		throw new InterceptorException("No usable billing address found for order " + order.getCode()
				+ " (payment, delivery, and saved addresses are all incomplete)");
	}

	private AddressModel findUsableCustomerAddress(OrderModel order)
	{
		if (order.getUser() instanceof CustomerModel)
		{
			CustomerModel customer = (CustomerModel) order.getUser();

			for (AddressModel candidate : customer.getAddresses())
			{
				if (isUsableAddress(candidate))
				{
					return candidate;
				}
			}
		}

		return null;
	}

	private boolean isUsableAddress(AddressModel address)
	{
		return address != null && address.getFirstname() != null && !address.getFirstname().isBlank()
				&& address.getLastname() != null && !address.getLastname().isBlank() && address.getTown() != null
				&& !address.getTown().isBlank() && address.getPostalcode() != null && !address.getPostalcode().isBlank()
				&& address.getCountry() != null;
	}

	private String resolveEmail(OrderModel order, AddressModel billingAddress)
	{
		if (billingAddress.getEmail() != null && !billingAddress.getEmail().isBlank())
		{
			return billingAddress.getEmail();
		}

		if (order.getUser() != null && order.getUser().getUid() != null)
		{
			return order.getUser().getUid();
		}

		return null;
	}

	private String getToken(OrderModel order)
	{
		return novalnetDao.getStoredPaymentToken(order);
	}

	private String getOrderLanguage(OrderModel order)
	{
		LanguageModel language = order.getLanguage();

		if (language != null && language.getIsocode() != null && !language.getIsocode().isEmpty())
		{
			return language.getIsocode().toUpperCase(Locale.ROOT);
		}

		LOG.warn("No language found on order " + order.getCode() + ", falling back to " + DEFAULT_LANG);

		return DEFAULT_LANG;
	}

	public static String getPaymentType(String paymentProvider)
	{
		switch (paymentProvider)
		{
			case "novalnetCreditCard":
				return "CREDITCARD";

			case "novalnetDirectDebitSepa":
				return "DIRECT_DEBIT_SEPA";

			case "novalnetGooglePay":
				return "GOOGLEPAY";

			case "novalnetApplePay":
				return "APPLEPAY";

			case "novalnetDirectDebitAch":
				return "DIRECT_DEBIT_ACH";

			default:
				return paymentProvider;
		}
	}
}