/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.forms.leaseBalanceComplete = function(response, data) {
		NRS.getAccountInfo();
	}

	$("#lease_balance_modal").on("show.bs.modal", function() {
		$("#lease_balance_help").html("A lease of <strong>9000 blocks</strong> is about 10 days.");
	});

	$("#lease_balance_period").on("change", function() {
		if ($(this).val() > 32767) {
			$("#lease_balance_help").html("Lease period cannot be higher than <strong>32767 blocks</strong>.");
		} else {
			var days = Math.round($(this).val() / 900);
			$("#lease_balance_help").html("A lease of <strong>" + $(this).val() + " blocks</strong> is about " + Math.round(days) + " days.");
		}
	});

	return NRS;
}(NRS || {}, jQuery));