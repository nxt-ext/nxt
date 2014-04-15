var NRS = (function(NRS, $, undefined) {
	"use strict";

	// save the original function object
	var _superModal = $.fn.modal;

	// add locked as a new option
	$.extend(_superModal.Constructor.DEFAULTS, {
		locked: false
	});

	// capture the original hide
	var _hide = _superModal.Constructor.prototype.hide;

	// add the lock, unlock and override the hide of modal
	$.extend(_superModal.Constructor.prototype, {
		// locks the dialog so that it cannot be hidden
		lock: function() {
			this.options.locked = true;
		}
		// unlocks the dialog so that it can be hidden by 'esc' or clicking on the backdrop (if not static)
		,
		unlock: function() {
			this.options.locked = false;
		}
		// override the original hide so that the original is only called if the modal is unlocked
		,
		hide: function() {
			if (this.options.locked) return;

			_hide.apply(this, arguments);
		}
	});

	NRS.helpers = {};
	NRS.state = {};
	NRS.blocks = [];
	NRS.temp = {
		"blocks": []
	};
	NRS.normalVersion = {};
	NRS.betaVersion = {};
	NRS.account = {};
	NRS.currentPage = "dashboard";
	NRS.newsRefresh = 0;
	NRS.messages = {};
	NRS.forms = {
		"errorMessages": {}
	};
	NRS.lastBlockHeight = 0;
	NRS.lastTransactionTimestamp = 0;
	NRS.account = "";
	NRS.server = "";
	NRS.pages = {};
	NRS.incoming = {};
	NRS.lastTransactionsTimestamp = 0;
	NRS.lastTransactions = "";
	NRS.xhrPool = [];
	NRS.genesis = "1739068987193023818";
	NRS.selectedContext = null;
	NRS.database = null;
	NRS.databaseSupport = false;
	NRS.assets = [];
	NRS.assetIds = [];
	NRS.loadedBefore = [];
	NRS.contacts = {};
	NRS.accountBalance = {};
	NRS.transactionsPageType = null;
	NRS.blocksPageType = null;
	NRS.downloadingBlockchain = false;
	NRS.blockchainCalculationServers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 12];
	NRS.isTestNet = false;
	NRS.fetchingModalData = false;
	NRS.closedGroups = [];
	NRS.isLocalHost = false;
	NRS.rememberPassword = false;
	NRS.settings = {};
	NRS.defaultSettings = {
		"submit_on_enter": 0,
		"use_new_address_format": 0
	};
	NRS.isForging = false;
	NRS.unconfirmedTransactions = [];
	NRS.unconfirmedTransactionIds = "";
	NRS.unconfirmedTransactionsChange = true;
	NRS.firstAssetPageLoad = true;
	NRS.useNQT = false;

	NRS.init = function() {
		if (location.port && location.port != "6876") {
			$(".testnet_only").hide();
		} else {
			NRS.isTestNet = true;
			NRS.blockchainCalculationServers = [9, 10];
			$(".testnet_only, #testnet_login, #testnet_warning").show();
		}

		NRS.useNQT = (NRS.isTestNet || NRS.lastBlockHeight >= 150000);

		if (!NRS.server) {
			var hostName = window.location.hostname.toLowerCase();
			NRS.isLocalHost = hostName == "localhost" || hostName == "127.0.0.1" || NRS.isPrivateIP(hostName);
		}

		if (!NRS.isLocalHost) {
			$(".remote_warning").show();
		}

		NRS.createDatabase(function() {
			NRS.getSettings();
		});

		NRS.getState(function() {
			NRS.checkAliasVersions();
		});

		NRS.showLockscreen();

		NRS.checkServerTime();

		//every 30 seconds check for new block..
		setInterval(function() {
			NRS.getState();
		}, 1000 * 30);

		setInterval(NRS.checkAliasVersions, 1000 * 60 * 60);

		$("#login_password").keypress(function(e) {
			if (e.which == '13') {
				e.preventDefault();
				var password = $("#login_password").val();
				NRS.login(password);
			}
		});

		$(".modal form input").keydown(function(e) {
			if (e.which == "13") {
				e.preventDefault();
				if (NRS.settings["submit_on_enter"] && e.target.type != "textarea") {
					$(this).submit();
				} else {
					return false;
				}
			}
		});

		$("#send_money_recipient, #transfer_asset_recipient, #send_message_recipient, #add_contact_account_id, #update_contact_account_id").blur(function() {
			var value = $(this).val();
			var modal = $(this).closest(".modal");

			if (value) {
				NRS.checkRecipient(value, modal);
			} else {
				modal.find(".account_info").hide();
			}
		});

		$(".show_popover").popover({
			"trigger": "hover"
		});
	}

	NRS.checkServerTime = function() {
		$.ajax({
			url: 'http://www.convert-unix-time.com/api?timestamp=now&returnType=jsonp',
			dataType: 'jsonp'
		}).done(function(response) {
			if (response.timestamp) {
				var comparisonTimestamp = response.timestamp;

				NRS.sendRequest("getTime", function(response) {
					var serverTimestamp = Math.round(new Date(Date.UTC(2013, 10, 24, 12, 0, 0, 0) + response.time * 1000).getTime() / 1000);
					var difference = Math.abs(serverTimestamp - comparisonTimestamp);

					if (difference > 16) {
						//possibly out of date time... (15 sec difference max - allowing 1 sec extra for ajax request handling)
						//todo
					}
				});
			}
		});
	}

	NRS.getState = function(callback) {
		NRS.sendRequest('getState', function(response) {
			if (response.errorCode) {
				//todo
			} else {
				if (!("lastBlock" in NRS.state)) {
					//first time...
					NRS.state = response;

					$("#nrs_version").html(NRS.state.version).removeClass("loading_dots");

					NRS.getBlock(NRS.state.lastBlock, NRS.handleInitialBlocks);
				} else if (NRS.state.lastBlock != response.lastBlock) {
					NRS.temp.blocks = [];
					NRS.state = response;
					NRS.getAccountBalance();
					NRS.getBlock(NRS.state.lastBlock, NRS.handleNewBlocks);
					NRS.getNewTransactions();
				} else {
					NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
						NRS.handleIncomingTransactions(unconfirmedTransactions, false);
					});
				}

				if (callback) {
					callback();
				}
			}
		});
	}

	$("#nrs_modal").on('show.bs.modal', function(e) {
		for (var key in NRS.state) {
			var el = $("#nrs_node_state_" + key);
			if (el.length) {
				if (key.indexOf("number") != -1) {
					el.html(NRS.formatAmount(NRS.state[key]));
				} else if (key.indexOf("Memory") != -1) {
					el.html(NRS.formatVolume(NRS.state[key]));
				} else if (key == "time") {
					el.html(NRS.formatTimestamp(NRS.state[key]));
				} else {
					el.html(String(NRS.state[key]).escapeHTML());
				}
			}
		}

		$("#nrs_update_explanation").show();
		$("#nrs_modal_state").show();

	});

	$("#nrs_modal").on('hide.bs.modal', function(e) {
		$("body").off("dragover.nrs, drop.nrs");

		$("#nrs_update_drop_zone, #nrs_update_result, #nrs_update_hashes, #nrs_update_hash_progress").hide();

		$(this).find("ul.nav li.active").removeClass("active");
		$("#nrs_modal_state_nav").addClass("active");

		$(".nrs_modal_content").hide();
	});

	$("#nrs_modal ul.nav li").click(function(e) {
		e.preventDefault();

		var tab = $(this).data("tab");

		$(this).siblings().removeClass("active");
		$(this).addClass("active");

		$(".nrs_modal_content").hide();

		var content = $("#nrs_modal_" + tab);

		content.show();
	});


	//when modal closes remove all those events...

	NRS.calculateBlockchainDownloadTime = function(callback) {
		if (!NRS.blockchainCalculationServers.length) {
			return;
		}

		var key = Math.floor((Math.random() * NRS.blockchainCalculationServers.length));
		var value = NRS.blockchainCalculationServers[key];

		NRS.blockchainCalculationServers.splice(key, 1);

		try {
			if (NRS.isTestNet) {
				var url = "http://node" + value + ".mynxtcoin.org:6876/nxt?requestType=getState";
			} else {
				var url = "http://vps" + value + ".nxtcrypto.org:7876/nxt?requestType=getState";
			}

			NRS.sendOutsideRequest(url, function(response) {
				if (response.numberOfBlocks && response.time && response.numberOfBlocks > NRS.state.numberOfBlocks && Math.abs(NRS.state.time - response.time) < 120) {
					NRS.blockchainExpectedBlocks = response.numberOfBlocks;
					if (callback) {
						callback();
					}
				} else if (callback) {
					NRS.calculateBlockchainDownloadTime(callback);
				}
			}, false);
		} catch (err) {
			if (callback) {
				NRS.calculateBlockchainDownloadTime(callback);
			}
		}
	}

	NRS.updateBlockchainDownloadProgress = function() {
		var percentage = parseInt(Math.round((NRS.state.numberOfBlocks / NRS.blockchainExpectedBlocks) * 100), 10);

		$("#downloading_blockchain .progress-bar").css("width", percentage + "%").prop("aria-valuenow", percentage);
		$("#downloading_blockchain .sr-only").html(percentage + "% Complete");
	}

	NRS.handleInitialBlocks = function(response) {
		if (response.errorCode) {
			return;
		}

		NRS.blocks.push(response);
		if (NRS.blocks.length < 10 && response.previousBlock) {
			NRS.getBlock(response.previousBlock, NRS.handleInitialBlocks);
		} else {
			NRS.lastBlockHeight = NRS.blocks[0].height;

			NRS.useNQT = (NRS.isTestNet || NRS.lastBlockHeight >= 150000);

			if (NRS.state && NRS.state.time - NRS.blocks[0].timestamp > 60 * 60 * 30) {
				NRS.downloadingBlockchain = true;
				$("#downloading_blockchain, #nrs_update_explanation_blockchain_sync").show();
				$("#show_console").hide();
				NRS.calculateBlockchainDownloadTime(function() {
					NRS.updateBlockchainDownloadProgress();
				});
			}

			var rows = "";

			for (var i = 0; i < NRS.blocks.length; i++) {
				var block = NRS.blocks[i];

				if (NRS.useNQT) {
					block.totalAmount = new BigInteger(block.totalAmountNQT);
					block.totalFee = new BigInteger(block.totalFeeNQT);
				}

				rows += "<tr><td>" + (block.numberOfTransactions > 0 ? "<a href='#' data-block='" + String(block.height).escapeHTML() + "' class='block' style='font-weight:bold'>" + String(block.height).escapeHTML() + "</a>" : String(block.height).escapeHTML()) + "</td><td>" + NRS.formatTimestamp(block.timestamp) + "</td><td>" + NRS.formatAmount(block.totalAmount) + " + " + NRS.formatAmount(block.totalFee) + "</td><td>" + block.numberOfTransactions + "</td></tr>";
			}

			$("#dashboard_blocks_table tbody").empty().append(rows);
			NRS.dataLoadFinished($("#dashboard_blocks_table"));
		}
	}

	NRS.handleNewBlocks = function(response) {
		if (NRS.downloadingBlockchain) {
			//new round started...
			if (NRS.temp.blocks.length == 0 && NRS.state.lastBlock != response.id) {
				return;
			}
		}

		//we have all blocks 	
		if (response.height - 1 == NRS.lastBlockHeight || NRS.temp.blocks.length == 99) {
			var newBlocks = [];

			//there was only 1 new block (response)
			if (NRS.temp.blocks.length == 0) {
				//remove oldest block, add newest block
				NRS.blocks.unshift(response);
				newBlocks.push(response);
			} else {
				NRS.temp.blocks.push(response);
				//remove oldest blocks, add newest blocks
				[].unshift.apply(NRS.blocks, NRS.temp.blocks);
				newBlocks = NRS.temp.blocks;
				NRS.temp.blocks = [];
			}

			if (NRS.blocks.length > 100) {
				NRS.blocks = NRS.blocks.slice(0, 100);
			}

			//set new last block height
			NRS.lastBlockHeight = NRS.blocks[0].height;

			NRS.useNQT = (NRS.isTestNet || NRS.lastBlockHeight >= 150000);

			NRS.incoming.updateDashboardBlocks(newBlocks);
		} else {
			NRS.temp.blocks.push(response);
			NRS.getBlock(response.previousBlock, NRS.handleNewBlocks);
		}
	}

	NRS.getNewTransactions = function() {
		NRS.sendRequest("getAccountTransactionIds", {
			"account": NRS.account,
			"timestamp": NRS.lastTransactionsTimestamp
		}, function(response) {
			if (response.transactionIds && response.transactionIds.length) {
				var transactionIds = response.transactionIds.reverse().slice(0, 10);

				if (transactionIds.toString() == NRS.lastTransactions) {
					NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
						NRS.handleIncomingTransactions(unconfirmedTransactions);
					});
					return;
				}

				NRS.transactionIds = transactionIds;

				var nrTransactions = 0;

				var newTransactions = [];

				//if we have a new transaction, we just get them all.. (10 max)
				for (var i = 0; i < transactionIds.length; i++) {
					NRS.sendRequest('getTransaction', {
						"transaction": transactionIds[i]
					}, function(transaction, input) {
						nrTransactions++;

						transaction.id = input.transaction;
						transaction.confirmed = true;
						newTransactions.push(transaction);

						if (nrTransactions == transactionIds.length) {
							NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
								NRS.handleIncomingTransactions(newTransactions.concat(unconfirmedTransactions), transactionIds);
							});
						}
					});
				}
			} else {
				NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
					NRS.handleIncomingTransactions(unconfirmedTransactions);
				});
			}
		});
	}

	NRS.getUnconfirmedTransactions = function(callback) {
		NRS.sendRequest("getUnconfirmedTransactionIds", {
			"account": NRS.account
		}, function(response) {
			if (response.unconfirmedTransactionIds && response.unconfirmedTransactionIds.length) {
				var unconfirmedTransactionIds = response.unconfirmedTransactionIds.reverse();

				var nr_transactions = 0;

				var unconfirmedTransactions = [];
				var unconfirmedTransactionIdArray = [];

				for (var i = 0; i < unconfirmedTransactionIds.length; i++) {
					NRS.sendRequest('getTransaction', {
						"transaction": unconfirmedTransactionIds[i]
					}, function(transaction, input) {
						nr_transactions++;

						//leave this for now, for older versions that do not yet have the account param added to getUnconfirmedTransactionIds
						if (transaction.sender == NRS.account) {
							transaction.id = input.transaction;
							transaction.confirmed = false;
							transaction.confirmations = "/";
							unconfirmedTransactions.push(transaction);
							unconfirmedTransactionIdArray.push(transaction.id);
						}

						if (nr_transactions == unconfirmedTransactionIds.length) {
							NRS.unconfirmedTransactions = unconfirmedTransactions;

							var unconfirmedTransactionIdString = unconfirmedTransactionIdArray.toString();

							if (unconfirmedTransactionIdString != NRS.unconfirmedTransactionIds) {
								NRS.unconfirmedTransactionsChange = true;
								NRS.unconfirmedTransactionIds = unconfirmedTransactionIdString;
							} else {
								NRS.unconfirmedTransactionsChange = false;
							}

							if (callback) {
								callback(unconfirmedTransactions);
							} else if (NRS.unconfirmedTransactionsChange) {
								NRS.incoming.updateDashboardTransactions(unconfirmedTransactions, true);
							}
						}
					});
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
				} else if (NRS.unconfirmedTransactionsChange) {
					NRS.incoming.updateDashboardTransactions([], true);
				}
			}
		});
	}

	NRS.handleIncomingTransactions = function(transactions, confirmedTransactionIds) {
		var oldBlock = (confirmedTransactionIds === false); //we pass false instead of an [] in case there is no new block..

		if (typeof confirmedTransactionIds != "object") {
			confirmedTransactionIds = [];
		}

		if (confirmedTransactionIds.length) {
			NRS.lastTransactions = confirmedTransactionIds.toString();

			for (var i = transactions.length - 1; i >= 0; i--) {
				if (transactions[i].confirmed) {
					NRS.lastTransactionsTimestamp = transactions[i].timestamp;
					break;
				}
			}
		}

		if (confirmedTransactionIds.length || NRS.unconfirmedTransactionsChange) {
			transactions.sort(NRS.sortArray);

			NRS.incoming.updateDashboardTransactions(transactions, confirmedTransactionIds.length == 0);
		}

		if (!oldBlock || NRS.unconfirmedTransactionsChange) {
			if (NRS.incoming[NRS.currentPage]) {
				NRS.incoming[NRS.currentPage](transactions);
			}
		}
	}

	//we always update the dashboard page..
	NRS.incoming.updateDashboardBlocks = function(newBlocks) {
		var newBlockCount = newBlocks.length;

		if (newBlockCount > 10) {
			newBlocks = newBlocks.slice(0, 10);
			newBlockCount = newBlocks.length;
		}

		if (NRS.downloadingBlockchain) {
			if (NRS.state && NRS.state.time - NRS.blocks[0].timestamp < 60 * 60 * 30) {
				NRS.downloadingBlockchain = false;
				$("#downloading_blockchain, #nrs_update_explanation_blockchain_sync").hide();
				$("#show_console").show();
				$.growl("The block chain is now up to date.", {
					"type": "success"
				});
				NRS.checkAliasVersions();
			} else {
				NRS.updateBlockchainDownloadProgress();
			}
		}

		var rows = "";

		for (var i = 0; i < newBlockCount; i++) {
			var block = newBlocks[i];

			if (NRS.useNQT) {
				block.totalAmount = new BigInteger(block.totalAmountNQT);
				block.totalFee = new BigInteger(block.totalFeeNQT);
			}

			rows += "<tr><td>" + (block.numberOfTransactions > 0 ? "<a href='#' data-block='" + String(block.height).escapeHTML() + "' class='block' style='font-weight:bold'>" + String(block.height).escapeHTML() + "</a>" : String(block.height).escapeHTML()) + "</td><td>" + NRS.formatTimestamp(block.timestamp) + "</td><td>" + NRS.formatAmount(block.totalAmount) + " + " + NRS.formatAmount(block.totalFee) + "</td><td>" + NRS.formatAmount(block.numberOfTransactions) + "</td></tr>";
		}

		if (newBlockCount == 1) {
			$("#dashboard_blocks_table tbody tr:last").remove();
		} else if (newBlockCount == 10) {
			$("#dashboard_blocks_table tbody").empty();
		} else {
			$("#dashboard_blocks_table tbody tr").slice(10 - newBlockCount).remove();
		}

		$("#dashboard_blocks_table tbody").prepend(rows);

		//update number of confirmations... perhaps we should also update it in tne NRS.transactions array
		$("#dashboard_transactions_table tr.confirmed td.confirmations").each(function() {
			if ($(this).data("incoming")) {
				$(this).removeData("incoming");
				return true;
			}

			var confirmations = parseInt($(this).data("confirmations"), 10);

			if (confirmations <= 10) {
				var nrConfirmations = confirmations + newBlocks.length;

				$(this).data("confirmations", nrConfirmations);

				if (nrConfirmations > 10) {
					nrConfirmations = '10+';
				}
				$(this).html(nrConfirmations);
			}
		});
	}

	NRS.incoming.updateDashboardTransactions = function(newTransactions, unconfirmed) {
		var newTransactionCount = newTransactions.length;

		if (newTransactionCount) {
			var rows = "";

			var onlyUnconfirmed = true;

			for (var i = 0; i < newTransactionCount; i++) {
				var transaction = newTransactions[i];

				var receiving = transaction.recipient == NRS.account;
				var account = (receiving ? String(transaction.sender).escapeHTML() : String(transaction.recipient).escapeHTML());

				if (transaction.confirmed) {
					onlyUnconfirmed = false;
				}

				if (transaction.amountNQT) {
					transaction.amount = new BigInteger(transaction.amountNQT);
					transaction.fee = new BigInteger(transaction.feeNQT);
				}

				rows += "<tr class='" + (!transaction.confirmed ? "tentative" : "confirmed") + "'><td>" + (transaction.attachment ? "<a href='#' data-transaction='" + String(transaction.id).escapeHTML() + "' style='font-weight:bold'>" + NRS.formatTimestamp(transaction.timestamp) + "</a>" : NRS.formatTimestamp(transaction.timestamp)) + "</td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fa fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fa fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td><span" + (transaction.type == 0 && receiving ? " style='color:#006400'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + NRS.formatAmount(transaction.amount) + "</span> <span" + ((!receiving && transaction.type == 0) ? " style='color:red'" : "") + ">+</span> <span" + (!receiving ? " style='color:red'" : "") + ">" + NRS.formatAmount(transaction.fee) + "</span></td><td>" + (account != NRS.genesis ? "<a href='#' data-user='" + account + "' class='user_info'>" + NRS.getAccountTitle(account) + "</a>" : "Genesis") + "</td><td class='confirmations' data-confirmations='" + String(transaction.confirmations).escapeHTML() + "' data-initial='true'>" + (transaction.confirmations > 10 ? "10+" : String(transaction.confirmations).escapeHTML()) + "</td></tr>";
			}

			if (onlyUnconfirmed) {
				$("#dashboard_transactions_table tbody tr.tentative").remove();
				$("#dashboard_transactions_table tbody").prepend(rows);
			} else {
				$("#dashboard_transactions_table tbody").empty().append(rows);
			}

			var $parent = $("#dashboard_transactions_table").parent();

			if ($parent.hasClass("data-empty")) {
				$parent.removeClass("data-empty");
				if ($parent.data("no-padding")) {
					$parent.parent().addClass("no-padding");
				}
			}
		} else if (unconfirmed) {
			$("#dashboard_transactions_table tbody tr.tentative").remove();
		}
	}

	$("#account_balance_modal").on("show.bs.modal", function(e) {
		if (NRS.accountBalance.errorCode) {
			$("#account_balance_table").hide();

			if (NRS.accountBalance.errorCode == 5) {
				$("#account_balance_warning").html("Your account is brand new. You should fund it with some coins. Your account ID is <strong>" + NRS.account + "</strong>").show();
			} else {
				$("#account_balance_warning").html(NRS.accountBalance.errorDescription.escapeHTML()).show();
			}
		} else {
			$("#account_balance_warning").hide();

			if (NRS.useNQT) {
				$("#account_balance_balance").html(NRS.formatAmount(new BigInteger(NRS.accountBalance.balanceNQT)) + " NXT");
				$("#account_balance_unconfirmed_balance").html(NRS.formatAmount(new BigInteger(NRS.accountBalance.unconfirmedBalanceNQT)) + " NXT");
				$("#account_balance_effective_balance").html(NRS.formatAmount(NRS.accountBalance.effectiveBalanceNXT) + " NXT");
			} else {
				$("#account_balance_balance").html(NRS.formatAmount(NRS.accountBalance.balance / 100) + " NXT");
				$("#account_balance_unconfirmed_balance").html(NRS.formatAmount(NRS.accountBalance.unconfirmedBalance / 100) + " NXT");
				$("#account_balance_effective_balance").html(NRS.formatAmount(NRS.accountBalance.effectiveBalance / 100) + " NXT");
			}

			$("#account_balance_public_key").html(String(NRS.accountBalance.publicKey).escapeHTML());
			$("#account_balance_account_id").html(String(NRS.account).escapeHTML());

			var address = new NxtAddress();

			if (address.set(NRS.account, true)) {
				$("#account_balance_new_address_format").html(address.toString().escapeHTML());
			} else {
				$("#account_balance_new_address_format").html("/");
			}

			if (!NRS.accountBalance.publicKey) {
				$("#account_balance_public_key").html("/");
				$("#account_balance_warning").html("Your account does not have a public key! This means it's not as protected as other accounts. You must make an outgoing transaction to fix this issue. (<a href='#' data-toggle='modal' data-target='#send_message_modal'>send a message</a>, <a href='#' data-toggle='modal' data-target='#register_alias_modal'>buy an alias</a>, <a href='#' data-toggle='modal' data-target='#send_money_modal'>send Nxt</a>, ...)").show();
			}
		}
	});

	NRS.getBlock = function(blockID, callback, async) {
		NRS.sendRequest('getBlock', {
			"block": blockID
		}, function(response) {
			if (response.errorCode && response.errorCode == -1) {
				NRS.getBlock(blockID, callback, async);
			} else {
				if (callback) {
					response.id = blockID;
					callback(response);
				}
			}
		}, (async == undefined ? true : async));
	}

	$("#id_search").on("submit", function(e) {
		e.preventDefault();

		var id = $("#id_search input[name=q]").val();

		if (!/^\d+$/.test(id)) {
			$.growl("You can search by account ID, transaction ID or block ID, nothing else.", {
				"type": "danger"
			});
			return;
		}
		NRS.sendRequest("getTransaction", {
			"transaction": id
		}, function(response, input) {
			if (!response.errorCode) {
				response.id = input.transaction;
				NRS.showTransactionModal(response);
			} else {
				NRS.sendRequest("getAccount", {
					"account": id
				}, function(response, input) {
					if (!response.errorCode) {
						response.id = input.account;
						NRS.showAccountModal(response);
					} else {
						NRS.sendRequest("getBlock", {
							"block": id
						}, function(response, input) {
							if (!response.errorCode) {
								response.id = input.block;
								NRS.showBlockModal(response);
							} else {
								$.growl("Nothing found, please try another query.", {
									"type": "danger"
								});
							}
						});
					}
				});
			}
		});
	});

	$("#logo, .sidebar-menu a").click(function(event, data) {
		if ($(this).hasClass("ignore")) {
			$(this).removeClass("ignore");
			return;
		}

		event.preventDefault();

		if ($(this).data("toggle") == "modal") {
			return;
		}

		var page = $(this).data("page");

		if (page == NRS.currentPage) {
			return;
		}

		NRS.abortOutstandingRequests();

		$(".page").hide();

		$("body").scrollTop(0);

		$("#" + page + "_page").show();


		$(".content-header h1").find(".loading_dots").remove();

		var changeActive = !($(this).closest("ul").hasClass("treeview-menu"));

		if (changeActive) {
			var currentActive = $("ul.sidebar-menu > li.active");

			if (currentActive.hasClass("treeview")) {
				currentActive.children("a").first().addClass("ignore").click();
			} else {
				currentActive.removeClass("active");
			}

			if ($(this).attr("id") && $(this).attr("id") == "logo") {
				$("#dashboard_link").addClass("active");
			} else {
				$(this).parent().addClass("active");
			}
		}

		if (NRS.currentPage != "messages") {
			$("#inline_message_password").val("");
		}

		//NRS.previousPage = NRS.currentPage;
		NRS.currentPage = page;
		NRS.currentSubPage = "";

		if (NRS.pages[page]) {
			if (data && data.callback) {
				NRS.pages[page](data.callback);
			} else if (data) {
				NRS.pages[page](data);
			} else {
				NRS.pages[page]();
			}
		}
	});

	NRS.pageLoading = function() {
		if ($.inArray(NRS.currentPage, NRS.loadedBefore) != -1) {
			$("#" + NRS.currentPage + "_page .content-header h1").append("<span class='loading_dots'><span>.</span><span>.</span><span>.</span></span>");
		} else {
			NRS.loadedBefore.push(NRS.currentPage);
		}
	}

	NRS.pageLoaded = function(callback) {
		$("#" + NRS.currentPage + "_page .content-header h1").find(".loading_dots").remove();
		if (callback) {
			callback();
		}
	}

	$("button.goto-page, a.goto-page").click(function(event) {
		event.preventDefault();

		var page = $(this).data("page");

		var $link = $("ul.sidebar-menu a[data-page=" + page + "]");

		if ($link.length) {
			$link.trigger("click");
		} else {
			NRS.currentPage = page;
			$("ul.sidebar-menu a.active").removeClass("active");
			$(".page").hide();
			$("#" + page + "_page").show();
			if (NRS.pages[page]) {
				NRS.pages[page]();
			}
		}
	});

	NRS.userInfoModal = {
		"user": 0
	};

	$("#blocks_table, #polls_table, #contacts_table, #transactions_table, #dashboard_transactions_table, #asset_account, #asset_exchange_ask_orders_table, #asset_exchange_bid_orders_table").on("click", "a[data-user]", function(e) {
		e.preventDefault();

		var account = $(this).data("user");

		NRS.showAccountModal(account);
	});

	NRS.showAccountModal = function(account) {
		if (NRS.fetchingModalData) {
			return;
		}

		if (typeof account == "object") {
			NRS.userInfoModal.user = account.id;
		} else {
			NRS.userInfoModal.user = account;
			NRS.fetchingModalData = true;
		}

		$("#user_info_modal_account").html(NRS.getAccountFormatted(NRS.userInfoModal.user));

		$("#user_info_modal_actions button").data("account", NRS.userInfoModal.user);

		if (NRS.userInfoModal.user in NRS.contacts) {
			$("#user_info_modal_add_as_contact").hide();
		} else {
			$("#user_info_modal_add_as_contact").show();
		}

		if (NRS.fetchingModalData) {
			NRS.sendRequest("getAccount", {
				"account": NRS.userInfoModal.user
			}, function(response) {
				NRS.processAccountModalData(response);
				NRS.fetchingModalData = false;
			});
		} else {
			NRS.processAccountModalData(account);
		}

		$("#user_info_modal_transactions").show();

		NRS.userInfoModal.transactions();
	}

	NRS.processAccountModalData = function(account) {
		var balance;

		if (NRS.useNQT) {
			balance = new BigInteger(account.unconfirmedBalanceNQT);
		} else {
			balance = (account.unconfirmedBalance / 100) || 0;
		}

		if (balance == 0) {
			$("#user_info_modal_balance").html("0");
		} else {
			$("#user_info_modal_balance").html(NRS.formatAmount(balance) + " NXT");
		}

		$("#user_info_modal").modal("show");
	}

	$("#user_info_modal").on("hidden.bs.modal", function(e) {
		$(this).find(".user_info_modal_content").hide();
		$(this).find(".user_info_modal_content table tbody").empty();
		$(this).find(".user_info_modal_content:not(.data-loading,.data-never-loading)").addClass("data-loading");
		$(this).find("ul.nav li.active").removeClass("active");
		$("#user_info_transactions").addClass("active");
		NRS.userInfoModal.user = 0;
	});

	$("#user_info_modal ul.nav li").click(function(e) {
		e.preventDefault();

		var tab = $(this).data("tab");

		$(this).siblings().removeClass("active");
		$(this).addClass("active");

		$(".user_info_modal_content").hide();

		var content = $("#user_info_modal_" + tab);

		content.show();

		if (content.hasClass("data-loading")) {
			NRS.userInfoModal[tab]();
		}
	});

	/*some duplicate methods here...*/
	NRS.userInfoModal.transactions = function(type) {
		NRS.sendRequest("getAccountTransactionIds", {
			"account": NRS.userInfoModal.user,
			"timestamp": 0
		}, function(response) {
			if (response.transactionIds && response.transactionIds.length) {
				var transactions = {};
				var nr_transactions = 0;

				var transactionIds = response.transactionIds.reverse().slice(0, 100);

				for (var i = 0; i < transactionIds.length; i++) {
					NRS.sendRequest("getTransaction", {
						"transaction": transactionIds[i]
					}, function(transaction, input) {
						/*
    					if (NRS.currentPage != "transactions") {
    						transactions = {};
    						return;
    					}*/

						transactions[input.transaction] = transaction;
						nr_transactions++;

						if (nr_transactions == transactionIds.length) {
							var rows = "";

							for (var i = 0; i < nr_transactions; i++) {
								var transaction = transactions[transactionIds[i]];

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

								var receiving = transaction.recipient == NRS.userInfoModal.user;
								var account = (receiving ? String(transaction.sender).escapeHTML() : String(transaction.recipient).escapeHTML());

								if (transaction.amountNQT) {
									transaction.amount = new BigInteger(transaction.amountNQT);
									transaction.fee = new BigInteger(transaction.feeNQT);
								}

								rows += "<tr><td>" + NRS.formatTimestamp(transaction.timestamp) + "</td><td>" + transactionType + "</td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fa fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fa fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td " + (transaction.type == 0 && receiving ? " style='color:#006400;'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + NRS.formatAmount(transaction.amount) + "</td><td " + (!receiving ? " style='color:red'" : "") + ">" + NRS.formatAmount(transaction.fee) + "</td><td>" + NRS.getAccountTitle(account) + "</td></tr>";
							}

							$("#user_info_modal_transactions_table tbody").empty().append(rows);
							NRS.dataLoadFinished($("#user_info_modal_transactions_table"));
						}
					});

					/*
    				if (NRS.currentPage != "transactions") {
    					transactions = {};
    					return;
    				}*/
				}
			} else {
				$("#user_info_modal_transactions_table tbody").empty();
				NRS.dataLoadFinished($("#user_info_modal_transactions_table"));
			}
		});
	}

	NRS.userInfoModal.aliases = function() {
		NRS.sendRequest("listAccountAliases", {
			"account": NRS.userInfoModal.user
		}, function(response) {
			if (response.aliases && response.aliases.length) {
				var aliases = response.aliases;

				aliases.sort(function(a, b) {
					if (a.alias.toLowerCase() > b.alias.toLowerCase()) {
						return 1;
					} else if (a.alias.toLowerCase() < b.alias.toLowerCase()) {
						return -1;
					} else {
						return 0;
					}
				});

				var rows = "";

				var alias_account_count = 0,
					alias_uri_count = 0,
					empty_alias_count = 0,
					alias_count = aliases.length;

				for (var i = 0; i < alias_count; i++) {
					var alias = aliases[i];

					rows += "<tr data-alias='" + alias.alias.toLowerCase().escapeHTML() + "'><td class='alias'>" + alias.alias.escapeHTML() + "</td><td class='uri'>" + (alias.uri.indexOf("http") === 0 ? "<a href='" + String(alias.uri).escapeHTML() + "' target='_blank'>" + String(alias.uri).escapeHTML() + "</a>" : String(alias.uri).escapeHTML()) + "</td></tr>";
					if (!alias.uri) {
						empty_alias_count++;
					} else if (alias.uri.indexOf("http") === 0) {
						alias_uri_count++;
					} else if (alias.uri.indexOf("acct:") === 0 || alias.uri.indexOf("nacc:") === 0) {
						alias_account_count++;
					}
				}

				$("#user_info_modal_aliases_table tbody").empty().append(rows);
				NRS.dataLoadFinished($("#user_info_modal_aliases_table"));
			} else {
				$("#user_info_modal_aliases_table tbody").empty();
				NRS.dataLoadFinished($("#user_info_modal_aliases_table"));
			}
		});
	}

	NRS.userInfoModal.assets = function() {
		NRS.sendRequest("getAccount", {
			"account": NRS.userInfoModal.user
		}, function(response) {
			/*
			if (NRS.currentPage != "my_assets") {
				return;
			}*/

			if (response.assetBalances && response.assetBalances.length) {
				var assets = [];
				var nr_assets = 0;
				var ignored_assets = 0;

				for (var i = 0; i < response.assetBalances.length; i++) {
					if (response.assetBalances[i].balance == 0) {
						ignored_assets++;

						if (nr_assets + ignored_assets == response.assetBalances.length) {
							NRS.userInfoModal.assetsLoaded(assets);
						}
						continue;
					}

					NRS.sendRequest("getAsset", {
						"asset": response.assetBalances[i].asset,
						"_extra": {
							"balance": response.assetBalances[i].balance
						}
					}, function(asset, input) {
						/*
						if (NRS.currentPage != "my_assets") {
							return;
						}*/


						asset.asset = input.asset;
						asset.balance = input["_extra"].balance;

						assets[nr_assets] = asset;
						nr_assets++;

						if (nr_assets + ignored_assets == response.assetBalances.length) {
							NRS.userInfoModal.assetsLoaded(assets);
						}
					});

					/*
					if (NRS.currentPage != "my_assets") {
						return;
					}*/
				}
			} else {
				$("#user_info_modal_assets_table tbody").empty();
				NRS.dataLoadFinished($("#user_info_modal_assets_table"));
			}
		});
	}

	NRS.userInfoModal.assetsLoaded = function(assets) {
		var rows = "";

		assets.sort(function(a, b) {
			if (a.name.toLowerCase() > b.name.toLowerCase()) {
				return 1;
			} else if (a.name.toLowerCase() < b.name.toLowerCase()) {
				return -1;
			} else {
				return 0;
			}
		});

		for (var i = 0; i < assets.length; i++) {
			var asset = assets[i];

			var percentageAsset = parseFloat(asset.balance / asset.quantity);
			percentageAsset = Math.round(percentageAsset * 10000000) / 100000;

			rows += "<tr><td>" + asset.name.escapeHTML() + "</td><td>" + NRS.formatAmount(asset.balance) + "</td><td>" + NRS.formatAmount(asset.quantity) + "</td><td>" + percentageAsset + "%</td></tr>";
		}

		$("#user_info_modal_assets_table tbody").empty().append(rows);
		NRS.dataLoadFinished($("#user_info_modal_assets_table"));
	}

	$("#blocks_table, #dashboard_blocks_table").on("click", "a[data-block]", function(event) {
		event.preventDefault();

		var blockHeight = $(this).data("block");

		var block = $(NRS.blocks).filter(function() {
			return parseInt(this.height) == parseInt(blockHeight);
		}).get(0);

		NRS.showBlockModal(block);
	});

	NRS.showBlockModal = function(block) {
		if (NRS.fetchingModalData) {
			return;
		}

		NRS.fetchingModalData = true;

		$("#block_info_modal_block").html(String(block.id).escapeHTML());

		$("#block_info_transactions_tab_link").tab("show");

		var blockDetails = $.extend({}, block);
		delete blockDetails.transactions;
		delete blockDetails.previousBlockHash;
		delete blockDetails.nextBlockHash;
		delete blockDetails.generationSignature;
		delete blockDetails.payloadHash;
		delete blockDetails.id;

		$("#block_info_details_table tbody").empty().append(NRS.createInfoTable(blockDetails));
		$("#block_info_details_table").show();

		if (block.transactions.length) {
			$("#block_info_transactions_none").hide();
			$("#block_info_transactions_table").show();

			var transactions = {};
			var nrTransactions = 0;

			for (var i = 0; i < block.transactions.length; i++) {
				NRS.sendRequest("getTransaction", {
					"transaction": block.transactions[i]
				}, function(transaction, input) {
					nrTransactions++;
					transactions[input.transaction] = transaction;

					if (nrTransactions == block.transactions.length) {
						var rows = "";

						for (var i = 0; i < nrTransactions; i++) {
							var transaction = transactions[block.transactions[i]];

							if (transaction.amountNQT) {
								transaction.amount = new BigInteger(transaction.amountNQT);
								transaction.fee = new BigInteger(transaction.feeNQT);
							}

							rows += "<tr><td>" + NRS.formatTime(transaction.timestamp) + "</td><td>" + NRS.formatAmount(transaction.amount) + "</td><td>" + NRS.formatAmount(transaction.fee) + "</td><td>" + NRS.getAccountTitle(transaction.recipient) + "</td><td>" + NRS.getAccountTitle(transaction.sender) + "</td></tr>";
						}

						$("#block_info_transactions_table tbody").empty().append(rows);
						$("#block_info_modal").modal("show");

						NRS.fetchingModalData = false;
					}
				});
			}
		} else {
			$("#block_info_transactions_none").show();
			$("#block_info_transactions_table").hide();
			$("#block_info_modal").modal("show");

			NRS.fetchingModalData = false;
		}
	}

	NRS.forms.sendMoneyComplete = function(response, data) {
		if (!(data["_extra"] && data["_extra"].convertedAccount) && !(data.recipient in NRS.contacts)) {
			$.growl("NXT has been sent! <a href='#' data-account='" + String(data.recipient).escapeHTML() + "' data-toggle='modal' data-target='#add_contact_modal' style='text-decoration:underline'>Add recipient to contacts?</a>", {
				"type": "success"
			});
		} else {
			$.growl("NXT has been sent!", {
				"type": "success"
			});
		}
	}

	//todo: add to dashboard? 
	NRS.addUnconfirmedTransaction = function(transactionId) {
		NRS.sendRequest("getTransaction", {
			"transaction": transactionId
		}, function(response) {
			if (!response.errorCode) {
				response.id = transactionId;
				response.confirmations = "/";
				response.confirmed = false;
				NRS.unconfirmedTransactions.push(response);
			}
		});
	}

	NRS.forms.sendMessageComplete = function(response, data) {
		NRS.addUnconfirmedTransaction(response.transaction);

		data.message = data._extra.message;

		if (!(data["_extra"] && data["_extra"].convertedAccount)) {
			$.growl("Your message has been sent! <a href='#' data-account='" + String(data.recipient).escapeHTML() + "' data-toggle='modal' data-target='#add_contact_modal' style='text-decoration:underline'>Add recipient to contacts?</a>", {
				"type": "success"
			});
		} else {
			$.growl("Your message has been sent!", {
				"type": "success"
			});
		}

		if (NRS.currentPage == "messages") {
			var date = new Date(Date.UTC(2013, 10, 24, 12, 0, 0, 0)).getTime();

			var now = parseInt(((new Date().getTime()) - date) / 1000, 10);

			var $sidebar = $("#messages_sidebar");

			var $existing = $sidebar.find("a.list-group-item[data-account=" + String(data.recipient).escapeHTML() + "]");

			if ($existing.length) {
				$sidebar.prepend($existing);
				$existing.find("p.list-group-item-text").html(NRS.formatTimestamp(now));

				if ($existing.hasClass("active")) {
					$("#message_details dl.chat").append("<dd class='to tentative'><p>" + data.message.escapeHTML() + "</p></dd>");
				}
			} else {
				var accountTitle = NRS.getAccountTitle(data.recipient);

				var extra = "";

				if (accountTitle != data.recipient) {
					extra = " data-context='messages_sidebar_update_context'";
				}

				var listGroupItem = "<a href='#' class='list-group-item' data-account='" + String(data.recipient).escapeHTML() + "'" + extra + "><h4 class='list-group-item-heading'>" + accountTitle + "</h4><p class='list-group-item-text'>" + NRS.formatTimestamp(now) + "</p></a>";
				$("#messages_sidebar").prepend(listGroupItem);
			}
		}
	}

	NRS.createInfoTable = function(data, fixed) {
		var rows = "";

		for (var key in data) {
			var value = data[key];

			//no need to mess with input, already done if Formatted is at end of key
			if (/Formatted$/i.test(key)) {
				key = key.replace("Formatted", "");
				value = String(value).escapeHTML();
			} else if (key == "Quantity") {
				if (NRS.useNQT) {
					value = NRS.formatAmount(new BigInteger(value));
				} else {
					value = NRS.formatAmount(value);
				}
			} else if (key == "Price" || key == "Total") {
				if (NRS.useNQT) {
					value = NRS.formatAmount(new BigInteger(value)) + " NXT";
				} else {
					value = NRS.formatAmount(value / 100, true) + " NXT"; //ROUND
				}
			} else {
				value = String(value).escapeHTML();
			}

			rows += "<tr><td style='font-weight:bold;white-space:nowrap" + (fixed ? ";width:150px" : "") + "'>" + String(key).escapeHTML() + ":</td><td style='width:90%;" + (/hash|signature|publicKey/i.test(key) ? "word-break:break-all" : "") + "'>" + value + "</td></tr>";
		}

		return rows;
	}

	$("#transactions_table, #dashboard_transactions_table").on("click", "a[data-transaction]", function(e) {
		e.preventDefault();

		var transactionId = $(this).data("transaction");

		NRS.showTransactionModal(transactionId);
	});

	NRS.showTransactionModal = function(transaction) {
		if (NRS.fetchingModalData) {
			return;
		}

		NRS.fetchingModalData = true;

		$("#transaction_info_output").html("").hide();
		$("#transaction_info_table").hide();
		$("#transaction_info_table tbody").empty();

		if (typeof transaction != "object") {
			NRS.sendRequest("getTransaction", {
				"transaction": transaction
			}, function(response, input) {
				response.id = input.transaction;
				NRS.processTransactionModalData(response);
			});
		} else {
			NRS.processTransactionModalData(transaction);
		}
	}

	NRS.processTransactionModalData = function(transaction) {
		var async = false;

		var transactionDetails = $.extend({}, transaction);
		delete transactionDetails.attachment;
		if (transactionDetails.referencedTransaction == "0") {
			delete transactionDetails.referencedTransaction;
		}
		delete transactionDetails.id;

		$("#transaction_info_modal_transaction").html(String(transaction.id).escapeHTML());

		$("#transaction_info_tab_link").tab("show");

		$("#transaction_info_details_table tbody").append(NRS.createInfoTable(transactionDetails, true));

		var incorrect = false;

		if (transaction.type == 1) {
			switch (transaction.subtype) {
				case 0:
					var hex = transaction.attachment.message;

					//password: return {"requestType": "sendMessage", "data": data};

					var message;

					if (hex.indexOf("4352595054454421") === 0) { //starts with CRYPTED!
						NRS.sendRequest("getAccountPublicKey", {
							"account": (transaction.recipient == NRS.account ? transaction.sender : transaction.recipient)
						}, function(response) {
							if (!response.publicKey) {
								$.growl("Could not find public key for recipient, which is necessary for sending encrypted messages.", {
									"type": "danger"
								});
							}

							message = NRS.decryptMessage("return {\"requestType\": \"sendMessage\", \"data\": data};", response.publicKey, hex);
						}, false);
					} else {
						try {
							message = converters.hexStringToString(hex);
						} catch (err) {
							message = "Could not convert hex to string: " + hex;
						}
					}

					var sender_info = "";

					if (transaction.sender == NRS.account || transaction.recipient == NRS.account) {
						if (transaction.sender == NRS.account) {
							sender_info = "<strong>To</strong>: " + NRS.getAccountTitle(transaction.recipient);
						} else {
							sender_info = "<strong>From</strong>: " + NRS.getAccountTitle(transaction.sender);
						}
					} else {
						sender_info = "<strong>To</strong>: " + NRS.getAccountTitle(transaction.recipient) + "<br />";
						sender_info += "<strong>From</strong>: " + NRS.getAccountTitle(transaction.sender);
					}

					$("#transaction_info_output").html(message.escapeHTML().nl2br() + "<br /><br />" + sender_info).show();
					break;
				case 1:
					var data = {
						"Type": "Alias Assignment",
						"Alias": transaction.attachment.alias,
						"URI": transaction.attachment.uri
					};

					if (transaction.sender != NRS.account) {
						data["Sender"] = NRS.getAccountTitle(transaction.sender);
					}

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 2:
					var data = {
						"Type": "Poll Creation",
						"Name": transaction.attachment.name,
						"Description": transaction.attachment.description
					};

					if (transaction.sender != NRS.account) {
						data["Sender"] = NRS.getAccountTitle(transaction.sender);
					}

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 3:
					var data = {
						"Type": "Vote Casting"
					};

					if (transaction.sender != NRS.account) {
						data["Sender"] = NRS.getAccountTitle(transaction.sender);
					}

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();
					break;
				default:
					incorrect = true;
					break;
			}
		} else if (transaction.type == 2) {
			switch (transaction.subtype) {
				case 0:
					var data = {
						"Type": "Asset Issuance",
						"Name": transaction.attachment.name,
						"QuantityFormatted": NRS.formatQuantity(transaction.attachment.quantityQNT, transaction.attachment.decimals),
						"Decimals": transaction.attachment.decimals,
						"Description": transaction.attachment.description
					};

					if (transaction.sender != NRS.account) {
						data["Sender"] = NRS.getAccountTitle(transaction.sender);
					}

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 1:
					async = true;

					NRS.sendRequest("getAsset", {
						"asset": transaction.attachment.asset
					}, function(asset, input) {
						var data = {
							"Type": "Asset Transfer",
							"Asset Name": asset.name,
							"Quantity": transaction.attachment.quantity
						};

						data["Sender"] = NRS.getAccountTitle(transaction.sender);
						data["Recipient"] = NRS.getAccountTitle(transaction.recipient);

						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();

						$("#transaction_info_modal").modal("show");
						NRS.fetchingModalData = false;
					});

					break;
				case 2:
					async = true;

					NRS.sendRequest("getAsset", {
						"asset": transaction.attachment.asset
					}, function(asset, input) {
						var data = {
							"Type": "Ask Order Placement",
							"Asset Name": asset.name,
							"Quantity": transaction.attachment.quantity,
							"Price": transaction.attachment.price,
							"Total": transaction.attachment.quantity * transaction.attachment.price
						};

						if (transaction.sender != NRS.account) {
							data["Sender"] = NRS.getAccountTitle(transaction.sender);
						}

						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();

						$("#transaction_info_modal").modal("show");
						NRS.fetchingModalData = false;
					});

					break;
				case 3:
					async = true;

					NRS.sendRequest("getAsset", {
						"asset": transaction.attachment.asset
					}, function(asset, input) {
						var data = {
							"Type": "Bid Order Placement",
							"Asset Name": asset.name,
							"Quantity": transaction.attachment.quantity,
							"Price": transaction.attachment.price,
							"Total": transaction.attachment.quantity * transaction.attachment.price
						};

						if (transaction.sender != NRS.account) {
							data["Sender"] = NRS.getAccountTitle(transaction.sender);
						}

						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();

						$("#transaction_info_modal").modal("show");
						NRS.fetchingModalData = false;
					});

					break;
				case 4:
					async = true;

					NRS.sendRequest("getTransaction", {
						"transaction": transaction.attachment.order
					}, function(transaction, input) {
						if (transaction.attachment.asset) {
							NRS.sendRequest("getAsset", {
								"asset": transaction.attachment.asset
							}, function(asset) {
								var data = {
									"Type": "Ask Order Cancellation",
									"Asset Name": asset.name,
									"Quantity": transaction.attachment.quantity,
									"Price": transaction.attachment.price,
									"Total": transaction.attachment.quantity * transaction.attachment.price
								};

								if (transaction.sender != NRS.account) {
									data["Sender"] = NRS.getAccountTitle(transaction.sender);
								}

								$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
								$("#transaction_info_table").show();

								$("#transaction_info_modal").modal("show");
								NRS.fetchingModalData = false;
							});
						} else {
							NRS.fetchingModalData = false;
						}
					});

					break;
				case 5:
					async = true;

					NRS.sendRequest("getTransaction", {
						"transaction": transaction.attachment.order
					}, function(transaction) {
						if (transaction.attachment.asset) {
							NRS.sendRequest("getAsset", {
								"asset": transaction.attachment.asset
							}, function(asset) {
								var data = {
									"Type": "Bid Order Cancellation",
									"Asset Name": asset.name,
									"Quantity": transaction.attachment.quantity,
									"Price": transaction.attachment.price,
									"Total": transaction.attachment.quantity * transaction.attachment.price
								};

								if (transaction.sender != NRS.account) {
									data["Sender"] = NRS.getAccountTitle(transaction.sender);
								}

								$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
								$("#transaction_info_table").show();

								$("#transaction_info_modal").modal("show");
								NRS.fetchingModalData = false;
							});
						} else {
							NRS.fetchingModalData = false;
						}
					});

					break;
				default:
					incorrect = true;
					break;
			}
		}

		if (incorrect) {
			NRS.fetchingModalData = false;
			return;
		}

		if (!async) {
			$("#transaction_info_modal").modal("show");
			NRS.fetchingModalData = false;
		}
	}

	NRS.getAccountTitle = function(accountId) {
		if (accountId in NRS.contacts) {
			return NRS.contacts[accountId].name.escapeHTML();
		} else if (accountId == NRS.account) {
			return "You";
		} else {
			return NRS.getAccountFormatted(accountId);
		}
	}

	NRS.getAccountFormatted = function(accountId) {
		if (NRS.settings["use_new_address_format"]) {
			var address = new NxtAddress();

			if (address.set(accountId, true)) {
				return address.toString().escapeHTML();
			} else {
				return String(accountId).escapeHTML();
			}
		} else {
			return String(accountId).escapeHTML();
		}
	}

	//todo later: http://twitter.github.io/typeahead.js/
	$("span.recipient_selector button").on("click", function(e) {
		if (!Object.keys(NRS.contacts).length) {
			e.preventDefault();
			e.stopPropagation();
			return;
		}

		var $list = $(this).parent().find("ul");

		$list.empty();

		for (var accountId in NRS.contacts) {
			$list.append("<li><a href='#' data-contact='" + NRS.contacts[accountId].name.escapeHTML() + "'>" + NRS.contacts[accountId].name.escapeHTML() + "</a></li>");
		}
	});

	$("span.recipient_selector").on("click", "ul li a", function(e) {
		e.preventDefault();
		$(this).closest("form").find("input[name=recipient],input[name=account_id]").val($(this).data("contact")).trigger("blur");
	});

	/* GENERATE TOKEN */
	$("#generate_token_modal").on("show.bs.modal", function(e) {
		$("#generate_token_website").val("http://");
		$("#generate_token_token").html("").hide();
	});

	NRS.forms.generateToken = function($modal) {
		var url = $.trim($("#generate_token_website").val());

		if (!url || url == "http://") {
			return {
				"error": "Website is a required field."
			};
			$("#generate_token_token").html("").hide();
		} else {
			return {};
		}
	}

	NRS.forms.generateTokenComplete = function(response, data) {
		$("#generate_token_modal").find(".error_message").hide();

		if (response.token) {
			$("#generate_token_token").html("The generated token for <strong>" + data.website.escapeHTML() + "</strong> is: <br /><br /><textarea style='width:100%' rows='3'>" + response.token.escapeHTML() + "</textarea>").show();
		} else {
			$.growl("Could not generate token.", {
				"type": "danger"
			});
			$("#generate_token_modal").modal("hide");
		}
	}

	//hide modal when another one is activated.
	$(".modal").on("show.bs.modal", function(e) {
		var $visible_modal = $(".modal.in");

		if ($visible_modal.length) {
			$visible_modal.modal("hide");
		}
	});

	$(".modal button.btn-primary:not([data-dismiss=modal])").click(function() {
		NRS.submitForm($(this).closest(".modal"), $(this));
	});

	NRS.submitForm = function($modal, $btn) {
		if (!$btn) {
			$btn = $modal.find("button.btn-primary:not([data-dismiss=modal])");
		}

		var $modal = $btn.closest(".modal");

		$modal.modal("lock");
		$modal.find("button").prop("disabled", true);
		$btn.button("loading");

		var requestType = $modal.find("input[name=request_type]").val();
		var successMessage = $modal.find("input[name=success_message]").val();
		var errorMessage = $modal.find("input[name=error_message]").val();
		var data = null;

		var formFunction = NRS["forms"][requestType];

		var originalRequestType = requestType;

		if (typeof formFunction == 'function') {
			var output = formFunction($modal);

			if (!output) {
				return;
			} else if (output.error) {
				$modal.find(".error_message").html(output.error.escapeHTML()).show();
				NRS.unlockForm($modal, $btn);
				return;
			} else {
				if (output.requestType) {
					requestType = output.requestType;
				}
				if (output.data) {
					data = output.data;
				}
				if (output.successMessage) {
					successMessage = output.successMessage;
				}
				if (output.errorMessage) {
					errorMessage = output.errorMessage;
				}
				if (output.stop) {
					NRS.unlockForm($modal, $btn, true);
					return;
				}
			}
		}

		if (!data) {
			data = NRS.getFormData($modal.find("form:first"));
		}

		if (data.deadline) {
			data.deadline = String(data.deadline * 60); //hours to minutes
		}

		if (data.recipient) {
			data.recipient = $.trim(data.recipient);
			if (!/^\d+$/.test(data.recipient)) {
				var convertedAccountId = $modal.find("input[name=converted_account_id]").val();
				if (!convertedAccountId || !/^\d+$/.test(convertedAccountId)) {
					$modal.find(".error_message").html("Invalid account ID.").show();
					NRS.unlockForm($modal, $btn);
					return;
				} else {
					data.recipient = convertedAccountId;
					data["_extra"] = {
						"convertedAccount": true
					};
				}
			}
		}

		if ("secretPhrase" in data && !data.secretPhrase.length && !NRS.rememberPassword) {
			$modal.find(".error_message").html("Secret phrase is a required field.").show();
			NRS.unlockForm($modal, $btn);
			return;
		}

		NRS.sendRequest(requestType, data, function(response) {
			if (response.errorCode) {
				if (NRS.forms.errorMessages[requestType] && NRS.forms.errorMessages[requestType][response.errorCode]) {
					$modal.find(".error_message").html(NRS.forms.errorMessages[requestType][response.errorCode].escapeHTML()).show();
				} else if (NRS.forms.errorMessages[originalRequestType] && NRS.forms.errorMessages[originalRequestType][response.errorCode]) {
					$modal.find(".error_message").html(NRS.forms.errorMessages[originalRequestType][response.errorCode].escapeHTML()).show();
				} else {
					$modal.find(".error_message").html(response.errorDescription ? response.errorDescription.escapeHTML() : "Unknown error occured.").show();
				}
				NRS.unlockForm($modal, $btn);
			} else if (response.hash) {
				//should we add a fake transaction to the recent transactions?? or just wait until the next block comes!??
				NRS.unlockForm($modal, $btn);

				if (!$modal.hasClass("modal-no-hide")) {
					$modal.modal("hide");
				}

				if (successMessage) {
					$.growl(successMessage.escapeHTML(), {
						type: 'success'
					});
				}

				var formCompleteFunction = NRS["forms"][originalRequestType + "Complete"];

				if (typeof formCompleteFunction == 'function') {
					data.requestType = requestType;
					formCompleteFunction(response, data);
				}

				if (NRS.accountBalance && !NRS.accountBalance.publicKey) {
					$("#dashboard_message").hide();
				}
			} else {
				var sentToFunction = false;

				if (!errorMessage) {
					var formCompleteFunction = NRS["forms"][originalRequestType + "Complete"];

					if (typeof formCompleteFunction == 'function') {
						sentToFunction = true;
						data.requestType = requestType;

						NRS.unlockForm($modal, $btn);

						if (!$modal.hasClass("modal-no-hide")) {
							$modal.modal("hide");
						}
						formCompleteFunction(response, data);
					} else {
						errorMessage = "An unknown error occured.";
					}
				}

				if (!sentToFunction) {
					NRS.unlockForm($modal, $btn, true);

					$.growl(errorMessage.escapeHTML(), {
						type: 'danger'
					});
				}
			}
		});
	}

	NRS.unlockForm = function($modal, $btn, hide) {
		$modal.find("button").prop("disabled", false);
		if ($btn) {
			$btn.button("reset");
		}
		$modal.modal("unlock");
		if (hide) {
			$modal.modal("hide");
		}
	}

	$("#send_message_modal, #send_money_modal, #add_contact_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var account = $invoker.data("account");

		if (account) {
			account = NRS.getAccountFormatted(account);
		} else {
			account = $invoker.data("contact");
		}

		if (account) {
			$(this).find("input[name=recipient], input[name=account_id]").val(account.unescapeHTML()).trigger("blur");
		}
	});

	$("#send_money_amount").on("input", function(e) {
		var amount = parseInt($(this).val(), 10);
		$("#send_money_fee").val(isNaN(amount) ? "1" : (amount < 500 ? 1 : Math.round(amount / 1000)));
	});

	NRS.sendMoneyShowAccountInformation = function(accountId) {
		NRS.getAccountError(accountId, function(response) {
			if (response.type == "success") {
				$("#send_money_account_info").hide();
			} else {
				$("#send_money_account_info").html(response.message).show();

			}
		});
	}

	NRS.getAccountError = function(accountId, callback) {
		NRS.sendRequest("getAccount", {
			"account": accountId
		}, function(response) {
			if (response.publicKey) {
				if (NRS.useNQT) {
					var balance = new BigInteger(response.unconfirmedBalanceNQT);
				} else {
					var balance = (response.balance / 100) || 0;
				}
				callback({
					"type": "info",
					"message": "The recipient account has a public key and a balance of " + NRS.formatAmount(balance, false, true) + "NXT."
				});
			} else {
				if (response.errorCode) {
					if (response.errorCode == 4) {
						callback({
							"type": "danger",
							"message": "The recipient account is malformed, please adjust. If you want to type an alias, prepend it with the @ character."
						});
					} else if (response.errorCode == 5) {
						callback({
							"type": "warning",
							"message": "The recipient account is an unknown account, meaning it has never had an incoming or outgoing transaction. Please double check your recipient address before submitting."
						});
					} else {
						callback({
							"type": "danger",
							"message": "There is a problem with the recipient account: " + response.errorDescription
						});
					}
				} else {
					if (NRS.useNQT) {
						var balance = new BigInteger(response.unconfirmedBalanceNQT);
					} else {
						var balance = (response.balance / 100) || 0;
					}
					callback({
						"type": "warning",
						"message": "The recipient account does not have a public key, meaning it has never had an outgoing transaction. The account has a balance of " + NRS.formatAmount(balance, false, true) + " NXT. Please double check your recipient address before submitting."
					});
				}
			}
		});
	}

	NRS.correctAddressMistake = function(el) {
		$(el).closest(".modal-body").find("input[name=recipient],input[name=account_id]").val($(el).data("address")).trigger("blur");
	}

	NRS.checkRecipient = function(account, modal) {
		var classes = "callout-info callout-danger callout-warning";

		var callout = modal.find(".account_info").first();
		var accountInputField = modal.find("input[name=converted_account_id]");

		accountInputField.val("");

		account = $.trim(account);

		//solomon reed. Btw, this regex can be shortened..
		if (/^(NXT\-)?[A-Z0-9]+\-[A-Z0-9]+\-[A-Z0-9]+\-[A-Z0-9]+/i.test(account)) {
			account = account.replace(/^NXT\-/i, "");

			var address = new NxtAddress();

			if (address.set(account)) {
				var accountId = address.account_id();

				NRS.getAccountError(accountId, function(response) {
					callout.removeClass(classes).addClass("callout-" + response.type).html("The recipient address translates to account <strong>" + String(accountId).escapeHTML() + "</strong>, " + response.message.replace("The recipient account", "which")).show();
					if (response.type == "info" || response.type == "warning") {
						accountInputField.val(accountId);
					}
				});
			} else {
				if (address.guess.length == 1) {

					callout.removeClass(classes).addClass("callout-danger").html("The recipient address is malformed, did you mean <span class='malformed_address' data-address='" + String(address.guess[0]).escapeHTML() + "' onclick='NRS.correctAddressMistake(this);'>" + address.format_guess(address.guess[0], account) + "</span> ?").show();
				} else if (address.guess.length > 1) {
					var html = "The recipient address is malformed, did you mean:<ul>";
					for (var i = 0; i < adr.guess.length; i++) {
						html += "<li><span clas='malformed_address' data-address='" + String(address.guess[i]).escapeHTML() + "' onclick='NRS.correctAddressMistake(this);'>" + adddress.format_guess(address.guess[i], account) + "</span></li>";
					}

					callout.removeClass(classes).addClass("callout-danger").html(html).show();
				} else {
					callout.removeClass(classes).addClass("callout-danger").html("The recipient address is malformed, please adjust.").show();
				}
			}
		} else if (!(/^\d+$/.test(account))) {
			if (NRS.databaseSupport && account.charAt(0) != '@') {
				NRS.database.select("contacts", [{
					"name": account
				}], function(error, contact) {
					if (!error && contact.length) {
						contact = contact[0];
						NRS.getAccountError(contact.accountId, function(response) {
							callout.removeClass(classes).addClass("callout-" + response.type).html("The contact links to account <strong>" + String(contact.accountId).escapeHTML() + "</strong>. " + response.message.escapeHTML()).show();

							if (response.type == "info" || response.type == "warning") {
								accountInputField.val(contact.accountId);
							}
						});
					} else if (/^[a-z0-9]+$/i.test(account)) {
						NRS.checkRecipientAlias(account, modal);
					} else {
						callout.removeClass(classes).addClass("callout-danger").html("The recipient account is malformed, please adjust.").show();
					}
				});
			} else if (/^[a-z0-9@]+$/i.test(account)) {
				if (account.charAt(0) == '@') {
					account = account.substring(1);
					NRS.checkRecipientAlias(account, modal);
				}
			} else {
				callout.removeClass(classes).addClass("callout-danger").html("The recipient account is malformed, please adjust.").show();
			}
		} else {
			NRS.getAccountError(account, function(response) {
				callout.removeClass(classes).addClass("callout-" + response.type).html(response.message.escapeHTML()).show();
			});
		}
	}

	NRS.checkRecipientAlias = function(account, modal) {
		var classes = "callout-info callout-danger callout-warning";
		var callout = modal.find(".account_info").first();
		var accountInputField = modal.find("input[name=converted_account_id]");

		accountInputField.val("");

		NRS.sendRequest("getAliasId", {
			"alias": account
		}, function(response) {
			if (response.id) {
				NRS.sendRequest("getAlias", {
					"alias": response.id
				}, function(response) {
					if (response.errorCode) {
						callout.removeClass(classes).addClass("callout-danger").html(response.errorDescription ? "Error: " + response.errorDescription.escapeHTML() : "The alias does not exist.").show();
					} else {
						if (response.uri) {
							var alias = response.uri;
							var timestamp = response.timestamp;

							var regex_1 = /acct:(\d+)@nxt/;
							var regex_2 = /nacc:(\d+)/;

							var match = alias.match(regex_1);

							if (!match) {
								match = alias.match(regex_2);
							}

							if (match && match[1]) {
								NRS.getAccountError(match[1], function(response) {
									accountInputField.val(match[1].escapeHTML());
									callout.html("The alias links to account <strong>" + match[1].escapeHTML() + "</strong>, " + response.message.replace("The recipient account", "which") + "The alias was last adjusted on " + NRS.formatTimestamp(timestamp) + ".").removeClass(classes).addClass("callout-" + response.type).show();
								});
							} else {
								callout.removeClass(classes).addClass("callout-danger").html("The alias does not link to an account. " + (!alias ? "The URI is empty." : "The URI is '" + alias.escapeHTML() + "'")).show();
							}
						} else if (response.alias) {
							callout.removeClass(classes).addClass("callout-danger").html("The alias links to an empty URI.").show();
						} else {
							callout.removeClass(classes).addClass("callout-danger").html(response.errorDescription ? "Error: " + response.errorDescription.escapeHTML() : "The alias does not exist.").show();
						}
					}
				});
			} else {
				callout.removeClass(classes).addClass("callout-danger").html(response.errorDescription ? "Error: " + response.errorDescription.escapeHTML() : "The alias does not exist.").show();
			}
		});
	}

	NRS.createDatabase = function(callback) {
		var schema = {
			contacts: {
				id: {
					"primary": true,
					"autoincrement": true,
					"type": "NUMBER"
				},
				name: "VARCHAR(100) COLLATE NOCASE",
				email: "VARCHAR(200)",
				accountId: "VARCHAR(25)",
				description: "TEXT"
			},
			assets: {
				account: "VARCHAR(25)",
				asset: {
					"primary": true,
					"type": "VARCHAR(25)"
				},
				description: "TEXT",
				name: "VARCHAR(10)",
				position: "NUMBER",
				decimals: "NUMBER",
				quantityQNT: "VARCHAR(15)",
				groupName: "VARCHAR(30) COLLATE NOCASE"
			},
			data: {
				id: {
					"primary": true,
					"type": "VARCHAR(40)"
				},
				contents: "TEXT"
			}
		};

		try {
			NRS.database = new WebDB("NRS2", schema, 1, 4, function(error, db) {
				if (!error) {
					NRS.databaseSupport = true;

					NRS.loadContacts();
					NRS.database.select("data", [{
						"id": "closed_groups"
					}], function(error, result) {
						if (result.length) {
							NRS.closedGroups = result[0].contents.split("#");
						} else {
							NRS.database.insert("data", {
								id: "closed_groups",
								contents: ""
							});
						}
					});
					if (callback) {
						callback();
					}
				}
			});
		} catch (err) {
			NRS.database = null;
			NRS.databaseSupport = false;
		}
	}

	NRS.setupClipboardFunctionality = function() {
		var elements = "#asset_id_dropdown .dropdown-menu a, #account_id_dropdown .dropdown-menu a";

		if (NRS.isLocalHost) {
			$("#account_id_dropdown li.remote_only").remove();
		}

		var $el = $(elements);

		var clipboard = new ZeroClipboard($el, {
			moviePath: "js/3rdparty/zeroclipboard.swf"
		});


		clipboard.on("dataRequested", function(client, args) {
			switch ($(this).data("type")) {
				case "account_id":
					client.setText(NRS.account);
					break;
				case "new_address_format":
					var address = new NxtAddress();

					if (address.set(NRS.account, true)) {
						client.setText(address.toString());
					} else {
						client.setText(NRS.account);
					}

					break;
				case "message_link":
					client.setText(document.URL.replace(/#.*$/, "") + "#message:" + NRS.account);
					break;
				case "send_link":
					client.setText(document.URL.replace(/#.*$/, "") + "#send:" + NRS.account);
					break;
				case "asset_id":
					client.setText($("#asset_id").text());
					break;
				case "asset_link":
					client.setText(document.URL.replace(/#.*/, "") + "#asset:" + $("#asset_id").text());
					break;
			}
		});

		if ($el.hasClass("dropdown-toggle")) {
			$el.removeClass("dropdown-toggle").data("toggle", "");
			$el.parent().remove(".dropdown-menu");
		}

		clipboard.on("complete", function(client, args) {
			$.growl("Copied to the clipboard successfully.", {
				"type": "success"
			});
		});

		clipboard.on("noflash", function(client, args) {
			$.growl("Your browser doesn't support flash, therefore copy to clipboard functionality will not work.", {
				"type": "danger"
			});
		});
		clipboard.on("wrongflash", function(client, args) {
			$.growl("Your browser flash version is too old. The copy to clipboard functionality needs version 10 or newer.");
		});
	}

	NRS.checkLocationHash = function(password) {
		if (window.location.hash) {
			var hash = window.location.hash.replace("#", "").split(":")

			if (hash.length == 2) {
				if (hash[0] == "message") {
					var $modal = $("#send_message_modal");
				} else if (hash[0] == "send") {
					var $modal = $("#send_money_modal");
				} else if (hash[0] == "asset") {
					NRS.goToAsset(hash[1]);
					return;
				} else {
					var $modal = "";
				}

				if ($modal) {
					var account_id = String($.trim(hash[1]));
					if (!/^\d+$/.test(account_id) && account_id.indexOf("@") !== 0) {
						account_id = "@" + account_id;
					}

					$modal.find("input[name=recipient]").val(account_id.unescapeHTML()).trigger("blur");
					if (password && typeof password == "string") {
						$modal.find("input[name=secretPhrase]").val(password);
					}
					$modal.modal("show");
				}
			}

			window.location.hash = "#";
		}
	}

	NRS.getAccountBalance = function(firstRun) {
		NRS.sendRequest("getAccount", {
			"account": NRS.account
		}, function(response) {
			var previousAccountBalance = NRS.accountBalance;

			NRS.accountBalance = response;

			if (response.errorCode) {
				$("#account_balance").html("0");
				$("#account_nr_assets").html("0");

				if (NRS.accountBalance.errorCode == 5) {
					$("#dashboard_message").addClass("alert-success").removeClass("alert-danger").html("Welcome to your brand new account. You should fund it with some coins. Your account ID is: <strong>" + NRS.account + "</strong>").show();
				} else {
					$("#dashboard_message").addClass("alert-danger").removeClass("alert-success").html(NRS.accountBalance.errorDescription ? NRS.accountBalance.errorDescription.escapeHTML() : "An unknown error occured.").show();
				}
			} else {
				if (!NRS.accountBalance.publicKey) {
					$("#dashboard_message").addClass("alert-danger").removeClass("alert-success").html("<b>Warning!</b>: Your account does not have a public key! This means it's not as protected as other accounts. You must make an outgoing transaction to fix this issue. (<a href='#' data-toggle='modal' data-target='#send_message_modal'>send a message</a>, <a href='#' data-toggle='modal' data-target='#register_alias_modal'>buy an alias</a>, <a href='#' data-toggle='modal' data-target='#send_money_modal'>send Nxt</a>, ...)").show();
				} else {
					$("#dashboard_message").hide();
				}

				if (NRS.databaseSupport) {
					NRS.database.select("data", [{
						"id": "asset_balances_" + NRS.account
					}], function(error, asset_balance) {
						if (!error && asset_balance.length) {
							var previous_balances = asset_balance[0].contents;

							if (!NRS.accountBalance.assetBalances) {
								NRS.accountBalance.assetBalances = [];
							}

							var current_balances = JSON.stringify(NRS.accountBalance.assetBalances);

							if (previous_balances != current_balances) {
								if (previous_balances != "undefined") {
									previous_balances = JSON.parse(previous_balances);
								} else {
									previous_balances = [];
								}
								NRS.database.update("data", {
									contents: current_balances
								}, [{
									id: "asset_balances_" + NRS.account
								}]);
								NRS.checkAssetDifferences(NRS.accountBalance.assetBalances, previous_balances);
							}
						} else {
							NRS.database.insert("data", {
								id: "asset_balances_" + NRS.account,
								contents: JSON.stringify(NRS.accountBalance.assetBalances)
							});
						}
					});
				} else if (previousAccountBalance && previousAccountBalance.assetBalances) {
					var previous_balances = JSON.stringify(previousAccountBalance.assetBalances);
					var current_balances = JSON.stringify(NRS.accountBalance.assetBalances);

					if (previous_balances != current_balances) {
						NRS.checkAssetDifferences(NRS.accountBalance.assetBalances, previousAccountBalance.assetBalances);
					}
				}

				if (NRS.useNQT) {
					var balance = NRS.formatAmount(new BigInteger(response.unconfirmedBalanceNQT));
					balance = balance.split(".");
					if (balance.length == 2) {
						balance = balance[0] + "<span style='font-size:12px'>." + balance[1] + "</span>";
					} else {
						balance = balance[0];
					}
				} else {
					var balance = NRS.formatAmount(response.unconfirmedBalance / 100);
				}

				$("#account_balance").html(balance);

				var nr_assets = 0;

				if (response.assetBalances) {
					for (var i = 0; i < response.assetBalances.length; i++) {
						if ((NRS.useNQT && response.assetBalances[i].balanceNQT != "0") || (!NRS.useNQT && response.assetBalances[i].balance > 0)) {
							nr_assets++;
						}
					}
				}

				$("#account_nr_assets").html(nr_assets);
			}

			if (firstRun) {
				$("#account_balance, #account_nr_assets").removeClass("loading_dots");
			}
		});
	}

	NRS.checkAssetDifferences = function(current_balances, previous_balances) {
		var current_balances_ = {};
		var previous_balances_ = {};

		for (var k in previous_balances) {
			previous_balances_[previous_balances[k].asset] = previous_balances[k].balance;
		}

		for (var k in current_balances) {
			current_balances_[current_balances[k].asset] = current_balances[k].balance;
		}

		var diff = {};

		for (var k in previous_balances_) {
			if (!(k in current_balances_)) {
				diff[k] = -(previous_balances_[k]);
			} else if (previous_balances_[k] !== current_balances_[k]) {
				var change = current_balances_[k] - previous_balances_[k];
				diff[k] = change;
			}
		}

		for (k in current_balances_) {
			if (!(k in previous_balances_)) {
				diff[k] = current_balances_[k]; // property is new
			}
		}

		var nr = Object.keys(diff).length;

		if (nr == 0) {
			return;
		} else if (nr <= 3) {
			for (k in diff) {
				NRS.sendRequest("getAsset", {
					"asset": k,
					"_extra": {
						"id": k,
						"difference": diff[k]
					}
				}, function(asset, input) {
					asset.difference = input["_extra"].difference;
					asset.id = input["_extra"].id;

					if (asset.difference > 0) {
						$.growl("You received <a href='#' data-goto-asset='" + String(asset.id).escapeHTML() + "'>" + NRS.formatAmount(asset.difference) + " " + asset.name.escapeHTML() + (asset.difference == 1 ? " asset" : " assets") + "</a>.", {
							"type": "success"
						});
					} else {
						$.growl("You sold <a href='#' data-goto-asset='" + String(asset.id).escapeHTML() + "'>" + NRS.formatAmount(Math.abs(asset.difference)) + " " + asset.name.escapeHTML() + (asset.difference == 1 ? " asset" : "assets") + "</a>.", {
							"type": "success"
						});
					}
				});
			}
		} else {
			$.growl("Multiple different assets have been sold and/or bought.", {
				"type": "success"
			});
		}
	}

	NRS.handleInitialTransactions = function(transactions, transactionIds) {
		if (transactions.length) {
			var rows = "";

			transactions.sort(NRS.sortArray);

			if (transactions.length >= 1) {
				NRS.lastTransactions = transactionIds.toString();

				for (var i = transactions.length - 1; i >= 0; i--) {
					if (transactions[i].confirmed) {
						NRS.lastTransactionsTimestamp = transactions[i].timestamp;
						break;
					}
				}
			}

			for (var i = 0; i < transactions.length; i++) {
				var transaction = transactions[i];

				var receiving = transaction.recipient == NRS.account;
				var account = (receiving ? String(transaction.sender).escapeHTML() : String(transaction.recipient).escapeHTML());

				if (transaction.amountNQT) {
					transaction.amount = new BigInteger(transaction.amountNQT);
					transaction.fee = new BigInteger(transaction.feeNQT);
				}

				//todo: !receiving && transaction.amount NQT

				//todo transactionIds!!

				rows += "<tr class='" + (!transaction.confirmed ? "tentative" : "confirmed") + "'><td>" + (transaction.attachment ? "<a href='#' data-transaction='" + String(transaction.id).escapeHTML() + "' style='font-weight:bold'>" + NRS.formatTimestamp(transaction.timestamp) + "</a>" : NRS.formatTimestamp(transaction.timestamp)) + "</td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fa fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fa fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td><span" + (transaction.type == 0 && receiving ? " style='color:#006400'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + NRS.formatAmount(transaction.amount) + "</span> <span" + ((!receiving && transaction.type == 0) ? " style='color:red'" : "") + ">+</span> <span" + (!receiving ? " style='color:red'" : "") + ">" + NRS.formatAmount(transaction.fee) + "</span></td><td>" + (account != NRS.genesis ? "<a href='#' data-user='" + account + "' class='user_info'>" + NRS.getAccountTitle(account) + "</a>" : "Genesis") + "</td><td class='confirmations' data-confirmations='" + String(transaction.confirmations).escapeHTML() + "' data-initial='true'>" + (transaction.confirmations > 10 ? "10+" : String(transaction.confirmations).escapeHTML()) + "</td></tr>";
			}

			$("#dashboard_transactions_table tbody").empty().append(rows);
		}

		NRS.dataLoadFinished($("#dashboard_transactions_table"));
	}

	NRS.sortArray = function(a, b) {
		return b.timestamp - a.timestamp;
	}

	NRS.forms.errorMessages.startForging = {
		"5": "You cannot forge. Either your balance is 0 or your account is too new (you must wait a day or so)."
	};

	NRS.forms.startForgingComplete = function(response, data) {
		if ("deadline" in response) {
			$("#forging_indicator i.fa").removeClass("text-danger").addClass("text-success");
			$("#forging_indicator span").html("Forging");
			NRS.isForging = true;
			$.growl("Forging started successfully.", {
				type: "success"
			});
		} else {
			NRS.isForging = false;
			$.growl("Couldn't start forging, unknown error.", {
				type: 'danger'
			});
		}
	}

	NRS.forms.stopForgingComplete = function(response, data) {
		if ($("#stop_forging_modal .show_logout").css("display") == "inline") {
			NRS.logout();
			return;
		}

		$("#forging_indicator i.fa").removeClass("text-success").addClass("text-danger");
		$("#forging_indicator span").html("Not forging");

		NRS.isForging = false;

		if (response.foundAndStopped) {
			$.growl("Forging stopped successfully.", {
				type: 'success'
			});
		} else {
			$.growl("You weren't forging to begin with.", {
				type: 'danger'
			});
		}
	}

	$("#forging_indicator").click(function(e) {
		e.preventDefault();

		var $forgingIndicator = $(this).find("i.fa-circle");

		if ($forgingIndicator.hasClass("text-success")) {
			$("#stop_forging_modal").modal("show");
		} else {
			$("#start_forging_modal").modal("show");
		}
	});

	NRS.verifyTransactionBytes = function(transactionBytes, requestType, data) {
		var transaction = {};

		var currentPosition = 0;

		var byteArray = converters.hexStringToByteArray(transactionBytes);

		transaction.type = byteArray[0];
		transaction.subType = byteArray[1];
		transaction.timestamp = String(converters.byteArrayToSignedInt32(byteArray, 2));
		transaction.deadline = String(converters.byteArrayToSignedShort(byteArray, 6));
		//sender public key == bytes 8 - 39
		transaction.recipient = String(converters.byteArrayToBigInteger(byteArray, 40));
		transaction.amount = String(converters.byteArrayToSignedInt32(byteArray, 48));
		transaction.fee = String(converters.byteArrayToSignedInt32(byteArray, 52));
		transaction.referencedTransaction = String(converters.byteArrayToBigInteger(byteArray, 56));

		if (transaction.referencedTransaction == "0") {
			transaction.referencedTransaction = null;
		}

		//signature == 64 - 127

		if (!("amount" in data)) {
			data.amount = "0";
		}

		if (!("recipient" in data)) {
			//recipient == genesis
			data.recipient = "1739068987193023818";
		}

		if (transaction.deadline !== data.deadline || transaction.recipient !== data.recipient || transaction.amount !== data.amount || transaction.fee !== data.fee) {
			return false;
		}

		if ("referencedTransaction" in data && transaction.referencedTransaction !== data.referencedTransaction) {
			return false;
		}

		var pos = 128;

		switch (requestType) {
			case "sendMoney":
				if (transaction.type !== 0 || transaction.subType !== 0) {
					return false;
				}
				break;
			case "sendMessage":
				if (transaction.type !== 1 || transaction.subType !== 0) {
					return false;
				}

				var message_length = String(converters.byteArrayToSignedInt32(byteArray, pos));

				pos += 4;

				var slice = byteArray.slice(pos, pos + message_length);

				transaction.message = converters.byteArrayToHexString(slice);

				if (transaction.message !== data.message) {
					return false;
				}
				break;
			case "assignAlias":
				if (transaction.type !== 1 || transaction.subType !== 1) {
					return false;
				}

				var alias_length = parseInt(byteArray[pos], 10);

				pos++;

				transaction.alias = converters.byteArrayToString(byteArray, pos, alias_length);

				pos += alias_length;

				var uri_length = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.uri = converters.byteArrayToString(byteArray, pos, uri_length);

				if (transaction.alias !== data.alias || transaction.uri !== data.uri) {
					return false;
				}
				break;
			case "createPoll":
				if (transaction.type !== 1 || transaction.subType !== 2) {
					return false;
				}

				var name_length = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.name = converters.byteArrayToString(byteArray, pos, name_length);

				pos += name_length;

				var description_length = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.description = converters.byteArrayToString(byteArray, pos, description_length);

				pos += description_length;

				var nr_options = byteArray[pos];

				pos++;

				for (var i = 0; i < nr_options; i++) {
					var option_length = converters.byteArrayToSignedShort(byteArray, pos);

					pos += 2;

					transaction["option" + i] = converters.byteArrayToString(byteArray, pos, option_length);

					pos += option_length;
				}

				transaction.minNumberOfOptions = String(byteArray[pos]);

				pos++;

				transaction.maxNumberOfOptions = String(byteArray[pos]);

				pos++;

				transaction.optionsAreBinary = String(byteArray[pos]);

				if (transaction.name !== data.name || transaction.description !== data.description || transaction.minNumberOfOptions !== data.minNumberOfOptions || transaction.maxNumberOfOptions !== data.maxNumberOfOptions || transaction.optionsAreBinary !== data.optionsAreBinary) {
					return false;
				}

				for (var i = 0; i < nr_options; i++) {
					if (transaction["option" + i] !== data["option" + i]) {
						return false;
					}
				}

				if (("option" + i) in data) {
					return false;
				}

				break;
			case "castVote":
				if (transaction.type !== 1 || transaction.subType !== 3) {
					return false;
				}

				transaction.poll = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				var vote_length = byteArray[pos];

				pos++;

				transaction.votes = [];

				for (var i = 0; i < vote_length; i++) {
					transaction.votes.push(bytesArray[pos]);

					pos++;
				}

				return false;
				break;
			case "issueAsset":
				if (transaction.type !== 2 || transaction.subType !== 0) {
					return false;
				}

				var name_length = byteArray[pos];

				pos++;

				transaction.name = converters.byteArrayToString(byteArray, pos, name_length);

				pos += name_length;

				var description_length = converters.byteArrayToSignedShort(byteArray, pos); //6-7

				pos += 2;

				transaction.description = converters.byteArrayToString(byteArray, pos, description_length);

				pos += description_length;

				transaction.quantity = String(converters.byteArrayToSignedInt32(byteArray, pos));

				if (transaction.name !== data.name || transaction.description !== data.description || transaction.quantity !== data.quantity) {
					return false;
				}
				break;
			case "transferAsset":
				if (transaction.type !== 2 || transaction.subType !== 1) {
					return false;
				}

				transaction.asset = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.quantity = String(converters.byteArrayToSignedInt32(byteArray, pos));

				if (transaction.asset !== data.asset || transaction.quantity !== data.quantity) {
					return false;
				}
				break;
			case "placeAskOrder":
			case "placeBidOrder":
				if (transaction.type !== 2) {
					return false;
				} else if (requestType == "placeAskOrder" && transaction.subType !== 2) {
					return false;
				} else if (requestType == "placeBidOrder" && transaction.subType !== 3) {
					return false;
				}

				transaction.asset = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.quantity = String(converters.byteArrayToSignedInt32(byteArray, pos));

				pos += 4;

				transaction.price = String(converters.byteArrayToBigInteger(byteArray, pos));

				if (transaction.asset !== data.asset || transaction.quantity !== data.quantity || transaction.price !== data.price) {
					return false;
				}
				break;
			case "cancelAskOrder":
			case "cancelBidOrder":
				if (transaction.type !== 2) {
					return false;
				} else if (requestType == "cancelAskOrder" && transaction.subType !== 4) {
					return false;
				} else if (requestType == "cancelBidOrder" && transaction.subType !== 5) {
					return false;
				}

				transaction.order = String(converters.byteArrayToBigInteger(byteArray, pos));

				if (transaction.order !== data.order) {
					return false;
				}

				break;
			default:
				//invalid requestType..
				return false;
		}

		return true;
	}

	NRS.broadcastTransactionBytes = function(transactionData, callback, original_response, original_data) {
		$.ajax({
			url: NRS.server + "/nxt?requestType=broadcastTransaction",
			crossDomain: true,
			dataType: "json",
			type: "POST",
			timeout: 20000, //20 seconds
			async: true,
			data: {
				"transactionBytes": transactionData
			}
		}).done(function(response, status, xhr) {
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, response);
			}

			if (callback) {
				if (response.errorCode && !response.errorDescription) {
					response.errorDescription = (response.errorMessage ? response.errorMessage : "Unknown error occured.");
					callback(response, original_data);
				} else {
					callback(original_response, original_data);
				}
			}
		}).fail(function(xhr, textStatus, error) {
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, error, true);
			}

			if (callback) {
				if (error == "timeout") {
					error = "The request timed out. Warning: This does not mean the request did not go through. You should wait for the next block and see if your request has been processed.";
				}
				callback({
					"errorCode": -1,
					"errorDescription": error
				}, {});
			}
		});
	}

	$(".modal").on("shown.bs.modal", function() {
		$(this).find("input[type=text]:first, input[type=password]:first").first().focus();
		$(this).find("input[name=converted_account_id]").val("");
	});

	//Reset form to initial state when modal is closed
	$(".modal").on("hidden.bs.modal", function(e) {
		$(this).find(":input:not([type=hidden],button)").each(function(index) {
			var default_value = $(this).data("default");
			var type = $(this).attr("type");

			if (type == "checkbox") {
				if (default_value == "checked") {
					$(this).prop("checked", true);
				} else {
					$(this).prop("checked", false);
				}
			} else {
				if (default_value) {
					$(this).val(default_value);
				} else {
					$(this).val("");
				}
			}
		});

		//Hidden form field
		$(this).find("input[name=converted_account_id]").val("");

		//Hide/Reset any possible error messages
		$(this).find(".callout-danger:not(.never_hide), .error_message, .account_info").html("").hide();
	});

	$(".sidebar_context").on("contextmenu", "a", function(e) {
		e.preventDefault();

		if (!NRS.databaseSupport) {
			return;
		}

		NRS.closeContextMenu();

		if ($(this).hasClass("no-context")) {
			return;
		}

		NRS.selectedContext = $(this);

		NRS.selectedContext.addClass("context");

		$(document).on("click.contextmenu", NRS.closeContextMenu);

		var contextMenu = $(this).data("context");

		if (!contextMenu) {
			contextMenu = $(this).closest(".list-group").attr("id") + "_context";
		}

		var $contextMenu = $("#" + contextMenu);

		if ($contextMenu.length) {
			var $options = $contextMenu.find("ul.dropdown-menu a");

			$.each($options, function() {
				var requiredClass = $(this).data("class");

				if (!requiredClass) {
					$(this).show();
				} else if (NRS.selectedContext.hasClass(requiredClass)) {
					$(this).show();
				} else {
					$(this).hide();
				}
			});

			$contextMenu.css({
				display: "block",
				left: e.pageX,
				top: e.pageY
			});
		}

		return false;
	});

	NRS.closeContextMenu = function(e) {
		if (e && e.which == 3) {
			return;
		}

		$(".context_menu").hide();

		if (NRS.selectedContext) {
			NRS.selectedContext.removeClass("context");
			//NRS.selectedContext = null;
		}

		$(document).off("click.contextmenu");
	}

	NRS.dataLoadFinished = function($table, fadeIn) {
		var $parent = $table.parent();

		if (fadeIn) {
			$parent.hide();
		}

		$parent.removeClass("data-loading");

		var extra = $parent.data("extra");

		if ($table.find("tbody tr").length > 0) {
			$parent.removeClass("data-empty");
			if ($parent.data("no-padding")) {
				$parent.parent().addClass("no-padding");
			}

			if (extra) {
				$(extra).show();
			}
		} else {
			$parent.addClass("data-empty");
			if ($parent.data("no-padding")) {
				$parent.parent().removeClass("no-padding");
			}
			if (extra) {
				$(extra).hide();
			}
		}

		if (fadeIn) {
			$parent.fadeIn();
		}
	}

	return NRS;
}(NRS || {}, jQuery));

$(document).ready(function() {
	NRS.init();
});

window.addEventListener("message", receiveMessage, false);

function receiveMessage(event) {
	if (event.origin != "file://") {
		return;
	}

	//parent.postMessage("from iframe", "file://");
}