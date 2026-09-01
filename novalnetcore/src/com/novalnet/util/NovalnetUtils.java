/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.util;

import de.hybris.platform.commercefacades.user.data.TitleData;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.servicelayer.exceptions.SystemException;
import de.hybris.platform.store.BaseStoreModel;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;

import de.novalnet.beans.NnCallbackEventData;
import de.novalnet.beans.NnCallbackRequestData;
import de.novalnet.beans.NnCallbackResultData;
import de.novalnet.beans.NnCallbackTransactionData;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.DatatypeConverter;


public final class NovalnetUtils
{

	private static final String AUTHORIZE_WITH_ZERO_AMOUNT = "AUTHORIZE_WITH_ZERO_AMOUNT";
	private static final Logger LOGGER = Logger.getLogger(NovalnetUtils.class);
	public static final int AGE_REQUIREMENT = 18;
	public static final int DAYS_IN_A_YEAR = 365;
	public static final int TOTAL_HOURS = 24;
	public static final int TOTAL_MINUTES_SECONDS = 60;
	private static final String SHA_256 = "SHA-256";
	private static final String LOCALHOST_IP = "127.0.0.1";



	private NovalnetUtils()
	{
	}

	public static String getLanguageCode(final OrderModel order)
	{
		if (order != null && order.getLanguage() != null)
		{
			return order.getLanguage().getIsocode().toUpperCase();
		}
		return "EN";
	}

	public static boolean isPopulated(final String val)
	{
		return val != null && !val.trim().isEmpty();
	}

	public static String formatAmount(String amount)
	{
		if (amount.contains(","))
		{
			try
			{
				final NumberFormat formattedAmount = NumberFormat.getNumberInstance(Locale.GERMANY);
				final double formattedValue = formattedAmount.parse(amount).doubleValue();
				amount = Double.toString(formattedValue);
			}
			catch (final Exception e)
			{
				amount = amount.replace(",", ".");
			}
		}
		return amount;
	}

	public String getEncodedValue(final String input)
	{

		try
		{
			final byte[] data = input.getBytes(StandardCharsets.UTF_8);
			return DatatypeConverter.printBase64Binary(data);
		}
		catch (final Exception e)
		{
			return e.getMessage();
		}
	}

	public String getServerIpAddr()
	{
		try
		{
			final InetAddress ipAddr = InetAddress.getLocalHost();

			if (ipAddr instanceof Inet4Address)
			{
				return ipAddr.getHostAddress();
			}
			else if (ipAddr instanceof Inet6Address)
			{
				return LOCALHOST_IP;
			}
		}
		catch (final UnknownHostException ex)
		{
			LOGGER.error("UnknownHostException ", ex);
		}

		return LOCALHOST_IP;
	}

	public static boolean hasAgeRequirement(final String dateInString)
	{
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
		try
		{
			final Date birthDate = sdf.parse(dateInString);

			final long ageInMillis = System.currentTimeMillis() - birthDate.getTime();

			final long years = ageInMillis / (DAYS_IN_A_YEAR * TOTAL_HOURS * TOTAL_MINUTES_SECONDS * TOTAL_MINUTES_SECONDS * 1000l);

			return years >= AGE_REQUIREMENT;
		}
		catch (final ParseException e)
		{
			return false;
		}
	}

	public static boolean isZeroAmountBooking(final Object actionType)
	{
		return actionType != null && AUTHORIZE_WITH_ZERO_AMOUNT.equals(actionType.toString());
	}

