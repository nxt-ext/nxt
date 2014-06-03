var NRS = (function(NRS, $, undefined) {
	$("#transactions_table, #dashboard_transactions_table").on("click", "a[data-transaction]", function(e) {
		e.preventDefault();

		var transactionId = $(this).data("transaction");

		NRS.showTransactionModal(transactionId);
	});

	NRS.showTransactionModal = function(transaction) {
		if (NRS.fetchingModalData) {
			return;
		}

		NRS.fetchingModalData = true;

		$("#transaction_info_output").html("").hide();
		$("#transaction_info_callout").hide();
		$("#transaction_info_table").hide();
		$("#transaction_info_table tbody").empty();

		if (typeof transaction != "object") {
			NRS.sendRequest("getTransaction", {
				"transaction": transaction
			}, function(response, input) {
				response.transaction = input.transaction;
				NRS.processTransactionModalData(response);
			});
		} else {
			NRS.processTransactionModalData(transaction);
		}
	}

	NRS.processTransactionModalData = function(transaction) {
		var async = false;

		var transactionDetails = $.extend({}, transaction);
		delete transactionDetails.attachment;
		if (transactionDetails.referencedTransaction == "0") {
			delete transactionDetails.referencedTransaction;
		}
		delete transactionDetails.transaction;

		$("#transaction_info_modal_transaction").html(String(transaction.transaction).escapeHTML());

		$("#transaction_info_tab_link").tab("show");

		$("#transaction_info_details_table tbody").empty().append(NRS.createInfoTable(transactionDetails, true))
		$("#transaction_info_table tbody").empty();

		var incorrect = false;

		if (transaction.type == 0) {
			switch (transaction.subtype) {
				case 0:
					var data = {
						"Type": "Ordinary Payment",
						"Amount": transaction.amountNQT,
						"Fee": transaction.feeNQT,
						"Recipient": NRS.getAccountTitle(transaction, "recipient"),
						"Sender": NRS.getAccountTitle(transaction, "sender")
					};

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				default:
					incorrect = true;
					break;
			}
		}
		if (transaction.type == 1) {
			switch (transaction.subtype) {
				case 0:
					try {
						var message = converters.hexStringToString(transaction.attachment.message);
					} catch (err) {
						var message = "Could not convert hex to string: " + String(transaction.attachment.message);
					}

					var sender_info = "";

					if (transaction.sender == NRS.account || transaction.recipient == NRS.account) {
						if (transaction.sender == NRS.account) {
							sender_info = "<strong>To</strong>: " + NRS.getAccountLink(transaction, "recipient");

						} else {
							sender_info = "<strong>From</strong>: " + NRS.getAccountLink(transaction, "sender");
						}
					} else {
						sender_info = "<strong>To</strong>: " + NRS.getAccountLink(transaction, "recipient") + "<br />";
						sender_info += "<strong>From</strong>: " + NRS.getAccountLink(transaction, "sender");
					}

					$("#transaction_info_output").html("<div style='color:#999999;padding-bottom:10px'><i class='fa fa-unlock'></i> Public Message</div><div style='padding-bottom:10px'>" + message.escapeHTML().nl2br() + "</div>" + sender_info).show();

					break;
				case 1:
					var data = {
						"Type": "Alias Assignment",
						"Alias": transaction.attachment.alias,
						"DataFormattedHTML": transaction.attachment.uri.autoLink()
					};

					if (transaction.sender != NRS.account) {
						data["Sender"] = NRS.getAccountTitle(transaction, "sender");
					}

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 2:
					var data = {
						"Type": "Poll Creation",
						"Name": transaction.attachment.name,
						"Description": transaction.attachment.description
					};

					if (transaction.sender != NRS.account) {
						data["Sender"] = NRS.getAccountTitle(transaction, "sender");
					}

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 3:
					var data = {
						"Type": "Vote Casting"
					};

					if (transaction.sender != NRS.account) {
						data["Sender"] = NRS.getAccountTitle(transaction, "sender");
					}

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 4:
					var data = {
						"Type": "Hub Announcement"
					};

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 5:
					var data = {
						"Type": "Account Info",
						"Name": transaction.attachment.name,
						"Description": transaction.attachment.description
					};

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 6:
					if (transaction.attachment.priceNQT == "0") {
						if (transaction.sender == transaction.recipient) {
							var type = "Alias Sale Cancellation";
						} else {
							var type = "Alias Transfer";
						}
					} else {
						var type = "Alias Sale";
					}

					var data = {
						"Type": type,
						"Alias Name": transaction.attachment.alias
					}

					if (type == "Alias Sale") {
						data["Price"] = transaction.attachment.priceNQT
					}

					if (type != "Alias Sale Cancellation") {
						data["Recipient"] = NRS.getAccountTitle(transaction, "recipient");
					}

					data["Sender"] = NRS.getAccountTitle(transaction, "sender");

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 7:
					var data = {
						"Type": "Alias Buy",
						"Alias Name": transaction.attachment.alias,
						"Recipient": NRS.getAccountTitle(transaction, "recipient"),
						"Sender": NRS.getAccountTitle(transaction, "sender")
					}

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 8:
					var data = {
						"Type": "Encrypted Message"
					}

					var showDecryptionForm = false;

					if (transaction.recipient == NRS.account || transaction.sender == NRS.account) {
						try {
							var message = NRS.decryptMessage(transaction.attachment.message, {
								"nonce": transaction.attachment.nonce,
								"accountId": (transaction.recipient == NRS.account ? transaction.sender : transaction.recipient)
							});
						} catch (err) {
							var message = String(err.message ? err.message : err);
							if (err.errorCode && err.errorCode == 1) {
								showDecryptionForm = true;
							}
						}

						if (transaction.sender == NRS.account) {
							sender_info = "<strong>To</strong>: " + NRS.getAccountLink(transaction, "recipient");
						} else {
							sender_info = "<strong>From</strong>: " + NRS.getAccountLink(transaction, "sender");
						}
					} else {
						var message = "This is an encrypted message not addressed to you. You cannot read it's contents.";

						sender_info = "<strong>To</strong>: " + NRS.getAccountLink(transaction, "recipient") + "<br />";
						sender_info += "<strong>From</strong>: " + NRS.getAccountLink(transaction, "sender");
					}

					var output = "<div style='color:#999999;padding-bottom:10px'><i class='fa fa-lock'></i> Encrypted Message</div>";

					if (showDecryptionForm) {
						output += "<div id='transaction_info_decryption_form'></div><div id='transaction_info_decrypted_note' style='padding-bottom:10px;display:none'></div>";
					} else {
						output += "<div style='padding-bottom:10px'>" + message.escapeHTML().nl2br() + "</div>"
					}

					output += sender_info;

					$("#transaction_info_output").html(output);


					if (showDecryptionForm) {
						$("#decrypt_note_form_container input[name=otherAccount]").val(transaction.recipient == NRS.account ? transaction.sender : transaction.recipient);
						$("#decrypt_note_form_container input[name=encryptedNote]").val(transaction.attachment.message);
						$("#decrypt_note_form_container input[name=encryptedNoteNonce]").val(transaction.attachment.nonce);
						$("#decrypt_note_form_container").detach().appendTo("#transaction_info_decryption_form");
						$("#decrypt_note_form_container").show();
					}

					$("#transaction_info_output").show();

					break;
				default:
					incorrect = true;
					break;
			}
		} else if (transaction.type == 2) {
			switch (transaction.subtype) {
				case 0:
					var data = {
						"Type": "Asset Issuance",
						"Name": transaction.attachment.name,
						"Quantity": [transaction.attachment.quantityQNT, transaction.attachment.decimals],
						"Decimals": transaction.attachment.decimals,
						"Description": transaction.attachment.description
					};

					if (transaction.sender != NRS.account) {
						data["Sender"] = NRS.getAccountTitle(transaction, "sender");
					}

					$("#transaction_info_callout").html("<a href='#' data-goto-asset='" + String(transaction.transaction).escapeHTML() + "'>Click here</a> to view this asset in the Asset Exchange.").show();

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 1:
					async = true;

					NRS.sendRequest("getAsset", {
						"asset": transaction.attachment.asset
					}, function(asset, input) {
						var data = {
							"Type": "Asset Transfer",
							"Asset Name": asset.name,
							"Quantity": [transaction.attachment.quantityQNT, asset.decimals],
							"Comment": transaction.attachment.comment
						};

						data["Sender"] = NRS.getAccountTitle(transaction, "sender");
						data["Recipient"] = NRS.getAccountTitle(transaction, "recipient");

						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();

						$("#transaction_info_modal").modal("show");
						NRS.fetchingModalData = false;
					});

					break;
				case 2:
					async = true;

					NRS.sendRequest("getAsset", {
						"asset": transaction.attachment.asset
					}, function(asset, input) {
						var data = {
							"Type": "Ask Order Placement",
							"Asset Name": asset.name,
							"Quantity": [transaction.attachment.quantityQNT, asset.decimals],
							"PriceFormattedHTML": NRS.formatOrderPricePerWholeQNT(transaction.attachment.priceNQT, asset.decimals) + " NXT",
							"TotalFormattedHTML": NRS.formatAmount(NRS.calculateOrderTotalNQT(transaction.attachment.quantityQNT, transaction.attachment.priceNQT)) + " NXT"
						};

						if (transaction.sender != NRS.account) {
							data["Sender"] = NRS.getAccountTitle(transaction, "sender");
						}

						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();

						$("#transaction_info_modal").modal("show");
						NRS.fetchingModalData = false;
					});

					break;
				case 3:
					async = true;

					NRS.sendRequest("getAsset", {
						"asset": transaction.attachment.asset
					}, function(asset, input) {
						var data = {
							"Type": "Bid Order Placement",
							"Asset Name": asset.name,
							"Quantity": [transaction.attachment.quantityQNT, asset.decimals],
							"PriceFormattedHTML": NRS.formatOrderPricePerWholeQNT(transaction.attachment.priceNQT, asset.decimals) + " NXT",
							"TotalFormattedHTML": NRS.formatAmount(NRS.calculateOrderTotalNQT(transaction.attachment.quantityQNT, transaction.attachment.priceNQT)) + " NXT"
						};

						if (transaction.sender != NRS.account) {
							data["Sender"] = NRS.getAccountTitle(transaction, "sender");
						}

						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();

						$("#transaction_info_modal").modal("show");
						NRS.fetchingModalData = false;
					});

					break;
				case 4:
					async = true;

					NRS.sendRequest("getTransaction", {
						"transaction": transaction.attachment.order
					}, function(transaction, input) {
						if (transaction.attachment.asset) {
							NRS.sendRequest("getAsset", {
								"asset": transaction.attachment.asset
							}, function(asset) {
								var data = {
									"Type": "Ask Order Cancellation",
									"Asset Name": asset.name,
									"Quantity": [transaction.attachment.quantityQNT, asset.decimals],
									"PriceFormattedHTML": NRS.formatOrderPricePerWholeQNT(transaction.attachment.priceNQT, asset.decimals) + " NXT",
									"TotalFormattedHTML": NRS.formatAmount(NRS.calculateOrderTotalNQT(transaction.attachment.quantityQNT, transaction.attachment.priceNQT)) + " NXT"
								};

								if (transaction.sender != NRS.account) {
									data["Sender"] = NRS.getAccountTitle(transaction, "sender");
								}

								$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
								$("#transaction_info_table").show();

								$("#transaction_info_modal").modal("show");
								NRS.fetchingModalData = false;
							});
						} else {
							NRS.fetchingModalData = false;
						}
					});

					break;
				case 5:
					async = true;

					NRS.sendRequest("getTransaction", {
						"transaction": transaction.attachment.order
					}, function(transaction) {
						if (transaction.attachment.asset) {
							NRS.sendRequest("getAsset", {
								"asset": transaction.attachment.asset
							}, function(asset) {
								var data = {
									"Type": "Bid Order Cancellation",
									"Asset Name": asset.name,
									"Quantity": [transaction.attachment.quantityQNT, asset.decimals],
									"PriceFormattedHTML": NRS.formatOrderPricePerWholeQNT(transaction.attachment.priceNQT, asset.decimals) + " NXT",
									"TotalFormattedHTML": NRS.formatAmount(NRS.calculateOrderTotalNQT(transaction.attachment.quantityQNT, transaction.attachment.priceNQT)) + " NXT"
								};

								if (transaction.sender != NRS.account) {
									data["Sender"] = NRS.getAccountTitle(transaction, "sender");
								}

								$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
								$("#transaction_info_table").show();

								$("#transaction_info_modal").modal("show");
								NRS.fetchingModalData = false;
							});
						} else {
							NRS.fetchingModalData = false;
						}
					});

					break;
				default:
					incorrect = true;
					break;
			}
		} else if (transaction.type == 3) {
			switch (transaction.subtype) {
				case 0:
					var data = {
						"Type": "Marketplace Listing",
						"Name": transaction.attachment.name,
						"Description": transaction.attachment.description,
						"Price": transaction.attachment.priceNQT,
						"quantityFormattedHTML": NRS.format(transaction.attachment.quantity),
						"Seller": NRS.getAccountFormatted(transaction, "sender")
					};

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 1:
					async = true;

					NRS.sendRequest("getDGSGood", {
						"goods": transaction.attachment.goods
					}, function(goods) {
						var data = {
							"Type": "Marketplace Removal",
							"Item Name": goods.name,
							"Seller": NRS.getAccountFormatted(goods, "seller")
						};

						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();

						$("#transaction_info_modal").modal("show");
						NRS.fetchingModalData = false;
					});

					break;
				case 2:
					async = true;

					NRS.sendRequest("getDGSGood", {
						"goods": transaction.attachment.goods
					}, function(goods) {
						var data = {
							"Type": "Marketplace Price Change",
							"Item Name": goods.name,
							"New PriceFormattedHTML": NRS.formatAmount(transaction.attachment.priceNQT) + " NXT",
							"Seller": NRS.getAccountFormatted(goods, "seller")
						};

						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();

						$("#transaction_info_modal").modal("show");
						NRS.fetchingModalData = false;
					});

					break;
				case 3:
					async = true;

					NRS.sendRequest("getDGSGood", {
						"goods": transaction.attachment.goods
					}, function(goods) {
						var data = {
							"Type": "Marketplace Quantity Change",
							"Item Name": goods.name,
							"Delta Quantity": transaction.attachment.deltaQuantity,
							"Seller": NRS.getAccountFormatted(goods, "seller")
						};

						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();

						$("#transaction_info_modal").modal("show");
						NRS.fetchingModalData = false;
					});

					break;
				case 4:
					var data = {
						"Type": "Marketplace Purchase"
					};
					break;
				case 5:
					var data = {
						"Type": "Marketplace Delivery"
					};
					break;
				case 6:
					var data = {
						"Type": "Marketplace Feedback"
					};
					break;
				case 7:
					var data = {
						"Type": "Marketplace Refund"
					};
					break;
				default:
					incorrect = true;
					break
			}
		} else if (transaction.type == 4) {
			switch (transaction.subtype) {
				case 0:
					var data = {
						"Type": "Balance Leasing",
						"Period": transaction.attachment.period
					};

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;

				default:
					incorrect = true;
					break;
			}
		}

		if (incorrect) {
			$.growl("Invalid or unknown transaction type.", {
				"type": "danger"
			});

			NRS.fetchingModalData = false;
			return;
		}

		if (!async) {
			$("#transaction_info_modal").modal("show");
			NRS.fetchingModalData = false;
		}
	}

	$("#transaction_info_modal").on("hide.bs.modal", function(e) {
		if ($("#decrypt_note_form_container").length) {
			$("#decrypt_note_form_container input").val("");
			$("#decrypt_note_form_container").hide().detach().appendTo("body");
			$("#transaction_info_decrypted_note").html("").hide();
		}
	});

	$("#decrypt_note_form_container").on("submit", function(e) {
		e.preventDefault();

		var message = $(this).find("input[name=encryptedNote]").val();
		var nonce = $(this).find("input[name=encryptedNoteNonce]").val();
		var otherAccount = $(this).find("input[name=otherAccount]").val();
		var password = $(this).find("input[name=secretPhrase]").val();
		var rememberPassword = $(this).find("input[name=rememberPassword]").is(":checked");

		try {
			var message = NRS.decryptMessage(message, {
				"nonce": nonce,
				"accountId": otherAccount
			}, password);
			$(this).hide();
			$("#transaction_info_decrypted_note").html(message.escapeHTML().nl2br()).show();
		} catch (err) {
			var message = String(err.message ? err.message : err);
			$(this).find(".callout").html(message.escapeHTML());
		}

		if (rememberPassword) {
			NRS.decryptionPassword = password;
		}
	});

	return NRS;
}(NRS || {}, jQuery));