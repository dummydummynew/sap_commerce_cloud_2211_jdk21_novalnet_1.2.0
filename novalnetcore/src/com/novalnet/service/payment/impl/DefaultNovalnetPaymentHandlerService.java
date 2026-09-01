/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.payment.impl;

import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.servicelayer.session.SessionService;

import java.util.Arrays;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import com.novalnet.dto.PaymentConfigResult;
import com.novalnet.dto.payment.request.Customer;
import com.novalnet.dto.payment.request.HostedPage;
import com.novalnet.dto.payment.request.PaymentData;
import com.novalnet.dto.payment.request.Transaction;
import com.novalnet.model.NovalnetAliPayPaymentModeModel;
import com.novalnet.model.NovalnetApplePayPaymentModeModel;
import com.novalnet.model.NovalnetBancontactPaymentModeModel;
import com.novalnet.model.NovalnetBlikPaymentModeModel;
import com.novalnet.model.NovalnetCreditCardPaymentModeModel;
import com.novalnet.model.NovalnetDirectDebitAchPaymentModeModel;
import com.novalnet.model.NovalnetDirectDebitSepaPaymentModeModel;
import com.novalnet.model.NovalnetEpsPaymentModeModel;
import com.novalnet.model.NovalnetGooglePayPaymentModeModel;
import com.novalnet.model.NovalnetGuaranteedDirectDebitSepaPaymentModeModel;
import com.novalnet.model.NovalnetGuaranteedInvoicePaymentModeModel;
import com.novalnet.model.NovalnetIdealPaymentModeModel;
import com.novalnet.model.NovalnetInvoicePaymentModeModel;
import com.novalnet.model.NovalnetMbWayPaymentModeModel;
import com.novalnet.model.NovalnetMultibancoPaymentModeModel;
import com.novalnet.model.NovalnetOnlineBankTransferPaymentModeModel;
import com.novalnet.model.NovalnetPayPalPaymentModeModel;
import com.novalnet.model.NovalnetPostFinanceCardPaymentModeModel;
import com.novalnet.model.NovalnetPostFinancePaymentModeModel;
import com.novalnet.model.NovalnetPrepaymentPaymentModeModel;
import com.novalnet.model.NovalnetPrzelewy24PaymentModeModel;
import com.novalnet.model.NovalnetTrustlyPaymentModeModel;
import com.novalnet.model.NovalnetTwintPaymentModeModel;
import com.novalnet.model.NovalnetWechatPayPaymentModeModel;
import com.novalnet.service.checkout.NovalnetCheckoutService;
import com.novalnet.service.payment.NovalnetPaymentHandlerService;
import com.novalnet.util.NovalnetUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;


@Service("novalnetPaymentHandlerService")
public class DefaultNovalnetPaymentHandlerService implements NovalnetPaymentHandlerService
{
	private static final Log LOGGER = LogFactory.getLog(DefaultNovalnetPaymentHandlerService.class);

	private static final String PAYMENT_AUTHORIZE = "AUTHORIZE";
	private static final String AUTHORIZE_WITH_ZERO_AMOUNT = "AUTHORIZE_WITH_ZERO_AMOUNT";

	public static final int PREPAYMENT_FROM_DATE = 7;
	public static final int PREPAYMENT_TILL_DATE = 28;

	private static final String ONHOLD_ACTION_LOG = "Onhold Action : ";
	private static final String SESSION_ATTR_CREDIT_CARD_TOKEN = "novalnetCreditCardtoken";
	private static final String ONHOLD_ORDER_AMOUNT_NULL_LOG = "onhold order amount is null";
	private static final String SESSION_ATTR_DIRECT_DEBIT_ACH_TOKEN = "novalnetDirectDebitAchtoken";
	private static final String SESSION_ATTR_DIRECT_DEBIT_SEPA_TOKEN = "novalnetDirectDebitSepatoken";
	private static final String SESSION_ATTR_GUARANTEED_SEPA_BIC = "novalnetGuaranteedDirectDebitSepaAccountBic";

