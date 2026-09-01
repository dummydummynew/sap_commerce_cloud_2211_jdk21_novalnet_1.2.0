package novalnet.novalnetcheckoutaddon.jalo;

import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;

import org.apache.log4j.Logger;

import novalnet.novalnetcheckoutaddon.constants.NovalnetcheckoutaddonConstants;


public class NovalnetcheckoutaddonManager extends GeneratedNovalnetcheckoutaddonManager
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(NovalnetcheckoutaddonManager.class.getName());

	public static final NovalnetcheckoutaddonManager getInstance()
	{
		ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (NovalnetcheckoutaddonManager) em.getExtension(NovalnetcheckoutaddonConstants.EXTENSIONNAME);
	}

}
