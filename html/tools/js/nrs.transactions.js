var NRS = (function(NRS, $, undefined) {
	NRS.pages.transactions = function() {
		NRS.pageLoading();

		var params = {
			"account": NRS.account,
			"timestamp": 0
		};

		if (NRS.transactionsPageType) {
			params.type = NRS.transactionsPageType.type;
			params.subtype = NRS.transactionsPageType.subtype;
		}

		var rows = "";

		if (NRS.unconfirmedTransactions.length) {
			for (var j = 0; j < NRS.unconfirmedTransactions.length; j++) {
				var unconfirmedTransaction = NRS.unconfirmedTransactions[j];

				if (NRS.transactionsPageType) {
					if (unconfirmedTransaction.type != params.type || unconfirmedTransaction.subtype != params.subtype) {
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
						if (NRS.currentPage != "transactions") {
							transactions = {};
							return;
						}

						transaction.id = input.transaction;
						transaction.confirmed = true;

						transactions[input.transaction] = transaction;
						nr_transactions++;

						if (nr_transactions == transactionIds.length) {
							for (var i = 0; i < nr_transactions; i++) {
								var transaction = transactions[transactionIds[i]];

								rows += NRS.getTransactionRowHTML(transaction);

							}

							$("#transactions_table tbody").empty().append(rows);
							NRS.dataLoadFinished($("#transactions_table"));

							NRS.pageLoaded();
						}
					});

					if (NRS.currentPage != "transactions") {
						transactions = {};
						return;
					}
				}
			} else {

				$("#transactions_table tbody").empty().append(rows);
				NRS.dataLoadFinished($("#transactions_table"));

				NRS.pageLoaded();
			}
		});
	}

	NRS.incoming.transactions = function(transactions) {
		NRS.pages.transactions();
	}

	NRS.getTransactionRowHTML = function(transaction) {
		var transactionType = "Unknown";

		if (transaction.type == 0) {
			transactionType = "Ordinary payment";
		} else if (transaction.type == 1) {
			switch (transaction.subtype) {
				case 0:
					transactionType = "Arbitrary message";
					break;
				case 1:
					transactionType = "Alias assignment";
					break;
				case 2:
					transactionType = "Poll creation";
					break;
				case 3:
					transactionType = "Vote casting";
					break;
			}
		} else if (transaction.type == 2) {
			switch (transaction.subtype) {
				case 0:
					transactionType = "Asset issuance";
					break;
				case 1:
					transactionType = "Asset transfer";
					break;
				case 2:
					transactionType = "Ask order placement";
					break;
				case 3:
					transactionType = "Bid order placement";
					break;
				case 4:
					transactionType = "Ask order cancellation";
					break;
				case 5:
					transactionType = "Bid order cancellation";
					break;
			}
		}

		var receiving = transaction.recipient == NRS.account;
		var account = (receiving ? String(transaction.sender).escapeHTML() : String(transaction.recipient).escapeHTML());

		if (transaction.amountNQT) {
			transaction.amount = new BigInteger(transaction.amountNQT);
			transaction.fee = new BigInteger(transaction.feeNQT);
		}

		return "<tr " + (!transaction.confirmed ? " class='tentative'" : "") + "><td>" + (transaction.attachment ? "<a href='#' data-transaction='" + String(transaction.id).escapeHTML() + "' style='font-weight:bold'>" + String(transaction.id).escapeHTML() + "</a>" : String(transaction.id).escapeHTML()) + "</td><td>" + NRS.formatTimestamp(transaction.timestamp) + "</td><td>" + transactionType + "</td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fa fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fa fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td " + (transaction.type == 0 && receiving ? " style='color:#006400;'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + NRS.formatAmount(transaction.amount) + "</td><td " + (!receiving ? " style='color:red'" : "") + ">" + NRS.formatAmount(transaction.fee) + "</td><td>" + (account != NRS.genesis ? "<a href='#' data-user='" + account + "' class='user_info'>" + NRS.getAccountTitle(account) + "</a>" : "Genesis") + "</td><td>" + (!transaction.confirmed ? "/" : (transaction.confirmations > 1000 ? "1000+" : NRS.formatAmount(transaction.confirmations))) + "</td></tr>";
	}

	$("#transactions_page_type li a").click(function(e) {
		e.preventDefault();

		var type = $(this).data("type");

		if (type) {
			type = type.split(":");
			NRS.transactionsPageType = {
				"type": type[0],
				"subtype": type[1]
			};
		} else {
			NRS.transactionsPageType = null;
		}

		$(this).parents(".btn-group").find(".text").text($(this).text());

		NRS.pages.transactions();
	});

	return NRS;
}(NRS || {}, jQuery));