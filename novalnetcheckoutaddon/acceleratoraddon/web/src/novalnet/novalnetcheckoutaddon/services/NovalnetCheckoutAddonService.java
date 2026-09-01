/**
 * 
 */
package novalnet.novalnetcheckoutaddon.services;

import de.hybris.platform.acceleratorstorefrontcommons.forms.GuestRegisterForm;
import de.hybris.platform.commercefacades.order.data.OrderData;

import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public interface NovalnetCheckoutAddonService
{
	String registerGuestUser(GuestRegisterForm form, Model model, HttpServletRequest request, HttpServletResponse response,
			RedirectAttributes redirectModel);

	OrderData getOrderConfirmationDetails(String orderCode, Model model, HttpServletRequest request);

	void processEmailAddress(Model model, OrderData orderDetails);

}
