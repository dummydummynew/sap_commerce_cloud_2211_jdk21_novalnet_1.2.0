package com.novalnet.service.payment.impl;

import static de.hybris.platform.basecommerce.enums.ConsignmentStatus.PICKUP_COMPLETE;
import static de.hybris.platform.basecommerce.enums.ConsignmentStatus.SHIPPED;

import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.AdapterException;
import de.hybris.platform.payment.PaymentService;
import de.hybris.platform.payment.commands.request.FollowOnRefundRequest;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.returns.model.ReturnRequestModel;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.util.localization.Localization;
import de.hybris.platform.warehousing.returns.service.RefundAmountCalculationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.novalnet.dao.NovalnetDao;
import com.novalnet.dto.NovalnetTransactionResult;
import com.novalnet.dto.OrderPaymentCommentsData;
import com.novalnet.model.NovalnetCallbackInfoModel;
import com.novalnet.model.NovalnetPaymentInfoModel;
import com.novalnet.service.http.NovalnetApiService;
import com.novalnet.service.order.NovalnetOrderService;
import com.novalnet.service.payment.NovalnetEndpointConfigService;
import com.novalnet.service.payment.NovalnetPaymentService;
import com.novalnet.service.payment.NovalnetTransactionService;
import com.novalnet.util.NovalnetUtils;

import jakarta.annotation.Resource;


public class DefaultNovalnetTransactionService implements NovalnetTransactionService
{
	private static final Logger LOG = LoggerFactory.getLogger(DefaultNovalnetTransactionService.class);

	private static final String ON_HOLD = "ON_HOLD";
	private static final String NOVALNET_PAYMENT_PREFIX = "novalnet";
	private static final String NOVALNET_STATUS_SUCCESS = "SUCCESS";
	private static final String CAPTURED = "CAPTURE";
	private static final long AMOUNT_MULTIPLIER_CENTS = 100;

	private static final String STATUS = "status";
	private static final String SHOP_INVOKED = "shop_invoked";
	private static final String RESULT = "result";
	private static final String TRANSACTION = "transaction";
	private static final String STATUS_TEXT = "status_text";
	private static final String QR_IMAGE = "qr_image";
	private static final String ORDER_NO = "order_no";
	private static final String NOVALNET_ORDER_NOTFOUND = "novalnet.order.notfound";
	private static final String AMOUNT = "amount";
	private static final String CUSTOM = "custom";
	private static final String DUE_DATE = "due_date";

	@Resource(name = "novalnetPaymentService")
	private NovalnetPaymentService novalnetPaymentService;

	@Resource
	private NovalnetEndpointConfigService novalnetEndpointConfigService;

	@Resource
	private NovalnetDao novalnetDao;

	@Resource
	private SessionService sessionService;

	@Resource(name = "novalnetOrderService")
	private NovalnetOrderService novalnetOrderService;

	@Resource(name = "novalnetApiService")
	private NovalnetApiService novalnetApiService;

	@Resource(name = "configurationService")
	private ConfigurationService configurationService;

	@Resource
	private PaymentService paymentService;

	@Resource
	private RefundAmountCalculationService refundAmountCalculationService;

	@Resource
	private ModelService modelService;

	@Override
	public OrderPaymentCommentsData buildOrderAndPaymentComments(String currentPayment, JSONObject transactionJsonObject,
			CartData cartData)
	{
		String orderComments = buildOrderComments(currentPayment, transactionJsonObject);

		String bankDetails = buildBankDetails(currentPayment, transactionJsonObject, cartData);

		return new OrderPaymentCommentsData(orderComments, bankDetails);
	}

	private String buildOrderComments(String currentPayment, JSONObject transactionJsonObject)
	{
		String paymentName = novalnetPaymentService.getPaymentName(currentPayment);
		String testMode = "";

		if (transactionJsonObject.get("test_mode").toString().equals("1"))
		{
			testMode = " " + Localization.getLocalizedString("novalnet.testOrderText");
		}

		String orderComments = Localization.getLocalizedString("novalnet.paymentname") + ": " + paymentName + "<br>";

		orderComments += Localization.getLocalizedString("novalnet.transactionId") + " : " + transactionJsonObject.get("tid")
				+ "<br>" + testMode + "<br>";

		Boolean isZeroAmountBooking = sessionService.getAttribute("novalnetZeroAmountBooking");

		if (Boolean.TRUE.equals(isZeroAmountBooking))
		{
			orderComments += "<br>" + Localization.getLocalizedString("novalnet.zeroAmountBooking");
		}

		sessionService.removeAttribute("novalnetZeroAmountBooking");

		return orderComments;
	}

