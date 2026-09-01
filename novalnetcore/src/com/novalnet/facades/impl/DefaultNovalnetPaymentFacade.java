package com.novalnet.facades.impl;

import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.store.BaseStoreModel;

import java.util.Map;

import org.springframework.ui.Model;

import com.novalnet.dto.NovalnetPaymentDetailsForm;
import com.novalnet.dto.payment.request.Customer;
import com.novalnet.facades.NovalnetPaymentFacade;
import com.novalnet.service.payment.NovalnetPaymentMethodService;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;


public class DefaultNovalnetPaymentFacade implements NovalnetPaymentFacade
{
	@Resource
	private NovalnetPaymentMethodService novalnetPaymentMethodService;

	@Override
	public String callNovalnetMerchantDetails(String productActivationKey, BaseStoreModel baseStore) throws Exception
	{
		return novalnetPaymentMethodService.callNovalnetMerchantDetails(productActivationKey, baseStore);
	}

	@Override
	public void addPaymentProcess(Model model, NovalnetPaymentDetailsForm paymentDetailsForm, CartData cartData)
			throws CMSItemNotFoundException
	{
		novalnetPaymentMethodService.addPaymentProcess(model, paymentDetailsForm, cartData);
	}

	@Override
	public void populateCustomerAddressDetails(Model model, NovalnetPaymentDetailsForm paymentDetailsForm, CartData cartData,
			Customer customer, AddressData addressData)
	{
		novalnetPaymentMethodService.populateCustomerAddressDetails(model, paymentDetailsForm, cartData, customer, addressData);
	}

	@Override
	public boolean processOneClickTokenData(String currentPayment, NovalnetPaymentDetailsForm paymentDetailsForm, Model model,
			CartData cartData, AddressData deliveryAddress) throws CMSItemNotFoundException
	{
		return novalnetPaymentMethodService.processOneClickTokenData(currentPayment, paymentDetailsForm, model, cartData,
				deliveryAddress);
	}

	@Override
	public boolean processTransaction(Map<String, String> resultMap)
	{
		return novalnetPaymentMethodService.processTransaction(resultMap);
	}

	@Override
	public StringBuilder createTransaction(HttpServletRequest request, BaseStoreModel baseStore, String currentPayment,
			String customerNo, Integer orderAmountCent, CartData cartData)
	{
		return novalnetPaymentMethodService.createTransaction(request, baseStore, currentPayment, customerNo, orderAmountCent,
				cartData);
	}

	@Override
	public String bookWalletTransaction(HttpServletRequest request, BaseStoreModel baseStore, String customerNo,
			Integer orderAmountCent, CartData cartData) throws Exception
	{
		return novalnetPaymentMethodService.bookWalletTransaction(request, baseStore, customerNo, orderAmountCent, cartData);
	}
}