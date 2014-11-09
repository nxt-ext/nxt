/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.pages.polls = function() {
		NRS.sendRequest("getPollIds+", function(response) {
			if (response.pollIds && response.pollIds.length) {
				var polls = {};
				var nrPolls = 0;

				for (var i = 0; i < response.pollIds.length; i++) {
					NRS.sendRequest("getTransaction+", {
						"transaction": response.pollIds[i]
					}, function(poll, input) {
						if (NRS.currentPage != "polls") {
							polls = {};
							return;
						}

						if (!poll.errorCode) {
							polls[input.transaction] = poll;
						}

						nrPolls++;

						if (nrPolls == response.pollIds.length) {
							var rows = "";

							if (NRS.unconfirmedTransactions.length) {
								for (var i = 0; i < NRS.unconfirmedTransactions.length; i++) {
									var unconfirmedTransaction = NRS.unconfirmedTransactions[i];

									if (unconfirmedTransaction.type == 1 && unconfirmedTransaction.subType == 2) {
										var pollDescription = String(unconfirmedTransaction.attachment.description);

										if (pollDescription.length > 100) {
											pollDescription = pollDescription.substring(0, 100) + "...";
										}

										rows += "<tr class='tentative'><td>" + String(unconfirmedTransaction.attachment.name).escapeHTML() + "</td><td>" + pollDescription.escapeHTML() + "</td><td>" + (unconfirmedTransaction.sender != NRS.genesis ? "<a href='#' data-user='" + NRS.getAccountFormatted(unconfirmedTransaction, "sender") + "' class='user_info'>" + NRS.getAccountTitle(unconfirmedTransaction, "sender") + "</a>" : "Genesis") + "</td><td>" + NRS.formatTimestamp(unconfirmedTransaction.timestamp) + "</td><td>" + String(unconfirmedTransaction.attachment.finishBlockHeight - NRS.lastBlockHeight)  + "</td><td><a href='#'>Vote (todo)</td></tr>";
									}
								}
							}

							for (var i = 0; i < nrPolls; i++) {
								var poll = polls[response.pollIds[i]];

								if (!poll) {
									continue;
								}

								var pollDescription = String(poll.attachment.description);

								if (pollDescription.length > 100) {
									pollDescription = pollDescription.substring(0, 100) + "...";
								}
								rows += "<tr><td><a href='#' data-transaction='"+poll.transaction+"'>" + String(poll.attachment.name).escapeHTML() + "</a></td><td>" + pollDescription.escapeHTML() + "</td><td>" + (poll.sender != NRS.genesis ? "<a href='#' data-user='" + NRS.getAccountFormatted(poll, "sender") + "' class='user_info'>" + NRS.getAccountTitle(poll, "sender") + "</a>" : "Genesis") + "</td><td>" + NRS.formatTimestamp(poll.timestamp) + "</td><td>" + String(poll.attachment.finishBlockHeight - NRS.lastBlockHeight) + "</td><td><a href='#' data-toggle='modal' data-target='#cast_vote_modal'>Vote </td></tr>";
							}

							NRS.dataLoaded(rows);
						}
					});
				}
			} else {
				NRS.dataLoaded();
			}
		});
	}

	NRS.incoming.polls = function() {
		NRS.loadPage("polls");
	}

	$("#create_poll_answers").on("click", "button.btn.remove_answer", function(e) {
		e.preventDefault();

		if ($("#create_poll_answers > .form-group").length == 1) {
			return;
		}

		$(this).closest("div.form-group").remove();
	});

	$("#create_poll_answers_add").click(function(e) {
		var $clone = $("#create_poll_answers > .form-group").first().clone();

		$clone.find("input").val("");

		$clone.appendTo("#create_poll_answers");
	});

	$("#create_poll_type").change(function() {
		// poll type changed, lets see if we have to include/remove the asset id
		if($("#create_poll_type").val() == "2") {
			$("#create_poll_asset_id_group").css("display", "inline");
			$("#create_poll_type_group").removeClass("col-xs-12").addClass("col-xs-6");
			$("#create_poll_type_group").removeClass("col-sm-12").addClass("col-sm-6");
			$("#create_poll_type_group").removeClass("col-md-12").addClass("col-md-6");
		}
		else {
			$("#create_poll_asset_id_group").css("display", "none");
			$("#create_poll_type_group").removeClass("col-xs-6").addClass("col-xs-12");
			$("#create_poll_type_group").removeClass("col-sm-6").addClass("col-sm-12");
			$("#create_poll_type_group").removeClass("col-md-6").addClass("col-md-12");
		}

	});

	NRS.forms.createPoll = function($modal) {
		var options = new Array();

		$("#create_poll_answers input.create_poll_answers").each(function() {
			var option = $.trim($(this).val());

			if (option) {
				options.push(option);
			}
		});

		if (!options.length) {
			//...
		}

		$("#create_poll_")
		var data = {
			"name": $("#create_poll_name").val(),
			"description": $("#create_poll_description").val(),
			"finishHeight": (parseInt(NRS.lastBlockHeight) + parseInt($("#create_poll_duration").val())),
			"minNumberOfOptions": $("#create_poll_min_options").val(),
			"maxNumberOfOptions": $("#create_poll_max_options").val(),
			"minRangeValue": $("#create_poll_min_options").val(),
			"maxRangeValue": $("#create_poll_max_options").val(),
			"minBalance": $("#create_poll_min_balance").val(),
			"feeNQT": (parseInt($("#create_poll_fee").val()) * 100000000),
			"deadline": "24",
			"secretPhrase": $("#create_poll_password").val()
		};

		if($("#create_poll_type").val() == "0")
		{
			data["votingModel"] = 0;

		}
		if($("#create_poll_type").val() == "1")
		{
			data["votingModel"] = 1;
		}
		if($("#create_poll_type").val() == "2")
		{
			data["votingModel"] = 2;
			data["assetId"] = $("#create_poll_asset_id").val();
		}

		for (var i = 0; i < options.length; i++) {
			data["option" + i] = options[i];
		}



		return {
			"requestType": "createPoll",
			"data": data
		};
	}

	NRS.forms.createPollComplete = function(response, data) {
		if (NRS.currentPage == "polls") {
			var $table = $("#polls_table tbody");

			var date = new Date(Date.UTC(2013, 10, 24, 12, 0, 0, 0)).getTime();

			var now = parseInt(((new Date().getTime()) - date) / 1000, 10);

			var rowToAdd = "<tr class='tentative'><td>" + String(data.name).escapeHTML() + " - <strong>" + $.t("pending") + "</strong></td><td>" + String(data.description).escapeHTML() + "</td><td><a href='#' data-user='" + NRS.getAccountFormatted(NRS.accountRS) + "' class='user_info'>" + NRS.getAccountTitle(NRS.accountRS) + "</a></td><td>" + NRS.formatTimestamp(now) + "</td><td>/</td></tr>";

			$table.prepend(rowToAdd);

			if ($("#polls_table").parent().hasClass("data-empty")) {
				$("#polls_table").parent().removeClass("data-empty");
			}
		}
	}

	NRS.forms.castVote = function($modal) {

	}

	return NRS;
}(NRS || {}, jQuery));

