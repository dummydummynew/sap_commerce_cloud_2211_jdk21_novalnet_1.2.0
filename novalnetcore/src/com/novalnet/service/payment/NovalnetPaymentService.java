/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.payment;


import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.store.BaseStoreModel;

import java.util.List;

import org.springframework.ui.Model;

import com.novalnet.dto.NovalnetPaymentDetailsForm;
import com.novalnet.model.NovalnetCallbackInfoModel;
import com.novalnet.model.NovalnetPaymentInfoModel;


public interface NovalnetPaymentService
{
	NovalnetPaymentInfoModel getPaymentModel(List<NovalnetPaymentInfoModel> paymentInfo);

	void handleReferenceTransactionInfo(StringBuilder response, String customerNo, String currentPayment);

	String getPaymentName(String currentPayment);

	BaseStoreModel getBaseStoreModel();

	void updatePaymentInfo(List<NovalnetPaymentInfoModel> orderReference, String tidStatus);

	void updateCallbackInfo(long callbackTid, List<NovalnetCallbackInfoModel> orderReference, int orderPaidAmount);

	void updateCancelStatus(final String orderCode);

	PaymentTransactionEntryModel createTransactionEntry(String requestId, CartModel cartModel, int amount,
			String backendTransactionComments, String currencyCode);

	void addPaymentProcess(Model model, NovalnetPaymentDetailsForm paymentDetailsForm, CartData cartData)
			throws CMSItemNotFoundException;

}