	@Resource
	private SessionService sessionService;

	@Resource
	private NovalnetCheckoutService novalnetCheckoutService;

	@Override
	public PaymentConfigResult handlePayment(String currentPayment, PaymentModeModel paymentModeModel, Transaction transaction,
			PaymentData paymentData, Customer customer, Integer orderAmountCent, HttpServletRequest request, HostedPage hostedPage)
	{
		PaymentConfigResult result = new PaymentConfigResult();

		switch (currentPayment)
		{
			case "novalnetDirectDebitSepa":
				configureSepa((NovalnetDirectDebitSepaPaymentModeModel) paymentModeModel, transaction, paymentData, orderAmountCent,
						result);
				break;

			case "novalnetDirectDebitAch":
				configureAch((NovalnetDirectDebitAchPaymentModeModel) paymentModeModel, transaction, paymentData, result);
				break;

			case "novalnetGuaranteedDirectDebitSepa":
				configureGuaranteedSepa((NovalnetGuaranteedDirectDebitSepaPaymentModeModel) paymentModeModel, transaction,
						paymentData, customer, orderAmountCent, result);
				break;

			case "novalnetPayPal":
				configurePayPal((NovalnetPayPalPaymentModeModel) paymentModeModel, orderAmountCent, result);
				break;

			case "novalnetCreditCard":
				configureCreditCard((NovalnetCreditCardPaymentModeModel) paymentModeModel, transaction, paymentData, orderAmountCent,
						result);
				break;

			case "novalnetInvoice":
				configureInvoice((NovalnetInvoicePaymentModeModel) paymentModeModel, transaction, orderAmountCent, result);
				break;

			case "novalnetPrepayment":
				configurePrepayment((NovalnetPrepaymentPaymentModeModel) paymentModeModel, transaction, result);
				break;

			case "novalnetMultibanco":
				configureMultibanco((NovalnetMultibancoPaymentModeModel) paymentModeModel, result);
				break;

			case "novalnetTwint":
				configureTwint((NovalnetTwintPaymentModeModel) paymentModeModel, result);
				break;

			case "novalnetMbWay":
				configureMbWay((NovalnetMbWayPaymentModeModel) paymentModeModel, result);
				break;

			case "novalnetTrustly":
				configureTrustly((NovalnetTrustlyPaymentModeModel) paymentModeModel, result);
				break;

			case "novalnetBlik":
				configureBlik((NovalnetBlikPaymentModeModel) paymentModeModel, result);
				break;

			case "novalnetWechatPay":
				configureWechatPay((NovalnetWechatPayPaymentModeModel) paymentModeModel, result);
				break;

			case "novalnetAlipay":
				configureAlipay((NovalnetAliPayPaymentModeModel) paymentModeModel, result);
				break;

			case "novalnetGuaranteedInvoice":
				configureGuaranteedInvoice((NovalnetGuaranteedInvoicePaymentModeModel) paymentModeModel, customer, orderAmountCent,
						result);
				break;

			case "novalnetOnlineBankTransfer":
				configureOnlineBankTransfer((NovalnetOnlineBankTransferPaymentModeModel) paymentModeModel, result);
				break;

			case "novalnetBancontact":
				configureBancontact((NovalnetBancontactPaymentModeModel) paymentModeModel, result);
				break;

			case "novalnetIdeal":
				configureIdeal((NovalnetIdealPaymentModeModel) paymentModeModel, result);
				break;

			case "novalnetGooglePay":
				configureGooglePay((NovalnetGooglePayPaymentModeModel) paymentModeModel, request, paymentData, transaction,
						orderAmountCent, result);
				break;

			case "novalnetApplePay":
				configureApplePay((NovalnetApplePayPaymentModeModel) paymentModeModel, hostedPage, orderAmountCent, result);
				break;

			case "novalnetEps":
				configureEps((NovalnetEpsPaymentModeModel) paymentModeModel, result);
				break;

			case "novalnetPostFinance":
				configurePostFinance((NovalnetPostFinancePaymentModeModel) paymentModeModel, result);
				break;

			case "novalnetPostFinanceCard":
				configurePostFinanceCard((NovalnetPostFinanceCardPaymentModeModel) paymentModeModel, result);
				break;

			case "novalnetPrzelewy24":
				configurePrzelewy24((NovalnetPrzelewy24PaymentModeModel) paymentModeModel, result);
				break;

			default:
				LOGGER.warn("Unsupported payment type: " + currentPayment);
				break;
		}

		return result;
	}


