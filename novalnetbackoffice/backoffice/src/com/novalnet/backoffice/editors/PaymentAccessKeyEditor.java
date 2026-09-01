package com.novalnet.backoffice.editors;

import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.store.BaseStoreModel;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventQueue;
import org.zkoss.zk.ui.event.EventQueues;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybris.cockpitng.editors.CockpitEditorRenderer;
import com.hybris.cockpitng.editors.EditorContext;
import com.hybris.cockpitng.editors.EditorListener;
import com.novalnet.facades.NovalnetPaymentFacade;
import com.novalnet.util.NovalnetUtils;


public class PaymentAccessKeyEditor implements CockpitEditorRenderer<String>
{
	private static final Logger LOG = Logger.getLogger(PaymentAccessKeyEditor.class);
	private static final String ERROR_TITLE = "Error";
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final String PAYMENT_KEY_PREFIX = "PAYMENT_KEY_";
	private static final String PRODUCT_KEY_PREFIX = "PRODUCT_KEY_";
	private static final String CONTEXT_PARENT_OBJECT = "parentObject";
	protected static final String NOVALNET_TARIFF_EVENT = "onNovalnetTariffRefresh";

	private ModelService modelService;
	private NovalnetPaymentFacade novalnetPaymentFacade;

	public void setNovalnetPaymentFacade(NovalnetPaymentFacade novalnetPaymentFacade)
	{
		this.novalnetPaymentFacade = novalnetPaymentFacade;
	}

	public void setModelService(ModelService modelService)
	{
		this.modelService = modelService;
	}

	@Override
	public void render(Component parent, EditorContext<String> context, EditorListener<String> listener)
	{
		LOG.info("PaymentAccessKeyEditor initialized");

		BaseStoreModel baseStore = null;
		Object paramObj = context.getParameter(CONTEXT_PARENT_OBJECT);

		if (paramObj instanceof BaseStoreModel basestoremodel)
		{
			baseStore = basestoremodel;
		}

		if (baseStore == null)
		{
			try
			{
				throw new IllegalArgumentException("BaseStore cannot be null");
			}
			catch (IllegalArgumentException e)
			{
				LOG.error("BaseStore cannot be null", e);
				Messagebox.show(e.getMessage(), ERROR_TITLE, Messagebox.OK, Messagebox.ERROR);
				return;
			}
		}

		String storeId = baseStore.getUid();
		LOG.info("storeId: " + storeId);

		Textbox textbox = new Textbox();
		textbox.setWidth("75%");
		textbox.setParent(parent);

		String dbValue = context.getInitialValue();

		if (NovalnetUtils.isPopulated(dbValue))
		{
			textbox.setValue(dbValue);
			Sessions.getCurrent().setAttribute(PAYMENT_KEY_PREFIX + storeId, dbValue.trim());

			try
			{
				processNovalnetMerchantDetails(context, storeId);
			}
			catch (Exception e)
			{
				LOG.error("Error while calling Novalnet API on init", e);
			}
		}

		textbox.addEventListener(Events.ON_CHANGE, event -> {
			String paymentKey = textbox.getValue();

			listener.onValueChanged(paymentKey);
			Sessions.getCurrent().setAttribute(PAYMENT_KEY_PREFIX + storeId, paymentKey.trim());

			try
			{
				processNovalnetMerchantDetails(context, storeId);
			}
			catch (Exception e)
			{
				LOG.error("Error while calling Novalnet API on change", e);
			}
		});
	}

	private void processNovalnetMerchantDetails(EditorContext<String> context, String storeId) throws Exception
	{
		String productKey = (String) Sessions.getCurrent().getAttribute(PRODUCT_KEY_PREFIX + storeId);

		String paymentKey = (String) Sessions.getCurrent().getAttribute(PAYMENT_KEY_PREFIX + storeId);

		if (productKey == null || productKey.trim().isEmpty() || paymentKey == null || paymentKey.trim().isEmpty())
		{
			LOG.warn("Novalnet skipped due to missing keys");
			return;
		}

		LOG.info("Calling Novalnet with both keys");

		Object paramObj = context.getParameter(CONTEXT_PARENT_OBJECT);

		if (!(paramObj instanceof BaseStoreModel))
		{
			LOG.warn("BaseStore not available");
			return;
		}

		BaseStoreModel baseStore = (BaseStoreModel) paramObj;

		String response = novalnetPaymentFacade.callNovalnetMerchantDetails(productKey, baseStore);

		Map<String, Object> jsonMap = MAPPER.readValue(response, Map.class);
		Map<String, Object> result = (Map<String, Object>) jsonMap.get("result");
		String status = (String) result.get("status");

		Map<String, String> tariffMapSession = new HashMap<>();

		if ("SUCCESS".equalsIgnoreCase(status))
		{
			Map<String, Object> merchantMap = (Map<String, Object>) jsonMap.get("merchant");
			Map<String, Object> tariffMap = (Map<String, Object>) merchantMap.get("tariff");
			String clientKey = (String) merchantMap.get("client_key");

			baseStore.setNovalnetClientKey(clientKey);

			modelService.save(baseStore);
			modelService.refresh(baseStore);

			LOG.info("ClientKey saved successfully");

			for (Map.Entry<String, Object> entry : tariffMap.entrySet())
			{
				String tariffId = entry.getKey();
				Map<String, Object> tariff = (Map<String, Object>) entry.getValue();
				String name = (String) tariff.get("name");

				if (name != null)
				{
					tariffMapSession.put(tariffId, name);
				}
			}

			Sessions.getCurrent().setAttribute("NOVALNET_TARIFF_NAMES_" + storeId, tariffMapSession);
		}
		else
		{
			Sessions.getCurrent().setAttribute("NOVALNET_TARIFF_NAMES_" + storeId, new HashMap<>());

			String statustext = (String) result.get("status_text");

			Messagebox.show(statustext, ERROR_TITLE, Messagebox.OK, Messagebox.ERROR);

			LOG.warn("Novalnet API failed: " + statustext);
		}

		EventQueue<Event> queue = EventQueues.lookup(NOVALNET_TARIFF_EVENT, EventQueues.DESKTOP, true);

		queue.publish(new Event(NOVALNET_TARIFF_EVENT));
	}
}
