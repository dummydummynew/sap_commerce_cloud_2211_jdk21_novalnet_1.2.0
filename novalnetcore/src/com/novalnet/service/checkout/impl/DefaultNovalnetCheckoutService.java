package com.novalnet.service.checkout.impl;

import de.hybris.platform.acceleratorfacades.order.impl.DefaultAcceleratorCheckoutFacade;
import de.hybris.platform.commercefacades.consent.ConsentFacade;
import de.hybris.platform.commercefacades.consent.CustomerConsentDataStrategy;
import de.hybris.platform.commercefacades.customer.CustomerFacade;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.product.ProductFacade;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commerceservices.enums.CustomerType;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.orderhistory.model.OrderHistoryEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.session.SessionService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.novalnet.model.NovalnetAliPayPaymentModeModel;
import com.novalnet.model.NovalnetApplePayPaymentModeModel;
import com.novalnet.model.NovalnetBancontactPaymentModeModel;
import com.novalnet.model.NovalnetBlikPaymentModeModel;
import com.novalnet.model.NovalnetCallbackInfoModel;
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
import com.novalnet.model.NovalnetPaymentInfoModel;
import com.novalnet.model.NovalnetPostFinanceCardPaymentModeModel;
import com.novalnet.model.NovalnetPostFinancePaymentModeModel;
import com.novalnet.model.NovalnetPrepaymentPaymentModeModel;
import com.novalnet.model.NovalnetPrzelewy24PaymentModeModel;
import com.novalnet.model.NovalnetTrustlyPaymentModeModel;
import com.novalnet.model.NovalnetTwintPaymentModeModel;
import com.novalnet.model.NovalnetWechatPayPaymentModeModel;
import com.novalnet.service.checkout.NovalnetCheckoutService;
import com.novalnet.service.order.NovalnetOrderService;
import com.novalnet.service.payment.NovalnetPaymentService;

import jakarta.annotation.Resource;


public class DefaultNovalnetCheckoutService extends DefaultAcceleratorCheckoutFacade implements NovalnetCheckoutService
{
	public static final String REDIRECT_PREFIX = "redirect:";
	public static final String ROOT = "/";

	@Resource
	private Converter<AddressData, AddressModel> addressReverseConverter;

	@Resource
	private NovalnetPaymentService novalnetPaymentService;

	@Resource
	private PaymentModeService paymentModeService;

	@Resource
	private NovalnetOrderService novalnetOrderService;

	@Resource
	private SessionService sessionService;

	@Resource(name = "consentFacade")
	protected ConsentFacade consentFacade;

	@Resource(name = "customerFacade")
	private CustomerFacade customerFacade;

	@Resource(name = "customerConsentDataStrategy")
	protected CustomerConsentDataStrategy customerConsentDataStrategy;

	@Resource(name = "orderFacade")
	private OrderFacade orderFacade;

	@Resource(name = "productFacade")
	private ProductFacade productFacade;

	private CartService cartService;

	@Override
	public void setCartService(final CartService cartService)
	{
		super.setCartService(cartService);
		this.cartService = cartService;
	}

