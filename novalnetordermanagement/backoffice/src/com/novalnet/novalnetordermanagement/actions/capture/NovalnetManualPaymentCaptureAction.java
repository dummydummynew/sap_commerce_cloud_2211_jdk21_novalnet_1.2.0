package com.novalnet.novalnetordermanagement.actions.capture;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.util.localization.Localization;

import org.apache.log4j.Logger;

import com.hybris.backoffice.widgets.notificationarea.NotificationService;
import com.hybris.backoffice.widgets.notificationarea.event.NotificationEvent;
import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.novalnet.dto.NovalnetTransactionResult;
import com.novalnet.facades.NovalnetTransactionFacade;

import jakarta.annotation.Resource;


public class NovalnetManualPaymentCaptureAction implements CockpitAction<OrderModel, OrderModel>
{
	private static final Logger LOG = Logger.getLogger(NovalnetManualPaymentCaptureAction.class);

	private static final String NOTIFICATION_SOURCE = "General";

	@Resource(name = "novalnetTransactionFacade")
	private NovalnetTransactionFacade novalnetTransactionFacade;

	@Resource(name = "notificationService")
	private NotificationService notificationService;

	@Override
	public boolean canPerform(final ActionContext<OrderModel> ctx)
	{
		return novalnetTransactionFacade.canCapture(ctx.getData());
	}

	@Override
	public boolean needsConfirmation(final ActionContext<OrderModel> ctx)
	{
		return true;
	}

	@Override
	public String getConfirmationMessage(final ActionContext<OrderModel> ctx)
	{
		return Localization.getLocalizedString("novalnet.capture");
	}

	@Override
	public ActionResult<OrderModel> perform(final ActionContext<OrderModel> ctx)
	{
		OrderModel order = ctx.getData();

		if (order == null)
		{
			LOG.warn("Order is null in action context");

			notifyFailure(Localization.getLocalizedString("novalnet.order.null"));

			return new ActionResult<>(ActionResult.ERROR, null);
		}

		final NovalnetTransactionResult result = novalnetTransactionFacade.captureOrder(order);

		if (result.isSuccess())
		{
			notificationService.notifyUser("", NOTIFICATION_SOURCE, NotificationEvent.Level.SUCCESS, result.getMessage());

			final ActionResult<OrderModel> actionResult = new ActionResult<>(ActionResult.SUCCESS, order);

			actionResult.getStatusFlags().add(ActionResult.StatusFlag.OBJECT_PERSISTED);

			return actionResult;
		}

		notifyFailure(result.getMessage());

		return new ActionResult<>(ActionResult.ERROR, null);
	}

	private void notifyFailure(final String message)
	{
		notificationService.notifyUser("", NOTIFICATION_SOURCE, NotificationEvent.Level.FAILURE, message);
	}
}
