package novalnet.novalnetcheckoutaddon.controllers.pages.checkout.steps;

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
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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
import com.novalnet.dto.OrderPaymentCommentsData;
import com.novalnet.dto.payment.request.Custom;
import com.novalnet.dto.payment.request.NovalnetPaymentRequest;
import com.novalnet.dto.payment.request.Transaction;
import com.novalnet.facades.NovalnetPaymentFacade;
import com.novalnet.service.checkout.NovalnetCheckoutService;
import com.novalnet.service.http.NovalnetApiService;
import com.novalnet.service.payment.NovalnetEndpointConfigService;
import com.novalnet.service.payment.NovalnetPaymentService;
import com.novalnet.service.payment.NovalnetTransactionService;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import novalnet.novalnetcheckoutaddon.controllers.NovalnetcheckoutaddonControllerConstants;


@Controller
@RequestMapping(value = "/checkout/multi/novalnet/summary")
public class NovalnetSummaryCheckoutStepController extends AbstractCheckoutStepController
{
	private static final Logger LOGGER = Logger.getLogger(NovalnetSummaryCheckoutStepController.class);

	private static final String SUMMARY = "summary";

	public static final int CONVERT_TO_CENT_OR_SUCCESS_STATUS = 100;
	public static final int PREPAYMENT_FROM_DATE = 7;
	public static final int PREPAYMENT_TILL_DATE = 28;

	protected static final String REDIRECT_URL_ORDER_CONFIRMATION = REDIRECT_PREFIX + "/checkout/novalnet/orderConfirmation/";

	private static final String NOVALNET_GOOGLE_PAY = "novalnetGooglePay";
	private static final String NOVALNET_APPLE_PAY = "novalnetApplePay";
	private static final String NOVALNET_CHECKOUT_ERROR = "novalnetCheckoutError";
	private static final String PAYMENT_DATA = "payment_data";
	private static final String STATUS_TEXT = "status_text";
	private static final String NOVALNET_ORDER_AMOUNT = "novalnetOrderAmount";
	private static final String EMAIL = "email";
	private static final String TRANSACTION = "transaction";

	private static final String STATUS = "status";
	private static final String RESULT = "result";
	private static final String AMOUNT = "amount";
	private static final String CUSTOM = "custom";


	@Resource(name = "baseStoreService")
	private BaseStoreService baseStoreService;

	@Resource(name = "novalnetEndpointConfigService")
	private NovalnetEndpointConfigService novalnetEndpointConfigService;

	@Resource(name = "cartService")
	private CartService cartService;

	@Resource(name = "configurationService")
	private ConfigurationService configurationService;

	@Resource
	private Converter<AddressData, AddressModel> addressReverseConverter;

	@Resource(name = "novalnetCheckoutService")
	private NovalnetCheckoutService novalnetCheckoutService;

	@Resource(name = "novalnetApiService")
	private NovalnetApiService novalnetApiService;

	@Resource
	private PaymentModeService paymentModeService;

	@Resource(name = "novalnetPaymentService")
	private NovalnetPaymentService novalnetPaymentService;

	@Resource(name = "novalnetPaymentFacade")
	private NovalnetPaymentFacade novalnetPaymentFacade;

	@Resource
	private SessionService sessionService;

	@Resource(name = "novalnetTransactionService")
	private NovalnetTransactionService novalnetTransactionService;


