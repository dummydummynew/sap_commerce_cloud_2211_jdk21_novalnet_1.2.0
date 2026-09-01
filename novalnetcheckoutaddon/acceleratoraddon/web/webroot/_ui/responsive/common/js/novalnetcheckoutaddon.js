/*
 * [y] hybris Platform
 *
 * Copyright (c) 2017 SAP SE or an SAP affiliate company.  All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */

ACC.novalnetcheckoutaddon = {

	spinner: $("<img id='novalnetLoading' src='" + ACC.config.commonResourcePath + "/images/spinner.gif' />"),


	getGooglePayLocale: function() {
		var lang = ($('#lang').val() || 'en').toLowerCase();
		var country = ($("#countryCode").val() || 'US').toUpperCase();
		var locale = lang + '-' + country;
		return locale;
	},

	initialProcess: function() {
		if ($('#wallet_container').length) {
			// Setting up the Payment Intent for authentication and payment button processing
			var merchantInformation = {
				countryCode: $("#countryCode").val(),
				partnerId: $("#merchantId").val()
			};

			var transactionInformation = {
				amount: $("#orderAmount").val(),
				enforce3d: ($("#enforce3D").val() == "true") ? true : false,
				currency: $("#currency").val(),
				paymentMethod: ($("#currentPayment").val() == 'novalnetGooglePay') ? 'GOOGLEPAY' : 'APPLEPAY',
				environment: ($("#testMode").val() == "true") ? "SANDBOX" : "PRODUCTION",
			};

			var buttonInformation = {
				type: $('#novalnetGooglepayButtonType').val(),
				style: "black",
				locale: ACC.novalnetcheckoutaddon.getGooglePayLocale(),
				boxSizing: "fill",
				dimensions: {
					width: 0,
					height: parseInt($('#novalnetGooglepayButtonHeight').val())
				}
			};

			var paymentIntent = {
				clientKey: $('#Clientkey').val(),
				paymentIntent: {
					merchant: merchantInformation,
					transaction: transactionInformation,
					button: buttonInformation,
					callbacks: {

						onPaymentButtonClicked: function(bookingResult) {
							bookingResult({ status: $("#Terms1").prop("checked") ? 'SUCCESS' : 'FAILURE' });
							if ($("#Terms1").prop("checked") == false) {
								if (!$('#termserror').length) {
									var errorMessage = $("#termsCheckErrorMessage").val();
									var div = $("<div>").attr("id", "termserror").attr("class", "alert alert-danger alert-dismissable getAccAlert").text(errorMessage);
									$('#placeOrderForm1').prepend($(div));
								}
								return false;
							}
						},
						"onProcessCompletion": function(payLoad, bookingResult) {

							// Handle response here and setup the bookingresult
							if (payLoad.result && payLoad.result.status) {

								// Only on success, we proceed further with the booking   
								if (payLoad.result.status == 'SUCCESS') {
									// Sending the token and amount to Novalnet server for the booking
									$.post('bookWalletTransaction', { token: payLoad.transaction.token, doRedirect: payLoad.transaction.doRedirect, amount: payLoad.transaction.amount }).done(function(processedResult) {

										processedResult = $.parseJSON(processedResult);
										const validStatus = ['CONFIRMED', 'ON_HOLD'];
										if (processedResult.result.status == "SUCCESS" && processedResult.result.redirect_url != undefined) {
											window.location = processedResult.result.redirect_url;
										} else if (processedResult.hasOwnProperty('transaction') && validStatus.includes(processedResult.transaction.status)) {
											bookingResult({ status: "SUCCESS", statusText: "" });
											document.getElementById('placeOrder').click();
										} else {
											bookingResult({ status: "FAILURE", statusText: processedResult.result.status_text });
										}

									});

								} else {

									// Upon failure, displaying the error text 
									if (payLoad.result.status_text) {
										alert(payLoad.result.status_text);
									}
								}
							}
						}
					}
				}
			};
			try {
				// Loading the payment instances
				var NovalnetPaymentInstance = NovalnetPayment();
				var walletNovalnetPaymentObj = NovalnetPaymentInstance.createPaymentObject();

				// Setting up the payment intent in your object 
				walletNovalnetPaymentObj.setPaymentIntent(paymentIntent);

				// Checking for the payment method availability
				walletNovalnetPaymentObj.isPaymentMethodAvailable(function(displayGooglePayButton) {
					// Initiating the Payment Request for the Wallet Payment
					walletNovalnetPaymentObj.addPaymentButton("#wallet_container");
				});

			} catch (e) {
				// Handling the errors from the payment intent setup
				console.log(e.message);
			}
		}
		if ($('#previousSelectedPayment') != undefined && $('#previousSelectedPayment').val() != null && $('#previousSelectedPayment').val() != '') {
			$(document).find('input:radio[name=selectedPaymentMethodId][value=' + $('#previousSelectedPayment').val() + ']').attr('checked', true);
		} else {
			$(document).find('input:radio[name=selectedPaymentMethodId]:first').attr('checked', true);
		}

		ACC.novalnetcheckoutaddon.paymentSelectionProcess();
		$('input:radio[name=selectedPaymentMethodId]').click(function() {
			ACC.novalnetcheckoutaddon.paymentSelectionProcess();
		});

		if ($('.order-billing-address').length && $(".novalnetaddressdata").length) {
			if ($('.order-billing-address').children('.value-order').length) {
				$('.order-billing-address').children('.value-order').replaceWith($(".novalnetaddressdata").html());
			} else {
				$('.order-billing-address').append("<div class = value-order>" + $(".novalnetaddressdata").html() + "</div>")
			}
		}

		if ($('#guaranteeAccountBic').length) {
			$('#guaranteeAccountBic').css('display', 'none');
		}

		if ($('#accountBic').length) {
			$('#accountBic').css('display', 'none');
		}

		if ($('#accountIban').length) {
			$('#accountIban').css('text-transform', 'uppercase');
		}
		if ($('#accountBic').length) {
			$('#accountBic').css('text-transform', 'uppercase');
		}

		if ($('#creditcardSaveData').length) {
			$('#creditcardSaveData').prop('checked', true);
		}

		if ($('#paypalSaveData').length) {
			$('#paypalSaveData').prop('checked', true);
		}

		if ($('#directDebitSepaSaveData').length) {
			$('#directDebitSepaSaveData').prop('checked', true);
		}

		if ($('#directDebitAchSaveData').length) {
			$('#directDebitAchSaveData').prop('checked', true);
		}

		if ($('#guaranteedDirectDebitSepaSaveData').length) {
			$('#guaranteedDirectDebitSepaSaveData').prop('checked', true);
		}

		$("#accountBic").keypress(function(event) {
			return NovalnetUtility.formatBic(event);
		});

		$("#accountIban").keypress(function(event) {
			return NovalnetUtility.formatIban(event, 'accountBic');
		});

		if ($('#guaranteeAccountIban').length) {
			$('#accountIban').css('text-transform', 'uppercase');
		}

		if ($('#guaranteeAccountBic').length) {
			$('#accountBic').css('text-transform', 'uppercase');
		}

		$("#guaranteeAccountIban").keypress(function(event) {
			return NovalnetUtility.formatIban(event, 'guaranteeAccountBic');
		});

		$("#guaranteeAccountIban").on("input", function(event) {
			NovalnetUtility.formatIban(event, "guaranteeAccountBic");
		});

		$("#guaranteeAccountBic").keypress(function(event) {
			return NovalnetUtility.formatBic(event);
		});

		$("#accountIban").on("input", function(event) {
			NovalnetUtility.formatIban(event, 'accountBic');
		});

		$("#accountBic").change(function(event) {
			return NovalnetUtility.formatBic(event);
		});

		if ($('#creditCardOneClickData1').length) {
			$('#creditCardOneClickData1').prop('checked', true);
		}

		if ($('#paypalOneClickData1').length) {
			$('#paypalOneClickData1').prop('checked', true);
		}

		if ($('#directDebitSepaOneClickData1').length) {
			$('#directDebitSepaOneClickData1').prop('checked', true);
		}

		if ($('#novalnetDirectDebitAchOneClickData1').length) {
			$('#novalnetDirectDebitAchOneClickData1').prop('checked', true);
		}

		if ($('#GuaranteedDirectDebitSepaOneClickData1').length) {
			$('#GuaranteedDirectDebitSepaOneClickData1').prop('checked', true);
		}

		acceptedCountry = new Array("DE", "AU", 'AT', 'CH');
		var sepaError = 0;
		var invoiceError = 0;

		if ($('#sepaGuaranteeCheck').length && $('#useDeliveryAddress').is(':not(:checked)')) {
			$('.novalnetGuaranteedDirectDebitSepa').css('display', 'none');
			sepaError = 1;
		}
		if ($('#sepaGuaranteeCheck').length && $('#useDeliveryAddress').is(":checked") && $.inArray($('#ship_country').val(), acceptedCountry) == -1) {
			$('.novalnetGuaranteedDirectDebitSepa').css('display', 'none');
			sepaError = 1;
		}
		if ($('#invoiceGuaranteeCheck').length && $('#useDeliveryAddress').is(':not(:checked)')) {
			$('.novalnetGuaranteedInvoice').css('display', 'none');
			invoiceError = 1;
		}
		if ($('#invoiceGuaranteeCheck').length && $('#useDeliveryAddress').is(":checked") && $.inArray($('#ship_country').val(), acceptedCountry) == -1) {
			$('.novalnetGuaranteedInvoice').css('display', 'none');
			invoiceError = 1;
		}

		$('#useDeliveryAddress').click(function() {
			if ($('#sepaGuaranteeCheck').length && $('#useDeliveryAddress').is(':not(:checked)')) {
				$('.novalnetGuaranteedDirectDebitSepa').css('display', 'none');
				$('#novalnetGuaranteedDirectDebitSepa').prop('checked', false);
				if ($('#sepaforceGuaranteeCheck').length && $('#sepaforceGuaranteeCheck').val() == "true") {
					$('.novalnetSepa').css('display', 'block');
					$('#novalnetDirectDebitSepa').prop('checked', true);
				}
				sepaError = 1;
			} else if ($('#sepaGuaranteeCheck').length && $('#useDeliveryAddress').is(":checked") && $.inArray($('#ship_country').val(), acceptedCountry) == -1) {
				$('.novalnetGuaranteedDirectDebitSepa').css('display', 'none');
				sepaError = 1;
			} else if ($('#sepaGuaranteeCheck').length && $('#useDeliveryAddress').is(":checked") && $.inArray($('#ship_country').val(), acceptedCountry) != -1) {
				$('.novalnetGuaranteedDirectDebitSepa').css('display', 'block');
				$('.novalnetSepa').css('display', 'none');
				$('#novalnetGuaranteedDirectDebitSepa').prop('checked', true);
			}

			if ($('#invoiceGuaranteeCheck').length && $('#useDeliveryAddress').is(':not(:checked)')) {
				$('.novalnetGuaranteedInvoice').css('display', 'none');
				$('#novalnetGuaranteedInvoice').prop('checked', false);
				if ($('#invoiceforceGuaranteeCheck').length && $('#invoiceforceGuaranteeCheck').val() == "true") {
					$('.novalnetInvoice').css('display', 'block');
					$('#novalnetInvoice').prop('checked', true);
				} else {
					$('input:radio[name="selectedPaymentMethodId"]').each(function() {
						if ($(this).attr('id') === 'novalnetGuaranteedInvoice' || $(this).attr('id') === 'novalnetInvoice') {
							return true;
						}
						$(this).prop('checked', true);
						return false;
					});
				}
				invoiceError = 1;
			} else if ($('#invoiceGuaranteeCheck').length && $('#useDeliveryAddress').is(":checked") && $.inArray($('#ship_country').val(), acceptedCountry) == -1) {
				$('.novalnetGuaranteedInvoice').css('display', 'none');
				invoiceError = 1;
			} else if ($('#invoiceGuaranteeCheck').length && $('#useDeliveryAddress').is(":checked") && $.inArray($('#ship_country').val(), acceptedCountry) != -1) {
				$('.novalnetGuaranteedInvoice').css('display', 'block');
				$('.novalnetInvoice').css('display', 'none');
				$('#novalnetGuaranteedInvoice').prop('checked', true);
			}
		});


		var hideInvoice = 0;
		var hidesepa = 0;

		if ($('#novalnetApplePay').length && !NovalnetUtility.isApplePayAllowed()) {
			$('#novalnetApplePay').parent('div').css('display', 'none');
		}
		if ((invoiceError == 1 || !$('#invoiceGuaranteeCheck').length) && ($('#invoiceforceGuaranteeCheck').length && $('#invoiceforceGuaranteeCheck').val() == "false")) {
			$('.novalnetInvoice').css('display', 'block');
			$('.novalnetGuaranteedInvoice').css('display', 'none');
		}

		if ((invoiceError == 1 || !$('#invoiceGuaranteeCheck').length) && ($('#invoiceforceGuaranteeCheck').length && $('#invoiceforceGuaranteeCheck').val() == "false")) {
			$('.novalnetInvoice').css('display', 'none');
			$('.novalnetGuaranteedInvoice').css('display', 'none');
			hideInvoice = 1;
		}

		if ((sepaError == 1 || !$('#sepaGuaranteeCheck').length) && ($('#sepaforceGuaranteeCheck').length && $('#sepaforceGuaranteeCheck').val() == "true")) {
			$('.novalnetSepa').css('display', 'block');
			$('.novalnetGuaranteedDirectDebitSepa').css('display', 'none');
		}

		if ((sepaError == 1 || !$('#sepaGuaranteeCheck').length) && ($('#sepaforceGuaranteeCheck').length && $('#sepaforceGuaranteeCheck').val() == "false")) {
			$('.novalnetSepa').css('display', 'none');
			$('.novalnetGuaranteedDirectDebitSepa').css('display', 'none');
			hidesepa = 1;
		}

		if (hidesepa == 0 && ($('.novalnetGuaranteedDirectDebitSepa').css('display') == 'none' || !$('.novalnetGuaranteedDirectDebitSepa').length)) {
			$('.novalnetSepa').css('display', 'block');
		}

		if ($('.novalnetDirectDebitAch').length) {
			$('.novalnetDirectDebitAch').css('display', 'block');
		}

		if (hideInvoice == 0 && ($('.novalnetGuaranteedInvoice').css('display') == 'none' || !$('.novalnetGuaranteedInvoice').length)) {
			$('.novalnetInvoice').css('display', 'block');
		}

		// Process hash call.
		if ($('#novalnetCreditCard').is(":checked")) {
			ACC.novalnetcheckoutaddon.loadIframe();
		}

		$(".submit_novalnetPaymentDetailsForm").click(function() {
			if ($('#novalnetCreditCard').is(":checked") && $('#novalnetCreditCardPanHash').val() == '') {
				event.preventDefault();
				event.stopImmediatePropagation();
				if ($('#novalnetCreditCardIframe').css('display') != 'none' && $('#novalnetCreditCardIframe').parent('div').css('display') != 'none') {
					NovalnetUtility.getPanHash();
				} else {
					$('#paymentDetailsForm').submit();
				}

			} else {
				$('#paymentDetailsForm').submit();
			}
		});

		$('input:radio[name=directDebitSepaOneClickData1]').click(function() {
			if ($('#directDebitSepaOneClickData1AddNew').is(":checked")) {
				$('.novalnetDirectDebitSepaOneClickForm').css('display', 'block');
				$('.novalnetCreditCardIframe').css('display', 'block');

			} else {
				$('.novalnetDirectDebitSepaOneClickForm').css('display', 'none');
				$('.novalnetCreditCardIframe').css('display', 'none');
			}
		});

		$('input:radio[name=directDebitAchOneClickData1]').click(function() {

			if ($('#novalnetDirectDebitAchOneClickData1AddNew').is(":checked")) {
				$('.novalnetDirectDebitAchOneClickForm').css('display', 'block');
			} else {
				$('.novalnetDirectDebitAchOneClickForm').css('display', 'none');
			}
		});


		$('input:radio[name=guaranteedDirectDebitSepaOneClickData1]').click(function() {
			if ($('#guaranteedDirectDebitSepaOneClickData1AddNew').is(":checked")) {
				$('.novalnetGuaranteedDirectDebitSepaOneClickForm').css('display', 'block');
				$('.novalnetCreditCardIframe').css('display', 'block');

			} else {
				$('.novalnetGuaranteedDirectDebitSepaOneClickForm').css('display', 'none');
				$('.novalnetCreditCardIframe').css('display', 'none');
			}
		});
		$('input:radio[name=creditCardOneClickData1]').click(function() {
			if ($('#creditCardOneClickNewDeatails').is(":checked")) {
				$('.novalnetCreditCardOneClickForm').css('display', 'block');

			} else {
				$('.novalnetCreditCardOneClickForm').css('display', 'none');
			}
		});
		$('input:radio[name=payPalOneClickData1]').click(function() {
			if ($('#payPalOneClickData1AddNew').is(":checked")) {
				$('.novalnetPayPalOneClickForm').css('display', 'block');

			} else {
				$('.novalnetPayPalOneClickForm').css('display', 'none');
			}
		});

	},
	paymentSelectionProcess: function() {
		if ($('input:radio[name=selectedPaymentMethodId]') != undefined) {
			$.each(['novalnetCreditCard', 'novalnetDirectDebitSepa', 'novalnetDirectDebitAch', 'novalnetInvoice', 'novalnetPrepayment', 'novalnetPayPal', 'novalnetOnlineBankTransfer', 'novalnetIdeal', 'novalnetApplePay', 'novalnetGooglePay', 'novalnetTwint', 'novalnetMbWay', 'novalnetAlipay', 'novalnetTrustly', 'novalnetBlik', 'novalnetWechatPay', 'novalnetEps', 'novalnetPrzelewy24', 'novalnetGuaranteedDirectDebitSepa', 'novalnetGuaranteedInvoice', 'novalnetPostFinance', 'novalnetPostFinanceCard', 'novalnetMultibanco', 'novalnetBancontact'], function(index, value) {
				$('input:radio[name=selectedPaymentMethodId]:checked').val() == value ? $('#' + value + 'PaymentForm').show() : $('#' + value + 'PaymentForm').hide();
			});

			if ($('#novalnetDirectDebitSepa').is(":checked") || $('#novalnetGuaranteedDirectDebitSepa').is(":checked")) {

				$('#accountIban').keypress(
					function(event) {
						return ACC.novalnetcheckoutaddon.allowAlphanumericKey(event);
					}
				);
			}

			if ($('#novalnetDirectDebitAch').is(":checked")) {

				$('#novalnetAchAccountHolder').keypress(function(event) {
					return ACC.novalnetcheckoutaddon.allowAchNameKey(event);
				});

				$('#novalnetAchAccountNumber').keypress(function(event) {
					return ACC.novalnetcheckoutaddon.allowAchNumberKey(event);
				});

				$('#novalnetAchRoutingNumber').keypress(function(event) {
					return ACC.novalnetcheckoutaddon.allowAchNumberKey(event);
				});
			}

			$('#novalnetAchAccountHolder').on('input', function() {
				this.value = this.value.replace(/[^a-zA-Z\s]/g, '');
			});

			$('#novalnetAchAccountNumber, #novalnetAchRoutingNumber').on('input', function() {
				this.value = this.value.replace(/[^0-9]/g, '');
			});

			if ($('#novalnetCreditCardIframe').attr("id") != undefined) {

				$('#novalnetCreditCardPaymentOption').on(
					'click', function(event) {

						if ($('#novalnetCreditCardOneClickElements').css('display') == 'none') {
							ACC.novalnetcheckoutaddon.showOneClickForm('novalnetCreditCard');
						} else {
							ACC.novalnetcheckoutaddon.showPaymentForm('novalnetCreditCard');
						}
					}
				);

				if ($('#novalnetCreditCardOneClickProcess').val() == '1') {
					ACC.novalnetcheckoutaddon.showOneClickForm('novalnetCreditCard');
				} else {
					ACC.novalnetcheckoutaddon.showPaymentForm('novalnetCreditCard');
				}
			}
			$('#novalnetDirectDebitSepaPaymentOption').on(
				'click', function(event) {
					if ($('#novalnetDirectDebitSepaOneClickElements').css('display') == 'none') {
						ACC.novalnetcheckoutaddon.showOneClickForm('novalnetDirectDebitSepa');
						$('#novalnetDirectDebitSepaDateOfBirth').parent('div').css('display', 'none');
					} else {
						ACC.novalnetcheckoutaddon.showPaymentForm('novalnetDirectDebitSepa');
						$('#novalnetDirectDebitSepaDateOfBirth').parent('div').css('display', 'block');
					}
				}
			);
			$('#novalnetDirectDebitAchPaymentOption').on(
				'click',
				function(event) {

					if ($('#novalnetDirectDebitAchOneClickElements').css('display') == 'none') {
						ACC.novalnetcheckoutaddon.showOneClickForm('novalnetDirectDebitAch');
					} else {
						ACC.novalnetcheckoutaddon.showPaymentForm('novalnetDirectDebitAch');
					}
				}
			);
		}

		if ($('#novalnetDirectDebitSepaOneClickProcess').val() == '1') {
			ACC.novalnetcheckoutaddon.showOneClickForm('novalnetDirectDebitSepa');
			$('#novalnetDirectDebitSepaDateOfBirth').parent('div').css('display', 'none');
		} else {
			ACC.novalnetcheckoutaddon.showPaymentForm('novalnetDirectDebitSepa');
			$('#novalnetDirectDebitSepaDateOfBirth').parent('div').css('display', 'block');
		}

		if ($('#novalnetDirectDebitAchOneClickProcess').val() == '1') {
			ACC.novalnetcheckoutaddon.showOneClickForm('novalnetDirectDebitAch');
		} else {
			ACC.novalnetcheckoutaddon.showPaymentForm('novalnetDirectDebitAch');
		}
	},


	loadIframe: function() {

		var inline = 0;
		var testMode;
		var sameAsBilling = 0;
		var bill_to_country;

		if ($('#novalnetInlineCC').length) {
			inline = 1;
		}

		if ($('#useDeliveryAddress').is(":checked")) {
			sameAsBilling = 1;
		}

		if (sameAsBilling == 1) {
			bill_to_country = $("#address.country").children("option:selected").val();
			bill_to_zip = $("#address.postcode").val();
			bill_to_city = $("#address.townCity").val();
			bill_to_street = $("#address.line1").val() + ' ' + $("#address.line2").val();
		}

		NovalnetUtility.setClientKey($('#Clientkey').val());

		if ($('#novalnetTestMode').val() != undefined && $('#novalnetTestMode').val() == 1) {
			testMode = 1;
		} else {
			testMode = 0;
		}
		var configurationObject = {

			// You can handle the process here, when specific events occur.
			callback: {

				// Called once the pan_hash (temp. token) created successfully.
				on_success: function(data) {
					document.getElementById('novalnetCreditCardPanHash').value = data['hash'];
					document.getElementById('novalnetCreditCardUniqueId').value = data['unique_id'];
					document.getElementById('do_redirect').value = data['do_redirect'];
					$('#paymentDetailsForm').submit();
					return true;
				},

				// Called in case of an invalid payment data or incomplete input.
				on_error: function(data) {
					document.getElementById("nn_overlay").classList.remove("novalnet-challenge-window-overlay");
					document.getElementById("novalnetCreditCardIframe").classList.remove("novalnet-challenge-window-overlay");
					if (undefined !== data['error_message']) {
						alert(data['error_message']);
						return false;
					}
				},

				// Called in case the challenge window Overlay (for 3ds2.0) displays
				on_show_overlay: function(data) {
					document.getElementById('novalnetCreditCardIframe').classList.add("novalnet-challenge-window-overlay");
				},

				// Called in case the Challenge window Overlay (for 3ds2.0) hided
				on_hide_overlay: function(data) {
					document.getElementById("novalnetCreditCardIframe").classList.remove("novalnet-challenge-window-overlay");
					document.getElementById("nn_overlay").classList.add("novalnet-challenge-window-overlay");
				}
			},

			iframe: {

				// It is mandatory to pass the Iframe ID here.  Based on which the entire process will took place.
				id: "novalnetCreditCardIframe",

				// Set to 1 to make you Iframe input container more compact (default - 0)
				inline: inline,

				// Add the style (css) here for either the whole Iframe contanier or for particular label/input field
				style: {
					// The css for the Iframe container
					container: $('#novalnetStandardCss').val(),

					// The css for the input field of the Iframe container
					input: $('#novalnetStandardInputCss').val(),

					// The css for the label of the Iframe container
					label: $('#novalnetStandardLabelCss').val()
				},

				// You can customize the text of the Iframe container here
				text: {

					// The End-customers selected language. The Iframe container will be rendered in this Language.
					lang: $('#lang').val(),

					// Basic Error Message
					error: "Your credit card details are invalid",

					// You can customize the text for the Card Holder here
					card_holder: {

						// You have to give the Customized label text for the Card Holder Container here
						label: ACC.addons.novalnetcheckoutaddon['novalnet.creditcard.holder.label.text'],

						// You have to give the Customized placeholder text for the Card Holder Container here
						place_holder: ACC.addons.novalnetcheckoutaddon['novalnet.creditcard.holder.placeholder.text'],

						// You have to give the Customized error text for the Card Holder Container here
						error: "Please enter the valid card holder name"
					},

					card_number: {

						// You have to give the Customized label text for the Card Number Container here
						label: ACC.addons.novalnetcheckoutaddon['novalnet.creditcard.number.label.text'],

						// You have to give the Customized placeholder text for the Card Number Container here
						place_holder: ACC.addons.novalnetcheckoutaddon['novalnet.creditcard.number.placeholder.text'],

						// You have to give the Customized error text for the Card Number Container here
						error: "Please enter the valid card number"
					},
					expiry_date: {

						// You have to give the Customized label text for the Expiry Date Container here
						label: "Expiry date",

						// You have to give the Customized error text for the Expiry Date Container here
						error: "Please enter the valid expiry month / year in the given format"
					},
					cvc: {

						// You have to give the Customized label text for the CVC/CVV/CID Container here
						label: ACC.addons.novalnetcheckoutaddon['novalnet.creditcard.cvc.label.text'],

						// You have to give the Customized placeholder text for the CVC/CVV/CID Container here
						place_holder: ACC.addons.novalnetcheckoutaddon['novalnet.creditcard.cvc.placeholder.text'],

						// You have to give the Customized error text for the CVC/CVV/CID Container here
						error: "Please enter the valid CVC/CVV/CID"
					}
				}
			},

			// Add Customer data
			customer: {

				// Your End-customer's First name which will be prefilled in the Card Holder field
				first_name: $('#customerFirstName').val(),

				// Your End-customer's Last name which will be prefilled in the Card Holder field
				last_name: $('#customerLastName').val(),

				// Your End-customer's Email ID.
				email: $('#email').val(),

				// Your End-customer's billing address.
				billing: {

					// Your End-customer's billing street (incl. House no).
					street: $('#ship_street').val(),

					// Your End-customer's billing city.
					city: $('#ship_city').val(),

					// Your End-customer's billing zip.
					zip: $('#ship_zip').val(),

					// Your End-customer's billing country ISO code.
					country_code: $('#ship_country').val(),
				},
				shipping: {

					// Set to 1 if the billing and shipping address are same and no need to specify shipping details again here.
					"same_as_billing": 1,
				},
			},

			// Add transaction data
			transaction: {

				// The payable amount that can be charged for the transaction (in minor units), for eg:- Euro in Eurocents (5,22 EUR = 522).
				amount: $("#orderAmount").val(),

				// The three-character currency code as defined in ISO-4217.
				currency: $("#currency").val(),

				// Set to 1 for the TEST transaction (default - 0).
				test_mode: testMode
			}
		}
		NovalnetUtility.createCreditCardForm(configurationObject);
	},

	showPaymentForm: function(paymentName) {
		// Assigning payment form functionality.
		$('#' + paymentName + 'PaymentFormElements').css('display', 'block');
		$('#' + paymentName + 'OneClickElements').css('display', 'none');
		$('#' + paymentName + 'OneClickProcess').val('0');
		$('#' + paymentName + 'PaymentOption').html(ACC.addons.novalnetcheckoutaddon['novalnet.' + paymentName + 'GivenDetails.text']);
	},

	showOneClickForm: function(paymentName) {
		// Assigning one click shop functionality.
		$('#' + paymentName + 'PaymentFormElements').css('display', 'none');
		$('#' + paymentName + 'OneClickElements').css('display', 'block');
		$('#' + paymentName + 'OneClickProcess').val('1');
		$('#' + paymentName + 'PaymentOption').html(ACC.addons.novalnetcheckoutaddon['novalnet.' + paymentName + 'NewDetails.text']);
		$('#' + paymentName + 'OneClickElements input[type="text"]').prop('readonly', true);
		$('#' + paymentName + 'PaymentOption').removeAttr('class');
	},
	allowAlphanumericKey: function(event) {

		var keycode = ('which' in event) ? event.which : event.keyCode,
			reg = /^(?:[0-9a-zA-Z]+$)/;
		return (reg.test(String.fromCharCode(keycode)) || 0 === keycode || 8 === keycode);
	},
	allowNameKey: function(event) {

		var keycode = ('which' in event) ? event.which : event.keyCode,
			reg = /[^0-9\[\]\/\\#,+@!^()$~%'"=:;<>{}\_\|*?`]/g;
		return (reg.test(String.fromCharCode(keycode)) || 0 === keycode || 8 === keycode);
	},
	allowAchNameKey: function(event) {

		var keycode = ('which' in event) ? event.which : event.keyCode,
			reg = /^[a-zA-Z\s]$/;

		return (reg.test(String.fromCharCode(keycode)) || keycode === 0 || keycode === 8);
	}, allowAchNumberKey: function(event) {

		var keycode = ('which' in event) ? event.which : event.keyCode,
			reg = /^[0-9]$/;

		return (reg.test(String.fromCharCode(keycode)) || keycode === 0 || keycode === 8);
	},
}

$(document).ready(function() {
	if ($("#novalnetComments") != undefined && $(".orderBox.payment") != undefined && (parseFloat($("#novalnetComments").css("height")) > parseFloat($(".orderBox.payment").css("height")))) {
		$(".orderBox.payment").css("height",
			parseFloat($(".orderBox.payment").css("height")) + parseFloat($("#novalnetComments").css("height"))
		);
	}
	if ($('#cancelorderForm') != undefined) {
		$('#cancelorderForm').hide();
	}

	if ($('.order-payment-data').find('.value-order') != undefined) {
		if ($('.order-payment-data').find('.value-order').html() != undefined) {
			var orderCommernts = $('.order-payment-data').find('.value-order').html();
			$('.order-payment-data').find('.value-order').html("<span class='novalnet-transaction-comments' >" + orderCommernts.replace(/&lt;br&gt;/g, "</span>&nbsp;<span class='novalnet-transaction-comments'>") + "</span>");
		}
	}

	with (ACC.novalnetcheckoutaddon) {
		initialProcess();
		return true;
	}

	if ($('#paygateurl').length) {
		window.open($('#paygateurl').val(), '_self');
	}


});