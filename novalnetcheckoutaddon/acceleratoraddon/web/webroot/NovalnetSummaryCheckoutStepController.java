/*
 *
 * @author    Novalnet AG
 * @copyright Copyright by Novalnet
 * @license   https://www.novalnet.de/payment-plugins/kostenlos/lizenz
 *
 * If you have found this script useful a small
 * recommendation as well as a comment on merchant form
 * would be greatly appreciated.
 *
 */

package novalnet.novalnetcheckoutaddon.controllers.pages.checkout.steps;

import de.hybris.novalnet.core.model.NovalnetAliPayPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetApplePayPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetBancontactPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetBlikPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetCreditCardPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetDirectDebitAchPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetDirectDebitSepaPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetEpsPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetGooglePayPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetGuaranteedDirectDebitSepaPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetGuaranteedInvoicePaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetIdealPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetInvoicePaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetMbWayPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetMultibancoPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetOnlineBankTransferPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetPayPalPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetPostFinanceCardPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetPostFinancePaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetPrepaymentPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetPrzelewy24PaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetTrustlyPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetTwintPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetWechatPayPaymentModeModel;
import de.hybris.platform.acceleratorservices.enums.CheckoutPciOptionEnum;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.PreValidateCheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.PreValidateQuoteCheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import de.hybris.platform.acceleratorstorefrontcommons.checkout.steps.CheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.constants.WebConstants;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.checkout.steps.AbstractCheckoutStepController;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.acceleratorstorefrontcommons.forms.PlaceOrderForm;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.order.data.OrderEntryData;
import de.hybris.platform.commercefacades.product.ProductOption;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.util.Config;
import de.hybris.platform.util.localization.Localization;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import novalnet.novalnetcheckoutaddon.controllers.NovalnetcheckoutaddonControllerConstants;
import novalnet.novalnetcheckoutaddon.facades.impl.NovalnetFacade;


@Controller
@RequestMapping(value = "/checkout/multi/novalnet/summary")
public class NovalnetSummaryCheckoutStepController extends AbstractCheckoutStepController
{
	private static final Logger LOGGER = Logger.getLogger(NovalnetSummaryCheckoutStepController.class);

	private static final String SUMMARY = "summary";

	private static final String PAYMENT_AUTHORIZE = "AUTHORIZE";
	private static final String AUTHORIZE_WITH_ZERO_AMOUNT = "AUTHORIZE_WITH_ZERO_AMOUNT";

	public static final int CONVERT_TO_CENT_OR_SUCCESS_STATUS = 100;
	public static final int PREPAYMENT_FROM_DATE = 7;
	public static final int PREPAYMENT_TILL_DATE = 28;


	protected static final String REDIRECT_URL_ORDER_CONFIRMATION = REDIRECT_PREFIX + "/checkout/novalnet/orderConfirmation/";

	@Resource(name = "baseStoreService")
	private BaseStoreService baseStoreService;

	@Resource(name = "novalnetFacade")
	NovalnetFacade novalnetFacade;

	@Resource(name = "cartService")
	private CartService cartService;

	@Resource(name = "configurationService")
	private ConfigurationService configurationService;

	@Resource
	private Converter<AddressData, AddressModel> addressReverseConverter;

	@Resource
	private PaymentModeService paymentModeService;

	private static final String TRANSACTION_UPDATE_URL = "https://payport.novalnet.de/v2/transaction/update";

	private static final String API_PAYMENT_URL = "https://payport.novalnet.de/v2/payment";

	private static final String API_AUTHORIZE_URL = "https://payport.novalnet.de/v2/authorize";

	private static final String API_HOSTED_PAYMENT_URL = "https://payport.novalnet.de/v2/seamless/payment";

	private static final String NOVALNET_VERSION = "1.2.0";

	private static final String API_HOSTED_AUTHORIZE_URL = "https://payport.novalnet.de/v2/seamless/authorize";

	final Map<String, Object> transactionParameters = new HashMap<>();
	final Map<String, Object> merchantParameters = new HashMap<>();
	final Map<String, Object> customerParameters = new HashMap<>();
	final Map<String, Object> billingParameters = new HashMap<>();
	final Map<String, Object> shippingParameters = new HashMap<>();
	final Map<String, Object> customParameters = new HashMap<>();
	final Map<String, Object> paymentParameters = new HashMap<>();
	final Map<String, Object> dataParameters = new HashMap<>();
	final Map<String, Object> hostedPageParameters = new HashMap<>();


	@GetMapping(value = "/enter")
	@RequireHardLogIn
	@PreValidateQuoteCheckoutStep
	@PreValidateCheckoutStep(checkoutStep = SUMMARY)
	public String enterStep(final Model model, final RedirectAttributes redirectAttributes) throws CMSItemNotFoundException, // NOSONAR
			CommerceCartModificationException
	{
		final CartData cartData = getCheckoutFacade().getCheckoutCart();
		if (cartData.getEntries() != null && !cartData.getEntries().isEmpty())
		{
			for (final OrderEntryData entry : cartData.getEntries())
			{
				final String productCode = entry.getProduct().getCode();
				final ProductData product = getProductFacade().getProductForCodeAndOptions(productCode, Arrays.asList(
						ProductOption.BASIC, ProductOption.PRICE, ProductOption.VARIANT_MATRIX_BASE, ProductOption.PRICE_RANGE));
				entry.setProduct(product);
			}
		}

		String currentPayment = getSessionService().getAttribute("selectedPaymentMethodId");
		if (currentPayment == null || currentPayment.equals(""))
		{
			getSessionService().setAttribute("novalnetCheckoutError", "checkout.multi.paymentDetails.notprovided");
			return getCheckoutStep().previousStep();
		}

		model.addAttribute("currentPayment", currentPayment);

		model.addAttribute("cartData", cartData);
		model.addAttribute("allItems", cartData.getEntries());
		model.addAttribute("deliveryAddress", cartData.getDeliveryAddress());
		model.addAttribute("deliveryMode", cartData.getDeliveryMode());
		model.addAttribute("paymentInfo", cartData.getPaymentInfo());

		// Only request the security code if the SubscriptionPciOption is set to Default.
		final boolean requestSecurityCode = CheckoutPciOptionEnum.DEFAULT
				.equals(getCheckoutFlowFacade().getSubscriptionPciOption());
		model.addAttribute("requestSecurityCode", Boolean.valueOf(requestSecurityCode));

		model.addAttribute(new PlaceOrderForm());

		final Map<String, Object> customerParameter = (Map<String, Object>) getSessionService()
				.getAttribute("novalnetCustomerParams");

		final String currency = cartData.getTotalPriceWithTax().getCurrencyIso();
		model.addAttribute("currency", currency);
		model.addAttribute("countryCode", customerParameter.get("country"));

		final ContentPageModel multiCheckoutSummaryPage = getContentPageForLabelOrId(MULTI_CHECKOUT_SUMMARY_CMS_PAGE_LABEL);
		storeCmsPageInModel(model, multiCheckoutSummaryPage);
		setUpMetaDataForContentPage(model, multiCheckoutSummaryPage);

		model.addAttribute(WebConstants.BREADCRUMBS_KEY,
				getResourceBreadcrumbBuilder().getBreadcrumbs("checkout.multi.summary.breadcrumb"));
		model.addAttribute("metaRobots", "noindex,nofollow");
		setCheckoutStepLinksForModel(model, getCheckoutStep());

		final Locale language = JaloSession.getCurrentSession().getSessionContext().getLocale();
		final String languageCode = (language != null) ? language.toString().toUpperCase() : "EN";
		model.addAttribute("lang", languageCode);

		final BaseStoreModel baseStore = this.getBaseStoreModel();
		model.addAttribute("novalnetBaseStoreConfiguration", baseStore);

		model.addAttribute("novalnetApplePay", paymentModeService.getPaymentModeForCode("novalnetApplePay"));
		model.addAttribute("novalnetGooglePay", paymentModeService.getPaymentModeForCode("novalnetGooglePay"));

		BigDecimal totalAmount = cartData.getTotalPriceWithTax().getValue();

		BigDecimal orderAmountCents = totalAmount.multiply(BigDecimal.valueOf(CONVERT_TO_CENT_OR_SUCCESS_STATUS)).setScale(0,
				RoundingMode.HALF_UP);

		Integer orderAmountCent = orderAmountCents.intValue();

		getSessionService().setAttribute("novalnetOrderAmount", orderAmountCent);

		model.addAttribute("orderAmount", orderAmountCent);

		return NovalnetcheckoutaddonControllerConstants.CheckoutSummaryPage;
	}

