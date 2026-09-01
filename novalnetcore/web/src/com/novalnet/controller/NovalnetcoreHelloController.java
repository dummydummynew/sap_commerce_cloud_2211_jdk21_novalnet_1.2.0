/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.controller;

import static com.novalnet.constants.NovalnetcoreConstants.PLATFORM_LOGO_CODE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;

import com.novalnet.service.payment.NovalnetcoreService;


@Controller
public class NovalnetcoreHelloController
{
	@Autowired
	private NovalnetcoreService novalnetcoreService;

	@GetMapping("/")
	public String printWelcome(final ModelMap model)
	{
		model.addAttribute("logoUrl", novalnetcoreService.getHybrisLogoUrl(PLATFORM_LOGO_CODE));
		return "welcome";
	}
}
