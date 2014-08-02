/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.pages.aliases = function() {
		NRS.sendRequest("getAliases+", {
			"account": NRS.account,
			"timestamp": 0
		}, function(response) {
			if (response.aliases && response.aliases.length) {
				var aliases = response.aliases;

				if (NRS.unconfirmedTransactions.length) {
					for (var i = 0; i < NRS.unconfirmedTransactions.length; i++) {
						var unconfirmedTransaction = NRS.unconfirmedTransactions[i];

						if (unconfirmedTransaction.type == 1 && (unconfirmedTransaction.subtype == 1 || unconfirmedTransaction.subtype == 7)) {
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
									"aliasURI": (unconfirmedTransaction.attachment.uri ? unconfirmedTransaction.attachment.uri : ""),
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
						alias.buyer = unconfirmedTransaction.recipient;
						alias.priceNQT = unconfirmedTransaction.priceNQT;
					}

					if (!alias.aliasURI) {
						alias.aliasURI = "";
					}

					var allowCancel = false;

					if (alias.buyer) {
						if (alias.priceNQT == "0") {
							if (alias.buyer == NRS.account) {
								alias.status = $.t("cancelling_sale");
							} else {
								alias.status = $.t("transfer_in_progress");
							}
						} else {
							if (!alias.tentative) {
								allowCancel = true;
							}

							if (alias.buyer != NRS.genesis) {
								alias.status = $.t("for_sale_direct");
							} else {
								alias.status = $.t("for_sale_indirect");
							}
						}
					}

					if (alias.status != "/") {
						alias.status = "<span class='label label-info'>" + alias.status + "</span>";
					}

					rows += "<tr" + (alias.tentative ? " class='tentative'" : "") + " data-alias='" + String(alias.aliasName).toLowerCase().escapeHTML() + "'><td class='alias'>" + String(alias.aliasName).escapeHTML() + "</td><td class='uri'>" + (alias.aliasURI.indexOf("http") === 0 ? "<a href='" + String(alias.aliasURI).escapeHTML() + "' target='_blank'>" + String(alias.aliasURI).escapeHTML() + "</a>" : String(alias.aliasURI).escapeHTML()) + "</td><td class='status'>" + alias.status + "</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#register_alias_modal' data-alias='" + String(alias.aliasName).escapeHTML() + "'>" + $.t("edit") + "</a>" + (NRS.dgsBlockPassed ? " <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#transfer_alias_modal' data-alias='" + String(alias.aliasName).escapeHTML() + "'>" + $.t("transfer") + "</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#sell_alias_modal' data-alias='" + String(alias.aliasName).escapeHTML() + "'>" + $.t("sell") + "</a>" + (allowCancel ? " <a class='btn btn-xs btn-default cancel_alias_sale' href='#' data-toggle='modal' data-target='#cancel_alias_sale_modal' data-alias='" + String(alias.aliasName).escapeHTML() + "'>" + $.t("cancel_sale") + "</a>" : "") : "") + "</td></tr>";

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

		var successMessage = "";
		var errorMessage = "";

		if (data.cancelSale) {
			data.priceNQT = "0";
			data.recipient = NRS.accountRS;
			delete data.cancelSale;

			successMessage = $.t("success_cancel_alias");
			errorMessage = $.t("error_cancel_alias");
		} else if (data.priceNQT == "0") {
			if (!data.recipient) {
				return {
					"error": $.t("error_not_specified", {
						"name": $.t("recipient").toLowerCase()
					}).capitalize()
				};
			}
			successMessage = $.t("success_transfer_alias");
			errorMessage = $.t("error_transfer_alias");
		} else {
			successMessage = $.t("success_sell_alias");
			errorMessage = $.t("error_sell_alias");

			delete data.add_message;
			delete data.encrypt_message;
			delete data.message;
		}

		return {
			"data": data,
			"successMessage": successMessage,
			"errorMessage": errorMessage
		};
	}

	NRS.forms.sellAliasComplete = function(response, data) {
		if (response.alreadyProcessed) {
			return;
		}

		var $row = $("#aliases_table tr[data-alias=" + String(data.aliasName).toLowerCase().escapeHTML() + "]");

		$row.addClass("tentative");

		//transfer
		if (data.priceNQT == "0") {
			if (data.recipient == NRS.account) {
				$row.find("td.status").html("<span class='label label-info'>" + $.t("cancelling_sale") + "</span>");
				$row.find("a.cancel_alias_sale").remove();
			} else {
				$row.find("td.status").html("<span class='label label-info'>" + $.t("transfer_in_progress") + "</span>");
			}
		} else {
			if (data.recipient != NRS.genesis) {
				$row.find("td.status").html("<span class='label label-info'>" + $.t("for_sale_direct") + "</span>");
			} else {
				$row.find("td.status").html("<span class='label label-info'>" + $.t("for_sale_indirect") + "</span>");
			}
		}
	}

	/*
	$("#sell_alias_add_message").on("change", function(e) {
		var $modal = $(this).closest(".modal");
		var $active = $modal.find(".nav li.active a").first();

		if ($active.attr("id") == "sell_alias_to_anyone") {
			$("#sell_alias_to_anyone_message_options").show();
			$("#sell_alias_to_specific_account_message_options").hide();
		} else {
			$("#sell_alias_to_anyone_message_options").hide();
			$("#sell_alias_to_specific_account_message_options").show();
		}
	});*/

	$("#sell_alias_to_specific_account, #sell_alias_to_anyone").on("click", function(e) {
		e.preventDefault();

		$(this).closest("ul").find("li").removeClass("active");
		$(this).parent().addClass("active");

		var $modal = $(this).closest(".modal");

		if ($(this).attr("id") == "sell_alias_to_anyone") {
			$modal.find("input[name=recipient]").val(NRS.genesisRS);
			$("#sell_alias_recipient_div").hide();
			$modal.find(".add_message_container, .optional_message").hide();
		} else {
			$modal.find("input[name=recipient]").val("");
			$("#sell_alias_recipient_div").show();
			$modal.find(".add_message_container").show();

			if ($("#sell_alias_add_message").is(":checked")) {
				$modal.find(".optional_message").show();
			} else {
				$modal.find(".optional_message").hide();
			}
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
				$.growl($.t("error_alias_not_found"), {
					"type": "danger"
				});
			} else {
				if (!response.buyer) {
					e.preventDefault();
					$.growl($.t("error_alias_not_for_sale"), {
						"type": "danger"
					});
				} else if (response.buyer != NRS.genesis && response.buyer != NRS.account) {
					e.preventDefault();
					$.growl($.t("error_alias_sale_different_account"), {
						"type": "danger"
					});
				} else {
					$modal.find("input[name=recipient]").val(String(response.accountRS).escapeHTML());
					$modal.find("input[name=aliasName]").val(alias.escapeHTML());
					$modal.find(".alias_name_display").html(alias.escapeHTML());
					$modal.find("input[name=amountNXT]").val(NRS.convertToNXT(response.priceNQT)).prop("readonly", true);
				}
			}
		}, false);
	});

	NRS.forms.buyAliasError = function() {
		$("#buy_alias_modal").find("input[name=priceNXT]").prop("readonly", false);
	}

	NRS.forms.buyAliasComplete = function(response, data) {
		if (response.alreadyProcessed) {
			return;
		}

		if (NRS.currentPage != "aliases") {
			return;
		}

		data.aliasName = String(data.aliasName).escapeHTML();
		data.aliasURI = "";

		$("#aliases_table tbody").prepend("<tr class='tentative' data-alias='" + data.aliasName.toLowerCase() + "'><td class='alias'>" + data.aliasName + "</td><td class='uri'>" + (data.aliasURI && data.aliasURI.indexOf("http") === 0 ? "<a href='" + data.aliasURI + "' target='_blank'>" + data.aliasURI + "</a>" : data.aliasURI) + "</td><td>/</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#'>" + $.t("edit") + "</a>" + (NRS.dgsBlockPassed ? " <a class='btn btn-xs btn-default' href='#'>" + $.t("transfer") + "</a> <a class='btn btn-xs btn-default' href='#'>" + $.t("sell") + "</a>" : "") + "</td></tr>");

		if ($("#aliases_table").parent().hasClass("data-empty")) {
			$("#aliases_table").parent().removeClass("data-empty");
		}
	}

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
					$.growl($.t("error_alias_not_found"), {
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

					$("#register_alias_modal h4.modal-title").html($.t("update_alias"));
					$("#register_alias_modal .btn-primary").html($.t("update"));
					$("#register_alias_alias").val(alias.escapeHTML()).hide();
					$("#register_alias_alias_noneditable").html(alias.escapeHTML()).show();
					$("#register_alias_alias_update").val(1);
					$("#register_alias_uri").val(response.aliasURI);
				}
			}, false);
		} else {
			$("#register_alias_modal h4.modal-title").html($.t("register_alias"));
			$("#register_alias_modal .btn-primary").html($.t("register"));

			var prefill = $invoker.data("prefill-alias");

			if (prefill) {
				$("#register_alias_alias").val(prefill).show();
			} else {
				$("#register_alias_alias").val("").show();
			}
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
						"error": $.t("error_numeric_ids_not_allowed")
					};
				} else {
					return {
						"error": $.t(".error_invalid_account_id")
					};
				}
			}
		}

		delete data["type"];

		if ($("#register_alias_alias_update").val() == 1) {
			return {
				"data": data,
				"successMessage": $.t("success_alias_update")
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
			$("#register_alias_uri_label").html($.t("uri"));
			$("#register_alias_uri").prop("placeholder", $.t("uri"));
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
			$("#register_alias_uri_label").html($.t("account_id"));
			$("#register_alias_uri").prop("placeholder", $.t("account_id"));
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
			$("#register_alias_help").html($.t("alias_account_help")).show();
		} else {
			$("#register_alias_uri_label").html($.t("data"));
			$("#register_alias_uri").prop("placeholder", $.t("data"));
			if (uri) {
				if (uri == NRS.accountRS) {
					$("#register_alias_uri").val("");
				} else if (uri == "http://") {
					$("#register_alias_uri").val("");
				} else {
					$("#register_alias_uri").val(uri);
				}
			}
			$("#register_alias_help").html($.t("alias_data_help")).show();
		}
	}

	$("#register_alias_type").on("change", function() {
		var type = $(this).val();
		NRS.forms.setAliasType(type, $("#register_alias_uri").val());
	});

	NRS.forms.setAliasError = function(response, data) {
		if (response && response.errorCode && response.errorCode == 8) {
			var errorDescription = response.errorDescription.escapeHTML();

			NRS.sendRequest("getAlias", {
				"aliasName": data.aliasName
			}, function(response) {
				var message;

				if (!response.errorCode) {
					if (response.buyer) {
						if (response.buyer == NRS.account) {
							message = $.t("alias_sale_direct_offer", {
								"nxt": NRS.formatAmount(response.priceNQT)
							}) + " <a href='#' data-alias='" + String(response.aliasName).escapeHTML() + "' data-toggle='modal' data-target='#buy_alias_modal'>" + $.t("buy_it_q") + "</a>";
						} else if (response.buyer == NRS.genesis) {
							message = $.t("alias_sale_indirect_offer", {
								"nxt": NRS.formatAmount(response.priceNQT)
							}) + " <a href='#' data-alias='" + String(response.aliasName).escapeHTML() + "' data-toggle='modal' data-target='#buy_alias_modal'>" + $.t("buy_it_q") + "</a>";
						} else {
							message = $.t("error_alias_sale_different_account");
						}
					} else {
						message = "<a href='#' data-user='" + NRS.getAccountFormatted(response, "account") + "'>" + $.t("view_owner_info_q") + "</a>";
					}

					$("#register_alias_modal").find(".error_message").html(errorDescription + ". " + message);
				}
			}, false);
		}
	}

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

				$.growl($.t("success_alias_update"), {
					"type": "success"
				});
			} else {
				var $rows = $table.find("tr");

				var rowToAdd = "<tr class='tentative' data-alias='" + data.aliasName.toLowerCase() + "'><td class='alias'>" + data.aliasName + "</td><td class='uri'>" + (data.aliasURI && data.aliasURI.indexOf("http") === 0 ? "<a href='" + data.aliasURI + "' target='_blank'>" + data.aliasURI + "</a>" : data.aliasURI) + "</td><td>/</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#'>" + $.t("edit") + "</a>" + (NRS.dgsBlockPassed ? " <a class='btn btn-xs btn-default' href='#'>" + $.t("transfer") + "</a> <a class='btn btn-xs btn-default' href='#'>" + $.t("sell") + "</a>" : "") + "</td></tr>";

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

				$.growl($.t("success_alias_register"), {
					"type": "success"
				});
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
				$.growl($.t("error_alias_not_found") + " <a href='#' data-toggle='modal' data-target='#register_alias_modal' data-prefill-alias='" + String(alias).escapeHTML() + "'>" + $.t("register_q") + "</a>", {
					"type": "danger"
				});
				NRS.fetchingModalData = false;
			} else {
				$("#alias_info_modal_alias").html(String(response.aliasName).escapeHTML());

				var data = {
					"account": NRS.getAccountTitle(response, "account"),
					"last_updated": NRS.formatTimestamp(response.timestamp),
					"data_formatted_html": String(response.aliasURI).autoLink()
				}

				if (response.buyer) {
					if (response.buyer == NRS.account) {
						$("#alias_sale_callout").html($.t("alias_sale_direct_offer", {
							"nxt": NRS.formatAmount(response.priceNQT)
						}) + " <a href='#' data-alias='" + String(response.aliasName).escapeHTML() + "' data-toggle='modal' data-target='#buy_alias_modal'>" + $.t("buy_it_q") + "</a>").show();
					} else if (response.buyer == NRS.genesis) {
						$("#alias_sale_callout").html($.t("alias_sale_indirect_offer", {
							"nxt": NRS.formatAmount(response.priceNQT)
						}) + " <a href='#' data-alias='" + String(response.aliasName).escapeHTML() + "' data-toggle='modal' data-target='#buy_alias_modal'>" + $.t("buy_it_q") + "</a>").show();
					} else {
						$("#alias_sale_callout").html($.t("error_alias_sale_different_account")).show();
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