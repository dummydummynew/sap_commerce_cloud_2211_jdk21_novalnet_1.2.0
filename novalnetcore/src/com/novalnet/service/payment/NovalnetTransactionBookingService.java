/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.payment;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;


public interface NovalnetTransactionBookingService
{
	void bookTransaction(OrderModel order) throws InterceptorException;
}