	private String buildBankDetails(String currentPayment, JSONObject transactionJsonObject, CartData cartData)
	{
		if ("novalnetInvoice".equals(currentPayment) || "novalnetPrepayment".equals(currentPayment)
				|| "novalnetGuaranteedInvoice".equals(currentPayment))
		{
			return buildInvoiceLikeBankDetails(currentPayment, transactionJsonObject, cartData);
		}

		if ("novalnetMultibanco".equals(currentPayment) && transactionJsonObject.has("partner_payment_reference"))
		{
			return buildMultibancoBankDetails(transactionJsonObject, cartData);
		}

		if ("novalnetGuaranteedDirectDebitSepa".equals(currentPayment)
				&& "75".equals(transactionJsonObject.get("status_code").toString()))
		{
			return "<br>" + Localization.getLocalizedString("novalnet.sepa.status75");
		}

		return "";
	}

	private String buildInvoiceLikeBankDetails(String currentPayment, JSONObject transactionJsonObject, CartData cartData)
	{
		JSONObject bankdeatailsJsonObject = transactionJsonObject.getJSONObject("bank_details");

		String bankDetails = "<br>" + String.format(Localization.getLocalizedString("novalnet.bankDetailsComments1"),
				cartData.getTotalPriceWithTax().getFormattedValue());

		if (transactionJsonObject.has(DUE_DATE) && !ON_HOLD.equals(transactionJsonObject.get(STATUS).toString()))
		{
			LocalDate localDate = LocalDate.parse(transactionJsonObject.get(DUE_DATE).toString());

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
			String formattedDate = localDate.format(formatter);

			bankDetails += " " + String.format(Localization.getLocalizedString("novalnet.bankDetailsComments2"), formattedDate);
		}

		bankDetails += "<br>" + Localization.getLocalizedString("novalnet.bankDetailsAccountHolder") + " "
				+ bankdeatailsJsonObject.get("account_holder").toString();

		bankDetails += "<br>" + Localization.getLocalizedString("novalnet.bankDetailsIban") + " "
				+ bankdeatailsJsonObject.get("iban").toString();

		bankDetails += "<br>" + Localization.getLocalizedString("novalnet.bankDetailsBic") + " "
				+ bankdeatailsJsonObject.get("bic").toString();

		bankDetails += "<br>" + Localization.getLocalizedString("novalnet.bankDetailsBank") + " "
				+ bankdeatailsJsonObject.get("bank_name").toString();

		bankDetails += "<br>" + Localization.getLocalizedString("novalnet.bankPlace") + " "
				+ bankdeatailsJsonObject.get("bank_place").toString();

		bankDetails += "<br>" + Localization.getLocalizedString("novalnet.bankDetailspaymentRefernceMulti") + "<br>"
				+ Localization.getLocalizedString("novalnet.bankDetailsPaymentReference") + " : "
				+ transactionJsonObject.get("tid").toString() + "<br>";

		bankDetails = appendQrCodeIfPresent(bankDetails, bankdeatailsJsonObject);

		if ("novalnetGuaranteedInvoice".equals(currentPayment) && "75".equals(transactionJsonObject.get("status_code").toString()))
		{
			bankDetails = "<br>" + Localization.getLocalizedString("novalnet.status75");
		}

		return bankDetails;
	}

	private String appendQrCodeIfPresent(String bankDetails, JSONObject bankdeatailsJsonObject)
	{
		String updatedBankDetails = bankDetails;

		if (bankdeatailsJsonObject.has(QR_IMAGE) && !bankdeatailsJsonObject.optString(QR_IMAGE).isEmpty())
		{
			updatedBankDetails += "<br>";
			updatedBankDetails += Localization.getLocalizedString("novalnet.qrCodeComments");
			updatedBankDetails += "<br>";
			updatedBankDetails += "<img alt='nn_qr_code' src='" + bankdeatailsJsonObject.optString(QR_IMAGE) + "'>";
			updatedBankDetails += "<br>";
		}

		return updatedBankDetails;
	}

