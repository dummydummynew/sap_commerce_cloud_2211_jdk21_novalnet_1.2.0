package com.novalnet.novalnetordermanagement.actions.cancel;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.util.localization.Localization;

import org.apache.log4j.Logger;

import com.hybris.backoffice.widgets.notificationarea.NotificationService;
import com.hybris.backoffice.widgets.notificationarea.event.NotificationEvent;
import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.ActionResult.StatusFlag;
import com.hybris.cockpitng.actions.CockpitAction;
import com.novalnet.dto.NovalnetTransactionResult;
import com.novalnet.facades.NovalnetTransactionFacade;

import jakarta.annotation.Resource;


public class NovalnetCancelAction implements CockpitAction<OrderModel, OrderModel>
{
	private static final Logger LOG = Logger.getLogger(NovalnetCancelAction.class);

	private static final String NOTIFICATION_SOURCE = "General";

	@Resource(name = "notificationService")
	private NotificationService notificationService;

	@Resource(name = "novalnetTransactionFacade")
	private NovalnetTransactionFacade novalnetTransactionFacade;

	@Override
	public boolean canPerform(final ActionContext<OrderModel> ctx)
	{
		final OrderModel order = ctx.getData();

		return novalnetTransactionFacade.canCancel(order);
	}

	@Override
	public boolean needsConfirmation(final ActionContext<OrderModel> ctx)
	{
		return true;
	}

	@Override
	public String getConfirmationMessage(final ActionContext<OrderModel> ctx)
	{
		return Localization.getLocalizedString("novalnet.cancel.confirmation");
	}


	@Override
	public ActionResult<OrderModel> perform(final ActionContext<OrderModel> ctx)
	{
		final OrderModel order = ctx.getData();

		if (order == null)
		{
			LOG.warn("Order is null in action context");

			notifyUserFailure("novalnet.order.null");

			return new ActionResult<>(ActionResult.ERROR, null);
		}
		try
		{
			final NovalnetTransactionResult result = novalnetTransactionFacade.cancelOrder(order);

			if (result.isSuccess())
			{
				notificationService.notifyUser("", NOTIFICATION_SOURCE, NotificationEvent.Level.SUCCESS, result.getMessage());

				final ActionResult<OrderModel> actionResult = new ActionResult<>(ActionResult.SUCCESS, order);

				actionResult.getStatusFlags().add(StatusFlag.OBJECT_PERSISTED);

				return actionResult;
			}

			notificationService.notifyUser("", NOTIFICATION_SOURCE, NotificationEvent.Level.FAILURE, result.getMessage());

			return new ActionResult<>(ActionResult.ERROR, null);
		}
		catch (final Exception e)
		{
			LOG.error("Failed to perform order cancel", e);

			notifyUserFailure("novalnet.order.cancel.failed");

			return new ActionResult<>(ActionResult.ERROR, null);
		}
	}

	private void notifyUserFailure(final String message)
	{
		notificationService.notifyUser("", NOTIFICATION_SOURCE, NotificationEvent.Level.FAILURE, message);
	}
}
