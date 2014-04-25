var NRS = (function(NRS, $, undefined) {
	NRS.forms = {
		"errorMessages": {}
	};

	$(".modal form input").keydown(function(e) {
		if (e.which == "13") {
			e.preventDefault();
			if (NRS.settings["submit_on_enter"] && e.target.type != "textarea") {
				$(this).submit();
			} else {
				return false;
			}
		}
	});

	$(".modal button.btn-primary:not([data-dismiss=modal])").click(function() {
		NRS.submitForm($(this).closest(".modal"), $(this));
	});

	NRS.submitForm = function($modal, $btn) {
		if (!$btn) {
			$btn = $modal.find("button.btn-primary:not([data-dismiss=modal])");
		}

		var $modal = $btn.closest(".modal");

		$modal.modal("lock");
		$modal.find("button").prop("disabled", true);
		$btn.button("loading");

		var requestType = $modal.find("input[name=request_type]").val();
		var successMessage = $modal.find("input[name=success_message]").val();
		var errorMessage = $modal.find("input[name=error_message]").val();
		var data = null;

		var formFunction = NRS["forms"][requestType];

		var originalRequestType = requestType;

		var $form = $modal.find("form:first");

		var invalidElement = false;

		$form.find(":input").each(function() {
			if ($(this).is(":invalid")) {
				$modal.find(".error_message").html($(this).attr("name").capitalize().escapeHTML() + " is invalid.").show();
				NRS.unlockForm($modal, $btn);
				invalidElement = true;
				return false;
			}
		});

		if (invalidElement) {
			return;
		}

		if (typeof formFunction == 'function') {
			var output = formFunction($modal);

			if (!output) {
				return;
			} else if (output.error) {
				$modal.find(".error_message").html(output.error.escapeHTML()).show();
				NRS.unlockForm($modal, $btn);
				return;
			} else {
				if (output.requestType) {
					requestType = output.requestType;
				}
				if (output.data) {
					data = output.data;
				}
				if (output.successMessage) {
					successMessage = output.successMessage;
				}
				if (output.errorMessage) {
					errorMessage = output.errorMessage;
				}
				if (output.stop) {
					NRS.unlockForm($modal, $btn, true);
					return;
				}
			}
		}

		if (!data) {
			data = NRS.getFormData($modal.find("form:first"));
		}

		if (data.deadline) {
			data.deadline = String(data.deadline * 60); //hours to minutes
		}

		if (data.recipient) {
			data.recipient = $.trim(data.recipient);
			if (!/^\d+$/.test(data.recipient)) {
				var convertedAccountId = $modal.find("input[name=converted_account_id]").val();
				if (!convertedAccountId || !/^\d+$/.test(convertedAccountId)) {
					$modal.find(".error_message").html("Invalid account ID.").show();
					NRS.unlockForm($modal, $btn);
					return;
				} else {
					data.recipient = convertedAccountId;
					data["_extra"] = {
						"convertedAccount": true
					};
				}
			}
		}

		if ("secretPhrase" in data && !data.secretPhrase.length && !NRS.rememberPassword) {
			$modal.find(".error_message").html("Secret phrase is a required field.").show();
			NRS.unlockForm($modal, $btn);
			return;
		}

		NRS.sendRequest(requestType, data, function(response) {
			if (response.errorCode) {
				if (NRS.forms.errorMessages[requestType] && NRS.forms.errorMessages[requestType][response.errorCode]) {
					$modal.find(".error_message").html(NRS.forms.errorMessages[requestType][response.errorCode].escapeHTML()).show();
				} else if (NRS.forms.errorMessages[originalRequestType] && NRS.forms.errorMessages[originalRequestType][response.errorCode]) {
					$modal.find(".error_message").html(NRS.forms.errorMessages[originalRequestType][response.errorCode].escapeHTML()).show();
				} else {
					$modal.find(".error_message").html(response.errorDescription ? response.errorDescription.escapeHTML() : "Unknown error occured.").show();
				}
				NRS.unlockForm($modal, $btn);
			} else if (response.hash) {
				//should we add a fake transaction to the recent transactions?? or just wait until the next block comes!??
				NRS.unlockForm($modal, $btn);

				if (!$modal.hasClass("modal-no-hide")) {
					$modal.modal("hide");
				}

				if (successMessage) {
					$.growl(successMessage.escapeHTML(), {
						type: 'success'
					});
				}

				var formCompleteFunction = NRS["forms"][originalRequestType + "Complete"];

				if (typeof formCompleteFunction == 'function') {
					data.requestType = requestType;
					formCompleteFunction(response, data);
				}

				if (NRS.accountInfo && !NRS.accountInfo.publicKey) {
					$("#dashboard_message").hide();
				}
			} else {
				var sentToFunction = false;

				if (!errorMessage) {
					var formCompleteFunction = NRS["forms"][originalRequestType + "Complete"];

					if (typeof formCompleteFunction == 'function') {
						sentToFunction = true;
						data.requestType = requestType;

						NRS.unlockForm($modal, $btn);

						if (!$modal.hasClass("modal-no-hide")) {
							$modal.modal("hide");
						}
						formCompleteFunction(response, data);
					} else {
						errorMessage = "An unknown error occured.";
					}
				}

				if (!sentToFunction) {
					NRS.unlockForm($modal, $btn, true);

					$.growl(errorMessage.escapeHTML(), {
						type: 'danger'
					});
				}
			}
		});
	}

	NRS.unlockForm = function($modal, $btn, hide) {
		$modal.find("button").prop("disabled", false);
		if ($btn) {
			$btn.button("reset");
		}
		$modal.modal("unlock");
		if (hide) {
			$modal.modal("hide");
		}
	}

	return NRS;
}(NRS || {}, jQuery));