/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved
 */
package com.novalnet.backoffice.jalo;

import com.novalnet.backoffice.constants.NovalnetbackofficeConstants;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;

public class NovalnetbackofficeManager extends GeneratedNovalnetbackofficeManager
{
	public static final NovalnetbackofficeManager getInstance()
	{
		ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (NovalnetbackofficeManager) em.getExtension(NovalnetbackofficeConstants.EXTENSIONNAME);
	}
	
}