	private String buildMultibancoBankDetails(JSONObject transactionJsonObject, CartData cartData)
	{
		return "<br>" + Localization.getLocalizedString("novalnet.multibancocomments1") + " "
				+ cartData.getTotalPriceWithTax().getFormattedValue()
				+ Localization.getLocalizedString("novalnet.multibancocomments2") + "<br>"
				+ Localization.getLocalizedString("novalnet.bankDetailsPaymentReference") + " : "
				+ transactionJsonObject.get("partner_payment_reference").toString() + "<br>"
				+ Localization.getLocalizedString("novalnet.multibancosupplierid") + " : "
				+ transactionJsonObject.get("service_supplier_id").toString();
	}

	@Override
	public boolean canCancel(OrderModel order)
	{
		if (order == null)
		{
			return false;
		}

		String paymentMethod = order.getPaymentMode() != null ? order.getPaymentMode().getCode() : null;

		LOG.info("Novalnet cancel service - payment method: {}", paymentMethod);

		if (paymentMethod != null && paymentMethod.startsWith(NOVALNET_PAYMENT_PREFIX))
		{
			List<NovalnetPaymentInfoModel> paymentInfoList = novalnetDao.getNovalnetPaymentInfo(order.getCode());

			if (paymentInfoList != null)
			{
				return paymentInfoList.stream()
						.anyMatch(paymentInfo -> ON_HOLD.equalsIgnoreCase(paymentInfo.getPaymentGatewayStatus()));
			}
		}

		return false;
	}

	@Override
	public NovalnetTransactionResult cancelOrder(OrderModel order)
	{
		List<NovalnetCallbackInfoModel> orderReferences = novalnetDao.getPaymentDetailsInfo(order.getCode());

		if (orderReferences == null || orderReferences.isEmpty())
		{
			LOG.warn("No order reference found for order: {}", order.getCode());
			return new NovalnetTransactionResult(false, NOVALNET_ORDER_NOTFOUND);
		}

		long originalTid = orderReferences.get(0).getOrginalTid();

		try
		{
			return cancelPayment(originalTid, order.getStore(), order);
		}
		catch (Exception e)
		{
			LOG.error("Failed to perform order cancel", e);
			return new NovalnetTransactionResult(false, "novalnet.order.cancel.failed");
		}
	}

	private NovalnetTransactionResult cancelPayment(long tid, BaseStoreModel baseStore, OrderModel order)
	{
		JSONObject transaction = new JSONObject();
		transaction.put("tid", tid);

		JSONObject custom = new JSONObject();
		custom.put("lang", NovalnetUtils.getLanguageCode(order));
		custom.put(SHOP_INVOKED, 1);

		JSONObject request = new JSONObject();
		request.put(TRANSACTION, transaction);
		request.put(CUSTOM, custom);

		LOG.info("Sending Novalnet cancel request for TID: {}", tid);

		StringBuilder response = novalnetApiService.followupSendRequest(novalnetEndpointConfigService.getTransactionCancelUrl(),
				request.toString(), baseStore);

		JSONObject jsonResponse = new JSONObject(response.toString());
		JSONObject result = jsonResponse.getJSONObject(RESULT);

		if (NOVALNET_STATUS_SUCCESS.equals(result.getString(STATUS)))
		{
			return handleCancelSuccess(jsonResponse);
		}

		String statusText = result.getString(STATUS_TEXT);

		LOG.warn("Novalnet cancel failed: {}", statusText);

		return new NovalnetTransactionResult(false, statusText);
	}

	private NovalnetTransactionResult handleCancelSuccess(JSONObject jsonResponse)
	{
		JSONObject transaction = jsonResponse.getJSONObject(TRANSACTION);

		String transactionStatus = transaction.getString(STATUS);
		String nnOrderNo = transaction.getString(ORDER_NO);

		List<NovalnetPaymentInfoModel> paymentInfoList = novalnetDao.getNovalnetPaymentInfo(nnOrderNo);

		String currentDate = NovalnetUtils.getCurrentDate();

		String extensionComments = Localization.getLocalizedString("novalnet.transaction.cancel") + " " + currentDate + " "
				+ Localization.getLocalizedString("novalnet.cancel.confirm.suffix");

		novalnetPaymentService.updatePaymentInfo(paymentInfoList, transactionStatus);
		novalnetPaymentService.updateCancelStatus(nnOrderNo);

		novalnetOrderService.updateCallbackComments(extensionComments, nnOrderNo, transactionStatus);

		LOG.info("Novalnet payment cancelled successfully. Order: {}", nnOrderNo);

		return new NovalnetTransactionResult(true, extensionComments);
	}

