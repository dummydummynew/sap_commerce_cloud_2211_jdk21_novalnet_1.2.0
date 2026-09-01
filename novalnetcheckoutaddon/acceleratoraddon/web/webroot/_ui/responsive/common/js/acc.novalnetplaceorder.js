ACC.novalnetplaceorder = {
	paymentOption: '',

	bindAll: function() {
		this.bindPlaceOrder();
	},

	bindPlaceOrder: function() {
		$("#Terms1").on("click", function() {
			if ($("#Terms1").prop("checked") == true) {
				$("#placeOrder").removeAttr("disabled");
			} else {
				$("#placeOrder").attr('disabled', 'disabled');
			}
		});

		$("#placeOrder").on("click", function() {
			if ($("#Terms1").prop("checked") == false) {
				if (!$('#termserror').length) {
					var errorMessage = $("#termsCheckErrorMessage").val();
					var div = $("<div>").attr("id", "termserror").attr("class", "alert alert-danger alert-dismissable getAccAlert").text(errorMessage);
					$('#placeOrderForm1').prepend($(div));
				}
				return false;

			} else {
				$("#placeOrder").attr('disabled', 'disabled');
				$("#placeOrderForm1").submit();

			}
		});

	},


	updatePlaceOrderButton: function() {
		$("#placeOrder").removeAttr("disabled");
		// need rewrite /  class changes
	}
};

$(document).ready(function() {
	ACC.novalnetplaceorder.bindAll();
});


