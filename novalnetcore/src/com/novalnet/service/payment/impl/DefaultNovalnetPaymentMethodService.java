/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.payment.impl;

import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.i18n.I18NFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.util.Config;
import de.hybris.platform.util.localization.Localization;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.novalnet.dto.AddressForm;
import com.novalnet.dto.NovalnetPaymentDetailsForm;
import com.novalnet.dto.PaymentConfigResult;
import com.novalnet.dto.payment.request.Billing;
import com.novalnet.dto.payment.request.Custom;
import com.novalnet.dto.payment.request.Customer;
import com.novalnet.dto.payment.request.HostedPage;
import com.novalnet.dto.payment.request.Merchant;
import com.novalnet.dto.payment.request.NovalnetPaymentRequest;
import com.novalnet.dto.payment.request.PaymentData;
import com.novalnet.dto.payment.request.Shipping;
import com.novalnet.dto.payment.request.Transaction;
import com.novalnet.model.NovalnetApplePayPaymentModeModel;
import com.novalnet.model.NovalnetCreditCardPaymentModeModel;
import com.novalnet.model.NovalnetDirectDebitAchPaymentModeModel;
import com.novalnet.model.NovalnetDirectDebitSepaPaymentModeModel;
import com.novalnet.model.NovalnetGooglePayPaymentModeModel;
import com.novalnet.model.NovalnetGuaranteedDirectDebitSepaPaymentModeModel;
import com.novalnet.service.checkout.NovalnetCheckoutService;
import com.novalnet.service.http.NovalnetApiService;
import com.novalnet.service.payment.NovalnetEndpointConfigService;
import com.novalnet.service.payment.NovalnetPaymentHandlerService;
import com.novalnet.service.payment.NovalnetPaymentMethodService;
import com.novalnet.service.payment.NovalnetPaymentService;
import com.novalnet.util.NovalnetUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;


public class DefaultNovalnetPaymentMethodService implements NovalnetPaymentMethodService
{
	public static final String REDIRECT_PREFIX = "redirect:";
	public static final int PREPAYMENT_FROM_DATE = 7;
	public static final int PREPAYMENT_TILL_DATE = 28;

	protected static final String REDIRECT_URL_ORDER_CONFIRMATION = REDIRECT_PREFIX + "/checkout/novalnet/orderConfirmation/";

	private static final String NOVALNET_VERSION = "1.2.0";

	private static final String NOVALNET_ACCOUNT_HOLDER_REQUIRED = "novalnet.account.holder.required";
	private static final String NOVALNET_CREDIT_CARD = "novalnetCreditCard";
	private static final String NOVALNET_DIRECT_DEBIT_SEPA = "novalnetDirectDebitSepa";
	private static final String NOVALNET_DIRECT_DEBIT_ACH = "novalnetDirectDebitAch";
	private static final String NOVALNET_GUARANTEED_INVOICE = "novalnetGuaranteedInvoice";
	private static final String NOVALNET_DIRECT_DEBIT_ACH_STORE_PAYMENT_DATA = "novalnetDirectDebitAchStorePaymentData";
	private static final String NOVALNET_CHECKOUT_ERROR = "novalnetCheckoutError";
	private static final String NOVALNET_ZERO_AMOUNT_BOOKING = "novalnetZeroAmountBooking";
	private static final String NOVALNET_GUARANTEED_SEPA_STORE_PAYMENT_DATA = "novalnetGuaranteedDirectDebitSepaStorePaymentData";
	private static final String NOVALNET_DIRECT_DEBIT_ACH_ROUTING_NUMBER = "novalnetDirectDebitAchRoutingNumber";
	private static final String NOVALNET_CREDIT_CARD_STORE_PAYMENT_DATA = "novalnetCreditCardStorePaymentData";
	private static final String NOVALNET_UNKNOWN_ERROR = "novalnet.unknown.error";
	private static final String NOVALNET_DIRECT_DEBIT_SEPA_ACCOUNT_IBAN = "novalnetDirectDebitSepaAccountIban";
	private static final String NOVALNET_IBAN_REQUIRED = "novalnet.iban.required";
	private static final String NOVALNET_GUARANTEED_DIRECT_DEBIT_SEPA = "novalnetGuaranteedDirectDebitSepa";
	private static final String NOVALNET_GUARANTEED_SEPA_ACCOUNT_IBAN = "novalnetGuaranteedDirectDebitSepaAccountIban";
	private static final String NOVALNET_GUARANTEED_SEPA_ACCOUNT_HOLDER = "novalnetGuaranteedDirectDebitSepaAccountHolder";
	private static final String NOVALNET_DIRECT_DEBIT_SEPA_ACCOUNT_HOLDER = "novalnetDirectDebitSepaAccountHolder";
	private static final String NOVALNET_DIRECT_DEBIT_ACH_ACCOUNT_NUMBER = "novalnetDirectDebitAchAchAccountNumber";
	private static final String NOVALNET_ROUTING_NUMBER_REQUIRED = "novalnet.routing.number.required";
	private static final String TRANSACTION = "transaction";
	private static final String NOVALNET_GOOGLE_PAY = "novalnetGooglePay";
	private static final String NOVALNET_ACCOUNT_NUMBER_REQUIRED = "novalnet.account.number.required";
	private static final String TXN_SECRET = "txn_secret";
	private static final String NOVALNET_DIRECT_DEBIT_SEPA_ACCOUNT_BIC = "novalnetDirectDebitSepaAccountBic";
	private static final String NOVALNET_DIRECT_DEBIT_ACH_ACCOUNT_HOLDER = "novalnetDirectDebitAchAccountHolder";
	private static final String NOVALNET_APPLE_PAY = "novalnetApplePay";
	private static final String NOVALNET_DIRECT_DEBIT_SEPA_TOKEN = "novalnetDirectDebitSepatoken";
	private static final String NOVALNET_DIRECT_DEBIT_SEPA_STORE_PAYMENT_DATA = "novalnetDirectDebitSepaStorePaymentData";
	private static final String NOVALNET_GUARANTEED_SEPA_ACCOUNT_BIC = "novalnetGuaranteedDirectDebitSepaAccountBic";
	private static final String PAYMENT_DATA = "payment_data";

	@Resource
	private NovalnetApiService novalnetApiService;

	@Resource
	private NovalnetPaymentService novalnetPaymentService;

	@Resource
	private NovalnetEndpointConfigService novalnetEndpointConfigService;

	@Resource
	private ConfigurationService configurationService;

	@Resource
	private I18NFacade i18NFacade;