	@Override
	public NovalnetTransactionResult captureOrder(OrderModel order)
	{
		if (order == null)
		{
			LOG.warn("Order is null in capture service");
			return new NovalnetTransactionResult(false, "novalnet.order.null");
		}

		BaseStoreModel baseStore = order.getStore();

		if (baseStore == null)
		{
			LOG.warn("BaseStore is missing for order: {}", order.getCode());
			return new NovalnetTransactionResult(false, "novalnet.order.capture.failed");
		}

		List<NovalnetCallbackInfoModel> orderReferences = novalnetDao.getPaymentDetailsInfo(order.getCode());

		if (orderReferences == null || orderReferences.isEmpty())
		{
			LOG.warn("No order reference found for order: {}", order.getCode());
			return new NovalnetTransactionResult(false, NOVALNET_ORDER_NOTFOUND);
		}

		long originalTid = orderReferences.get(0).getOrginalTid();

		try
		{
			return capturePayment(originalTid, baseStore, order);
		}
		catch (Exception e)
		{
			LOG.error("Failed to perform order capture", e);
			return new NovalnetTransactionResult(false, "novalnet.order.capture.failed");
		}
	}

	private NovalnetTransactionResult capturePayment(long tid, BaseStoreModel baseStore, OrderModel order)
	{
		JSONObject transaction = new JSONObject();
		transaction.put("tid", tid);

		JSONObject custom = new JSONObject();
		custom.put("lang", NovalnetUtils.getLanguageCode(order));
		custom.put(SHOP_INVOKED, 1);

		JSONObject request = new JSONObject();
		request.put(TRANSACTION, transaction);
		request.put(CUSTOM, custom);

		LOG.info("Sending Novalnet capture request for TID: {}", tid);

		StringBuilder response = novalnetApiService.followupSendRequest(novalnetEndpointConfigService.getTransactionCaptureUrl(),
				request.toString(), baseStore);

		JSONObject jsonResponse = new JSONObject(response.toString());
		JSONObject result = jsonResponse.getJSONObject(RESULT);

		if (NOVALNET_STATUS_SUCCESS.equals(result.getString(STATUS)))
		{
			return handleCaptureSuccess(jsonResponse);
		}

		String statusText = result.getString(STATUS_TEXT);

		LOG.warn("Novalnet capture failed: {}", statusText);

		return new NovalnetTransactionResult(false, statusText);
	}

	private NovalnetTransactionResult handleCaptureSuccess(JSONObject jsonResponse)
	{
		JSONObject transaction = jsonResponse.getJSONObject(TRANSACTION);

		String transactionStatus = transaction.getString(STATUS);
		String nnOrderNo = transaction.getString(ORDER_NO);
		String paymentType = transaction.getString("payment_type");

		List<NovalnetPaymentInfoModel> paymentInfoList = novalnetDao.getNovalnetPaymentInfo(nnOrderNo);

		NovalnetPaymentInfoModel paymentInfoModel = novalnetPaymentService.getPaymentModel(paymentInfoList);

		String currentDate = NovalnetUtils.getCurrentDate();
		String extensionComments;

		if ("GUARANTEED_INVOICE".equals(paymentType) || "INVOICE".equals(paymentType))
		{
			int amountInCents = transaction.getInt(AMOUNT);

			String amount = BigDecimal.valueOf(amountInCents).movePointLeft(2).toPlainString();

			String dueDate = transaction.getString(DUE_DATE);

			extensionComments = Localization.getLocalizedString("novalnet.invoice.capture.confirm") + " " + currentDate
					+ Localization.getLocalizedString("novalnet.invoice.capture.confirm.suffix") + " "
					+ Localization.getLocalizedString("novalnet.capture.transfer") + " " + amount + " "
					+ Localization.getLocalizedString("novalnet.capture.before") + " " + dueDate;
		}
		else
		{
			extensionComments = Localization.getLocalizedString("novalnet.capture.confirm") + " " + currentDate;
		}

		novalnetPaymentService.updatePaymentInfo(paymentInfoList, CAPTURED);

		novalnetOrderService.updateOrderStatus(nnOrderNo, paymentInfoModel);

		novalnetOrderService.updateCallbackComments(extensionComments, nnOrderNo, transactionStatus);

		LOG.info("Novalnet payment captured successfully. Order: {}", nnOrderNo);

		return new NovalnetTransactionResult(true, extensionComments);
	}

