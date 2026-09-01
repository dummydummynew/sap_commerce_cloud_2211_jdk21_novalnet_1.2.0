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

import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.dto.converter.Converter;

import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.novalnet.facades.NovalnetPaymentFacade;
import com.novalnet.service.checkout.NovalnetCheckoutService;
import com.novalnet.service.http.NovalnetApiService;
import com.novalnet.service.payment.NovalnetPaymentService;
import com.novalnet.util.NovalnetUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;



@Controller
@RequestMapping(value = "/")
public class NovalnetHopPaymentResponseController extends NovalnetPaymentMethodCheckoutStepController
{
	private static final Logger LOGGER = Logger.getLogger(NovalnetHopPaymentResponseController.class);

	@Resource(name = "novalnetPaymentService")
	private NovalnetPaymentService novalnetPaymentService;

	@Resource(name = "novalnetCheckoutService")
	private NovalnetCheckoutService novalnetCheckoutService;

	@Resource(name = "novalnetPaymentFacade")
	private NovalnetPaymentFacade novalnetPaymentFacade;

	protected static final String REDIRECT_URL_ORDER_CONFIRMATION = REDIRECT_PREFIX + "/checkout/novalnet/orderConfirmation/";

	@PostMapping(value = "checkout/multi/novalnet/hop-response")
	@RequireHardLogIn
	public String doHandleHopResponse(final HttpServletRequest request) throws // NOSONAR
	InvalidCartException
	{
		final OrderData orderData;
		try
		{
			orderData = getCheckoutFacade().placeOrder();
		}
		catch (final InvalidCartException e)
		{
			return getCheckoutStep().currentStep();
		}

		return confirmationPageURL(orderData);

	}

	@GetMapping(value = "checkout/multi/novalnet/hop-response")
	@RequireHardLogIn
	public String handleHopResponse(final RedirectAttributes redirectAttributes, final HttpServletRequest request)
	{

		final Map<String, String> resultMap = getRequestParameterMap(request);

		String transactionSecret = getSessionService().getAttribute("txn_secret");

		if (!"".equals(resultMap.get("checksum")) && !"".equals(resultMap.get("tid")) && !"".equals(transactionSecret)
				&& !"".equals(resultMap.get("status")))
		{
			LOGGER.info("Checksum validation started");
			String tokenString = resultMap.get("tid") + transactionSecret + resultMap.get("status")
					+ new StringBuilder(getSessionService().getAttribute("txn_check").toString()).reverse().toString();

			String generatedChecksum = NovalnetUtils.generateChecksum(tokenString);

			if (!generatedChecksum.equals(resultMap.get("checksum")))
			{
				final String statusMessage = "While redirecting some data has been changed. The hash check failed";
				getSessionService().setAttribute("novalnetCheckoutError", statusMessage);
				return getCheckoutStep().currentStep();
			}
			else
			{
				boolean success = novalnetPaymentFacade.processTransaction(resultMap);

				if (!success)
				{
					return getCheckoutStep().currentStep();
				}

				OrderData orderData = getSessionService().getAttribute("novalnetOrderData");
				return confirmationPageURL(orderData);
			}
		}
		else
		{
			final String statusMessage = "While redirecting some data has been changed. The hash check failed";
			getSessionService().setAttribute("novalnetCheckoutError", statusMessage);
			return getCheckoutStep().currentStep();
		}

	}

	protected String confirmationPageURL(final OrderData orderData)
	{
		return REDIRECT_URL_ORDER_CONFIRMATION
				+ (getCheckoutCustomerStrategy().isAnonymousCheckout() ? orderData.getGuid() : orderData.getCode());
	}

}