	public static String formatAmount(String amount)
	{
		if (amount.contains(","))
		{
			try
			{
				NumberFormat formattedAmount = NumberFormat.getNumberInstance(Locale.GERMANY);
				double formattedValue = formattedAmount.parse(amount).doubleValue();
				amount = Double.toString(formattedValue);
			}
			catch (Exception e)
			{
				amount = amount.replace(",", ".");
			}
		}
		return amount;
	}

	@PostMapping(value =
	{ "/bookWalletTransaction" })
	@RequireHardLogIn
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public String bookWalletTransaction(final Model model, final HttpServletRequest request,
			final RedirectAttributes redirectModel) throws CMSItemNotFoundException, // NOSONAR
			InvalidCartException, CommerceCartModificationException, Exception
	{
		final BaseStoreModel baseStore = this.getBaseStoreModel();
		String currentPayment = "novalnetGooglePay";
		String customerNo = JaloSession.getCurrentSession().getUser().getPK().toString();
		Integer orderAmountCent = (Integer) getSessionService().getAttribute("novalnetOrderAmount");
		StringBuilder response = createTransaction(request, baseStore, currentPayment, customerNo, orderAmountCent);
		LOGGER.info("Novalnet Response: {}" + response);
		JSONObject tomJsonObject = new JSONObject(response.toString());
		JSONObject resultJsonObject = tomJsonObject.getJSONObject("result");

		if (resultJsonObject.has("redirect_url"))
		{
			String redirectURL = resultJsonObject.get("redirect_url").toString();
			getSessionService().setAttribute("txn_check", baseStore.getNovalnetPaymentAccessKey().trim());
		}

		if (tomJsonObject.has("transaction"))
		{
			JSONObject transactionJsonObject = tomJsonObject.getJSONObject("transaction");
			if (transactionJsonObject.has("txn_secret"))
			{
				getSessionService().setAttribute("txn_secret", transactionJsonObject.get("txn_secret").toString());
			}
			if (transactionJsonObject.has("tid"))
			{
				getSessionService().setAttribute("wallet_tid", transactionJsonObject.get("tid").toString());
			}
		}

		return response.toString();
	}


