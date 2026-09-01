/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.checkout;

import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.order.InvalidCartException;


public interface NovalnetCheckoutService
{
	OrderData saveOrderData(String orderComments, String currentPayment, String transactionStatus, int orderAmountCent,
			String currency, String transactionID, String email, AddressData addressData, String bankDetails)
			throws InvalidCartException;

	void saveData(AddressModel billingAddress, CartModel cartModel);

	CartModel getNovalnetCheckoutCart();

	UserModel getCurrentUser();

	AddressModel getBillingAddress();

	Boolean isGuestUser();

	String getGuestEmail();
}