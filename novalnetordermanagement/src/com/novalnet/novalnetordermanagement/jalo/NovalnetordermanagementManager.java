/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved
 */
package com.novalnet.novalnetordermanagement.jalo;

import com.novalnet.novalnetordermanagement.constants.NovalnetordermanagementConstants;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;

public class NovalnetordermanagementManager extends GeneratedNovalnetordermanagementManager
{
	public static final NovalnetordermanagementManager getInstance()
	{
		ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (NovalnetordermanagementManager) em.getExtension(NovalnetordermanagementConstants.EXTENSIONNAME);
	}
	
}