	@GetMapping(value = "/enter")
	@RequireHardLogIn
	@PreValidateQuoteCheckoutStep
	@PreValidateCheckoutStep(checkoutStep = SUMMARY)
	public String enterStep(Model model, RedirectAttributes redirectAttributes)
			throws CMSItemNotFoundException, CommerceCartModificationException
	{
		CartData cartData = getCheckoutFacade().getCheckoutCart();

		if (cartData.getEntries() != null && !cartData.getEntries().isEmpty())
		{
			for (OrderEntryData entry : cartData.getEntries())
			{
				String productCode = entry.getProduct().getCode();

				ProductData product = getProductFacade().getProductForCodeAndOptions(productCode, Arrays.asList(ProductOption.BASIC,
						ProductOption.PRICE, ProductOption.VARIANT_MATRIX_BASE, ProductOption.PRICE_RANGE));

				entry.setProduct(product);
			}
		}

		String currentPayment = sessionService.getAttribute("selectedPaymentMethodId");

		if (currentPayment == null || currentPayment.equals(""))
		{
			sessionService.setAttribute(NOVALNET_CHECKOUT_ERROR, "checkout.multi.paymentDetails.notprovided");

			return getCheckoutStep().previousStep();
		}

		model.addAttribute("currentPayment", currentPayment);
		model.addAttribute("cartData", cartData);
		model.addAttribute("allItems", cartData.getEntries());
		model.addAttribute("deliveryAddress", cartData.getDeliveryAddress());
		model.addAttribute("deliveryMode", cartData.getDeliveryMode());
		model.addAttribute("paymentInfo", cartData.getPaymentInfo());

		boolean requestSecurityCode = CheckoutPciOptionEnum.DEFAULT.equals(getCheckoutFlowFacade().getSubscriptionPciOption());

		model.addAttribute("requestSecurityCode", Boolean.valueOf(requestSecurityCode));

		model.addAttribute(new PlaceOrderForm());

		String currency = cartData.getTotalPriceWithTax().getCurrencyIso();
		model.addAttribute("currency", currency);

		NovalnetPaymentRequest request = sessionService.getAttribute("novalnetPaymentRequest");

		if (request != null && request.getCustomer() != null && request.getCustomer().getBilling() != null)
		{
			model.addAttribute("countryCode", request.getCustomer().getBilling().getCountry_code());

			LOGGER.info("countryCode" + request.getCustomer().getBilling().getCountry_code());
		}

		ContentPageModel multiCheckoutSummaryPage = getContentPageForLabelOrId(MULTI_CHECKOUT_SUMMARY_CMS_PAGE_LABEL);

		storeCmsPageInModel(model, multiCheckoutSummaryPage);
		setUpMetaDataForContentPage(model, multiCheckoutSummaryPage);

		model.addAttribute(WebConstants.BREADCRUMBS_KEY,
				getResourceBreadcrumbBuilder().getBreadcrumbs("checkout.multi.summary.breadcrumb"));

		model.addAttribute("metaRobots", "noindex,nofollow");

		setCheckoutStepLinksForModel(model, getCheckoutStep());

		Locale language = JaloSession.getCurrentSession().getSessionContext().getLocale();

		String languageCode = (language != null) ? language.toString().toUpperCase() : "EN";

		model.addAttribute("lang", languageCode);

		BaseStoreModel baseStore = getBaseStoreModel();

		model.addAttribute("novalnetBaseStoreConfiguration", baseStore);

		model.addAttribute("novalnetApplePay", paymentModeService.getPaymentModeForCode("novalnetApplePay"));

		model.addAttribute(NOVALNET_GOOGLE_PAY, paymentModeService.getPaymentModeForCode(NOVALNET_GOOGLE_PAY));

		BigDecimal totalAmount = cartData.getTotalPriceWithTax().getValue();

		BigDecimal orderAmountCents = totalAmount.multiply(BigDecimal.valueOf(CONVERT_TO_CENT_OR_SUCCESS_STATUS)).setScale(0,
				RoundingMode.HALF_UP);

		Integer orderAmountCent = orderAmountCents.intValue();

		sessionService.setAttribute(NOVALNET_ORDER_AMOUNT, orderAmountCent);

		model.addAttribute("orderAmount", orderAmountCent);

		return NovalnetcheckoutaddonControllerConstants.CheckoutSummaryPage;
	}


	@PostMapping(value ={ "/bookWalletTransaction" })
	@RequireHardLogIn
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public String bookWalletTransaction(Model model, HttpServletRequest request, RedirectAttributes redirectModel)
			throws CMSItemNotFoundException, InvalidCartException, CommerceCartModificationException, Exception
	{
		BaseStoreModel baseStore = getBaseStoreModel();

		CartData cartData = getCheckoutFacade().getCheckoutCart();

		String customerNo = JaloSession.getCurrentSession().getUser().getPK().toString();

		Integer orderAmountCent = sessionService.getAttribute(NOVALNET_ORDER_AMOUNT);

		return  novalnetPaymentFacade.bookWalletTransaction(request, baseStore, customerNo, orderAmountCent, cartData);

	}


