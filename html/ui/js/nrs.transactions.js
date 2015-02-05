/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {

	NRS.lastTransactions = "";

	NRS.unconfirmedTransactions = [];
	NRS.unconfirmedTransactionIds = "";
	NRS.unconfirmedTransactionsChange = true;


	NRS.handleIncomingTransactions = function(transactions, confirmedTransactionIds) {
		var oldBlock = (confirmedTransactionIds === false); //we pass false instead of an [] in case there is no new block..

		if (typeof confirmedTransactionIds != "object") {
			confirmedTransactionIds = [];
		}

		if (confirmedTransactionIds.length) {
			NRS.lastTransactions = confirmedTransactionIds.toString();
		}

		if (confirmedTransactionIds.length || NRS.unconfirmedTransactionsChange) {
			transactions.sort(NRS.sortArray);
		}

		//always refresh peers and unconfirmed transactions..
		if (NRS.currentPage == "peers") {
			NRS.incoming.peers();
		} else if (NRS.currentPage == "transactions" && $('#transactions_type_navi li.active a').attr('data-transaction-type') == "unconfirmed") {
			NRS.incoming.transactions();
		} else {
			if (NRS.currentPage != 'messages' && (!oldBlock || NRS.unconfirmedTransactionsChange)) {
				if (NRS.incoming[NRS.currentPage]) {
					NRS.incoming[NRS.currentPage](transactions);
				}
			}
		}
		// always call incoming for messages to enable message notifications
		if (!oldBlock || NRS.unconfirmedTransactionsChange) {
			NRS.incoming['messages'](transactions);
		}
	}

	NRS.getUnconfirmedTransactions = function(callback) {
		NRS.sendRequest("getUnconfirmedTransactions", {
			"account": NRS.account
		}, function(response) {
			if (response.unconfirmedTransactions && response.unconfirmedTransactions.length) {
				var unconfirmedTransactions = [];
				var unconfirmedTransactionIds = [];

				response.unconfirmedTransactions.sort(function(x, y) {
					if (x.timestamp < y.timestamp) {
						return 1;
					} else if (x.timestamp > y.timestamp) {
						return -1;
					} else {
						return 0;
					}
				});
				
				for (var i = 0; i < response.unconfirmedTransactions.length; i++) {
					var unconfirmedTransaction = response.unconfirmedTransactions[i];

					unconfirmedTransaction.confirmed = false;
					unconfirmedTransaction.unconfirmed = true;
					unconfirmedTransaction.confirmations = "/";

					if (unconfirmedTransaction.attachment) {
						for (var key in unconfirmedTransaction.attachment) {
							if (!unconfirmedTransaction.hasOwnProperty(key)) {
								unconfirmedTransaction[key] = unconfirmedTransaction.attachment[key];
							}
						}
					}
					unconfirmedTransactions.push(unconfirmedTransaction);
					unconfirmedTransactionIds.push(unconfirmedTransaction.transaction);
				}
				NRS.unconfirmedTransactions = unconfirmedTransactions;
				var unconfirmedTransactionIdString = unconfirmedTransactionIds.toString();

				if (unconfirmedTransactionIdString != NRS.unconfirmedTransactionIds) {
					NRS.unconfirmedTransactionsChange = true;
					NRS.unconfirmedTransactionIds = unconfirmedTransactionIdString;
				} else {
					NRS.unconfirmedTransactionsChange = false;
				}

				if (callback) {
					callback(unconfirmedTransactions);
				}
			} else {
				NRS.unconfirmedTransactions = [];

				if (NRS.unconfirmedTransactionIds) {
					NRS.unconfirmedTransactionsChange = true;
				} else {
					NRS.unconfirmedTransactionsChange = false;
				}

				NRS.unconfirmedTransactionIds = "";
				if (callback) {
					callback([]);
				}
			}
		});
	}

	NRS.handleInitialTransactions = function(transactions, transactionIds) {
		if (transactions.length) {
			var rows = "";

			transactions.sort(NRS.sortArray);

			if (transactionIds.length) {
				NRS.lastTransactions = transactionIds.toString();
			}

			for (var i = 0; i < transactions.length; i++) {
				var transaction = transactions[i];
				rows += NRS.getTransactionRowHTML(transaction);
			}

			$("#dashboard_transactions_table tbody").empty().append(rows);
		}

		NRS.dataLoadFinished($("#dashboard_transactions_table"));
	}

	NRS.getInitialTransactions = function() {
		NRS.sendRequest("getAccountTransactions", {
			"account": NRS.account,
			"firstIndex": 0,
			"lastIndex": 9
		}, function(response) {
			if (response.transactions && response.transactions.length) {
				var transactions = [];
				var transactionIds = [];

				for (var i = 0; i < response.transactions.length; i++) {
					var transaction = response.transactions[i];

					transaction.confirmed = true;
					transactions.push(transaction);

					transactionIds.push(transaction.transaction);
				}

				NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
					NRS.handleInitialTransactions(transactions.concat(unconfirmedTransactions), transactionIds);
				});
			} else {
				NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
					NRS.handleInitialTransactions(unconfirmedTransactions, []);
				});
			}
		});
	}

	NRS.getNewTransactions = function() {
		//check if there is a new transaction..
		NRS.sendRequest("getAccountTransactionIds", {
			"account": NRS.account,
			"timestamp": NRS.blocks[0].timestamp + 1,
			"firstIndex": 0,
			"lastIndex": 0
		}, function(response) {
			//if there is, get latest 10 transactions
			if (response.transactionIds && response.transactionIds.length) {
				NRS.sendRequest("getAccountTransactions", {
					"account": NRS.account,
					"firstIndex": 0,
					"lastIndex": 9
				}, function(response) {
					if (response.transactions && response.transactions.length) {
						var transactionIds = [];

						$.each(response.transactions, function(key, transaction) {
							transactionIds.push(transaction.transaction);
							response.transactions[key].confirmed = true;
						});

						NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
							NRS.handleIncomingTransactions(response.transactions.concat(unconfirmedTransactions), transactionIds);
						});
					} else {
						NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
							NRS.handleIncomingTransactions(unconfirmedTransactions);
						});
					}
				});
			} else {
				NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
					NRS.handleIncomingTransactions(unconfirmedTransactions);
				});
			}
		});
	}

	//todo: add to dashboard? 
	NRS.addUnconfirmedTransaction = function(transactionId, callback) {
		NRS.sendRequest("getTransaction", {
			"transaction": transactionId
		}, function(response) {
			if (!response.errorCode) {
				response.transaction = transactionId;
				response.confirmations = "/";
				response.confirmed = false;
				response.unconfirmed = true;

				if (response.attachment) {
					for (var key in response.attachment) {
						if (!response.hasOwnProperty(key)) {
							response[key] = response.attachment[key];
						}
					}
				}
				var alreadyProcessed = false;
				try {
					var regex = new RegExp("(^|,)" + transactionId + "(,|$)");

					if (regex.exec(NRS.lastTransactions)) {
						alreadyProcessed = true;
					} else {
						$.each(NRS.unconfirmedTransactions, function(key, unconfirmedTransaction) {
							if (unconfirmedTransaction.transaction == transactionId) {
								alreadyProcessed = true;
								return false;
							}
						});
					}
				} catch (e) {}

				if (!alreadyProcessed) {
					NRS.unconfirmedTransactions.unshift(response);
				}
				if (callback) {
					callback(alreadyProcessed);
				}
				if (NRS.currentPage == 'transactions' || NRS.currentPage == 'dashboard') {
					NRS.incoming[NRS.currentPage]();
				}

				NRS.getAccountInfo();
			} else if (callback) {
				callback(false);
			}
		});
	}

	NRS.sortArray = function(a, b) {
		return b.timestamp - a.timestamp;
	}

	NRS.getPendingTransactionHTML = function(t) {
		if (t.attachment && t.attachment["version.TwoPhased"] && t.attachment.votingModel) {
			var html = "";
			var attachment = t.attachment;
			var vm = attachment.votingModel;

			html += String(attachment.quorum);
			if (vm == 0) {
				html += " NXT";
			} else if (vm == 1) {
				html += ' <i class="fa fa-group"></i>';
			} else if (vm == 2) {
				html = "Asset";
			} else {
				html = "Currency";
			}
			return html;
		} else {
			return "&nbsp;";
		}
	}

	NRS.getTransactionRowHTML = function(transaction) {
		var transactionType = $.t(NRS.transactionTypes[transaction.type]['subTypes'][transaction.subtype]['i18nKeyTitle']);

		if (transaction.type == 1 && transaction.subtype == 6 && transaction.attachment.priceNQT == "0") {
			if (transaction.sender == NRS.account && transaction.recipient == NRS.account) {
				transactionType = $.t("alias_sale_cancellation");
			} else {
				transactionType = $.t("alias_transfer");
			}
		}

		var receiving = transaction.recipient == NRS.account;
		var account = (receiving ? "sender" : "recipient");

		if (transaction.amountNQT) {
			transaction.amount = new BigInteger(transaction.amountNQT);
			transaction.fee = new BigInteger(transaction.feeNQT);
		}

		var hasMessage = false;

		if (transaction.attachment) {
			if (transaction.attachment.encryptedMessage || transaction.attachment.message) {
				hasMessage = true;
			} else if (transaction.sender == NRS.account && transaction.attachment.encryptToSelfMessage) {
				hasMessage = true;
			}
		}

		var html = "";
		html += "<tr>";
		
		html += "<td>";
  		html += "<a href='#' data-timestamp='" + String(transaction.timestamp).escapeHTML() + "' ";
  		html += "data-transaction='" + String(transaction.transaction).escapeHTML() + "'>";
  		html += NRS.formatTimestamp(transaction.timestamp) + "</a>";
  		html += "</td>";

  		html += "<td style='text-align:center;'>" + (hasMessage ? "&nbsp; <i class='fa fa-envelope-o'></i>&nbsp;" : "&nbsp;") + "</td>";
		
		var iconHTML = NRS.transactionTypes[transaction.type]['iconHTML'] + " " + NRS.transactionTypes[transaction.type]['subTypes'][transaction.subtype]['iconHTML'];
		html += '<td style="vertical-align:middle;">';
		html += '<span class="label label-primary" style="font-size:12px;">' + iconHTML + '</span>&nbsp; ';
		html += '<span style="font-size:11px;display:inline-block;margin-top:5px;">' + transactionType + '</span>';
		html += '</td>';
		
		html += "<td style='width:5px;padding-right:0;vertical-align:middle;'>";
		html += (transaction.type == 0 ? (receiving ? "<i class='fa fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fa fa-minus-circle' style='color:#E04434'></i>") : "") + "</td>";
		html += "<td style='vertical-align:middle;" + (transaction.type == 0 && receiving ? " color:#006400;" : (!receiving && transaction.amount > 0 ? " color:red;" : "")) + "'>" + NRS.formatAmount(transaction.amount) + "</td>";
		html += "<td style='vertical-align:middle;text-align:center;" + (!receiving ? " color:red;" : "") + "'>" + NRS.formatAmount(transaction.fee) + "</td>";

		html += "<td>" + ((NRS.getAccountLink(transaction, "sender") == "/" && transaction.type == 2) ? "Asset Exchange" : NRS.getAccountLink(transaction, "sender")) + " ";
		html += "<i class='fa fa-arrow-circle-right' style='color:#777;'></i> " + ((NRS.getAccountLink(transaction, "recipient") == "/" && transaction.type == 2) ? "Asset Exchange" : NRS.getAccountLink(transaction, "recipient")) + "</td>";

		html += "<td style='text-align:center;'>" + NRS.getPendingTransactionHTML(transaction) + "</td>";

		html += "<td class='confirmations' ";
		html += "data-content='" + (transaction.confirmed ? NRS.formatAmount(transaction.confirmations) + " " + $.t("confirmations") : $.t("unconfirmed_transaction")) + "' ";
		html += "data-container='body' data-placement='left' style='vertical-align:middle;text-align:center;font-size:12px;'>";
		html += (!transaction.confirmed ? "-" : (transaction.confirmations > 1440 ? "1440+" : NRS.formatAmount(transaction.confirmations))) + "</td>";
		html += "</tr>";
		return html;
	}

	NRS.incoming.dashboard = function() {
		var rows = "";
		var params = {
			"account": NRS.account,
			"firstIndex": 0,
			"lastIndex": 9
		};
		
		var unconfirmedTransactions = NRS.unconfirmedTransactions;
		if (unconfirmedTransactions) {
			for (var i = 0; i < unconfirmedTransactions.length; i++) {
				rows += NRS.getTransactionRowHTML(unconfirmedTransactions[i]);
			}
		}

		NRS.sendRequest("getAccountTransactions+", params, function(response) {
			if (response.transactions && response.transactions.length) {
				for (var i = 0; i < response.transactions.length; i++) {
					var transaction = response.transactions[i];
					transaction.confirmed = true;
					rows += NRS.getTransactionRowHTML(transaction);
				}

				$("#dashboard_transactions_table tbody").empty().append(rows);
			} else {
				$("#dashboard_transactions_table tbody").empty().append(rows);
			}
		});
	}

	NRS.buildTransactionsTypeNavi = function() {
		var html = '';
		html += '<li role="presentation" class="active"><a href="#" data-transaction-type="" ';
		html += 'data-toggle="popover" data-placement="top" data-content="All" data-container="body" data-i18n="[data-content]all">';
		html += '<span data-i18n="all">All</span></a></li>';
		$('#transactions_type_navi').append(html);

		$.each(NRS.transactionTypes, function(typeIndex, typeDict) {
			titleString = $.t(typeDict.i18nKeyTitle);
			html = '<li role="presentation"><a href="#" data-transaction-type="' + typeIndex + '" ';
			html += 'data-toggle="popover" data-placement="top" data-content="' + titleString + '" data-container="body">';
			html += typeDict.iconHTML + '</a></li>';
			$('#transactions_type_navi').append(html);
		});

		html  = '<li role="presentation"><a href="#" data-transaction-type="unconfirmed" ';
		html += 'data-toggle="popover" data-placement="top" data-content="Unconfirmed" data-container="body" data-i18n="[data-content]unconfirmed">';
		html += '<span data-i18n="unconfirmed">Unconfirmed</span></a></li>';
		$('#transactions_type_navi').append(html);
		html  = '<li role="presentation"><a href="#" data-transaction-type="pending" ';
		html += 'data-toggle="popover" data-placement="top" data-content="Pending" data-container="body" data-i18n="[data-content]pending">';
		html += '<span data-i18n="pending">Pending</span></a></li>';
		$('#transactions_type_navi').append(html);

		$('#transactions_type_navi a[data-toggle="popover"]').popover({
			"trigger": "hover"
		});
	}

	NRS.buildTransactionsSubTypeNavi = function() {
		$('#transactions_sub_type_navi').empty();
		html  = '<li role="presentation" class="active"><a href="#" data-transaction-sub-type="">';
		html += '<span data-i18n="all_types">All Types</span></a></li>';
		$('#transactions_sub_type_navi').append(html);

		var typeIndex = $('#transactions_type_navi li.active a').attr('data-transaction-type');
		if (typeIndex && typeIndex != "unconfirmed" && typeIndex != "pending") {
				var typeDict = NRS.transactionTypes[typeIndex];
				$.each(typeDict["subTypes"], function(subTypeIndex, subTypeDict) {
				subTitleString = $.t(subTypeDict.i18nKeyTitle);
				html = '<li role="presentation"><a href="#" data-transaction-sub-type="' + subTypeIndex + '">';
				html += subTypeDict.iconHTML + ' ' + subTitleString + '</a></li>';
				$('#transactions_sub_type_navi').append(html);
			});
		}
	}

	NRS.displayUnconfirmedTransactions = function() {
		NRS.sendRequest("getUnconfirmedTransactions", function(response) {
			var rows = "";

			if (response.unconfirmedTransactions && response.unconfirmedTransactions.length) {
				for (var i = 0; i < response.unconfirmedTransactions.length; i++) {
					rows += NRS.getTransactionRowHTML(response.unconfirmedTransactions[i]);
				}
			}
			NRS.dataLoaded(rows);
		});
	}

	NRS.displayPendingTransactions = function() {
		NRS.sendRequest("getPendingTransactions", function(response) {
			var rows = "";

			if (response.unconfirmedTransactions && response.unconfirmedTransactions.length) {
				for (var i = 0; i < response.unconfirmedTransactions.length; i++) {
					rows += NRS.getTransactionRowHTML(response.unconfirmedTransactions[i]);
				}
			}
			NRS.dataLoaded(rows);
		});
	}

	NRS.pages.transactions = function() {
		if ($('#transactions_type_navi').children().length == 0) {
			NRS.buildTransactionsTypeNavi();
			NRS.buildTransactionsSubTypeNavi();
		}

		var selectedType = $('#transactions_type_navi li.active a').attr('data-transaction-type');
		var selectedSubType = $('#transactions_sub_type_navi li.active a').attr('data-transaction-sub-type');
		if (!selectedSubType) {
			selectedSubType = "";
		}
		if (selectedType == "unconfirmed") {
			NRS.displayUnconfirmedTransactions();
			return;
		}
		/*if (selectedType == "pending") {
			NRS.displayPendingTransactions();
			return;
		}*/

		var rows = "";
		var params = {
			"account": NRS.account,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		};

		if (selectedType) {
			params.type = selectedType;
			params.subtype = selectedSubType;

			var unconfirmedTransactions = NRS.getUnconfirmedTransactionsFromCache(params.type, (params.subtype ? params.subtype : []));
		} else {
			var unconfirmedTransactions = NRS.unconfirmedTransactions;
		}

		if (unconfirmedTransactions) {
			for (var i = 0; i < unconfirmedTransactions.length; i++) {
				rows += NRS.getTransactionRowHTML(unconfirmedTransactions[i]);
			}
		}

		NRS.sendRequest("getAccountTransactions+", params, function(response) {
			if (response.transactions && response.transactions.length) {
				if (response.transactions.length > NRS.itemsPerPage) {
					NRS.hasMorePages = true;
					response.transactions.pop();
				}

				for (var i = 0; i < response.transactions.length; i++) {
					var transaction = response.transactions[i];

					transaction.confirmed = true;

					rows += NRS.getTransactionRowHTML(transaction);
				}

				NRS.dataLoaded(rows);
			} else {
				NRS.dataLoaded(rows);
			}
		});
	}

	NRS.incoming.transactions = function(transactions) {
		NRS.loadPage("transactions");
	}

	$(document).on("click", "#transactions_type_navi li a", function(e) {
		e.preventDefault();
		$('#transactions_type_navi li.active').removeClass('active');
  		$(this).parent('li').addClass('active');
  		NRS.buildTransactionsSubTypeNavi();
  		NRS.pageNumber = 1;
		NRS.loadPage("transactions");
	});

	$(document).on("click", "#transactions_sub_type_navi li a", function(e) {
		e.preventDefault();
		$('#transactions_sub_type_navi li.active').removeClass('active');
  		$(this).parent('li').addClass('active');
  		NRS.pageNumber = 1;
		NRS.loadPage("transactions");
	});

	$(document).on("click", "#transactions_sub_type_show_hide_btn", function(e) {
		e.preventDefault();
		if ($('#transactions_sub_type_navi_box').is(':visible')) {
			$('#transactions_sub_type_navi_box').hide();
			$(this).text($.t('show_type_menu', 'Show Type Menu'));
		} else {
			$('#transactions_sub_type_navi_box').show();
			$(this).text($.t('hide_type_menu', 'Hide Type Menu'));
		}
	});

	return NRS;
}(NRS || {}, jQuery));