	/**
	 * @param request
	 * @param baseStore
	 * @param currentPayment
	 * @param customerNo
	 * @param orderAmountCent
	 * @return
	 */
	private StringBuilder createTransaction(HttpServletRequest request, BaseStoreModel baseStore, String currentPayment,
			String customerNo, Integer orderAmountCent)
	{
		final Integer tariff = baseStore.getNovalnetTariffId();
		final String apiKey = baseStore.getNovalnetAPIKey();
		String hybrisVersion = Config.getString("build.version", "unknown");
		String novalnetVersion = NOVALNET_VERSION;
		String token = "";

		final CartData cartData = getCheckoutFacade().getCheckoutCart();

		final String currency = cartData.getTotalPriceWithTax().getCurrencyIso();
		final Map<String, Object> customerParameter = (Map<String, Object>) getSessionService()
				.getAttribute("novalnetCustomerParams");
		PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(currentPayment);

		orderAmountCent = getSessionService().getAttribute("novalnetOrderAmount");

		Integer testMode = 0;
		boolean redirect = false;


		merchantParameters.put("signature", apiKey);
		merchantParameters.put("tariff", tariff);

		customerParameters.put("first_name", customerParameter.get("first_name"));
		customerParameters.put("last_name", customerParameter.get("last_name"));
		customerParameters.put("email", customerParameter.get("email"));
		customerParameters.put("customer_ip", getRemoteIpAddr(request));
		customerParameters.put("customer_no", customerNo);
		customerParameters.put("gender", "u");


		billingParameters.put("street", customerParameter.get("street"));
		billingParameters.put("city", customerParameter.get("city"));
		billingParameters.put("zip", customerParameter.get("zip"));
		billingParameters.put("country_code", customerParameter.get("country"));

		String sameAsBilling = getSessionService().getAttribute("same_as_billing");
		if ("1".equals(sameAsBilling))
		{
			shippingParameters.put("same_as_billing", sameAsBilling);
			getSessionService().setAttribute("same_as_billing", null);
		}
		else
		{
			shippingParameters.put("street", customerParameter.get("shipping_street"));
			shippingParameters.put("city", customerParameter.get("shipping_city"));
			shippingParameters.put("zip", customerParameter.get("shipping_zip"));
			shippingParameters.put("country_code", customerParameter.get("shipping_country"));
			shippingParameters.put("first_name", customerParameter.get("shipping_first_name"));
			shippingParameters.put("last_name", customerParameter.get("shipping_last_name"));
		}

		customerParameters.put("billing", billingParameters);
		customerParameters.put("shipping", shippingParameters);

		transactionParameters.put("payment_type", getPaymentType(currentPayment));
		transactionParameters.put("currency", currency);
		transactionParameters.put("amount", orderAmountCent);
		transactionParameters.put("system_name", "SAP Commerce Cloud");
		transactionParameters.put("system_version", hybrisVersion + "-NN" + novalnetVersion);

		boolean verify_payment_data = false;

		boolean oneClickShopping = false;
		boolean zeroAmountBooking = false;

		final Locale language = JaloSession.getCurrentSession().getSessionContext().getLocale();
		final String languageCode = (language != null) ? language.toString().toUpperCase() : "EN";
		customParameters.put("lang", languageCode);
		Integer onholdOrderAmount = 0;

		if ("novalnetDirectDebitSepa".equals(currentPayment))
		{

			NovalnetDirectDebitSepaPaymentModeModel novalnetPaymentMethod = (NovalnetDirectDebitSepaPaymentModeModel) paymentModeModel;

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}

			Integer sepaDueDate = novalnetPaymentMethod.getNovalnetDueDate();
			if (sepaDueDate != null && sepaDueDate >= 3 && sepaDueDate <= 14)
			{
				transactionParameters.put("due_date", formatDate(sepaDueDate));
			}

			if (novalnetPaymentMethod != null)
			{
				onholdOrderAmount = novalnetPaymentMethod.getNovalnetOnholdAmount();
				if (onholdOrderAmount == null)
				{
					onholdOrderAmount = 0;
				}
			}
			else
			{
				LOGGER.error("onhold order amount is null");
			}

			if (PAYMENT_AUTHORIZE.equals(novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount().toString())
					&& orderAmountCent >= onholdOrderAmount)
			{
				LOGGER.info("ActionType = [" + novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount() + "]");
				verify_payment_data = true;
			}
			LOGGER.info("ActionType = [" + novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount() + "]");
			LOGGER.error("PAYMENT_AUTHORIZE sepa" + novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount());

			if (AUTHORIZE_WITH_ZERO_AMOUNT.equals(novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount().toString()))
			{
				LOGGER.info("Onhold Action : " + novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount());
				zeroAmountBooking = true;
			}

			boolean novalnetDirectDebitSepaStorePaymentData = getSessionService()
					.getAttribute("novalnetDirectDebitSepaStorePaymentData");

			if (getSessionService().getAttribute("novalnetDirectDebitSepatoken") != null)
			{
				token = getSessionService().getAttribute("novalnetDirectDebitSepatoken");
			}
			else
			{
				LOGGER.info("novalnetDirectDebitSepatoken is null");
				token = "";
			}


			if (Boolean.FALSE.equals(novalnetFacade.isGuestUser())
					&& Boolean.TRUE.equals(novalnetPaymentMethod.getNovalnetOneClickShopping())
					&& Boolean.TRUE.equals(novalnetDirectDebitSepaStorePaymentData))
			{
				transactionParameters.put("create_token", '1');
				oneClickShopping = true;
			}


			if (Boolean.FALSE.equals(novalnetFacade.isGuestUser())
					&& Boolean.TRUE.equals(novalnetPaymentMethod.getNovalnetOneClickShopping()) && !"".equals(token))
			{
				paymentParameters.put("token", token);
				getSessionService().setAttribute("novalnetDirectDebitSepatoken", null);
			}

			if ("".equals(token))
			{
				String accountHolder = getSessionService().getAttribute("novalnetDirectDebitSepaAccountHolder");
				paymentParameters.put("iban", getSessionService().getAttribute("novalnetDirectDebitSepaAccountIban").toString());
				if (getSessionService().getAttribute("novalnetDirectDebitSepaAccountBic") != null
						&& !"".equals(getSessionService().getAttribute("novalnetDirectDebitSepaAccountBic").toString()))
				{
					paymentParameters.put("bic", getSessionService().getAttribute("novalnetDirectDebitSepaAccountBic").toString());
					getSessionService().setAttribute("novalnetDirectDebitSepaAccountBic", null);
				}
				paymentParameters.put("bank_account_holder", accountHolder.replace("&", ""));
				getSessionService().setAttribute("novalnetDirectDebitSepaAccountIban", null);
				getSessionService().setAttribute("novalnetDirectDebitSepaAccountHolder", null);

			}

		}

		else if ("novalnetDirectDebitAch".equals(currentPayment))
		{
			NovalnetDirectDebitAchPaymentModeModel novalnetPaymentMethod = (NovalnetDirectDebitAchPaymentModeModel) paymentModeModel;

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}

			if (AUTHORIZE_WITH_ZERO_AMOUNT.equals(novalnetPaymentMethod.getOnholdActionTypeWithoutAuthorize().toString()))
			{
				LOGGER.info("Onhold Action : " + novalnetPaymentMethod.getOnholdActionTypeWithoutAuthorize());
				zeroAmountBooking = true;
			}

			boolean novalnetDirectDebitAchStorePaymentData = getSessionService()
					.getAttribute("novalnetDirectDebitAchStorePaymentData");

			if (getSessionService().getAttribute("novalnetDirectDebitAchtoken") != null)
			{
				token = getSessionService().getAttribute("novalnetDirectDebitAchtoken");
			}
			else
			{
				LOGGER.info("novalnetDirectDebitAchtoken is null");
				token = "";
			}


			if (Boolean.FALSE.equals(novalnetFacade.isGuestUser())
					&& Boolean.TRUE.equals(novalnetPaymentMethod.getNovalnetOneClickShopping())
					&& Boolean.TRUE.equals(novalnetDirectDebitAchStorePaymentData))
			{
				transactionParameters.put("create_token", '1');
				oneClickShopping = true;
			}


			if (Boolean.FALSE.equals(novalnetFacade.isGuestUser())
					&& Boolean.TRUE.equals(novalnetPaymentMethod.getNovalnetOneClickShopping()) && !"".equals(token))
			{
				paymentParameters.put("token", token);
				getSessionService().setAttribute("novalnetDirectDebitAchtoken", null);
			}

