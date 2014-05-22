var NRS = (function(NRS, $, undefined) {
	NRS.pages.my_dgs_listings = function() {
		NRS.pageLoading();

		var params = {
			"account": NRS.account,
			"timestamp": 0,
			"type": 3,
			"subtype": 0
		};

		var rows = "";

		if (NRS.unconfirmedTransactions.length) {
			for (var j = 0; j < NRS.unconfirmedTransactions.length; j++) {
				var unconfirmedTransaction = NRS.unconfirmedTransactions[j];

				if (NRS.transactionsPageType) {
					if (unconfirmedTransaction.type != 3 || unconfirmedTransaction.subtype != 3) {
						continue;
					}
				}

				rows += "<tr class='tentative'><td><a href='#' data-toggle='modal' data-target='#dgs_listing_modal' data-id='" + String(unconfirmedTransaction.id).escapeHTML() + "'>" + String(unconfirmedTransaction.name).escapeHTML() + "</a></td><td>" + String(unconfirmedTransaction.tags).escapeHTML() + "</td><td>" + NRS.format(unconfirmedTransaction.quantity) + "</td><td>" + NRS.formatAmount(unconfirmedTransaction.priceNQT) + " NXT</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_price_change_modal' data-id='" + String(unconfirmedTransaction.id).escapeHTML() + "'>Change Price</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_quantity_change_modal' data-id='" + String(unconfirmedTransaction.id).escapeHTML() + "'>Change QTY</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_delisting_modal' data-id='" + String(unconfirmedTransaction.id).escapeHTML() + "'>Delete</a></td></tr>";
			}
		}

		NRS.sendRequest("getAccountTransactionIds+", params, function(response) {
			if (response.transactionIds && response.transactionIds.length) {
				var transactions = {};
				var nr_transactions = 0;

				var transactionIds = response.transactionIds.reverse().slice(0, 100);

				for (var i = 0; i < transactionIds.length; i++) {
					NRS.sendRequest("getTransaction+", {
						"transaction": transactionIds[i]
					}, function(transaction, input) {
						if (NRS.currentPage != "my_dgs_listings_table") {
							transactions = {};
							return;
						}

						transaction.transaction = input.transaction;
						transaction.confirmed = true;

						transactions[input.transaction] = transaction;
						nr_transactions++;

						if (nr_transactions == transactionIds.length) {
							for (var i = 0; i < nr_transactions; i++) {
								var transaction = transactions[transactionIds[i]];

								rows += "<tr><td><a href='#' data-toggle='modal' data-target='#dgs_listing_modal' data-id='" + String(transaction.id).escapeHTML() + "'>" + String(transaction.name).escapeHTML() + "</a></td><td>" + String(transaction.tags).escapeHTML() + "</td><td>" + NRS.format(transaction.quantity) + "</td><td>" + NRS.formatAmount(transaction.priceNQT) + " NXT</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_price_change_modal' data-id='" + String(transaction.id).escapeHTML() + "'>Change Price</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_quantity_change_modal' data-id='" + String(transaction.id).escapeHTML() + "'>Change QTY</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_delisting_modal' data-id='" + String(transaction.id).escapeHTML() + "'>Delete</a></td></tr>";
							}

							$("#my_dgs_listings_table tbody").empty().append(rows);
							NRS.dataLoadFinished($("#my_dgs_listings_table"));

							NRS.pageLoaded();
						}
					});

					if (NRS.currentPage != "my_dgs_listings") {
						transactions = {};
						return;
					}
				}
			} else {

				$("#my_dgs_listings_table tbody").empty().append(rows);
				NRS.dataLoadFinished($("#my_dgs_listings_table"));

				NRS.pageLoaded();
			}
		});
	}

	NRS.forms.dgsListingComplete = function(response, data) {
		if (response.alreadyProcessed) {
			return;
		}

		if (NRS.currentPage == "my_dgs_listings") {
			var $table = $("#my_dgs_listings_table tbody");

			var rowToAdd = "<tr class='tentative'><td><a href='#' data-toggle='modal' data-target='#dgs_listing_modal' data-id='" + String(data.id).escapeHTML() + "'>" + String(data.name).escapeHTML() + "</a></td><td>" + String(data.tags).escapeHTML() + "</td><td>" + NRS.format(data.quantity) + "</td><td>" + NRS.formatAmount(data.priceNQT) + " NXT</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_price_change_modal' data-id='" + String(data.id).escapeHTML() + "'>Change Price</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_quantity_change_modal' data-id='" + String(data.id).escapeHTML() + "'>Change QTY</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#dgs_delisting_modal' data-id='" + String(data.id).escapeHTML() + "'>Delete</a></td></tr>";

			$table.prepend(rowToAdd);

			if ($("#polls_table").parent().hasClass("data-empty")) {
				$("#polls_table").parent().removeClass("data-empty");
			}
		}
	}

	return NRS;
}(NRS || {}, jQuery));