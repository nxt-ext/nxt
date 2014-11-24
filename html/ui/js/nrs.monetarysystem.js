/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {

	/* MONETARY SYSTEM PAGE */
	$("#currency_search").on("submit", function(e) {
		e.preventDefault();
		$("#MSnoCode").hide();
		$("#MScode").show();
		var currencyCode = $.trim($("#currency_search input[name=q]").val());
		$("#buy_currency_with_nxt").html("Buy " + currencyCode + " with NXT");
		$("#sell_currency_with_nxt").html("Sell " + currencyCode + " for NXT");
		$(".currency_code").html(String(currencyCode).escapeHTML());
		$("#sell_currency_button").data("currency", currencyCode);
		$("#buy_currency_button").data("currency", currencyCode);
		
		NRS.sendRequest("getCurrency+", {
			"code": currencyCode
		}, function(response, input) {
			if (response) {
				$("#currency_account").html(String(response.accountRS).escapeHTML());
				$("#currency_id").html(String(response.currency).escapeHTML());
				$("#currency_name").html(String(response.name).escapeHTML());
				$("#currency_current_supply").html(String(response.currentSupply).escapeHTML());
				$("#currency_max_supply").html(String(response.maxSupply).escapeHTML());
				$("#currency_decimals").html(String(response.decimals).escapeHTML());
				$("#currency_description").html(String(response.description).escapeHTML());
				$("#buy_currency_button").data("decimals", response.decimals);
			}
		});
		
		NRS.sendRequest("getAccountCurrencies+", {
			"account": NRS.accountRS,
			"code": currencyCode
		}, function(response, input) {
			if (response) {
				$("#your_currency_balance").html(NRS.formatQuantity(response.units, response.decimals));
			}
		});
		
		NRS.sendRequest("getSellOffers+", {
			"code": currencyCode,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		}, function(response, input) {
			if (response.offers && response.offers.length) {
				if (response.offers.length > NRS.itemsPerPage) {
					NRS.hasMorePages = true;
					response.offers.pop();
				}
				var rows = "";
				for (var i = 0; i < response.offers.length; i++) {
                	var sellOffers = response.offers[i];
					if (i == 0)
						$("#buy_currency_price").val(NRS.calculateOrderPricePerWholeQNT(sellOffers.rateNQT,$("#currency_decimals").html()));
					rows += "<tr><td><a href='#' class='user-info' data-user='" + String(sellOffers.accountRS).escapeHTML() + "'>" + String(sellOffers.accountRS).escapeHTML() + "</a></td>" +
                  "<td>" + sellOffers.supply + "</td>" +
                  "<td>" + sellOffers.limit + "</td>" +
                  "<td>" + NRS.formatAmount(sellOffers.rateNQT) + "</td>" +
                  "</tr>";
				}
				$("#ms_open_sell_orders_table tbody").empty().append(rows);
			} else {
				$("#ms_open_sell_orders_table tbody").empty();
			}
			NRS.dataLoadFinished($("#ms_open_sell_orders_table"), true);
		});
		NRS.sendRequest("getBuyOffers+", {
			"code": currencyCode,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		}, function(response, input) {
			if (response.offers && response.offers.length) {
				if (response.offers.length > NRS.itemsPerPage) {
					NRS.hasMorePages = true;
					response.offers.pop();
				}
				var rows = "";
				for (var i = 0; i < response.offers.length; i++) {
                	var buyOffers = response.offers[i];
                	if (i == 0)
						$("#sell_currency_price").val(NRS.calculateOrderPricePerWholeQNT(buyOffers.rateNQT,$("#currency_decimals").html()));
					rows += "<tr><td><a href='#' class='user-info' data-user='" + String(buyOffers.accountRS).escapeHTML() + "'>" + String(buyOffers.accountRS).escapeHTML() + "</a></td>" +
                  "<td>" + buyOffers.supply + "</td>" +
                  "<td>" + buyOffers.limit + "</td>" +
                  "<td>" + NRS.formatAmount(buyOffers.rateNQT) + "</td>" +
                  "</tr>";
				}
				$("#ms_open_buy_orders_table tbody").empty().append(rows);
			} else {
				$("#ms_open_buy_orders_table tbody").empty();
			}
			NRS.dataLoadFinished($("#ms_open_buy_orders_table"), true);
		});
		NRS.getExchangeHistory(currencyCode);
		if (NRS.accountInfo.unconfirmedBalanceNQT == "0") {
			$("#ms_your_nxt_balance").html("0");
			$("#buy_automatic_price").addClass("zero").removeClass("nonzero");
		} else {
			$("#ms_your_nxt_balance").html(NRS.formatAmount(NRS.accountInfo.unconfirmedBalanceNQT));
			$("#buy_automatic_price").addClass("nonzero").removeClass("zero");
		}
		NRS.pageLoaded();
	});
	
	NRS.getExchangeHistory = function(currencyCode) {
		if (NRS.currenciesTradeHistoryType == "my_exchanges") {
			NRS.sendRequest("getExchanges+", {
				"code": currencyCode,
				"account": NRS.accountRS,
				"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
				"lastIndex": NRS.pageNumber * NRS.itemsPerPage
			}, function(response, input) {
				if (response.exchanges && response.exchanges.length) {
					if (response.exchanges.length > NRS.itemsPerPage) {
						NRS.hasMorePages = true;
						response.exchanges.pop();
					}
					var rows = "";
					for (var i = 0; i < response.exchanges.length; i++) {
					var exchanges = response.exchanges[i];
						rows += "<tr><td>" + NRS.formatTimestamp(exchanges.timestamp) + "</td>" +
						"<td><a href='#' class='user-info' data-user='" + (exchanges.sellerRS == NRS.accountRS ? "You" : exchanges.sellerRS) + "'>" + (exchanges.sellerRS == NRS.accountRS ? "You" : exchanges.sellerRS) + "</a></td>" +
					  "<td><a href='#' class='user-info' data-user='" + (exchanges.buyerRS == NRS.accountRS ? "You" : exchanges.buyerRS) + "'>" + (exchanges.buyerRS == NRS.accountRS ? "You" : exchanges.buyerRS) + "</a></td>" +
					  "<td>" + NRS.formatQuantity(exchanges.units, exchanges.decimals) + "</td>" +
					  "<td>" + NRS.formatAmount(exchanges.rateNQT) + "</td>" +
					  "</tr>";
					}
					$("#ms_my_exchanges_history_table tbody").empty().append(rows);
				} else {
					$("#ms_my_exchanges_history_table tbody").empty();
				}
				NRS.dataLoadFinished($("#ms_exchanges_history_table"), true);
			});
		} else {
			NRS.sendRequest("getExchanges+", {
				"code": currencyCode,
				"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
				"lastIndex": NRS.pageNumber * NRS.itemsPerPage
			}, function(response, input) {
				if (response.exchanges && response.exchanges.length) {
					if (response.exchanges.length > NRS.itemsPerPage) {
						NRS.hasMorePages = true;
						response.exchanges.pop();
					}
					var rows = "";
					for (var i = 0; i < response.exchanges.length; i++) {
					var exchanges = response.exchanges[i];
						rows += "<tr><td>" + NRS.formatTimestamp(exchanges.timestamp) + "</td>" +
						"<td><a href='#' class='user-info' data-user='" + (exchanges.sellerRS == NRS.accountRS ? "You" : exchanges.sellerRS) + "'>" + (exchanges.sellerRS == NRS.accountRS ? "You" : exchanges.sellerRS) + "</a></td>" +
					  "<td><a href='#' class='user-info' data-user='" + (exchanges.buyerRS == NRS.accountRS ? "You" : exchanges.buyerRS) + "'>" + (exchanges.buyerRS == NRS.accountRS ? "You" : exchanges.buyerRS) + "</a></td>" +
					  "<td>" + NRS.formatQuantity(exchanges.units, exchanges.decimals) + "</td>" +
					  "<td>" + NRS.formatAmount(exchanges.rateNQT) + "</td>" +
					  "</tr>";
					}
					$("#ms_exchanges_history_table tbody").empty().append(rows);
				} else {
					$("#ms_exchanges_history_table tbody").empty();
				}
				NRS.dataLoadFinished($("#ms_exchanges_history_table"), true);
			});
		}
	}
	
	/* Monetary System Auto Quantity TODO */
	$("#sell_automatic_currency_price, #buy_automatic_currency_price").on("click", function(e) {
		try {
			
			var type = ($(this).attr("id") == "sell_automatic_currency_price" ? "sell" : "buy");
			var currencyBalance = parseFloat($("#your_currency_balance").html().replace("'",""));
			
			var price = new Big(NRS.convertToNQT(String($("#" + type + "_currency_price").val())));
			var balance = new Big(type == "buy" ? NRS.accountInfo.unconfirmedBalanceNQT : currencyBalance);
			var balanceNQT = new Big(NRS.accountInfo.unconfirmedBalanceNQT);
			var maxQuantity = new Big(NRS.convertToQNT(currencyBalance, $("#currency_decimals").html()));

			if (balance.cmp(new Big("0")) <= 0) {
				return;
			}

			/*if (price.cmp(new Big("0")) <= 0) {
				//get minimum price if no offers exist, based on currency decimals..
				price = new Big("" + Math.pow(10, $("#currency_decimals").html()));
				$("#" + type + "_currency_price").val(NRS.convertToNXT(price.toString()));
			}*/

			var quantity = new Big(NRS.amountToPrecision((type == "sell" ? balanceNQT : balance).div(price).toString(), $("#currency_decimals").html()));

			var total = quantity.times(price);

			//proposed quantity is bigger than available quantity
			if (quantity.cmp(maxQuantity) == 1) {
				quantity = maxQuantity;
				total = quantity.times(price);
			}

			if (type == "sell") {
				var maxUserQuantity = new Big(NRS.convertToQNTf(balance, $("#currency_decimals").html()));
				if (quantity.cmp(maxUserQuantity) == -1) {
					quantity = maxUserQuantity;
					total = quantity.times(price);
				}
			}

			$("#" + type + "_currency_quantity").val(quantity.toString());
			$("#" + type + "_currency_total").val(NRS.convertToNXT(total.toString()));

			$("#" + type + "_currency_total").css({
				"background": "",
				"color": ""
			});
		} catch (err) {}
	});
	
	/* Monetary System Page Search capitalization */
    $("#currency_search input[name=q]").blur(function(e) {
		this.value = this.value.toLocaleUpperCase();
	});
	$("#currency_search input[name=q]").keyup(function(e) {
		this.value = this.value.toLocaleUpperCase();
	});
	
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

		var orderType = $invoker.data("type");
		var currencyId = $invoker.data("currency");
		var currencyDecimals = $invoker.data("decimals");

		$("#currency_order_modal_button").html(orderType + " currency").data("resetText", orderType + " currency");

		orderType = orderType.toLowerCase();

		try {
			//TODO
			var quantity = String($("#" + orderType + "_currency_quantity").val());
			var quantityQNT = new BigInteger(NRS.convertToQNT(quantity, currencyDecimals));
			var priceNQT = new BigInteger(NRS.calculatePricePerWholeQNT(NRS.convertToNQT(String($("#" + orderType + "_currency_price").val())), currencyDecimals));
			var feeNQT = new BigInteger(NRS.convertToNQT(String($("#" + orderType + "_currency_fee").val())));
			var totalNXT = NRS.formatAmount(NRS.calculateOrderTotalNQT(quantityQNT, priceNQT, currencyDecimals), false, true);
		} catch (err) {
			$.growl($.t("error_invalid_input"), {
				"type": "danger"
			});
			return e.preventDefault();
		}

		if (priceNQT.toString() == "0" || quantityQNT.toString() == "0") {
			$.growl($.t("error_amount_price_required"), {
				"type": "danger"
			});
			return e.preventDefault();
		}

		if (feeNQT.toString() == "0") {
			feeNQT = new BigInteger("100000000");
		}

		var priceNQTPerWholeQNT = priceNQT.multiply(new BigInteger("" + Math.pow(10, currencyDecimals)));

		if (orderType == "buy") {
			var description = $.t("buy_currency_description", {
				"quantity": NRS.formatQuantity(quantityQNT, currencyDecimals, true),
				"currency_name": $("#currency_name").html().escapeHTML(),
				"nxt": NRS.formatAmount(priceNQTPerWholeQNT)
			});
			var tooltipTitle = $.t("buy_order_description_help", {
				"nxt": NRS.formatAmount(priceNQTPerWholeQNT, false, true),
				"total_nxt": totalNXT
			});
		} else {
			var description = $.t("sell_currency_description", {
				"quantity": NRS.formatQuantity(quantityQNT, currencyDecimals, true),
				"currency_name": $("#currency_name").html().escapeHTML(),
				"nxt": NRS.formatAmount(priceNQTPerWholeQNT)
			});
			var tooltipTitle = $.t("sell_order_description_help", {
				"nxt": NRS.formatAmount(priceNQTPerWholeQNT, false, true),
				"total_nxt": totalNXT
			});
		}

		$("#currency_order_description").html(description);
		$("#currency_order_total").html(totalNXT + " NXT");
		$("#currency_order_fee_paid").html(NRS.formatAmount(feeNQT) + " NXT");

		if (quantity != "1") {
			$("#currency_order_total_tooltip").show();
			$("#currency_order_total_tooltip").popover("destroy");
			$("#currency_order_total_tooltip").data("content", tooltipTitle);
			$("#currency_order_total_tooltip").popover({
				"content": tooltipTitle,
				"trigger": "hover"
			});
		} else {
			$("#currency_order_total_tooltip").hide();
		}

		$("#currency_order_type").val((orderType == "buy" ? "currencyBuy" : "currencySell"));
		$("#currency_order_code").val(currencyId);
		$("#currency_order_quantity").val(quantityQNT.toString());
		$("#currency_order_price").val(priceNQT.toString());
		$("#currency_order_fee").val(feeNQT.toString());
	});
	
	NRS.forms.orderCurrency = function($modal) {
		var orderType = $("#currency_order_type").val();

		return {
			"requestType": orderType,
			"successMessage": (orderType == "currencyBuy" ? $.t("success_buy_order_currency") : $.t("success_sell_order_currency")),
			"errorMessage": $.t("error_order_currency")
		};
	}
	
	//calculate preview price (calculated on every keypress)
	$("#sell_currency_quantity, #sell_currency_price, #buy_currency_quantity, #buy_currency_price").keyup(function(e) {
		var currencyDecimals = $('#currency_decimals').val();
		var orderType = $(this).data("type").toLowerCase();

		try {
			var quantityQNT = new BigInteger(NRS.convertToQNT(String($("#" + orderType + "_currency_quantity").val()), currencyDecimals));
			var priceNQT = new BigInteger(NRS.calculatePricePerWholeQNT(NRS.convertToNQT(String($("#" + orderType + "_currency_price").val())), currencyDecimals));

			if (priceNQT.toString() == "0" || quantityQNT.toString() == "0") {
				$("#" + orderType + "_currency_total").val("0");
			} else {
				var total = NRS.calculateOrderTotal(quantityQNT, priceNQT, currencyDecimals);
				$("#" + orderType + "_currency_total").val(total.toString());
			}
		} catch (err) {
			$("#" + orderType + "_currency_total").val("0");
		}
	});
	
	
	$("#ms_exchange_history_type .btn").click(function(e) {
		e.preventDefault();

		NRS.currenciesTradeHistoryType = $(this).data("type");

		NRS.getExchangeHistory($.trim($("#currency_search input[name=q]").val()));
	});

	/* CURRENCIES PAGE */
	NRS.pages.currencies = function() {
		if (NRS.currenciesPageType == "my_currencies") {
			NRS.sendRequest("getAccountCurrencies+", {
				"account": NRS.accountRS,
				"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
				"lastIndex": NRS.pageNumber * NRS.itemsPerPage
			}, function(response, input) {
				if (response.accountCurrencies && response.accountCurrencies.length) {
					if (response.accountCurrencies.length > NRS.itemsPerPage) {
						NRS.hasMorePages = true;
						response.accountCurrencies.pop();
					}
					var rows = "";
					for (var i = 0; i < response.accountCurrencies.length; i++) {
						var currency = response.accountCurrencies[i];						
						rows += "<tr><td><a href='#' onClick='NRS.goToCurrency(&quot;" + String(currency.code) + "&quot;)' >" + String(currency.currency).escapeHTML() + "</a></td>" +
							"<td>" + currency.name + "</td>" +
							"<td>" + currency.code + "</td>" +
							"<td>" + NRS.formatQuantity(currency.units, currency.decimals) + "</td>" +
							"<td><a href='#' data-toggle='modal' data-target='#transfer_currency_modal' data-currency='" + String(currency.currency).escapeHTML() + "' data-name='" + String(currency.name).escapeHTML() + "' data-decimals='" + String(currency.decimals).escapeHTML() + "'>" + $.t("transfer") + "</a></td>" +
							"</tr>";
					}
					$('#currencies_table [data-i18n="type"]').hide();
					$('#currencies_table [data-i18n="max_supply"]').hide();
					$('#currencies_table [data-i18n="supply"]').hide();
					$('#currencies_table [data-i18n="delete"]').hide();
					$('#currencies_table [data-i18n="reserve"]').hide();
					$('#currencies_table [data-i18n="claim"]').hide();
					$('#currencies_table [data-i18n="transfer"]').show();
					$('#currencies_table [data-i18n="units"]').show();
					NRS.dataLoaded(rows);
				} else {
					NRS.dataLoaded();
				}
			});
		} else {
			NRS.sendRequest("getAllCurrencies+", {
				"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
				"lastIndex": NRS.pageNumber * NRS.itemsPerPage
			}, function(response, input) {
				if (response.currencies && response.currencies.length) {
					if (response.currencies.length > NRS.itemsPerPage) {
						NRS.hasMorePages = true;
						response.currencies.pop();
					}
					var rows = "";
					$('#currencies_table [data-i18n="delete"]').hide();
					$('#currencies_table [data-i18n="reserve"]').hide();
					$('#currencies_table [data-i18n="claim"]').hide();
					for (var i = 0; i < response.currencies.length; i++) {
						var currency_type = "";
						var currency = response.currencies[i];
						if (currency.type & 0x1)
							currency_type += "Exchangeable<br />";
						if (currency.type & 0x2)
							currency_type += "Controllable<br />";
						if (currency.type & 0x4)
							currency_type += "Reservable<br />";
						if (currency.type & 0x8)
							currency_type += "Claimable<br />";
						if (currency.type & 0x10)
							currency_type += "Mintable<br />";
						if (currency.type & 0x20)
							currency_type += "Shuffleable";
						rows += "<tr><td><a href='#' onClick='NRS.goToCurrency(&quot;" + String(currency.code) + "&quot;)' >" + String(currency.currency).escapeHTML() + "</a></td>" +
						"<td>" + currency.name + "</td>" +
						"<td>" + currency.code + "</td>" +
						"<td>" + currency_type + "</td>" +
						"<td>" + NRS.formatQuantity(currency.currentSupply, currency.decimals) + "</td>" +
						"<td>" + NRS.formatQuantity(currency.maxSupply, currency.decimals) + "</td>";
						if (currency.accountRS==NRS.accountRS){
							rows += "<td><a href='#' data-toggle='modal' data-target='#delete_currency_modal' data-currency='" + String(currency.currency).escapeHTML() + "' data-name='" + String(currency.name).escapeHTML() + "' >" + $.t("delete") + "</a></td>";
							$('#currencies_table [data-i18n="delete"]').show();
						}
						if (currency.issuanceHeight > NRS.lastBlockHeight && (currency.type & 0x4)){
							rows += "<td><a href='#' data-toggle='modal' data-target='#reserve_currency_modal' data-currency='" + String(currency.currency).escapeHTML() + "' data-name='" + String(currency.name).escapeHTML() + "' >" + $.t("reserve") + "</a></td>";
							$('#currencies_table [data-i18n="reserve"]').show();
						}
						if (currency.issuanceHeight <= NRS.lastBlockHeight && (currency.type & 0x8)){
							rows += "<td><a href='#' data-toggle='modal' data-target='#claim_currency_modal' data-currency='" + String(currency.currency).escapeHTML() + "' data-name='" + String(currency.name).escapeHTML() + "' >" + $.t("claim") + "</a></td>";
							$('#currencies_table [data-i18n="claim"]').show();
						}
							
						rows += "</tr>";
					}
					$('#currencies_table [data-i18n="type"]').show();
					$('#currencies_table [data-i18n="max_supply"]').show();
					$('#currencies_table [data-i18n="supply"]').show();
					$('#currencies_table [data-i18n="transfer"]').hide();
					$('#currencies_table [data-i18n="units"]').hide();
					NRS.dataLoaded(rows);
				} else {
					NRS.dataLoaded();
				}
			});
		}
	};
	
	$("#currencies_page_type .btn").click(function(e) {
		e.preventDefault();

		NRS.currenciesPageType = $(this).data("type");

		$("#currencies_table tbody").empty();
		$("#currencies_table").parent().addClass("data-loading").removeClass("data-empty");

		NRS.loadPage("currencies");
	});
	
	NRS.goToCurrency = function(currency) {
		$("#currency_search input[name=q]").val(currency);
		$("#currency_search").submit();
		$("ul.sidebar-menu a[data-page=monetary_system]").last().trigger("click");
	};
	
	/* Transfer Currency Model */
	$("#transfer_currency_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var currency = $invoker.data("currency");
		var currencyName = $invoker.data("name");
		var decimals = $invoker.data("decimals");

		$("#transfer_currency_currency").val(currency);
		$("#transfer_currency_decimals").val(decimals);
		$("#transfer_currency_name, #transfer_currency_quantity_name").html(String(currencyName).escapeHTML());
		$("#transfer_currency_available").html("");
		
		NRS.sendRequest("getCurrencyAccounts+", {
			"currency": currency
		}, function(response, input) {
			availablecurrencysMessage = " - None Available for Transfer";
			if (response.accountCurrencies && response.accountCurrencies.length) {
				if (response.accountCurrencies && response.accountCurrencies.length) {
					for (var i = 0; i < response.accountCurrencies.length; i++) {
						if (response.accountCurrencies[i].accountRS == NRS.accountRS){
							availablecurrencysMessage = " - " + $.t("available_for_transfer", {
								"qty": NRS.formatQuantity(response.accountCurrencies[i].units, decimals)
							});
							break;
						}
					}
				}
			}
			$("#transfer_currency_available").html(availablecurrencysMessage);
		});
	});
	
	/* EXCHANGE HISTORY PAGE */
	NRS.pages.exchange_history = function() {
		NRS.sendRequest("getExchanges+", {
			"account": NRS.accountRS,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		}, function(response, input) {
			if (response.exchanges && response.exchanges.length) {
				if (response.exchanges.length > NRS.itemsPerPage) {
					NRS.hasMorePages = true;
					response.exchanges.pop();
				}
				var rows = "";
				for (var i = 0; i < response.exchanges.length; i++) {
                var exchange = response.exchanges[i];
					rows += "<tr><td><a href='#' data-transaction='" + String(exchange.transaction).escapeHTML() + "'>" + String(exchange.transaction).escapeHTML() + "</a></td>" +
					"<td>" + (exchange.sellerRS == NRS.accountRS ? "You" : "<a href='#' data-user='" + String(exchange.sellerRS).escapeHTML() + "'>" + String(exchange.sellerRS).escapeHTML() + "</a>") + "</td>" +
					"<td>" + (exchange.buyerRS == NRS.accountRS ? "You" : "<a href='#' data-user='" + String(exchange.buyerRS).escapeHTML() + "'>" + String(exchange.buyerRS).escapeHTML() + "</a>") + "</td>" +
                  	"<td>" + exchange.name + "</td>" +
                  	"<td>" + exchange.code + "</td>" +
                  	"<td>" + NRS.formatQuantity(exchange.units, exchange.decimals) + "</td>" +
                  	"<td>" + NRS.formatAmount(exchange.rateNQT) + "</td>" +
                  	"</tr>";
				}
				NRS.dataLoaded(rows);
			} else {
				NRS.dataLoaded();
			}
		});
	};
	
	/* Calculate correct fees based on currency code length */
	$("#issue_currency_code").keyup(function(e) {
		if($("#issue_currency_code").val().length < 4){
			$("#issue_currency_fee").val("25000");
			$("#issue_currency_modal .advanced_fee").html("25'000 NXT");
		}
		else if($("#issue_currency_code").val().length == 4){
			$("#issue_currency_fee").val("1000");
			$("#issue_currency_modal .advanced_fee").html("1'000 NXT");
		}
		else {
			$("#issue_currency_fee").val("40");
			$("#issue_currency_modal .advanced_fee").html("40 NXT");
		}
		this.value = this.value.toLocaleUpperCase();
	});
	$("#issue_currency_code").blur(function(e) {
		if($("#issue_currency_code").val().length < 4){
			$("#issue_currency_fee").val("25000");
			$("#issue_currency_modal .advanced_fee").html("25'000 NXT");
		}
		else if($("#issue_currency_code").val().length == 4){
			$("#issue_currency_fee").val("1000");
			$("#issue_currency_modal .advanced_fee").html("1'000 NXT");
		}
		else {
			$("#issue_currency_fee").val("40");
			$("#issue_currency_modal .advanced_fee").html("40 NXT");
		}
		this.value = this.value.toLocaleUpperCase();
	});
	
	/* Set initial supply to max supply (todo: this is not true for all the types) */
	$("#issue_currency_max_supply").keyup(function(e) {
		if (!$('#issue_currency_claimable').prop('checked'))
			$("#issue_currency_initial_supply").val($("#issue_currency_max_supply").val());
	});
	$("#issue_currency_max_supply").blur(function(e) {
		if (!$('#issue_currency_claimable').prop('checked'))
			$("#issue_currency_initial_supply").val($("#issue_currency_max_supply").val());
	});
	
	/* ISSUE CURRENCY FORM */
	NRS.forms.issueCurrency = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		data.description = $.trim(data.description);
		data.minReservePerUnitNQT = NRS.convertToNQT(data.minReservePerUnitNQT);
		
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
		} else {
			return {
				"data": data
			};
		}
	}
	
	$('#issue_currency_reservable').change(function() {
        if($(this).is(":checked")){
        	//$("#issue_currency_claimable").prop("disabled", false);
			$("#issue_currency_issuance_height").val("")
			$("#issue_currency_issuance_height").prop("disabled", false);
            
        }
		else{
			//$("#issue_currency_claimable").prop("disabled", true);
            $("#issue_currency_issuance_height").val(0)
            $("#issue_currency_issuance_height").prop("disabled", true);
		}
    });
    $('#issue_currency_claimable').change(function() {
        if($(this).is(":checked")){
        	$("#issue_currency_initial_supply").val(0);
        	//$("#issue_currency_initial_supply").prop("disabled", true);
        	$("#issue_currency_issuance_height").prop("disabled", false);
        	$(".optional_reserve").show();
        	$('#issue_currency_reservable').prop('checked', true);
        }
		else{
			$("#issue_currency_initial_supply").val($("#issue_currency_max_supply").val());
			//$("#issue_currency_initial_supply").prop("disabled", false);
			//$('#issue_currency_reservable').prop('checked', true);
		}
    });
	$('#issue_currency_reservable').change(function() {
        if($(this).is(":checked"))
            $( ".optional_reserve" ).show();
		else
			$( ".optional_reserve" ).hide();
    });
    $('#issue_currency_mintable').change(function() {
        if($(this).is(":checked"))
            $( ".optional_mint" ).show();
		else
			$( ".optional_mint" ).hide();
    });
    
    /* PUBLISH EXCHANGE OFFER MODEL */
    NRS.forms.publishExchangeOffer = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		data.buyRateNQT = NRS.convertToNQT(data.buyRateNQT);
		data.sellRateNQT = NRS.convertToNQT(data.sellRateNQT);
		data.expirationHeight = (parseInt(data.expirationHeight,10) * 60) + parseInt($("#nrs_current_block").html(),10);
		return {
			"data": data
		};
	}
    
    /* Capitalize Publish Exchange Offer Model Currency Code */
    $("#publish_exchange_offer_modal #currency_code").blur(function(e) {
		this.value = this.value.toLocaleUpperCase();
		$('#publish_exchange_offer_modal .currency_code').html(this.value);
	});
	$("#publish_exchange_offer_modal #currency_code").keyup(function(e) {
		this.value = this.value.toLocaleUpperCase();
		$('#publish_exchange_offer_modal .currency_code').html(this.value);
	});
    
    /* DELETE CURRENCY MODEL */
    $("#delete_currency_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var currency = $invoker.data("currency");
		var currencyName = $invoker.data("name");

		$("#delete_currency_currency").val(currency);
		$("#delete_currency_name").html(String(currencyName).escapeHTML());
		
	});
	
	/* RESERVE CURRENCY MODEL */
    $("#reserve_currency_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var currency = $invoker.data("currency");
		var currencyName = $invoker.data("name");

		$("#reserve_currency_currency").val(currency);
		$("#reserve_currency_name").html(String(currencyName).escapeHTML());
		
	});
	
	NRS.forms.currencyReserveIncrease = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		data.amountPerUnitNQT = NRS.convertToNQT(data.amountPerUnitNQT);

		return {
			"data": data
		};
	}
	
	/* CLAIM CURRENCY MODEL */
    $("#claim_currency_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var currency = $invoker.data("currency");
		var currencyName = $invoker.data("name");

		$("#claim_currency_currency").val(currency);
		$("#claim_currency_name").html(String(currencyName).escapeHTML());
		
	});

   return NRS;
}(NRS || {}, jQuery));