	private Integer resolveOnholdOrderAmount(boolean paymentMethodPresent, Supplier<Integer> amountSupplier, boolean useErrorLevel)
	{
		if (paymentMethodPresent)
		{
			Integer onholdOrderAmount = amountSupplier.get();
			return onholdOrderAmount == null ? 0 : onholdOrderAmount;
		}

		if (useErrorLevel)
		{
			LOGGER.error(ONHOLD_ORDER_AMOUNT_NULL_LOG);
		}
		else
		{
			LOGGER.info(ONHOLD_ORDER_AMOUNT_NULL_LOG);
		}

		return 0;
	}


	private String resolveToken(String sessionKey)
	{
		String token = sessionService.getAttribute(sessionKey);

		if (token != null)
		{
			return token;
		}

		LOGGER.info(sessionKey + " is null");
		return "";
	}

	private void applyVerifyPaymentData(String onholdActionType, Integer orderAmountCent, Integer onholdOrderAmount,
			PaymentConfigResult result)
	{
		if (PAYMENT_AUTHORIZE.equals(onholdActionType) && orderAmountCent >= onholdOrderAmount)
		{
			result.setVerifyPaymentData(true);
		}
	}

	private void applyZeroAmountBooking(String onholdActionType, PaymentConfigResult result)
	{
		if (AUTHORIZE_WITH_ZERO_AMOUNT.equals(onholdActionType))
		{
			LOGGER.info(ONHOLD_ACTION_LOG + onholdActionType);
			result.setZeroAmountBooking(true);
		}
	}

	private void applyOneClickTokenCreation(boolean eligible, Transaction transaction)
	{
		if (eligible)
		{
			transaction.setCreate_token("1");
		}
	}

	private void applyOneClickTokenUsage(boolean eligible, String token, PaymentData paymentData, String tokenSessionKey)
	{
		if (eligible)
		{
			paymentData.setToken(token);
			sessionService.setAttribute(tokenSessionKey, null);
		}
	}

	private void configureSepa(NovalnetDirectDebitSepaPaymentModeModel novalnetPaymentMethod, Transaction transaction,
			PaymentData paymentData, Integer orderAmountCent, PaymentConfigResult result)
	{
		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}

		Integer sepaDueDate = novalnetPaymentMethod.getNovalnetDueDate();

		if (sepaDueDate != null && sepaDueDate >= 3 && sepaDueDate <= 14)
		{
			transaction.setDue_date(NovalnetUtils.formatDate(sepaDueDate));
		}

		Integer onholdOrderAmount = resolveOnholdOrderAmount(novalnetPaymentMethod != null,
				() -> novalnetPaymentMethod.getNovalnetOnholdAmount(), true);

		applyVerifyPaymentData(novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount().toString(), orderAmountCent,
				onholdOrderAmount, result);

		applyZeroAmountBooking(novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount().toString(), result);

		boolean novalnetDirectDebitSepaStorePaymentData = sessionService.getAttribute("novalnetDirectDebitSepaStorePaymentData");

		String token = resolveToken(SESSION_ATTR_DIRECT_DEBIT_SEPA_TOKEN);

		boolean isGuestUser = novalnetCheckoutService.isGuestUser();
		boolean oneClickShopping = novalnetPaymentMethod.getNovalnetOneClickShopping();

