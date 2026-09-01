/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.facades;

import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.store.BaseStoreModel;

import java.util.Map;

import org.springframework.ui.Model;

import com.novalnet.dto.NovalnetPaymentDetailsForm;
import com.novalnet.dto.payment.request.Customer;

import jakarta.servlet.http.HttpServletRequest;


public interface NovalnetPaymentFacade
{
	String callNovalnetMerchantDetails(String productActivationKey, BaseStoreModel baseStore) throws Exception;

	void addPaymentProcess(final Model model, final NovalnetPaymentDetailsForm paymentDetailsForm, final CartData cartData)
			throws CMSItemNotFoundException;


	void populateCustomerAddressDetails(Model model, NovalnetPaymentDetailsForm paymentDetailsForm, CartData cartData,
			Customer customer, AddressData addressData);

	boolean processOneClickTokenData(String currentPayment, NovalnetPaymentDetailsForm paymentDetailsForm, Model model,
			CartData cartData, AddressData deliveryAddress) throws CMSItemNotFoundException;

	boolean processTransaction(Map<String, String> resultMap);

	StringBuilder createTransaction(HttpServletRequest request, BaseStoreModel baseStore, String currentPayment, String customerNo,
			Integer orderAmountCent, CartData cartData);


	String bookWalletTransaction(HttpServletRequest request, BaseStoreModel baseStore, String customerNo, Integer orderAmountCent,
			CartData cartData) throws Exception;

}