	@Resource
	private SessionService sessionService;

	@Resource
	private NovalnetCheckoutService novalnetCheckoutService;

	@Resource
	private PaymentModeService paymentModeService;

	@Resource
	private NovalnetPaymentHandlerService novalnetPaymentHandlerService;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultNovalnetPaymentMethodService.class);

	@Override
	public String callNovalnetMerchantDetails(String productActivationKey, BaseStoreModel baseStore) throws Exception
	{
		Map<String, Object> requestMap = new HashMap<>();
		Map<String, Object> merchantMap = new HashMap<>();

		merchantMap.put("signature", productActivationKey);

		Map<String, Object> customMap = new HashMap<>();
		customMap.put("lang", baseStore.getDefaultLanguage().getIsocode().toUpperCase());

		requestMap.put("merchant", merchantMap);
		requestMap.put("custom", customMap);

		String requestBody = MAPPER.writeValueAsString(requestMap);

		return novalnetApiService.fetchMerchantDetails(novalnetEndpointConfigService.getMerchantDetailsUrl(), requestBody,
				baseStore);
	}

	@Override
	public void addPaymentProcess(Model model, NovalnetPaymentDetailsForm paymentDetailsForm, CartData cartData)
			throws CMSItemNotFoundException
	{
		novalnetPaymentService.addPaymentProcess(model, paymentDetailsForm, cartData);
	}

	@Override
	public void populateCustomerAddressDetails(Model model, NovalnetPaymentDetailsForm paymentDetailsForm, CartData cartData,
			Customer customer, AddressData addressData)
	{
		AddressForm addressForm = paymentDetailsForm.getBillingAddress();
		Billing billing = new Billing();
		Shipping shipping = new Shipping();
		NovalnetPaymentRequest request = new NovalnetPaymentRequest();

		if (Boolean.TRUE.equals(paymentDetailsForm.isUseDeliveryAddress()))
		{
			billing.setFirst_name(addressData.getFirstName());
			billing.setLast_name(addressData.getLastName());
			billing.setStreet(addressData.getLine1() + addressData.getLine2());
			billing.setCity(addressData.getTown());
			billing.setZip(addressData.getPostalCode());

			if (addressData.getCountry() != null)
			{
				billing.setCountry_code(addressData.getCountry().getIsocode());
			}

			shipping.setSame_as_billing("1");
		}
		else
		{
			shipping.setFirst_name(cartData.getDeliveryAddress().getFirstName());
			shipping.setLast_name(cartData.getDeliveryAddress().getLastName());
			shipping.setStreet(cartData.getDeliveryAddress().getLine1() + cartData.getDeliveryAddress().getLine2());
			shipping.setCity(cartData.getDeliveryAddress().getTown());
			shipping.setZip(cartData.getDeliveryAddress().getPostalCode());

			if (cartData.getDeliveryAddress().getCountry() != null)
			{
				shipping.setCountry_code(cartData.getDeliveryAddress().getCountry().getIsocode());
			}

			if (addressForm != null)
			{
				addressData.setId(addressForm.getAddressId());
				addressData.setTitleCode(addressForm.getTitleCode());
				addressData.setFirstName(addressForm.getFirstName());
				addressData.setLastName(addressForm.getLastName());
				addressData.setLine1(addressForm.getLine1());
				addressData.setLine2(addressForm.getLine2());
				addressData.setTown(addressForm.getTownCity());
				addressData.setPostalCode(addressForm.getPostcode());

				if (addressForm.getCountryIso() != null)
				{
					addressData.setCountry(i18NFacade.getCountryForIsocode(addressForm.getCountryIso()));
				}

				if (addressForm.getRegionIso() != null)
				{
					addressData.setRegion(i18NFacade.getRegion(addressForm.getCountryIso(), addressForm.getRegionIso()));
				}

				addressData.setShippingAddress(Boolean.TRUE.equals(addressForm.getShippingAddress()));
				addressData.setBillingAddress(Boolean.TRUE.equals(addressForm.getBillingAddress()));
			}

			billing.setFirst_name(paymentDetailsForm.getBillTo_firstName());
			billing.setLast_name(paymentDetailsForm.getBillTo_lastName());
			billing.setStreet(paymentDetailsForm.getBillTo_street1() + paymentDetailsForm.getBillTo_street2());
			billing.setCity(paymentDetailsForm.getBillTo_city());
			billing.setZip(paymentDetailsForm.getBillTo_postalCode());
			billing.setCountry_code(paymentDetailsForm.getBillTo_country());

			addressData.setTitleCode(paymentDetailsForm.getBillTo_titleCode());
			addressData.setFirstName(paymentDetailsForm.getBillTo_firstName());
			addressData.setLastName(paymentDetailsForm.getBillTo_lastName());
			addressData.setLine1(paymentDetailsForm.getBillTo_street1());
			addressData.setLine2(paymentDetailsForm.getBillTo_street2());
			addressData.setTown(paymentDetailsForm.getBillTo_city());
			addressData.setPostalCode(paymentDetailsForm.getBillTo_postalCode());
		}

		customer.setFirst_name(addressData.getFirstName());
		customer.setLast_name(addressData.getLastName());
		customer.setBilling(billing);
		customer.setShipping(shipping);

		request.setCustomer(customer);
		sessionService.setAttribute("novalnetPaymentRequest", request);
	}

	@Override
	public boolean processOneClickTokenData(String currentPayment, NovalnetPaymentDetailsForm paymentDetailsForm, Model model,
			CartData cartData, AddressData deliveryAddress) throws CMSItemNotFoundException
	{
		if (currentPayment == null)
		{
			return false;
		}

		switch (currentPayment)
		{
			case NOVALNET_DIRECT_DEBIT_SEPA:
				return handleSepa(paymentDetailsForm);

			case NOVALNET_DIRECT_DEBIT_ACH:
				return handleAch(paymentDetailsForm);

			case NOVALNET_GUARANTEED_DIRECT_DEBIT_SEPA:
				return handleGuaranteedSepa(paymentDetailsForm, model, cartData, deliveryAddress);

			case NOVALNET_GUARANTEED_INVOICE:
				return handleGuaranteedInvoice(paymentDetailsForm, model, cartData, deliveryAddress);

			case NOVALNET_CREDIT_CARD:
				return handleCreditCard(paymentDetailsForm, model, cartData);

			case NOVALNET_GOOGLE_PAY:
				return handleGooglePay(currentPayment);

			case NOVALNET_APPLE_PAY:
				return handleApplePay(currentPayment);

			default:
				return true;
		}
	}

	private boolean handleGooglePay(String currentPayment)
	{
		if (currentPayment == null)
		{
			return false;
		}

		PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(currentPayment);

		if (paymentModeModel == null)
		{
			return false;
		}

		NovalnetGooglePayPaymentModeModel googlePayPaymentMethod = (NovalnetGooglePayPaymentModeModel) paymentModeModel;

		boolean zeroAmountBooking = NovalnetUtils.isZeroAmountBooking(googlePayPaymentMethod.getOnholdActionTypeWithZeroAmount());

		sessionService.setAttribute("novalnetGooglePayStorePaymentData", zeroAmountBooking);

		LOGGER.info("novalnetGooglePayStorePaymentData: {}", zeroAmountBooking);

		return true;
	}

	private boolean handleApplePay(String currentPayment)
	{
		if (currentPayment == null)
		{
			return false;
		}

		PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(currentPayment);

		if (paymentModeModel == null)
		{
			return false;
		}

		NovalnetApplePayPaymentModeModel applePayPaymentMethod = (NovalnetApplePayPaymentModeModel) paymentModeModel;

		boolean zeroAmountBooking = NovalnetUtils.isZeroAmountBooking(applePayPaymentMethod.getOnholdActionTypeWithZeroAmount());

		sessionService.setAttribute("novalnetApplePayStorePaymentData", zeroAmountBooking);

		LOGGER.info("novalnetApplePayStorePaymentData: {}", zeroAmountBooking);

		return true;
	}

	private boolean requireAndStore(String key, String value, String errorMessageKey)
	{
		if (value != null && !"".equals(value.trim()))
		{
			sessionService.setAttribute(key, value.trim());
			return true;
		}

		sessionService.setAttribute(NOVALNET_CHECKOUT_ERROR, Localization.getLocalizedString(errorMessageKey));

		return false;
	}

	private void storeIfPresent(String key, String value)
	{
		if (value != null && !"".equals(value.trim()))
		{
			sessionService.setAttribute(key, value.trim());
		}
	}

	private boolean handleSepa(NovalnetPaymentDetailsForm paymentDetailsForm)
	{
		PaymentModeModel paymentNovalnetDirectDebitSepaModeModel = paymentModeService
				.getPaymentModeForCode("novalnetDirectDebitSepa");

		NovalnetDirectDebitSepaPaymentModeModel novalnetDirectDebitSepaPaymentMethod = (NovalnetDirectDebitSepaPaymentModeModel) paymentNovalnetDirectDebitSepaModeModel;

		sessionService.setAttribute(NOVALNET_DIRECT_DEBIT_SEPA_STORE_PAYMENT_DATA, false);

		if (Boolean.TRUE.equals(novalnetCheckoutService.isGuestUser()) && !validateGuestSepaFields(paymentDetailsForm))
		{
			return false;
		}

		boolean oneClickEligible = Boolean.TRUE.equals(novalnetDirectDebitSepaPaymentMethod.getNovalnetOneClickShopping())
				&& Boolean.FALSE.equals(novalnetCheckoutService.isGuestUser());

		if (oneClickEligible)
		{
			return handleSepaOneClick(paymentDetailsForm);
		}

		storeIfPresent(NOVALNET_DIRECT_DEBIT_SEPA_ACCOUNT_IBAN, paymentDetailsForm.getAccountIban());
		storeIfPresent(NOVALNET_DIRECT_DEBIT_SEPA_ACCOUNT_HOLDER, paymentDetailsForm.getAccountHolder());
		storeIfPresent(NOVALNET_DIRECT_DEBIT_SEPA_ACCOUNT_BIC, paymentDetailsForm.getAccountBic());

		return true;
	}

	private boolean validateGuestSepaFields(NovalnetPaymentDetailsForm paymentDetailsForm)
	{
		if (StringUtils.isBlank(paymentDetailsForm.getAccountIban()))
		{
			sessionService.setAttribute(NOVALNET_CHECKOUT_ERROR, Localization.getLocalizedString(NOVALNET_IBAN_REQUIRED));
			return false;
		}

		if (StringUtils.isBlank(paymentDetailsForm.getAccountHolder()))
		{
			sessionService.setAttribute(NOVALNET_CHECKOUT_ERROR, Localization.getLocalizedString(NOVALNET_ACCOUNT_HOLDER_REQUIRED));
			return false;
		}

		sessionService.setAttribute(NOVALNET_DIRECT_DEBIT_SEPA_ACCOUNT_IBAN, paymentDetailsForm.getAccountIban().trim());

		sessionService.setAttribute(NOVALNET_DIRECT_DEBIT_SEPA_ACCOUNT_HOLDER, paymentDetailsForm.getAccountHolder().trim());

		storeIfPresent(NOVALNET_DIRECT_DEBIT_SEPA_ACCOUNT_BIC, paymentDetailsForm.getAccountBic());

		return true;
	}

	private boolean handleSepaOneClick(NovalnetPaymentDetailsForm paymentDetailsForm)
	{
		String oneClickData = paymentDetailsForm != null ? paymentDetailsForm.getDirectDebitSepaOneClickData1() : null;

		if (oneClickData == null)
		{
			oneClickData = "";
		}

		if (paymentDetailsForm == null)
		{
			LOGGER.info("paymentDetailsForm was null");
		}

		if ("".equals(oneClickData))
		{
			return storeSepaManualEntry(paymentDetailsForm);
		}

		String selection = paymentDetailsForm.getDirectDebitSepaOneClickData1().trim();

		if ("3".equals(selection))
		{
			return storeSepaManualEntry(paymentDetailsForm);
		}

		if ("1".equals(selection))
		{
			sessionService.setAttribute(NOVALNET_DIRECT_DEBIT_SEPA_TOKEN,
					sessionService.getAttribute("novalnetDirectDebitSepaOneClickToken1"));
		}

		if ("2".equals(selection))
		{
			sessionService.setAttribute(NOVALNET_DIRECT_DEBIT_SEPA_TOKEN,
					sessionService.getAttribute("novalnetDirectDebitSepaOneClickToken2"));
		}

		return true;
	}

	private boolean storeSepaManualEntry(NovalnetPaymentDetailsForm paymentDetailsForm)
	{
		if (Boolean.TRUE.equals(paymentDetailsForm.isDirectDebitSepaSaveData()))
		{
			sessionService.setAttribute(NOVALNET_DIRECT_DEBIT_SEPA_STORE_PAYMENT_DATA, true);
		}

		storeIfPresent(NOVALNET_DIRECT_DEBIT_SEPA_ACCOUNT_BIC, paymentDetailsForm.getAccountBic());

		if (!requireAndStore(NOVALNET_DIRECT_DEBIT_SEPA_ACCOUNT_IBAN, paymentDetailsForm.getAccountIban(), NOVALNET_IBAN_REQUIRED))
		{
			return false;
		}

		return requireAndStore(NOVALNET_DIRECT_DEBIT_SEPA_ACCOUNT_HOLDER, paymentDetailsForm.getAccountHolder(),
				NOVALNET_ACCOUNT_HOLDER_REQUIRED);
	}

	private boolean handleAch(NovalnetPaymentDetailsForm paymentDetailsForm)
	{
		PaymentModeModel paymentNovalnetDirectDebitAchModeModel = paymentModeService
				.getPaymentModeForCode("novalnetDirectDebitAch");

		NovalnetDirectDebitAchPaymentModeModel novalnetDirectDebitAchPaymentMethod = (NovalnetDirectDebitAchPaymentModeModel) paymentNovalnetDirectDebitAchModeModel;

		sessionService.setAttribute(NOVALNET_DIRECT_DEBIT_ACH_STORE_PAYMENT_DATA, false);

		if (Boolean.TRUE.equals(novalnetCheckoutService.isGuestUser()) && !validateGuestAchFields(paymentDetailsForm))
		{
			return false;
		}

		boolean oneClickEligible = Boolean.TRUE.equals(novalnetDirectDebitAchPaymentMethod.getNovalnetOneClickShopping())
				&& Boolean.FALSE.equals(novalnetCheckoutService.isGuestUser());

		if (oneClickEligible)
		{
			return handleAchOneClick(paymentDetailsForm);
		}

		storeIfPresent(NOVALNET_DIRECT_DEBIT_ACH_ACCOUNT_HOLDER, paymentDetailsForm.getAchAccountHolder());
		storeIfPresent(NOVALNET_DIRECT_DEBIT_ACH_ACCOUNT_NUMBER, paymentDetailsForm.getAchAccountNumber());
		storeIfPresent(NOVALNET_DIRECT_DEBIT_ACH_ROUTING_NUMBER, paymentDetailsForm.getAchRoutingNumber());

		return true;
	}

	private boolean validateGuestAchFields(NovalnetPaymentDetailsForm paymentDetailsForm)
	{
		if (!requireAndStore(NOVALNET_DIRECT_DEBIT_ACH_ACCOUNT_HOLDER, paymentDetailsForm.getAchAccountHolder(),
				NOVALNET_ACCOUNT_HOLDER_REQUIRED))
		{
			return false;
		}

		if (!requireAndStore(NOVALNET_DIRECT_DEBIT_ACH_ACCOUNT_NUMBER, paymentDetailsForm.getAchAccountNumber(),
				NOVALNET_ACCOUNT_NUMBER_REQUIRED))
		{
			return false;
		}

		return requireAndStore(NOVALNET_DIRECT_DEBIT_ACH_ROUTING_NUMBER, paymentDetailsForm.getAchRoutingNumber(),
				NOVALNET_ROUTING_NUMBER_REQUIRED);
	}

	private boolean handleAchOneClick(NovalnetPaymentDetailsForm paymentDetailsForm)
	{
		String oneClickData = paymentDetailsForm != null ? paymentDetailsForm.getDirectDebitAchOneClickData1() : null;

		if (oneClickData == null)
		{
			oneClickData = "";
		}

		if (paymentDetailsForm == null)
		{
			LOGGER.info("paymentDetailsForm was null");
		}

		if ("".equals(oneClickData))
		{
			return storeAchManualEntry(paymentDetailsForm);
		}

		String selection = paymentDetailsForm.getDirectDebitAchOneClickData1().trim();

		if ("3".equals(selection))
		{
			return storeAchManualEntry(paymentDetailsForm);
		}

		if ("1".equals(selection))
		{
			sessionService.setAttribute("novalnetDirectDebitAchtoken",
					sessionService.getAttribute("novalnetDirectDebitAchOneClickToken1"));
		}

		if ("2".equals(selection))
		{
			sessionService.setAttribute("novalnetDirectDebitAchtoken",
					sessionService.getAttribute("novalnetDirectDebitAchOneClickToken2"));
		}

		return true;
	}

	private boolean storeAchManualEntry(NovalnetPaymentDetailsForm paymentDetailsForm)
	{
		if (Boolean.TRUE.equals(paymentDetailsForm.isDirectDebitAchSaveData()))
		{
			sessionService.setAttribute(NOVALNET_DIRECT_DEBIT_ACH_STORE_PAYMENT_DATA, true);
		}

		if (!requireAndStore(NOVALNET_DIRECT_DEBIT_ACH_ACCOUNT_HOLDER, paymentDetailsForm.getAchAccountHolder(),
				NOVALNET_ACCOUNT_HOLDER_REQUIRED))
		{
			return false;
		}

		if (!requireAndStore(NOVALNET_DIRECT_DEBIT_ACH_ACCOUNT_NUMBER, paymentDetailsForm.getAchAccountNumber(),
				NOVALNET_ACCOUNT_NUMBER_REQUIRED))
		{
			return false;
		}

		return requireAndStore(NOVALNET_DIRECT_DEBIT_ACH_ROUTING_NUMBER, paymentDetailsForm.getAchRoutingNumber(),
				NOVALNET_ROUTING_NUMBER_REQUIRED);
	}

	private boolean handleGuaranteedSepa(NovalnetPaymentDetailsForm paymentDetailsForm, Model model, CartData cartData,
			AddressData deliveryAddress) throws CMSItemNotFoundException
	{
		PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(NOVALNET_GUARANTEED_DIRECT_DEBIT_SEPA);

		NovalnetGuaranteedDirectDebitSepaPaymentModeModel novalnetPaymentMethod = (NovalnetGuaranteedDirectDebitSepaPaymentModeModel) paymentModeModel;

		sessionService.setAttribute(NOVALNET_GUARANTEED_SEPA_STORE_PAYMENT_DATA, false);

		if (Boolean.TRUE.equals(novalnetCheckoutService.isGuestUser()) && !validateGuestGuaranteedSepaFields(paymentDetailsForm))
		{
			return false;
		}

		boolean oneClickEligible = Boolean.TRUE.equals(novalnetPaymentMethod.getNovalnetOneClickShopping())
				&& Boolean.FALSE.equals(novalnetCheckoutService.isGuestUser());

		if (oneClickEligible && !handleGuaranteedSepaOneClick(paymentDetailsForm))
		{
			return false;
		}

		sessionService.setAttribute(NOVALNET_GUARANTEED_SEPA_ACCOUNT_IBAN, paymentDetailsForm.getGuaranteeAccountIban().trim());

		sessionService.setAttribute(NOVALNET_GUARANTEED_SEPA_ACCOUNT_HOLDER, paymentDetailsForm.getGuaranteeAccountHolder().trim());

		sessionService.setAttribute(NOVALNET_GUARANTEED_SEPA_ACCOUNT_BIC, paymentDetailsForm.getGuaranteeAccountBic().trim());

		String novalnetDirectDebitSepaGuaranteeError = handleGuaranteeProcess(NOVALNET_GUARANTEED_DIRECT_DEBIT_SEPA,
				paymentDetailsForm.getNovalnetGuaranteedDirectDebitSepaDateOfBirth(), paymentDetailsForm, deliveryAddress);

		if (!"".equals(novalnetDirectDebitSepaGuaranteeError))
		{
			novalnetPaymentService.addPaymentProcess(model, paymentDetailsForm, cartData);
		}

		return true;
	}

	private boolean validateGuestGuaranteedSepaFields(NovalnetPaymentDetailsForm paymentDetailsForm)
	{
		if (!requireAndStore(NOVALNET_GUARANTEED_SEPA_ACCOUNT_IBAN, paymentDetailsForm.getGuaranteeAccountIban(),
				NOVALNET_IBAN_REQUIRED))
		{
			return false;
		}

		if (!requireAndStore(NOVALNET_GUARANTEED_SEPA_ACCOUNT_HOLDER, paymentDetailsForm.getGuaranteeAccountHolder(),
				NOVALNET_ACCOUNT_HOLDER_REQUIRED))
		{
			return false;
		}

		storeIfPresent(NOVALNET_GUARANTEED_SEPA_ACCOUNT_BIC, paymentDetailsForm.getGuaranteeAccountBic());

		return true;
	}

	private boolean handleGuaranteedSepaOneClick(NovalnetPaymentDetailsForm paymentDetailsForm)
	{
		String oneClickData = paymentDetailsForm != null ? paymentDetailsForm.getGuaranteedDirectDebitSepaOneClickData1() : null;

		if (oneClickData == null)
		{
			oneClickData = "";
		}

		if (paymentDetailsForm == null)
		{
			LOGGER.info("paymentDetailsForm was null");
		}

		if ("".equals(oneClickData))
		{
			return storeGuaranteedSepaManualEntry(paymentDetailsForm);
		}

		String selection = paymentDetailsForm.getGuaranteedDirectDebitSepaOneClickData1().trim();

		if ("3".equals(selection))
		{
			return storeGuaranteedSepaManualEntry(paymentDetailsForm);
		}

		if ("1".equals(selection))
		{
			sessionService.setAttribute(NOVALNET_DIRECT_DEBIT_SEPA_TOKEN,
					sessionService.getAttribute("novalnetDirectDebitSepaOneClickToken1"));
		}

		if ("2".equals(selection))
		{
			sessionService.setAttribute(NOVALNET_DIRECT_DEBIT_SEPA_TOKEN,
					sessionService.getAttribute("novalnetDirectDebitSepaOneClickToken2"));
		}

		return true;
	}

	private boolean storeGuaranteedSepaManualEntry(NovalnetPaymentDetailsForm paymentDetailsForm)
	{
		if (Boolean.TRUE.equals(paymentDetailsForm.isGuaranteedDirectDebitSepaSaveData()))
		{
			sessionService.setAttribute(NOVALNET_GUARANTEED_SEPA_STORE_PAYMENT_DATA, true);
		}

		if (!requireAndStore(NOVALNET_GUARANTEED_SEPA_ACCOUNT_IBAN, paymentDetailsForm.getGuaranteeAccountIban(),
				NOVALNET_IBAN_REQUIRED))
		{
			return false;
		}

		if (!requireAndStore(NOVALNET_GUARANTEED_SEPA_ACCOUNT_HOLDER, paymentDetailsForm.getGuaranteeAccountHolder(),
				NOVALNET_ACCOUNT_HOLDER_REQUIRED))
		{
			return false;
		}

		storeIfPresent(NOVALNET_GUARANTEED_SEPA_ACCOUNT_BIC, paymentDetailsForm.getGuaranteeAccountBic());

		return true;
	}

	private boolean handleGuaranteedInvoice(NovalnetPaymentDetailsForm paymentDetailsForm, Model model, CartData cartData,
			AddressData deliveryAddress) throws CMSItemNotFoundException
	{
		String novalnetGuaranteedInvoiceGuaranteeError = handleGuaranteeProcess("novalnetGuaranteedInvoice",
				paymentDetailsForm.getNovalnetGuaranteedInvoiceDateOfBirth(), paymentDetailsForm, deliveryAddress);

		if (!"".equals(novalnetGuaranteedInvoiceGuaranteeError))
		{
			novalnetPaymentService.addPaymentProcess(model, paymentDetailsForm, cartData);
		}

		return true;
	}

	private boolean handleCreditCard(NovalnetPaymentDetailsForm paymentDetailsForm, Model model, CartData cartData)
			throws CMSItemNotFoundException
	{
		PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(NOVALNET_CREDIT_CARD);

		NovalnetCreditCardPaymentModeModel novalnetPaymentMethod = (NovalnetCreditCardPaymentModeModel) paymentModeModel;

		sessionService.setAttribute("novalnetCreditCardPanHash", paymentDetailsForm.getNovalnetCreditCardPanHash().trim());

		sessionService.setAttribute("novalnetCreditCardUniqueId", paymentDetailsForm.getNovalnetCreditCardUniqueId().trim());

		sessionService.setAttribute("do_redirect", paymentDetailsForm.getDo_redirect().trim());

		if ("".equals(paymentDetailsForm.getNovalnetCreditCardPanHash().trim())
				&& Boolean.FALSE.equals(novalnetPaymentMethod.getNovalnetOneClickShopping()))
		{
			addPaymentProcess(model, paymentDetailsForm, cartData);
		}

		sessionService.setAttribute(NOVALNET_CREDIT_CARD_STORE_PAYMENT_DATA, false);

		boolean oneClickEligible = Boolean.TRUE.equals(novalnetPaymentMethod.getNovalnetOneClickShopping())
				&& Boolean.FALSE.equals(novalnetCheckoutService.isGuestUser());

		if (oneClickEligible)
		{
			String oneClickData = paymentDetailsForm != null ? paymentDetailsForm.getCreditCardOneClickData1() : null;

			if (oneClickData == null)
			{
				oneClickData = "";
			}

			if (paymentDetailsForm == null)
			{
				LOGGER.info("paymentDetailsForm was null");
			}

			if (!"".equals(oneClickData))
			{
				String selection = paymentDetailsForm.getCreditCardOneClickData1().trim();

				if ("3".equals(selection) && Boolean.TRUE.equals(paymentDetailsForm.isCreditcardSaveData()))
				{
					sessionService.setAttribute(NOVALNET_CREDIT_CARD_STORE_PAYMENT_DATA, true);
				}

				if ("1".equals(selection))
				{
					sessionService.setAttribute("novalnetCreditCardtoken",
							sessionService.getAttribute("novalnetCreditCardOneClickToken1"));
				}

				if ("2".equals(selection))
				{
					sessionService.setAttribute("novalnetCreditCardtoken",
							sessionService.getAttribute("novalnetCreditCardOneClickToken2"));
				}
			}
			else if (Boolean.TRUE.equals(paymentDetailsForm.isCreditcardSaveData()))
			{
				sessionService.setAttribute(NOVALNET_CREDIT_CARD_STORE_PAYMENT_DATA, true);
			}
		}

		return true;
	}

	@Override
	public boolean processTransaction(Map<String, String> resultMap)
	{
		Transaction transaction = new Transaction();
		Custom custom = new Custom();

		transaction.setTid(resultMap.get("tid"));
		custom.setLang(resolveLanguageCode());

		NovalnetPaymentRequest novalnetRequest = new NovalnetPaymentRequest();

		novalnetRequest.setTransaction(transaction);
		novalnetRequest.setCustom(custom);

		String jsonString = serializeOrSetError(novalnetRequest, "Error while converting Novalnet request to JSON");

		if (jsonString == null)
		{
			return false;
		}

		StringBuilder response = novalnetApiService.sendRequest(novalnetEndpointConfigService.getTransactionDetailsUrl(),
				jsonString);

		LOGGER.info("response: " + response);

		JsonNode rootNode = parseResponseOrSetError(response);

		if (rootNode == null)
		{
			return false;
		}

		JsonNode resultJsonObject = rootNode.path("result");
		JsonNode customerJsonObject = rootNode.path("customer");
		JsonNode transactionJsonObject = rootNode.path(TRANSACTION);

		String currentPayment = sessionService.getAttribute("selectedPaymentMethodId");

		String customerNo = customerJsonObject.path("customer_no").asText();

		JSONObject responseJson = new JSONObject(response.toString());

		if (responseJson.has(TRANSACTION))
		{
			JSONObject transactionJson = responseJson.getJSONObject(TRANSACTION);

			if (transactionJson.has(PAYMENT_DATA))
			{
				JSONObject paymentData = transactionJson.getJSONObject(PAYMENT_DATA);

				if (paymentData.has("token") && !novalnetCheckoutService.isGuestUser())
				{
					Boolean storePaymentData = sessionService.getAttribute(currentPayment + "StorePaymentData");

					LOGGER.info("Store payment data for {}: {}", currentPayment, storePaymentData);

					if (Boolean.TRUE.equals(storePaymentData))
					{
						LOGGER.info("Storing wallet payment token for: " + currentPayment);

						novalnetPaymentService.handleReferenceTransactionInfo(response, customerNo, currentPayment);
					}
				}
			}
		}
		String[] successStatus =
		{ "CONFIRMED", "ON_HOLD", "PENDING" };

		if (Arrays.asList(successStatus).contains(transactionJsonObject.path("status").asText()))
		{
			return handleSuccessfulTransaction(resultMap, custom, currentPayment, response, transactionJsonObject,
					customerJsonObject, jsonString);
		}

		return handleFailedTransaction(resultMap, resultJsonObject);
	}

	private String resolveLanguageCode()
	{
		Locale language = JaloSession.getCurrentSession().getSessionContext().getLocale();

		return language != null ? language.toString().toUpperCase() : "EN";
	}

	private String serializeOrSetError(NovalnetPaymentRequest request, String errorLogMessage)
	{
		try
		{
			return MAPPER.writeValueAsString(request);
		}
		catch (JsonProcessingException e)
		{
			LOGGER.error(errorLogMessage, e);

			sessionService.setAttribute(NOVALNET_CHECKOUT_ERROR, Localization.getLocalizedString(NOVALNET_UNKNOWN_ERROR));

			return null;
		}
	}

	private JsonNode parseResponseOrSetError(StringBuilder response)
	{
		try
		{
			return MAPPER.readTree(response.toString());
		}
		catch (IOException e)
		{
			LOGGER.error("Error while parsing Novalnet response", e);

			sessionService.setAttribute(NOVALNET_CHECKOUT_ERROR, Localization.getLocalizedString(NOVALNET_UNKNOWN_ERROR));

			return null;
		}
	}

	private boolean handleSuccessfulTransaction(Map<String, String> resultMap, Custom custom, String currentPayment,
			StringBuilder response, JsonNode transactionJsonObject, JsonNode customerJsonObject, String initialJsonString)
	{
		String orderComments = buildOrderComments(currentPayment, transactionJsonObject);

		AddressData addressData = sessionService.getAttribute("novalnetAddressData");

		int orderAmountCentValue = transactionJsonObject.path("amount").asInt();

		String transactionEmail = customerJsonObject.path("email").asText();

		OrderData orderData;

		String bankDetails = "";

		try
		{
			orderData = novalnetCheckoutService.saveOrderData(orderComments, currentPayment,
					transactionJsonObject.path("status").asText(), orderAmountCentValue,
					transactionJsonObject.path("currency").asText(), transactionJsonObject.path("tid").asText(), transactionEmail,
					addressData, bankDetails);
		}
		catch (InvalidCartException e)
		{
			sessionService.setAttribute(NOVALNET_CHECKOUT_ERROR, Localization.getLocalizedString(NOVALNET_UNKNOWN_ERROR));

			return false;
		}

		updateTransactionOrder(resultMap, custom, orderData, initialJsonString);

		handleStorePayment(currentPayment, response, customerJsonObject);

		sessionService.setAttribute("tid", orderComments);
		sessionService.setAttribute("email", transactionEmail);
		sessionService.setAttribute("novalnetOrderData", orderData);

		return true;
	}

	private String buildOrderComments(String currentPayment, JsonNode transactionJsonObject)
	{
		String paymentName = novalnetPaymentService.getPaymentName(currentPayment);

		String testMode = "";

		if ("1".equals(transactionJsonObject.path("test_mode").asText()))
		{
			testMode = " " + Localization.getLocalizedString("novalnet.testOrderText");
		}

		String orderComments = Localization.getLocalizedString("novalnet.paymentname") + ": " + paymentName + "<br>";

		orderComments += Localization.getLocalizedString("novalnet.transactionId") + " : "
				+ transactionJsonObject.path("tid").asText() + "<br>" + testMode;

		Boolean isZeroAmountBooking = sessionService.getAttribute(NOVALNET_ZERO_AMOUNT_BOOKING);

		if (Boolean.TRUE.equals(isZeroAmountBooking))
		{
			orderComments += "<br>" + Localization.getLocalizedString("novalnet.zeroAmountBooking");
		}

		sessionService.removeAttribute(NOVALNET_ZERO_AMOUNT_BOOKING);

		return orderComments;
	}

	private void updateTransactionOrder(Map<String, String> resultMap, Custom custom, OrderData orderData, String fallbackJson)
	{
		Transaction updateTransaction = new Transaction();

		updateTransaction.setTid(resultMap.get("tid"));
		updateTransaction.setOrder_no(orderData.getCode());

		NovalnetPaymentRequest updateRequest = new NovalnetPaymentRequest();

		updateRequest.setTransaction(updateTransaction);
		updateRequest.setCustom(custom);

		String jsonString = fallbackJson;

		try
		{
			jsonString = MAPPER.writeValueAsString(updateRequest);
		}
		catch (JsonProcessingException e)
		{
			LOGGER.error("Error while converting Novalnet update request to JSON", e);
		}

		StringBuilder responseString = novalnetApiService.sendRequest(novalnetEndpointConfigService.getTransactionUpdateUrl(),
				jsonString);

		LOGGER.info("Novalnet response received: " + responseString);
	}

	private boolean handleFailedTransaction(Map<String, String> resultMap, JsonNode resultJsonObject)
	{
		sessionService.setAttribute("novalnetOrderCurrency", null);

		sessionService.setAttribute("novalnetOrderAmount", null);

		sessionService.setAttribute("novalnetCustomerParams", null);

		sessionService.setAttribute("novalnetRedirectPaymentTestModeValue", null);

		sessionService.setAttribute("novalnetRedirectPaymentName", null);

		sessionService.setAttribute("novalnetCreditCardPanHash", null);

		sessionService.setAttribute("paymentAccessKey", null);

		String statusMessage = !resultJsonObject.path("status_text").isMissingNode() ? resultJsonObject.path("status_text").asText()
				: resultMap.get("status_desc");

		sessionService.setAttribute(NOVALNET_CHECKOUT_ERROR, statusMessage);

		return false;
	}

	@Override
	public void handleStorePayment(String currentPayment, StringBuilder response, JsonNode customerJsonObject)
	{
		String[] walletPayments =
		{ NOVALNET_GOOGLE_PAY, NOVALNET_APPLE_PAY };

		if (NOVALNET_CREDIT_CARD.equals(currentPayment) && !novalnetCheckoutService.isGuestUser())
		{
			boolean novalnetCreditCardStorePaymentData = sessionService.getAttribute(NOVALNET_CREDIT_CARD_STORE_PAYMENT_DATA);

			if (novalnetCreditCardStorePaymentData)
			{
				novalnetPaymentService.handleReferenceTransactionInfo(response, customerJsonObject.path("customer_no").asText(),
						NOVALNET_CREDIT_CARD);
			}
		}
		else if (Arrays.asList(walletPayments).contains(currentPayment))
		{
			JSONObject responseJson = new JSONObject(response.toString());

			if (responseJson.has(TRANSACTION))
			{
				JSONObject transaction = responseJson.getJSONObject(TRANSACTION);

				if (transaction.has("payment_data"))
				{
					JSONObject paymentData = transaction.getJSONObject("payment_data");

					if (paymentData.has("token") && !novalnetCheckoutService.isGuestUser())
					{
						Boolean storePaymentData = sessionService.getAttribute(currentPayment + "StorePaymentData");

						if (Boolean.TRUE.equals(storePaymentData))
						{
							novalnetPaymentService.handleReferenceTransactionInfo(response,
									customerJsonObject.path("customer_no").asText(), currentPayment);
						}
					}
				}
			}
		}
	}

	@Override
	public StringBuilder createTransaction(HttpServletRequest request, BaseStoreModel baseStore, String currentPayment,
			String customerNo, Integer orderAmountCent, CartData cartData)
	{
		HostedPage hostedPage = new HostedPage();
		Merchant merchant = new Merchant();
		Transaction transaction = new Transaction();
		PaymentData paymentData = new PaymentData();
		Custom custom = new Custom();

		NovalnetPaymentRequest paymentRequest = sessionService.getAttribute("novalnetPaymentRequest");

		if (paymentRequest == null || paymentRequest.getCustomer() == null)
		{
			throw new IllegalStateException("Customer data not found");
		}

		Customer customer = paymentRequest.getCustomer();

		Integer tariff = baseStore.getNovalnetTariffId();

		String apiKey = baseStore.getNovalnetAPIKey();

		String hybrisVersion = Config.getString("build.version", "unknown");

		String currency = cartData.getTotalPriceWithTax().getCurrencyIso();

		PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(currentPayment);

		Integer sessionOrderAmountCent = sessionService.getAttribute("novalnetOrderAmount");

		merchant.setSignature(apiKey);
		merchant.setTariff(String.valueOf(tariff));

		populateTransactionCustomer(customer, paymentRequest, request, customerNo);

		populateTransactionBase(transaction, currentPayment, currency, sessionOrderAmountCent, hybrisVersion);

		custom.setLang(resolveLanguageCode());

		String paymentName = novalnetPaymentService.getPaymentName(currentPayment);

		custom.setInput1("paymentName");
		custom.setInputval1(paymentName);

		PaymentConfigResult configResult = novalnetPaymentHandlerService.handlePayment(currentPayment, paymentModeModel,
				transaction, paymentData, customer, sessionOrderAmountCent, request, hostedPage);

		boolean verifyPaymentData = configResult.isVerifyPaymentData();

		transaction.setTest_mode(String.valueOf(configResult.getTestMode()));

		applyRedirectSettings(transaction, paymentData, request, currentPayment, configResult.isRedirect());

		applyZeroAmountBookingFlag(transaction, configResult.isZeroAmountBooking());

		Map<String, Object> dataParameters = new HashMap<>();

		dataParameters.put("merchant", merchant);
		dataParameters.put("customer", customer);
		dataParameters.put(TRANSACTION, transaction);
		dataParameters.put("custom", custom);
		dataParameters.put("hosted_page", hostedPage);

		Gson gson = new GsonBuilder().create();

		String jsonString = gson.toJson(dataParameters);

		LOGGER.info("verify_payment_data = " + verifyPaymentData);

		String url = resolveTransactionUrl(currentPayment, verifyPaymentData);

		StringBuilder response = novalnetApiService.sendRequest(url, jsonString);

		LOGGER.info("Novalnet API Response : " + response);

		return response;
	}

	private void populateTransactionCustomer(Customer customer, NovalnetPaymentRequest paymentRequest, HttpServletRequest request,
			String customerNo)
	{
		customer.setFirst_name(paymentRequest.getCustomer().getFirst_name());

		customer.setLast_name(paymentRequest.getCustomer().getLast_name());

		customer.setCustomer_ip(NovalnetUtils.getRemoteIpAddr(request));

		customer.setCustomer_no(customerNo);
		customer.setGender("u");
	}

	private void populateTransactionBase(Transaction transaction, String currentPayment, String currency, Integer orderAmountCent,
			String hybrisVersion)
	{
		transaction.setPayment_type(NovalnetUtils.getPaymentType(currentPayment));

		transaction.setCurrency(currency);
		transaction.setAmount(orderAmountCent.longValue());

		transaction.setSystem_name("SAP Commerce Cloud");

		transaction.setSystem_version(hybrisVersion + "-NN" + NOVALNET_VERSION);
	}

	private void applyRedirectSettings(Transaction transaction, PaymentData paymentData, HttpServletRequest request,
			String currentPayment, boolean redirect)
	{
		if (Boolean.TRUE.equals(redirect))
		{
			String currentUrl = request.getRequestURL().toString();

			String[] walletPayments =
			{ NOVALNET_GOOGLE_PAY };

			String returnUrl = currentUrl
					.replace(!Arrays.asList(walletPayments).contains(currentPayment) ? "novalnet/summary/placeOrder"
							: "novalnet/summary/bookWalletTransaction", "novalnet/hop-response");

			transaction.setReturn_url(returnUrl);
			transaction.setError_return_url(returnUrl);
		}

		transaction.setPayment_data(paymentData);
	}

	private void applyZeroAmountBookingFlag(Transaction transaction, boolean zeroAmountBooking)
	{
		if (Boolean.TRUE.equals(zeroAmountBooking))
		{
			LOGGER.info("Zero amount booking is enabled.");

			transaction.setAmount(0L);

			sessionService.setAttribute(NOVALNET_ZERO_AMOUNT_BOOKING, Boolean.TRUE);
		}
		else
		{
			sessionService.setAttribute(NOVALNET_ZERO_AMOUNT_BOOKING, Boolean.FALSE);
		}
	}

	private String resolveTransactionUrl(String currentPayment, boolean verifyPaymentData)
	{
		if (NOVALNET_APPLE_PAY.equals(currentPayment))
		{
			return verifyPaymentData ? novalnetEndpointConfigService.getAuthorizeHostedUrl()
					: novalnetEndpointConfigService.getPaymentHostedUrl();
		}

		return verifyPaymentData ? novalnetEndpointConfigService.getAuthorizeUrl() : novalnetEndpointConfigService.getPaymentUrl();
	}

	@Override
	public String bookWalletTransaction(HttpServletRequest request, BaseStoreModel baseStore, String customerNo,
			Integer orderAmountCent, CartData cartData) throws Exception
	{
		String currentPayment = NOVALNET_GOOGLE_PAY;

		StringBuilder response = createTransaction(request, baseStore, currentPayment, customerNo, orderAmountCent, cartData);

		LOGGER.info("Novalnet Response: {}", response);

		JSONObject tomJsonObject = new JSONObject(response.toString());

		JSONObject resultJsonObject = tomJsonObject.getJSONObject("result");

		if (resultJsonObject.has("redirect_url"))
		{
			sessionService.setAttribute("txn_check", baseStore.getNovalnetPaymentAccessKey().trim());
		}

		if (tomJsonObject.has(TRANSACTION))
		{
			JSONObject transactionJsonObject = tomJsonObject.getJSONObject(TRANSACTION);

			if (transactionJsonObject.has(TXN_SECRET))
			{
				sessionService.setAttribute(TXN_SECRET, transactionJsonObject.get(TXN_SECRET).toString());
			}

			if (transactionJsonObject.has("tid"))
			{
				sessionService.setAttribute("wallet_tid", transactionJsonObject.get("tid").toString());
			}
		}

		return response.toString();
	}

	public String handleGuaranteeProcess(String paymentName, String dob, NovalnetPaymentDetailsForm paymentDetailsForm,
			AddressData deliveryAddress)
	{
		if (paymentDetailsForm == null)
		{
			throw new IllegalArgumentException("Payment details are required.");
		}

		if (deliveryAddress == null)
		{
			throw new IllegalArgumentException("Delivery address is required.");
		}

		if (Boolean.FALSE.equals(paymentDetailsForm.isUseDeliveryAddress())
				&& (!paymentDetailsForm.getBillTo_street1().equals(deliveryAddress.getLine1())
						|| !paymentDetailsForm.getBillTo_street2().equals(deliveryAddress.getLine2())
						|| !paymentDetailsForm.getBillTo_postalCode().equals(deliveryAddress.getPostalCode())
						|| !paymentDetailsForm.getBillTo_city().equals(deliveryAddress.getTown())
						|| !paymentDetailsForm.getBillTo_country().equals(deliveryAddress.getCountry().getIsocode())))
		{
			return "novalnet.address.error";
		}

		String Guaranteerror = sessionService.getAttribute(paymentName + "GuaranteeError");

		if (Guaranteerror != null)
		{
			return Guaranteerror;
		}

		if (!"".equals(dob))
		{
			boolean isValidDob = NovalnetUtils.hasAgeRequirement(dob);

			if (Boolean.FALSE.equals(isValidDob))
			{
				return "novalnet.age.error";
			}
			else if (Boolean.TRUE.equals(isValidDob))
			{
				sessionService.setAttribute(paymentName + "DateOfBirth", dob.trim());

				sessionService.setAttribute(paymentName + "PaymentGuarantee", true);
			}
		}
		else
		{
			return "novalnet.dob.error";
		}

		return "";
	}
}