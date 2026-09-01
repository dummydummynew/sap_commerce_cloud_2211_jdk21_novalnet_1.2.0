package com.novalnet.novalnetordermanagement.actions.returns;

import de.hybris.platform.core.model.order.OrderModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.hybris.cockpitng.engine.impl.AbstractComponentWidgetAdapterAware;
import com.novalnet.facades.NovalnetTransactionFacade;

import jakarta.annotation.Resource;


public class NovalnetCreateReturnRequestAction extends AbstractComponentWidgetAdapterAware
		implements CockpitAction<OrderModel, OrderModel>
{
	private static final Logger LOG = LoggerFactory.getLogger(NovalnetCreateReturnRequestAction.class);

	protected static final String SOCKET_OUT_CONTEXT = "createReturnRequestContext";

	@Resource(name = "novalnetTransactionFacade")
	private NovalnetTransactionFacade novalnetTransactionFacade;

	@Override
	public boolean canPerform(final ActionContext<OrderModel> actionContext)
	{
		final OrderModel order = actionContext.getData();

		if (order == null)
		{
			LOG.info("Order is null, return request button will not be visible");
			return false;
		}

		return novalnetTransactionFacade.canCreateReturnRequest(order);
	}

	@Override
	public ActionResult<OrderModel> perform(final ActionContext<OrderModel> actionContext)
	{
		final OrderModel order = actionContext.getData();

		if (order == null)
		{
			LOG.warn("Order is null in return request action");
			return new ActionResult<>(ActionResult.ERROR, null);
		}

		LOG.info("Starting return request for order {}", order.getCode());

		sendOutput(SOCKET_OUT_CONTEXT, order);

		LOG.info("Return request completed for order {}", order.getCode());

		final ActionResult<OrderModel> result = new ActionResult<>(ActionResult.SUCCESS, order);

		result.getStatusFlags().add(ActionResult.StatusFlag.OBJECT_PERSISTED);

		return result;
	}

	@Override
	public String getConfirmationMessage(final ActionContext<OrderModel> actionContext)
	{
		return null;
	}

	@Override
	public boolean needsConfirmation(final ActionContext<OrderModel> actionContext)
	{
		return false;
	}
}