	public static String getPaymentType(final String paymentName)
	{
		final Map<String, String> paymentType = new HashMap<>();
		paymentType.put("novalnetCreditCard", "CREDITCARD");
		paymentType.put("novalnetDirectDebitSepa", "DIRECT_DEBIT_SEPA");
		paymentType.put("novalnetDirectDebitAch", "DIRECT_DEBIT_ACH");
		paymentType.put("novalnetGuaranteedDirectDebitSepa", "GUARANTEED_DIRECT_DEBIT_SEPA");
		paymentType.put("novalnetInvoice", "INVOICE");
		paymentType.put("novalnetGuaranteedInvoice", "GUARANTEED_INVOICE");
		paymentType.put("novalnetPrepayment", "PREPAYMENT");
		paymentType.put("novalnetPayPal", "PAYPAL");
		paymentType.put("novalnetOnlineBankTransfer", "ONLINE_BANK_TRANSFER");
		paymentType.put("novalnetBancontact", "BANCONTACT");
		paymentType.put("novalnetMultibanco", "MULTIBANCO");
		paymentType.put("novalnetIdeal", "IDEAL");
		paymentType.put("novalnetGooglePay", "GOOGLEPAY");
		paymentType.put("novalnetApplePay", "APPLEPAY");
		paymentType.put("novalnetTwint", "TWINT");
		paymentType.put("novalnetMbWay", "MBWAY");
		paymentType.put("novalnetAlipay", "ALIPAY");
		paymentType.put("novalnetTrustly", "TRUSTLY");
		paymentType.put("novalnetBlik", "BLIK");
		paymentType.put("novalnetWechatPay", "WECHATPAY");
		paymentType.put("novalnetEps", "EPS");
		paymentType.put("novalnetPrzelewy24", "PRZELEWY24");
		paymentType.put("novalnetPostFinanceCard", "POSTFINANCE_CARD");
		paymentType.put("novalnetPostFinance", "POSTFINANCE");
		return paymentType.get(paymentName);
	}