	@Override
	public boolean canCapture(OrderModel order)
	{
		if (order == null)
		{
			return false;
		}

		String paymentMethod = order.getPaymentMode() != null ? order.getPaymentMode().getCode() : null;

		LOG.info("Novalnet capture service - payment method: {}", paymentMethod);

		if (paymentMethod != null && paymentMethod.startsWith(NOVALNET_PAYMENT_PREFIX))
		{
			List<NovalnetPaymentInfoModel> paymentInfoList = novalnetDao.getNovalnetPaymentInfo(order.getCode());

			if (paymentInfoList != null)
			{
				return paymentInfoList.stream()
						.anyMatch(paymentInfo -> ON_HOLD.equalsIgnoreCase(paymentInfo.getPaymentGatewayStatus()));
			}
		}

		return false;
	}

	@Override
	public boolean canCreateReturnRequest(OrderModel order)
	{
		if (order == null)
		{
			LOG.info("Order is null, return request is not allowed");
			return false;
		}

		if (isFullyRefunded(order))
		{
			LOG.info("Order {} is fully refunded, return request is not allowed", order.getCode());
			return false;
		}

		boolean returnable = isReturnable(order);
		boolean refundable = isRefundable(order);
		boolean result = returnable && refundable;

		LOG.info("Return request eligibility for order {}: {}", order.getCode(), result);

		return result;
	}

	public boolean isReturnable(OrderModel order)
	{
		if (order == null || order.getConsignments() == null || order.getEntries() == null)
		{
			return false;
		}

		boolean hasValidConsignment = order.getConsignments().stream()
				.anyMatch(consignment -> SHIPPED.equals(consignment.getStatus()) || PICKUP_COMPLETE.equals(consignment.getStatus()));

		LOG.info("Order {} returnable status: {}", order.getCode(), hasValidConsignment);

		return hasValidConsignment;
	}

	@Override
	public boolean isRefundable(OrderModel order)
	{
		if (!(order.getPaymentInfo() instanceof NovalnetPaymentInfoModel))
		{
			return false;
		}

		List<NovalnetPaymentInfoModel> paymentInfoList = novalnetDao.getNovalnetPaymentInfo(order.getCode());

		if (paymentInfoList == null || paymentInfoList.isEmpty())
		{
			return false;
		}

		String status = paymentInfoList.get(0).getPaymentGatewayStatus();

		return status != null && "CONFIRMED".equalsIgnoreCase(status);
	}

	@Override
	public boolean isFullyRefunded(OrderModel order)
	{
		if (order.getPaymentTransactions() == null || order.getPaymentTransactions().isEmpty())
		{
			LOG.info("Order {} has no payment transactions", order.getCode());
			return false;
		}

		long totalRefundedCents = 0;

		for (PaymentTransactionModel transaction : order.getPaymentTransactions())
		{
			LOG.info("Checking payment transaction {}", transaction.getCode());

			for (PaymentTransactionEntryModel entry : transaction.getEntries())
			{
				LOG.info("Transaction entry type {} status {} amount {}", entry.getType(), entry.getTransactionStatus(),
						entry.getAmount());

				if (PaymentTransactionType.REFUND_FOLLOW_ON.equals(entry.getType())
						&& "ACCEPTED".equalsIgnoreCase(entry.getTransactionStatus()))
				{
					long amountCents = entry.getAmount().multiply(BigDecimal.valueOf(AMOUNT_MULTIPLIER_CENTS)).longValue();

					totalRefundedCents += amountCents;

					LOG.info("Refunded amount: {}, total refunded: {}", amountCents, totalRefundedCents);
				}
			}
		}

		long orderTotalCents = BigDecimal.valueOf(order.getTotalPrice()).multiply(BigDecimal.valueOf(AMOUNT_MULTIPLIER_CENTS))
				.longValue();

		boolean fullyRefunded = totalRefundedCents >= orderTotalCents;

		LOG.info("Order {} total refunded: {}, order total: {}, fully refunded: {}", order.getCode(), totalRefundedCents,
				orderTotalCents, fullyRefunded);

		return fullyRefunded;
	}

