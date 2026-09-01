package com.novalnet.commands.impl;

import de.hybris.platform.payment.commands.request.FollowOnRefundRequest;
import de.hybris.platform.payment.commands.result.RefundResult;
import de.hybris.platform.payment.dto.TransactionStatus;
import de.hybris.platform.payment.dto.TransactionStatusDetails;

import com.novalnet.commands.NovalnetFollowOnRefundCommand;
import com.novalnet.dto.NovalnetTransactionResult;
import com.novalnet.facades.NovalnetTransactionFacade;

import jakarta.annotation.Resource;


public class NovalnetFollowOnRefundCommandImpl implements NovalnetFollowOnRefundCommand
{
	@Resource(name = "novalnetTransactionFacade")
	private NovalnetTransactionFacade novalnetTransactionFacade;

	@Override
	public RefundResult perform(FollowOnRefundRequest request)
	{
		NovalnetTransactionResult result = novalnetTransactionFacade.processRefund(request);

		RefundResult refundResult = new RefundResult();

		if (result.isSuccess())
		{
			refundResult.setTransactionStatus(TransactionStatus.ACCEPTED);
			refundResult.setTransactionStatusDetails(TransactionStatusDetails.SUCCESFULL);
			refundResult.setTotalAmount(request.getTotalAmount());
		}
		else
		{
			refundResult.setTransactionStatus(TransactionStatus.REJECTED);
			refundResult.setTransactionStatusDetails(TransactionStatusDetails.UNKNOWN_CODE);
		}

		return refundResult;
	}
}