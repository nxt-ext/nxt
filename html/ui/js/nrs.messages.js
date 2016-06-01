/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $) {
    var _messages;
    var _latestMessages;
	
    NRS.resetMessagesState = function () {
        _messages = {};
        _latestMessages = {};
	};
	NRS.resetMessagesState();

	NRS.pages.messages = function(callback) {
		_messages = {};
        $("#inline_message_form").hide();
        $("#message_details").empty();
        $("#no_message_selected").show();
		$(".content.content-stretch:visible").width($(".page:visible").width());

		NRS.sendRequest("getBlockchainTransactions+", {
			"account": NRS.account,
			"firstIndex": 0,
			"lastIndex": 75,
			"type": 1,
			"subtype": 0
		}, function(response) {
			if (response.transactions && response.transactions.length) {
				for (var i = 0; i < response.transactions.length; i++) {
					var otherUser = (response.transactions[i].recipient == NRS.account ? response.transactions[i].sender : response.transactions[i].recipient);
					if (!(otherUser in _messages)) {
						_messages[otherUser] = [];
					}
					_messages[otherUser].push(response.transactions[i]);
				}
				displayMessageSidebar(callback);
			} else {
				$("#no_message_selected").hide();
				$("#no_messages_available").show();
				$("#messages_sidebar").empty();
				NRS.pageLoaded(callback);
			}
		});
	};

	NRS.setup.messages = function() {
		NRS.addTreeviewSidebarMenuItem({
			"id": 'sidebar_messages',
			"titleHTML": '<i class="fa fa-envelope"></i> <span data-i18n="messages">Messages</span>',
			"page": 'my_messages',
			"desiredPosition": 90,
			"depends": {tags: [NRS.constants.API_TAGS.MESSAGES]}
		});
		NRS.appendMenuItemToTSMenuItem('sidebar_messages', {
			"titleHTML": '<i class="fa fa-comment"></i> <span data-i18n="chat">Chat</span>',
			"type": 'PAGE',
			"page": 'messages'
		});
	};

	NRS.jsondata = NRS.jsondata || {};

	NRS.jsondata.messages = function (response) {
		var transaction = NRS.getTransactionLink(response.transaction, NRS.formatTimestamp(response.timestamp));
		var from = NRS.getAccountLink(response, "sender");
		var to = NRS.getAccountLink(response, "recipient");
		var message = "place holder";
		var decryptAction = "decrypt";
		var retrieveAction = "retrieve";
		return {
			transactionFormatted: transaction,
			fromFormatted: from,
			toFormatted: to,
			messageFormatted: message,
			action_decrypt: decryptAction,
			action_retrieve: retrieveAction
		};
	};
	
	NRS.pages.my_messages = function (type) {
		NRS.hasMorePages = false;
		var view = NRS.simpleview.get('my_messages_section', {
			errorMessage: null,
			isLoading: true,
			isEmpty: false,
			messages: []
		});
		var params = {
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage,
			"account": NRS.account,
			"type": 1,
			"subtype": 0
		};
		NRS.sendRequest("getBlockchainTransactions+", params, 
			function (response) {
				if (response.transactions.length > NRS.itemsPerPage) {
					NRS.hasMorePages = true;
					response.transactions.pop();
				}
				view.messages.length = 0;
				response.transactions.forEach(
					function (transactionsJson) {
						view.messages.push(NRS.jsondata.messages(transactionsJson));
					}
				);
				view.render({
					isLoading: false,
					isEmpty: view.messages.length == 0
				});
				NRS.pageLoaded();
			}
		);
	};


	function displayMessageSidebar(callback) {
		var activeAccount = false;
		var messagesSidebar = $("#messages_sidebar");
		var $active = messagesSidebar.find("a.active");
		if ($active.length) {
			activeAccount = $active.data("account");
		}

		var rows = "";
		var sortedMessages = [];
		for (var otherUser in _messages) {
			if (!_messages.hasOwnProperty(otherUser)) {
				continue;
			}
			_messages[otherUser].sort(function (a, b) {
				if (a.timestamp > b.timestamp) {
					return 1;
				} else if (a.timestamp < b.timestamp) {
					return -1;
				} else {
					return 0;
				}
			});

			var otherUserRS = (otherUser == _messages[otherUser][0].sender ? _messages[otherUser][0].senderRS : _messages[otherUser][0].recipientRS);
			sortedMessages.push({
				"timestamp": _messages[otherUser][_messages[otherUser].length - 1].timestamp,
				"user": otherUser,
				"userRS": otherUserRS
			});
		}

		sortedMessages.sort(function (a, b) {
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
				extra = "data-contact='" + NRS.getAccountTitle(sortedMessage, "user") + "' data-context='messages_sidebar_update_context'";
			}
			rows += "<a href='#' class='list-group-item' data-account='" + NRS.getAccountFormatted(sortedMessage, "user") + "' data-account-id='" + NRS.getAccountFormatted(sortedMessage.user) + "' " + extra + ">" +
				"<h4 class='list-group-item-heading'>" + NRS.getAccountTitle(sortedMessage, "user") + "</h4>" +
				"<p class='list-group-item-text'>" + NRS.formatTimestamp(sortedMessage.timestamp) + "</p></a>";
		}
		messagesSidebar.empty().append(rows);
		if (activeAccount) {
			messagesSidebar.find("a[data-account=" + activeAccount + "]").addClass("active").trigger("click");
		}
		NRS.pageLoaded(callback);
	}

	NRS.incoming.messages = function(transactions) {
		if (NRS.hasTransactionUpdates(transactions)) {
			if (transactions.length) {
				for (var i=0; i<transactions.length; i++) {
					var trans = transactions[i];
					if (trans.confirmed && trans.type == 1 && trans.subtype == 0 && trans.senderRS != NRS.accountRS) {
						if (trans.height >= NRS.lastBlockHeight - 3 && !_latestMessages[trans.transaction]) {
							_latestMessages[trans.transaction] = trans;
							$.growl($.t("you_received_message", {
								"account": NRS.getAccountFormatted(trans, "sender"),
								"name": NRS.getAccountTitle(trans, "sender")
							}), {
								"type": "success"
							});
						}
					}
				}
			}
			if (NRS.currentPage == "messages") {
				NRS.loadPage("messages");
			}
		}
	};

    function getMessage(message) {
        var decoded = {};
        if (!message.attachment) {
            decoded.message = $.t("message_empty");
        } else if (message.attachment.encryptedMessage) {
            try {
                $.extend(decoded, NRS.tryToDecryptMessage(message));
                decoded.extra = "decrypted";
            } catch (err) {
                if (err.errorCode && err.errorCode == 1) {
                    decoded.message = $.t("error_decryption_passphrase_required");
                    decoded.extra = "to_decrypt";
                } else {
                    decoded.message = $.t("error_decryption_unknown");
                }
            }
        } else if (message.attachment.message) {
            if (!message.attachment["version.Message"] && !message.attachment["version.PrunablePlainMessage"]) {
                try {
                    decoded.message = converters.hexStringToString(message.attachment.message);
                } catch (err) {
                    //legacy
                    if (message.attachment.message.indexOf("feff") === 0) {
                        decoded.message = NRS.convertFromHex16(message.attachment.message);
                    } else {
                        decoded.message = NRS.convertFromHex8(message.attachment.message);
                    }
                }
            } else {
                decoded.message = String(message.attachment.message);
            }
        } else if (message.attachment.messageHash || message.attachment.encryptedMessageHash) {
            decoded.message = $.t("message_pruned");
        } else {
            decoded.message = $.t("message_empty");
        }
        if (!$.isEmptyObject(decoded)) {
            if (!decoded.message) {
                decoded.message = $.t("message_empty");
            }
            decoded.message = String(decoded.message).escapeHTML().nl2br();
            if (decoded.extra == "to_decrypt") {
                decoded.message = "<i class='fa fa-warning'></i> " + decoded.message;
            } else if (decoded.extra == "decrypted") {
                decoded.message = "<i class='fa fa-unlock'></i> " + decoded.message;
            }
        } else {
            decoded.message = "<i class='fa fa-warning'></i> " + $.t("error_could_not_decrypt_message");
            decoded.extra = "decryption_failed";
        }
        return decoded;
    };
    $("#messages_sidebar").on("click", "a", function(e) {
		e.preventDefault();
		$("#messages_sidebar").find("a.active").removeClass("active");
		$(this).addClass("active");
		var otherUser = $(this).data("account-id");
		$("#no_message_selected, #no_messages_available").hide();
		$("#inline_message_recipient").val(otherUser);
		$("#inline_message_form").show();

		var last_day = "";
		var output = "<dl class='chat'>";
		var messages = _messages[otherUser];
		if (messages) {
			for (var i = 0; i < messages.length; i++) {
                var decoded = getMessage(messages[i]);
				var day = NRS.formatTimestamp(messages[i].timestamp, true);
				if (day != last_day) {
					output += "<dt><strong>" + day + "</strong></dt>";
					last_day = day;
				}
				var messageClass = (messages[i].recipient == NRS.account ? "from" : "to") + (decoded.extra ? " " + decoded.extra : "");
				var sharedKeyTag = "";
                if (decoded.sharedKey) {
                    var inverseIcon = messages[i].recipient == NRS.account ? "" : " fa-inverse";
					sharedKeyTag = "<a href='#' class='btn btn-xs' data-toggle='modal' data-target='#shared_key_modal' " +
						"data-sharedkey='" + decoded.sharedKey + "' data-transaction='" + messages[i].transaction +"'>" +
						"<i class='fa fa-link" + inverseIcon + "'></i>" +
					"</a>";
				}
                output += "<dd class='" + messageClass + "'><p>" + decoded.message + sharedKeyTag + "</p></dd>";
			}
		}
		output += "</dl>";
		$("#message_details").empty().append(output);
        var splitter = $('#messages_page').find('.content-splitter-right-inner');
        splitter.scrollTop(splitter[0].scrollHeight);
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

	$("body").on("click", "a[data-goto-messages-account]", function(e) {
		e.preventDefault();
		var account = $(this).data("goto-messages-account");
		NRS.goToPage("messages", function(){ $('#message_sidebar').find('a[data-account=' + account + ']').trigger('click'); });
	});

	NRS.forms.sendMessage = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));
		var converted = $modal.find("input[name=converted_account_id]").val();
		if (converted) {
			data.recipient = converted;
		}
		return {
			"data": data
		};
	};

	$("#inline_message_form").submit(function(e) {
		e.preventDefault();
        var passpharse = $("#inline_message_password").val();
        var data = {
			"recipient": $.trim($("#inline_message_recipient").val()),
			"feeNXT": "1",
			"deadline": "1440",
			"secretPhrase": $.trim(passpharse)
		};

		if (!NRS.rememberPassword) {
			if (passpharse == "") {
				$.growl($.t("error_passphrase_required"), {
					"type": "danger"
				});
				return;
			}
			var accountId = NRS.getAccountId(data.secretPhrase);
			if (accountId != NRS.account) {
				$.growl($.t("error_passphrase_incorrect"), {
					"type": "danger"
				});
				return;
			}
		}

		data.message = $.trim($("#inline_message_text").val());
		var $btn = $("#inline_message_submit");
		$btn.button("loading");
		var requestType = "sendMessage";
		if ($("#inline_message_encrypt").is(":checked")) {
			data.encrypt_message = true;
		}
		if (data.message) {
			try {
				data = NRS.addMessageData(data, "sendMessage");
			} catch (err) {
				$.growl(String(err.message).escapeHTML(), {
					"type": "danger"
				});
				return;
			}
		} else {
			data["_extra"] = {
				"message": data.message
			};
		}

		NRS.sendRequest(requestType, data, function(response) {
			if (response.errorCode) {
				$.growl(NRS.translateServerError(response).escapeHTML(), {
					type: "danger"
				});
			} else if (response.fullHash) {
				$.growl($.t("success_message_sent"), {
					type: "success"
				});
				$("#inline_message_text").val("");
				if (data["_extra"].message && data.encryptedMessageData) {
					NRS.addDecryptedTransaction(response.transaction, {
						"encryptedMessage": String(data["_extra"].message)
					});
				}

                NRS.addUnconfirmedTransaction(response.transaction, function (alreadyProcessed) {
                    if (!alreadyProcessed) {
                        $("#message_details").find("dl.chat").append("<dd class='to tentative" + (data.encryptedMessageData ? " decrypted" : "") + "'><p>" + (data.encryptedMessageData ? "<i class='fa fa-lock'></i> " : "") + (!data["_extra"].message ? $.t("message_empty") : String(data["_extra"].message).escapeHTML()) + "</p></dd>");
                        var splitter = $('#messages_page').find('.content-splitter-right-inner');
                        splitter.scrollTop(splitter[0].scrollHeight);
                    }
                });
				//leave password alone until user moves to another page.
			} else {
				//TODO
				$.growl($.t("error_send_message"), {
					type: "danger"
				});
			}
			$btn.button("reset");
		});
	});

	NRS.forms.sendMessageComplete = function(response, data) {
		data.message = data._extra.message;
		if (!(data["_extra"] && data["_extra"].convertedAccount)) {
			$.growl($.t("success_message_sent") + " <a href='#' data-account='" + NRS.getAccountFormatted(data, "recipient") + "' data-toggle='modal' data-target='#add_contact_modal' style='text-decoration:underline'>" + $.t("add_recipient_to_contacts_q") + "</a>", {
				"type": "success"
			});
		} else {
			$.growl($.t("success_message_sent"), {
				"type": "success"
			});
		}
	};

	$("#message_details").on("click", "dd.to_decrypt", function() {
		$("#messages_decrypt_modal").modal("show");
	});

	NRS.forms.decryptMessages = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));
		var success = false;
		try {
			var messagesToDecrypt = [];
			for (var otherUser in _messages) {
				if (!_messages.hasOwnProperty(otherUser)) {
					continue;
				}
				for (var key in _messages[otherUser]) {
					if (!_messages[otherUser].hasOwnProperty(key)) {
						continue;
					}
					var message = _messages[otherUser][key];
					if (message.attachment && message.attachment.encryptedMessage) {
						messagesToDecrypt.push(message);
					}
				}
			}
			success = NRS.decryptAllMessages(messagesToDecrypt, data.secretPhrase, data.sharedKey);
		} catch (err) {
			if (err.errorCode && err.errorCode <= 2) {
				return {
					"error": err.message.escapeHTML()
				};
			} else {
				return {
					"error": $.t("error_messages_decrypt")
				};
			}
		}

		if (data.rememberPassword) {
			NRS.setDecryptionPassword(data.secretPhrase);
		}
		$("#messages_sidebar").find("a.active").trigger("click");
		if (success) {
			$.growl($.t("success_messages_decrypt"), {
				"type": "success"
			});
		} else {
			$.growl($.t("error_messages_decrypt"), {
				"type": "danger"
			});
		}
		return {
			"stop": true
		};
	};

    $("#shared_key_modal").on("show.bs.modal", function(e) {
        var $invoker = $(e.relatedTarget);
        $("#shared_key_text").val($invoker.data("sharedkey"));
        $("#shared_key_transaction").html(NRS.getTransactionLink($invoker.data("transaction")));
    });

	return NRS;
}(NRS || {}, jQuery));