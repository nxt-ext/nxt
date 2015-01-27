//noinspection JSUnusedLocalSymbols
/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {

	var EXCHANGEABLE = 0x01;
	var CONTROLLABLE = 0x02;
	var RESERVABLE = 0x04;
	var CLAIMABLE = 0x08;
	var MINTABLE = 0x10;
	var NON_SHUFFLEABLE = 0x20;

	NRS.isExchangeable = function(type) {
		return type & EXCHANGEABLE;
	};

	NRS.isControllable = function(type) {
		return type & CONTROLLABLE;
	};

	NRS.isReservable = function(type) {
		return type & RESERVABLE;
	};

	NRS.isClaimable = function(type) {
		return type & CLAIMABLE;
	};

	NRS.isMintable = function(type) {
		return type & MINTABLE;
	};

	NRS.isNonShuffleable = function(type) {
		return type & NON_SHUFFLEABLE;
	};

	/* MONETARY SYSTEM PAGE */
	/* Monetary System Page Search capitalization */
	var search = $("#currency_search");
	search.find("input[name=q]").blur(function() {
		if (this.value && this.value != this.value.toLocaleUpperCase()) {
			this.value = this.value.toLocaleUpperCase();
		}
	});

	search.find("input[name=q]").keyup(function() {
		if (this.value && this.value != this.value.toLocaleUpperCase()) {
			this.value = this.value.toLocaleUpperCase();
		}
	});

	search.on("submit", function(e, data) {
		e.preventDefault();
		//refresh is true if data is refreshed automatically by the system (when a new block arrives)
		var refresh = false;
		if (data && data.refresh) {
			refresh = true;
		}
		NRS.pageNumber = 1;
		var currencyCode = $.trim($("#currency_search").find("input[name=q]").val());
		$("#buy_currency_with_nxt").html("Exchange NXT for " + currencyCode);
		$("#buy_currency_offers").html("Offers to Exchange NXT for " + currencyCode);
		$("#sell_currency_with_nxt").html("Exchange " + currencyCode + " for NXT");
		$("#sell_currency_offers").html("Offers to Exchange " + currencyCode + " for NXT");
		$(".currency_code").html(String(currencyCode).escapeHTML());

		var currencyId = 0;
		NRS.sendRequest("getCurrency+", {
			"code": currencyCode
		}, function(response) {
			if (response && !response.errorDescription) {
				$("#MSnoCode").hide();
				$("#MScode").show();
				$("#currency_account").html(String(response.accountRS).escapeHTML());
				currencyId = response.currency;
				$("#currency_id").html(String(currencyId).escapeHTML());
				$("#currency_name").html(String(response.name).escapeHTML());
				$("#currency_code").html(String(response.code).escapeHTML());
				$("#currency_current_supply").html(NRS.convertToQNTf(response.currentSupply, response.decimals).escapeHTML());
				$("#currency_max_supply").html(NRS.convertToQNTf(response.maxSupply, response.decimals).escapeHTML());
				$("#currency_decimals").html(String(response.decimals).escapeHTML());
				$("#currency_description").html(String(response.description).escapeHTML());
				var buyCurrencyButton = $("#buy_currency_button");
				buyCurrencyButton.data("currency", currencyId);
				buyCurrencyButton.data("decimals", response.decimals);
				var sellCurrencyButton = $("#sell_currency_button");
				sellCurrencyButton.data("currency", currencyId);
				sellCurrencyButton.data("decimals", response.decimals);
				if (!refresh) {
					var msLinksCallout = $("#ms_links_callout");
					msLinksCallout.html("");
					msLinksCallout.append("<a href='#' data-toggle='modal' data-target='#transfer_currency_modal' data-currency='" + String(response.currency).escapeHTML() + "' data-code='" + response.code + "' data-decimals='" + response.decimals + "'>" + $.t("transfer") + "</a>");
					msLinksCallout.append(" | ");
					msLinksCallout.append("<a href='#' data-toggle='modal' data-target='#publish_exchange_offer_modal' data-currency='" + String(response.currency).escapeHTML() + "' data-code='" + response.code + "' data-decimals='" + response.decimals + "'>" + $.t("offer") + "</a>");
				}
			} else {
				$("#MSnoCode").show();
				$("#MScode").hide();
				$.growl(response.errorDescription, {
					"type": "danger"
				});
			}
		}, false);
		
		NRS.sendRequest("getAccountCurrencies+", {
			"account": NRS.accountRS,
			"currency": currencyId
		}, function(response) {
			if (response.unconfirmedUnits) {
				$("#your_currency_balance").html(NRS.formatQuantity(response.unconfirmedUnits, response.decimals));
			} else {
				$("#your_currency_balance").html(0);
			}
		});
		
		NRS.loadCurrencyOffers("buy", currencyId, refresh);
		NRS.loadCurrencyOffers("sell", currencyId, refresh);
		NRS.getExchangeRequests(currencyId, refresh);
		NRS.getExchangeHistory(currencyId, refresh);
		if (NRS.accountInfo.unconfirmedBalanceNQT == "0") {
			$("#ms_your_nxt_balance").html("0");
			$("#buy_automatic_price").addClass("zero").removeClass("nonzero");
		} else {
			$("#ms_your_nxt_balance").html(NRS.formatAmount(NRS.accountInfo.unconfirmedBalanceNQT));
			$("#buy_automatic_price").addClass("nonzero").removeClass("zero");
		}
		NRS.pageLoaded();
	});
	
	/* Search on Currencies Page */
	$("#currencies_search").on("submit", function(e) {
		e.preventDefault();
		NRS.pageNumber = 1;
		var requestAPI = "searchCurrencies+";
		var query = $.trim($("#currencies_search").find("input[name=searchquery]").val());
		if (query == "") requestAPI = "getAllCurrencies+";
		NRS.sendRequest(requestAPI, {
			"query": query,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		}, function(response) {
			NRS.hasMorePages = false;
			if (response.currencies && response.currencies.length) {
				if (response.currencies.length > NRS.itemsPerPage) {
					NRS.hasMorePages = true;
					response.currencies.pop();
				}
				var rows = NRS.getCurrencyRows(response);
				NRS.currenciesTableLayout();
				NRS.dataLoaded(rows);
			} else {
				NRS.dataLoaded();
			}
		}, false);
	});

	NRS.getCurrencyRows = function(response) {
		var rows = "";
		for (var i = 0; i < response.currencies.length; i++) {
			var currency = response.currencies[i];
			var name = String(currency.name).escapeHTML();
			var currencyId = String(currency.currency).escapeHTML();
			var code = String(currency.code).escapeHTML();
			var resSupply = NRS.convertToQNTf(currency.reserveSupply, currency.decimals);
			var decimals = String(currency.decimals).escapeHTML();
			var minReserve = String(currency.minReservePerUnitNQT).escapeHTML();
			var typeIcons = NRS.getTypeIcons(currency.type);
			rows += "<tr>" +
				"<td>" +
					"<a href='#' data-transaction='" + currencyId + "' >" + code + "</a>" +
				"</td>" +
				"<td>" + name + "</td>" +
				"<td>" + typeIcons + "</td>" +
				"<td>" + NRS.formatQuantity(currency.currentSupply, currency.decimals) + "</td>" +
				"<td>" + NRS.formatQuantity(currency.maxSupply, currency.decimals) + "</td>" +
				"<td>";
				rows += "<a href='#' class='btn btn-xs btn-default' onClick='NRS.goToCurrency(&quot;" + code + "&quot;)' " + (!NRS.isExchangeable(currency.type) ? "disabled" : "") + ">" + $.t("exchange") + "</a> ";
				rows += "<a href='#' class='btn btn-xs btn-default' data-toggle='modal' data-target='#reserve_currency_modal' data-currency='" + currencyId + "' data-name='" + name + "' data-code='" + code + "' data-ressupply='" + resSupply + "' data-decimals='" + decimals + "' data-minreserve='" + minReserve + "' " + (currency.issuanceHeight > NRS.lastBlockHeight && NRS.isReservable(currency.type) ? "" : "disabled") + " >" + $.t("reserve") + "</a> ";
				rows += "</td></tr>";
		}
		return rows;
	};

	NRS.getTypeIcons = function(type) {
		var typeIcons = "";
		if (NRS.isExchangeable(type)) {
			typeIcons += "<i title='" + $.t('exchangeable') + "' class='fa fa-exchange'></i> ";
		}
		if (NRS.isControllable(type)) {
			typeIcons += "<i title='" + $.t('controllable') + "' class='fa fa-sliders'></i> ";
		}
		if (NRS.isReservable(type)) {
			typeIcons += "<i title='" + $.t('reservable') + "' class='fa fa-university'></i> ";
		}
		if (NRS.isClaimable(type)) {
			typeIcons += "<i title='" + $.t('claimable') + "' class='ion-android-archive'></i> ";
		}
		if (NRS.isMintable(type)) {
			typeIcons += "<i title='" + $.t('mintable') + "' class='fa fa-money'></i> ";
		}
		return typeIcons;
	};

	NRS.currenciesTableLayout = function() {
		var currenciesTable = $('#currencies_table');
		currenciesTable.find('[data-i18n="type"]').show();
		currenciesTable.find('[data-i18n="supply"]').show();
		currenciesTable.find('[data-i18n="max_supply"]').show();
		currenciesTable.find('[data-i18n="units"]').hide();
	};

	NRS.loadCurrencyOffers = function(type, currencyId, refresh) {
		NRS.sendRequest("get" + type.capitalize() + "Offers+", {
			"currency": currencyId, "availableOnly": "true",
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		}, function(response) {
			if (response.offers && response.offers.length > NRS.itemsPerPage) {
				NRS.hasMorePages = true;
				response.offers.pop();
			}
			var offersTable = $("#ms_open_" + type + "_orders_table");
			var offers = response.offers;
			if (!offers) {
				offers = [];
			}
			if (NRS.unconfirmedTransactions.length) {
				var added = false;
				for (var i = 0; i < NRS.unconfirmedTransactions.length; i++) {
					var unconfirmedTransaction = NRS.unconfirmedTransactions[i];
					unconfirmedTransaction.offer = unconfirmedTransaction.transaction;

					if (unconfirmedTransaction.type == 5 && unconfirmedTransaction.subtype == 4 && unconfirmedTransaction.currency == currencyId) {
						offers.push($.extend(true, {}, unconfirmedTransaction)); //make sure it's a deep copy
						added = true;
					}
				}

				if (added) {
					offers.sort(function (a, b) {
						if (type == "sell") {
							//lowest price at the top
							return new BigInteger(a.sellRateNQT).compareTo(new BigInteger(b.sellRateNQT));
						} else {
							//highest price at the top
							return new BigInteger(b.buyRateNQT).compareTo(new BigInteger(a.buyRateNQT));
						}
					});
				}
			}
         if (response.offers && response.offers.length) {
				var rows = "";
				var decimals = parseInt($("#currency_decimals").text(), 10);
				for (i = 0; i < response.offers.length; i++) {
					var offer = response.offers[i];
					var rateNQT = offer.rateNQT || (type == "sell" ? offer.attachment.sellRateNQT : offer.attachment.buyRateNQT);
               if (i == 0 && !refresh) {
						$("#" + (type == "sell" ? "buy" : "sell") + "_currency_price").val(NRS.calculateOrderPricePerWholeQNT(rateNQT, decimals));
					}

					// The offers collection contains both real offers and unconfirmed offers and the code below works for both types
					var transactionLink;
					if (offer.unconfirmed) {
						transactionLink = "<a href='#' data-transaction='" + String(offer.transaction).escapeHTML() + "'>/</a>";
					} else {
						transactionLink = "<a href='#' data-transaction='" + String(offer.offer).escapeHTML() + "'>" + offer.height + "</a>";
					}
					var accountRS = offer.accountRS || offer.senderRS;
               accountRS = String(accountRS).escapeHTML();
					var accountLink = offer.unconfirmed ? "You - <strong>Pending</strong>" : (offer.account == NRS.account ? "<strong>You</strong>" : "<a href='#' class='user-info' data-user='" + accountRS + "'>" + accountRS + "</a>");
					var supply = offer.supply || (type == "sell" ? offer.attachment.initialSellSupply : offer.attachment.initialBuySupply);
					var limit = offer.limit || (type == "sell" ? offer.attachment.totalSellLimit : offer.attachment.totalBuyLimit);
               rows += "<tr>" +
						"<td>" + transactionLink + "</td>" +
						"<td>" + accountLink + "</td>" +
						"<td>" + NRS.convertToQNTf(supply, decimals) + "</td>" +
						"<td>" + NRS.convertToQNTf(limit, decimals) + "</td>" +
						"<td>" + NRS.calculateOrderPricePerWholeQNT(rateNQT, decimals) + "</td>" +
					"</tr>";
				}
				offersTable.find("tbody").empty().append(rows);
			} else {
				offersTable.find("tbody").empty();
			}
			NRS.dataLoadFinished(offersTable, !refresh);
		});
	};
	
	NRS.incoming.monetary_system = function() {
		search.trigger("submit", [{ "refresh": true }]);
	};

	/* CURRENCY FOUNDERS MODEL */
	var foundersModal = $("#currency_founders_modal");
	foundersModal.on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var currencyId = $invoker.data("currency");
		var issueHeight = $invoker.data("issueheight");
		if (issueHeight > NRS.lastBlockHeight) {
			$("#founders_blocks_active").html(issueHeight - NRS.lastBlockHeight);
		} else {
			$("#founders_blocks_active").html("Active");
		}
		$("#founders_currency_code").html(String($invoker.data("code")).escapeHTML());

		NRS.sendRequest("getCurrencyFounders", {
			"currency": currencyId
		}, function(response) {
			var rows = "";
			var decimals = $invoker.data("decimals"); // has to be numeric not string
			var minReservePerUnitNQT = new BigInteger(String($invoker.data("minreserve"))).multiply(new BigInteger("" + Math.pow(10, decimals)));
			var initialSupply = new BigInteger(String($invoker.data("initialsupply")));
			var resSupply = new BigInteger(String($invoker.data("ressupply")));
			var totalAmountReserved = BigInteger.ZERO;
			$("#founders_reserve_units").html(NRS.formatQuantity(resSupply, decimals));
			$("#founders_issuer_units").html(NRS.formatQuantity(initialSupply, decimals));
			if (response.founders && response.founders.length) {
				var amountPerUnitNQT = BigInteger.ZERO;
				for (var i = 0; i < response.founders.length; i++) {
					amountPerUnitNQT = new BigInteger(response.founders[i].amountPerUnitNQT).multiply(new BigInteger("" + Math.pow(10, decimals)));
					totalAmountReserved = totalAmountReserved.add(amountPerUnitNQT);
				}
				for (i = 0; i < response.founders.length; i++) {
					var account = response.founders[i].accountRS;
					amountPerUnitNQT = new BigInteger(response.founders[i].amountPerUnitNQT).multiply(new BigInteger("" + Math.pow(10, decimals)));
					var percentage = NRS.calculatePercentage(amountPerUnitNQT, minReservePerUnitNQT);
					rows += "<tr>" +
						"<td>" +
							"<a href='#' data-user='" + NRS.getAccountFormatted(account, "account") + "' class='user_info'>" + NRS.getAccountTitle(account, "account") + "</a>" +
						"</td>" +
						"<td>" + NRS.convertToNXT(amountPerUnitNQT) + "</td>" +
						"<td>" + NRS.convertToNXT(amountPerUnitNQT.multiply(new BigInteger(NRS.convertToQNTf(resSupply, decimals)))) + "</td>" +
						"<td>" + NRS.formatQuantity(resSupply.subtract(initialSupply).multiply(amountPerUnitNQT).divide(totalAmountReserved), decimals) + "</td>" +
						"<td>" + percentage + "</td>" +
					"</tr>";
				}
			} else {
				rows = "<tr><td colspan='5'>None</td></tr>";
			}
			rows += "<tr>" +
				"<td><b>Totals</b></td>" +
				"<td>" + NRS.convertToNXT(totalAmountReserved) + "</td>" +
				"<td>" + NRS.convertToNXT(totalAmountReserved.multiply(new BigInteger(NRS.convertToQNTf(resSupply, decimals)))) + "</td>" +
				"<td>" + NRS.formatQuantity(resSupply.subtract(initialSupply), decimals) + "</td>" +
				"<td>" + NRS.calculatePercentage(totalAmountReserved, minReservePerUnitNQT) + "</td>" +
			"</tr>";
			var foundersTable = $("#currency_founders_table");
			foundersTable.find("tbody").empty().append(rows);
			NRS.dataLoadFinished(foundersTable);
		});
	});

	foundersModal.on("hidden.bs.modal", function() {
		var foundersTable = $("#currency_founders_table");
		foundersTable.find("tbody").empty();
		foundersTable.parent().addClass("data-loading");
	});
	
	NRS.getExchangeHistory = function(currencyId, refresh) {
		if (NRS.currenciesTradeHistoryType == "my_exchanges") {
			NRS.sendRequest("getExchanges+", {
				"currency": currencyId,
				"account": NRS.accountRS,
				"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
				"lastIndex": NRS.pageNumber * NRS.itemsPerPage
			}, function(response) {
				var historyTable = $("#ms_exchanges_history_table");
            if (response.exchanges && response.exchanges.length) {
					if (response.exchanges.length > NRS.itemsPerPage) {
						NRS.hasMorePages = true;
						response.exchanges.pop();
					}
					var rows = "";
					for (var i = 0; i < response.exchanges.length; i++) {
						var exchange = response.exchanges[i];
						rows += "<tr>" +
							"<td>" +
								"<a href='#' data-transaction='" + String(exchange.transaction).escapeHTML() + "'>" + NRS.formatTimestamp(exchange.timestamp) + "</a>" +
							"</td>" +
							"<td>" +
								"<a href='#' class='user-info' data-user='" + exchange.sellerRS + "'>" + NRS.getAccountTitle(exchange.sellerRS) + "</a>" +
							"</td>" +
							"<td>" +
								"<a href='#' class='user-info' data-user='" + exchange.buyerRS + "'>" + NRS.getAccountTitle(exchange.buyerRS) + "</a>" +
							"</td>" +
							"<td>" + NRS.formatQuantity(exchange.units, exchange.decimals) + "</td>" +
							"<td>" + NRS.calculateOrderPricePerWholeQNT(exchange.rateNQT, exchange.decimals) + "</td>" +
							"<td>" + NRS.formatAmount(NRS.calculateOrderTotalNQT(exchange.units, exchange.rateNQT)) + "</td>" +
					  "</tr>";
					}
					historyTable.find("tbody").empty().append(rows);
				} else {
					historyTable.find("tbody").empty();
				}
				NRS.dataLoadFinished(historyTable, !refresh);
			});
		} else {
			NRS.sendRequest("getExchanges+", {
				"currency": currencyId,
				"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
				"lastIndex": NRS.pageNumber * NRS.itemsPerPage
			}, function(response) {
				var historyTable = $("#ms_exchanges_history_table");
            if (response.exchanges && response.exchanges.length) {
					if (response.exchanges.length > NRS.itemsPerPage) {
						NRS.hasMorePages = true;
						response.exchanges.pop();
					}
					var rows = "";
					for (var i = 0; i < response.exchanges.length; i++) {
						var exchange = response.exchanges[i];
						rows += "<tr>" +
							"<td>" +
								"<a href='#' data-transaction='" + String(exchange.transaction).escapeHTML() + "'>" + NRS.formatTimestamp(exchange.timestamp) + "</a>" +
							"</td>" +
							"<td>" +
								"<a href='#' class='user-info' data-user='" + exchange.sellerRS + "'>" + NRS.getAccountTitle(exchange.sellerRS) + "</a>" +
							"</td>" +
							"<td>" +
								"<a href='#' class='user-info' data-user='" + exchange.buyerRS + "'>" + NRS.getAccountTitle(exchange.buyerRS) + "</a>" +
							"</td>" +
							"<td>" + NRS.formatQuantity(exchange.units, exchange.decimals) + "</td>" +
							"<td>" + NRS.calculateOrderPricePerWholeQNT(exchange.rateNQT, exchange.decimals) + "</td>" +
							"<td>" + NRS.formatAmount(NRS.calculateOrderTotalNQT(exchange.units, exchange.rateNQT)) + "</td>" +
						"</tr>";
					}
					historyTable.find("tbody").empty().append(rows);
				} else {
					historyTable.find("tbody").empty();
				}
				NRS.dataLoadFinished(historyTable, !refresh);
			});
		}
	};

	NRS.getExchangeRequests = function(currencyId, refresh) {
		NRS.sendRequest("getAccountExchangeRequests+", {
			"currency": currencyId,
			"account": NRS.accountRS,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		}, function (response) {
			if (response.exchangeRequests && response.exchangeRequests.length) {
				if (response.exchangeRequests.length > NRS.itemsPerPage) {
					NRS.hasMorePages = true;
					response.exchangeRequests.pop();
				}
			}
			var requestTable = $("#ms_exchange_requests_table");
			var exchangeRequests = response.exchangeRequests;
			if (!exchangeRequests) {
				exchangeRequests = [];
			}
			if (NRS.unconfirmedTransactions.length) {
				for (var i = 0; i < NRS.unconfirmedTransactions.length; i++) {
					var unconfirmedTransaction = NRS.unconfirmedTransactions[i];
					if (unconfirmedTransaction.type == 5 && (unconfirmedTransaction.subtype == 5 || unconfirmedTransaction.subtype == 6) && unconfirmedTransaction.currency == currencyId) {
						exchangeRequests.unshift($.extend(true, {}, unconfirmedTransaction)); //make sure it's a deep copy
					}
				}
			}
			if (response.exchangeRequests && response.exchangeRequests.length) {
				if (response.exchangeRequests.length > NRS.itemsPerPage) {
					NRS.hasMorePages = true;
					response.exchangeRequests.pop();
				}
				var rows = "";
				var decimals = parseInt($("#currency_decimals").text(), 10);
				for (i = 0; i < response.exchangeRequests.length; i++) {
					var exchangeRequest = response.exchangeRequests[i];
					var type = (exchangeRequest.subtype == 5 ? "buy" : (exchangeRequest.subtype == 6 ? "sell" : exchangeRequest.type));
					rows += "<tr class='" + (exchangeRequest.hasOwnProperty('confirmed') && !exchangeRequest.confirmed ? "tentative-allow-links" : "confirmed") + "'>" +
						"<td>" +
							"<a href='#' data-transaction='" + String(exchangeRequest.transaction).escapeHTML() + "'>" + NRS.formatTimestamp(exchangeRequest.timestamp) + "</a>" +
						"</td>" +
						"<td>" + type + "</td>" +
						"<td>" + NRS.convertToQNTf(exchangeRequest.units, decimals) + "</td>" +
						"<td>" + NRS.calculateOrderPricePerWholeQNT(exchangeRequest.rateNQT, decimals) + "</td>" +
						"<td>" + NRS.formatAmount(NRS.calculateOrderTotalNQT(exchangeRequest.units, exchangeRequest.rateNQT)) + "</td>" +
					"</tr>";
				}
				requestTable.find("tbody").empty().append(rows);
			} else {
				requestTable.find("tbody").empty();
			}
			NRS.dataLoadFinished(requestTable, !refresh);
		});
	};

	/* Monetary System Buy/Sell boxes */
	$("#buy_currency_box .box-header, #sell_currency_box .box-header").click(function(e) {
		e.preventDefault();
		//Find the box parent        
		var box = $(this).parents(".box").first();
		//Find the body and the footer
		var bf = box.find(".box-body, .box-footer");
		if (!box.hasClass("collapsed-box")) {
			box.addClass("collapsed-box");
			$(this).find(".btn i.fa").removeClass("fa-minus").addClass("fa-plus");
			bf.slideUp();
		} else {
			box.removeClass("collapsed-box");
			bf.slideDown();
			$(this).find(".btn i.fa").removeClass("fa-plus").addClass("fa-minus");
		}
	});
	
	/* Currency Order Model */
	$("#currency_order_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var exchangeType = $invoker.data("type");
		var currencyId = $invoker.data("currency");
		var currencyDecimals = parseInt($invoker.data("decimals"), 10);

		$("#currency_order_modal_button").html(exchangeType + " currency").data("resetText", exchangeType + " currency");

		try {
			var units = String($("#" + exchangeType + "_currency_units").val());
			var unitsQNT = new BigInteger(NRS.convertToQNT(units, currencyDecimals));
			var rateNQT = new BigInteger(NRS.calculatePricePerWholeQNT(NRS.convertToNQT(String($("#" + exchangeType + "_currency_price").val())), currencyDecimals));
			var feeNQT = new BigInteger(NRS.convertToNQT(String($("#" + exchangeType + "_currency_fee").val())));
			var totalNXT = NRS.formatAmount(NRS.calculateOrderTotalNQT(unitsQNT, rateNQT), false, true);
		} catch (err) {
			$.growl($.t("error_invalid_input"), {
				"type": "danger"
			});
			return e.preventDefault();
		}

		if (rateNQT.toString() == "0" || unitsQNT.toString() == "0") {
			$.growl($.t("error_amount_price_required"), {
				"type": "danger"
			});
			return e.preventDefault();
		}

		if (feeNQT.toString() == "0") {
			feeNQT = new BigInteger("100000000");
		}

		var rateNQTPerWholeQNT = rateNQT.multiply(new BigInteger("" + Math.pow(10, currencyDecimals)));
		var description;
		var tooltipTitle;
		if (exchangeType == "buy") {
			description = $.t("buy_currency_description", {
				"total": totalNXT,
				"quantity": NRS.formatQuantity(unitsQNT, currencyDecimals, true),
				"currency_code": $("#currency_code").html().escapeHTML(),
				"rate": NRS.formatAmount(rateNQTPerWholeQNT)
			});
			tooltipTitle = $.t("buy_currency_description_help", {
				"rate": NRS.formatAmount(rateNQTPerWholeQNT, false, true),
				"total_nxt": totalNXT
			});
		} else {
			description = $.t("sell_currency_description", {
				"total": totalNXT,
				"quantity": NRS.formatQuantity(unitsQNT, currencyDecimals, true),
				"currency_code": $("#currency_code").html().escapeHTML(),
				"rate": NRS.formatAmount(rateNQTPerWholeQNT)
			});
			tooltipTitle = $.t("sell_currency_description_help", {
				"rate": NRS.formatAmount(rateNQTPerWholeQNT, false, true),
				"total_nxt": totalNXT
			});
		}

		$("#currency_order_description").html(description);
		$("#currency_order_total").html(totalNXT + " NXT");
		$("#currency_order_fee_paid").html(NRS.formatAmount(feeNQT) + " NXT");

		var totalTooltip = $("#currency_order_total_tooltip");
      if (units != "1") {
			totalTooltip.show();
			totalTooltip.popover("destroy");
			totalTooltip.data("content", tooltipTitle);
			totalTooltip.popover({
				"content": tooltipTitle,
				"trigger": "hover"
			});
		} else {
			totalTooltip.hide();
		}

		$("#currency_order_type").val((exchangeType == "buy" ? "currencyBuy" : "currencySell"));
		$("#currency_order_currency").val(currencyId);
		$("#currency_order_units").val(unitsQNT.toString());
		$("#currency_order_rate").val(rateNQT.toString());
		$("#currency_order_fee").val(feeNQT.toString());
	});
	
	NRS.forms.orderCurrency = function() {
		var orderType = $("#currency_order_type").val();

		return {
			"requestType": orderType,
			"successMessage": (orderType == "currencyBuy" ? $.t("success_buy_order_currency") : $.t("success_sell_order_currency")),
			"errorMessage": $.t("error_order_currency")
		};
	};
	
	$("#buy_currency_units_initial, #buy_currency_units_total, #buy_currency_rate, #sell_currency_units_initial, #sell_currency_units_total, #sell_currency_rate").keydown(function(e) {
		var decimals = parseInt($("#publish_exchange_offer_decimals").val(), 10);

		var charCode = !e.charCode ? e.which : e.charCode;
		if (NRS.isControlKey(charCode) || e.ctrlKey || e.metaKey) {
			return;
		}
		var isUnitsField = /_units/i.test($(this).attr("id"));
		var maxFractionLength = (isUnitsField ? decimals : 8 - decimals);
		return NRS.validateDecimals(maxFractionLength, charCode, $(this).val(), e);
	});

	var currencyUnitsAndRate = $("#sell_currency_units, #sell_currency_price, #buy_currency_units, #buy_currency_price");
	currencyUnitsAndRate.keydown(function(e) {
		var decimals = parseInt($("#currency_decimals").html(), 10);

		var charCode = !e.charCode ? e.which : e.charCode;
		if (NRS.isControlKey(charCode) || e.ctrlKey || e.metaKey) {
			return;
		}
		var isUnitsField = /_units/i.test($(this).attr("id"));
		var maxFractionLength = (isUnitsField ? decimals : 8 - decimals);
		return NRS.validateDecimals(maxFractionLength, charCode, $(this).val(), e);
	});

	//Calculate preview price (calculated on every keypress)
	currencyUnitsAndRate.keyup(function() {
		var currencyDecimals = parseInt($("#currency_decimals").text(), 10);
		var orderType = $(this).data("type").toLowerCase();

		try {
			var units = new BigInteger(NRS.convertToQNT(String($("#" + orderType + "_currency_units").val()), currencyDecimals));
			var priceNQT = new BigInteger(NRS.calculatePricePerWholeQNT(NRS.convertToNQT(String($("#" + orderType + "_currency_price").val())), currencyDecimals));

			if (priceNQT.toString() == "0" || units.toString() == "0") {
				$("#" + orderType + "_currency_total").val("0");
			} else {
				var total = NRS.calculateOrderTotal(units, priceNQT, currencyDecimals);
				$("#" + orderType + "_currency_total").val(total.toString());
			}
		} catch (err) {
			$("#" + orderType + "_currency_total").val("0");
		}
	});
	
	/* CURRENCIES PAGE */
	NRS.pages.currencies = function() {
		if (NRS.currenciesPageType == "my_currencies") {
			NRS.sendRequest("getAccountCurrencies+", {
				"account": NRS.accountRS,
				"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
				"lastIndex": NRS.pageNumber * NRS.itemsPerPage
			}, function(response) {
				if (response.accountCurrencies && response.accountCurrencies.length) {
					if (response.accountCurrencies.length > NRS.itemsPerPage) {
						NRS.hasMorePages = true;
						response.accountCurrencies.pop();
					}
					var rows = "";
					for (var i = 0; i < response.accountCurrencies.length; i++) {
						var currency = response.accountCurrencies[i];
						var currencyId = String(currency.currency).escapeHTML();
						var code = String(currency.code).escapeHTML();
						var name = String(currency.name).escapeHTML();
						var decimals = String(currency.decimals).escapeHTML();
						var typeIcons = NRS.getTypeIcons(currency.type);
						var isOfferEnabled = NRS.isExchangeable(currency.type) && (!NRS.isControllable(currency.type) || NRS.account == currency.issuerAccount);
						rows += "<tr>" +
							"<td>" +
								"<a href='#' data-transaction='" + currencyId + "' >" + code + "</a>" +
							"</td>" +
							"<td>" + currency.name + "</td>" +
							"<td>" + typeIcons + "</td>" +
							"<td>" + NRS.formatQuantity(currency.unconfirmedUnits, currency.decimals) + "</td>" +
							"<td>" +
								"<a href='#' class='btn btn-xs btn-default' onClick='NRS.goToCurrency(&quot;" + code + "&quot;)' " + (!NRS.isExchangeable(currency.type) ? "disabled" : "") + ">" + $.t("exchange") + "</a> " +
								"<a href='#' class='btn btn-xs btn-default' data-toggle='modal' data-target='#transfer_currency_modal' data-currency='" + String(currency.currency).escapeHTML() + "' data-code='" + code + "' data-decimals='" + decimals + "'>" + $.t("transfer") + "</a> " +
								"<a href='#' class='btn btn-xs btn-default' data-toggle='modal' data-target='#publish_exchange_offer_modal' data-currency='" + String(currency.currency).escapeHTML() + "' data-code='" + code + "' data-decimals='" + decimals + "' " + (isOfferEnabled ? "" : "disabled") + " >" + $.t("offer") + "</a> " +
              				"<a href='#' class='btn btn-xs btn-default' data-toggle='modal' data-target='#claim_currency_modal' data-currency='" + currencyId + "' data-name='" + name + "' data-code='" + code + "' data-decimals='" + decimals + "' " + (currency.issuanceHeight <= NRS.lastBlockHeight && NRS.isClaimable(currency.type) ? "" : "disabled") + " >" + $.t("claim") + "</a> " +
							"</td>" +
						"</tr>";
					}
					var currenciesTable = $('#currencies_table');
					currenciesTable.find('[data-i18n="type"]').show();
					currenciesTable.find('[data-i18n="supply"]').hide();
					currenciesTable.find('[data-i18n="max_supply"]').hide();
					currenciesTable.find('[data-i18n="units"]').show();
					NRS.dataLoaded(rows);
				} else {
					NRS.dataLoaded();
				}
			});
		} else {
			NRS.sendRequest("getAllCurrencies+", {
				"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
				"lastIndex": NRS.pageNumber * NRS.itemsPerPage
			}, function(response) {
				if (response.currencies && response.currencies.length) {
					if (response.currencies.length > NRS.itemsPerPage) {
						NRS.hasMorePages = true;
						response.currencies.pop();
					}
					var rows = NRS.getCurrencyRows(response);
					NRS.currenciesTableLayout();
					NRS.dataLoaded(rows);
				} else {
					NRS.dataLoaded();
				}
			});
		}
	};
	
	$("#currencies_page_type").find(".btn").click(function(e) {
		e.preventDefault();
		NRS.currenciesPageType = $(this).data("type");

		var currenciesTable = $("#currencies_table");
		currenciesTable.find("tbody").empty();
		currenciesTable.parent().addClass("data-loading").removeClass("data-empty");
		NRS.loadPage("currencies");
	});
	
	$("#ms_exchange_history_type").find(".btn").click(function(e) {
		e.preventDefault();
		NRS.currenciesTradeHistoryType = $(this).data("type");

		var exchangeHistoryTable = $("#ms_exchanges_history_table");
		exchangeHistoryTable.find("tbody").empty();
		exchangeHistoryTable.parent().addClass("data-loading").removeClass("data-empty");
		NRS.getExchangeHistory($("#currency_id").html(), false);
	});

	$("body").on("click", "a[data-goto-currency]", function(e) {
		e.preventDefault();

		var $visible_modal = $(".modal.in");

		if ($visible_modal.length) {
			$visible_modal.modal("hide");
		}

		NRS.goToCurrency($(this).data("goto-currency"));
	});

	NRS.goToCurrency = function(currency) {
		var currencySearch = $("#currency_search");
		currencySearch.find("input[name=q]").val(currency);
		currencySearch.submit();
		$("ul.sidebar-menu a[data-page=monetary_system]").last().trigger("click");
	};
	
	/* Transfer Currency Model */
	$("#transfer_currency_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var currency = $invoker.data("currency");
		var currencyCode = $invoker.data("code");
		var decimals = $invoker.data("decimals");

		$("#transfer_currency_currency").val(currency);
		$("#transfer_currency_decimals").val(decimals);
		$("#transfer_currency_code, #transfer_currency_units_code").html(String(currencyCode).escapeHTML());
		$("#transfer_currency_available").html("");
		
		NRS.sendRequest("getAccountCurrencies", {
			"currency": currency,
			"account": NRS.accountRS
		}, function(response) {
			var availablecurrencysMessage = " - None Available for Transfer";
			if (response.unconfirmedUnits && response.unconfirmedUnits != "0") {
				availablecurrencysMessage = " - " + $.t("available_units") + " " + NRS.formatQuantity(response.unconfirmedUnits, response.decimals);
			}
			$("#transfer_currency_available").html(availablecurrencysMessage);
		})
	});
	
	/* Publish Exchange Offer Model */
	$("#publish_exchange_offer_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		$("#publish_exchange_offer_currency").val($invoker.data("currency"));
		$("#publish_exchange_offer_decimals").val($invoker.data("decimals"));
		$(".currency_code").html(String($invoker.data("code")).escapeHTML());

		NRS.sendRequest("getAccountCurrencies", {
			"currency": $invoker.data("currency"),
			"account": NRS.accountRS
		}, function(response) {
			var availablecurrencysMessage = " - None Available";
			if (response.unconfirmedUnits && response.unconfirmedUnits != "0") {
				availablecurrencysMessage = " - " + $.t("available_units") + " " + NRS.formatQuantity(response.unconfirmedUnits, response.decimals);
			}
			$("#publish_exchange_available").html(availablecurrencysMessage);
		})

	});

	/* EXCHANGE HISTORY PAGE */
	NRS.pages.exchange_history = function() {
		NRS.sendRequest("getExchanges+", {
			"account": NRS.accountRS,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		}, function(response) {
			if (response.exchanges && response.exchanges.length) {
				if (response.exchanges.length > NRS.itemsPerPage) {
					NRS.hasMorePages = true;
					response.exchanges.pop();
				}
				var rows = "";
				for (var i = 0; i < response.exchanges.length; i++) {
				 	var exchange = response.exchanges[i];
					rows += "<tr>" +
						"<td>" + NRS.formatTimestamp(exchange.timestamp) + "</td>" +
						"<td>" +
							"<a href='#' data-transaction='" + String(exchange.transaction).escapeHTML() + "'>" + String(exchange.transaction).escapeHTML() + "</a>" +
						"</td>" +
						"<td>" +
							"<a href='#' data-transaction='" + String(exchange.offer).escapeHTML() + "'>" + String(exchange.offer).escapeHTML() + "</a>" +
						"</td>" +
						"<td>" +
							"<a href='#' data-transaction='" + String(exchange.currency).escapeHTML() + "' >" + String(exchange.code).escapeHTML() + "</a>" +
						"</td>" +
						"<td>" +
							"<a href='#' class='user-info' data-user='" + exchange.sellerRS + "'>" + NRS.getAccountTitle(exchange.sellerRS) + "</a>" +
						"</td>" +
						"<td>" +
							"<a href='#' class='user-info' data-user='" + exchange.buyerRS + "'>" + NRS.getAccountTitle(exchange.buyerRS) + "</a>" +
						"</td>" +
						"<td>" + NRS.formatQuantity(exchange.units, exchange.decimals) + "</td>" +
						"<td>" + NRS.calculateOrderPricePerWholeQNT(exchange.rateNQT, exchange.decimals) + "</td>" +
						"<td>" + NRS.formatAmount(NRS.calculateOrderTotalNQT(exchange.units, exchange.rateNQT, exchange.decimals)) + "</td>" +
					"</tr>";
				}
				NRS.dataLoaded(rows);
			} else {
				NRS.dataLoaded();
			}
		});
	};
	
	/* Calculate correct fees based on currency code length */
	var issueCurrencyCode = $("#issue_currency_code");
	issueCurrencyCode.keyup(function() {
		if(issueCurrencyCode.val().length < 4){
			$("#issue_currency_fee").val("25000");
			$("#issue_currency_modal").find(".advanced_fee").html("25'000 NXT");
		} else if($("#issue_currency_code").val().length == 4){
			$("#issue_currency_fee").val("1000");
			$("#issue_currency_modal").find(".advanced_fee").html("1'000 NXT");
		} else {
			$("#issue_currency_fee").val("40");
			$("#issue_currency_modal").find(".advanced_fee").html("40 NXT");
		}
		this.value = this.value.toLocaleUpperCase();
	});

	issueCurrencyCode.blur(function() {
		if(issueCurrencyCode.val().length < 4){
			$("#issue_currency_fee").val("25000");
			$("#issue_currency_moda").find(".advanced_fee").html("25'000 NXT");
		} else if($("#issue_currency_code").val().length == 4){
			$("#issue_currency_fee").val("1000");
			$("#issue_currency_modal").find(".advanced_fee").html("1'000 NXT");
		} else {
			$("#issue_currency_fee").val("40");
			$("#issue_currency_modal").find(".advanced_fee").html("40 NXT");
		}
		this.value = this.value.toLocaleUpperCase();
	});
	
	/* ISSUE CURRENCY FORM */
	NRS.forms.issueCurrency = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		data.description = $.trim(data.description);
		if (data.minReservePerUnitNQT) {
			data.minReservePerUnitNQT = NRS.convertToNQT(data.minReservePerUnitNQT);
			data.minReservePerUnitNQT = NRS.convertToQNTf(data.minReservePerUnitNQT, data.decimals);
		}
		if (!data.initialSupply) {
			data.initialSupply = "0";
		}
		if (!data.reserveSupply) {
			data.reserveSupply = "0";
		}
		data.type = 0;
		$("[name='type']:checked").each(function() {
        	data.type += parseInt($(this).val(), 10);
    	});

		if (!data.description) {
			return {
				"error": $.t("error_description_required")
			};
		} else if (!data.name) {
			return {
				"error": $.t("error_name_required")
			};
		} else if (!data.code || data.code.length < 3) {
			return {
				"error": $.t("error_code_required")
			};
		} else if (!data.maxSupply || data.maxSupply < 1) {
			return {
				"error": $.t("error_type_supply")
			};
		} else if (!/^\d+$/.test(data.maxSupply) || !/^\d+$/.test(data.initialSupply)|| !/^\d+$/.test(data.reserveSupply)) {
			return {
				"error": $.t("error_whole_units")
			};
		} else {
			try {
				data.maxSupply = NRS.convertToQNT(data.maxSupply, data.decimals);
				data.initialSupply = NRS.convertToQNT(data.initialSupply, data.decimals);
				data.reserveSupply = NRS.convertToQNT(data.reserveSupply, data.decimals);
			} catch (e) {
				return {
					"error": $.t("error_whole_units")
				};
			}
			return {
				"data": data
			};
		}
	};

	/* TRANSFER CURRENCY FORM */
	NRS.forms.transferCurrency = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));
		var decimals = parseInt(data.decimals, 10);
		if (!data.units) {
			return {
				"error": $.t("error_not_specified", {
					"name": NRS.getTranslatedFieldName("units").toLowerCase()
				}).capitalize()
			};
		}

		if (!NRS.showedFormWarning) {
			if (NRS.settings["currency_transfer_warning"] && NRS.settings["currency_transfer_warning"] != 0) {
				if (new Big(data.units).cmp(new Big(NRS.settings["currency_transfer_warning"])) > 0) {
					NRS.showedFormWarning = true;
					return {
						"error": $.t("error_max_currency_transfer_warning", {
							"qty": String(NRS.settings["currency_transfer_warning"]).escapeHTML()
						})
					};
				}
			}
		}

		try {
			data.units = NRS.convertToQNT(data.units, decimals);
		} catch (e) {
			return {
				"error": $.t("error_incorrect_units_plus", {
					"err": e.escapeHTML()
				})
			};
		}

		delete data.decimals;

		if (!data.add_message) {
			delete data.add_message;
			delete data.message;
			delete data.encrypt_message;
		}

		return {
			"data": data
		};
	};

	$('#issue_currency_reservable').change(function () {
		var issuanceHeight = $("#issue_currency_issuance_height");
      if ($(this).is(":checked")) {
			$(".optional_reserve").show();
			issuanceHeight.val("");
			issuanceHeight.prop("disabled", false);
			$(".optional_reserve input").prop("disabled", false);
		} else {
			$(".optional_reserve").hide();
			$(".optional_reserve input").prop("disabled", true);
			issuanceHeight.val(0);
			issuanceHeight.prop("disabled", true);
		}
	});

	$('#issue_currency_claimable').change(function () {
		if ($(this).is(":checked")) {
			$("#issue_currency_initial_supply").val(0);
			$("#issue_currency_issuance_height").prop("disabled", false);
			$(".optional_reserve").show();
			$('#issue_currency_reservable').prop('checked', true);
			$("#issue_currency_min_reserve").prop("disabled", false);
			$("#issue_currency_min_reserve_supply").prop("disabled", false);
		} else {
			$("#issue_currency_initial_supply").val($("#issue_currency_max_supply").val());
		}
	});

	$('#issue_currency_mintable').change(function () {
		if ($(this).is(":checked")) {
			$(".optional_mint").show();
			$(".optional_mint input").prop("disabled", false);
		} else {
			$(".optional_mint").hide();
			$(".optional_mint input").prop("disabled", true);
		}
	});

	/* PUBLISH EXCHANGE OFFER MODEL */
	NRS.forms.publishExchangeOffer = function ($modal) {
		var data = NRS.getFormData($modal.find("form:first"));
		var decimals = parseInt(data.decimals, 10);
		data.initialBuySupply = NRS.convertToQNT(data.initialBuySupply, decimals);
		data.totalBuyLimit = NRS.convertToQNT(data.totalBuyLimit, decimals);
		data.buyRateNQT = NRS.calculatePricePerWholeQNT(NRS.convertToNQT(data.buyRateNQT), decimals);
		data.initialSellSupply = NRS.convertToQNT(data.initialSellSupply, decimals);
		data.totalSellLimit = NRS.convertToQNT(data.totalSellLimit, decimals);
		data.sellRateNQT = NRS.calculatePricePerWholeQNT(NRS.convertToNQT(data.sellRateNQT), decimals);
		return {
			"data": data
		};
	};

	/* DELETE CURRENCY MODEL */
	$("#delete_currency_modal").on("show.bs.modal", function (e) {
		var $invoker = $(e.relatedTarget);

		var currency = $invoker.data("currency");
		var currencyCode = $invoker.data("code");

		$("#delete_currency_currency").val(currency);
		$("#delete_currency_code").html(String(currencyCode).escapeHTML());
		$("#delete_currency_modal").find(".error_message").html("Once submitted, this cannot be undone").show();
	});

	/* RESERVE CURRENCY MODEL */
	$("#reserve_currency_modal").on("show.bs.modal", function (e) {
		var $invoker = $(e.relatedTarget);
		var currency = $invoker.data("currency");
		var currencyCode = $invoker.data("code");
		NRS.sendRequest("getCurrency+", {
			"currency": currency
		}, function(response) {
			if (response && !response.errorDescription) {
				var currency = response.currency;
				var decimals = response.decimals;
				var minReserve = response.minReservePerUnitNQT;
				var currentReserve = response.currentReservePerUnitNQT;
				var resSupply = response.reserveSupply;
				var initialSupply = response.initialSupply;

				$("#reserve_currency_code").html(String(currencyCode).escapeHTML());
				$("#reserve_currency_currency").val(currency);
				$("#reserve_currency_decimals").val(decimals);
				$("#reserve_currency_minReserve").val(minReserve);
				var minReservePerUnitNQT = new BigInteger(minReserve).multiply(new BigInteger("" + Math.pow(10, decimals)));
				$("#reserve_currency_minReserve_text").html(NRS.formatAmount(minReservePerUnitNQT));
				$("#reserve_currency_currentReserve").val(currentReserve);
				var currentReservePerUnitNQT = new BigInteger(currentReserve).multiply(new BigInteger("" + Math.pow(10, decimals)));
				$("#reserve_currency_currentReserve_text").html(NRS.formatAmount(currentReservePerUnitNQT));
				$("#reserve_currency_resSupply").val(resSupply);
				$("#reserve_currency_resSupply_text").html(NRS.formatQuantity(resSupply, decimals));
				$("#reserve_currency_initialSupply_text").html(NRS.formatQuantity(initialSupply, decimals));
			}
		})
	});

	var reserveCurrencyAmount = $("#reserve_currency_amount");
	reserveCurrencyAmount.keydown(function(e) {
		var decimals = parseInt($("#reserve_currency_decimals").val(), 10);

		var charCode = !e.charCode ? e.which : e.charCode;
		if (NRS.isControlKey(charCode) || e.ctrlKey || e.metaKey) {
			return;
		}
		return NRS.validateDecimals(8-decimals, charCode, $(this).val(), e);
	});

	reserveCurrencyAmount.blur(function() {
		var resSupply = NRS.convertToQNTf($("#reserve_currency_resSupply").val(), $("#reserve_currency_decimals").val());
		var unitAmountNQT = NRS.convertToNQT(this.value);
		$("#reserve_currency_total").html(NRS.formatAmount(NRS.calculateOrderTotalNQT(unitAmountNQT, resSupply)));
	});

	NRS.forms.currencyReserveIncrease = function ($modal) {
		var data = NRS.getFormData($modal.find("form:first"));
		var decimals = parseInt(data.decimals, 10);
		data.amountPerUnitNQT = NRS.calculatePricePerWholeQNT(NRS.convertToNQT(data.amountPerUnitNQT), decimals);

		return {
			"data": data
		};
	};

	/* CLAIM CURRENCY MODEL */
	$("#claim_currency_modal").on("show.bs.modal", function (e) {
		var $invoker = $(e.relatedTarget);

		var currency = $invoker.data("currency");
		var currencyCode = $invoker.data("code");

		NRS.sendRequest("getAccountCurrencies", {
			"currency": currency,
			"account": NRS.accountRS
		}, function (response) {
			var availableUnits = "0";
			if (response.units) {
				availableUnits = NRS.formatQuantity(response.units, response.decimals);
			}
			$("#claimAvailable").html(availableUnits);
		});

		NRS.sendRequest("getCurrency", {
			"currency": currency
		}, function (response) {
			var currentReservePerUnitNQT = new BigInteger(response.currentReservePerUnitNQT).multiply(new BigInteger("" + Math.pow(10, response.decimals)));
			$("#claimRate").html(NRS.formatAmount(currentReservePerUnitNQT) + " [NXT/" + currencyCode + "]");
		});

		$("#claim_currency_decimals").val($invoker.data("decimals"));
		$("#claim_currency_currency").val(currency);
		$("#claim_currency_code").html(String(currencyCode).escapeHTML());

	});
	
	$("#claim_currency_amount").keydown(function(e) {
		var decimals = parseInt($("#claim_currency_decimals").val(), 10);

		var charCode = !e.charCode ? e.which : e.charCode;
		if (NRS.isControlKey(charCode) || e.ctrlKey || e.metaKey) {
			return;
		}
		return NRS.validateDecimals(decimals, charCode, $(this).val(), e);
	});

	/* Respect decimal positions on claiming a currency */
	NRS.forms.currencyReserveClaim = function ($modal) {
		var data = NRS.getFormData($modal.find("form:first"));
		var decimals = parseInt(data.decimals, 10);
		data.units = NRS.convertToQNT(data.units, decimals);

		return {
			"data": data
		};
	};

	/* MINT CURRENCY MODEL */
	$("#mine_currency_modal").on("show.bs.modal", function (e) {
		var $invoker = $(e.relatedTarget);

		var currency = $invoker.data("currency");
		var currencyCode = $invoker.data("code");
		var decimals = $invoker.data("decimals");

		$("#mine_currency_currency").val(currency);
		$("#mine_currency_code").html(String(currencyCode).escapeHTML());
		$("#mine_currency_decimals").val(decimals);

	});

	$("#mine_currency_units, #mine_currency_counter, #mine_currency_nonce").keydown(function(e) {
		var decimals = parseInt($("#mine_currency_decimals").val(), 10);

		var charCode = !e.charCode ? e.which : e.charCode;
		if (NRS.isControlKey(charCode) || e.ctrlKey || e.metaKey) {
			return;
		}
		var isUnitsField = /_units/i.test($(this).attr("id"));
		var maxFractionLength = (isUnitsField ? decimals : 0); // decimals not allowed for nonce and counter
		return NRS.validateDecimals(maxFractionLength, charCode, $(this).val(), e);
	});

	/* Fill in counter field after units is inputed */
	$("#mine_currency_units").blur(function() {
		NRS.sendRequest("getMintingTarget", {
			"currency": $("#mine_currency_currency").val(),
			"account": NRS.accountRS,
			"units": NRS.convertToQNT(this.value, $("#mine_currency_decimals").val())
		}, function(response) {
			var currencyModal = $("#mine_currency_modal");
         if (response && !response.errorCode){
				currencyModal.find(".error_message").hide();
				$("#mine_currency_counter").val(response.counter+1);
				$("#mine_currency_difficulty").val(response.difficulty);
			} else if (response.errorCode){
				currencyModal.find(".error_message").html(response.errorDescription);
				currencyModal.find(".error_message").show();
			}
		})
	});

	/* Respect decimal positions on claiming a currency */
	NRS.forms.currencyMint = function ($modal) {
		var data = NRS.getFormData($modal.find("form:first"));
		var decimals = parseInt(data.decimals, 10);
		data.units = NRS.convertToQNT(data.units, decimals);

		return {
			"data": data
		};
	};


   return NRS;
}(NRS || {}, jQuery));