	@Override
	public OrderData saveOrderData(String orderComments, String currentPayment, String transactionStatus, int orderAmountCent,
			String currency, String transactionID, String email, AddressData addressData, String bankDetails)
			throws InvalidCartException
	{
		CartModel cartModel = cartService.getSessionCart();
		UserModel currentUser = getCurrentUserForCheckout();
		String backendTransactionComments = orderComments.replaceAll("<br\\s*/?>", " ");

		AddressModel billingAddress = getModelService().create(AddressModel.class);
		billingAddress = addressReverseConverter.convert(addressData, billingAddress);

		billingAddress.setEmail(email);
		billingAddress.setOwner(cartModel);

		NovalnetPaymentInfoModel paymentInfoModel = new NovalnetPaymentInfoModel();

		paymentInfoModel.setBillingAddress(billingAddress);
		paymentInfoModel.setPaymentEmailAddress(email);
		paymentInfoModel.setDuplicate(Boolean.FALSE);
		paymentInfoModel.setSaved(Boolean.TRUE);
		paymentInfoModel.setUser(currentUser);
		paymentInfoModel.setPaymentInfo(orderComments);
		paymentInfoModel.setOrderHistoryNotes(bankDetails);
		paymentInfoModel.setPaymentProvider(currentPayment);
		paymentInfoModel.setPaymentGatewayStatus(transactionStatus);

		cartModel.setPaymentInfo(paymentInfoModel);
		paymentInfoModel.setCode("");

		List<PaymentTransactionEntryModel> paymentTransactionEntries = new ArrayList<>();

		PaymentTransactionEntryModel orderTransactionEntry = novalnetPaymentService.createTransactionEntry(transactionID, cartModel,
				orderAmountCent, backendTransactionComments, currency);

		paymentTransactionEntries.add(orderTransactionEntry);

		PaymentTransactionModel paymentTransactionModel = new PaymentTransactionModel();
		paymentTransactionModel.setPaymentProvider(currentPayment);
		paymentTransactionModel.setRequestId(transactionID);
		paymentTransactionModel.setEntries(paymentTransactionEntries);
		paymentTransactionModel.setOrder(cartModel);
		paymentTransactionModel.setInfo(paymentInfoModel);

		cartModel.setPaymentTransactions(Arrays.asList(paymentTransactionModel));

		beforePlaceOrder(cartModel);

		OrderModel orderModel = placeOrder(cartModel);
		String orderNumber = orderModel.getCode();

		novalnetOrderService.updateOrderStatus(orderNumber, paymentInfoModel);

		PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(currentPayment);

		if ("novalnetCreditCard".equals(currentPayment))
		{
			NovalnetCreditCardPaymentModeModel novalnetPaymentMethod = (NovalnetCreditCardPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetDirectDebitSepa".equals(currentPayment))
		{
			NovalnetDirectDebitSepaPaymentModeModel novalnetPaymentMethod = (NovalnetDirectDebitSepaPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetDirectDebitAch".equals(currentPayment))
		{
			NovalnetDirectDebitAchPaymentModeModel novalnetPaymentMethod = (NovalnetDirectDebitAchPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetGuaranteedInvoice".equals(currentPayment))
		{
			NovalnetGuaranteedInvoicePaymentModeModel novalnetPaymentMethod = (NovalnetGuaranteedInvoicePaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetGuaranteedDirectDebitSepa".equals(currentPayment))
		{
			NovalnetGuaranteedDirectDebitSepaPaymentModeModel novalnetPaymentMethod = (NovalnetGuaranteedDirectDebitSepaPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetPayPal".equals(currentPayment))
		{
			NovalnetPayPalPaymentModeModel novalnetPaymentMethod = (NovalnetPayPalPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetInvoice".equals(currentPayment))
		{
			NovalnetInvoicePaymentModeModel novalnetPaymentMethod = (NovalnetInvoicePaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetPrepayment".equals(currentPayment))
		{
			NovalnetPrepaymentPaymentModeModel novalnetPaymentMethod = (NovalnetPrepaymentPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetOnlineBankTransfer".equals(currentPayment))
		{
			NovalnetOnlineBankTransferPaymentModeModel novalnetPaymentMethod = (NovalnetOnlineBankTransferPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetMultibanco".equals(currentPayment))
		{
			NovalnetMultibancoPaymentModeModel novalnetPaymentMethod = (NovalnetMultibancoPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetBancontact".equals(currentPayment))
		{
			NovalnetBancontactPaymentModeModel novalnetPaymentMethod = (NovalnetBancontactPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetPostFinanceCard".equals(currentPayment))
		{
			NovalnetPostFinanceCardPaymentModeModel novalnetPaymentMethod = (NovalnetPostFinanceCardPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetPostFinance".equals(currentPayment))
		{
			NovalnetPostFinancePaymentModeModel novalnetPaymentMethod = (NovalnetPostFinancePaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetIdeal".equals(currentPayment))
		{
			NovalnetIdealPaymentModeModel novalnetPaymentMethod = (NovalnetIdealPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetTwint".equals(currentPayment))
		{
			NovalnetTwintPaymentModeModel novalnetPaymentMethod = (NovalnetTwintPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetMbWay".equals(currentPayment))
		{
			NovalnetMbWayPaymentModeModel novalnetPaymentMethod = (NovalnetMbWayPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetTrustly".equals(currentPayment))
		{
			NovalnetTrustlyPaymentModeModel novalnetPaymentMethod = (NovalnetTrustlyPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetBlik".equals(currentPayment))
		{
			NovalnetBlikPaymentModeModel novalnetPaymentMethod = (NovalnetBlikPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetWechatPay".equals(currentPayment))
		{
			NovalnetWechatPayPaymentModeModel novalnetPaymentMethod = (NovalnetWechatPayPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetAlipay".equals(currentPayment))
		{
			NovalnetAliPayPaymentModeModel novalnetPaymentMethod = (NovalnetAliPayPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetGooglePay".equals(currentPayment))
		{
			NovalnetGooglePayPaymentModeModel novalnetPaymentMethod = (NovalnetGooglePayPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetApplePay".equals(currentPayment))
		{
			NovalnetApplePayPaymentModeModel novalnetPaymentMethod = (NovalnetApplePayPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetEps".equals(currentPayment))
		{
			NovalnetEpsPaymentModeModel novalnetPaymentMethod = (NovalnetEpsPaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}
		else if ("novalnetPrzelewy24".equals(currentPayment))
		{
			NovalnetPrzelewy24PaymentModeModel novalnetPaymentMethod = (NovalnetPrzelewy24PaymentModeModel) paymentModeModel;
			orderModel.setPaymentMode(novalnetPaymentMethod);
		}

		paymentInfoModel.setPaymentInfo(orderComments);
		paymentInfoModel.setPaymentProvider(currentPayment);
		paymentInfoModel.setPaymentGatewayStatus(transactionStatus);
		paymentInfoModel.setOrderHistoryNotes(bankDetails);

		orderModel.setStatusInfo(backendTransactionComments);
		paymentInfoModel.setCode(orderNumber);

		getModelService().saveAll(paymentInfoModel, cartModel, billingAddress);

		OrderHistoryEntryModel orderEntry = getModelService().create(OrderHistoryEntryModel.class);

		orderEntry.setTimestamp(new Date());
		orderEntry.setOrder(orderModel);
		orderEntry.setDescription(backendTransactionComments);

		orderModel.setPaymentInfo(paymentInfoModel);

		int orderPaidAmount;

		String[] bankPayments =
		{ "novalnetInvoice", "novalnetPrepayment", "novalnetGuaranteedDirectDebitSepa", "novalnetGuaranteedInvoice" };

		boolean isInvoicePrepayment = Arrays.asList(bankPayments).contains(currentPayment);

		String[] pendingStatusCode =
		{ "PENDING" };

		if (isInvoicePrepayment || Arrays.asList(pendingStatusCode).contains(transactionStatus))
		{
			orderPaidAmount = 0;
		}
		else
		{
			orderPaidAmount = orderAmountCent;
		}

		orderModel.setPaidAmount(orderPaidAmount);

		getModelService().saveAll(orderModel, orderEntry);

		afterPlaceOrder(cartModel, orderModel);

		long callbackInfoTid = Long.parseLong(transactionID);

		NovalnetCallbackInfoModel novalnetCallbackInfo = new NovalnetCallbackInfoModel();

		novalnetCallbackInfo.setPaymentType(currentPayment);
		novalnetCallbackInfo.setOrderAmount(orderAmountCent);
		novalnetCallbackInfo.setCallbackTid(callbackInfoTid);
		novalnetCallbackInfo.setOrginalTid(callbackInfoTid);
		novalnetCallbackInfo.setPaidAmount(orderPaidAmount);
		novalnetCallbackInfo.setOrderNo(orderNumber);

		getModelService().save(novalnetCallbackInfo);

		return getOrderConverter().convert(orderModel);
	}

	@Override
	public void saveData(AddressModel billingAddress, CartModel cartModel)
	{
		getModelService().saveAll(billingAddress, cartModel);
	}

	@Override
	public AddressModel getBillingAddress()
	{
		return getModelService().create(AddressModel.class);
	}

	@Override
	public Boolean isGuestUser()
	{
		CartModel cart = cartService.getSessionCart();
		UserModel user = cart.getUser();

		return user instanceof CustomerModel && ((CustomerModel) user).getType() == CustomerType.GUEST;
	}

	@Override
	public String getGuestEmail()
	{
		CartModel cart = cartService.getSessionCart();
		UserModel user = cart.getUser();

		return user instanceof CustomerModel && ((CustomerModel) user).getType() == CustomerType.GUEST
				? user.getUid().substring(user.getUid().indexOf('|') + 1)
				: null;
	}

	@Override
	public CartModel getNovalnetCheckoutCart()
	{
		return getCart();
	}

	@Override
	public UserModel getCurrentUser()
	{
		return getCurrentUserForCheckout();
	}
}