			if ("".equals(token))
			{
				String accountHolder = getSessionService().getAttribute("novalnetDirectDebitAchAccountHolder");
				paymentParameters.put("account_number",
						getSessionService().getAttribute("novalnetDirectDebitAchAchAccountNumber").toString());
				if (getSessionService().getAttribute("novalnetDirectDebitAchRoutingNumber") != null
						&& !"".equals(getSessionService().getAttribute("novalnetDirectDebitAchRoutingNumber").toString()))
				{
					paymentParameters.put("routing_number",
							getSessionService().getAttribute("novalnetDirectDebitAchRoutingNumber").toString());
					getSessionService().setAttribute("novalnetDirectDebitAchRoutingNumber", null);
				}
				paymentParameters.put("account_holder", accountHolder.replace("&", ""));
				getSessionService().setAttribute("novalnetDirectDebitAchAchAccountNumber", null);
				getSessionService().setAttribute("novalnetDirectDebitAchAccountHolder", null);

			}

		}

		else if ("novalnetGuaranteedDirectDebitSepa".equals(currentPayment))
		{

			NovalnetGuaranteedDirectDebitSepaPaymentModeModel novalnetPaymentMethod = (NovalnetGuaranteedDirectDebitSepaPaymentModeModel) paymentModeModel;

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
			if (novalnetPaymentMethod != null)
			{
				onholdOrderAmount = novalnetPaymentMethod.getNovalnetOnholdAmount();
				if (onholdOrderAmount == null)
				{
					onholdOrderAmount = 0;
				}
			}
			else
			{
				LOGGER.error("onhold order amount is null");
			}

			boolean novalnetGuaranteedDirectDebitSepaStorePaymentData = getSessionService()
					.getAttribute("novalnetGuaranteedDirectDebitSepaStorePaymentData");

			if (getSessionService().getAttribute("novalnetDirectDebitSepatoken") != null)
			{
				token = getSessionService().getAttribute("novalnetDirectDebitSepatoken");
			}
			else
			{
				LOGGER.info("novalnetDirectDebitSepatoken is null");
				token = "";
			}


			if (Boolean.FALSE.equals(novalnetFacade.isGuestUser())
					&& Boolean.TRUE.equals(novalnetPaymentMethod.getNovalnetOneClickShopping())
					&& Boolean.TRUE.equals(novalnetGuaranteedDirectDebitSepaStorePaymentData))
			{
				transactionParameters.put("create_token", '1');
				oneClickShopping = true;
			}


			if (Boolean.FALSE.equals(novalnetFacade.isGuestUser())
					&& Boolean.TRUE.equals(novalnetPaymentMethod.getNovalnetOneClickShopping()) && !"".equals(token))
			{
				paymentParameters.put("token", token);
				getSessionService().setAttribute("novalnetDirectDebitSepatoken", null);
			}

			if ("".equals(token))
			{
				String accountHolder = getSessionService().getAttribute("novalnetGuaranteedDirectDebitSepaAccountHolder");
				paymentParameters.put("iban",
						getSessionService().getAttribute("novalnetGuaranteedDirectDebitSepaAccountIban").toString());
				if (getSessionService().getAttribute("novalnetGuaranteedDirectDebitSepaAccountBic") != null
						&& !"".equals(getSessionService().getAttribute("novalnetGuaranteedDirectDebitSepaAccountBic").toString()))
				{
					paymentParameters.put("bic",
							getSessionService().getAttribute("novalnetGuaranteedDirectDebitSepaAccountBic").toString());
					getSessionService().setAttribute("novalnetGuaranteedDirectDebitSepaAccountBic", null);
				}
				paymentParameters.put("bank_account_holder", accountHolder.replace("&", ""));
				getSessionService().setAttribute("novalnetGuaranteedDirectDebitSepaAccountIban", null);
				getSessionService().setAttribute("novalnetGuaranteedDirectDebitSepaAccountHolder", null);
			}

			String dob = getSessionService().getAttribute("novalnetGuaranteedDirectDebitSepaDateOfBirth");
			customerParameters.put("birth_date", dob);

			if (PAYMENT_AUTHORIZE.equals(novalnetPaymentMethod.getNovalnetOnholdAction().toString())
					&& orderAmountCent >= onholdOrderAmount)
			{
				verify_payment_data = true;
			}
		}
		else if ("novalnetPayPal".equals(currentPayment))
		{
			redirect = true;
			NovalnetPayPalPaymentModeModel novalnetPaymentMethod = (NovalnetPayPalPaymentModeModel) paymentModeModel;

			if (novalnetPaymentMethod != null)
			{
				onholdOrderAmount = novalnetPaymentMethod.getNovalnetOnholdAmount();
				if (onholdOrderAmount == null)
				{
					onholdOrderAmount = 0;
				}
			}
			else
			{
				LOGGER.info("onhold order amount is null");
			}

			if (PAYMENT_AUTHORIZE.equals(novalnetPaymentMethod.getNovalnetOnholdAction().toString())
					&& orderAmountCent >= onholdOrderAmount)
			{
				verify_payment_data = true;
			}

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
		}
		else if ("novalnetCreditCard".equals(currentPayment))
		{
			NovalnetCreditCardPaymentModeModel novalnetPaymentMethod = (NovalnetCreditCardPaymentModeModel) paymentModeModel;
			if (novalnetPaymentMethod != null)
			{
				onholdOrderAmount = novalnetPaymentMethod.getNovalnetOnholdAmount();
				if (onholdOrderAmount == null)
				{
					onholdOrderAmount = 0;
				}

			}
			else
			{
				LOGGER.info("onhold order amount is null");
			}

			if (Boolean.TRUE.equals(novalnetPaymentMethod.getNovalnetEnforce3D()))
			{
				transactionParameters.put("enforce_3d", 1);
				LOGGER.info("Enforce 3D enabled for Credit Card");
			}

			if (PAYMENT_AUTHORIZE.equals(novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount().toString())
					&& orderAmountCent >= onholdOrderAmount)
			{
				verify_payment_data = true;
			}

			if (AUTHORIZE_WITH_ZERO_AMOUNT.equals(novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount().toString()))
			{
				LOGGER.info("Onhold Action : " + novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount());
				zeroAmountBooking = true;

			}


			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
				
			}

			if (getSessionService().getAttribute("novalnetCreditCardtoken") != null)
			{
				token = getSessionService().getAttribute("novalnetCreditCardtoken");
			}
			else
			{
				LOGGER.info("novalnetCreditCardtoken is null");
				token = "";
			}

			if (getSessionService().getAttribute("novalnetCreditCardtoken") != null)
			{
				token = getSessionService().getAttribute("novalnetCreditCardtoken");
			}
			else
			{
				LOGGER.info("onhold order amount is null");
				onholdOrderAmount = 0;
			}

			boolean novalnetCreditCardStorePaymentData = getSessionService().getAttribute("novalnetCreditCardStorePaymentData");

			if (!novalnetFacade.isGuestUser() && novalnetPaymentMethod.getNovalnetOneClickShopping()
					&& Boolean.TRUE.equals(novalnetCreditCardStorePaymentData))
			{
				transactionParameters.put("create_token", '1');
				oneClickShopping = true;
			}

			String referenceTid = getSessionService().getAttribute("novalnetCreditCardReferenceTid");
			if (!novalnetFacade.isGuestUser() && novalnetPaymentMethod.getNovalnetOneClickShopping() && !"".equals(token))
			{
				paymentParameters.put("token", token);
				getSessionService().setAttribute("novalnetCreditCardtoken", null);
			}
			else
			{
				paymentParameters.put("pan_hash", getSessionService().getAttribute("novalnetCreditCardPanHash"));
				paymentParameters.put("unique_id", getSessionService().getAttribute("novalnetCreditCardUniqueId"));
				String do_redirect = getSessionService().getAttribute("do_redirect");

				if (!"".equals(do_redirect))
				{
					redirect = true;
				}

				getSessionService().setAttribute("novalnetCreditCardPanHash", null);

			}
		}
		else if ("novalnetInvoice".equals(currentPayment))
		{
			NovalnetInvoicePaymentModeModel novalnetPaymentMethod = (NovalnetInvoicePaymentModeModel) paymentModeModel;
			transactionParameters.put("invoice_type", "INVOICE");

			Integer invoiceDueDate = novalnetPaymentMethod.getNovalnetDueDate();
			if (invoiceDueDate != null && invoiceDueDate > 7)
			{
				transactionParameters.put("due_date", formatDate(invoiceDueDate));
			}

			if (novalnetPaymentMethod != null)
			{
				onholdOrderAmount = novalnetPaymentMethod.getNovalnetOnholdAmount();
				if (onholdOrderAmount == null)
				{
					onholdOrderAmount = 0;
				}
			}
			else
			{
				LOGGER.info("onhold order amount is null");
			}

			if (PAYMENT_AUTHORIZE.equals(novalnetPaymentMethod.getNovalnetOnholdAction().toString())
					&& orderAmountCent >= onholdOrderAmount)
			{
				verify_payment_data = true;
			}


			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
		}
		else if ("novalnetPrepayment".equals(currentPayment))
		{
			NovalnetPrepaymentPaymentModeModel novalnetPaymentMethod = (NovalnetPrepaymentPaymentModeModel) paymentModeModel;
			Integer prepaymentDueDate = novalnetPaymentMethod.getNovalnetDueDate();
			if (prepaymentDueDate != null && PREPAYMENT_FROM_DATE >= 7 && prepaymentDueDate <= PREPAYMENT_TILL_DATE)
			{
				transactionParameters.put("due_date", formatDate(prepaymentDueDate));
			}
			transactionParameters.put("invoice_type", "PREPAYMENT");

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
		}
		else if ("novalnetMultibanco".equals(currentPayment))
		{
			NovalnetMultibancoPaymentModeModel novalnetPaymentMethod = (NovalnetMultibancoPaymentModeModel) paymentModeModel;
			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
		}
		else if ("novalnetTwint".equals(currentPayment))
		{
			NovalnetTwintPaymentModeModel novalnetPaymentMethod = (NovalnetTwintPaymentModeModel) paymentModeModel;

			redirect = true;

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
		}
		else if ("novalnetMbWay".equals(currentPayment))
		{
			NovalnetMbWayPaymentModeModel novalnetPaymentMethod = (NovalnetMbWayPaymentModeModel) paymentModeModel;

			redirect = true;

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
		}
		else if ("novalnetTrustly".equals(currentPayment))
		{
			NovalnetTrustlyPaymentModeModel novalnetPaymentMethod = (NovalnetTrustlyPaymentModeModel) paymentModeModel;

			redirect = true;

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
		}
		else if ("novalnetBlik".equals(currentPayment))
		{
			NovalnetBlikPaymentModeModel novalnetPaymentMethod = (NovalnetBlikPaymentModeModel) paymentModeModel;

			redirect = true;

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
		}
		else if ("novalnetWechatPay".equals(currentPayment))
		{
			NovalnetWechatPayPaymentModeModel novalnetPaymentMethod = (NovalnetWechatPayPaymentModeModel) paymentModeModel;

			redirect = true;

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
		}
		else if ("novalnetAlipay".equals(currentPayment))
		{
			NovalnetAliPayPaymentModeModel novalnetPaymentMethod = (NovalnetAliPayPaymentModeModel) paymentModeModel;

			redirect = true;

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
		}

		else if ("novalnetGuaranteedInvoice".equals(currentPayment))
		{
			NovalnetGuaranteedInvoicePaymentModeModel novalnetPaymentMethod = (NovalnetGuaranteedInvoicePaymentModeModel) paymentModeModel;

			if (novalnetPaymentMethod != null)
			{
				onholdOrderAmount = novalnetPaymentMethod.getNovalnetOnholdAmount();
				if (onholdOrderAmount == null)
				{
					onholdOrderAmount = 0;
				}
			}
			else
			{
				LOGGER.info("onhold order amount is null");
			}

			if (PAYMENT_AUTHORIZE.equals(novalnetPaymentMethod.getNovalnetOnholdAction().toString())
					&& orderAmountCent >= onholdOrderAmount)
			{
				verify_payment_data = true;
			}

			String dob = getSessionService().getAttribute("novalnetGuaranteedInvoiceDateOfBirth");
			customerParameters.put("birth_date", dob);

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
		}
		else if ("novalnetOnlineBankTransfer".equals(currentPayment))
		{
			NovalnetOnlineBankTransferPaymentModeModel novalnetPaymentMethod = (NovalnetOnlineBankTransferPaymentModeModel) paymentModeModel;

			redirect = true;

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
		}
		else if ("novalnetBancontact".equals(currentPayment))
		{
			NovalnetBancontactPaymentModeModel novalnetPaymentMethod = (NovalnetBancontactPaymentModeModel) paymentModeModel;

			redirect = true;

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
		}
		else if ("novalnetIdeal".equals(currentPayment))
		{
			NovalnetIdealPaymentModeModel novalnetPaymentMethod = (NovalnetIdealPaymentModeModel) paymentModeModel;

			redirect = true;

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
		}
		else if ("novalnetGooglePay".equals(currentPayment))
		{
			NovalnetGooglePayPaymentModeModel novalnetPaymentMethod = (NovalnetGooglePayPaymentModeModel) paymentModeModel;

			paymentParameters.put("wallet_token", request.getParameter("token"));

			if (request.getParameter("doRedirect").equals("true"))
			{
				redirect = true;
			}

			if (Boolean.TRUE.equals(novalnetPaymentMethod.getNovalnetEnforce3D()))
			{
				transactionParameters.put("enforce_3d", 1);
				LOGGER.info("Enforce 3D enabled for GooglePay");
			}

			if (novalnetPaymentMethod != null)
			{
				onholdOrderAmount = novalnetPaymentMethod.getNovalnetOnholdAmount();
				if (onholdOrderAmount == null)
				{
					onholdOrderAmount = 0;
				}
			}
			else
			{
				LOGGER.info("onhold order amount is null");
			}

			if (PAYMENT_AUTHORIZE.equals(novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount().toString())
					&& orderAmountCent >= onholdOrderAmount)
			{
				verify_payment_data = true;
			}
			if (AUTHORIZE_WITH_ZERO_AMOUNT.equals(novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount().toString()))
			{
				LOGGER.info("Onhold Action : " + novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount());
				zeroAmountBooking = true;
			}


			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}

		}
		else if ("novalnetApplePay".equals(currentPayment))
		{
			hostedPageParameters.put("display_payments", Arrays.asList("APPLEPAY"));
			hostedPageParameters.put("hide_blocks", Arrays.asList("ADDRESS_FORM", "SHOP_INFO", "LANGUAGE_MENU", "HEADER", "TARIFF"));
			hostedPageParameters.put("skip_pages", Arrays.asList("CONFIRMATION_PAGE", "SUCCESS_PAGE"));

			dataParameters.put("hosted_page", hostedPageParameters);

			LOGGER.info("Apple Pay hosted_page parameters: " + hostedPageParameters);

			NovalnetApplePayPaymentModeModel novalnetPaymentMethod = (NovalnetApplePayPaymentModeModel) paymentModeModel;

			redirect = true;

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
			if (novalnetPaymentMethod != null)
			{

				onholdOrderAmount = novalnetPaymentMethod.getNovalnetOnholdAmount();
				if (onholdOrderAmount == null)
				{
					onholdOrderAmount = 0;
				}
			}
			else
			{
				LOGGER.info("onhold order amount is null");
			}

			if (PAYMENT_AUTHORIZE.equals(novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount().toString())
					&& orderAmountCent >= onholdOrderAmount)
			{
				verify_payment_data = true;
			}
			if (AUTHORIZE_WITH_ZERO_AMOUNT.equals(novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount().toString()))
			{
				LOGGER.info("Onhold Action : " + novalnetPaymentMethod.getOnholdActionTypeWithZeroAmount());
				zeroAmountBooking = true;

			}

		}

		else if ("novalnetEps".equals(currentPayment))
		{
			NovalnetEpsPaymentModeModel novalnetPaymentMethod = (NovalnetEpsPaymentModeModel) paymentModeModel;

			redirect = true;

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}

		}
		else if ("novalnetPostFinance".equals(currentPayment))
		{
			NovalnetPostFinancePaymentModeModel novalnetPaymentMethod = (NovalnetPostFinancePaymentModeModel) paymentModeModel;

			redirect = true;

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
		}
		else if ("novalnetPostFinanceCard".equals(currentPayment))
		{
			NovalnetPostFinanceCardPaymentModeModel novalnetPaymentMethod = (NovalnetPostFinanceCardPaymentModeModel) paymentModeModel;

			redirect = true;

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
		}
		else if ("novalnetPrzelewy24".equals(currentPayment))
		{
			NovalnetPrzelewy24PaymentModeModel novalnetPaymentMethod = (NovalnetPrzelewy24PaymentModeModel) paymentModeModel;

			redirect = true;

			if (novalnetPaymentMethod.getNovalnetTestMode())
			{
				testMode = 1;
			}
		}

		transactionParameters.put("test_mode", testMode);

		if (Boolean.TRUE.equals(redirect))
		{
			final String currentUrl = request.getRequestURL().toString();

			String[] walletPayments =
			{ "novalnetGooglePay" };

			String returnUrl = currentUrl
					.replace((!Arrays.asList(walletPayments).contains(currentPayment)) ? "novalnet/summary/placeOrder"
							: "novalnet/summary/bookWalletTransaction", "novalnet/hop-response");

			transactionParameters.put("return_url", returnUrl);
			transactionParameters.put("error_return_url", returnUrl);
			transactionParameters.put("payment_data", paymentParameters);
		}
		else
		{

			transactionParameters.put("payment_data", paymentParameters);
		}

		if (Boolean.TRUE.equals(zeroAmountBooking))
		{
			LOGGER.info("Zero amount booking is enabled.");
			transactionParameters.put("amount", 0);
		}

		dataParameters.put("merchant", merchantParameters);
		dataParameters.put("customer", customerParameters);
		dataParameters.put("transaction", transactionParameters);
		dataParameters.put("custom", customParameters);

		Gson gson = new GsonBuilder().create();
		String jsonString = gson.toJson(dataParameters);

		LOGGER.info("verify_payment_data = " + verify_payment_data);

		String url;

		if ("novalnetApplePay".equals(currentPayment))
		{
			url = verify_payment_data ? API_HOSTED_AUTHORIZE_URL : API_HOSTED_PAYMENT_URL;
		}
		else
		{
			url = verify_payment_data ? API_AUTHORIZE_URL : API_PAYMENT_URL;
		}

		StringBuilder response = novalnetFacade.sendRequest(url, jsonString);
		JSONObject tomJsonObject = new JSONObject(response.toString());
		LOGGER.info("Novalnet API Response : " + response.toString());

		transactionParameters.clear();
		customerParameters.clear();
		dataParameters.clear();
		paymentParameters.clear();
		customParameters.clear();
		merchantParameters.clear();
		billingParameters.clear();
		shippingParameters.clear();
		hostedPageParameters.clear();
		return response;
	}

	@RequestMapping(value = "/placeOrder")
	@PreValidateQuoteCheckoutStep
	@RequireHardLogIn
	public String placeOrder(@ModelAttribute("placeOrderForm")
	final PlaceOrderForm placeOrderForm, final Model model, final HttpServletRequest request,
			final RedirectAttributes redirectModel) throws CMSItemNotFoundException, // NOSONAR
			InvalidCartException, CommerceCartModificationException
	{
		final CartData cartData = getCheckoutFacade().getCheckoutCart();
		final BaseStoreModel baseStore = this.getBaseStoreModel();
		final String currency = cartData.getTotalPriceWithTax().getCurrencyIso();
		final Map<String, Object> customerParameter = (Map<String, Object>) getSessionService()
				.getAttribute("novalnetCustomerParams");
		String customerNo = JaloSession.getCurrentSession().getUser().getPK().toString();
		final Locale language = JaloSession.getCurrentSession().getSessionContext().getLocale();
		final String languageCode = (language != null) ? language.toString().toUpperCase() : "EN";
		String currentPayment = getSessionService().getAttribute("selectedPaymentMethodId");
		LOGGER.info("Selected Payment Method ID: {}" + currentPayment);
		PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(currentPayment);

		Integer orderAmountCent = getSessionService().getAttribute("novalnetOrderAmount");

		String[] walletPayments =
		{ "novalnetGooglePay" };

		Gson gson = new GsonBuilder().create();

		String url = "";

		String jsonString = "";

		StringBuilder response = new StringBuilder();

		if ((Arrays.asList(walletPayments).contains(currentPayment)))
		{
			transactionParameters.put("tid", getSessionService().getAttribute("wallet_tid"));
			customParameters.put("lang", languageCode);
			dataParameters.put("transaction", transactionParameters);
			dataParameters.put("custom", customParameters);
			jsonString = gson.toJson(dataParameters);
			url = "https://payport.novalnet.de/v2/transaction/details";
			response = novalnetFacade.sendRequest(url, jsonString);
			LOGGER.info(response);
			getSessionService().removeAttribute("wallet_tid");
		}

		else
		{
			response = createTransaction(request, baseStore, currentPayment, customerNo, orderAmountCent);
			LOGGER.info(response);
		}
		JSONObject tomJsonObject = new JSONObject(response.toString());
		JSONObject resultJsonObject = tomJsonObject.getJSONObject("result");
		JSONObject transactionJsonObject = tomJsonObject.getJSONObject("transaction");

		if (!String.valueOf(CONVERT_TO_CENT_OR_SUCCESS_STATUS).equals(resultJsonObject.get("status_code").toString()))
		{
			final String statMessage = resultJsonObject.get("status_text").toString() != null
					? resultJsonObject.get("status_text").toString()
					: resultJsonObject.get("status_desc").toString();
			getSessionService().setAttribute("novalnetCheckoutError", statMessage);
			return getCheckoutStep().previousStep();
		}

		if (resultJsonObject.has("redirect_url"))
		{
			String redirectURL = resultJsonObject.get("redirect_url").toString();
			LOGGER.info("Novalnet Redirect URL : {}" + redirectURL);
			setupPageModel(model);
			model.addAttribute("paygateUrl", redirectURL);
			getSessionService().setAttribute("txn_secret", transactionJsonObject.get("txn_secret").toString());
			getSessionService().setAttribute("txn_check", baseStore.getNovalnetPaymentAccessKey().trim());
			return "redirect:" + redirectURL;
		}

		JSONObject customerJsonObject = tomJsonObject.getJSONObject("customer");

		if (validateOrderForm(placeOrderForm, model))
		{
			return enterStep(model, redirectModel);
		}

		if (validateCart(redirectModel))
		{
			return REDIRECT_PREFIX + "/cart";
		}

		String[] successStatus =
		{ "CONFIRMED", "ON_HOLD", "PENDING" };

		if (Arrays.asList(successStatus).contains(transactionJsonObject.get("status").toString()))
		{
			String paymentName = novalnetFacade.getPaymentName(currentPayment);
			
			String testMode = "";

			if (transactionJsonObject.get("test_mode").toString().equals("1"))
			{
				testMode = " " + Localization.getLocalizedString("novalnet.testOrderText");
			}
			String orderComments = Localization.getLocalizedString("novalnet.paymentname") + ": " + paymentName + "<br>";
			
			orderComments += Localization.getLocalizedString("novalnet.transactionId") + " : " + transactionJsonObject.get("tid") + "<br>"+testMode +"<br>";
			
			AddressData addressData = getSessionService().getAttribute("novalnetAddressData");

			String bankDetails = "";
			if (("novalnetInvoice".equals(currentPayment) || "novalnetPrepayment".equals(currentPayment)
					|| "novalnetGuaranteedInvoice".equals(currentPayment)))
			{
				JSONObject bankdeatailsJsonObject = transactionJsonObject.getJSONObject("bank_details");

				bankDetails += "<br>" + String.format(Localization.getLocalizedString("novalnet.bankDetailsComments1"),
						cartData.getTotalPriceWithTax().getFormattedValue());

				if (transactionJsonObject.has("due_date") && !"ON_HOLD".equals(transactionJsonObject.get("status").toString()))
				{
               LocalDate localDate = LocalDate.parse(transactionJsonObject.get("due_date").toString());
               DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
               String formattedDate = localDate.format(formatter);
					bankDetails += " "
							+ String.format(Localization.getLocalizedString("novalnet.bankDetailsComments2"), formattedDate);
				}

				bankDetails += "<br>" + Localization.getLocalizedString("novalnet.bankDetailsAccountHolder") + " "
						+ bankdeatailsJsonObject.get("account_holder").toString();

				bankDetails += "<br>" + Localization.getLocalizedString("novalnet.bankDetailsIban") + " "
						+ bankdeatailsJsonObject.get("iban").toString();

				bankDetails += "<br>" + Localization.getLocalizedString("novalnet.bankDetailsBic") + " "
						+ bankdeatailsJsonObject.get("bic").toString();

				bankDetails += "<br>" + Localization.getLocalizedString("novalnet.bankDetailsBank") + " "
						+ bankdeatailsJsonObject.get("bank_name").toString();

				bankDetails += "<br>" + Localization.getLocalizedString("novalnet.bankPlace") + " "
						+ bankdeatailsJsonObject.get("bank_place").toString();

				bankDetails += "<br>" + Localization.getLocalizedString("novalnet.bankDetailspaymentRefernceMulti") + "<br>"
						+ Localization.getLocalizedString("novalnet.bankDetailsPaymentReference") + " : "
						+ transactionJsonObject.get("tid").toString() + "<br>";

				if (bankdeatailsJsonObject.has("qr_image") && bankdeatailsJsonObject.optString("qr_image") != null
						&& !bankdeatailsJsonObject.optString("qr_image").isEmpty())
				{
					bankDetails += "<br>";
					bankDetails += Localization.getLocalizedString("novalnet.qrCodeComments");
					bankDetails += "<br>";
					bankDetails += "<img alt='nn_qr_code' src='" + bankdeatailsJsonObject.optString("qr_image") + "'>";
					bankDetails += "<br>";
				}

				if ("novalnetGuaranteedInvoice".equals(currentPayment)
						&& "75".equals(transactionJsonObject.get("status_code").toString()))
				{
					bankDetails = "<br>" + Localization.getLocalizedString("novalnet.status75");
				}
			}
			else if ("novalnetMultibanco".equals(currentPayment) && transactionJsonObject.has("partner_payment_reference"))
			{
				bankDetails += "<br>" + Localization.getLocalizedString("novalnet.multibancocomments1") + " "
						+ cartData.getTotalPriceWithTax().getFormattedValue()
						+ Localization.getLocalizedString("novalnet.multibancocomments2") + "<br>"
						+ Localization.getLocalizedString("novalnet.bankDetailsPaymentReference") + " : "
						+ transactionJsonObject.get("partner_payment_reference").toString() + "<br>"
						+ Localization.getLocalizedString("novalnet.multibancosupplierid") + " : "
						+ transactionJsonObject.get("service_supplier_id").toString();
			}

			else if ("novalnetGuaranteedDirectDebitSepa".equals(currentPayment)
					&& "75".equals(transactionJsonObject.get("status_code").toString()))
			{
				bankDetails += "<br>" + Localization.getLocalizedString("novalnet.sepa.status75");
			}

			getSessionService().setAttribute("tid", orderComments + bankDetails);
			getSessionService().setAttribute("email", customerJsonObject.getString("email"));

			if (transactionJsonObject.has("payment_data"))
			{
				JSONObject paymentDataJsonObject = transactionJsonObject.getJSONObject("payment_data");
				if (paymentDataJsonObject.has("token") && !novalnetFacade.isGuestUser())
				{
					boolean storePaymentData = getSessionService().getAttribute(currentPayment + "StorePaymentData");
					if (storePaymentData == true)
					{
						novalnetFacade.handleReferenceTransactionInfo(response, customerNo, currentPayment);
					}
				}
			}
			final OrderData orderData;

			orderData = novalnetFacade.saveOrderData(orderComments, currentPayment, transactionJsonObject.get("status").toString(),
					orderAmountCent, transactionJsonObject.getString("currency"), transactionJsonObject.get("tid").toString(),
					customerJsonObject.getString("email"), addressData, bankDetails);


			transactionParameters.put("tid", transactionJsonObject.get("tid"));
			transactionParameters.put("order_no", orderData.getCode());

			dataParameters.put("transaction", transactionParameters);
			dataParameters.put("custom", customParameters);

			jsonString = gson.toJson(dataParameters);
			StringBuilder responseString = novalnetFacade.sendRequest(TRANSACTION_UPDATE_URL, jsonString);
			LOGGER.info("Novalnet response: {}" + responseString.toString());

			transactionParameters.clear();
			dataParameters.clear();

			return confirmationPageURL(orderData);
		}
		else
		{
			final String statusMessage = resultJsonObject.get("status_text").toString() != null
					? resultJsonObject.get("status_text").toString()
					: resultJsonObject.get("status_desc").toString();
			getSessionService().setAttribute("novalnetCheckoutError", statusMessage);
			return getCheckoutStep().previousStep();
		}
	}


	/**
	 * Get the value of the remote ip address.
	 *
	 * @param request
	 *           Request value.
	 * @return Remote IP address
	 */
	public static String getRemoteIpAddr(HttpServletRequest request)
	{
		try
		{
			InetAddress ipAddr = InetAddress.getByName(request.getRemoteAddr());
			if (ipAddr instanceof Inet4Address)
			{
				return ipAddr.getHostAddress();
			}
			else if (ipAddr instanceof Inet6Address)
			{
				return "127.0.0.1";
			}
		}
		catch (UnknownHostException ex)
		{
			LOGGER.error("UnknownHostException ", ex);
		}
		return "127.0.0.1";
	}

	public static Map parseResponse(String response)
	{
		Map<String, String> parsedResponse = new HashMap<>();
		String parameters[] = response.split("&");
		for (String parameter : parameters)
		{
			String p[] = parameter.split("=");
			if (p != null && p.length > 1)
			{
				parsedResponse.put(p[0], p[1]);
			}
		}
		return parsedResponse;
	}

	public static String formatDate(int date)
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
		Calendar calendarInsatance = Calendar.getInstance();
		calendarInsatance.add(calendarInsatance.DATE, date);
		return dateFormat.format(calendarInsatance.getTime());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.hybris.platform.storefront.controllers.pages.AbstractCheckoutController#redirectToOrderConfirmationPage
	 * (javax.servlet.http.HttpServletRequest)
	 */
	protected String confirmationPageURL(final OrderData orderData)
	{
		return REDIRECT_URL_ORDER_CONFIRMATION
				+ (getCheckoutCustomerStrategy().isAnonymousCheckout() ? orderData.getGuid() : orderData.getCode());
	}

	/**
	 * Validates the order form before to filter out invalid order states
	 *
	 * @param placeOrderForm
	 *           The spring form of the order being submitted
	 * @param model
	 *           A spring Model
	 * @return True if the order form is invalid and false if everything is valid.
	 */
	protected boolean validateOrderForm(final PlaceOrderForm placeOrderForm, final Model model)
	{
		final String securityCode = placeOrderForm.getSecurityCode();
		boolean invalid = false;

		if (getCheckoutFlowFacade().hasNoDeliveryAddress())
		{
			GlobalMessages.addErrorMessage(model, "checkout.deliveryAddress.notSelected");
			invalid = true;
		}

		if (getCheckoutFlowFacade().hasNoDeliveryMode())
		{
			GlobalMessages.addErrorMessage(model, "checkout.deliveryMethod.notSelected");
			invalid = true;
		}

		if (!getCheckoutFlowFacade().hasNoPaymentInfo()
				&& CheckoutPciOptionEnum.DEFAULT.equals(getCheckoutFlowFacade().getSubscriptionPciOption())
				&& StringUtils.isBlank(securityCode))
		{
			GlobalMessages.addErrorMessage(model, "checkout.paymentMethod.noSecurityCode");
			invalid = true;
		}


		if (!placeOrderForm.isTermsCheck())
		{
			GlobalMessages.addErrorMessage(model, "checkout.error.terms.not.accepted");
			invalid = true;
			return invalid;
		}
		final CartData cartData = getCheckoutFacade().getCheckoutCart();

		if (!getCheckoutFacade().containsTaxValues())
		{
			LOGGER.error(String.format(
					"Cart %s does not have any tax values, which means the tax cacluation was not properly done, placement of order can't continue",
					cartData.getCode()));
			GlobalMessages.addErrorMessage(model, "checkout.error.tax.missing");
			invalid = true;
		}

		if (!cartData.isCalculated())
		{
			LOGGER.error(
					String.format("Cart %s has a calculated flag of FALSE, placement of order can't continue", cartData.getCode()));
			GlobalMessages.addErrorMessage(model, "checkout.error.cart.notcalculated");
			invalid = true;
		}

		return invalid;
	}

	@GetMapping(value = "/back")
	@RequireHardLogIn
	@Override
	public String back(final RedirectAttributes redirectAttributes)
	{
		return getCheckoutStep().previousStep();
	}

	@GetMapping(value = "/next")
	@RequireHardLogIn
	@Override
	public String next(final RedirectAttributes redirectAttributes)
	{
		return getCheckoutStep().nextStep();
	}

	protected CheckoutStep getCheckoutStep()
	{
		return getCheckoutStep(SUMMARY);
	}

	public BaseStoreModel getBaseStoreModel()
	{
		return getBaseStoreService().getCurrentBaseStore();
	}

	public static String getPaymentType(String paymentName)
	{
		final Map<String, String> paymentType = new HashMap<>();
		paymentType.put("novalnetCreditCard", "CREDITCARD");
		paymentType.put("novalnetDirectDebitSepa", "DIRECT_DEBIT_SEPA");
		paymentType.put("novalnetDirectDebitAch", "DIRECT_DEBIT_ACH");
		paymentType.put("novalnetGuaranteedDirectDebitSepa", "GUARANTEED_DIRECT_DEBIT_SEPA");
		paymentType.put("novalnetInvoice", "INVOICE");
		paymentType.put("novalnetGuaranteedInvoice", "GUARANTEED_INVOICE");
		paymentType.put("novalnetPrepayment", "PREPAYMENT");
		paymentType.put("novalnetPayPal", "PAYPAL");
		paymentType.put("novalnetOnlineBankTransfer", "ONLINE_BANK_TRANSFER");
		paymentType.put("novalnetBancontact", "BANCONTACT");
		paymentType.put("novalnetMultibanco", "MULTIBANCO");
		paymentType.put("novalnetIdeal", "IDEAL");
		paymentType.put("novalnetGooglePay", "GOOGLEPAY");
		paymentType.put("novalnetApplePay", "APPLEPAY");
		paymentType.put("novalnetTwint", "TWINT");
		paymentType.put("novalnetMbWay", "MBWAY");
		paymentType.put("novalnetAlipay", "ALIPAY");
		paymentType.put("novalnetTrustly", "TRUSTLY");
		paymentType.put("novalnetBlik", "BLIK");
		paymentType.put("novalnetWechatPay", "WECHATPAY");
		paymentType.put("novalnetEps", "EPS");
		paymentType.put("novalnetPrzelewy24", "PRZELEWY24");
		paymentType.put("novalnetPostFinanceCard", "POSTFINANCE_CARD");
		paymentType.put("novalnetPostFinance", "POSTFINANCE");
		return paymentType.get(paymentName);
	}

	public BaseStoreService getBaseStoreService()
	{
		return baseStoreService;
	}

	public void setBaseStoreService(BaseStoreService baseStoreService)
	{
		this.baseStoreService = baseStoreService;
	}

	protected void setupPageModel(final Model model) throws CMSItemNotFoundException
	{
		model.addAttribute("metaRobots", "noindex,nofollow");
		model.addAttribute("hasNoPaymentInfo", Boolean.valueOf(getCheckoutFlowFacade().hasNoPaymentInfo()));
		prepareDataForPage(model);
		model.addAttribute(WebConstants.BREADCRUMBS_KEY,
				getResourceBreadcrumbBuilder().getBreadcrumbs("checkout.multi.paymentMethod.breadcrumb"));
		final ContentPageModel contentPage = getContentPageForLabelOrId(MULTI_CHECKOUT_SUMMARY_CMS_PAGE_LABEL);
		storeCmsPageInModel(model, contentPage);
		setUpMetaDataForContentPage(model, contentPage);
		setCheckoutStepLinksForModel(model, getCheckoutStep());
	}



}