	@Override
	public NovalnetTransactionResult refund(ReturnRequestModel returnRequest)
	{
		if (returnRequest == null || returnRequest.getOrder() == null)
		{
			LOG.error("Return request or order is null");
			return new NovalnetTransactionResult(false, NOVALNET_ORDER_NOTFOUND);
		}

		List<PaymentTransactionModel> transactions = returnRequest.getOrder().getPaymentTransactions();

		if (transactions == null || transactions.isEmpty())
		{
			LOG.warn("No payment transaction found for return request: {}", returnRequest.getCode());

			return new NovalnetTransactionResult(false, NOVALNET_ORDER_NOTFOUND);
		}

		PaymentTransactionModel transaction = transactions.get(0);

		BigDecimal customRefundAmount = refundAmountCalculationService.getCustomRefundAmount(returnRequest);

		BigDecimal amountToRefund;

		if (customRefundAmount != null && customRefundAmount.compareTo(BigDecimal.ZERO) > 0)
		{
			amountToRefund = customRefundAmount;

			LOG.info("Using custom refund amount {} for return request {}", amountToRefund, returnRequest.getCode());
		}
		else
		{
			amountToRefund = refundAmountCalculationService.getOriginalRefundAmount(returnRequest);

			LOG.info("Using original refund amount {} for return request {}", amountToRefund, returnRequest.getCode());
		}

		if (amountToRefund == null || amountToRefund.compareTo(BigDecimal.ZERO) <= 0)
		{
			LOG.warn("Invalid refund amount {} for return request {}", amountToRefund, returnRequest.getCode());

			return new NovalnetTransactionResult(false, "novalnet.refund.amount.invalid");
		}

		try
		{
			LOG.info("Calling payment service for refund. Return request: {}, amount: {}", returnRequest.getCode(), amountToRefund);

			PaymentTransactionEntryModel refundEntry = paymentService.refundFollowOn(transaction, amountToRefund);

			if (refundEntry == null || !"ACCEPTED".equalsIgnoreCase(refundEntry.getTransactionStatus()))
			{
				LOG.error("Refund was not accepted for return request {}", returnRequest.getCode());

				return new NovalnetTransactionResult(false, "novalnet.order.refund.failed");
			}

			LOG.info("Refund successful for return request {}", returnRequest.getCode());

			return new NovalnetTransactionResult(true, "novalnet.order.refund.success");
		}
		catch (AdapterException e)
		{
			LOG.error("Refund failed for return request {}", returnRequest.getCode(), e);

			return new NovalnetTransactionResult(false, "novalnet.order.refund.failed");
		}
	}

	@Override
	public NovalnetTransactionResult processRefund(FollowOnRefundRequest request)
	{
		if (request == null)
		{
			LOG.error("Refund request is null");
			return new NovalnetTransactionResult(false, "Refund request is null");
		}

		String tid = request.getRequestId();
		BigDecimal totalAmount = request.getTotalAmount();

		if (tid == null || tid.isBlank())
		{
			LOG.error("Refund TID is missing");
			return new NovalnetTransactionResult(false, "Refund TID is missing");
		}

		if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0)
		{
			LOG.error("Invalid refund amount for TID: {}", tid);
			return new NovalnetTransactionResult(false, "Invalid refund amount");
		}

