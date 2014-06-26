/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
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
					var message;

					try {
						message = converters.hexStringToString(transaction.attachment.message);
					} catch (err) {
						if (transaction.attachment.message.indexOf("feff") === 0) {
							message = NRS.convertFromHex16(transaction.attachment.message);
						} else {
							message = NRS.convertFromHex8(transaction.attachment.message);
						}
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

					if (type == "Alias Sale") {
						var message = "";

						if (transaction.recipient == NRS.account) {
							message = "You have been offered this alias for " + NRS.formatAmount(transaction.attachment.priceNQT) + " NXT. <a href='#' data-alias='" + String(transaction.attachment.alias).escapeHTML() + "' data-toggle='modal' data-target='#buy_alias_modal'>Buy it?</a>";
						} else if (transaction.recipient == NRS.genesis) {
							message = "This alias is offered for sale for " + NRS.formatAmount(transaction.attachment.priceNQT) + " NXT. <a href='#' data-alias='" + String(transaction.attachment.alias).escapeHTML() + "' data-toggle='modal' data-target='#buy_alias_modal'>Buy it?</a>";
						} else if (transaction.senderRS == NRS.accountRS) {
							if (transaction.attachment.priceNQT != "0") {
								message = "You are offering this alias for sale. <a href='#' data-alias='" + String(transaction.attachment.alias).escapeHTML() + "' data-toggle='modal' data-target='#cancel_alias_sale_modal'>Cancel sale?</a>";
							}
						} else {
							message = "This alias is offered for sale to another account pending decision.";
						}

						if (message) {
							$("#transaction_info_output_bottom").html("<div class='callout callout-info' style='margin-top:15px;margin-bottom:0;'>" + message + "</div>").show();
						}
					}

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
							}
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
										}
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

	$("#transaction_info_modal").on("hide.bs.modal", function(e) {
		NRS.removeDecryptionForm($(this));
		$("#transaction_info_output_bottom, #transaction_info_output_top").html("").hide();
	});

	return NRS;
}(NRS || {}, jQuery));