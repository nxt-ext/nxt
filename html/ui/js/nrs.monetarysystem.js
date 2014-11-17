/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {

	/* MONETARY SYSTEM PAGE */
	$("#currency_search").on("submit", function(e) {
		e.preventDefault();
		var currencyCode = $.trim($("#currency_search input[name=q]").val());
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
					rows += "";
				}
				$("#ms_exchanges_history_table tbody").empty().append(rows);
			} else {
				$("#ms_exchanges_history_table tbody").empty();
			}
			NRS.dataLoadFinished($("#ms_exchanges_history_table"), true);
		});
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
					rows += "";
				}
				$("#ms_my_exchanges_history_table tbody").empty().append(rows);
			} else {
				$("#ms_my_exchanges_history_table tbody").empty();
			}
			NRS.dataLoadFinished($("#ms_my_exchanges_history_table"), true);
		});
		NRS.pageLoaded();
	});
	
	/* Monetary System Page Search capitalization */
    $("#currency_search input[name=q]").blur(function(e) {
		this.value = this.value.toLocaleUpperCase();
	});
	$("#currency_search input[name=q]").keyup(function(e) {
		this.value = this.value.toLocaleUpperCase();
	});

	/* CURRENCIES PAGE */
	NRS.pages.currencies = function() {
		NRS.sendRequest("getAllCurrencies+", {
			"account": NRS.accountRS,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		}, function(response, input) {
			if (response.currencies && response.currencies.length) {
				if (response.currencies.length > NRS.itemsPerPage) {
					NRS.hasMorePages = true;
					response.currencies.pop();
				}
				var rows = "";
				for (var i = 0; i < response.currencies.length; i++) {
               		var currency = response.currencies[i];
               		if (currency.type == 1)
               			currency.type = "Exchangeable";
               		else if (currency.type == 2)
               			currency.type = "Controllable";
               		else if (currency.type == 4)
               			currency.type = "Reservable";
               		else if (currency.type == 8)
               			currency.type = "Claimable";
               		else if (currency.type == 10)
               			currency.type = "Mintable";
               		else if (currency.type == 20)
               			currency.type = "Shuffleable";
					rows += "<tr><td><a href='#' onClick='NRS.goToCurrency(&quot;" + String(currency.code) + "&quot;)' >" + String(currency.currency).escapeHTML() + "</a></td>" +
                  		"<td>" + currency.name + "</td>" +
                  		"<td>" + currency.code + "</td>" +
                  		"<td>" + currency.type + "</td>" +
                  		"<td>" + currency.currentSupply + "</td>" +
                  		"<td><a href='#' data-toggle='modal' data-target='#transfer_currency_modal' data-currency='" + String(currency.code).escapeHTML() + "' data-name='" + String(currency.name).escapeHTML() + "' data-decimals='" + String(currency.decimals).escapeHTML() + "'>" + $.t("transfer") + "</a></td>" +
                  		"</tr>";
				}
				NRS.dataLoaded(rows);
			} else {
				NRS.dataLoaded();
			}
		});
	};
	
	NRS.goToCurrency = function(currency) {
		$("#currency_search input[name=q]").val(currency);
		$("#currency_search").submit();
		$("ul.sidebar-menu a[data-page=monetary_system]").last().trigger("click");
	};
	
	$("#transfer_currency_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var currencyCode = $invoker.data("currency");
		var currencyName = $invoker.data("name");
		var decimals = $invoker.data("decimals");

		$("#transfer_currency_currency").val(currencyCode);
		$("#transfer_currency_decimals").val(decimals);
		$("#transfer_currency_name, #transfer_currency_quantity_name").html(String(currencyName).escapeHTML());
		$("#transfer_currency_available").html("");
		
		NRS.sendRequest("getCurrencyAccounts+", {
			"code": currencyCode,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		}, function(response, input) {
			availablecurrencysMessage = " - None Available for Transfer";
			if (response.accountCurrencies && response.accountCurrencies.length) {
				if (response.accountCurrencies && response.accountCurrencies.length) {
					if (response.accountCurrencies.length > NRS.itemsPerPage) {
						NRS.hasMorePages = true;
						response.accountCurrencies.pop();
					}
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
					rows += "<tr><td><a href='#' data-transaction='" + String(exchange.exchange).escapeHTML() + "'>" + String(exchange.exchange).escapeHTML() + "</a></td>" +
                  "<td>" + exchange.name + "</td>" +
                  "<td>" + exchange.code + "</td>" +
                  "<td>" + exchange.type + "</td>" +
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
		$("#issue_currency_initial_supply").val($("#issue_currency_max_supply").val());
	});
	$("#issue_currency_max_supply").blur(function(e) {
		$("#issue_currency_initial_supply").val($("#issue_currency_max_supply").val());
	});
	
	NRS.forms.issueCurrency = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		data.description = $.trim(data.description);

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
	
	$('#issue_currency_exchangeable').change(function() {
        if($(this).is(":checked")){
            //$("#issue_currency_claimable").prop("disabled", true);
            $("#issue_currency_issuance_height").val(0)
            $("#issue_currency_issuance_height").prop("disabled", true);
        }
		else{
			//$("#issue_currency_claimable").prop("disabled", false);
			$("#issue_currency_issuance_height").val("")
			$("#issue_currency_issuance_height").prop("disabled", false);
		}
    });
    $('#issue_currency_claimable').change(function() {
        //if($(this).is(":checked"))
            //$( "#issue_currency_exchangeable" ).prop("disabled", true);
		//else
			//$( "#issue_currency_exchangeable" ).prop("disabled", false);
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
    
    /* Publish Exchange Offer Model Code */
    $("#currency_code").blur(function(e) {
		this.value = this.value.toLocaleUpperCase();
	});
	$("#currency_code").keyup(function(e) {
		this.value = this.value.toLocaleUpperCase();
	});
    

   return NRS;
}(NRS || {}, jQuery));