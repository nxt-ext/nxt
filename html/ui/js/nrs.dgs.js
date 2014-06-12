var NRS = (function(NRS, $, undefined) {
	NRS.getMarketplaceItemHTML = function(good) {
		return "<div style='float:right;color: #999999;background:white;padding:5px;border:1px solid #ccc;border-radius:3px'>" +
			"<strong>Seller</strong>: <span><a href='#' data-user='" + NRS.getAccountFormatted(good, "seller") + "' class='user_info'>" + NRS.getAccountTitle(good, "seller") + "</a></span><br>" +
			"<strong>Product Id</strong>: &nbsp;<a href='#''>" + String(good.goods).escapeHTML() + "</a>" +
			"</div>" +
			"<h3 class='title'><a href='#' data-goods='" + String(good.goods).escapeHTML() + "' data-toggle='modal' data-target='#dgs_purchase_modal'>" + String(good.name).escapeHTML() + "</a></h3>" +
			"<div class='price'><strong>" + NRS.formatAmount(good.priceNQT) + " NXT</strong></div>" +
			"<div class='showmore'><div class='moreblock description'>" + String(good.description).escapeHTML().nl2br() + "</div></div>" +
			"<span class='tags'><strong>Tags</strong>: " + String(good.tags).escapeHTML() + "</span><hr />";
	}

	NRS.getMarketplacePurchaseHTML = function(purchase) {
		return "<div" + (purchase.unconfirmed ? " class='tentative'" : "") + "><div style='float:right;color: #999999;background:white;padding:5px;border:1px solid #ccc;border-radius:3px'>" +
			"<strong>Seller</strong>: <span><a href='#' data-user='" + NRS.getAccountFormatted(purchase, "seller") + "' class='user_info'>" + NRS.getAccountTitle(purchase, "seller") + "</a></span><br>" +
			"<strong>Product Id</strong>: &nbsp;<a href='#''>" + String(purchase.goods).escapeHTML() + "</a>" +
			"</div>" +
			"<h3 class='title'><a href='#' data-purchase='" + String(purchase.purchase).escapeHTML() + "' data-toggle='modal' data-target='#dgs_view_delivery_modal'>" + String(purchase.name).escapeHTML() + "</a></h3>" +
			"<table>" +
			"<tr><td><strong>Order Date</strong>:</td><td>" + NRS.formatTimestamp(purchase.timestamp) + "</td></tr>" +
			"<tr><td><strong>Order Status</strong>:</td><td>" + (purchase.unconfirmed ? "Tentative" : (purchase.pending ? "<span class='label label-warning'>Pending</span>" : "Complete")) + "</td></tr>" +
			(purchase.pending ? "<tr><td><strong>Delivery Deadline</strong>:</td><td>" + NRS.formatTimestamp(new Date(purchase.deliveryDeadlineTimestamp * 1000)) + "</td></tr>" : "") +
			"<tr><td><strong>Price</strong>:</td><td>" + NRS.formatAmount(purchase.priceNQT) + " NXT</td></tr>" +
			"<tr><td><strong>Quantity</strong>:</td><td>" + NRS.format(purchase.quantity) + "</td></tr>" +
			"</table></div>" +
			"<hr />";
	}

	NRS.getMarketplacePendingPurchaseHTML = function(purchase) {
		var delivered = NRS.getUnconfirmedTransactionsFromCache(3, [5, 7], {
			"purchase": purchase.purchase
		});

		return "<div data-purchase='" + String(purchase.purchase).escapeHTML() + "'" + (delivered ? " class='tentative'" : "") + "><div style='float:right;color: #999999;background:white;padding:5px;border:1px solid #ccc;border-radius:3px'>" +
			"<strong>Buyer</strong>: <span><a href='#' data-user='" + NRS.getAccountFormatted(purchase, "buyer") + "' class='user_info'>" + NRS.getAccountTitle(purchase, "buyer") + "</a></span><br>" +
			"<strong>Product Id</strong>: &nbsp;<a href='#''>" + String(purchase.goods).escapeHTML() + "</a>" +
			"</div>" +
			"<h3 class='title'><a href='#' data-purchase='" + String(purchase.purchase).escapeHTML() + "' data-toggle='modal' data-target='#dgs_view_purchase_modal'>" + String(purchase.name).escapeHTML() + "</a></h3>" +
			"<table class='purchase' style='margin-bottom:5px'>" +
			"<tr><td><strong>Order Date</strong>:</td><td>" + NRS.formatTimestamp(purchase.timestamp) + "</td></tr>" +
			"<tr><td><strong>Delivery Deadline</strong>:</td><td>" + NRS.formatTimestamp(new Date(purchase.deliveryDeadlineTimestamp * 1000)) + "</td></tr>" +
			"<tr><td><strong>Price</strong>:</td><td>" + NRS.formatAmount(purchase.priceNQT) + " NXT</td></tr>" +
			"<tr><td><strong>Quantity</strong>:</td><td>" + NRS.format(purchase.quantity) + "</td></tr>" +
			"</table>" +
			"<span class='delivery'>" + (!delivered ? "<button type='button' class='btn btn-default btn-deliver' data-toggle='modal' data-target='#dgs_delivery_modal' data-purchase='" + String(purchase.purchase).escapeHTML() + "'>Deliver Goods</button>" : "Delivered") + "</span>" +
			"</div><hr />";
	}

	NRS.pages.newest_dgs = function() {
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
					content += NRS.getMarketplaceItemHTML(response.goods[i]);
				}
			}

			NRS.dataLoaded(content);
			NRS.showMore();
		});
	}

	NRS.pages.dgs_seller = function() {
		var content = "";

		var seller = $(".dgs_search input[name=q]").val();

		NRS.sendRequest("getDGSGoods+", {
			"seller": seller,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		}, function(response) {
			if (response.goods && response.goods.length) {
				if (response.goods.length > NRS.itemsPerPage) {
					NRS.hasMorePages = true;
					response.goods.pop();
				}

				var content = "";

				for (var i = 0; i < response.goods.length; i++) {
					content += NRS.getMarketplaceItemHTML(response.goods[i]);
				}
			}

			NRS.dataLoaded(content);
			NRS.showMore();
		});
	}

	NRS.pages.purchased_dgs = function() {
		var content = "";

		if (NRS.pageNumber == 1) {
			var unconfirmedTransactions = NRS.getUnconfirmedTransactionsFromCache(3, 4);

			if (unconfirmedTransactions) {
				for (var i = 0; i < unconfirmedTransactions.length; i++) {
					var unconfirmedTransaction = unconfirmedTransactions[i];
					content += NRS.getMarketplacePurchaseHTML(unconfirmedTransaction);
				}
			}
		}

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

				for (var i = 0; i < response.purchases.length; i++) {
					content += NRS.getMarketplacePurchaseHTML(response.purchases[i]);
				}
			}

			NRS.dataLoaded(content);
		});
	}

	NRS.pages.pending_purchases_dgs = function() {
		NRS.sendRequest("getDGSPendingPurchases", {
			"seller": NRS.account,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		}, function(response) {
			var content = "";

			if (response.purchases && response.purchases.length) {
				if (response.purchases.length > NRS.itemsPerPage) {
					NRS.hasMorePages = true;
					response.purchases.pop();
				}

				for (var i = 0; i < response.purchases.length; i++) {
					content += NRS.getMarketplacePendingPurchaseHTML(response.purchases[i]);
				}
			}

			NRS.dataLoaded(content);
		});
	}

	NRS.pages.my_dgs_listings = function() {
		var rows = "";

		var unconfirmedTransactions = NRS.getUnconfirmedTransactionsFromCache(3, 0);

		if (unconfirmedTransactions) {
			for (var i = 0; i < unconfirmedTransactions.length; i++) {
				var unconfirmedTransaction = unconfirmedTransactions[i];
				rows += "<tr class='tentative' data-goods='" + String(unconfirmedTransaction.goods).escapeHTML() + "'><td><a href='#' data-toggle='modal' data-target='#dgs_listing_modal' data-goods='" + String(unconfirmedTransaction.goods).escapeHTML() + "'>" + String(unconfirmedTransaction.name).escapeHTML() + "</a></td><td class='quantity'>" + NRS.format(unconfirmedTransaction.quantity) + "</td><td class='price'>" + NRS.formatAmount(unconfirmedTransaction.priceNQT) + " NXT</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_price_change_modal' data-goods='" + String(unconfirmedTransaction.goods).escapeHTML() + "'>Change Price</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_quantity_change_modal' data-goods='" + String(unconfirmedTransaction.goods).escapeHTML() + "'>Change QTY</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_delisting_modal' data-goods='" + String(unconfirmedTransaction.goods).escapeHTML() + "'>Delete</a></td></tr>";
			}
		}

		//inStockOnly doesn't work here, need to get all but delisted.
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

					var unconfirmedTransaction = NRS.getUnconfirmedTransactionFromCache(3, [1, 2, 3], {
						"goods": good.goods
					});

					if (unconfirmedTransaction) {
						if (unconfirmedTransaction.subtype == 1) {
							deleted = tentative = true;
						} else if (unconfirmedTransaction.subtype == 2) {
							good.priceNQT = unconfirmedTransaction.priceNQT;
							tentative = true;
						} else {
							good.quantity = NRS.format(good.quantity) + " " + NRS.format(unconfirmedTransaction.deltaQuantity);
							tentative = true;
							quantityFormatted = true;
						}
					}

					rows += "<tr class='" + (tentative ? "tentative" : "") + (deleted ? " tentative-crossed" : "") + "' data-goods='" + String(good.goods).escapeHTML() + "'><td><a href='#' data-toggle='modal' data-target='#dgs_listing_modal' data-goods='" + String(good.goods).escapeHTML() + "'>" + String(good.name).escapeHTML() + "</a></td><td class='quantity'>" + (quantityFormatted ? good.quantity : NRS.format(good.quantity)) + "</td><td class='price'>" + NRS.formatAmount(good.priceNQT) + " NXT</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_price_change_modal' data-goods='" + String(good.goods).escapeHTML() + "'>Change Price</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_quantity_change_modal' data-goods='" + String(good.goods).escapeHTML() + "'>Change QTY</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_delisting_modal' data-goods='" + String(good.goods).escapeHTML() + "'>Delete</a></td></tr>";
				}
			}

			NRS.dataLoaded(rows);
		});
	}

	NRS.incoming.my_dgs_listings = function(transactions) {
		if (transactions || NRS.unconfirmedTransactionsChange || NRS.state.isScanning) {
			NRS.loadPage("my_dgs_listings");
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

			var rowToAdd = "<tr class='tentative' data-goods='" + String(response.transaction).escapeHTML() + "'><td><a href='#' data-toggle='modal' data-target='#dgs_listing_modal' data-goods='" + String(response.transaction).escapeHTML() + "'>" + String(data.name).escapeHTML() + "</a></td><td class='quantity'>" + NRS.format(data.quantity) + "</td><td class='price'>" + NRS.formatAmount(data.priceNQT) + " NXT</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_price_change_modal' data-goods='" + String(response.transaction).escapeHTML() + "'>Change Price</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_quantity_change_modal' data-goods='" + String(response.transaction).escapeHTML() + "'>Change QTY</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_delisting_modal' data-goods='" + String(response.transaction).escapeHTML() + "'>Delete</a></td></tr>";

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

		NRS.sendRequest("getDGSPurchase", {
			"purchase": data.purchase
		}, function(response) {
			if (response.errorCode) {
				return {
					"error": "Could not fetch purchase."
				};
			} else {
				data.buyer = response.buyer;
			}
		}, false);

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
				data.buyer = response.buyer;
			}
		}, false);

		if (data.data) {
			try {
				var encrypted = NRS.encryptNote(data.data, {
					"account": data.buyer
				});

				data.encryptedGoodsData = encrypted.message;
				data.encryptedGoodsNonce = encrypted.nonce;
			} catch (err) {
				return {
					"error": err.message
				};
			}
		}

		delete data.buyer;
		delete data.data;

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

		$("#pending_purchases_dgs_contents div[data-purchase=" + String(data.purchase).escapeHTML() + "]").fadeOut();
	}

	NRS.forms.dgsDeliveryComplete = function(response, data) {
		if (response.alreadyProcessed) {
			return;
		}

		$("#pending_purchases_dgs_contents div[data-purchase=" + String(data.purchase).escapeHTML() + "]").addClass("tentative").find("span.delivery").html("Delivered");
	}

	$("#dgs_refund_modal, #dgs_delivery_modal, #dgs_view_purchase_modal, #dgs_view_delivery_modal").on("show.bs.modal", function(e) {
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
						var output = "<table>";
						output += "<tr><td><strong>Product</strong>:</td><td>" + String(good.name).escapeHTML() + "</td></tr>";
						output += "<tr><td><strong>Price</strong>:</td><td>" + NRS.formatAmount(response.priceNQT) + " NXT</td></tr>";
						output += "<tr><td><strong>Quantity</strong>:</td><td>" + NRS.format(response.quantity) + "</td></tr>";

						if (type == "dgs_delivery_modal" || type == "dgs_refund_modal") {
							if (response.note) {
								try {
									response.note = NRS.decryptNote(response.note, {
										"nonce": response.noteNonce,
										"account": response.buyer
									});
								} catch (err) {
									response.note = String(err.message);
								}
							}

							output += "<tr><td><strong>Note</strong>:</td><td>" + String(response.note).escapeHTML().nl2br() + "</td></tr>";
						}

						output += "</table>";

						$modal.find(".purchase_info").html(output);

						if (type == "dgs_refund_modal") {
							$("#dgs_refund_refund").val(NRS.convertToNXT(response.priceNQT));
						} else if (type == "dgs_view_purchase_modal") {
							console.log(response);
						} else if (type == "dgs_view_delivery_modal") {
							console.log(response);
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

	$(".dgs_search").on("submit", function(e) {
		e.preventDefault();

		var seller = $.trim($(this).find("input[name=q]").val());

		$(".dgs_search input[name=q]").val(seller);

		if (/^\d+$/.test(seller) || /^(NXT\-)/i.test(seller)) {
			$("#dgs_seller_page_link").show();

			NRS.goToPage("dgs_seller");
		} else {
			$.growl("Invalid seller ID.", {
				"type": "danger"
			});
		}
	});

	return NRS;
}(NRS || {}, jQuery));