		try
		{
			return executeRefund(tid, totalAmount);
		}
		catch (Exception e)
		{
			LOG.error("Exception during Novalnet refund for TID: {}", tid, e);

			return new NovalnetTransactionResult(false, "Novalnet refund failed");
		}
	}

	private NovalnetTransactionResult executeRefund(String tid, BigDecimal totalAmount)
	{
		LOG.info("Refund initiated for TID: {} with amount: {}", tid, totalAmount);

		OrderModel order = novalnetDao.getOrderByTid(tid);

		if (order == null)
		{
			LOG.error("No order found for TID: {}", tid);
			return new NovalnetTransactionResult(false, "Order not found");
		}

		BigDecimal orderTotal = order.getTotalPrice() != null ? BigDecimal.valueOf(order.getTotalPrice()) : BigDecimal.ZERO;

		if (totalAmount.compareTo(orderTotal) > 0)
		{
			LOG.error("Refund amount {} exceeds order total {} for order {}", totalAmount, orderTotal, order.getCode());

			return new NovalnetTransactionResult(false, "Refund amount exceeds order total");
		}

		BaseStoreModel baseStore = order.getStore();

		if (baseStore == null)
		{
			LOG.error("BaseStore is null for order: {}", order.getCode());
			return new NovalnetTransactionResult(false, "BaseStore is missing");
		}

		JSONObject response = sendRefundRequest(tid, totalAmount, order, baseStore);

		JSONObject result = response.getJSONObject(RESULT);

		if (!NOVALNET_STATUS_SUCCESS.equalsIgnoreCase(result.getString(STATUS)))
		{
			String statusText = result.optString(STATUS_TEXT, "Refund failed");

			LOG.error("Novalnet refund failed for TID {}: {}", tid, statusText);

			return new NovalnetTransactionResult(false, statusText);
		}

		return handleRefundSuccess(tid, response);
	}

	private JSONObject sendRefundRequest(String tid, BigDecimal totalAmount, OrderModel order, BaseStoreModel baseStore)
	{
		int amountInCents = totalAmount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();

		JSONObject transaction = new JSONObject();
		transaction.put("tid", tid);
		transaction.put(AMOUNT, amountInCents);

		JSONObject custom = new JSONObject();
		custom.put("lang", NovalnetUtils.getLanguageCode(order));
		custom.put(SHOP_INVOKED, 1);

		JSONObject data = new JSONObject();
		data.put(TRANSACTION, transaction);
		data.put(CUSTOM, custom);

		LOG.info("Sending Novalnet refund request for TID: {}", tid);

		StringBuilder responseStr = novalnetApiService.followupSendRequest(novalnetEndpointConfigService.getTransactionRefundUrl(),
				data.toString(), baseStore);

		LOG.info("Refund API raw response: {}", responseStr);

		return new JSONObject(responseStr.toString());
	}

	private NovalnetTransactionResult handleRefundSuccess(String tid, JSONObject response)
	{
		JSONObject transactionObject = response.getJSONObject(TRANSACTION);

		String transactionStatus = transactionObject.getString(STATUS);

		String nnOrderNo = transactionObject.getString(ORDER_NO);

		JSONObject refundObject = transactionObject.optJSONObject("refund");

		if (refundObject == null)
		{
			LOG.error("Refund object missing in Novalnet response for TID: {}", tid);

			return new NovalnetTransactionResult(false, "Refund response is invalid");
		}

		long newTid = refundObject.getLong("tid");
		int refundedAmount = refundObject.getInt(AMOUNT);

		BigDecimal formattedRefundAmount = BigDecimal.valueOf(refundedAmount).movePointLeft(2);

		LOG.info("Novalnet refund successful. Original TID: {}, New TID: {}, Amount: {}", tid, newTid, formattedRefundAmount);

		List<NovalnetPaymentInfoModel> paymentInfo = novalnetDao.getNovalnetPaymentInfo(nnOrderNo);

		String currency = transactionObject.optString("currency", "");

		String extensionComments = buildRefundComments(tid, formattedRefundAmount, currency, newTid);

		int orderAmount = transactionObject.getInt(AMOUNT);
		int totalRefundedAmount = refundObject.getInt(AMOUNT);

		String refundStatus = totalRefundedAmount >= orderAmount ? "REFUND" : "PARTIAL_REFUND";

		novalnetPaymentService.updatePaymentInfo(paymentInfo, refundStatus);

		novalnetOrderService.updateCallbackComments(extensionComments, nnOrderNo, transactionStatus);

		finalizeRefundOrderStatus(tid, nnOrderNo, totalRefundedAmount, orderAmount);

		return new NovalnetTransactionResult(true, extensionComments);
	}

	private String buildRefundComments(String tid, BigDecimal formattedRefundAmount, String currency, long newTid)
	{
		String extensionComments = Localization.getLocalizedString("novalnet.transaction.refund") + " " + tid + " "
				+ Localization.getLocalizedString("novalnet.with.amount1") + " " + formattedRefundAmount + " " + currency + ".";

		if (newTid > 0)
		{
			extensionComments += " " + Localization.getLocalizedString("novalnet.new.tid") + " " + newTid;
		}

		return extensionComments;
	}

	private void finalizeRefundOrderStatus(String tid, String nnOrderNo, int totalRefundedAmount, int orderAmount)
	{
		if (totalRefundedAmount >= orderAmount)
		{
			LOG.info("Full refund detected. Cancelling order {}", nnOrderNo);

			novalnetOrderService.updateCancelStatus(nnOrderNo);

			OrderModel updatedOrder = novalnetDao.getOrderByTid(tid);

			if (updatedOrder != null)
			{
				modelService.refresh(updatedOrder);
			}
		}
		else
		{
			LOG.info("Partial refund detected for order {}", nnOrderNo);
		}
	}
}