/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.pages.aliases = function() {
		NRS.sendRequest("getAliases+", {
			"account": NRS.accountRS,
			"timestamp": 0
		}, function(response) {
			if (response.aliases && response.aliases.length) {
				var aliases = response.aliases;

				if (NRS.unconfirmedTransactions.length) {
					for (var i = 0; i < NRS.unconfirmedTransactions.length; i++) {
						var unconfirmedTransaction = NRS.unconfirmedTransactions[i];

						if (unconfirmedTransaction.type == 1 && unconfirmedTransaction.subtype == 1) {
							var found = false;

							for (var j = 0; j < aliases.length; j++) {
								if (aliases[j].aliasName == unconfirmedTransaction.attachment.alias) {
									aliases[j].aliasURI = unconfirmedTransaction.attachment.uri;
									aliases[j].tentative = true;
									found = true;
									break;
								}
							}

							if (!found) {
								aliases.push({
									"aliasName": unconfirmedTransaction.attachment.alias,
									"aliasURI": unconfirmedTransaction.attachment.uri,
									"tentative": true
								});
							}
						}
					}
				}

				aliases.sort(function(a, b) {
					if (a.aliasName.toLowerCase() > b.aliasName.toLowerCase()) {
						return 1;
					} else if (a.aliasName.toLowerCase() < b.aliasName.toLowerCase()) {
						return -1;
					} else {
						return 0;
					}
				});

				var rows = "";

				var alias_account_count = 0,
					alias_uri_count = 0,
					empty_alias_count = 0,
					alias_count = aliases.length;

				for (var i = 0; i < alias_count; i++) {
					var alias = aliases[i];

					alias.status = "/";

					var unconfirmedTransaction = NRS.getUnconfirmedTransactionFromCache(1, 6, {
						"alias": alias.aliasName
					});

					if (unconfirmedTransaction) {
						alias.tentative = true;
						alias.buyerRS = unconfirmedTransaction.recipientRS;
						alias.priceNQT = unconfirmedTransaction.priceNQT;
					}

					var allowCancel = false;

					if (alias.buyerRS) {
						if (alias.priceNQT == "0") {
							if (alias.buyerRS == NRS.accountRS) {
								alias.status = "Cancelling Sale";
							} else {
								alias.status = "Transfer In Progress";
							}
						} else {
							allowCancel = true;

							if (alias.buyerRS != NRS.genesisRS) {
								alias.status = "For Sale (direct)";
							} else {
								alias.status = "For Sale (indirect)";
							}
						}
					}

					if (alias.status != "/") {
						alias.status = "<span class='label label-info'>" + alias.status + "</span>";
					}

					rows += "<tr" + (alias.tentative ? " class='tentative'" : "") + " data-alias='" + String(alias.aliasName).toLowerCase().escapeHTML() + "'><td class='alias'>" + String(alias.aliasName).escapeHTML() + "</td><td class='uri'>" + (alias.aliasURI.indexOf("http") === 0 ? "<a href='" + String(alias.aliasURI).escapeHTML() + "' target='_blank'>" + String(alias.aliasURI).escapeHTML() + "</a>" : String(alias.aliasURI).escapeHTML()) + "</td><td class='status'>" + alias.status + "</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#register_alias_modal' data-alias='" + String(alias.aliasName).escapeHTML() + "'>Edit</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#transfer_alias_modal' data-alias='" + String(alias.aliasName).escapeHTML() + "'>Transfer</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#sell_alias_modal' data-alias='" + String(alias.aliasName).escapeHTML() + "'>Sell</a>" + (allowCancel ? " <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#cancel_alias_sale_modal' data-alias='" + String(alias.aliasName).escapeHTML() + "'>Cancel Sale</a>" : "") + "</td></tr>";

					if (!alias.aliasURI) {
						empty_alias_count++;
					} else if (alias.aliasURI.indexOf("http") === 0) {
						alias_uri_count++;
					} else if (alias.aliasURI.indexOf("acct:") === 0 || alias.aliasURI.indexOf("nacc:") === 0) {
						alias_account_count++;
					}
				}

				$("#aliases_table tbody").empty().append(rows);
				NRS.dataLoadFinished($("#aliases_table"));

				$("#alias_account_count").html(alias_account_count).removeClass("loading_dots");
				$("#alias_uri_count").html(alias_uri_count).removeClass("loading_dots");
				$("#empty_alias_count").html(empty_alias_count).removeClass("loading_dots");
				$("#alias_count").html(alias_count).removeClass("loading_dots");
			} else {
				$("#aliases_table tbody").empty();
				NRS.dataLoadFinished($("#aliases_table"));

				$("#alias_account_count, #alias_uri_count, #empty_alias_count, #alias_count").html("0").removeClass("loading_dots");
			}

			NRS.pageLoaded();
		});
	}

	$("#transfer_alias_modal, #sell_alias_modal, #cancel_alias_sale_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var alias = String($invoker.data("alias"));

		$(this).find("input[name=aliasName]").val(alias.escapeHTML());
		$(this).find(".alias_name_display").html(alias.escapeHTML());

		if ($(this).attr("id") == "sell_alias_modal") {
			$(this).find("ul.nav-pills li").removeClass("active");
			$(this).find("ul.nav-pills li:first-child").addClass("active");
			$("#sell_alias_recipient_div").show();
		}
	});

	NRS.forms.sellAlias = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		if (data.cancelSale) {
			data.priceNQT = "0";
			data.recipient = NRS.accountRS;
			delete data.cancelSale;
		}

		return {
			"data": data
		};
	}

	NRS.forms.sellAliasComplete = function(response, data) {
		if (response.alreadyProcessed) {
			return;
		}

		console.log(response);
		console.log(data);

		var $row = $("#aliases_table tr[data-alias=" + String(data.aliasName).toLowerCase().escapeHTML() + "]");

		$row.addClass("tentative");

		//transfer
		if (data.priceNQT == "0") {
			if (data.recipient == NRS.accountRS) {
				$row.find("td.status").html("<span class='label label-info'>Cancelling Sale</span>");
			} else {
				$row.find("td.status").html("<span class='label label-info'>Transfer In Progress</span>");
			}
		} else {
			if (data.recipient != "0") {
				$row.find("td.status").html("<span class='label label-info'>For Sale (direct)</span>");
			} else {
				$row.find("td.status").html("<span class='label label-info'>For Sale (indirect)</span>");
			}
		}
	}

	$("#sell_alias_to_specific_account, #sell_alias_to_anyone").on("click", function(e) {
		e.preventDefault();

		$(this).closest("ul").find("li").removeClass("active");
		$(this).parent().addClass("active");

		var $modal = $(this).closest(".modal");

		if ($(this).attr("id") == "sell_alias_to_anyone") {
			$modal.find("input[name=recipient]").val("0");
			$("#sell_alias_recipient_div").hide();
		} else {
			$modal.find("input[name=recipient]").val("");
			$("#sell_alias_recipient_div").show();
		}

		$modal.find("input[name=converted_account_id]").val("");
		$modal.find(".callout").hide();
	});

	$("#buy_alias_modal").on("show.bs.modal", function(e) {
		var $modal = $(this);

		var $invoker = $(e.relatedTarget);

		NRS.fetchingModalData = true;

		var alias = String($invoker.data("alias"));

		NRS.sendRequest("getAlias", {
			"aliasName": alias
		}, function(response) {
			NRS.fetchingModalData = false;

			if (response.errorCode) {
				e.preventDefault();
				$.growl("Could not find alias.", {
					"type": "danger"
				});
			} else {
				if (!response.buyerRS) {
					e.preventDefault();
					$.growl("This alias is no longer for sale.", {
						"type": "danger"
					});
				} else if (response.buyerRS != NRS.genesisRS && response.buyerRS != NRS.accountRS) {
					e.preventDefault();
					$.growl("This alias is offered for sale to another account pending decision.", {
						"type": "danger"
					});
				} else {
					$modal.find("input[name=aliasName]").val(alias.escapeHTML());
					$modal.find(".alias_name_display").html(alias.escapeHTML());
					$modal.find("input[name=priceNXT]").val(NRS.convertToNXT(response.priceNQT));
				}
			}
		}, false);
	});

	$("#register_alias_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var alias = $invoker.data("alias");

		if (alias) {
			NRS.fetchingModalData = true;

			alias = String(alias);

			NRS.sendRequest("getAlias", {
				"aliasName": alias
			}, function(response) {
				if (response.errorCode) {
					e.preventDefault();
					$.growl("Could not find alias.", {
						"type": "danger"
					});
					NRS.fetchingModalData = false;
				} else {
					var aliasURI;

					if (/http:\/\//i.test(response.aliasURI)) {
						NRS.forms.setAliasType("uri");
					} else if ((aliasURI = /acct:(.*)@nxt/.exec(response.aliasURI)) || (aliasURI = /nacc:(.*)/.exec(response.aliasURI))) {
						NRS.forms.setAliasType("account");
						response.aliasURI = String(aliasURI[1]).toUpperCase();
					} else {
						NRS.forms.setAliasType("general");
					}

					$("#register_alias_modal h4.modal-title").html("Update Alias");
					$("#register_alias_modal .btn-primary").html("Update");
					$("#register_alias_alias").val(alias.escapeHTML()).hide();
					$("#register_alias_alias_noneditable").html(alias.escapeHTML()).show();
					$("#register_alias_alias_update").val(1);
					$("#register_alias_uri").val(response.aliasURI);
				}
			}, false);
		} else {
			$("#register_alias_modal h4.modal-title").html("Register Alias");
			$("#register_alias_modal .btn-primary").html("Register");
			$("#register_alias_alias").val("").show();
			$("#register_alias_alias_noneditable").html("").hide();
			$("#register_alias_alias_update").val(0);
			NRS.forms.setAliasType("uri");
		}
	});

	NRS.incoming.aliases = function(transactions) {
		if (NRS.hasTransactionUpdates(transactions)) {
			NRS.loadPage("aliases");
		}
	}

	NRS.forms.setAlias = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		data.aliasURI = $.trim(data.aliasURI).toLowerCase();

		if (data.type == "account") {
			if (!(/acct:(.*)@nxt/.test(data.aliasURI)) && !(/nacc:(.*)/.test(data.aliasURI))) {
				if (/^(NXT\-)/i.test(data.aliasURI)) {
					data.aliasURI = "acct:" + data.aliasURI + "@nxt";
				} else if (/^\d+$/.test(data.aliasURI)) {
					return {
						"error": "Numeric account ID's are no longer allowed."
					};
				} else {
					return {
						"error": "Invalid account ID."
					};
				}
			}
		}

		delete data["type"];

		if ($("#register_alias_alias_update").val() == 1) {
			return {
				"data": data,
				"successMessage": "Alias updated successfully"
			};
		} else {
			return {
				"data": data
			};
		}
	}

	NRS.forms.setAliasType = function(type, uri) {
		$("#register_alias_type").val(type);

		if (type == "uri") {
			$("#register_alias_uri_label").html("URI");
			$("#register_alias_uri").prop("placeholder", "URI");
			if (uri) {
				if (uri == NRS.accountRS) {
					$("#register_alias_uri").val("http://");
				} else if (!/https?:\/\//i.test(uri)) {
					$("#register_alias_uri").val("http://" + uri);
				} else {
					$("#register_alias_uri").val(uri);
				}
			} else {
				$("#register_alias_uri").val("http://");
			}
			$("#register_alias_help").hide();
		} else if (type == "account") {
			$("#register_alias_uri_label").html("Account ID");
			$("#register_alias_uri").prop("placeholder", "Account ID");
			$("#register_alias_uri").val("");
			if (uri) {
				if (!(/acct:(.*)@nxt/.test(uri)) && !(/nacc:(.*)/.test(uri))) {
					if (/^\d+$/.test(uri)) {
						$("#register_alias_uri").val(uri);
					} else {
						$("#register_alias_uri").val(NRS.accountRS);
					}
				} else {
					$("#register_alias_uri").val(uri);
				}
			} else {
				$("#register_alias_uri").val(NRS.accountRS);
			}
			$("#register_alias_help").html("The alias will reference the account number entered and can be used to send Nxt to, messages, etc..").show();
		} else {
			$("#register_alias_uri_label").html("Data");
			$("#register_alias_uri").prop("placeholder", "Data");
			if (uri) {
				if (uri == NRS.accountRS) {
					$("#register_alias_uri").val("");
				} else if (uri == "http://") {
					$("#register_alias_uri").val("");
				} else {
					$("#register_alias_uri").val(uri);
				}
			}
			$("#register_alias_help").html("The alias can contain any data you want.").show();
		}
	}

	$("#register_alias_type").on("change", function() {
		var type = $(this).val();
		NRS.forms.setAliasType(type, $("#register_alias_uri").val());
	});

	NRS.forms.setAliasComplete = function(response, data) {
		if (response.alreadyProcessed) {
			return;
		}

		if (NRS.currentPage == "aliases") {
			data.aliasName = String(data.aliasName).escapeHTML();
			data.aliasURI = String(data.aliasURI).escapeHTML();

			var $table = $("#aliases_table tbody");

			var $row = $table.find("tr[data-alias=" + data.aliasName.toLowerCase() + "]");

			if ($row.length) {
				$row.addClass("tentative");
				$row.find("td.alias").html(data.aliasName);

				if (data.aliasURI && data.aliasURI.indexOf("http") === 0) {
					$row.find("td.uri").html("<a href='" + data.aliasURI + "' target='_blank'>" + data.aliasURI + "</a>");
				} else {
					$row.find("td.uri").html(data.aliasURI);
				}
			} else {
				var $rows = $table.find("tr");

				var rowToAdd = "<tr class='tentative' data-alias='" + data.aliasName.toLowerCase() + "'><td class='alias'>" + data.aliasName + "</td><td class='uri'>" + (data.aliasURI && data.aliasURI.indexOf("http") === 0 ? "<a href='" + data.aliasURI + "' target='_blank'>" + data.aliasURI + "</a>" : data.aliasURI) + "</td><td>/</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#'>Edit</a> <a class='btn btn-xs btn-default' href='#'>Transfer</a> <a class='btn btn-xs btn-default' href='#'>Sell</a> <a class='btn btn-xs btn-default' href='#'>Cancel Sale</a></td></tr>";

				var rowAdded = false;

				var newAlias = data.aliasName.toLowerCase();

				if ($rows.length) {
					$rows.each(function() {
						var alias = $(this).data("alias");

						if (newAlias < alias) {
							$(this).before(rowToAdd);
							rowAdded = true;
							return false;
						}
					});
				}

				if (!rowAdded) {
					$table.append(rowToAdd);
				}

				if ($("#aliases_table").parent().hasClass("data-empty")) {
					$("#aliases_table").parent().removeClass("data-empty");
				}
			}
		}
	}

	$("#alias_search").on("submit", function(e) {
		e.preventDefault();

		if (NRS.fetchingModalData) {
			return;
		}

		NRS.fetchingModalData = true;

		var alias = $.trim($("#alias_search input[name=q]").val());

		$("#alias_info_table tbody").empty();

		NRS.sendRequest("getAlias", {
			"aliasName": alias
		}, function(response, input) {
			if (response.errorCode) {
				$.growl("Could not find alias.", {
					"type": "danger"
				});
				NRS.fetchingModalData = false;
			} else {
				$("#alias_info_modal_alias").html(String(response.aliasName).escapeHTML());

				var data = {
					"Account": NRS.getAccountTitle(response, "account"),
					"Last Updated": NRS.formatTimestamp(response.timestamp),
					"DataFormattedHTML": String(response.aliasURI).autoLink()
				}

				if (response.buyerRS) {
					if (response.buyerRS == NRS.accountRS) {
						$("#alias_sale_callout").html("You have been offered this alias for " + NRS.formatAmount(response.priceNQT) + " NXT. <a href='#' data-alias='" + String(response.aliasName).escapeHTML() + "' data-toggle='modal' data-target='#buy_alias_modal'>Buy it?</a>").show();
					} else if (response.buyerRS == NRS.genesisRS) {
						$("#alias_sale_callout").html("This alias is offered for sale for " + NRS.formatAmount(response.priceNQT) + " NXT. <a href='#' data-alias='" + String(response.aliasName).escapeHTML() + "' data-toggle='modal' data-target='#buy_alias_modal'>Buy it?</a>").show();
					} else {
						$("#alias_sale_callout").html("This alias is offered for sale to another account pending decision.").show();
					}
				} else {
					$("#alias_sale_callout").hide();
				}

				$("#alias_info_table tbody").append(NRS.createInfoTable(data));

				$("#alias_info_modal").modal("show");
				NRS.fetchingModalData = false;
			}
		});
	});

	return NRS;
}(NRS || {}, jQuery));