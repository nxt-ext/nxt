/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.pages.polls = function() {
		NRS.sendRequest("getPolls+", function(response) {
			if (response.polls && response.polls.length) {
				var polls = {};
				var nrPolls = 0;

				for (var i = 0; i < response.polls.length; i++) {
					NRS.sendRequest("getTransaction+", {
						"transaction": response.polls[i].poll
					}, function(poll, input) {
						if (NRS.currentPage != "polls") {
							polls = {};
							return;
						}

						if (!poll.errorCode && poll.attachment.finishHeight >= NRS.lastBlockHeight) {
							polls[input.transaction] = poll;
						}

						nrPolls++;


						if (nrPolls == response.polls.length) {
							var rows = "";

							for (var i = 0; i < nrPolls; i++) {
								var poll = polls[response.polls[i].poll];

								if (!poll) {
									continue;
								}

								var pollDescription = String(poll.attachment.description);

								if (pollDescription.length > 100) {
									pollDescription = pollDescription.substring(0, 100) + "...";
								}
								rows += "<tr>";
								rows += "<td><a class='poll_list_title show_transaction_modal_action' href='#' data-transaction='"+poll.transaction+"'>" + String(poll.attachment.name).escapeHTML() + "</a></td>";
								rows += "<td>" + pollDescription.escapeHTML() + "</td>";
								rows += "<td>" + (poll.sender != NRS.genesis ? "<a href='#' data-user='" + NRS.getAccountFormatted(poll, "sender") + "' class='show_account_modal_action user_info'>" + NRS.getAccountTitle(poll, "sender") + "</a>" : "Genesis") + "</td>";
								rows += "<td>" + NRS.formatTimestamp(poll.timestamp) + "</td>";
								rows += "<td>" + String(poll.attachment.finishHeight - NRS.lastBlockHeight) + "</td>";
								rows += "<td><a href='#' class='vote_button' data-poll='" + poll.transaction +"'>Vote </a></td>";
								rows += "<td><a href='#' class='follow_button' data-follow='" + poll.transaction + "'>Follow </a></td>";
								rows += "</tr>";
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

	NRS.pages.my_polls = function() {
		NRS.sendRequest("getPolls+", {"account": NRS.account}, function(response) {
			if (response.polls && response.polls.length) {
				var polls = {};
				var nrPolls = 0;

				for (var i = 0; i < response.polls.length; i++) {
					NRS.sendRequest("getTransaction+", {
						"transaction": response.polls[i].poll
					}, function(poll, input) {
						if (NRS.currentPage != "my_polls") {
							polls = {};
							return;
						}

						if (!poll.errorCode) {
							polls[input.transaction] = poll;
						}

						nrPolls++;


						if (nrPolls == response.polls.length) {
							var rows = "";

							for (var i = 0; i < nrPolls; i++) {
								var poll = polls[response.polls[i].poll];

								if (!poll) {
									continue;
								}

								var pollDescription = String(poll.attachment.description);

								if (pollDescription.length > 100) {
									pollDescription = pollDescription.substring(0, 100) + "...";
								}
								rows += "<tr>"
								rows += "<td><a class='poll_list_title show_transaction_modal_action' href='#'  data-transaction='"+poll.transaction+"'>" + String(poll.attachment.name).escapeHTML() + "</a></td>";
								rows += "<td>" + pollDescription.escapeHTML() + "</td>";
								rows += "<td>" + (poll.sender != NRS.genesis ? "<a href='#' data-user='" + NRS.getAccountFormatted(poll, "sender") + "' class='show_account_modal_action user_info'>" + NRS.getAccountTitle(poll, "sender") + "</a>" : "Genesis") + "</td>"
								rows += "<td>" + NRS.formatTimestamp(poll.timestamp) + "</td>";
								if(poll.attachment.finishHeight > NRS.lastBlockHeight)
								{
									rows += "<td>" + String(poll.attachment.finishHeight - NRS.lastBlockHeight) + "</td>";
									rows += "<td><a href='#' class='vote_button' data-poll='" + poll.transaction +"'>Vote </td>";
								}
								else
								{
									rows += "<td>Complete</td>";
									rows += "<td><a href='#' class='results_button' data-results='" + poll.transaction +"'>Results </a></td>";
								}
								rows += "<td><a href='#' class='follow_button' data-follow='" + poll.transaction + "'>Follow </a></td>";
								rows += "</tr>";
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

	NRS.incoming.my_polls = function() {
		NRS.loadPage("my_polls");
	}

	NRS.pages.voted_polls = function() {
		NRS.sendRequest("getAccountTransactions",{"account": NRS.accountRS, "type": 1, "subtype": 3}, function(response) {
			
			if (response.transactions && response.transactions.length > 0) {
				var polls = {};
				var nrPolls = 0;

				for (var i = 0; i < response.transactions.length; i++) {
					NRS.sendRequest("getTransaction", {
						"transaction": response.transactions[i].attachment.poll
					}, function(poll, input) {
						if (NRS.currentPage != "voted_polls") {
							polls = {};
							return;
						}

						if (!poll.errorCode) {
							polls[input.transaction] = poll;
						}

						nrPolls++;

						if (nrPolls == response.transactions.length) {
							var rows = "";

							for (var i = 0; i < nrPolls; i++) {
								var poll = polls[response.transactions[i].attachment.poll];

								if (!poll) {
									continue;
								}
								var pollDescription = String(poll.attachment.description);

								if (pollDescription.length > 100) {
									pollDescription = pollDescription.substring(0, 100) + "...";
								}
								rows += "<tr>"
								rows += "<td><a class='poll_list_title show_transaction_modal_action' href='#' data-transaction='"+poll.transaction+"'>" + String(poll.attachment.name).escapeHTML() + "</a></td>";
								rows += "<td>" + pollDescription.escapeHTML() + "</td>";
								rows += "<td>" + (poll.sender != NRS.genesis ? "<a href='#' data-user='" + NRS.getAccountFormatted(poll, "sender") + "' class='show_account_modal_action user_info'>" + NRS.getAccountTitle(poll, "sender") + "</a>" : "Genesis") + "</td>"
								rows += "<td>" + NRS.formatTimestamp(poll.timestamp) + "</td>";
								if(poll.attachment.finishHeight > NRS.lastBlockHeight)
								{
									rows += "<td>" + String(poll.attachment.finishHeight - NRS.lastBlockHeight) + "</td>";
									rows += "<td>Waiting... </td>";

								}
								else
								{
									rows += "<td>Complete</td>";
									rows += "<td><a href='#' class='results_button' data-results='" + poll.transaction +"'>Results </a></td>";
								}
								rows += "<td><a href='#' class='follow_button' data-follow='" + poll.transaction + "'>Follow </a></td>";
								rows += "</tr>";
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

	NRS.incoming.voted_polls = function() {
		NRS.loadPage("voted_polls");
	}

	NRS.setup.polls = function() {
		var sidebarId = 'sidebar_voting_system';
		var options = {
			"id": sidebarId,
			"titleHTML": '<i class="fa fa-check-square-o"></i><span data-i18n="voting_system">Voting</span>',
			"page": 'polls',
			"desiredPosition": 50
		}
		NRS.addTreeviewSidebarMenuItem(options);
		options = {
			"titleHTML": '<span data-i18n="active_polls">Active Polls</span>',
			"type": 'PAGE',
			"page": 'polls'
		}
		NRS.appendMenuItemToTSMenuItem(sidebarId, options);
		options = {
			"titleHTML": '<span data-i18n="followed_polls">Followed Polls</span>',
			"type": 'PAGE',
			"page": 'followed_polls'
		}
		NRS.appendMenuItemToTSMenuItem(sidebarId, options);
		options = {
			"titleHTML": '<span data-i18n="my_polls">My Polls</span>',
			"type": 'PAGE',
			"page": 'my_polls'
		}
		NRS.appendMenuItemToTSMenuItem(sidebarId, options);
		options = {
			"titleHTML": '<span data-i18n="my_votes">My Votes</span>',
			"type": 'PAGE',
			"page": 'voted_polls'
		}
		NRS.appendMenuItemToTSMenuItem(sidebarId, options);
		options = {
			"titleHTML": '<span data-i18n="create_poll">Create Poll</span>',
			"type": 'MODAL',
			"modalId": 'create_poll_modal'
		}
		NRS.appendMenuItemToTSMenuItem(sidebarId, options);

		$('#create_poll_modal input[name="feeNXT"]').attr('min', 10);
		$('#create_poll_modal input[name="feeNXT"]').data('default', 10);
		$('#create_poll_modal input[name="feeNXT"]').val(10);
	}


	$("#create_poll_answers").on("click", "button.btn.remove_answer", function(e) {
		e.preventDefault();

		if ($("#create_poll_answers > .form-group").length == 1) {
			return;
		}

		$(this).closest("div.form-group").remove();
		var options = $(".create_poll_answers").length;
		if(options > 20)
		{
			var fee = (options - 20) + 10;
			$("#create_poll_fee").text(fee + " NXT");
			$("#create_poll_fee_text").text(fee + " NXT");

		}
		else
		{ 
			$("#create_poll_fee").text("10 NXT");
			$("#create_poll_fee_text").text(fee + " NXT");
		}
	});

	$("#create_poll_answers_add").click(function(e) {
		var $clone = $("#create_poll_answers > .form-group").first().clone();

		$clone.find("input").val("");

		$clone.appendTo("#create_poll_answers");
		var options = $(".create_poll_answers").length;
		console.log(options);
		if(options > 20)
		{
			var fee = (options - 20) + 10;
			console.log(fee);
			console.log($("#create_poll_fee").text())
			$("#create_poll_fee").val(fee);
			$("#create_poll_fee_text").text(fee + " NXT");
		}
		else 
		{
			$("#create_poll_fee").val("10");
			$("#create_poll_fee_text").text(fee + " NXT");
		}
	});

	$("#create_poll_type").change(function() {
		// poll type changed, lets see if we have to include/remove the asset id
		if($("#create_poll_type").val() == "2") {
			$("#create_poll_asset_id_group").css("display", "inline");
			$("#create_poll_ms_currency_group").css("display", "none");
			$("#create_poll_type_group").removeClass("col-xs-12").addClass("col-xs-6");
			$("#create_poll_type_group").removeClass("col-sm-12").addClass("col-sm-6");
			$("#create_poll_type_group").removeClass("col-md-12").addClass("col-md-6");
		}
		else if($("#create_poll_type").val() == "3") {
			$("#create_poll_asset_id_group").css("display", "none");
			$("#create_poll_ms_currency_group").css("display", "inline");
			$("#create_poll_type_group").removeClass("col-xs-12").addClass("col-xs-6");
			$("#create_poll_type_group").removeClass("col-sm-12").addClass("col-sm-6");
			$("#create_poll_type_group").removeClass("col-md-12").addClass("col-md-6");
		}
		else {
			$("#create_poll_asset_id_group").css("display", "none");
			$("#create_poll_ms_currency_group").css("display", "none");
			$("#create_poll_type_group").removeClass("col-xs-6").addClass("col-xs-12");
			$("#create_poll_type_group").removeClass("col-sm-6").addClass("col-sm-12");
			$("#create_poll_type_group").removeClass("col-md-6").addClass("col-md-12");
		}

		if($("#create_poll_type").val() == "1")
		{
			// ok now lets show the bottom things...
			$("#create_poll_min_balance_type_group").css("display", "inline");
		}
		else
		{
			$("#create_poll_min_balance_type_group").css("display", "none");
		}

	});

	$("input[name=minBalanceType]:radio").change(function () {
		var value = $(this).val();

		if(value == "2") {
			$("#create_poll_asset_id_group").css("display", "inline");
			$("#create_poll_ms_currency_group").css("display", "none");
			$("#create_poll_type_group").removeClass("col-xs-12").addClass("col-xs-6");
			$("#create_poll_type_group").removeClass("col-sm-12").addClass("col-sm-6");
			$("#create_poll_type_group").removeClass("col-md-12").addClass("col-md-6");
		}
		else if(value == "3") {
			$("#create_poll_asset_id_group").css("display", "none");
			$("#create_poll_ms_currency_group").css("display", "inline");
			$("#create_poll_type_group").removeClass("col-xs-12").addClass("col-xs-6");
			$("#create_poll_type_group").removeClass("col-sm-12").addClass("col-sm-6");
			$("#create_poll_type_group").removeClass("col-md-12").addClass("col-md-6");
		}
		else {
			$("#create_poll_asset_id_group").css("display", "none");
			$("#create_poll_ms_currency_group").css("display", "none");
			$("#create_poll_type_group").removeClass("col-xs-6").addClass("col-xs-12");
			$("#create_poll_type_group").removeClass("col-sm-6").addClass("col-sm-12");
			$("#create_poll_type_group").removeClass("col-md-6").addClass("col-md-12");
		}

	});


		$("#polls_table, #my_polls_table").on("click", "a[data-poll]", function(e) {
			e.preventDefault();
			var transactionId = $(this).data("poll");

			NRS.sendRequest("getTransaction", {
				"transaction": transactionId
			}, function(response, input) {
				$("#cast_vote_poll_name").text(response.attachment.name);
				$("#cast_vote_poll_description").text(response.attachment.description);
				$("#cast_vote_answers_entry").text("");
				if(response.attachment.minNumberOfOptions != response.attachment.maxNumberOfOptions)
				$("#cast_vote_range").text("Select between " + response.attachment.minNumberOfOptions + " and " + response.attachment.maxNumberOfOptions + " options from below.")
				else if(response.attachment.minNumberOfOptions != 1) $("#cast_vote_range").text("Select " + response.attachment.minNumberOfOptions +  " options from below.")
				else $("#cast_vote_range").text("Select 1 option from below.")

				$("#cast_vote_poll").val(response.transaction);
				if(response.attachment.maxRangeValue != 1)
				{
					for(var b=0; b<response.attachment.options.length; b++)
					{
						$("#cast_vote_answers_entry").append("<div class='answer_slider'><label name='cast_vote_answer_"+b+"'>"+String(response.attachment.options[b]).escapeHTML()+"</label> &nbsp;&nbsp;<span class='badge'>"+response.attachment.minRangeValue+"</span><br/><input class='form-control' step='1' value='"+response.attachment.minRangeValue+"' max='"+response.attachment.maxRangeValue+"' min='"+response.attachment.minRangeValue+"' type='range'/></div>");
					}
				}
				else
				{
					for(var b=0; b<response.attachment.options.length; b++)
					{
						$("#cast_vote_answers_entry").append("<div class='answer_boxes'><label name='cast_vote_answer_"+b+"'><input type='checkbox'/>&nbsp;&nbsp;"+String(response.attachment.options[b]).escapeHTML()+"</label></div>");
					}
				}
				$("#cast_vote_modal").modal();
				$("input[type='range']").on("change mousemove", function() {
					$(this).parent().children(".badge").text($(this).val());

				});
			});

			
		});

    function layoutPollResults(resultsdata, polldata) {
        var results = resultsdata.results;
        var options = polldata.options;

        if (!results) {
            results = [];
        }
        var rows = "";
        if (results.length) {
            for (var i = 0; i < results.length; i++) {
                var result = results[i];
                rows += "<tr>";
                rows += "<td>" + String(options[i]).escapeHTML() + "</td>";
                if (polldata.votingModel == 0) {
                    rows += "<td>" + result.result + "</td>";
                    rows += "<td>" + result.weight + "</td>";
                } else if (polldata.votingModel == 1) {
                    rows += "<td>" + NRS.formatAmount(result.result) + "</td>";
                    rows += "<td>" + NRS.formatAmount(result.weight) + "</td>";
                } else if (resultsdata.holding) {
                    rows += "<td>" + NRS.formatQuantity(result.result, resultsdata.decimals) + "</td>";
                    rows += "<td>" + NRS.formatQuantity(result.weight, resultsdata.decimals) + "</td>";
                }
                rows += "</tr>";
            }
        }
        return rows;
    }

    $("#my_polls_table, #voted_polls_table").on("click", "a[data-results]", function(e) {
			e.preventDefault();
			var transactionId = $(this).data("results");

			NRS.sendRequest("getPollResult", {"poll": transactionId, "req":"getPollResult"}, voteModal);
			NRS.sendRequest("getPollVotes", {"poll": transactionId, "req":"getPollVotes"}, voteModal);
			NRS.sendRequest("getPoll", {"poll": transactionId, "req": "getPoll"}, voteModal);
			var resultsdata, votesdata, polldata;

			function voteModal(data, input)
			{
				if(input.req=="getPollResult") resultsdata = data;
				if(input.req=="getPollVotes") votesdata = data;
				if(input.req=="getPoll") polldata = data;

				if(resultsdata !== undefined && votesdata !== undefined && polldata !== undefined)
				{
					$("#poll_results_options").append("<tr><td style='font-weight: bold;width:180px;'><span data-i18n='poll_name'>Poll Name</span>:</td><td><span id='poll_results_poll_name'>"+String(polldata.name).escapeHTML()+"</span></td></tr>");
					$("#poll_results_options").append("<tr><td style='font-weight: bold;width:180px;'><span data-i18n='poll_id'>Poll Id</span>:</td><td><span id='poll_results_poll_id'>"+polldata.poll+"</span></td></tr>");

					$("#poll_results_poll_name").text(String(polldata.name).escapeHTML());
					$("#poll_results_poll_id").text(polldata.poll);

					//$("#poll_results_number_of_voters").text(votesdata.votes.length);

					$("#poll_results_modal").modal();
                    var rows = layoutPollResults(resultsdata, polldata);
                    $("#poll_results_table tbody").empty().append(rows);

					var votes = votesdata.votes;

					if (!votes) {
						votes = [];
					}

					if (votes.length) 
					{
					
						var head = "";
						head += "<tr>";
						head += "<th data-i18n=\"voter\">Voter</th>";

						for(var b=0; b<polldata.options.length; b++)
						{
							head += "<th>"+String(polldata.options[b].escapeHTML()) + "</th>";
						}
						head += "</tr>";

						$("#poll_voters_table thead").empty().append(head);				

						//$("#votes_cast_count").html("(" + votes.length + ")");

						var rows = "";

						for (var i = 0; i < votes.length; i++) {
							rows += "<tr>";
							var vote = votes[i];

							rows += "<td><a href='#' class='user_info' data-user='" + NRS.getAccountFormatted(vote, "voter") + "'>" + NRS.getAccountTitle(vote, "voter") + "</td>";

							for(var a=0;a<vote.votes.length;a++)
							{
								rows += "<td>" + vote.votes[a] + "</td>";
							}
						}

						$("#poll_voters_table tbody").empty().append(rows);
					}
					else 
					{
						$("#poll_voters_table tbody").empty();
						//$("#votes_cast_count").html("(0)");
					}
				}

			}
		});

		$("#polls_table, #my_polls_table, #voted_polls_table").on("click", "a[data-follow]", function(e) {
			e.preventDefault();
			var pollId = $(this).data("follow");

			NRS.sendRequest("getPoll", {"poll": pollId}, function(response) 
			{
				if (response.errorCode) {
					NRS.showModalError($.t("no_poll_found"), $modal);
				} else {
					NRS.saveFollowedPolls(new Array(response), NRS.forms.addFollowedPollsComplete);
				}
			});
		});

					

$("#poll_results_modal").on("show.bs.modal", function(e) {

		$("#poll_results_modal_statistics").show();
		// now lets put the data in the correct place...
});

$("#poll_results_modal ul.nav li").click(function(e) {
		e.preventDefault();

		var tab = $(this).data("tab");

		$(this).siblings().removeClass("active");
		$(this).addClass("active");

		$(".poll_results_modal_content").hide();

		var content = $("#poll_results_modal_" + tab);

		content.show();
	});

	$("#poll_results_modal").on("hidden.bs.modal", function(e) {
		$(this).find(".poll_results_modal_content").hide();
		$(this).find("ul.nav li.active").removeClass("active");
		$("#poll_results_statistics_nav").addClass("active");
		$("#poll_results_options").text("");
	});

		

	NRS.forms.createPoll = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

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

		$("#create_poll_") //  idk why this is here, made by wesley.. -Jones

		data["name"] = $("#create_poll_name").val();
		data["description"] = $("#create_poll_description").val();
		data["finishHeight"] = String(parseInt(NRS.lastBlockHeight) + parseInt($("#create_poll_duration").val()));
		data["minNumberOfOptions"] = $("#create_poll_min_options").val();
		data["maxNumberOfOptions"] = $("#create_poll_max_options").val();
		data["minRangeValue"] = $("#create_poll_min_range_value").val();
		data["maxRangeValue"] = $("#create_poll_max_range_value").val();
		data["minBalance"] = String(parseInt($("#create_poll_min_balance").val())*100000000);
		data["feeNQT"] = String(parseInt($("#create_poll_fee").val()) * 100000000);
		data["deadline"] = $("#create_poll_deadline").val();
		data["secretPhrase"] = $("#create_poll_password").val();

		if($("#create_poll_type").val() == "0")
		{
			data["votingModel"] = 1;
			data["minBalanceModel"] = 1;
		}
		if($("#create_poll_type").val() == "1")
		{
			data["votingModel"] = 0;
			var val = parseInt($('input:radio[name=minBalanceType]:checked').val());
			data["minBalanceModel"] = val;

			if(val == 2) data["holding"] = $("#create_poll_asset_id").val();
			else if(val == 3) data["holding"] = $("#create_poll_ms_id").val();
		}
		if($("#create_poll_type").val() == "2")
		{
			data["votingModel"] = 2;
			data["holding"] = $("#create_poll_asset_id").val();
			data["minBalanceModel"] = 2;
		}
		else if($("#create_poll_type").val() == "3")
		{
			data["votingModel"] = 3;
			data["holding"] = $("#create_poll_ms_id").val();
			data["minBalanceModel"] = 3;
		}

		for (var i = 0; i < options.length; i++) {
			var number;
			if(i < 10) number = "0" + i;
			else number = i;
			data["option" + (number)] = options[i];
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

			var rowToAdd = "<tr class='tentative'><td>" + String(data.name).escapeHTML() + " - <strong>" + $.t("pending") + "</strong></td><td>" + String(data.description).escapeHTML() + "</td><td><a href='#' data-user='" + NRS.getAccountFormatted(NRS.accountRS) + "' class='show_account_modal_action user_info'>" + NRS.getAccountTitle(NRS.accountRS) + "</a></td><td>" + NRS.formatTimestamp(now) + "</td><td>/</td></tr>";

			$table.prepend(rowToAdd);

			if ($("#polls_table").parent().hasClass("data-empty")) {
				$("#polls_table").parent().removeClass("data-empty");
			}
		}
	}

	NRS.forms.castVote = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		var options = Array();

		$("#cast_vote_answers_entry div.answer_slider input").each(function() {
			var option = $.trim($(this).val());
			if(option == 0) option = -128;
			if (option) {
				options.push(option);
			}
		});

		$("#cast_vote_answers_entry div.answer_boxes input").each(function() {
			var option = $(this).is(':checked') ? 1 : -128;
			options.push(option);
		});

		
		data["poll"] = $("#cast_vote_poll").val();
		data["feeNQT"] = String(parseInt($("#cast_vote_fee").val())*100000000);
		data["deadline"] = $("#cast_vote_deadline").val();
		data["secretPhrase"] =  $("#cast_vote_password").val();
		
		for (var i = 0; i < options.length; i++) {
			data["vote" + (i < 10 ? "0" + i : i)] = options[i];
		}
		return {
			"requestType": "castVote",
			"data": data
		};
	}

	NRS.forms.castVoteComplete = function(response, data) {
		// don't think anything needs to go here
	}




	// a lot of stuff in followed polls, lets put that here
	NRS.followedPolls = [];
	NRS.followedPollIds = [];
	NRS.viewingAsset = false; //viewing non-bookmarked asset
	NRS.currentPoll = {};
	var currentPollId = 0;

	NRS.pages.followed_polls = function(callback) {
		$(".content.content-stretch:visible").width($(".page:visible").width());

		if (NRS.databaseSupport) {
			NRS.followedPolls = [];
			NRS.followedPollIds = [];

			NRS.database.select("polls", null, function(error, polls) {
				//select already bookmarked assets
				$.each(polls, function(index, poll) {
					NRS.cachePoll(poll);
				});
				
					NRS.loadFollowedPollsSidebar(callback);
			});
		} 
	}

	NRS.cachePoll = function(poll) {
		if (NRS.followedPollIds.indexOf(poll.poll) != -1) {
			return;
		}

		NRS.followedPollIds.push(poll.poll);

			poll.groupName = "";

		var poll = {
			"poll": String(poll.poll),
			"name": String(poll.name).toLowerCase(),
			"description": String(poll.description),
			"account": String(poll.account),
			"accountRS": String(poll.accountRS),
			"finishHeight": String(poll.finishHeight)
		};

		NRS.followedPolls.push(poll);
	}

	NRS.forms.addFollowedPoll = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		data.id = $.trim(data.id);

		if (!data.id) {
			return {
				"error": $.t("error_poll_id_required")
			};
		}

		if (!/^\d+$/.test(data.id) && !/^NXT\-/i.test(data.id)) {
			return {
				"error": $.t("error_poll_id_invalid")
			};
		}
		else {
			NRS.sendRequest("getPoll", {
				"poll": data.id
			}, function(response) {
				if (response.errorCode) {
					NRS.showModalError($.t("no_poll_found"), $modal);
				} else {
					NRS.saveFollowedPolls(new Array(response), NRS.forms.addFollowedPollsComplete);
				}
			});
		}
	}

	/*$("#asset_exchange_bookmark_this_asset").on("click", function() {
		if (NRS.viewingAsset) {
			NRS.saveAssetBookmarks(new Array(NRS.viewingAsset), function(newAssets) {
				NRS.viewingAsset = false;
				NRS.loadAssetExchangeSidebar(function() {
					$("#asset_exchange_sidebar a[data-asset=" + newAssets[0].asset + "]").addClass("active").trigger("click");
				});
			});
		}
	});*/

	NRS.forms.addFollowedPollsComplete = function(newPolls, submittedPolls) {
		NRS.pollSearch = false;

		if (newPolls.length == 0) {
			NRS.closeModal();
			$.growl($.t("error_poll_already_bookmarked", {
				"count": submittedPolls.length
			}), {
				"type": "danger"
			});
			$("#followed_polls_sidebar a.active").removeClass("active");
			$("#followed_polls_sidebar a[data-poll=" + submittedPolls[0].poll + "]").addClass("active").trigger("click");
			return;
		} else {
			NRS.closeModal();

			var message = "";
			if (newPolls.length == 1) {
				message += $.t("success_poll_followed_one");
			} else {
				message += $.t("success_poll_followed", { "count": newPolls.length });
			}

			if (!NRS.databaseSupport) {
				message += " " + $.t("error_poll_save_db");
			}

			$.growl(message, {
				"type": "success"
			});

			NRS.loadFollowedPollsSidebar(function(callback) {
				$("#followed_polls_sidebar a.active").removeClass("active");
				$("#followed_polls_sidebar a[data-asset=" + newPolls[0].poll + "]").addClass("active").trigger("click");
			});
		}
	}

	NRS.saveFollowedPolls = function(polls, callback) {
		var newPollIds = [];
		var newPolls = [];

		$.each(polls, function(key, poll) {
			var newPoll = {
				"poll": String(poll.poll),
				"name": String(poll.name),
				"account": String(poll.account),
				"accountRS": String(poll.accountRS),
				"description": String(poll.description),
				"finishHeight": String(poll.finishHeight)
			};

			newPolls.push(newPoll);

			if (NRS.databaseSupport) {
				newPollIds.push({
					"poll": String(poll.poll)
				});
			}
		});

		if (!NRS.databaseSupport) {
			if (callback) {
				callback(newPolls, polls);
			}
			return;
		}

		NRS.database.select("polls", newPollIds, function(error, existingPolls) {
			var existingIds = [];

			if (existingPolls.length) {
				$.each(existingPolls, function(index, poll) {
					existingIds.push(poll.poll);
				});

				newPoll = $.grep(newPolls, function(v) {
					return (existingIds.indexOf(v.poll) === -1);
				});
			}

			if (newPolls.length == 0) {
				if (callback) {
					callback([], polls);
				}
			} else {
				NRS.database.insert("polls", newPolls, function(error) {
					$.each(newPolls, function(key, poll) {
						poll.name = poll.name.toLowerCase();
						NRS.followedPollIds.push(poll.poll);
						NRS.followedPolls.push(poll);
					});

					if (callback) {
						//for some reason we need to wait a little or DB won't be able to fetch inserted record yet..
						setTimeout(function() {
							callback(newPolls, polls);
						}, 50);
					}
				});
			}
		});
	}

	NRS.positionFollowedPollsSidebar = function() {
		$("#followed_polls_sidebar").parent().css("position", "relative");
		$("#followed_polls_sidebar").parent().css("padding-bottom", "5px");
		//$("#asset_exchange_sidebar_content").height($(window).height() - 120);
		$("#followed_polls_sidebar").height($(window).height() - 120);
	}

	//called on opening the asset exchange page and automatic refresh
	NRS.loadFollowedPollsSidebar = function(callback) {
		if (!NRS.followedPolls.length) {
			NRS.pageLoaded(callback);
			$("#followed_polls_sidebar_content").empty();
			$("#no_poll_selected, #loading_poll_data, #no_poll_search_results, #poll_details").hide();
			$("#no_polls_available").show();
			$("#followed_polls_page").addClass("no_polls");
			return;
		}

		var rows = "";

		$("#followed_polls_page").removeClass("no_polls");

		NRS.positionFollowedPollsSidebar();

		NRS.followedPolls.sort(function(a, b) {
				if (a.name > b.name) {
					return 1;
				} else if (a.name < b.name) {
					return -1;
				} else {
					return 0;
				}
		});

		var lastGroup = "";
		var ungrouped = true;
		var isClosedGroup = false;

		var isSearch = false;
		var searchResults = 0;

		for (var i = 0; i < NRS.followedPolls.length; i++) {
			var poll = NRS.followedPolls[i];

			rows += "<a href='#' class='list-group-item list-group-item-" + "ungrouped" + " not_owns_asset" + "' ";
			rows += "data-cache='" + i + "' ";
			rows += "data-poll='" + String(poll.poll).escapeHTML() + "'";
			rows += (isClosedGroup ? " style='display:none'" : "") + " data-closed='" + isClosedGroup + "'>";
			rows += "<h4 class='list-group-item-heading'>" + poll.name.escapeHTML() + "</h4>";

			if(NRS.lastBlockHeight > parseInt(poll.finishHeight))
			{
				rows += "<p class='list-group-item-text'><span data-i18n=\"completed\">Completed</span></p>";
			}
			else
			{
				rows += "<p class='list-group-item-text'><span data-i18n=\"blocks_left\">Blocks Left</span>: " + (parseInt(poll.finishHeight)-NRS.lastBlockHeight) + "</p>";
			}
			rows += "</a>";
		}

		var active = $("#followed_polls_sidebar a.active");


		if (active.length) {
			active = active.data("poll");
		} else {
			active = false;
		}

		$("#followed_polls_sidebar_content").empty().append(rows);

		if (active) {
			$("#followed_polls_sidebar a[data-poll=" + active + "]").addClass("active");
		}


		$("#followed_polls_sidebar_search").hide();

		if (NRS.viewingAsset) {
			$("#asset_exchange_bookmark_this_asset").show();
		} else {
			$("#asset_exchange_bookmark_this_asset").hide();
		}

		NRS.pageLoaded(callback);
	}

	NRS.incoming.followed_polls = function() {
		if (!NRS.viewingPoll) {
			//refresh active asset
			var $active = $("#followed_polls_sidebar a.active");

			if ($active.length) {
				$active.trigger("click", [{
					"refresh": true
				}]);
			}
		} else {
			NRS.loadPoll(NRS.viewingPoll, true);
		}

		//update assets owned (colored)
	}

	$("#followed_polls_sidebar").on("click", "a", function(e, data) {
		e.preventDefault();

		currentPollID = String($(this).data("poll")).escapeHTML();

		//refresh is true if data is refreshed automatically by the system (when a new block arrives)
		if (data && data.refresh) {
			var refresh = true;
		} else {
			var refresh = false;
		}

		//clicked on a group
		if (!currentPollID) {
			if (NRS.databaseSupport) {
				var group = $(this).data("groupname");
				var closed = $(this).data("closed");

					var $links = $("#followed_polls_sidebar a.list-group-item-ungrouped");

				if (!group) {
					group = "undefined";
				}

				if (closed) {
					var pos = NRS.closedGroups.indexOf(group);
					if (pos >= 0) {
						NRS.closedGroups.splice(pos);
					}
					$(this).data("closed", "");
					$(this).find("i").removeClass("fa-angle-right").addClass("fa-angle-down");
					$links.show();
				} else {
					NRS.closedGroups.push(group);
					$(this).data("closed", true);
					$(this).find("i").removeClass("fa-angle-down").addClass("fa-angle-right");
					$links.hide();
				}

				NRS.database.update("data", {
					"contents": NRS.closedGroups.join("#")
				}, [{
					"id": "closed_groups"
				}]);
			}

			return;
		}

		if (NRS.databaseSupport) {
			NRS.database.select("polls", [{
				"poll": currentPollID
			}], function(error, poll) {
				if (poll && poll.length && poll[0].poll == currentPollID) {
					NRS.loadPoll(poll[0], refresh);
				}
			});
		} else {
			NRS.sendRequest("getPoll+", {
				"poll": currentPollID
			}, function(response, input) {
				if (!response.errorCode && response.poll == currentPollID) {
					NRS.loadPoll(response, refresh);
				}
			});
		}
	});

	NRS.loadPoll = function(poll, refresh) {
		var pollId = poll.poll;
		NRS.currentPoll = poll;
		NRS.currentSubPage = pollId;

		if (!refresh) {
			$("#followed_polls_sidebar a.active").removeClass("active");
			$("#followed_polls_sidebar a[data-poll=" + pollId + "]").addClass("active");

			$("#no_poll_selected, #loading_poll_data, #no_polls_available, #no_poll_search_results").hide();
			$("#poll_details").show().parent().animate({
				"scrollTop": 0
			}, 0);

			$("#poll_account").html("<a href='#' data-user='" + NRS.getAccountFormatted(poll, "account") + "' class='user_info'>" + NRS.getAccountTitle(poll, "account") + "</a>");
			$("#poll_id").html(poll.poll.escapeHTML());

			$("#followed_polls_poll_name").html(String(poll.name).escapeHTML());
			$("#poll_description").html(String(poll.description).autoLink());
			$(".poll_name").html(String(poll.name).escapeHTML());
			if(poll.finishHeight > NRS.lastBlockHeight)
			{
				$("#vote_poll_link").removeClass('disabled');
				$("#vote_poll_link").attr("data-poll", pollId);
			}
			else $("#vote_poll_link").addClass('disabled');


			$("#followed_polls_poll_results tbody").empty();
			$("#followed_polls_votes_cast tbody").empty();
			$("#followed_polls_poll_results").parent().addClass("data-loading").removeClass("data-empty");
			$("#followed_polls_votes_cast").parent().addClass("data-loading").removeClass("data-empty");

			$(".data-loading img.loading").hide();

			setTimeout(function() {
				$(".data-loading img.loading").fadeIn(200);
			}, 200);

			if (NRS.databaseSupport) {
				NRS.sendRequest("getPoll", {
					"poll": pollId
				}, function(response) {
					if (!response.errorCode) {
						if (response.poll != poll.poll || response.account != poll.account || response.accountRS != poll.accountRS || response.description != poll.description || response.name != poll.name) {
							NRS.database.delete("polls", [{
								"poll": poll.poll
							}], function() {
								setTimeout(function() {
									NRS.loadPage("followed_polls");
									$.growl("Invalid poll.", {
										"type": "danger"
									});
								}, 50);
							});
						}
					}
				});
			}

			if (poll.viewingPoll) {
				$("#followed_polls_bookmark_this_poll").show();
				NRS.viewingPoll = poll;
			} else {
				$("#followed_polls_bookmark_this_poll").hide();
				NRS.viewingPoll = false;
			}
		}


		// Only asset issuers have the ability to pay dividends.
		if (poll.finishHeight < NRS.lastBlockHeight) {
         $("#vote_poll_link").show();
		} else {
			$("#vote_poll_link").hide();
		}

		NRS.loadPollResults(pollId, refresh);
		NRS.loadPollVotes(pollId, refresh);
	}

	NRS.loadPollResults = function(pollId, refresh) {
		NRS.sendRequest("getPoll+" + pollId, {
			"poll": pollId
		}, function(polldata, input) {

			NRS.sendRequest("getPollResult+" + pollId, {
				"poll": pollId
			}, function(response, input) {
                var rows = layoutPollResults(response, polldata);
                $("#followed_polls_poll_results tbody").empty().append(rows);
				NRS.dataLoadFinished($("#followed_polls_poll_results"), !refresh);
			});
		});
	}

	NRS.loadPollVotes = function(pollId, refresh)
	{
		NRS.sendRequest("getPoll+" + pollId, {
			"poll": pollId
		}, function(polldata, input) {

			NRS.sendRequest("getPollVotes+" + pollId, {
				"poll": pollId
			}, function(votesdata, input) {

				var votes = votesdata.votes;

				if (!votes) {
					votes = [];
				}

				if (votes.length) {
					
					var head = "";
					head += "<tr>";
					head += "<th data-i18n=\"voter\">Voter</th>";

					for(var b=0; b<polldata.options.length; b++)
					{
						head += "<th>"+String(polldata.options[b].escapeHTML()) + "</th>";
					}
					head += "</tr>";

					$("#followed_polls_votes_cast thead").empty().append(head);				

					$("#votes_cast_count").html("(" + votes.length + ")");

					var rows = "";

					for (var i = 0; i < votes.length; i++) {
						rows += "<tr>";
						var vote = votes[i];

						rows += "<td><a href='#' class='user_info' data-user='" + NRS.getAccountFormatted(vote, "voter") + "'>" + NRS.getAccountTitle(vote, "voter") + "</td>";

						for(var a=0;a<vote.votes.length;a++)
						{
							rows += "<td>" + vote.votes[a] + "</td>";
						}
					}

					$("#followed_polls_votes_cast tbody").empty().append(rows);
				} else {
					$("#followed_polls_votes_cast tbody").empty();
					$("#votes_cast_count").html("(0)");

				}

				NRS.dataLoadFinished($("#followed_polls_votes_cast"), !refresh);
			});
		})
	}



	return NRS;
}(NRS || {}, jQuery));

