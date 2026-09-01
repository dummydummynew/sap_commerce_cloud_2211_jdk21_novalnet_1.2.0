package com.novalnet.interceptors;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.servicelayer.interceptor.InterceptorContext;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;
import de.hybris.platform.servicelayer.interceptor.PrepareInterceptor;

import com.novalnet.exception.NovalnetInterceptorException;
import com.novalnet.facades.NovalnetTransactionFacade;


public class TransactionBookingInterceptor implements PrepareInterceptor<OrderModel>
{
	private NovalnetTransactionFacade novalnetTransactionFacade;

	public void setNovalnetTransactionFacade(NovalnetTransactionFacade novalnetTransactionFacade)
	{
		this.novalnetTransactionFacade = novalnetTransactionFacade;
	}

	@Override
	public void onPrepare(OrderModel order, InterceptorContext ctx) throws InterceptorException
	{
		if (!ctx.isModified(order, "bookAmount"))
		{
			return;
		}

		if (order.getBookAmount() == null)
		{
			return;
		}

		try
		{
			novalnetTransactionFacade.bookTransaction(order);
		}
		catch (final Exception e)
		{
			throw new NovalnetInterceptorException("Unable to book Novalnet transaction for Order: " + order.getCode(), e);
		}
	}
}