	public static String formatDate(final int date)
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
		Calendar calendarInsatance = Calendar.getInstance();
		calendarInsatance.add(Calendar.DATE, date);
		return dateFormat.format(calendarInsatance.getTime());
	}

	public static String getRemoteIpAddr(final HttpServletRequest request)
	{
		try
		{
			final InetAddress ipAddr = InetAddress.getByName(request.getRemoteAddr());
			if (ipAddr instanceof Inet4Address)
			{
				return ipAddr.getHostAddress();
			}
			else if (ipAddr instanceof Inet6Address)
			{
				return LOCALHOST_IP;
			}
		}
		catch (final UnknownHostException ex)
		{
			LOGGER.error("UnknownHostException ", ex);
		}
		return LOCALHOST_IP;
	}

	public static String generateChecksum(final String tokenString)
	{
		String checksum = "";
		try
		{
			final MessageDigest digest = MessageDigest.getInstance(SHA_256);
			final byte[] hash = digest.digest(tokenString.getBytes(StandardCharsets.UTF_8));
			final StringBuilder hexString = new StringBuilder();

			for (int i = 0; i < hash.length; i++)
			{
				final String hex = Integer.toHexString(0xff & hash[i]);
				if (hex.length() == 1)
				{
					hexString.append('0');
				}
				hexString.append(hex);
			}

			checksum = hexString.toString();
		}
		catch (final RuntimeException ex)
		{
			LOGGER.error("RuntimeException" + ex);
		}
		catch (final NoSuchAlgorithmException ex)
		{
			LOGGER.error("UnsupportedEncodingException" + ex);
		}
		return checksum;
	}

	public static TitleData findTitleForCode(final List<TitleData> titles, final String code)
	{
		if (code != null && !code.isEmpty() && titles != null && !titles.isEmpty())
		{
			for (final TitleData title : titles)
			{
				if (code.equals(title.getCode()))
				{
					return title;
				}
			}
		}
		return null;
	}

	public static String getCurrentDate()
	{
		final Locale locale = JaloSession.getCurrentSession().getSessionContext().getLocale();

		if (Locale.GERMAN.getLanguage().equals(locale.getLanguage()))
		{
			return new SimpleDateFormat("dd-MM-yyyy 'um' HH:mm:ss").format(new Date());
		}

		return new SimpleDateFormat("dd-MM-yyyy, HH:mm:ss").format(new Date());
	}


	public static void validateCallbackIp(final HttpServletRequest request, final BaseStoreModel baseStore)
	{

		final boolean testMode = Boolean.TRUE.equals(baseStore.getNovalnetVendorscriptTestMode());

		final String callerIp = extractClientIp(request);
		final String novalnetIp = resolveNovalnetIp();
		logStructured("IP_VALIDATION", "callerIp", callerIp, "novalnetIp", novalnetIp, "testMode", String.valueOf(testMode));

		if (!testMode && !novalnetIp.equals(callerIp))
		{
			throw new IllegalStateException("Unauthorized callback source IP: " + callerIp);
		}
	}

	public static void validateMandatoryParams(final NnCallbackRequestData request)
	{
		if (request.getEvent() == null || request.getTransaction() == null || request.getResult() == null)
		{
			throw new SystemException("Mandatory callback parameters missing");
		}
	}

	public static void validateCallbackChecksum(final NnCallbackRequestData request, final BaseStoreModel baseStore)
	{

		final String accessKey = baseStore.getNovalnetPaymentAccessKey();

		if (accessKey == null || accessKey.isEmpty())
		{
			throw new IllegalStateException("Novalnet payment access key is not configured");
		}

		final String calculatedChecksum = generateChecksum(request, accessKey);

		final String receivedChecksum = request.getEvent().getChecksum();

		logStructured("CHECKSUM_VALIDATION", "received", receivedChecksum, "calculated", calculatedChecksum);

		if (!calculatedChecksum.equals(receivedChecksum))
		{
			throw new IllegalStateException("Checksum validation failed. Callback payload may be tampered");
		}
	}

	private static String extractClientIp(final HttpServletRequest request)
	{

		final String forwardedFor = request.getHeader("X-Forwarded-For");

		if (forwardedFor != null && !forwardedFor.isEmpty())
		{
			return forwardedFor.split(",")[0].trim();
		}

		return request.getRemoteAddr();
	}

	private static String resolveNovalnetIp()
	{
		try
		{
			return InetAddress.getByName("pay-nn.de").getHostAddress();
		}
		catch (final Exception ex)
		{
			throw new IllegalStateException("Failed to resolve Novalnet host IP", ex);
		}
	}

	private static void logStructured(final String event, final String... kvPairs)
	{

		final StringBuilder log = new StringBuilder();
		log.append("{ \"event\":\"").append(event).append("\"");

		for (int i = 0; i < kvPairs.length; i += 2)
		{
			log.append(", \"").append(kvPairs[i]).append("\":\"").append(kvPairs[i + 1]).append("\"");
		}

		log.append(" }");
		LOGGER.info(log.toString());
	}

	private static String generateChecksum(final NnCallbackRequestData request, final String accessKey)
	{

		final NnCallbackEventData event = request.getEvent();
		final NnCallbackTransactionData txn = request.getTransaction();
		final NnCallbackResultData result = request.getResult();

		final StringBuilder token = new StringBuilder().append(event.getTid()).append(event.getType()).append(result.getStatus());

		if (txn.getAmount() != null)
		{
			token.append(txn.getAmount());
		}
		if (txn.getCurrency() != null)
		{
			token.append(txn.getCurrency());
		}

		token.append(new StringBuilder(accessKey.trim()).reverse());

		try
		{
			final MessageDigest digest = MessageDigest.getInstance(SHA_256);
			final byte[] hash = digest.digest(token.toString().getBytes(StandardCharsets.UTF_8));

			return toHex(hash);

		}
		catch (final Exception ex)
		{
			throw new IllegalStateException("Failed to generate checksum", ex);
		}
	}

	private static String toHex(final byte[] bytes)
	{
		final StringBuilder hex = new StringBuilder(bytes.length * 2);
		for (final byte b : bytes)
		{
			hex.append(String.format("%02x", b));
		}
		return hex.toString();
	}

}