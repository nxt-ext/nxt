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

				rows += NRS.getTransactionRowHTML(unconfirmedTransaction);
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

								rows += NRS.getTransactionRowHTML(transaction);
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

	return NRS;
}(NRS || {}, jQuery));