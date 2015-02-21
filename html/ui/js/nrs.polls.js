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

						if (!poll.errorCode && poll.attachment.finishHeight >= NRS.lastBlockHeight) {
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

										rows += "<tr class='tentative'><td>" + String(unconfirmedTransaction.attachment.name).escapeHTML() + "</td><td>" + pollDescription.escapeHTML() + "</td><td>" + (unconfirmedTransaction.sender != NRS.genesis ? "<a href='#' data-user='" + NRS.getAccountFormatted(unconfirmedTransaction, "sender") + "' class='user_info'>" + NRS.getAccountTitle(unconfirmedTransaction, "sender") + "</a>" : "Genesis") + "</td><td>" + NRS.formatTimestamp(unconfirmedTransaction.timestamp) + "</td><td>" + String(unconfirmedTransaction.attachment.finishHeight - NRS.lastBlockHeight)  + "</td><td><a href='#'>Vote (todo)</td></tr>";
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
								rows += "<tr><td><a class='poll_list_title' href='#' data-transaction='"+poll.transaction+"'>" + String(poll.attachment.name).escapeHTML() + "</a></td><td>" + pollDescription.escapeHTML() + "</td><td>" + (poll.sender != NRS.genesis ? "<a href='#' data-user='" + NRS.getAccountFormatted(poll, "sender") + "' class='user_info'>" + NRS.getAccountTitle(poll, "sender") + "</a>" : "Genesis") + "</td><td>" + NRS.formatTimestamp(poll.timestamp) + "</td><td>" + String(poll.attachment.finishHeight - NRS.lastBlockHeight) + "</td><td><a href='#' class='vote_button' data-poll='" + poll.transaction +"'>Vote </td></tr>";
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
		NRS.sendRequest("getPollIds+", {"account": NRS.account}, function(response) {
			if (response.pollIds && response.pollIds.length) {
				var polls = {};
				var nrPolls = 0;

				for (var i = 0; i < response.pollIds.length; i++) {
					NRS.sendRequest("getTransaction+", {
						"transaction": response.pollIds[i]
					}, function(poll, input) {
						if (NRS.currentPage != "my_polls") {
							polls = {};
							return;
						}

						if (!poll.errorCode) {
							polls[input.transaction] = poll;
						}

						nrPolls++;


						if (nrPolls == response.pollIds.length) {
							var rows = "";

							for (var i = 0; i < nrPolls; i++) {
								var poll = polls[response.pollIds[i]];

								if (!poll) {
									continue;
								}

								var pollDescription = String(poll.attachment.description);

								if (pollDescription.length > 100) {
									pollDescription = pollDescription.substring(0, 100) + "...";
								}
								rows += "<tr>"
								rows += "<td><a class='poll_list_title' href='#' data-transaction='"+poll.transaction+"'>" + String(poll.attachment.name).escapeHTML() + "</a></td>";
								rows += "<td>" + pollDescription.escapeHTML() + "</td>";
								rows += "<td>" + (poll.sender != NRS.genesis ? "<a href='#' data-user='" + NRS.getAccountFormatted(poll, "sender") + "' class='user_info'>" + NRS.getAccountTitle(poll, "sender") + "</a>" : "Genesis") + "</td>"
								rows += "<td>" + NRS.formatTimestamp(poll.timestamp) + "</td>";
								if(poll.attachment.finishHeight > NRS.lastBlockHeight)
								{
									rows += "<td>" + String(poll.attachment.finishHeight - NRS.lastBlockHeight) + "</td>";
									rows += "<td><a href='#' class='vote_button' data-poll='" + poll.transaction +"'>Vote </td>";

								}
								else
								{
									rows += "<td>Complete</td>";
									rows += "<td><a href='#' class='results_button' data-results='" + poll.transaction +"'>Results </td>";
								}
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
								rows += "<td><a class='poll_list_title' href='#' data-transaction='"+poll.transaction+"'>" + String(poll.attachment.name).escapeHTML() + "</a></td>";
								rows += "<td>" + pollDescription.escapeHTML() + "</td>";
								rows += "<td>" + (poll.sender != NRS.genesis ? "<a href='#' data-user='" + NRS.getAccountFormatted(poll, "sender") + "' class='user_info'>" + NRS.getAccountTitle(poll, "sender") + "</a>" : "Genesis") + "</td>"
								rows += "<td>" + NRS.formatTimestamp(poll.timestamp) + "</td>";
								if(poll.attachment.finishHeight > NRS.lastBlockHeight)
								{
									rows += "<td>" + String(poll.attachment.finishHeight - NRS.lastBlockHeight) + "</td>";
									rows += "<td>Waiting... </td>";

								}
								else
								{
									rows += "<td>Complete</td>";
									rows += "<td><a href='#' class='results_button' data-results='" + poll.transaction +"'>Results </td>";
								}
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
						$("#cast_vote_answers_entry").append("<div class='answer_slider'><label name='cast_vote_answer_"+b+"'>"+response.attachment.options[b]+"</label> &nbsp;&nbsp;<span class='badge'>"+response.attachment.minRangeValue+"</span><br/><input class='form-control' step='1' value='"+response.attachment.minRangeValue+"' max='"+response.attachment.maxRangeValue+"' min='"+response.attachment.minRangeValue+"' type='range'/></div>");
					}
				}
				else
				{
					for(var b=0; b<response.attachment.options.length; b++)
					{
						$("#cast_vote_answers_entry").append("<div class='answer_boxes'><label name='cast_vote_answer_"+b+"'><input type='checkbox'/>&nbsp;&nbsp;"+response.attachment.options[b]+"</label></div>");
					}
				}
				$("#cast_vote_modal").modal();
				$("input[type='range']").on("change mousemove", function() {
					$(this).parent().children(".badge").text($(this).val());

				});
			});

			
		});	

		$("#my_polls_table, #voted_polls_table").on("click", "a[data-results]", function(e) {
			e.preventDefault();
			var transactionId = $(this).data("results");

			NRS.sendRequest("getPollResult", {"poll": transactionId, "req":"getPollResult"}, voteModal);
			NRS.sendRequest("getPollVotes", {"poll": transactionId, "req":"getPollVotes"}, voteModal);
			NRS.sendRequest("getPoll", {"poll": transactionId, "req": "getPoll"}, voteModal);
			var results, votes, poll;

			function voteModal(data, input)
			{
				if(input.req=="getPollResult") results = data;
				if(input.req=="getPollVotes") votes = data;
				if(input.req=="getPoll") poll = data;

				if(results !== undefined && votes !== undefined && poll !== undefined)
				{
					$("#poll_results_options").append("<tr><td style='font-weight: bold;width:180px;'><span data-i18n='poll_name'>Poll Name</span>:</td><td><span id='poll_results_poll_name'>"+poll.name+"</span></td></tr>");
					$("#poll_results_options").append("<tr><td style='font-weight: bold;width:180px;'><span data-i18n='poll_id'>Poll Id</span>:</td><td><span id='poll_results_poll_id'>"+poll.poll+"</span></td></tr>");

					$("#poll_results_poll_name").text(poll.name);
					$("#poll_results_poll_id").text(poll.poll);


					$("#poll_results_number_of_voters").text(votes.votes.length);


					$("#poll_results_modal").modal();

					for(var b=0; b<results.results.length; b++)
					{
						$("#poll_results_options").append("<tr><td style='font-weight: bold;width:180px;'><span>"+Object.keys(results.results[b])[0]+"</span>:</td><td><span id='poll_results_result_"+b+"'>"+results.results[b][Object.keys(results.results[b])[0]]+"</span></td></tr>");
					}
					if(votes.votes.length == 0)
					{
						$("poll_results_voters").text("<span data-il8n='voter_data_pruned'>Voter data has been pruned from the blockchain</span>");
					}
					else {
						for(var c=0; c<votes.votes.length;c++)
						{
							
						}
					}


					/*if(response.attachment.minNumberOfOptions != response.attachment.maxNumberOfOptions)
					$("#cast_vote_range").text("Select between " + response.attachment.minNumberOfOptions + " and " + response.attachment.maxNumberOfOptions + " options from below.")
					else if(response.attachment.minNumberOfOptions != 1) $("#cast_vote_range").text("Select " + response.attachment.minNumberOfOptions +  " options from below.")
					else $("#cast_vote_range").text("Select 1 option from below.")

					$("#cast_vote_poll").val(response.transaction);
					if(response.attachment.maxRangeValue != 1)
					{
						for(var b=0; b<response.attachment.options.length; b++)
						{
							$("#cast_vote_answers_entry").append("<div class='answer_slider'><label name='cast_vote_answer_"+b+"'>"+response.attachment.options[b]+"</label> &nbsp;&nbsp;<span class='badge'>"+response.attachment.minRangeValue+"</span><br/><input class='form-control' step='1' value='"+response.attachment.minRangeValue+"' max='"+response.attachment.maxRangeValue+"' min='"+response.attachment.minRangeValue+"' type='range'/></div>");
						}
					}
					else
					{
						for(var b=0; b<response.attachment.options.length; b++)
						{
							$("#cast_vote_answers_entry").append("<div class='answer_boxes'><label name='cast_vote_answer_"+b+"'><input type='checkbox'/>&nbsp;&nbsp;"+response.attachment.options[b]+"</label></div>");
						}
					}
					
					$("input[type='range']").on("change mousemove", function() {
						$(this).parent().children(".badge").text($(this).val());

					});*/
				}


			}

			
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

		var data = {
			"name": $("#create_poll_name").val(),
			"description": $("#create_poll_description").val(),
			"finishHeight": (parseInt(NRS.lastBlockHeight) + parseInt($("#create_poll_duration").val())),
			"minNumberOfOptions": $("#create_poll_min_options").val(),
			"maxNumberOfOptions": $("#create_poll_max_options").val(),
			"minRangeValue": $("#create_poll_min_range_value").val(),
			"maxRangeValue": $("#create_poll_max_range_value").val(),
			"minBalance": (parseInt($("#create_poll_min_balance").val())*100000000),
			"feeNQT": (parseInt($("#create_poll_fee").val()) * 100000000),
			"deadline": $("#create_poll_deadline").val(),
			"secretPhrase": $("#create_poll_password").val()
		};

		if($("#create_poll_type").val() == "0")
		{
			data["votingModel"] = 1;
			data["minBalanceModel"] = 0;
		}
		if($("#create_poll_type").val() == "1")
		{
			data["votingModel"] = 1;
			var val = $('input:radio[name=minBalanceType]:checked').val();
			data["minBalanceModel"] = val;

			if(val == 2) data["holding"] = $("#create_poll_asset_id").val();
			else if(val == 3) data["holding"] = $("#create_poll_ms_currency").val();
		}
		if($("#create_poll_type").val() == "2")
		{
			data["votingModel"] = 2;
			data["holding"] = $("#create_poll_asset_id").val();
			data["minBalanceModel"] = 1;
		}
		else if($("#create_poll_type").val() == "3")
		{
			data["votingModel"] = 3;
			data["holding"] = $("#create_poll_ms_currency").val();
			data["minBalanceModel"] = 2;
		}

		for (var i = 0; i < options.length; i++) {
			data["option" + (i+1)] = options[i];
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
		var options = new Array();

		$("#cast_vote_answers_entry div.answer_slider input").each(function() {
			var option = $.trim($(this).val());
			if(option == 0) option = -127;
			if (option) {
				options.push(option);
			}
		});

		$("#cast_vote_answers_entry div.answer_boxes input").each(function() {
			var option = $(this).is(':checked') ? 1 : -128;
			options.push(option);
		});

		var data = {
			"poll": $("#cast_vote_poll").val(),
			"feeNQT": (parseInt($("#cast_vote_fee").val())*100000000),
			"deadline": $("#cast_vote_deadline").val(),
			"secretPhrase": $("#cast_vote_password").val()
		};
		for (var i = 0; i < options.length; i++) {
			data["vote" + (i+1)] = options[i];
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
		};

		NRS.polls.push(poll);
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

			var message = $.t("success_poll_followed", {
				"count": newPolls.length
			});

			if (!NRS.databaseSupport) {
				message += " " + $.t("error_polls_save_db");
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
				"poll": String(poll.asset),
				"name": String(poll.name),
				"description": String(poll.description),
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
			$("#asset_exchange_sidebar_content").empty();
			$("#no_asset_selected, #loading_asset_data, #no_asset_search_results, #asset_details").hide();
			$("#no_assets_available").show();
			$("#asset_exchange_page").addClass("no_assets");
			return;
		}

		var rows = "";

		$("#asset_exchange_page").removeClass("no_assets");

		NRS.positionAssetSidebar();

		NRS.assets.sort(function(a, b) {
			if (!a.groupName && !b.groupName) {
				if (a.name > b.name) {
					return 1;
				} else if (a.name < b.name) {
					return -1;
				} else {
					return 0;
				}
			} else if (!a.groupName) {
				return 1;
			} else if (!b.groupName) {
				return -1;
			} else if (a.groupName > b.groupName) {
				return 1;
			} else if (a.groupName < b.groupName) {
				return -1;
			} else {
				if (a.name > b.name) {
					return 1;
				} else if (a.name < b.name) {
					return -1;
				} else {
					return 0;
				}
			}
		});

		var lastGroup = "";
		var ungrouped = true;
		var isClosedGroup = false;

		var isSearch = NRS.assetSearch !== false;
		var searchResults = 0;

		for (var i = 0; i < NRS.assets.length; i++) {
			var asset = NRS.assets[i];

			if (isSearch) {
				if (NRS.assetSearch.indexOf(asset.asset) == -1) {
					continue;
				} else {
					searchResults++;
				}
			}

			if (asset.groupName.toLowerCase() != lastGroup) {
				var to_check = (asset.groupName ? asset.groupName : "undefined");

				if (NRS.closedGroups.indexOf(to_check) != -1) {
					isClosedGroup = true;
				} else {
					isClosedGroup = false;
				}

				if (asset.groupName) {
					ungrouped = false;
					rows += "<a href='#' class='list-group-item list-group-item-header" + (asset.groupName == "Ignore List" ? " no-context" : "") + "'" + (asset.groupName != "Ignore List" ? " data-context='asset_exchange_sidebar_group_context' " : "data-context=''") + " data-groupname='" + asset.groupName.escapeHTML() + "' data-closed='" + isClosedGroup + "'><h4 class='list-group-item-heading'>" + asset.groupName.toUpperCase().escapeHTML() + "</h4><i class='fa fa-angle-" + (isClosedGroup ? "right" : "down") + " group_icon'></i></h4></a>";
				} else {
					ungrouped = true;
					rows += "<a href='#' class='list-group-item list-group-item-header no-context' data-closed='" + isClosedGroup + "'><h4 class='list-group-item-heading'>UNGROUPED <i class='fa pull-right fa-angle-" + (isClosedGroup ? "right" : "down") + "'></i></h4></a>";
				}

				lastGroup = asset.groupName.toLowerCase();
			}

			var ownsAsset = false;
			var ownsQuantityQNT = 0;

			if (NRS.accountInfo.assetBalances) {
				$.each(NRS.accountInfo.assetBalances, function(key, assetBalance) {
					if (assetBalance.asset == asset.asset && assetBalance.balanceQNT != "0") {
						ownsAsset = true;
						ownsQuantityQNT = assetBalance.balanceQNT;
						return false;
					}
				});
			}

			rows += "<a href='#' class='list-group-item list-group-item-" + (ungrouped ? "ungrouped" : "grouped") + (ownsAsset ? " owns_asset" : " not_owns_asset") + "' ";
			rows += "data-cache='" + i + "' ";
			rows += "data-asset='" + String(asset.asset).escapeHTML() + "'" + (!ungrouped ? " data-groupname='" + asset.groupName.escapeHTML() + "'" : "");
			rows += (isClosedGroup ? " style='display:none'" : "") + " data-closed='" + isClosedGroup + "'>";
			rows += "<h4 class='list-group-item-heading'>" + asset.name.escapeHTML() + "</h4>";
			rows += "<p class='list-group-item-text'><span data-i18n=\"quantity\">Quantity</span>: " + NRS.formatQuantity(ownsQuantityQNT, asset.decimals) + "</p>";
			rows += "</a>";
		}

		var active = $("#asset_exchange_sidebar a.active");


		if (active.length) {
			active = active.data("asset");
		} else {
			active = false;
		}

		$("#asset_exchange_sidebar_content").empty().append(rows);
		$("#asset_exchange_sidebar_search").show();

		if (isSearch) {
			if (active && NRS.assetSearch.indexOf(active) != -1) {
				//check if currently selected asset is in search results, if so keep it at that
				$("#asset_exchange_sidebar a[data-asset=" + active + "]").addClass("active");
			} else if (NRS.assetSearch.length == 1) {
				//if there is only 1 search result, click it
				$("#asset_exchange_sidebar a[data-asset=" + NRS.assetSearch[0] + "]").addClass("active").trigger("click");
			}
		} else if (active) {
			$("#asset_exchange_sidebar a[data-asset=" + active + "]").addClass("active");
		}

		if (isSearch || NRS.assets.length >= 10) {
			$("#asset_exchange_sidebar_search").show();
		} else {
			$("#asset_exchange_sidebar_search").hide();
		}

		if (isSearch && NRS.assetSearch.length == 0) {
			$("#no_asset_search_results").show();
			$("#asset_details, #no_asset_selected, #no_assets_available").hide();
		} else if (!$("#asset_exchange_sidebar a.active").length) {
			$("#no_asset_selected").show();
			$("#asset_details, #no_assets_available, #no_asset_search_results").hide();
		} else if (active) {
			$("#no_assets_available, #no_asset_selected, #no_asset_search_results").hide();
		}

		if (NRS.viewingAsset) {
			$("#asset_exchange_bookmark_this_asset").show();
		} else {
			$("#asset_exchange_bookmark_this_asset").hide();
		}

		NRS.pageLoaded(callback);
	}

	NRS.incoming.asset_exchange = function() {
		if (!NRS.viewingAsset) {
			//refresh active asset
			var $active = $("#asset_exchange_sidebar a.active");

			if ($active.length) {
				$active.trigger("click", [{
					"refresh": true
				}]);
			}
		} else {
			NRS.loadAsset(NRS.viewingAsset, true);
		}

		//update assets owned (colored)
		$("#asset_exchange_sidebar a.list-group-item.owns_asset").removeClass("owns_asset").addClass("not_owns_asset");

		if (NRS.accountInfo.assetBalances) {
			$.each(NRS.accountInfo.assetBalances, function(key, assetBalance) {
				if (assetBalance.balanceQNT != "0") {
					$("#asset_exchange_sidebar a.list-group-item[data-asset=" + assetBalance.asset + "]").addClass("owns_asset").removeClass("not_owns_asset");
				}
			});
		}
	}

	$("#asset_exchange_sidebar").on("click", "a", function(e, data) {
		e.preventDefault();

		currentAssetID = String($(this).data("asset")).escapeHTML();

		//refresh is true if data is refreshed automatically by the system (when a new block arrives)
		if (data && data.refresh) {
			var refresh = true;
		} else {
			var refresh = false;
		}

		//clicked on a group
		if (!currentAssetID) {
			if (NRS.databaseSupport) {
				var group = $(this).data("groupname");
				var closed = $(this).data("closed");

				if (!group) {
					var $links = $("#asset_exchange_sidebar a.list-group-item-ungrouped");
				} else {
					var $links = $("#asset_exchange_sidebar a.list-group-item-grouped[data-groupname='" + group.escapeHTML() + "']");
				}

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
			NRS.database.select("assets", [{
				"asset": currentAssetID
			}], function(error, asset) {
				if (asset && asset.length && asset[0].asset == currentAssetID) {
					NRS.loadAsset(asset[0], refresh);
				}
			});
		} else {
			NRS.sendRequest("getAsset+", {
				"asset": currentAssetID
			}, function(response, input) {
				if (!response.errorCode && response.asset == currentAssetID) {
					NRS.loadAsset(response, refresh);
				}
			});
		}
	});

	NRS.loadAsset = function(asset, refresh) {
		var assetId = asset.asset;

		NRS.currentAsset = asset;
		NRS.currentSubPage = assetId;

		if (!refresh) {
			$("#asset_exchange_sidebar a.active").removeClass("active");
			$("#asset_exchange_sidebar a[data-asset=" + assetId + "]").addClass("active");

			$("#no_asset_selected, #loading_asset_data, #no_assets_available, #no_asset_search_results").hide();
			$("#asset_details").show().parent().animate({
				"scrollTop": 0
			}, 0);

			$("#asset_account").html("<a href='#' data-user='" + NRS.getAccountFormatted(asset, "account") + "' class='user_info'>" + NRS.getAccountTitle(asset, "account") + "</a>");
			$("#asset_id").html(assetId.escapeHTML());
			$("#asset_decimals").html(String(asset.decimals).escapeHTML());
			$("#asset_name").html(String(asset.name).escapeHTML());
			$("#asset_description").html(String(asset.description).autoLink());
			$("#asset_quantity").html(NRS.formatQuantity(asset.quantityQNT, asset.decimals));
			$(".asset_name").html(String(asset.name).escapeHTML());
			$("#sell_asset_button").data("asset", assetId);
			$("#buy_asset_button").data("asset", assetId);
			$("#view_asset_distribution_link").data("asset", assetId);
			$("#sell_asset_for_nxt").html($.t("sell_asset_for_nxt", {
				"assetName": String(asset.name).escapeHTML()
			}));
			$("#buy_asset_with_nxt").html($.t("buy_asset_with_nxt", {
				"assetName": String(asset.name).escapeHTML()
			}));
			$("#sell_asset_price, #buy_asset_price").val("");
			$("#sell_asset_quantity, #sell_asset_total, #buy_asset_quantity, #buy_asset_total").val("0");

			$("#asset_exchange_ask_orders_table tbody").empty();
			$("#asset_exchange_bid_orders_table tbody").empty();
			$("#asset_exchange_trade_history_table tbody").empty();
			$("#asset_exchange_ask_orders_table").parent().addClass("data-loading").removeClass("data-empty");
			$("#asset_exchange_bid_orders_table").parent().addClass("data-loading").removeClass("data-empty");
			$("#asset_exchange_trade_history_table").parent().addClass("data-loading").removeClass("data-empty");

			$(".data-loading img.loading").hide();

			setTimeout(function() {
				$(".data-loading img.loading").fadeIn(200);
			}, 200);

			var nrDuplicates = 0;

			$.each(NRS.assets, function(key, singleAsset) {
				if (String(singleAsset.name).toLowerCase() == String(asset.name).toLowerCase() && singleAsset.asset != assetId) {
					nrDuplicates++;
				}
			});

			$("#asset_exchange_duplicates_warning").html($.t("asset_exchange_duplicates_warning", {
				"count": nrDuplicates
			}));

			if (NRS.databaseSupport) {
				NRS.sendRequest("getAsset", {
					"asset": assetId
				}, function(response) {
					if (!response.errorCode) {
						if (response.asset != asset.asset || response.account != asset.account || response.accountRS != asset.accountRS || response.decimals != asset.decimals || response.description != asset.description || response.name != asset.name || response.quantityQNT != asset.quantityQNT) {
							NRS.database.delete("assets", [{
								"asset": asset.asset
							}], function() {
								setTimeout(function() {
									NRS.loadPage("asset_exchange");
									$.growl("Invalid asset.", {
										"type": "danger"
									});
								}, 50);
							});
						}
					}
				});
			}

			if (asset.viewingAsset) {
				$("#asset_exchange_bookmark_this_asset").show();
				NRS.viewingAsset = asset;
			} else {
				$("#asset_exchange_bookmark_this_asset").hide();
				NRS.viewingAsset = false;
			}
		}

		// Only asset issuers have the ability to pay dividends.
		if (asset.accountRS == NRS.accountRS) {
         $("#dividend_payment_link").show();
		} else {
			$("#dividend_payment_link").hide();
		}

		if (NRS.accountInfo.unconfirmedBalanceNQT == "0") {
			$("#your_nxt_balance").html("0");
			$("#buy_automatic_price").addClass("zero").removeClass("nonzero");
		} else {
			$("#your_nxt_balance").html(NRS.formatAmount(NRS.accountInfo.unconfirmedBalanceNQT));
			$("#buy_automatic_price").addClass("nonzero").removeClass("zero");
		}

		if (NRS.accountInfo.unconfirmedAssetBalances) {
			for (var i = 0; i < NRS.accountInfo.unconfirmedAssetBalances.length; i++) {
				var balance = NRS.accountInfo.unconfirmedAssetBalances[i];

				if (balance.asset == assetId) {
					NRS.currentAsset.yourBalanceQNT = balance.unconfirmedBalanceQNT;
					$("#your_asset_balance").html(NRS.formatQuantity(balance.unconfirmedBalanceQNT, NRS.currentAsset.decimals));
					if (balance.unconfirmedBalanceQNT == "0") {
						$("#sell_automatic_price").addClass("zero").removeClass("nonzero");
					} else {
						$("#sell_automatic_price").addClass("nonzero").removeClass("zero");
					}
					break;
				}
			}
		}

		if (!NRS.currentAsset.yourBalanceQNT) {
			NRS.currentAsset.yourBalanceQNT = "0";
			$("#your_asset_balance").html("0");
		}

		NRS.loadAssetOrders("ask", assetId, refresh);
		NRS.loadAssetOrders("bid", assetId, refresh);

		NRS.getAssetTradeHistory(assetId, refresh);
	}

	NRS.loadAssetOrders = function(type, assetId, refresh) {
		type = type.toLowerCase();

		NRS.sendRequest("get" + type.capitalize() + "Orders+" + assetId, {
			"asset": assetId,
			"firstIndex": 0,
			"lastIndex": 50
		}, function(response, input) {
			var orders = response[type + "Orders"];

			if (!orders) {
				orders = [];
			}

			if (NRS.unconfirmedTransactions.length) {
				var added = false;

				for (var i = 0; i < NRS.unconfirmedTransactions.length; i++) {
					var unconfirmedTransaction = NRS.unconfirmedTransactions[i];
					unconfirmedTransaction.order = unconfirmedTransaction.transaction;

					if (unconfirmedTransaction.type == 2 && (type == "ask" ? unconfirmedTransaction.subtype == 2 : unconfirmedTransaction.subtype == 3) && unconfirmedTransaction.asset == assetId) {
						orders.push($.extend(true, {}, unconfirmedTransaction)); //make sure it's a deep copy
						added = true;
					}
				}

				if (added) {
					orders.sort(function(a, b) {
						if (type == "ask") {
							//lowest price at the top
							return new BigInteger(a.priceNQT).compareTo(new BigInteger(b.priceNQT));
						} else {
							//highest price at the top
							return new BigInteger(b.priceNQT).compareTo(new BigInteger(a.priceNQT));
						}
					});
				}
			}

			if (orders.length) {
				$("#" + (type == "ask" ? "sell" : "buy") + "_orders_count").html("(" + orders.length + (orders.length == 50 ? "+" : "") + ")");

				var rows = "";

				for (var i = 0; i < orders.length; i++) {
					var order = orders[i];

					order.priceNQT = new BigInteger(order.priceNQT);
					order.quantityQNT = new BigInteger(order.quantityQNT);
					order.totalNQT = new BigInteger(NRS.calculateOrderTotalNQT(order.quantityQNT, order.priceNQT));

					if (i == 0 && !refresh) {
						$("#" + (type == "ask" ? "buy" : "sell") + "_asset_price").val(NRS.calculateOrderPricePerWholeQNT(order.priceNQT, NRS.currentAsset.decimals));
					}

					var className = (order.account == NRS.account ? "your-order" : "") + (order.unconfirmed ? " tentative" : (NRS.isUserCancelledOrder(order) ? " tentative tentative-crossed" : ""));

					rows += "<tr class='" + className + "' data-transaction='" + String(order.order).escapeHTML() + "' data-quantity='" + order.quantityQNT.toString().escapeHTML() + "' data-price='" + order.priceNQT.toString().escapeHTML() + "'><td>" + (order.unconfirmed ? "You - <strong>Pending</strong>" : (order.account == NRS.account ? "<strong>You</strong>" : "<a href='#' data-user='" + NRS.getAccountFormatted(order, "account") + "' class='user_info'>" + (order.account == NRS.currentAsset.account ? "Asset Issuer" : NRS.getAccountTitle(order, "account")) + "</a>")) + "</td><td>" + NRS.formatQuantity(order.quantityQNT, NRS.currentAsset.decimals) + "</td><td>" + NRS.formatOrderPricePerWholeQNT(order.priceNQT, NRS.currentAsset.decimals) + "</td><td>" + NRS.formatAmount(order.totalNQT) + "</tr>";
				}

				$("#asset_exchange_" + type + "_orders_table tbody").empty().append(rows);
			} else {
				$("#asset_exchange_" + type + "_orders_table tbody").empty();
				if (!refresh) {
					$("#" + (type == "ask" ? "buy" : "sell") + "_asset_price").val("0");
				}
				$("#" + (type == "ask" ? "sell" : "buy") + "_orders_count").html("");
			}

			NRS.dataLoadFinished($("#asset_exchange_" + type + "_orders_table"), !refresh);
		});
	}



	return NRS;
}(NRS || {}, jQuery));