		applyOneClickTokenCreation(Boolean.FALSE.equals(isGuestUser) && Boolean.TRUE.equals(oneClickShopping)
				&& Boolean.TRUE.equals(novalnetDirectDebitSepaStorePaymentData), transaction);

		applyOneClickTokenUsage(Boolean.FALSE.equals(isGuestUser) && Boolean.TRUE.equals(oneClickShopping) && !"".equals(token),
				token, paymentData, SESSION_ATTR_DIRECT_DEBIT_SEPA_TOKEN);

		if ("".equals(token))
		{
			applySepaAccountDetails(paymentData);
		}
	}

	private void applySepaAccountDetails(PaymentData paymentData)
	{
		String accountHolder = (String) sessionService.getAttribute("novalnetDirectDebitSepaAccountHolder");

		paymentData.setIban((String) sessionService.getAttribute("novalnetDirectDebitSepaAccountIban"));

		String bic = (String) sessionService.getAttribute("novalnetDirectDebitSepaAccountBic");

		if (bic != null && !bic.isEmpty())
		{
			paymentData.setBic(bic);
			sessionService.setAttribute("novalnetDirectDebitSepaAccountBic", null);
		}

		if (accountHolder != null)
		{
			paymentData.setAccount_holder(accountHolder.replace("&", ""));
		}

		sessionService.setAttribute("novalnetDirectDebitSepaAccountIban", null);
		sessionService.setAttribute("novalnetDirectDebitSepaAccountHolder", null);
	}

	private void configureAch(NovalnetDirectDebitAchPaymentModeModel novalnetPaymentMethod, Transaction transaction,
			PaymentData paymentData, PaymentConfigResult result)
	{
		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}

		applyZeroAmountBooking(novalnetPaymentMethod.getOnholdActionTypeWithoutAuthorize().toString(), result);

		boolean novalnetDirectDebitAchStorePaymentData = sessionService.getAttribute("novalnetDirectDebitAchStorePaymentData");

		String token = resolveToken(SESSION_ATTR_DIRECT_DEBIT_ACH_TOKEN);

		boolean isGuestUser = novalnetCheckoutService.isGuestUser();
		boolean oneClickShopping = novalnetPaymentMethod.getNovalnetOneClickShopping();

		applyOneClickTokenCreation(Boolean.FALSE.equals(isGuestUser) && Boolean.TRUE.equals(oneClickShopping)
				&& Boolean.TRUE.equals(novalnetDirectDebitAchStorePaymentData), transaction);

		applyOneClickTokenUsage(Boolean.FALSE.equals(isGuestUser) && Boolean.TRUE.equals(oneClickShopping) && !"".equals(token),
				token, paymentData, SESSION_ATTR_DIRECT_DEBIT_ACH_TOKEN);

		if ("".equals(token))
		{
			applyAchAccountDetails(paymentData);
		}
	}

	private void applyAchAccountDetails(PaymentData paymentData)
	{
		String accountHolder = (String) sessionService.getAttribute("novalnetDirectDebitAchAccountHolder");

		paymentData.setAccount_number((String) sessionService.getAttribute("novalnetDirectDebitAchAchAccountNumber"));

		String routingNumber = (String) sessionService.getAttribute("novalnetDirectDebitAchRoutingNumber");

		if (routingNumber != null && !routingNumber.isEmpty())
		{
			paymentData.setRouting_number(routingNumber);
			sessionService.setAttribute("novalnetDirectDebitAchRoutingNumber", null);
		}

		paymentData.setAccount_holder(accountHolder.replace("&", ""));

		sessionService.setAttribute("novalnetDirectDebitAchAchAccountNumber", null);
		sessionService.setAttribute("novalnetDirectDebitAchAccountHolder", null);
	}

	private void configureGuaranteedSepa(NovalnetGuaranteedDirectDebitSepaPaymentModeModel novalnetPaymentMethod,
			Transaction transaction, PaymentData paymentData, Customer customer, Integer orderAmountCent, PaymentConfigResult result)
	{
		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}

		Integer onholdOrderAmount = resolveOnholdOrderAmount(novalnetPaymentMethod != null,
				() -> novalnetPaymentMethod.getNovalnetOnholdAmount(), true);

		boolean novalnetGuaranteedDirectDebitSepaStorePaymentData = sessionService
				.getAttribute("novalnetGuaranteedDirectDebitSepaStorePaymentData");

		String token = resolveToken(SESSION_ATTR_DIRECT_DEBIT_SEPA_TOKEN);

		boolean isGuestUser = novalnetCheckoutService.isGuestUser();
		boolean oneClickShopping = novalnetPaymentMethod.getNovalnetOneClickShopping();

		applyOneClickTokenCreation(Boolean.FALSE.equals(isGuestUser) && Boolean.TRUE.equals(oneClickShopping)
				&& Boolean.TRUE.equals(novalnetGuaranteedDirectDebitSepaStorePaymentData), transaction);

		applyOneClickTokenUsage(Boolean.FALSE.equals(isGuestUser) && Boolean.TRUE.equals(oneClickShopping) && !"".equals(token),
				token, paymentData, SESSION_ATTR_DIRECT_DEBIT_SEPA_TOKEN);

		if ("".equals(token))
		{
			applyGuaranteedSepaAccountDetails(paymentData);
		}

		String dob = sessionService.getAttribute("novalnetGuaranteedDirectDebitSepaDateOfBirth");
		customer.setBirth_date(dob);

		applyVerifyPaymentData(novalnetPaymentMethod.getNovalnetOnholdAction().toString(), orderAmountCent, onholdOrderAmount,
				result);
	}

	private void applyGuaranteedSepaAccountDetails(PaymentData paymentData)
	{
		String accountHolder = sessionService.getAttribute("novalnetGuaranteedDirectDebitSepaAccountHolder");

		paymentData.setIban(sessionService.getAttribute("novalnetGuaranteedDirectDebitSepaAccountIban").toString());

		if (sessionService.getAttribute(SESSION_ATTR_GUARANTEED_SEPA_BIC) != null
				&& !"".equals(sessionService.getAttribute(SESSION_ATTR_GUARANTEED_SEPA_BIC).toString()))
		{
			paymentData.setBic(sessionService.getAttribute(SESSION_ATTR_GUARANTEED_SEPA_BIC).toString());

			sessionService.setAttribute(SESSION_ATTR_GUARANTEED_SEPA_BIC, null);
		}

		paymentData.setAccount_holder(accountHolder.replace("&", ""));

		sessionService.setAttribute("novalnetGuaranteedDirectDebitSepaAccountIban", null);
		sessionService.setAttribute("novalnetGuaranteedDirectDebitSepaAccountHolder", null);
	}

	private void configurePayPal(NovalnetPayPalPaymentModeModel novalnetPaymentMethod, Integer orderAmountCent,
			PaymentConfigResult result)
	{
		result.setRedirect(true);

		Integer onholdOrderAmount = resolveOnholdOrderAmount(novalnetPaymentMethod != null,
				() -> novalnetPaymentMethod.getNovalnetOnholdAmount(), false);

		applyVerifyPaymentData(novalnetPaymentMethod.getNovalnetOnholdAction().toString(), orderAmountCent, onholdOrderAmount,
				result);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configureCreditCard(NovalnetCreditCardPaymentModeModel novalnetPaymentMethod, Transaction transaction,
			PaymentData paymentData, Integer orderAmountCent, PaymentConfigResult result)
	{
		Integer onholdOrderAmount = resolveOnholdOrderAmount(novalnetPaymentMethod != null,
				() -> novalnetPaymentMethod.getNovalnetOnholdAmount(), false);

		if (Boolean.TRUE.equals(novalnetPaymentMethod.getNovalnetEnforce3D()))
		{
			transaction.setEnforce_3d(1);
			LOGGER.info("Enforce 3D enabled for Credit Card");
		}

		applyVerifyPaymentData(novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount().toString(), orderAmountCent,
				onholdOrderAmount, result);

		applyZeroAmountBooking(novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount().toString(), result);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}

		String token = resolveToken(SESSION_ATTR_CREDIT_CARD_TOKEN);

		boolean novalnetCreditCardStorePaymentData = sessionService.getAttribute("novalnetCreditCardStorePaymentData");

		boolean isGuestUser = novalnetCheckoutService.isGuestUser();
		boolean oneClickShopping = novalnetPaymentMethod.getNovalnetOneClickShopping();

		applyOneClickTokenCreation(!isGuestUser && oneClickShopping && Boolean.TRUE.equals(novalnetCreditCardStorePaymentData),
				transaction);

		if (!isGuestUser && oneClickShopping && !"".equals(token))
		{
			paymentData.setToken(token);
			sessionService.setAttribute(SESSION_ATTR_CREDIT_CARD_TOKEN, null);
		}
		else
		{
			paymentData.setPan_hash((String) sessionService.getAttribute("novalnetCreditCardPanHash"));

			paymentData.setUnique_id((String) sessionService.getAttribute("novalnetCreditCardUniqueId"));

			String do_redirect = sessionService.getAttribute("do_redirect");

			if (!"".equals(do_redirect))
			{
				result.setRedirect(true);
			}

			sessionService.setAttribute("novalnetCreditCardPanHash", null);
		}
	}

	private void configureInvoice(NovalnetInvoicePaymentModeModel novalnetPaymentMethod, Transaction transaction,
			Integer orderAmountCent, PaymentConfigResult result)
	{
		Integer invoiceDueDate = novalnetPaymentMethod.getNovalnetDueDate();

		if (invoiceDueDate != null && invoiceDueDate > 7)
		{
			transaction.setDue_date(NovalnetUtils.formatDate(invoiceDueDate));
		}

		Integer onholdOrderAmount = resolveOnholdOrderAmount(novalnetPaymentMethod != null,
				() -> novalnetPaymentMethod.getNovalnetOnholdAmount(), false);

		applyVerifyPaymentData(novalnetPaymentMethod.getNovalnetOnholdAction().toString(), orderAmountCent, onholdOrderAmount,
				result);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configurePrepayment(NovalnetPrepaymentPaymentModeModel novalnetPaymentMethod, Transaction transaction,
			PaymentConfigResult result)
	{
		Integer prepaymentDueDate = novalnetPaymentMethod.getNovalnetDueDate();

		if (prepaymentDueDate != null && PREPAYMENT_FROM_DATE >= 7 && prepaymentDueDate <= PREPAYMENT_TILL_DATE)
		{
			transaction.setDue_date(NovalnetUtils.formatDate(prepaymentDueDate));
		}

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configureMultibanco(NovalnetMultibancoPaymentModeModel novalnetPaymentMethod, PaymentConfigResult result)
	{
		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configureTwint(NovalnetTwintPaymentModeModel novalnetPaymentMethod, PaymentConfigResult result)
	{
		result.setRedirect(true);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configureMbWay(NovalnetMbWayPaymentModeModel novalnetPaymentMethod, PaymentConfigResult result)
	{
		result.setRedirect(true);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configureTrustly(NovalnetTrustlyPaymentModeModel novalnetPaymentMethod, PaymentConfigResult result)
	{
		result.setRedirect(true);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configureBlik(NovalnetBlikPaymentModeModel novalnetPaymentMethod, PaymentConfigResult result)
	{
		result.setRedirect(true);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configureWechatPay(NovalnetWechatPayPaymentModeModel novalnetPaymentMethod, PaymentConfigResult result)
	{
		result.setRedirect(true);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configureAlipay(NovalnetAliPayPaymentModeModel novalnetPaymentMethod, PaymentConfigResult result)
	{
		result.setRedirect(true);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configureGuaranteedInvoice(NovalnetGuaranteedInvoicePaymentModeModel novalnetPaymentMethod, Customer customer,
			Integer orderAmountCent, PaymentConfigResult result)
	{
		Integer onholdOrderAmount = resolveOnholdOrderAmount(novalnetPaymentMethod != null,
				() -> novalnetPaymentMethod.getNovalnetOnholdAmount(), false);

		applyVerifyPaymentData(novalnetPaymentMethod.getNovalnetOnholdAction().toString(), orderAmountCent, onholdOrderAmount,
				result);

		String dob = sessionService.getAttribute("novalnetGuaranteedInvoiceDateOfBirth");
		customer.setBirth_date(dob);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configureOnlineBankTransfer(NovalnetOnlineBankTransferPaymentModeModel novalnetPaymentMethod,
			PaymentConfigResult result)
	{
		result.setRedirect(true);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configureBancontact(NovalnetBancontactPaymentModeModel novalnetPaymentMethod, PaymentConfigResult result)
	{
		result.setRedirect(true);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configureIdeal(NovalnetIdealPaymentModeModel novalnetPaymentMethod, PaymentConfigResult result)
	{
		result.setRedirect(true);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configureGooglePay(NovalnetGooglePayPaymentModeModel novalnetPaymentMethod, HttpServletRequest request,
			PaymentData paymentData, Transaction transaction, Integer orderAmountCent, PaymentConfigResult result)
	{
		paymentData.setWallet_token(request.getParameter("token"));

		if (request.getParameter("doRedirect").equals("true"))
		{
			result.setRedirect(true);
		}

		if (Boolean.TRUE.equals(novalnetPaymentMethod.getNovalnetEnforce3D()))
		{
			transaction.setEnforce_3d(1);
			LOGGER.info("Enforce 3D enabled for GooglePay");
		}

		Integer onholdOrderAmount = resolveOnholdOrderAmount(novalnetPaymentMethod != null,
				() -> novalnetPaymentMethod.getNovalnetOnholdAmount(), false);

		applyVerifyPaymentData(novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount().toString(), orderAmountCent,
				onholdOrderAmount, result);

		applyZeroAmountBooking(novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount().toString(), result);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configureApplePay(NovalnetApplePayPaymentModeModel novalnetPaymentMethod, HostedPage hostedPage,
			Integer orderAmountCent, PaymentConfigResult result)
	{
		hostedPage.setDisplay_payments(Arrays.asList("APPLEPAY"));
		hostedPage.setHide_blocks(Arrays.asList("ADDRESS_FORM", "SHOP_INFO", "LANGUAGE_MENU", "HEADER", "TARIFF"));
		hostedPage.setSkip_pages(Arrays.asList("CONFIRMATION_PAGE", "SUCCESS_PAGE"));

		result.setRedirect(true);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}

		Integer onholdOrderAmount = resolveOnholdOrderAmount(novalnetPaymentMethod != null,
				() -> novalnetPaymentMethod.getNovalnetOnholdAmount(), false);

		applyVerifyPaymentData(novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount().toString(), orderAmountCent,
				onholdOrderAmount, result);

		applyZeroAmountBooking(novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount().toString(), result);
	}

	private void configureEps(NovalnetEpsPaymentModeModel novalnetPaymentMethod, PaymentConfigResult result)
	{
		result.setRedirect(true);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configurePostFinance(NovalnetPostFinancePaymentModeModel novalnetPaymentMethod, PaymentConfigResult result)
	{
		result.setRedirect(true);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configurePostFinanceCard(NovalnetPostFinanceCardPaymentModeModel novalnetPaymentMethod,
			PaymentConfigResult result)
	{
		result.setRedirect(true);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}

	private void configurePrzelewy24(NovalnetPrzelewy24PaymentModeModel novalnetPaymentMethod, PaymentConfigResult result)
	{
		result.setRedirect(true);

		if (novalnetPaymentMethod.getNovalnetTestMode())
		{
			result.setTestMode(1);
		}
		else
		{
			result.setTestMode(0);
		}
	}
}