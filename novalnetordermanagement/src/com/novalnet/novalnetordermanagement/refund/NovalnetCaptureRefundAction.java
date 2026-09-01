package com.novalnet.novalnetordermanagement.refund;

import static de.hybris.platform.basecommerce.enums.ReturnStatus.PAYMENT_REVERSAL_FAILED;
import static de.hybris.platform.basecommerce.enums.ReturnStatus.PAYMENT_REVERSED;
import static de.hybris.platform.processengine.action.AbstractSimpleDecisionAction.Transition.NOK;
import static de.hybris.platform.processengine.action.AbstractSimpleDecisionAction.Transition.OK;

import de.hybris.platform.basecommerce.enums.ReturnStatus;
import de.hybris.platform.processengine.action.AbstractSimpleDecisionAction;
import de.hybris.platform.returns.model.ReturnProcessModel;
import de.hybris.platform.returns.model.ReturnRequestModel;
import de.hybris.platform.task.RetryLaterException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.novalnet.dto.NovalnetTransactionResult;
import com.novalnet.facades.NovalnetTransactionFacade;

import jakarta.annotation.Resource;


public class NovalnetCaptureRefundAction extends AbstractSimpleDecisionAction<ReturnProcessModel>
{
	private static final Logger LOG = LoggerFactory.getLogger(NovalnetCaptureRefundAction.class);

	@Resource(name = "novalnetTransactionFacade")
	private NovalnetTransactionFacade novalnetTransactionFacade;

	@Override
	public Transition executeAction(final ReturnProcessModel process) throws RetryLaterException
	{
		LOG.info("Refund action started");

		if (process == null || process.getReturnRequest() == null)
		{
			LOG.error("Process or ReturnRequest is null");
			return NOK;
		}

		final ReturnRequestModel returnRequest = process.getReturnRequest();

		try
		{
			final NovalnetTransactionResult result = novalnetTransactionFacade.refund(returnRequest);

			if (result.isSuccess())
			{
				setReturnRequestStatus(returnRequest, PAYMENT_REVERSED);
				LOG.info("Refund successful for return request {}", returnRequest.getCode());
				return OK;
			}

			LOG.error("Refund failed for return request {}: {}", returnRequest.getCode(), result.getMessage());
			setReturnRequestStatus(returnRequest, PAYMENT_REVERSAL_FAILED);
			return NOK;
		}
		catch (final Exception e)
		{
			LOG.error("Exception during refund for return request {}", returnRequest.getCode(), e);
			setReturnRequestStatus(returnRequest, PAYMENT_REVERSAL_FAILED);
			return NOK;
		}
	}

	protected void setReturnRequestStatus(final ReturnRequestModel returnRequest, final ReturnStatus status)
	{
		LOG.info("Updating return request {} status to {}", returnRequest.getCode(), status);
		returnRequest.setStatus(status);
		returnRequest.getReturnEntries().forEach(entry -> entry.setStatus(status));
		getModelService().saveAll(returnRequest.getReturnEntries());
		getModelService().save(returnRequest);
		LOG.info("Return request {} status updated successfully", returnRequest.getCode());
	}
}