	@PostMapping(value = "/placeOrder")
	@PreValidateQuoteCheckoutStep
	@RequireHardLogIn
	public String placeOrder(@ModelAttribute("placeOrderForm")
	PlaceOrderForm placeOrderForm, Model model, HttpServletRequest request, RedirectAttributes redirectModel)
			throws CMSItemNotFoundException, InvalidCartException, CommerceCartModificationException
	{
		CartData cartData = getCheckoutFacade().getCheckoutCart();

		BaseStoreModel baseStore = getBaseStoreModel();

		String customerNo = JaloSession.getCurrentSession().getUser().getPK().toString();

		Locale language = JaloSession.getCurrentSession().getSessionContext().getLocale();

		String languageCode = (language != null) ? language.toString().toUpperCase() : "EN";

		String currentPayment = sessionService.getAttribute("selectedPaymentMethodId");

		LOGGER.info("Selected Payment Method ID: {}" + currentPayment);

		Integer orderAmountCent = sessionService.getAttribute(NOVALNET_ORDER_AMOUNT);

		String[] walletPayments ={ NOVALNET_GOOGLE_PAY};

		Gson gson = new GsonBuilder().create();

		String jsonString = "";

		StringBuilder response;

		if (Arrays.asList(walletPayments).contains(currentPayment))
		{
			Transaction detailsTransaction = new Transaction();

			Custom custom = new Custom();

			custom.setLang(languageCode);

			Map<String, Object> dataParameters = new HashMap<>();

			dataParameters.put(TRANSACTION, detailsTransaction);

			dataParameters.put(CUSTOM, custom);

			jsonString = gson.toJson(dataParameters);

			response = novalnetApiService.sendRequest(novalnetEndpointConfigService.getTransactionDetailsUrl(), jsonString);

			LOGGER.info("Novalnet wallet transaction details response: " + response);

			JSONObject responseJson = new JSONObject(response.toString());

			if (responseJson.has(TRANSACTION))
			{
				JSONObject transaction = responseJson.getJSONObject(TRANSACTION);

				LOGGER.info("transaction: " + transaction);

				if (transaction.has(PAYMENT_DATA))
				{
					JSONObject paymentData = transaction.getJSONObject(PAYMENT_DATA);

					LOGGER.info("payment data: " + paymentData);

					LOGGER.info("Current payment: " + currentPayment);

					if (paymentData.has("token") && !novalnetCheckoutService.isGuestUser())
					{
						Boolean storePaymentData = sessionService.getAttribute(currentPayment + "StorePaymentData");

						LOGGER.info("Store payment data for {}: {}" + currentPayment + storePaymentData);

						if (Boolean.TRUE.equals(storePaymentData))
						{
							LOGGER.info("Storing wallet payment token for: " + currentPayment);

							novalnetPaymentService.handleReferenceTransactionInfo(response, customerNo, currentPayment);
						}
					}
				}
			}

			sessionService.removeAttribute("wallet_tid");
		}
		else
		{
			response = novalnetPaymentFacade.createTransaction(request, baseStore, currentPayment, customerNo, orderAmountCent,
					cartData);

			LOGGER.info(response);
		}

		JSONObject tomJsonObject = new JSONObject(response.toString());

		JSONObject resultJsonObject = tomJsonObject.getJSONObject(RESULT);

		JSONObject transactionJsonObject = tomJsonObject.getJSONObject(TRANSACTION);
		
		if (transactionJsonObject.has(AMOUNT))
		{
		    orderAmountCent = transactionJsonObject.getInt(AMOUNT);
		}

		if (!String.valueOf(CONVERT_TO_CENT_OR_SUCCESS_STATUS).equals(resultJsonObject.get("status_code").toString()))
		{
			String statMessage = resultJsonObject.get(STATUS_TEXT).toString() != null ? resultJsonObject.get(STATUS_TEXT).toString()
					: resultJsonObject.get("status_desc").toString();

			sessionService.setAttribute(NOVALNET_CHECKOUT_ERROR, statMessage);

			return getCheckoutStep().previousStep();
		}

		if (resultJsonObject.has("redirect_url"))
		{
			String redirectURL = resultJsonObject.get("redirect_url").toString();

			setupPageModel(model);

			model.addAttribute("paygateUrl", redirectURL);

			sessionService.setAttribute("txn_secret", transactionJsonObject.get("txn_secret").toString());

			sessionService.setAttribute("txn_check", baseStore.getNovalnetPaymentAccessKey().trim());

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

		String[] successStatus ={ "CONFIRMED", "ON_HOLD", "PENDING" };

		if (Arrays.asList(successStatus).contains(transactionJsonObject.get(STATUS).toString()))
		{
			OrderPaymentCommentsData orderCommentsResult = novalnetTransactionService.buildOrderAndPaymentComments(currentPayment,
					transactionJsonObject, cartData);

			String orderComments = orderCommentsResult.getOrderComments();

			String bankDetails = orderCommentsResult.getBankDetails();

			AddressData addressData = sessionService.getAttribute("novalnetAddressData");

			sessionService.setAttribute("tid", orderComments + bankDetails);

			sessionService.setAttribute(EMAIL, customerJsonObject.getString(EMAIL));

			if (transactionJsonObject.has(PAYMENT_DATA))
			{
				JSONObject paymentDataJsonObject = transactionJsonObject.getJSONObject(PAYMENT_DATA);

				if (paymentDataJsonObject.has("token") && !novalnetCheckoutService.isGuestUser())
				{
					boolean storePaymentData = sessionService.getAttribute(currentPayment + "StorePaymentData");

					if (storePaymentData )
					{
						novalnetPaymentService.handleReferenceTransactionInfo(response, customerNo, currentPayment);
					}
				}
			}

			OrderData orderData = novalnetCheckoutService.saveOrderData(orderComments, currentPayment,
					transactionJsonObject.get(STATUS).toString(), orderAmountCent, transactionJsonObject.getString("currency"),
					transactionJsonObject.get("tid").toString(), customerJsonObject.getString(EMAIL), addressData, bankDetails);

			Transaction updateTransaction = new Transaction();

			updateTransaction.setTid(transactionJsonObject.get("tid").toString());

			updateTransaction.setOrder_no(orderData.getCode());

			Map<String, Object> updateRequest = new HashMap<>();

			updateRequest.put(TRANSACTION, updateTransaction);

			jsonString = gson.toJson(updateRequest);

			StringBuilder responseString = novalnetApiService.sendRequest(novalnetEndpointConfigService.getTransactionUpdateUrl(),
					jsonString);

			LOGGER.info("Novalnet response: {}" + responseString.toString());

			return confirmationPageURL(orderData);
		}
		else
		{
			String statusMessage = resultJsonObject.get(STATUS_TEXT).toString() != null
					? resultJsonObject.get(STATUS_TEXT).toString()
					: resultJsonObject.get("status_desc").toString();

			sessionService.setAttribute(NOVALNET_CHECKOUT_ERROR, statusMessage);

			return getCheckoutStep().previousStep();
		}
	}


	protected String confirmationPageURL(OrderData orderData)
	{
		return REDIRECT_URL_ORDER_CONFIRMATION
				+ (getCheckoutCustomerStrategy().isAnonymousCheckout() ? orderData.getGuid() : orderData.getCode());
	}


	protected boolean validateOrderForm(PlaceOrderForm placeOrderForm, Model model)
	{
		String securityCode = placeOrderForm.getSecurityCode();

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

		CartData cartData = getCheckoutFacade().getCheckoutCart();

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
	public String back(RedirectAttributes redirectAttributes)
	{
		return getCheckoutStep().previousStep();
	}


	@GetMapping(value = "/next")
	@RequireHardLogIn
	@Override
	public String next(RedirectAttributes redirectAttributes)
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


	public BaseStoreService getBaseStoreService()
	{
		return baseStoreService;
	}


	public void setBaseStoreService(BaseStoreService baseStoreService)
	{
		this.baseStoreService = baseStoreService;
	}


	protected void setupPageModel(Model model) throws CMSItemNotFoundException
	{
		model.addAttribute("metaRobots", "noindex,nofollow");

		model.addAttribute("hasNoPaymentInfo", Boolean.valueOf(getCheckoutFlowFacade().hasNoPaymentInfo()));

		prepareDataForPage(model);

		model.addAttribute(WebConstants.BREADCRUMBS_KEY,
				getResourceBreadcrumbBuilder().getBreadcrumbs("checkout.multi.paymentMethod.breadcrumb"));

		ContentPageModel contentPage = getContentPageForLabelOrId(MULTI_CHECKOUT_SUMMARY_CMS_PAGE_LABEL);

		storeCmsPageInModel(model, contentPage);

		setUpMetaDataForContentPage(model, contentPage);

		setCheckoutStepLinksForModel(model, getCheckoutStep());
	}
}
