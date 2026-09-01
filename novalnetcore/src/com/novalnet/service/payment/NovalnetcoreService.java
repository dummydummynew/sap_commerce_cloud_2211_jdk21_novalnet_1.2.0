/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.payment;

public interface NovalnetcoreService
{
	String getHybrisLogoUrl(String logoCode);

	void createLogo(String logoCode);
}
