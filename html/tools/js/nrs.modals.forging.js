var NRS = (function(NRS, $, undefined) {
	NRS.forms.errorMessages.startForging = {
		"5": "You cannot forge. Either your balance is 0 or your account is too new (you must wait a day or so)."
	};

	NRS.forms.startForgingComplete = function(response, data) {
		if ("deadline" in response) {
			$("#forging_indicator i.fa").removeClass("text-danger").addClass("text-success");
			$("#forging_indicator span").html("Forging");
			NRS.isForging = true;
			$.growl("Forging started successfully.", {
				type: "success"
			});
		} else {
			NRS.isForging = false;
			$.growl("Couldn't start forging, unknown error.", {
				type: 'danger'
			});
		}
	}

	NRS.forms.stopForgingComplete = function(response, data) {
		if ($("#stop_forging_modal .show_logout").css("display") == "inline") {
			NRS.logout();
			return;
		}

		$("#forging_indicator i.fa").removeClass("text-success").addClass("text-danger");
		$("#forging_indicator span").html("Not forging");

		NRS.isForging = false;

		if (response.foundAndStopped) {
			$.growl("Forging stopped successfully.", {
				type: 'success'
			});
		} else {
			$.growl("You weren't forging to begin with.", {
				type: 'danger'
			});
		}
	}

	$("#forging_indicator").click(function(e) {
		e.preventDefault();

		var $forgingIndicator = $(this).find("i.fa-circle");

		if ($forgingIndicator.hasClass("text-success")) {
			$("#stop_forging_modal").modal("show");
		} else {
			$("#start_forging_modal").modal("show");
		}
	});

	return NRS;
}(NRS || {}, jQuery));