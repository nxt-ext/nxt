/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {

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
					rows += "<tr><td><a href='#' data-transaction='" + String(currency.currency).escapeHTML() + "'>" + String(currency.currency).escapeHTML() + "</a></td>" +
                  "<td>" + currency.name + "</td>" +
                  "<td>" + currency.code + "</td>" +
                  "<td>" + currency.type + "</td>" +
                  "</tr>";
				}
				NRS.dataLoaded(rows);
			} else {
				NRS.dataLoaded();
			}
		});
	};

   return NRS;
}(NRS || {}, jQuery));