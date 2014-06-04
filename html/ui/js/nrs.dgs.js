var NRS = (function(NRS, $, undefined) {
	NRS.pages.newest_dgs = function() {
		NRS.pageLoading();

		var content = "";

		NRS.sendRequest("getDGSGoods+", {
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		}, function(response) {
			if (response.goods && response.goods.length) {
				if (response.goods.length > NRS.itemsPerPage) {
					NRS.hasMorePages = true;
					response.goods.pop();
				}
				for (var i = 0; i < response.goods.length; i++) {
					var good = response.goods[i];

					content += NRS.getMarketplaceItemHTML(good);
				}

				$("#newest_dgs_page_contents").empty().append(content);
				NRS.dataLoadFinished($("#newest_dgs_page_contents"));

				NRS.pageLoaded();
			} else {
				$("#newest_dgs_page_contents").empty();
				NRS.dataLoadFinished($("#newest_dgs_page_contents"));

				NRS.pageLoaded();
			}
		});
	}

	NRS.getMarketplaceItemHTML = function(good) {
		return "<div style='float:right;color: #999999;background:white;padding:5px;border:1px solid #ccc;border-radius:3px'>" +
			"<strong>Seller</strong>: <span><a href='#' data-user='" + NRS.getAccountFormatted(good, "seller") + "' class='user_info'>" + NRS.getAccountTitle(good, "seller") + "</a></span><br>" +
			"<strong>Product Id</strong>: &nbsp;<a href='#''>" + String(good.goods).escapeHTML() + "</a>" +
			"</div>" +
			"<h3 class='title'><a href='#' data-goods='" + String(good.goods).escapeHTML() + "' data-toggle='modal' data-target='#dgs_purchase_modal'>" + String(good.name).escapeHTML() + "</a></h3>" +
			"<span class='price'><strong>" + NRS.formatAmount(good.priceNQT) + " NXT</strong></span>" +
			"<div class='description'>" + String(good.description).escapeHTML().nl2br() + "</div>" +
			"<span class='tags'><strong>Tags</strong>: " + String(good.tags).escapeHTML() + "</span><hr />";
	}

	NRS.getMarketplacePurchaseHTML = function(purchase) {
		return "<div style='float:right;color: #999999;background:white;padding:5px;border:1px solid #ccc;border-radius:3px'>" +
			"<strong>Seller</strong>: <span><a href='#' data-user='" + NRS.getAccountFormatted(purchase, "seller") + "' class='user_info'>" + NRS.getAccountTitle(purchase, "seller") + "</a></span><br>" +
			"<strong>Product Id</strong>: &nbsp;<a href='#''>" + String(purchase.goods.goods).escapeHTML() + "</a>" +
			"</div>" +
			"<h3 class='title'><a href='#' data-goods='" + String(purchase.goods.goods).escapeHTML() + "' data-toggle='modal' data-target='#dgs_purchase_modal'>" + String(purchase.goods.name).escapeHTML() + "</a></h3>" +
			"<table>" +
			"<tr><td><strong>Order Date</strong>:</td><td>" + NRS.formatTimestamp(purchase.timestamp) + "</td></tr>" +
			"<tr><td><strong>Order Status</strong>:</td><td>" + (purchase.pending ? "<span class='label label-warning'>Pending</span>" : "Complete") + "</td></tr>" +
			(purchase.pending ? "<tr><td><strong>Delivery Deadline</strong>:</td><td>" + NRS.formatTimestamp(new Date(purchase.deliveryDeadlineTimestamp * 1000)) + "</td></tr>" : "") +
			"<tr><td><strong>Price</strong>:</td><td>" + NRS.formatAmount(purchase.priceNQT) + " NXT</td></tr>" +
			"<tr><td><strong>Quantity</strong>:</td><td>" + NRS.format(purchase.quantity) + "</td></tr>" +
			"</table>" +
			"<hr />";
	}

	NRS.getMarketplacePendingPurchaseHTML = function(purchase) {
		//do not show if refund has been initiated or order has been delivered
		if (NRS.getUnconfirmedTransaction(3, [5, 7], {
			"purchase": purchase.purchase
		})) {
			return "";
		}

		return "<div data-purchase='" + String(purchase.purchase).escapeHTML() + "'><div style='float:right;color: #999999;background:white;padding:5px;border:1px solid #ccc;border-radius:3px'>" +
			"<strong>Buyer</strong>: <span><a href='#' data-user='" + NRS.getAccountFormatted(purchase, "buyer") + "' class='user_info'>" + NRS.getAccountTitle(purchase, "buyer") + "</a></span><br>" +
			"<strong>Product Id</strong>: &nbsp;<a href='#''>" + String(purchase.goods.goods).escapeHTML() + "</a>" +
			"</div>" +
			"<h3 class='title'><a href='#' data-goods='" + String(purchase.goods.goods).escapeHTML() + "' data-toggle='modal' data-target='#dgs_purchase_modal'>" + String(purchase.goods.name).escapeHTML() + "</a></h3>" +
			"<table class='purchase' style='margin-bottom:5px'>" +
			"<tr><td><strong>Order Date</strong>:</td><td>" + NRS.formatTimestamp(purchase.timestamp) + "</td></tr>" +
			"<tr><td><strong>Delivery Deadline</strong>:</td><td>" + NRS.formatTimestamp(new Date(purchase.deliveryDeadlineTimestamp * 1000)) + "</td></tr>" +
			"<tr><td><strong>Price</strong>:</td><td>" + NRS.formatAmount(purchase.priceNQT) + " NXT</td></tr>" +
			"<tr><td><strong>Quantity</strong>:</td><td>" + NRS.format(purchase.quantity) + "</td></tr>" +
			(purchase.note ? "<tr><td><strong>Note</strong>:</td><td>" + String(purchase.note).escapeHTML().nl2br() + "</td></tr>" : "") +
			"</table>" +
			"<button type='button' class='btn btn-default btn-refund' data-toggle='modal' data-target='#dgs_refund_modal' data-purchase='" + String(purchase.purchase).escapeHTML() + "'>Refund</button> " +
			"<button type='button' class='btn btn-default btn-deliver' data-toggle='modal' data-target='#dgs_delivery_modal' data-purchase='" + String(purchase.purchase).escapeHTML() + "'>Deliver Goods</button>" +
			"<hr /></div>";
	}

	NRS.pages.purchased_dgs = function() {
		NRS.pageLoading();

		var goods = {};

		NRS.sendRequest("getDGSPurchases", {
			"buyer": NRS.account,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		}, function(response) {
			if (response.purchases && response.purchases.length) {
				if (response.purchases.length > NRS.itemsPerPage) {
					NRS.hasMorePages = true;
					response.purchases.pop();
				}

				var nr_goods = 0;

				for (var i = 0; i < response.purchases.length; i++) {
					NRS.sendRequest("getDGSGood", {
						"goods": response.purchases[i].goods
					}, function(good) {
						goods[good.goods] = good;
						nr_goods++;

						if (nr_goods == response.purchases.length) {
							var content = "";

							for (var j = 0; j < response.purchases.length; j++) {
								var purchase = response.purchases[j];
								purchase.goods = goods[purchase.goods];

								content += NRS.getMarketplacePurchaseHTML(purchase);
							}

							$("#purchased_dgs_page_contents").empty().append(content);
							NRS.dataLoadFinished($("#purchased_dgs_page_contents"));

							NRS.pageLoaded();
						}
					});
				}
			} else {
				$("#purchased_dgs_page_contents").empty();
				NRS.dataLoadFinished($("#purchased_dgs_page_contents"));

				NRS.pageLoaded();
			}
		});
	}

	NRS.pages.pending_purchases_dgs = function() {
		NRS.pageLoading();

		var goods = {};

		NRS.sendRequest("getDGSPendingPurchases", {
			"seller": NRS.account,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		}, function(response) {
			if (response.purchases && response.purchases.length) {
				if (response.purchases.length > NRS.itemsPerPage) {
					NRS.hasMorePages = true;
					response.purchases.pop();
				}

				var nr_goods = 0;

				for (var i = 0; i < response.purchases.length; i++) {
					NRS.sendRequest("getDGSGood", {
						"goods": response.purchases[i].goods
					}, function(good) {
						goods[good.goods] = good;
						nr_goods++;

						if (nr_goods == response.purchases.length) {
							var content = "";

							for (var j = 0; j < response.purchases.length; j++) {
								var purchase = response.purchases[j];
								purchase.goods = goods[purchase.goods];

								if (purchase.note) {
									try {
										purchase.note = NRS.decryptNote(purchase.note, {
											"nonce": purchase.noteNonce,
											"account": purchase.buyer
										});
									} catch (err) {
										purchase.note = String(err.message);
									}
								}

								content += NRS.getMarketplacePendingPurchaseHTML(purchase);
							}

							$("#pending_purchases_dgs_page_contents").empty().append(content);
							NRS.dataLoadFinished($("#pending_purchases_dgs_page_contents"));

							NRS.pageLoaded();
						}
					});
				}
			} else {
				$("#pending_purchases_dgs_page_contents").empty();
				NRS.dataLoadFinished($("#pending_purchases_dgs_page_contents"));

				NRS.pageLoaded();
			}
		});
	}

	NRS.pages.my_dgs_listings = function() {
		NRS.pageLoading();

		var rows = "";

		if (NRS.unconfirmedTransactions.length) {
			for (var j = 0; j < NRS.unconfirmedTransactions.length; j++) {
				var unconfirmedTransaction = NRS.unconfirmedTransactions[j];

				if (unconfirmedTransaction.type != 3 || unconfirmedTransaction.subtype != 0) {
					continue;
				}

				rows += "<tr class='tentative' data-goods='" + String(unconfirmedTransaction.goods).escapeHTML() + "'><td><a href='#' data-toggle='modal' data-target='#dgs_listing_modal' data-goods='" + String(unconfirmedTransaction.goods).escapeHTML() + "'>" + String(unconfirmedTransaction.name).escapeHTML() + "</a></td><td class='quantity'>" + NRS.format(unconfirmedTransaction.quantity) + "</td><td class='price'>" + NRS.formatAmount(unconfirmedTransaction.priceNQT) + " NXT</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_price_change_modal' data-goods='" + String(unconfirmedTransaction.goods).escapeHTML() + "'>Change Price</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_quantity_change_modal' data-goods='" + String(unconfirmedTransaction.goods).escapeHTML() + "'>Change QTY</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_delisting_modal' data-goods='" + String(unconfirmedTransaction.goods).escapeHTML() + "'>Delete</a></td></tr>";
			}
		}

		NRS.sendRequest("getDGSGoods+", {
			"seller": NRS.account,
			"firstIndex": 0,
			"lastIndex": 0
		}, function(response) {
			if (response.goods && response.goods.length) {
				for (var i = 0; i < response.goods.length; i++) {
					var good = response.goods[i];

					var deleted = false;
					var tentative = false;
					var quantityFormatted = false;

					if (NRS.unconfirmedTransactions.length) {
						for (var j = 0; j < NRS.unconfirmedTransactions.length; j++) {
							var unconfirmedTransaction = NRS.unconfirmedTransactions[j];

							if (unconfirmedTransaction.type == 3 && unconfirmedTransaction.goods == good.goods) {
								if (unconfirmedTransaction.subtype == 1) { //delisting
									deleted = tentative = true;
								} else if (unconfirmedTransaction.subtype == 2) { //price change
									good.priceNQT = unconfirmedTransaction.priceNQT;
									tentative = true;
								} else if (unconfirmedTransaction.subtype == 3) { //quantity change
									good.quantity = NRS.format(good.quantity) + " " + NRS.format(unconfirmedTransaction.deltaQuantity);
									tentative = true;
									quantityFormatted = true;
								}
							}
						}
					}

					rows += "<tr class='" + (tentative ? "tentative" : "") + (deleted ? " tentative-crossed" : "") + "' data-goods='" + String(good.goods).escapeHTML() + "'><td><a href='#' data-toggle='modal' data-target='#dgs_listing_modal' data-goods='" + String(good.goods).escapeHTML() + "'>" + String(good.name).escapeHTML() + "</a></td><td class='quantity'>" + (quantityFormatted ? good.quantity : NRS.format(good.quantity)) + "</td><td class='price'>" + NRS.formatAmount(good.priceNQT) + " NXT</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_price_change_modal' data-goods='" + String(good.goods).escapeHTML() + "'>Change Price</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_quantity_change_modal' data-goods='" + String(good.goods).escapeHTML() + "'>Change QTY</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_delisting_modal' data-goods='" + String(good.goods).escapeHTML() + "'>Delete</a></td></tr>";
				}

				$("#my_dgs_listings_table tbody").empty().append(rows);
				NRS.dataLoadFinished($("#my_dgs_listings_table"));

				NRS.pageLoaded();
			} else {
				$("#my_dgs_listings_table tbody").empty();
				NRS.dataLoadFinished($("#my_dgs_listings_table"));

				NRS.pageLoaded();
			}
		});
	}

	NRS.incoming.my_dgs_listings = function(transactions) {
		if (transactions || NRS.unconfirmedTransactionsChange || NRS.state.isScanning) {
			NRS.pages.my_dgs_listings();
		}
	}

	NRS.forms.dgsListing = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		if (!data.description) {
			return {
				"error": "Description is a required field."
			};
		}

		$.each(data, function(key, value) {
			data[key] = $.trim(value);
		});

		if (!data.description) {
			return {
				"error": "Description is a required field."
			};
		}

		if (data.tags) {
			data.tags = data.tags.toLowerCase();

			var tags = data.tags.split(",");

			if (tags.length > 3) {
				return {
					"error": "A maximum of 3 tags is allowed."
				};
			} else {
				var clean_tags = [];

				for (var i = 0; i < tags.length; i++) {
					var tag = $.trim(tags[i]);

					if (tag.length < 3 || tag.length > 20) {
						return {
							"error": "Incorrect \"tag\" (length must be in [3..20] range)"
						};
					} else if (!tag.match(/^[a-z]+$/i)) {
						return {
							"error": "Incorrect \"tag\" (must contain only alphabetic characters)"
						};
					} else if (clean_tags.indexOf(tag) > -1) {
						return {
							"error": "The same tag was inserted multiple times."
						};
					} else {
						clean_tags.push(tag);
					}
				}

				data.tags = clean_tags.join(",")
			}
		}

		return {
			"data": data
		};
	}

	NRS.forms.dgsListingComplete = function(response, data) {
		if (response.alreadyProcessed) {
			return;
		}

		if (NRS.currentPage == "my_dgs_listings") {
			var $table = $("#my_dgs_listings_table tbody");

			var rowToAdd = "<tr class='tentative' data-goods='" + String(response.transaction).escapeHTML() + "'><td><a href='#' data-toggle='modal' data-target='#dgs_listing_modal' data-goods='" + String(response.transaction).escapeHTML() + "'>" + String(data.name).escapeHTML() + "</a></td><td>" + String(data.tags).escapeHTML() + "</td><td class='quantity'>" + NRS.format(data.quantity) + "</td><td class='price'>" + NRS.formatAmount(data.priceNQT) + " NXT</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_price_change_modal' data-goods='" + String(response.transaction).escapeHTML() + "'>Change Price</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_quantity_change_modal' data-goods='" + String(response.transaction).escapeHTML() + "'>Change QTY</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_delisting_modal' data-goods='" + String(response.transaction).escapeHTML() + "'>Delete</a></td></tr>";

			$table.prepend(rowToAdd);

			if ($("#my_dgs_listings_table").parent().hasClass("data-empty")) {
				$("#my_dgs_listings_table").parent().removeClass("data-empty");
			}
		}
	}

	NRS.forms.dgsDelistingComplete = function(response, data) {
		if (response.alreadyProcessed) {
			return;
		}
		$("#my_dgs_listings_table tr[data-goods=" + String(data.goods).escapeHTML() + "]").addClass("tentative tentative-crossed");
	}

	/*
	NRS.forms.dgsFeedback = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		if (data.note) {
			var encrypted = nxtCrypto.encryptData(data.note);

			data.encryptedNoteNonce = encrypted.nonce;
			data.encryptedNote = encrypted.data;

			delete data.note;
		}

		return {
			"data": data
		};
	}

	NRS.forms.dgsPurchase = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		if (data.note) {
			var encrypted = nxtCrypto.encryptData(data.note);

			data.encryptedNoteNonce = encrypted.nonce;
			data.encryptedNote = encrypted.data;

			delete data.note;
		}

		return {
			"data": data
		};
	}*/

	NRS.forms.dgsRefund = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		if (data.note) {
			try {
				var encrypted = NRS.encryptNote(data.note, {
					"account": data.buyer
				});

				data.encryptedNoteNonce = encrypted.nonce;
				data.encryptedNote = encrypted.message;
			} catch (err) {
				return {
					"error": err.message
				};
			}
			delete data.note;
		}

		delete data.buyer;

		return {
			"data": data
		};
	}

	NRS.forms.dgsDelivery = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		NRS.sendRequest("getDGSPurchase", {
			"purchase": data.purchase
		}, function(response) {
			if (response.errorCode) {
				return {
					"error": "Could not fetch purchase."
				};
			} else {
				console.log(response);
				data.buyer = response.buyer;
			}
		}, false);

		if (data.data) {
			try {
				var encrypted = NRS.encryptNote(data.data, {
					"account": data.buyer
				});

				if (data.binaryData) {
					data.encryptedGoodsData = encrypted.nonce;
					data.encryptedGoodsNonce = encrypted.data;
				} else {
					data.encryptedGoodsText = encrypted.data;
					data.encryptedGoodsNonce = encrypted.nonce;
				}
			} catch (err) {
				return {
					"error": err.message
				};
			}

			delete data.goods;
		}

		delete data.buyer;
		delete data.data;
		delete data.binaryData;

		return {
			"data": data
		};
	}

	NRS.forms.dgsPurchase = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		data.deliveryDeadlineTimestamp = Math.floor(new Date().getTime() / 1000) + 60 * 60 * data.deliveryDeadlineTimestamp;

		return {
			"data": data
		};
	}

	NRS.forms.dgsQuantityChange = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		NRS.sendRequest("getDGSGood", {
			"goods": data.goods
		}, function(response) {
			if (response.errorCode) {
				return {
					"error": "Could not fetch good."
				};
			} else {
				if (data.quantity == response.quantity) {
					data.deltaQuantity = "0";
				} else if (data.quantity > response.quantity) {
					data.deltaQuantity = "+" + (data.quantity - response.quantity);
				} else {
					data.deltaQuantity = "-" + (response.quantity - data.quantity);
				}
			}
		}, false);

		if (data.deltaQuantity == "0") {
			return {
				"error": "No change in quantity."
			};
		}

		delete data.quantity;

		return {
			"data": data
		};
	}

	NRS.forms.dgsQuantityChangeComplete = function(response, data) {
		if (response.alreadyProcessed) {
			return;
		}

		var quantityField = $("#my_dgs_listings_table tr[data-goods=" + String(data.goods).escapeHTML() + "]").addClass("tentative").find(".quantity");

		quantityField.html(quantityField.html() + " " + String(data.deltaQuantity).escapeHTML());
	}

	NRS.forms.dgsPriceChangeComplete = function(response, data) {
		if (response.alreadyProcessed) {
			return;
		}

		$("#my_dgs_listings_table tr[data-goods=" + String(data.goods).escapeHTML() + "]").addClass("tentative").find(".price").html(NRS.formatAmount(data.priceNQT) + " NXT");
	}

	NRS.forms.dgsRefundComplete = function(response, data) {
		if (response.alreadyProcessed) {
			return;
		}

		$("#pending_purchases_dgs_page_contents div[data-purchase=" + String(data.purchase).escapeHTML() + "]").fadeOut();
	}

	NRS.forms.dgsDeliveryComplete = function(response, data) {
		if (response.alreadyProcessed) {
			return;
		}

		$("#pending_purchases_dgs_page_contents div[data-purchase=" + String(data.purchase).escapeHTML() + "]").fadeOut();
	}

	$("#dgs_refund_modal, #dgs_delivery_modal").on("show.bs.modal", function(e) {
		var $modal = $(this);
		var $invoker = $(e.relatedTarget);

		var type = $modal.attr("id");

		var purchase = $invoker.data("purchase");

		$modal.find("input[name=purchase]").val(purchase);

		NRS.sendRequest("getDGSPurchase", {
			"purchase": purchase
		}, function(response) {
			if (response.errorCode) {
				e.preventDefault();
				$.growl("Error fetching purchase info.", {
					"type": "danger"
				});
			} else {
				NRS.sendRequest("getDGSGood", {
					"goods": response.goods
				}, function(good) {
					if (response.errorCode) {
						e.preventDefault();
						$.growl("Error fetching product info.", {
							"type": "danger"
						});
					} else {
						var output = "<strong>Product Name</strong>: " + String(good.name).escapeHTML() + "<br /><strong>Price</strong>: " + NRS.formatAmount(response.priceNQT) + " NXT<br /><strong>Quantity</strong>: " + NRS.format(response.quantity);

						$modal.find(".purchase_info").html(output);

						if (type == "dgs_refund_modal") {
							$("#dgs_refund_refund").val(NRS.convertToNXT(response.priceNQT));
						}
					}
				}, false);
			}
		}, false);
	}).on("hidden.bs.modal", function(e) {
		$(this).find(".purchase_info").html("Loading...");
		$("#dgs_refund_purchase, #dgs_delivery_purchase").val("");
	});

	$("#dgs_delisting_modal, #dgs_quantity_change_modal, #dgs_price_change_modal, #dgs_purchase_modal").on("show.bs.modal", function(e) {
		var $modal = $(this);
		var $invoker = $(e.relatedTarget);

		var type = $modal.attr("id");

		var goods = $invoker.data("goods");

		$modal.find("input[name=goods]").val(goods);

		NRS.sendRequest("getDGSGood", {
			"goods": goods
		}, function(response) {
			if (response.errorCode) {
				e.preventDefault();
				$.growl("Error fetching product info.", {
					"type": "danger"
				});
			} else {
				var output = "<strong>Product Name</strong>: " + String(response.name).escapeHTML();
				if (type == "dgs_purchase_modal") {
					output += "<br /><div style='max-height:250px;overflow:auto;'>" + String(response.description).escapeHTML().nl2br() + "</div>";
				}
			}

			$modal.find(".goods_info").html(output);

			if (type == "dgs_quantity_change_modal") {
				$("#dgs_quantity_change_current_quantity, #dgs_quantity_change_quantity").val(String(response.quantity).escapeHTML());
			} else if (type == "dgs_price_change_modal") {
				$("#dgs_price_change_current_price, #dgs_price_change_price").val(NRS.convertToNXT(response.priceNQT).escapeHTML());
			} else if (type == "dgs_purchase_modal") {
				$("#dgs_purchase_price").val(NRS.convertToNXT(response.priceNQT).escapeHTML());
			}
		}, false);
	}).on("hidden.bs.modal", function(e) {
		$(this).find(".goods_info").html("Loading...");
		$("#dgs_quantity_change_current_quantity, #dgs_price_change_current_price, #dgs_quantity_change_quantity, #dgs_price_change_price").val("0");
	});

	return NRS;
}(NRS || {}, jQuery));