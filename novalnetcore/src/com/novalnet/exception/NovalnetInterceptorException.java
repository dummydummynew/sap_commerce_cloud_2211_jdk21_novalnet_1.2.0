/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.exception;


import de.hybris.platform.servicelayer.interceptor.InterceptorException;


public class NovalnetInterceptorException extends InterceptorException
{
	private static final long serialVersionUID = 1L;

	public NovalnetInterceptorException(final String message)
	{
		super(message);
	}

	public NovalnetInterceptorException(final String message, final Throwable cause)
	{
		super(message, cause);
	}
}