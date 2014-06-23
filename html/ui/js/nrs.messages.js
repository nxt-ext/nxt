var NRS = (function(NRS, $, undefined) {
	NRS.messages = {};

	var totalMessages = 0;
	var totalMessagesLoaded = 0;
	var messageTypesChecked = 0;

	NRS.pages.messages = function(callback) {
		NRS.messages = {};

		totalMessages = 0;
		totalMessagesLoaded = 0;
		messageTypesChecked = 0;

		$(".content.content-stretch:visible").width($(".page:visible").width());

		NRS.getAccountMessages("public", function() {
			NRS.getAccountMessagesComplete(callback);
		});

		NRS.getAccountMessages("private", function() {
			NRS.getAccountMessagesComplete(callback);
		});
	}

	NRS.getAccountMessages = function(type, callback) {
		NRS.sendRequest("getAccountTransactionIds+", {
			"account": NRS.account,
			"timestamp": 0,
			"type": 1,
			"subtype": (type == "public" ? 0 : 8)
		}, function(response) {
			messageTypesChecked++;

			if (response.transactionIds && response.transactionIds.length) {
				var transactionIds = response.transactionIds.reverse().slice(0, 75);
				var nrTransactions = 0;

				totalMessages += transactionIds.length;

				for (var i = 0; i < transactionIds.length; i++) {
					NRS.sendRequest("getTransaction+", {
						"transaction": transactionIds[i]
					}, function(response) {
						nrTransactions++;
						totalMessagesLoaded++;

						var otherUser = (response.recipient == NRS.account ? response.sender : response.recipient);

						if (!(otherUser in NRS.messages)) {
							NRS.messages[otherUser] = [];
						}

						NRS.messages[otherUser].push(response);

						if (nrTransactions == transactionIds.length) {
							callback();
						}
					});
				}
			} else {
				callback();
			}
		});
	}

	NRS.getAccountMessagesComplete = function(callback) {
		if (totalMessages == totalMessagesLoaded && messageTypesChecked == 2) {
			if (NRS.totalMessages == 0) {
				$("#no_message_selected").hide();
				$("#no_messages_available").show();
				$("#messages_sidebar").empty();
				NRS.pageLoaded(callback);
			} else {
				var rows = "";
				var menu = "";

				var sortedMessages = [];

				for (var otherUser in NRS.messages) {
					NRS.messages[otherUser].sort(function(a, b) {
						if (a.timestamp > b.timestamp) {
							return 1;
						} else if (a.timestamp < b.timestamp) {
							return -1;
						} else {
							return 0;
						}
					});

					var otherUserRS = (otherUser == NRS.messages[otherUser][0].sender ? NRS.messages[otherUser][0].senderRS : NRS.messages[otherUser][0].recipientRS);

					sortedMessages.push({
						"timestamp": NRS.messages[otherUser][NRS.messages[otherUser].length - 1].timestamp,
						"user": otherUser,
						"userRS": otherUserRS
					});
				}

				sortedMessages.sort(function(a, b) {
					if (a.timestamp < b.timestamp) {
						return 1;
					} else if (a.timestamp > b.timestamp) {
						return -1;
					} else {
						return 0;
					}
				});

				for (var i = 0; i < sortedMessages.length; i++) {
					var sortedMessage = sortedMessages[i];

					var extra = "";

					if (sortedMessage.user in NRS.contacts) {
						extra = " data-contact='" + NRS.getAccountTitle(sortedMessage, "user") + "' data-context='messages_sidebar_update_context'";
					}

					rows += "<a href='#' class='list-group-item' data-account='" + NRS.getAccountFormatted(sortedMessage, "user") + "' data-account-id='" + NRS.getAccountFormatted(sortedMessage.user) + "'" + extra + "><h4 class='list-group-item-heading'>" + NRS.getAccountTitle(sortedMessage, "user") + "</h4><p class='list-group-item-text'>" + NRS.formatTimestamp(sortedMessage.timestamp) + "</p></a>";
				}

				$("#messages_sidebar").empty().append(rows);

				NRS.pageLoaded(callback);
			}
		}
	}

	NRS.incoming.messages = function(transactions) {
		if (NRS.hasTransactionUpdates(transactions)) {
			//save current scrollTop    	
			var activeAccount = $("#messages_sidebar a.active");

			if (activeAccount.length) {
				activeAccount = activeAccount.data("account");
			} else {
				activeAccount = -1;
			}

			NRS.loadPage("messages", function() {
				$("#messages_sidebar a[data-account=" + activeAccount + "]").trigger("click");
			});
		}
	}

	$("#messages_sidebar").on("click", "a", function(e) {
		e.preventDefault();

		$("#messages_sidebar a.active").removeClass("active");
		$(this).addClass("active");

		var otherUser = $(this).data("account-id");

		$("#no_message_selected, #no_messages_available").hide();

		$("#inline_message_recipient").val(otherUser);
		$("#inline_message_form").show();

		var last_day = "";
		var output = "<dl class='chat'>";

		var messages = NRS.messages[otherUser];

		var sharedKey = null;

		if (messages) {
			for (var i = 0; i < messages.length; i++) {
				var message = messages[i].attachment;

				var decoded = "";
				var extra = "";

				if (messages[i].subtype == 8) {
					if (!sharedKey) {
						try {
							sharedKey = NRS.getSharedKeyWithAccount(otherUser);
						} catch (err) {
							sharedKey = -1;
						}
					}

					if (sharedKey != -1) {
						try {
							decoded = NRS.decryptNote(message.message, {
								"nonce": message.nonce,
								"sharedKey": sharedKey
							});
						} catch (err) {
							var message = String(err.message ? err.message : err);
							//.. handle
						}
					} else {
						//console.log("no shared key");
					}
				} else {
					try {
						decoded = converters.hexStringToString(message.message);
					} catch (err) {
						//legacy...
						if (message.message.indexOf("feff") === 0) {
							decoded = NRS.convertFromHex16(message.message);
						} else {
							decoded = NRS.convertFromHex8(message.message);
						}
					}
				}

				if (decoded) {
					decoded = decoded.escapeHTML().nl2br();
				} else {
					decoded = "<i class='fa fa-warning'></i> Could not decrypt message.";
					extra = "decryption_failed";
				}

				var day = NRS.formatTimestamp(messages[i].timestamp, true);

				if (day != last_day) {
					output += "<dt><strong>" + day + "</strong></dt>";
					last_day = day;
				}

				output += "<dd class='" + (messages[i].recipient == NRS.account ? "from" : "to") + "'><p class='" + extra + "'>" + decoded + "</p></dd>";
			}
		}

		if (NRS.unconfirmedTransactions.length) {
			for (var i = 0; i < NRS.unconfirmedTransactions.length; i++) {
				var unconfirmedTransaction = NRS.unconfirmedTransactions[i];

				if (unconfirmedTransaction.type == 1 && unconfirmedTransaction.subtype == 0 && unconfirmedTransaction.recipient == otherUser) {
					var hex = unconfirmedTransaction.attachment.message;
					if (hex.indexOf("feff") === 0) {
						var decoded = NRS.convertFromHex16(hex);
					} else {
						var decoded = NRS.convertFromHex8(hex);
					}

					output += "<dd class='to tentative'><p>" + decoded.escapeHTML().nl2br() + "</p></dd>";
				}
			}
		}

		output += "</dl>";

		$("#message_details").empty().append(output);
	});

	$("#messages_sidebar_context").on("click", "a", function(e) {
		e.preventDefault();

		var account = NRS.getAccountFormatted(NRS.selectedContext.data("account"));
		var option = $(this).data("option");

		NRS.closeContextMenu();

		if (option == "add_contact") {
			$("#add_contact_account_id").val(account).trigger("blur");
			$("#add_contact_modal").modal("show");
		} else if (option == "send_nxt") {
			$("#send_money_recipient").val(account).trigger("blur");
			$("#send_money_modal").modal("show");
		} else if (option == "account_info") {
			NRS.showAccountModal(account);
		}
	});

	$("#messages_sidebar_update_context").on("click", "a", function(e) {
		e.preventDefault();

		var account = NRS.getAccountFormatted(NRS.selectedContext.data("account"));
		var option = $(this).data("option");

		NRS.closeContextMenu();

		if (option == "update_contact") {
			$("#update_contact_modal").modal("show");
		} else if (option == "send_nxt") {
			$("#send_money_recipient").val(NRS.selectedContext.data("contact")).trigger("blur");
			$("#send_money_modal").modal("show");
		}

	});

	NRS.forms.sendMessage = function($modal) {
		var requestType = "sendMessage";

		var data = {
			"recipient": $.trim($("#send_message_recipient").val()),
			"feeNXT": $.trim($("#send_message_fee").val()),
			"deadline": $.trim($("#send_message_deadline").val()),
			"secretPhrase": $.trim($("#send_message_password").val())
		};

		var message = $.trim($("#send_message_message").val());

		if (!message) {
			return {
				"error": "Message is a required field."
			};
		}

		if ($("#send_message_encrypt").is(":checked")) {
			try {
				var encrypted = NRS.encryptNote(message, {
					"account": data.recipient
				}, NRS.rememberPassword ? NRS.password : data.secretPhrase);

				requestType = "sendEncryptedNote";

				data["encryptedNote"] = encrypted.message;
				data["encryptedNoteNonce"] = encrypted.nonce;
			} catch (err) {
				return {
					"error": "Could not encrypt message."
				};
			}
		} else {
			data["message"] = converters.stringToHexString(message);
		}

		data["_extra"] = {
			"message": message
		};

		return {
			"requestType": requestType,
			"data": data
		};
	}

	$("#inline_message_form").submit(function(e) {
		e.preventDefault();

		var data = {
			"recipient": $.trim($("#inline_message_recipient").val()),
			"feeNXT": "1",
			"deadline": "1440",
			"secretPhrase": $.trim($("#inline_message_password").val())
		};

		if (!NRS.rememberPassword) {
			if ($("#inline_message_password").val() == "") {
				$.growl("Secret phrase is a required field.", {
					"type": "danger"
				});
				return;
			}

			var accountId = NRS.getAccountId(data.secretPhrase);

			if (accountId != NRS.account) {
				$.growl("Incorrect secret phrase.", {
					"type": "danger"
				});
				return;
			}
		}

		var message = $.trim($("#inline_message_text").val());

		if (!message) {
			$.growl("Message is a required field.", {
				"type": "danger"
			});
			return;
		}

		var $btn = $("#inline_message_submit");

		$btn.button("loading");

		var hex = "";
		var error = "";

		if ($("#inline_message_encrypt").is(":checked")) {
			hex = NRS.encryptNote(message, {
				"account": data.recipient
			}, NRS.rememberPassword ? NRS.password : data.secretPhrase);
		} else {
			hex = converters.stringToHexString(message);
		}

		data["_extra"] = {
			"message": message
		};
		data["message"] = hex;

		NRS.sendRequest("sendMessage", data, function(response, input) {
			if (response.errorCode) {
				$.growl(response.errorDescription ? response.errorDescription.escapeHTML() : "Unknown error occured.", {
					type: "danger"
				});
			} else if (response.fullHash) {
				$.growl("Message sent.", {
					type: "success"
				});

				$("#inline_message_text").val("");

				NRS.addUnconfirmedTransaction(response.transaction, function(alreadyProcessed) {
					if (!alreadyProcessed) {
						$("#message_details dl.chat").append("<dd class='to tentative'><p>" + data["_extra"].message.escapeHTML() + "</p></dd>");
					}
				});

				//leave password alone until user moves to another page.
			} else {
				$.growl("An unknown error occured. Your message may or may not have been sent.", {
					type: "danger"
				});
			}
			$btn.button("reset");
		});
	});

	NRS.forms.sendMessageComplete = function(response, data) {
		data.message = data._extra.message;

		if (!(data["_extra"] && data["_extra"].convertedAccount)) {
			$.growl("Your message has been sent! <a href='#' data-account='" + NRS.getAccountFormatted(data, "recipient") + "' data-toggle='modal' data-target='#add_contact_modal' style='text-decoration:underline'>Add recipient to contacts?</a>", {
				"type": "success"
			});
		} else {
			$.growl("Your message has been sent!", {
				"type": "success"
			});
		}

		if (NRS.currentPage == "messages") {
			var date = new Date(Date.UTC(2013, 10, 24, 12, 0, 0, 0)).getTime();

			var now = parseInt(((new Date().getTime()) - date) / 1000, 10);

			var $sidebar = $("#messages_sidebar");

			var $existing = $sidebar.find("a.list-group-item[data-account=" + NRS.getAccountFormatted(data, "recipient") + "]");

			if ($existing.length) {
				if (response.alreadyProcesed) {
					return;
				}
				$sidebar.prepend($existing);
				$existing.find("p.list-group-item-text").html(NRS.formatTimestamp(now));

				if ($existing.hasClass("active")) {
					$("#message_details dl.chat").append("<dd class='to tentative'><p>" + data.message.escapeHTML() + "</p></dd>");
				}
			} else {
				var accountTitle = NRS.getAccountTitle(data, "recipient");

				var extra = "";

				if (accountTitle != data.recipient) {
					extra = " data-context='messages_sidebar_update_context'";
				}

				var listGroupItem = "<a href='#' class='list-group-item' data-account='" + NRS.getAccountFormatted(data, "recipient") + "'" + extra + "><h4 class='list-group-item-heading'>" + accountTitle + "</h4><p class='list-group-item-text'>" + NRS.formatTimestamp(now) + "</p></a>";
				$("#messages_sidebar").prepend(listGroupItem);
			}
		}
	}

	return NRS;
}(NRS || {}, jQuery));