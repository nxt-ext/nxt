var NRS = (function(NRS, $, undefined) {
	"use strict";

	NRS.server = "";
	NRS.state = {};
	NRS.blocks = [];
	NRS.genesis = "1739068987193023818";

	NRS.account = "";
	NRS.accountInfo = {};

	NRS.database = null;
	NRS.databaseSupport = false;

	NRS.settings = {};
	NRS.contacts = {};

	NRS.useNQT = false;
	NRS.isTestNet = false;
	NRS.isLocalHost = false;
	NRS.isForging = false;
	NRS.isLeased = false;

	NRS.lastBlockHeight = 0;
	NRS.downloadingBlockchain = false;
	NRS.blockchainCalculationServers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 12];

	NRS.rememberPassword = false;
	NRS.selectedContext = null;

	NRS.currentPage = "dashboard";
	NRS.pages = {};
	NRS.incoming = {};

	NRS.init = function() {
		if (location.port && location.port != "6876") {
			$(".testnet_only").hide();
		} else {
			NRS.isTestNet = true;
			NRS.blockchainCalculationServers = [9, 10];
			$(".testnet_only, #testnet_login, #testnet_warning").show();
		}

		NRS.useNQT = (NRS.isTestNet && NRS.lastBlockHeight >= 76500) || (!NRS.isTestNet && NRS.lastBlockHeight >= 132000);

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

		//every 30 seconds check for new block..
		setInterval(function() {
			NRS.getState();
		}, 1000 * 30);

		setInterval(NRS.checkAliasVersions, 1000 * 60 * 60);

		NRS.allowLoginViaEnter();

		NRS.automaticallyCheckRecipient();

		$(".show_popover").popover({
			"trigger": "hover"
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
					NRS.tempBlocks = [];
					NRS.state = response;
					if (NRS.account) {
						NRS.getAccountInfo();
					}
					NRS.getBlock(NRS.state.lastBlock, NRS.handleNewBlocks);
					if (NRS.account) {
						NRS.getNewTransactions();
					}
				} else {
					if (NRS.account) {
						NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
							NRS.handleIncomingTransactions(unconfirmedTransactions, false);
						});
					}
				}

				if (callback) {
					callback();
				}
			}
		});
	}

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
			NRS.abortOutstandingRequests();

			if (data && data.callback) {
				data.callback();
			}
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

	NRS.pageLoading = function() {
		var $pageHeader = $("#" + NRS.currentPage + "_page .content-header h1");
		$pageHeader.find(".loading_dots").remove();
		$pageHeader.append("<span class='loading_dots'><span>.</span><span>.</span><span>.</span></span>");
	}

	NRS.pageLoaded = function(callback) {
		$("#" + NRS.currentPage + "_page .content-header h1").find(".loading_dots").remove();
		if (callback) {
			callback();
		}
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
			NRS.database = new WebDB("NRSDBC", schema, 1, 4, function(error, db) {
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

	NRS.getAccountInfo = function(firstRun, callback) {
		NRS.sendRequest("getAccount", {
			"account": NRS.account
		}, function(response) {
			var previousAccountInfo = NRS.accountInfo;

			NRS.accountInfo = response;

			if (response.errorCode) {
				$("#account_balance").html("0");
				$("#account_nr_assets").html("0");

				if (NRS.accountInfo.errorCode == 5) {
					$("#dashboard_message").addClass("alert-success").removeClass("alert-danger").html("Welcome to your brand new account. You should fund it with some coins. Your account ID is: <strong>" + NRS.account + "</strong>").show();
				} else {
					$("#dashboard_message").addClass("alert-danger").removeClass("alert-success").html(NRS.accountInfo.errorDescription ? NRS.accountInfo.errorDescription.escapeHTML() : "An unknown error occured.").show();
				}
			} else {
				if (!NRS.accountInfo.publicKey) {
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

							if (!NRS.accountInfo.assetBalances) {
								NRS.accountInfo.assetBalances = [];
							}

							var current_balances = JSON.stringify(NRS.accountInfo.assetBalances);

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
								NRS.checkAssetDifferences(NRS.accountInfo.assetBalances, previous_balances);
							}
						} else {
							NRS.database.insert("data", {
								id: "asset_balances_" + NRS.account,
								contents: JSON.stringify(NRS.accountInfo.assetBalances)
							});
						}
					});
				} else if (previousAccountInfo && previousAccountInfo.assetBalances) {
					var previousBalances = JSON.stringify(previousAccountInfo.assetBalances);
					var currentBalances = JSON.stringify(NRS.accountInfo.assetBalances);

					if (previousBalances != currentBalances) {
						NRS.checkAssetDifferences(NRS.accountInfo.assetBalances, previousAccountInfo.assetBalances);
					}
				}

				var balance = NRS.formatAmount(new BigInteger(response.unconfirmedBalanceNQT));
				balance = balance.split(".");
				if (balance.length == 2) {
					balance = balance[0] + "<span style='font-size:12px'>." + balance[1] + "</span>";
				} else {
					balance = balance[0];
				}

				$("#account_balance").html(balance);

				var nr_assets = 0;

				if (response.assetBalances) {
					for (var i = 0; i < response.assetBalances.length; i++) {
						if (response.assetBalances[i].balanceNQT != "0") {
							nr_assets++;
						}
					}
				}

				$("#account_nr_assets").html(nr_assets);

				if (NRS.lastBlockHeight) {
					var isLeased = NRS.lastBlockHeight >= NRS.accountInfo.currentLeasingHeightFrom;
					if (isLeased != NRS.IsLeased) {
						var leasingChange = true;
						NRS.isLeased = isLeased;
					}
				} else {
					var leasingChange = false;
				}

				if (leasingChange ||
					(response.currentLeasingHeightFrom != previousAccountInfo.currentLeasingHeightFrom) ||
					(response.lessors && !previousAccountInfo.lessors) ||
					(!response.lessors && previousAccountInfo.lessors) ||
					(response.lessors && previousAccountInfo.lessors && response.lessors.sort().toString() != previousAccountInfo.lessors.sort().toString())) {
					NRS.updateAccountLeasingStatus();
				}

				if (response.name) {
					$("#account_name").html(response.name.escapeHTML());
				}
			}

			if (firstRun) {
				$("#account_balance, #account_nr_assets").removeClass("loading_dots");
			}

			if (callback) {
				callback();
			}
		});
	}

	NRS.updateAccountLeasingStatus = function() {
		var accountLeasingLabel = "";
		var accountLeasingStatus = "";

		if (NRS.lastBlockHeight >= NRS.accountInfo.currentLeasingHeightFrom) {
			accountLeasingLabel = "Leased Out";
			accountLeasingStatus = "Your account effective balance is leased out starting from block " + String(NRS.accountInfo.currentLeasingHeightFrom).escapeHTML() + " until block " + String(NRS.accountInfo.currentLeasingHeightTo).escapeHTML() + " to account <a href='#' data-user='" + String(NRS.accountInfo.currentLessee).escapeHTML() + "' class='user_info'>" + String(NRS.accountInfo.currentLessee).escapeHTML() + "</a>";
			$("#lease_balance_message").html("<strong>Remember</strong>: This lease will take effect after the current lease has ended.");

		} else if (NRS.lastBlockHeight < NRS.accountInfo.currentLeasingHeightTo) {
			accountLeasingLabel = "Leased Soon";
			accountLeasingStatus = "Your account effective balance will be leased out starting from block " + String(NRS.accountInfo.currentLeasingHeightFrom).escapeHTML() + " until block " + String(NRS.accountInfo.currentLeasingHeightTo).escapeHTML() + " to account <a href='#' data-user='" + String(NRS.accountInfo.currentLessee).escapeHTML() + "' class='user_info'>" + String(NRS.accountInfo.currentLessee).escapeHTML() + "</a>";
			$("#lease_balance_message").html("<strong>Remember</strong>: This lease will take effect after the current lease has ended.");
		} else {
			accountLeasingStatus = "Your account effective balance is not leased out.";
			$("#lease_balance_message").html("<strong>Remember</strong>: Once submitted the lease cannot be cancelled.");
		}

		if (NRS.accountInfo.effectiveBalanceNXT == 0) {
			$("#forging_indicator").removeClass("forging");
			$("#forging_indicator span").html("Not Forging");
			$("#forging_indicator").show();
			NRS.isForging = false;
		}

		if (NRS.accountInfo.lessors) {
			if (accountLeasingLabel) {
				accountLeasingLabel += ", ";
				accountLeasingStatus += "<br /><br />";
			}
			accountLeasingLabel += NRS.accountInfo.lessors.length + (NRS.accountInfo.lessors.length == 1 ? " lessor" : "lessors");
			accountLeasingStatus += NRS.accountInfo.lessors.length + " " + (NRS.accountInfo.lessors.length == 1 ? "lessor has" : "lessors have") + " leased their effective balance to your account.";

			var rows = "";

			for (var i = 0; i < NRS.accountInfo.lessors.length; i++) {
				var lessor = NRS.accountInfo.lessors[i];

				rows += "<tr><td><a href='#' data-user='" + String(lessor).escapeHTML() + "'>" + NRS.getAccountTitle(lessor) + "</a></td></tr>";
			}

			$("#account_lessor_table tbody").empty().append(rows);
			$("#account_lessor_container").show();
		} else {
			$("#account_lessor_table tbody").empty();
			$("#account_lessor_container").hide();
		}

		if (accountLeasingLabel) {
			$("#account_leasing").html(accountLeasingLabel).show();
		} else {
			$("#account_leasing").hide();
		}

		if (accountLeasingStatus) {
			$("#account_leasing_status").html(accountLeasingStatus).show();
		} else {
			$("#account_leasing_status").hide();
		}
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
						$.growl("You received <a href='#' data-goto-asset='" + String(asset.id).escapeHTML() + "'>" + NRS.formatAmount(asset.difference) + " " + String(asset.name).escapeHTML() + (asset.difference == 1 ? " asset" : " assets") + "</a>.", {
							"type": "success"
						});
					} else {
						$.growl("You sold <a href='#' data-goto-asset='" + String(asset.id).escapeHTML() + "'>" + NRS.formatAmount(Math.abs(asset.difference)) + " " + String(asset.name).escapeHTML() + (asset.difference == 1 ? " asset" : "assets") + "</a>.", {
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