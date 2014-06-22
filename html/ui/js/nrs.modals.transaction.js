var NRS = (function(NRS, $, undefined) {
	NRS.decryptedTransactions = {};
	NRS.encryptedNote = null;

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

		$("#transaction_info_output_top, #transaction_info_output_bottom").html("").hide();
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

					$("#transaction_info_output_top").html("<div style='color:#999999;padding-bottom:10px'><i class='fa fa-unlock'></i> Public Message</div><div style='padding-bottom:10px'>" + message.escapeHTML().nl2br() + "</div>" + sender_info).show();

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

					$("#transaction_info_output_top").html("<div style='color:#999999;padding-bottom:10px'><i class='fa fa-lock'></i> Encrypted Message</div><div id='transaction_info_decryption_form'></div><div id='transaction_info_decrypted_note' style='display:none;padding-bottom:10px;'></div>");

					var output = "";

					if (NRS.account == transaction.recipient || NRS.account == transaction.sender) {
						NRS.tryToDecrypt(transaction, {
							"message": {
								"title": "",
								"nonce": "nonce"
							},
						}, (transaction.recipient == NRS.account ? transaction.sender : transaction.recipient), {
							"noPadding": true,
							"formEl": "#transaction_info_decryption_form",
							"outputEl": "#transaction_info_decrypted_note"
						});

						if (transaction.sender == NRS.account) {
							output = "<strong>To</strong>: " + NRS.getAccountLink(transaction, "recipient");
						} else {
							output = "<strong>From</strong>: " + NRS.getAccountLink(transaction, "sender");
						}
					} else {
						output = "<div style='padding-bottom:10px'>This is an encrypted message not addressed to you. You cannot read it's contents.</div>";
						output = "<strong>To</strong>: " + NRS.getAccountLink(transaction, "recipient") + "<br />";
						output = "<strong>From</strong>: " + NRS.getAccountLink(transaction, "sender");
					}

					$("#transaction_info_output_top").append(output).show();

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
					async = true;

					NRS.sendRequest("getDGSGood", {
						"goods": transaction.attachment.goods
					}, function(goods) {
						var data = {
							"Type": "Marketplace Purchase",
							"Item Name": goods.name,
							"Price": transaction.attachment.priceNQT,
							"quantityFormattedHTML": NRS.format(transaction.attachment.quantity),
							"Buyer": NRS.getAccountFormatted(transaction, "sender"),
							"Seller": NRS.getAccountFormatted(goods, "seller")
						};

						if (transaction.attachment.note) {
							if (NRS.account == goods.seller || NRS.account == transaction.sender) {
								NRS.tryToDecrypt(transaction, {
									"note": "Note"
								}, (transaction.sender == NRS.account ? goods.seller : transaction.sender));
							} else {
								data["Note"] = "Note is encrypted and cannot be read by you.";
							}
						}

						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();

						$("#transaction_info_modal").modal("show");
						NRS.fetchingModalData = false;
					});

					break;
				case 5:
					async = true;

					NRS.sendRequest("getDGSPurchase", {
						"purchase": transaction.attachment.purchase
					}, function(purchase) {
						NRS.sendRequest("getDGSGood", {
							"goods": purchase.goods
						}, function(goods) {
							var data = {
								"Type": "Marketplace Delivery",
								"Item Name": goods.name,
								"Price": purchase.priceNQT,
								"quantityFormattedHTML": NRS.format(purchase.quantity),
								"Buyer": NRS.getAccountFormatted(purchase, "buyer"),
								"Seller": NRS.getAccountFormatted(purchase, "seller")
							};

							if (transaction.attachment.goodsData) {
								if (NRS.account == purchase.seller || NRS.account == purchase.buyer) {
									NRS.tryToDecrypt(transaction, {
										"goodsData": {
											"title": "Data",
											"nonce": "goodsNonce"
										},
									}, (purchase.buyer == NRS.account ? purchase.seller : purchase.buyer));
								} else {
									data["Data"] = "Goods data is encrypted and cannot be read by you.";
								}
							}

							$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
							$("#transaction_info_table").show();

							$("#transaction_info_modal").modal("show");
							NRS.fetchingModalData = false;
						});
					});

					break;
				case 6:
					async = true;

					NRS.sendRequest("getDGSPurchase", {
						"purchase": transaction.attachment.purchase
					}, function(purchase) {
						NRS.sendRequest("getDGSGood", {
							"goods": purchase.goods
						}, function(goods) {
							var data = {
								"Type": "Marketplace Feedback",
								"Item Name": goods.name,
								"Buyer": NRS.getAccountFormatted(purchase, "buyer"),
								"Seller": NRS.getAccountFormatted(purchase, "seller")
							};

							if (transaction.attachment.note) {
								if (NRS.account == purchase.seller || NRS.account == purchase.buyer) {
									NRS.tryToDecrypt(transaction, {
										"note": "Feedback"
									}, (purchase.buyer == NRS.account ? purchase.seller : purchase.buyer));
								} else {
									data["Feedback"] = "Feedback is encrypted and cannot be read by you.";
								}
							}

							$("#transaction_info_table tbody").append(NRS.createInfoTable(data));

							$("#transaction_info_table").show();

							$("#transaction_info_modal").modal("show");

							NRS.fetchingModalData = false;
						});
					});

					break;
				case 7:
					async = true;

					NRS.sendRequest("getDGSPurchase", {
						"purchase": transaction.attachment.purchase
					}, function(purchase) {
						NRS.sendRequest("getDGSGood", {
							"goods": purchase.goods
						}, function(goods) {
							var data = {
								"Type": "Marketplace Refund",
								"Item Name": goods.name,
								"Refund": transaction.attachment.refundNQT,
								"Buyer": NRS.getAccountFormatted(purchase, "buyer"),
								"Seller": NRS.getAccountFormatted(purchase, "seller")
							};

							if (transaction.attachment.note) {
								if (NRS.account == purchase.seller || NRS.account == purchase.buyer) {
									NRS.tryToDecrypt(transaction, {
										"note": "Note"
									}, (purchase.buyer == NRS.account ? purchase.seller : purchase.buyer));
								} else {
									data["Note"] = "Note is encrypted and cannot be read by you.";
								}
							}

							$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
							$("#transaction_info_table").show();

							$("#transaction_info_modal").modal("show");
							NRS.fetchingModalData = false;
						});
					});

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

	NRS.tryToDecrypt = function(transaction, fields, account, options) {
		var showDecryptionForm = false;

		if (!options) {
			options = {};
		}

		var nrFields = Object.keys(fields).length;

		var formEl = (options.formEl ? String(options.formEl).escapeHTML() : "#transaction_info_output_bottom");
		var outputEl = (options.outputEl ? String(options.outputEl).escapeHTML() : "#transaction_info_output_bottom");

		var output = "";

		//check in cache first..
		if (NRS.decryptedTransactions && NRS.decryptedTransactions[transaction.transaction]) {
			var decryptedTransaction = NRS.decryptedTransactions[transaction.transaction];

			$.each(fields, function(key, title) {
				if (typeof title != "string") {
					title = title.title;
				}

				if (key in decryptedTransaction) {
					output += "<div" + (!options.noPadding ? " style='padding-left:5px'" : "") + ">" + (title ? "<label" + (nrFields > 1 ? " style='margin-top:5px'" : "") + ">" + String(title).toUpperCase().escapeHTML() + "</label>" : "") + "<div>" + String(decryptedTransaction[key]).escapeHTML().nl2br() + "</div></div>";
				} else {
					//if a specific key was not found, the cache is outdated..
					output = "";
					delete NRS.decryptedTransactions[transaction.transaction];
					return false;
				}
			});
		}

		if (!output) {
			$.each(fields, function(key, title) {
				var data = "";

				var inAttachment = ("attachment" in transaction);

				var toDecrypt = (inAttachment ? transaction.attachment[key] : transaction[key]);

				if (toDecrypt) {
					if (typeof title != "string") {
						var nonce = (inAttachment ? transaction.attachment[title.nonce] : transaction[title.nonce]);
						title = title.title;
					} else {
						var nonce = (inAttachment ? transaction.attachment[key + "Nonce"] : transaction[key + "Nonce"]);
					}

					try {
						data = NRS.decryptNote(toDecrypt, {
							"nonce": nonce,
							"account": account
						});
					} catch (err) {
						var mesage = String(err.message ? err.message : err);
						if (err.errorCode && err.errorCode == 1) {
							showDecryptionForm = true;
							return false;
						} else {
							data = "Could not decrypt " + String(title).escapeHTML().toLowerCase() + ".";
						}
					}

					output += "<div" + (!options.noPadding ? " style='padding-left:5px'" : "") + ">" + (title ? "<label" + (nrFields > 1 ? " style='margin-top:5px'" : "") + ">" + String(title).toUpperCase().escapeHTML() + "</label>" : "") + "<div>" + String(data).escapeHTML().nl2br() + "</div></div>";
				}
			});
		}

		if (showDecryptionForm) {
			var identifier = (options.identifier ? transaction[identifier] : transaction.transaction);

			NRS.encryptedNote = {
				"transaction": transaction,
				"fields": fields,
				"account": account,
				"options": options,
				"identifier": identifier
			};

			//	$("#transaction_info_output_bottom").html("<div id='transaction_info_decryption_form'></div><div id='transaction_info_decrypted_note' style='padding-bottom:10px;display:none'></div>");

			$("#decrypt_note_form_container").detach().appendTo(formEl);

			$("#decrypt_note_form_container, " + formEl).show();
		} else {
			NRS.removeDecryptionForm();
			$(outputEl).html(output).show();
		}
	}

	NRS.removeDecryptionForm = function($modal) {
		if (($modal && $modal.find("#decrypt_note_form_container").length) || (!$modal && $("#decrypt_note_form_container").length)) {
			$("#decrypt_note_form_container input").val("");
			$("#decrypt_note_form_container").hide().detach().appendTo("body");
		}
	}

	$("#transaction_info_modal").on("hide.bs.modal", function(e) {
		NRS.removeDecryptionForm($(this));
		$("#transaction_info_output_bottom, #transaction_info_output_top").html("").hide();
	});

	$("#decrypt_note_form_container button.btn-primary").click(function() {
		$("#decrypt_note_form_container").trigger("submit");
	});

	$("#decrypt_note_form_container").on("submit", function(e) {
		e.preventDefault();

		var $form = $(this);

		if (!NRS.encryptedNote) {
			$form.find(".callout").html("Encrypted note not found.").show();
			return;
		}

		var password = $form.find("input[name=secretPhrase]").val();

		if (!password) {
			if (NRS.password) {
				password = NRS.password;
			} else if (NRS.decryptionPassword) {
				password = NRS.decryptionPassword;
			} else {
				$form.find(".callout").html("Secret phrase is a required field.").show();
				return;
			}
		}

		var accountId = NRS.generateAccountId(password);
		if (accountId != NRS.account) {
			$form.find(".callout").html("Incorrect secret phrase.").show();
			return;
		}

		var rememberPassword = $form.find("input[name=rememberPassword]").is(":checked");

		var otherAccount = NRS.encryptedNote.account;

		var output = "";
		var decryptionError = false;
		var decryptedFields = {};

		var inAttachment = ("attachment" in NRS.encryptedNote.transaction);

		var nrFields = Object.keys(NRS.encryptedNote.fields).length;

		$.each(NRS.encryptedNote.fields, function(key, title) {
			var note = (inAttachment ? NRS.encryptedNote.transaction.attachment[key] : NRS.encryptedNote.transaction[key]);

			if (typeof title != "string") {
				var noteNonce = (inAttachment ? NRS.encryptedNote.transaction.attachment[title.nonce] : NRS.encryptedNote.transaction[title.nonce]);
				title = title.title;
			} else {
				var noteNonce = (inAttachment ? NRS.encryptedNote.transaction.attachment[key + "Nonce"] : NRS.encryptedNote.transaction[key + "Nonce"]);
			}

			try {
				var note = NRS.decryptNote(note, {
					"nonce": noteNonce,
					"account": otherAccount
				}, password);

				decryptedFields[key] = note;

				output += "<div" + (!NRS.encryptedNote.options.noPadding ? " style='padding-left:5px'" : "") + ">" + (title ? "<label" + (nrFields > 1 ? " style='margin-top:5px'" : "") + ">" + String(title).toUpperCase().escapeHTML() + "</label>" : "") + "<div>" + note.escapeHTML().nl2br() + "</div></div>";
			} catch (err) {
				decryptionError = true;
				var message = String(err.message ? err.message : err);

				$form.find(".callout").html(message.escapeHTML());
				return false;
			}
		});

		if (decryptionError) {
			return;
		}

		NRS.decryptedTransactions[NRS.encryptedNote.identifier] = decryptedFields;

		NRS.removeDecryptionForm();

		var outputEl = (NRS.encryptedNote.options.outputEl ? String(NRS.encryptedNote.options.outputEl).escapeHTML() : "#transaction_info_output_bottom");

		$(outputEl).html(output).show();

		NRS.encryptedNote = null;

		if (rememberPassword) {
			NRS.decryptionPassword = password;
		}
	});

	return NRS;
}(NRS || {}, jQuery));