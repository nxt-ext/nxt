var NRS = (function(NRS, $, undefined) {
	NRS.assets = [];
	NRS.assetIds = [];
	NRS.firstAssetPageLoad = true;
	NRS.closedGroups = [];

	NRS.pages.asset_exchange = function(callback) {
		NRS.pageLoading();

		var $active = $("#asset_exchange_sidebar a.active");

		var activeAsset = false;

		if ($active.length) {
			activeAsset = $active.data("asset");
		}

		$(".content.content-stretch:visible").width($(".page:visible").width());

		NRS.assets = [];
		NRS.assetIds = [];

		if (NRS.databaseSupport) {
			//todo only select a few fields..
			NRS.database.select("assets", null, function(error, assets) {
				$.each(assets, function(index, asset) {
					NRS.assetIds.push(asset.asset);
					NRS.assets.push({
						"id": asset.asset,
						"name": asset.name.toLowerCase(),
						"groupName": asset.groupName.toLowerCase(),
						"quantityQNT": asset.quantityQNT,
						"decimals": asset.decimals
					});
				});

				assets = null;

				NRS.loadAssetExchangeSidebar(callback, activeAsset);
			});
		} else {
			NRS.loadAssetExchangeSidebar(callback, activeAsset);
		}
	}

	NRS.loadAssetExchangeSidebar = function(callback, activeAsset) {
		NRS.sendRequest("getAssetIds+", function(response) {
			if (response.assetIds && response.assetIds.length) {
				if (NRS.currentPage != "asset_exchange") {
					return;
				}

				if (NRS.databaseSupport && NRS.firstAssetPageLoad) {
					for (var i = 0; i < NRS.assetIds.length; i++) {
						if (response.assetIds.indexOf(NRS.assetIds[i]) == -1) {
							//something is wrong, the asset ID provided by the database does not exist in the list of asset IDS returned by the server. 
							//Possible if the user is using a different blockchain. We will clear the DB.
							NRS.assetIds = [];
							NRS.assets = [];

							NRS.database.delete("assets", []);
							break;
						}
					}

					NRS.firstAssetPageLoad = false;
				}

				var nrAssets = 0;
				var newAssets = [];

				for (var i = 0; i < response.assetIds.length; i++) {
					if (NRS.databaseSupport && NRS.assetIds.indexOf(response.assetIds[i]) > -1) {
						//already in database    					
						nrAssets++;

						if (nrAssets == response.assetIds.length) {
							NRS.saveNewAssets(newAssets, function() {
								NRS.assetExchangeSidebarLoaded(callback, activeAsset);
							});
						}
						continue;
					} else {
						//not in database (or no database support), fetch
						NRS.sendRequest("getAsset+", {
							"asset": response.assetIds[i],
							"_extra": {
								"position": i
							}
						}, function(asset, input) {
							if (asset.errorCode) {
								nrAssets++;
							} else {
								asset.groupName = "";
								//asset.position  = input["_extra"].position;

								NRS.assets.push({
									"id": asset.asset,
									"name": String(asset.name).toLowerCase(),
									"groupName": "",
									"quantityQNT": asset.quantityQNT,
									"decimals": asset.decimals
								});

								newAssets.push(asset);

								nrAssets++;

								if (nrAssets == response.assetIds.length) {
									NRS.saveNewAssets(newAssets, function() {
										NRS.assetExchangeSidebarLoaded(callback, activeAsset);
									});
								}
							}
						});

						if (NRS.currentPage != "asset_exchange") {
							return;
						}
					}
				}
			} else {
				NRS.pageLoaded();
				$("#asset_exchange_sidebar").empty();
				$("#no_asset_selected, #loading_asset_data").hide();
				$("#no_assets_available").show();
			}
		});
	}

	NRS.saveNewAssets = function(newAssets, callback) {
		if (NRS.databaseSupport && newAssets.length) {
			for (var i = 0; i < newAssets.length; i++) {
				delete newAssets[i].numberOfTrades;
			}

			NRS.database.insert("assets", newAssets, function(error) {
				if (!error && callback) {
					callback();
				}
			});
		} else if (callback) {
			callback();
		}
	}

	NRS.assetExchangeSidebarLoaded = function(callback, activeAsset) {
		var rows = "";

		NRS.assets.sort(function(a, b) {
			if ((!a.groupName && !b.groupName) || (a.groupName == "ignore list" && b.groupName == "ignore list")) {
				if (a.name > b.name) {
					return 1;
				} else if (a.name < b.name) {
					return -1;
				} else {
					return 0;
				}
			} else if (a.groupName == "ignore list") {
				return 1;
			} else if (b.groupName == "ignore list") {
				return -1;
			} else if (!a.groupName) {
				return 1;
			} else if (!b.groupName) {
				return -1;
			} else if (a.groupName > b.groupName) {
				return 1;
			} else if (a.groupName < b.groupName) {
				return -1;
			} else {
				if (a.name > b.name) {
					return 1;
				} else if (a.name < b.name) {
					return -1;
				} else {
					return 0;
				}
			}
		});

		var lastGroup = "";
		var ungrouped = true;
		var isClosedGroup = false;

		for (var i = 0; i < NRS.assets.length; i++) {
			var asset = NRS.assets[i];

			if (asset.groupName.toLowerCase() != lastGroup) {
				var to_check = (asset.groupName ? asset.groupName : "undefined");

				if (NRS.closedGroups.indexOf(to_check) != -1) {
					isClosedGroup = true;
				} else {
					isClosedGroup = false;
				}

				if (asset.groupName) {
					ungrouped = false;

					rows += "<a href='#' class='list-group-item list-group-item-header" + (asset.groupName == "Ignore List" ? " no-context" : "") + "'" + (asset.groupName != "Ignore List" ? " data-context='asset_exchange_sidebar_group_context' " : "data-context=''") + " data-groupname='" + asset.groupName.escapeHTML() + "' data-closed='" + isClosedGroup + "'><h4 class='list-group-item-heading'>" + asset.groupName.toUpperCase().escapeHTML() + " <i class='fa pull-right fa-angle-" + (isClosedGroup ? "right" : "down") + "'></i></h4></a>";
				} else {
					ungrouped = true;
					rows += "<a href='#' class='list-group-item list-group-item-header no-context' data-closed='" + isClosedGroup + "'><h4 class='list-group-item-heading'>UNGROUPED <i class='fa pull-right fa-angle-" + (isClosedGroup ? "right" : "down") + "'></i></h4></a>";
				}
				lastGroup = asset.groupName.toLowerCase();
			}

			rows += "<a href='#' class='list-group-item list-group-item-" + (ungrouped ? "ungrouped" : "grouped") + "' data-cache='" + i + "' data-asset='" + String(asset.id).escapeHTML() + "'" + (!ungrouped ? " data-groupname='" + asset.groupName.escapeHTML() + "'" : "") + (isClosedGroup ? " style='display:none'" : "") + " data-closed='" + isClosedGroup + "'><h4 class='list-group-item-heading'>" + asset.name.escapeHTML() + "</h4><p class='list-group-item-text'>qty: " + NRS.formatQuantity(asset.quantityQNT, asset.decimals) + "</p></a>";
		}

		var currentActiveAsset = $("#asset_exchange_sidebar a.active");

		if (currentActiveAsset.length) {
			currentActiveAsset = currentActiveAsset.data("asset");
		} else {
			currentActiveAsset = false;
		}

		$("#asset_exchange_sidebar").empty().append(rows);

		if (activeAsset !== false) {
			if (activeAsset !== currentActiveAsset) {
				$("#asset_exchange_sidebar a[data-asset=" + currentActiveAsset + "]").addClass("active");
			} else {
				$("#asset_exchange_sidebar a[data-asset=" + activeAsset + "]").addClass("active").trigger("click", [{
					"refresh": true
				}]);
			}
		}

		$("#no_assets_available").hide();

		NRS.pageLoaded(callback);
	}

	NRS.incoming.asset_exchange = function() {
		var $active = $("#asset_exchange_sidebar a.active");

		if ($active.length) {
			$active.trigger("click", [{
				"refresh": true
			}]);
		}
	}

	$("#asset_exchange_sidebar").on("click", "a", function(e, data) {
		e.preventDefault();

		var link = $(this);

		var assetId = $(this).data("asset");

		if (!assetId) {
			if (NRS.databaseSupport) {
				var group = $(this).data("groupname");
				var closed = $(this).data("closed");

				if (!group) {
					var $links = $("#asset_exchange_sidebar a.list-group-item-ungrouped");
				} else {
					var $links = $("#asset_exchange_sidebar a.list-group-item-grouped[data-groupname='" + group.escapeHTML() + "']");
				}

				if (!group) {
					group = "undefined";
				}

				if (closed) {
					var pos = NRS.closedGroups.indexOf(group);
					if (pos >= 0) {
						NRS.closedGroups.splice(pos);
					}
					$(this).data("closed", "");
					$(this).find("i").removeClass("fa-angle-right").addClass("fa-angle-down");
					$links.show();
				} else {
					NRS.closedGroups.push(group);
					$(this).data("closed", true);
					$(this).find("i").removeClass("fa-angle-down").addClass("fa-angle-right");
					$links.hide();
				}

				NRS.database.update("data", {
					"contents": NRS.closedGroups.join("#")
				}, [{
					"id": "closed_groups"
				}]);
			}

			return;
		}

		assetId = assetId.escapeHTML();

		NRS.abortOutstandingRequests(true);

		if (NRS.databaseSupport) {
			NRS.database.select("assets", [{
				"asset": assetId
			}], function(error, asset) {
				if (!error) {
					NRS.loadAsset(asset[0], link, data);
				}
			});
		} else {
			NRS.sendRequest("getAsset", {
				"asset": assetId
			}, function(response, input) {
				if (!response.errorCode) {
					NRS.loadAsset(response, link, data);
				}
			});
		}

	});

	NRS.loadAsset = function(asset, link, data) {
		var assetId = asset.asset;

		NRS.currentAsset = asset;
		NRS.currentSubPage = assetId;

		var asset_account = NRS.getAccountTitle(asset.account);

		var refresh = (data && data.refresh);

		if (!refresh) {
			$("#asset_exchange_sidebar a.active").removeClass("active");
			link.addClass("active");

			$("#no_asset_selected, #loading_asset_data, #no_assets_available").hide();
			$("#asset_details").show().parent().animate({
				"scrollTop": 0
			}, 0);

			$("#asset_account").html("<a href='#' data-user='" + asset_account + "' class='user_info'>" + asset_account + "</a>");
			$("#asset_id").html(assetId.escapeHTML());
			$("#asset_decimals").html(String(asset.decimals).escapeHTML());
			$("#asset_name").html(String(asset.name).escapeHTML());
			$("#asset_description").html(String(asset.description).escapeHTML());
			$("#asset_quantity").html(NRS.formatQuantity(asset.quantityQNT, asset.decimals));

			$(".asset_name").html(String(asset.name).escapeHTML());
			$("#sell_asset_button").data("asset", assetId);
			$("#buy_asset_button").data("asset", assetId);

			$("#sell_asset_price").val("");
			$("#buy_asset_price").val("");
			$("#buy_asset_quantity").val("0");
			$("#buy_asset_total").val("0");
			$("#sell_asset_quantity").val("0");
			$("#sell_asset_total").val("0");

			$("#asset_exchange_ask_orders_table tbody").empty();
			$("#asset_exchange_bid_orders_table tbody").empty();
			$("#asset_exchange_trade_history_table tbody").empty();
			$("#asset_exchange_ask_orders_table").parent().addClass("data-loading").removeClass("data-empty");
			$("#asset_exchange_bid_orders_table").parent().addClass("data-loading").removeClass("data-empty");
			$("#asset_exchange_trade_history_table").parent().addClass("data-loading").removeClass("data-empty");

			$(".data-loading img.loading").hide();

			setTimeout(function() {
				$(".data-loading img.loading").fadeIn(200);
			}, 200);
		}

		var responsesReceived = 0;
		var balance = -1;

		NRS.sendRequest("getAccount+" + assetId, {
			"account": NRS.account
		}, function(response) {
			NRS.accountInfo.unconfirmedBalanceNQT = response.unconfirmedBalanceNQT;

			if (response.unconfirmedBalanceNQT == "0") {
				$("#your_nxt_balance").html("0");
				$("#buy_automatic_price").addClass("zero").removeClass("nonzero");
			} else {
				$("#your_nxt_balance").html(NRS.formatAmount(response.unconfirmedBalanceNQT));
				$("#buy_automatic_price").addClass("nonzero").removeClass("zero");
			}

			if (response.assetBalances) {
				for (var i = 0; i < response.assetBalances.length; i++) {
					var asset = response.assetBalances[i];

					if (asset.asset == assetId) {
						NRS.currentAsset.yourBalanceNQT = asset.balanceQNT;
						$("#your_asset_balance").html(NRS.formatQuantity(asset.balanceQNT, NRS.currentAsset.decimals));
						if (asset.balanceQNT == "0") {
							$("#sell_automatic_price").addClass("zero").removeClass("nonzero");
						} else {
							$("#sell_automatic_price").addClass("nonzero").removeClass("zero");
						}
						break;
					}
				}
			}

			if (!NRS.currentAsset.yourBalanceNQT) {
				NRS.currentAsset.yourBalanceNQT = "0";
				$("#your_asset_balance").html("0");
			}
		});

		NRS.sendRequest("getAskOrderIds+" + assetId, {
			"asset": assetId,
			"timestamp": 0,
			"limit": 50,
			"_extra": {
				"refresh": refresh
			}
		}, function(response, input) {
			var refresh = input["_extra"].refresh;

			if (response.askOrderIds && response.askOrderIds.length) {
				var realNrAskOrders = response.askOrderIds.length;
				var askOrderIds = response.askOrderIds;
				var askOrders = {};
				var nrAskOrders = 0;

				$("#sell_orders_count").html("(" + realNrAskOrders + (realNrAskOrders == 50 ? "+" : "") + ")");

				for (var i = 0; i < askOrderIds.length; i++) {
					NRS.sendRequest("getAskOrder+" + assetId, {
						"order": askOrderIds[i],
						"_extra": {
							"refresh": refresh
						}
					}, function(order, input) {
						askOrders[input.order] = order;

						nrAskOrders++;

						if (nrAskOrders == askOrderIds.length) {
							var rows = "";

							var refresh = input["_extra"].refresh;

							for (var i = 0; i < nrAskOrders; i++) {
								var askOrder = askOrders[askOrderIds[i]];

								askOrder.priceNQT = new BigInteger(askOrder.priceNQT);
								askOrder.quantityQNT = new BigInteger(askOrder.quantityQNT);
								askOrder.totalNQT = new BigInteger(NRS.calculateOrderTotalNQT(askOrder.quantityQNT, askOrder.priceNQT));

								if (i == 0 && !input["_extra"].refresh) {
									$("#buy_asset_price").val(NRS.calculateOrderPricePerWholeQNT(askOrder.priceNQT, NRS.currentAsset.decimals));
								}

								var cancelled = false;

								if (NRS.unconfirmedTransactions.length) {
									for (var j = 0; j < NRS.unconfirmedTransactions.length; j++) {
										var unconfirmedTransaction = NRS.unconfirmedTransactions[j];

										if (unconfirmedTransaction.type == 2 && unconfirmedTransaction.subtype == 4 && unconfirmedTransaction.attachment.order == askOrderIds[i]) {
											cancelled = true;
											break;
										}
									}
								}

								var className = (askOrder.account == NRS.account ? "your-order" : "") + (cancelled ? " tentative tentative-crossed" : "");

								rows += "<tr class='" + className + "' data-quantity='" + askOrder.quantityQNT.toString().escapeHTML() + "' data-price='" + askOrder.priceNQT.toString().escapeHTML() + "'><td>" + (askOrder.account == NRS.account ? "<strong>You</strong>" : "<a href='#' data-user='" + String(askOrder.account).escapeHTML() + "' class='user_info'>" + (askOrder.account == asset_account ? "Asset Issuer" : NRS.getAccountTitle(askOrder.account)) + "</a>") + "</td><td>" + NRS.formatQuantity(askOrder.quantityQNT, NRS.currentAsset.decimals) + "</td><td>" + NRS.formatOrderPricePerWholeQNT(askOrder.priceNQT, NRS.currentAsset.decimals) + "</td><td>" + NRS.formatAmount(askOrder.totalNQT) + "</tr>";
							}

							$("#asset_exchange_ask_orders_table tbody").empty().append(rows);

							if (NRS.unconfirmedTransactions.length) {
								for (var i = 0; i < NRS.unconfirmedTransactions.length; i++) {
									var unconfirmedTransaction = NRS.unconfirmedTransactions[i];

									if (unconfirmedTransaction.type == 2 && unconfirmedTransaction.subtype == 2 && unconfirmedTransaction.attachment.asset == assetId) {
										var $rows = $("#asset_exchange_ask_orders_table tbody").find("tr");

										unconfirmedTransaction.priceNQT = new BigInteger(unconfirmedTransaction.attachment.priceNQT);
										unconfirmedTransaction.quantityQNT = new BigInteger(unconfirmedTransaction.attachment.quantityQNT);
										unconfirmedTransaction.totalNQT = new BigInteger(NRS.calculateOrderTotalNQT(unconfirmedTransaction.quantityQNT, unconfirmedTransaction.priceNQT));

										var rowToAdd = "<tr class='tentative' data-quantity='" + unconfirmedTransaction.quantityQNT.toString().escapeHTML() + "' data-price='" + unconfirmedTransaction.priceNQT.toString().escapeHTML() + "'><td>You - <strong>Pending Order</strong></td><td>" + NRS.formatQuantity(unconfirmedTransaction.quantityQNT, NRS.currentAsset.decimals) + "</td><td>" + NRS.formatOrderPricePerWholeQNT(unconfirmedTransaction.priceNQT, NRS.currentAsset.decimals) + "</td><td>" + NRS.formatAmount(unconfirmedTransaction.totalNQT) + "</td></tr>";

										var rowAdded = false;

										if ($rows.length) {
											$rows.each(function() {
												var rowPrice = new BigInteger(String($(this).data("price")));
												if (unconfirmedTransaction.priceNQT.compareTo(rowPrice) < 0) {
													$(this).before(rowToAdd);
													rowAdded = true;
													return false;
												}
											});
										}

										if (!rowAdded) {
											$("#asset_exchange_ask_orders_table tbody").append(rowToAdd);
										}
									}
								}
							}

							NRS.dataLoadFinished($("#asset_exchange_ask_orders_table"), !refresh);
						}
					});
				}
			} else {
				if (NRS.unconfirmedTransactions.length) {
					var rows = "";

					for (var i = 0; i < NRS.unconfirmedTransactions.length; i++) {
						var unconfirmedTransaction = NRS.unconfirmedTransactions[i];

						unconfirmedTransaction.priceNQT = new BigInteger(unconfirmedTransaction.attachment.priceNQT);
						unconfirmedTransaction.quantityQNT = new BigInteger(unconfirmedTransaction.attachment.quantityQNT);
						unconfirmedTransaction.totalNQT = new BigInteger(NRS.calculateOrderTotalNQT(unconfirmedTransaction.quantityQNT, unconfirmedTransaction.priceNQT));

						if (unconfirmedTransaction.type == 2 && unconfirmedTransaction.subtype == 2 && unconfirmedTransaction.attachment.asset == assetId) {
							rows += "<tr class='tentative' data-quantity='" + unconfirmedTransaction.quantityQNT.toString().escapeHTML() + "' data-price='" + unconfirmedTransaction.priceNQT.toString().escapeHTML() + "'><td>You - <strong>Pending Order</strong></td><td>" + NRS.formatQuantity(unconfirmedTransaction.quantityQNT, NRS.currentAsset.decimals) + "</td><td>" + NRS.formatOrderPricePerWholeQNT(unconfirmedTransaction.priceNQT, NRS.currentAsset.decimals) + "</td><td>" + NRS.formatAmount(unconfirmedTransaction.totalNQT) + "</td></tr>";
						}
					}

					$("#asset_exchange_ask_orders_table tbody").empty().append(rows);
				} else {
					$("#asset_exchange_ask_orders_table tbody").empty();
				}

				NRS.dataLoadFinished($("#asset_exchange_ask_orders_table"), !refresh);
				//refresh
				$("#buy_asset_price").val("0");
				$("#sell_orders_count").html("");
			}
		});

		NRS.sendRequest("getBidOrderIds+" + assetId, {
			"asset": assetId,
			"timestamp": 0,
			"limit": 50,
			"_extra": {
				"refresh": refresh
			}
		}, function(response, input) {
			var refresh = input["_extra"].refresh;

			if (response.bidOrderIds && response.bidOrderIds.length) {
				var realNrBidOrders = response.bidOrderIds.length;
				var bidOrderIds = response.bidOrderIds;
				var bidOrders = {};
				var nrBidOrders = 0;

				$("#buy_orders_count").html("(" + realNrBidOrders + (realNrBidOrders == 50 ? "+" : "") + ")");

				for (var i = 0; i < bidOrderIds.length; i++) {
					NRS.sendRequest("getBidOrder+" + assetId, {
						"order": bidOrderIds[i],
						"_extra": {
							"refresh": refresh
						}
					}, function(order, input) {
						bidOrders[input.order] = order;
						nrBidOrders++;

						if (nrBidOrders == bidOrderIds.length) {
							var rows = "";

							var refresh = input["_extra"].refresh;

							for (var i = 0; i < nrBidOrders; i++) {
								var bidOrder = bidOrders[bidOrderIds[i]];

								bidOrder.priceNQT = new BigInteger(bidOrder.priceNQT);
								bidOrder.quantityQNT = new BigInteger(bidOrder.quantityQNT);
								bidOrder.totalNQT = new BigInteger(NRS.calculateOrderTotalNQT(bidOrder.quantityQNT, bidOrder.priceNQT));

								if (i == 0 && !refresh) {
									$("#sell_asset_price").val(NRS.calculateOrderPricePerWholeQNT(bidOrder.priceNQT, NRS.currentAsset.decimals));
								}

								var cancelled = false;

								if (NRS.unconfirmedTransactions.length) {
									for (var j = 0; j < NRS.unconfirmedTransactions.length; j++) {
										var unconfirmedTransaction = NRS.unconfirmedTransactions[j];

										if (unconfirmedTransaction.type == 2 && unconfirmedTransaction.subtype == 5 && unconfirmedTransaction.attachment.order == bidOrderIds[i]) {
											cancelled = true;
											break;
										}
									}
								}

								var className = (bidOrder.account == NRS.account ? "your-order" : "") + (cancelled ? " tentative tentative-crossed" : "");

								rows += "<tr class='" + className + "' data-quantity='" + bidOrder.quantityQNT.toString().escapeHTML() + "' data-price='" + bidOrder.priceNQT.toString().escapeHTML() + "'><td>" + (bidOrder.account == NRS.account ? "<strong>You</strong>" : "<a href='#' data-user='" + String(bidOrder.account).escapeHTML() + "' class='user_info'>" + (bidOrder.account == asset_account ? "Asset Issuer" : NRS.getAccountTitle(bidOrder.account)) + "</a>") + "</td><td>" + NRS.formatQuantity(bidOrder.quantityQNT, NRS.currentAsset.decimals) + "</td><td>" + NRS.formatOrderPricePerWholeQNT(bidOrder.priceNQT, NRS.currentAsset.decimals) + "</td><td>" + NRS.formatAmount(bidOrder.totalNQT) + "</tr>";

							}

							$("#asset_exchange_bid_orders_table tbody").empty().append(rows);

							if (NRS.unconfirmedTransactions.length) {
								for (var i = 0; i < NRS.unconfirmedTransactions.length; i++) {
									var unconfirmedTransaction = NRS.unconfirmedTransactions[i];

									if (unconfirmedTransaction.type == 2 && unconfirmedTransaction.subtype == 3 && unconfirmedTransaction.attachment.asset == assetId) {
										var $rows = $("#asset_exchange_bid_orders_table tbody").find("tr");

										unconfirmedTransaction.priceNQT = new BigInteger(unconfirmedTransaction.attachment.priceNQT);
										unconfirmedTransaction.quantityQNT = new BigInteger(unconfirmedTransaction.attachment.quantityQNT);
										unconfirmedTransaction.totalNQT = new BigInteger(NRS.calculateOrderTotalNQT(unconfirmedTransaction.quantityQNT, unconfirmedTransaction.priceNQT));

										var rowToAdd = "<tr class='tentative' data-quantity='" + unconfirmedTransaction.quantityQNT.toString().escapeHTML() + "' data-price='" + unconfirmedTransaction.priceNQT.toString().escapeHTML() + "'><td>You - <strong>Pending Order</strong></td><td>" + NRS.formatQuantity(unconfirmedTransaction.quantityQNT, NRS.currentAsset.decimals) + "</td><td>" + NRS.formatOrderPricePerWholeQNT(unconfirmedTransaction.priceNQT, NRS.currentAsset.decimals) + "</td><td>" + NRS.formatAmount(unconfirmedTransaction.totalNQT) + "</td></tr>";

										var rowAdded = false;

										if ($rows.length) {
											$rows.each(function() {
												var rowPrice = new BigInteger(String($(this).data("price")));
												if (unconfirmedTransaction.priceNQT.compareTo(rowPrice) > 0) {
													$(this).before(rowToAdd);
													rowAdded = true;
													return false;
												}
											});
										}

										if (!rowAdded) {
											$("#asset_exchange_bid_orders_table tbody").append(rowToAdd);
										}
									}
								}
							}

							NRS.dataLoadFinished($("#asset_exchange_bid_orders_table"), !refresh);
						}
					});
				}
			} else {
				if (NRS.unconfirmedTransactions.length) {
					var rows = "";

					for (var i = 0; i < NRS.unconfirmedTransactions.length; i++) {
						var unconfirmedTransaction = NRS.unconfirmedTransactions[i];

						if (unconfirmedTransaction.type == 2 && unconfirmedTransaction.subtype == 3 && unconfirmedTransaction.attachment.asset == assetId) {
							unconfirmedTransaction.priceNQT = new BigInteger(unconfirmedTransaction.attachment.priceNQT);
							unconfirmedTransaction.quantityQNT = new BigInteger(unconfirmedTransaction.attachment.quantityQNT);
							unconfirmedTransaction.totalNQT = new BigInteger(NRS.calculateOrderTotalNQT(unconfirmedTransaction.quantityQNT, unconfirmedTransaction.priceNQT));

							rows += "<tr class='tentative' data-quantity='" + unconfirmedTransaction.quantityQNT.toString().escapeHTML() + "' data-price='" + unconfirmedTransaction.priceNQT.toString().escapeHTML() + "'><td>You - <strong>Pending Order</strong></td><td>" + NRS.formatQuantity(unconfirmedTransaction.quantityQNT, NRS.currentAsset.decimals) + "</td><td>" + NRS.formatOrderPricePerWholeQNT(unconfirmedTransaction.priceNQT, NRS.currentAsset.decimals) + "</td><td>" + NRS.formatAmount(unconfirmedTransaction.totalNQT) + "</td></tr>";
						}
					}

					$("#asset_exchange_bid_orders_table tbody").empty().append(rows);
				} else {
					$("#asset_exchange_bid_orders_table tbody").empty();
				}

				NRS.dataLoadFinished($("#asset_exchange_bid_orders_table"), !refresh);
				//refresh
				$("#sell_asset_price").val("0");
				$("#buy_orders_count").html("");
			}
		});

		NRS.sendRequest("getTrades+" + assetId, {
			"asset": assetId,
			"firstIndex": 0,
			"_extra": {
				"refresh": refresh
			}
		}, function(response, input) {
			if (response.trades && response.trades.length) {
				var trades = response.trades.reverse().slice(0, 50);
				var nrTrades = trades.length;

				var refresh = input["_extra"].refresh;

				var rows = "";

				for (var i = 0; i < nrTrades; i++) {
					trades[i].priceNQT = new BigInteger(trades[i].priceNQT);
					trades[i].quantityQNT = new BigInteger(trades[i].quantityQNT);
					trades[i].totalNQT = new BigInteger(NRS.calculateOrderTotalNQT(trades[i].priceNQT, trades[i].quantityQNT));

					rows += "<tr><td>" + NRS.formatTimestamp(trades[i].timestamp) + "</td><td>" + NRS.formatQuantity(trades[i].quantityQNT, NRS.currentAsset.decimals) + "</td><td class='asset_price'>" + NRS.formatOrderPricePerWholeQNT(trades[i].priceNQT, NRS.currentAsset.decimals) + "</td><td>" + NRS.formatAmount(trades[i].totalNQT) + "</td><td>" + String(trades[i].askOrder).escapeHTML() + "</td><td>" + String(trades[i].bidOrder).escapeHTML() + "</td></tr>";
				}

				$("#asset_exchange_trade_history_table tbody").empty().append(rows);
				NRS.dataLoadFinished($("#asset_exchange_trade_history_table"), !refresh);
			} else {
				$("#asset_exchange_trade_history_table tbody").empty();
				NRS.dataLoadFinished($("#asset_exchange_trade_history_table"), !refresh);
			}
		});
	}

	$("#buy_asset_box .box-header, #sell_asset_box .box-header").click(function(e) {
		e.preventDefault();
		//Find the box parent        
		var box = $(this).parents(".box").first();
		//Find the body and the footer
		var bf = box.find(".box-body, .box-footer");
		if (!box.hasClass("collapsed-box")) {
			box.addClass("collapsed-box");
			bf.slideUp();
		} else {
			box.removeClass("collapsed-box");
			bf.slideDown();
		}
	});

	$("#asset_exchange_bid_orders_table tbody, #asset_exchange_ask_orders_table tbody").on("click", "td", function(e) {
		var $target = $(e.target);

		if ($target.prop("tagName").toLowerCase() == "a") {
			return;
		}

		var type = ($target.closest("table").attr("id") == "asset_exchange_bid_orders_table" ? "sell" : "buy");

		var $tr = $target.closest("tr");

		try {
			var priceNQT = new BigInteger(String($tr.data("price")));
			var quantityQNT = new BigInteger(String($tr.data("quantity")));
			var totalNQT = new BigInteger(NRS.calculateOrderTotalNQT(quantityQNT, priceNQT));

			$("#" + type + "_asset_price").val(NRS.calculateOrderPricePerWholeQNT(priceNQT, NRS.currentAsset.decimals));
			$("#" + type + "_asset_quantity").val(NRS.convertToQNTf(quantityQNT, NRS.currentAsset.decimals));
			$("#" + type + "_asset_total").val(NRS.convertToNXT(totalNQT));
		} catch (err) {
			return;
		}

		if (type == "sell") {
			try {
				var balanceNQT = new BigInteger(NRS.accountInfo.unconfirmedBalanceNQT);
			} catch (err) {
				return;
			}

			if (totalNQT.compareTo(balanceNQT) > 0) {
				$("#" + type + "_asset_total").css({
					"background": "#ED4348",
					"color": "white"
				});
			} else {
				$("#" + type + "_asset_total").css({
					"background": "",
					"color": ""
				});
			}
		}

		var box = $("#" + type + "_asset_box");

		if (box.hasClass("collapsed-box")) {
			box.removeClass("collapsed-box");
			box.find(".box-body").slideDown();
		}
	});

	$("#sell_automatic_price, #buy_automatic_price").on("click", function(e) {
		try {
			var type = ($(this).attr("id") == "sell_automatic_price" ? "sell" : "buy");

			var price = new Big(NRS.convertToNQT(String($("#" + type + "_asset_price").val())));
			var balance = new Big(type == "buy" ? NRS.accountInfo.unconfirmedBalanceNQT : NRS.currentAsset.yourBalanceNQT);
			var balanceNQT = new Big(NRS.accountInfo.unconfirmedBalanceNQT);
			var maxQuantity = new Big(NRS.convertToQNTf(NRS.currentAsset.quantityQNT, NRS.currentAsset.decimals));

			if (balance.cmp(new Big("0")) <= 0) {
				return;
			}

			if (price.cmp(new Big("0")) <= 0) {
				//get minimum price if no offers exist, based on asset decimals..
				price = new Big("" + Math.pow(10, NRS.currentAsset.decimals));
				$("#" + type + "_asset_price").val(NRS.convertToNXT(price.toString()));
			}

			var quantity = new Big(NRS.amountToPrecision((type == "sell" ? balanceNQT : balance).div(price).toString(), NRS.currentAsset.decimals));

			var total = quantity.times(price);

			//proposed quantity is bigger than available quantity
			if (quantity.cmp(maxQuantity) == 1) {
				quantity = maxQuantity;
				total = quantity.times(price);
			}

			$("#" + type + "_asset_quantity").val(quantity.toString());
			$("#" + type + "_asset_total").val(NRS.convertToNXT(total.toString()));

			$("#" + type + "_asset_total").css({
				"background": "",
				"color": ""
			});
		} catch (err) {}
	});

	function isControlKey(charCode) {
		if (charCode >= 32)
			return false;
		if (charCode == 10)
			return false;
		if (charCode == 13)
			return false;

		return true;
	}

	$("#buy_asset_quantity, #buy_asset_price, #sell_asset_quantity, #sell_asset_price, #buy_asset_fee, #sell_asset_fee").keydown(function(e) {
		var charCode = !e.charCode ? e.which : e.charCode;

		if (isControlKey(charCode) || e.ctrlKey || e.metaKey) {
			return;
		}

		var isQuantityField = /_quantity/i.test($(this).attr("id"));

		var maxFractionLength = (isQuantityField ? NRS.currentAsset.decimals : 8 - NRS.currentAsset.decimals);

		if (maxFractionLength) {
			//allow 1 single period character
			if (charCode == 190) {
				if ($(this).val().indexOf(".") != -1) {
					e.preventDefault();
					return false;
				} else {
					return;
				}
			}
		} else {
			//do not allow period
			if (charCode == 190 || charCode == 188) {
				$.growl("Fractions are not allowed.", {
					"type": "danger"
				});
				e.preventDefault();
				return false;
			}
		}

		var input = $(this).val() + String.fromCharCode(charCode);

		var afterComma = input.match(/\.(\d*)$/);

		//only allow as many as there are decimals allowed..
		if (afterComma && afterComma[1].length > maxFractionLength) {
			var selectedText = NRS.getSelectedText();

			if (selectedText != $(this).val()) {
				if (isQuantityField) {
					errorMessage = "Only " + NRS.currentAsset.decimals + " digits after the decimal mark are allowed for this asset.";
				} else {
					errorMessage = "Only " + (8 - NRS.currentAsset.decimals) + " digits after the decimal mark are allowed.";
				}

				$.growl(errorMessage, {
					"type": "danger"
				});

				e.preventDefault();
				return false;
			}
		}

		//numeric characters, left/right key, backspace, delete
		if (charCode == 8 || charCode == 37 || charCode == 39 || charCode == 46 || (charCode >= 48 && charCode <= 57 && !isNaN(String.fromCharCode(charCode))) || (charCode >= 96 && charCode <= 105)) {
			return;
		} else {
			//comma
			if (charCode == 188) {
				$.growl("Comma is not allowed, use a dot instead.", {
					"type": "danger"
				});
			}
			e.preventDefault();
			return false;
		}
	});

	//calculate preview price (calculated on every keypress)
	$("#sell_asset_quantity, #sell_asset_price, #buy_asset_quantity, #buy_asset_price").keyup(function(e) {
		var orderType = $(this).data("type").toLowerCase();

		try {
			var quantityQNT = new BigInteger(NRS.convertToQNT(String($("#" + orderType + "_asset_quantity").val()), NRS.currentAsset.decimals));
			var priceNQT = new BigInteger(NRS.calculatePricePerWholeQNT(NRS.convertToNQT(String($("#" + orderType + "_asset_price").val())), NRS.currentAsset.decimals));

			if (priceNQT.toString() == "0" || quantityQNT.toString() == "0") {
				$("#" + orderType + "_asset_total").val("0");
			} else {
				var total = NRS.calculateOrderTotal(quantityQNT, priceNQT, NRS.currentAsset.decimals);
				$("#" + orderType + "_asset_total").val(total.toString());
			}
		} catch (err) {
			$("#" + orderType + "_asset_total").val("0");
		}
	});

	$("#asset_order_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var orderType = $invoker.data("type");
		var assetId = $invoker.data("asset");

		$("#asset_order_modal_button").html(orderType + " Asset").data("resetText", orderType + " Asset");

		orderType = orderType.toLowerCase();

		try {
			//TODO
			var quantity = String($("#" + orderType + "_asset_quantity").val());
			var quantityQNT = new BigInteger(NRS.convertToQNT(quantity, NRS.currentAsset.decimals));
			var priceNQT = new BigInteger(NRS.calculatePricePerWholeQNT(NRS.convertToNQT(String($("#" + orderType + "_asset_price").val())), NRS.currentAsset.decimals));
			var feeNQT = new BigInteger(NRS.convertToNQT(String($("#" + orderType + "_asset_fee").val())));
			var totalNXT = NRS.formatAmount(NRS.calculateOrderTotalNQT(quantityQNT, priceNQT, NRS.currentAsset.decimals), false, true);
		} catch (err) {
			$.growl("Invalid input.", {
				"type": "danger"
			});
			return e.preventDefault();
		}

		if (priceNQT.toString() == "0" || quantityQNT.toString() == "0") {
			$.growl("Please fill in an amount and price.", {
				"type": "danger"
			});
			return e.preventDefault();
		}

		if (feeNQT.toString() == "0") {
			feeNQT = new BigInteger("100000000");
		}

		var priceNQTPerWholeQNT = priceNQT.multiply(new BigInteger("" + Math.pow(10, NRS.currentAsset.decimals)));

		if (orderType == "buy") {
			var description = "Buy <strong>" + NRS.formatQuantity(quantityQNT, NRS.currentAsset.decimals, true) + " " + $("#asset_name").html() + "</strong> assets at <strong>" + NRS.formatAmount(priceNQTPerWholeQNT, false, true) + " NXT</strong> each.";
			var tooltipTitle = "Per whole asset bought you will pay " + NRS.formatAmount(priceNQTPerWholeQNT, false, true) + " NXT, making a total of " + totalNXT + " NXT once everything have been bought.";
		} else {
			var description = "Sell <strong>" + NRS.formatQuantity(quantityQNT, NRS.currentAsset.decimals, true) + " " + $("#asset_name").html() + "</strong> assets at <strong>" + NRS.formatAmount(priceNQTPerWholeQNT, false, true) + " NXT</strong> each.";
			var tooltipTitle = "Per whole asset sold you will receive " + NRS.formatAmount(priceNQTPerWholeQNT, false, true) + " NXT, making a total of " + totalNXT + " NXT once everything has been sold.";
		}

		$("#asset_order_description").html(description);
		$("#asset_order_total").html(totalNXT + " NXT");
		$("#asset_order_fee_paid").html(NRS.formatAmount(feeNQT) + " NXT");

		if (quantity != "1") {
			$("#asset_order_total_tooltip").show();
			$("#asset_order_total_tooltip").popover("destroy");
			$("#asset_order_total_tooltip").data("content", tooltipTitle);
			$("#asset_order_total_tooltip").popover({
				"content": tooltipTitle,
				"trigger": "hover"
			});
		} else {
			$("#asset_order_total_tooltip").hide();
		}

		$("#asset_order_type").val((orderType == "buy" ? "placeBidOrder" : "placeAskOrder"));
		$("#asset_order_asset").val(assetId);
		$("#asset_order_quantity").val(quantityQNT.toString());
		$("#asset_order_price").val(priceNQT.toString());
		$("#asset_order_fee").val(feeNQT.toString());
	});

	NRS.forms.orderAsset = function($modal) {
		var orderType = $("#asset_order_type").val();

		return {
			"requestType": orderType,
			"successMessage": $modal.find("input[name=success_message]").val().replace("__", (orderType == "placeBidOrder" ? "buy" : "sell"))
		};
	}

	NRS.forms.orderAssetComplete = function(response, data) {
		NRS.addUnconfirmedTransaction(response.transaction);

		if (data.requestType == "placeBidOrder") {
			var $table = $("#asset_exchange_bid_orders_table tbody");
		} else {
			var $table = $("#asset_exchange_ask_orders_table tbody");
		}

		var $rows = $table.find("tr");

		data.quantityQNT = new BigInteger(data.quantityQNT);
		data.priceNQT = new BigInteger(data.priceNQT);
		data.totalNQT = new BigInteger(NRS.calculateOrderTotalNQT(data.quantityQNT, data.priceNQT));

		var rowToAdd = "<tr class='tentative' data-transaction='" + String(response.transaction).escapeHTML() + "' data-quantity='" + data.quantityQNT.toString().escapeHTML() + "' data-price='" + data.priceNQT.toString().escapeHTML() + "'><td>You - <strong>Pending Order</strong></td><td>" + NRS.formatQuantity(data.quantityQNT, NRS.currentAsset.decimals) + "</td><td>" + NRS.formatOrderPricePerWholeQNT(data.priceNQT, NRS.currentAsset.decimals) + "</td><td>" + NRS.formatAmount(data.totalNQT) + "</td></tr>";

		var rowAdded = false;

		//update highest bid / lowest ask
		if ($rows.length) {
			$rows.each(function() {
				var rowPrice = new BigInteger(String($(this).data("price")));

				if (data.requestType == "placeBidOrder" && data.priceNQT.compareTo(rowPrice) > 0) {
					$(this).before(rowToAdd);
					rowAdded = true;
					return false;
				} else if (data.requestType == "placeAskOrder" && data.priceNQT.compareTo(rowPrice) < 0) {
					$(this).before(rowToAdd);
					rowAdded = true;
					return false;
				}
			});
		}

		if (!rowAdded) {
			//if (data.requestType == "placeBidOrder") {
			$table.append(rowToAdd);
			$table.parent().parent().removeClass("data-empty").parent().addClass("no-padding");
		}
	}

	NRS.forms.issueAsset = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		data.description = $.trim(data.description);

		if (!data.description) {
			return {
				"error": "Description is a required field."
			};
		} else if (!/^\d+$/.test(data.quantity)) {
			return {
				"error": "Quantity must be a whole numbrer."
			};
		} else {
			data.quantityQNT = String(data.quantity);

			if (data.decimals > 0) {
				for (var i = 0; i < data.decimals; i++) {
					data.quantityQNT += "0";
				}
			}

			delete data.quantity;

			return {
				"data": data
			};
		}
	}

	$("#asset_exchange_sidebar_group_context").on("click", "a", function(e) {
		e.preventDefault();

		var groupName = NRS.selectedContext.data("groupname");
		var option = $(this).data("option");

		if (option == "change_group_name") {
			$("#asset_exchange_change_group_name_old_display").html(groupName.escapeHTML());
			$("#asset_exchange_change_group_name_old").val(groupName);
			$("#asset_exchange_change_group_name_new").val("");
			$("#asset_exchange_change_group_name_modal").modal("show");
		}
	});

	NRS.forms.assetExchangeChangeGroupName = function($modal) {
		var oldGroupName = $("#asset_exchange_change_group_name_old").val();
		var newGroupName = $("#asset_exchange_change_group_name_new").val();

		if (!newGroupName.match(/^[a-z0-9 ]+$/i)) {
			return {
				"error": "Only alphanumerical characters can be used in the group name."
			};
		}

		NRS.database.update("assets", {
			"groupName": newGroupName
		}, [{
			"groupName": oldGroupName
		}], function() {
			NRS.pages.asset_exchange();
		});

		$.growl("Group name updated successfully.", {
			"type": "success"
		});

		return {
			"stop": true
		};
	}

	$("#asset_exchange_sidebar_context").on("click", "a", function(e) {
		e.preventDefault();

		var assetId = NRS.selectedContext.data("asset");
		var option = $(this).data("option");

		NRS.closeContextMenu();

		if (option == "add_to_group") {
			$("#asset_exchange_group_asset").val(assetId);

			NRS.database.select("assets", [{
				"asset": assetId
			}], function(error, asset) {
				asset = asset[0];

				$("#asset_exchange_group_title").html(String(asset.name).escapeHTML());

				NRS.database.select("assets", [], function(error, assets) {

					//NRS.database.execute("SELECT DISTINCT groupName FROM assets", [], function(groupNames) {					
					var groupNames = [];

					$.each(assets, function(index, asset) {
						if (asset.groupName && $.inArray(asset.groupName, groupNames) == -1) {
							groupNames.push(asset.groupName);
						}
					});

					assets = [];

					groupNames.sort(function(a, b) {
						if (a.toLowerCase() > b.toLowerCase()) {
							return 1;
						} else if (a.toLowerCase() < b.toLowerCase()) {
							return -1;
						} else {
							return 0;
						}
					});

					var groupSelect = $("#asset_exchange_group_group");

					groupSelect.empty();

					$.each(groupNames, function(index, groupName) {
						groupSelect.append("<option value='" + groupName.escapeHTML() + "'" + (asset.groupName && asset.groupName.toLowerCase() == groupName.toLowerCase() ? " selected='selected'" : "") + ">" + groupName.escapeHTML() + "</option>");
					});

					groupSelect.append("<option value='0'" + (!asset.groupName ? " selected='selected'" : "") + ">None</option>");
					groupSelect.append("<option value='-1'>New group</option>");

					$("#asset_exchange_group_modal").modal("show");
				});
			});
		} else if (option == "add_to_ignore_list") {
			NRS.database.update("assets", {
				"groupName": "Ignore List"
			}, [{
				"asset": assetId
			}], function() {
				NRS.pages.asset_exchange();
				$.growl("Asset added to ignore list successfully.", {
					"type": "success"
				});
			});
		} else if (option == "remove_from_group") {
			NRS.database.update("assets", {
				"groupName": ""
			}, [{
				"asset": assetId
			}], function() {
				NRS.pages.asset_exchange();
				$.growl("Asset removed from group successfully.", {
					"type": "success"
				});
			});
		}
	});

	$("#asset_exchange_group_group").on("change", function() {
		var value = $(this).val();

		if (value == -1) {
			$("#asset_exchange_group_new_group_div").show();
		} else {
			$("#asset_exchange_group_new_group_div").hide();
		}
	});

	NRS.forms.assetExchangeGroup = function($modal) {
		var assetId = $("#asset_exchange_group_asset").val();
		var groupName = $("#asset_exchange_group_group").val();

		if (groupName == 0) {
			groupName = "";
		} else if (groupName == -1) {
			groupName = $("#asset_exchange_group_new_group").val();
		}

		NRS.database.update("assets", {
			"groupName": groupName
		}, [{
			"asset": assetId
		}], function() {
			NRS.pages.asset_exchange();
			if (!groupName) {
				$.growl("Asset removed from group successfully.", {
					"type": "success"
				});
			} else {
				$.growl("Asset added to group successfully.", {
					"type": "success"
				});
			}
		});

		return {
			"stop": true
		};
	}

	$("#asset_exchange_group_modal").on("hidden.bs.modal", function(e) {
		$("#asset_exchange_group_new_group_div").val("").hide();
	});

	/* MY ASSETS PAGE */
	NRS.pages.my_assets = function() {
		NRS.pageLoading();

		NRS.sendRequest("getAccount+", {
			"account": NRS.account
		}, function(response) {
			if (response.assetBalances && response.assetBalances.length) {
				var result = {
					"assets": [],
					"bid_orders": {},
					"ask_orders": {}
				};
				var count = {
					"total_assets": response.assetBalances.length,
					"assets": 0,
					"ignored_assets": 0,
					"ask_orders": 0,
					"bid_orders": 0
				};

				for (var i = 0; i < response.assetBalances.length; i++) {
					if (response.assetBalances[i].balance == 0) {
						count.ignored_assets++;
						if (NRS.checkMyAssetsPageLoaded(count)) {
							NRS.myAssetsPageLoaded(result);
						}
						continue;
					}

					NRS.sendRequest("getAskOrderIds+", {
						"asset": response.assetBalances[i].asset,
						"limit": 1,
						"timestamp": 0
					}, function(response, input) {
						if (NRS.currentPage != "my_assets") {
							return;
						}

						if (response.askOrderIds && response.askOrderIds.length) {
							NRS.sendRequest("getAskOrder+", {
								"order": response.askOrderIds[0],
								"_extra": {
									"asset": input.asset
								}
							}, function(response, input) {
								if (NRS.currentPage != "my_assets") {
									return;
								}

								response.priceNQT = new BigInteger(response.priceNQT);

								result.ask_orders[input["_extra"].asset] = response.priceNQT;
								count.ask_orders++;
								if (NRS.checkMyAssetsPageLoaded(count)) {
									NRS.myAssetsPageLoaded(result);
								}
							});
						} else {
							result.ask_orders[input.asset] = -1;
							count.ask_orders++;
							if (NRS.checkMyAssetsPageLoaded(count)) {
								NRS.myAssetsPageLoaded(result);
							}
						}
					});

					NRS.sendRequest("getBidOrderIds+", {
						"asset": response.assetBalances[i].asset,
						"limit": 1,
						"timestamp": 0
					}, function(response, input) {
						if (NRS.currentPage != "my_assets") {
							return;
						}

						if (response.bidOrderIds && response.bidOrderIds.length) {
							NRS.sendRequest("getBidOrder+", {
								"order": response.bidOrderIds[0],
								"_extra": {
									"asset": input.asset
								}
							}, function(response, input) {
								if (NRS.currentPage != "my_assets") {
									return;
								}

								response.priceNQT = new BigInteger(response.priceNQT);

								result.bid_orders[input["_extra"].asset] = response.priceNQT;
								count.bid_orders++;
								if (NRS.checkMyAssetsPageLoaded(count)) {
									NRS.myAssetsPageLoaded(result);
								}
							});
						} else {
							result.bid_orders[input.asset] = -1;
							count.bid_orders++;
							if (NRS.checkMyAssetsPageLoaded(count)) {
								NRS.myAssetsPageLoaded(result);
							}
						}
					});

					NRS.sendRequest("getAsset+", {
						"asset": response.assetBalances[i].asset,
						"_extra": {
							"balanceQNT": response.assetBalances[i].balanceQNT
						}
					}, function(asset, input) {
						if (NRS.currentPage != "my_assets") {
							return;
						}

						asset.asset = input.asset;
						asset.balanceQNT = new BigInteger(input["_extra"].balanceQNT);
						asset.quantityQNT = new BigInteger(asset.quantityQNT);

						result.assets[count.assets] = asset;
						count.assets++;

						if (NRS.checkMyAssetsPageLoaded(count)) {
							NRS.myAssetsPageLoaded(result);
						}
					});

					if (NRS.currentPage != "my_assets") {
						return;
					}
				}
			} else {
				$("#my_assets_table tbody").empty();
				NRS.dataLoadFinished($("#my_assets_table"));
			}
		});
	}

	NRS.checkMyAssetsPageLoaded = function(count) {
		if ((count.assets + count.ignored_assets == count.total_assets) && (count.assets == count.ask_orders) && (count.assets == count.bid_orders)) {
			return true;
		} else {
			return false;
		}
	}

	NRS.myAssetsPageLoaded = function(result) {
		var rows = "";

		result.assets.sort(function(a, b) {
			if (a.name.toLowerCase() > b.name.toLowerCase()) {
				return 1;
			} else if (a.name.toLowerCase() < b.name.toLowerCase()) {
				return -1;
			} else {
				return 0;
			}
		});

		for (var i = 0; i < result.assets.length; i++) {
			var asset = result.assets[i];

			var lowestAskOrder = result.ask_orders[asset.asset];
			var highestBidOrder = result.bid_orders[asset.asset];

			var percentageAsset = NRS.calculatePercentage(asset.balanceQNT, asset.quantityQNT);

			if (highestBidOrder != -1) {
				var total = new BigInteger(NRS.calculateOrderTotalNQT(asset.balanceQNT, highestBidOrder, asset.decimals));
			} else {
				var total = 0;
			}

			var tentative = -1;

			if (NRS.unconfirmedTransactions.length) {
				for (var j = 0; j < NRS.unconfirmedTransactions.length; j++) {
					var unconfirmedTransaction = NRS.unconfirmedTransactions[j];

					if (unconfirmedTransaction.type == 2 && unconfirmedTransaction.subtype == 1 && unconfirmedTransaction.attachment.asset == asset.asset) {
						if (tentative == -1) {
							tentative = new BigInteger(unconfirmedTransaction.attachment.quantityQNT);
						} else {
							tentative = tentative.add(new BigInteger(unconfirmedTransaction.attachment.quantityQNT));
						}
					}
				}
			}

			rows += "<tr" + (tentative != -1 ? " class='tentative tentative-allow-links'" : "") + " data-asset='" + String(asset.asset).escapeHTML() + "'><td><a href='#' data-goto-asset='" + String(asset.asset).escapeHTML() + "'>" + String(asset.name).escapeHTML() + "</a></td><td class='quantity'>" + NRS.formatQuantity(asset.balanceQNT, asset.decimals) + (tentative != -1 ? " - <span class='added_quantity'>" + NRS.formatQuantity(tentative, asset.decimals) + "</span>" : "") + "</td><td>" + NRS.formatQuantity(asset.quantityQNT, asset.decimals) + "</td><td>" + percentageAsset + "%</td><td>" + (lowestAskOrder != -1 ? NRS.formatAmount(lowestAskOrder) : "/") + "</td><td>" + (highestBidOrder != -1 ? NRS.formatAmount(highestBidOrder) : "/") + "</td><td>" + (highestBidOrder != -1 ? NRS.formatAmount(total) : "/") + "</td><td><a href='#' data-toggle='modal' data-target='#transfer_asset_modal' data-asset='" + String(asset.asset).escapeHTML() + "' data-name='" + String(asset.name).escapeHTML() + "' data-decimals='" + String(asset.decimals).escapeHTML() + "'>Transfer</a></td></tr>";
		}

		$("#my_assets_table tbody").empty().append(rows);
		NRS.dataLoadFinished($("#my_assets_table"));

		NRS.pageLoaded();
	}

	NRS.incoming.my_assets = function() {
		NRS.pages.my_assets();
	}

	$("#transfer_asset_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var assetId = $invoker.data("asset");
		var assetName = $invoker.data("name");
		var decimals = $invoker.data("decimals");

		$("#transfer_asset_asset").val(assetId);
		$("#transfer_asset_decimals").val(decimals);
		$("#transfer_asset_name").html(String(assetName).escapeHTML());
	});

	NRS.forms.transferAsset = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		try {
			data.quantityQNT = NRS.convertToQNT(data.quantity, data.decimals);
		} catch (e) {
			return {
				"error": "Incorrect quantity: " + e
			};
		}

		delete data.quantity;
		delete data.decimals;

		return {
			"data": data
		};
	}

	NRS.forms.transferAssetComplete = function(response, data) {
		NRS.addUnconfirmedTransaction(response.transaction);
		NRS.pages.my_assets();
	}

	$("body").on("click", "a[data-goto-asset]", function(e) {
		e.preventDefault();

		var $visible_modal = $(".modal.in");

		if ($visible_modal.length) {
			$visible_modal.modal("hide");
		}

		NRS.goToAsset($(this).data("goto-asset"));
	});

	NRS.goToAsset = function(asset) {
		$("#asset_exchange_sidebar a.list-group-item.active").removeClass("active");
		$("#no_asset_selected, #asset_details, #no_assets_available").hide();
		$("#loading_asset_data").show();

		$("ul.sidebar-menu a[data-page=asset_exchange]").last().trigger("click", [{
			callback: function() {
				var assetLink = $("#asset_exchange_sidebar a[data-asset=" + asset + "]");

				if (assetLink.length) {
					assetLink.click();
				} else {
					$("#loading_asset_data, #no_assets_available").hide();
					$("#no_asset_selected").show();
				}
			}
		}]);
	}

	/* OPEN ORDERS PAGE */
	NRS.pages.open_orders = function() {
		var loaded = 0;

		NRS.pageLoading();

		NRS.getOpenOrders("ask", function() {
			loaded++;
			if (loaded == 2) {
				NRS.pageLoaded();
			}
		});

		NRS.getOpenOrders("bid", function() {
			loaded++;
			if (loaded == 2) {
				NRS.pageLoaded();
			}
		});
	}

	NRS.getOpenOrders = function(type, callback) {
		var uppercase = type.charAt(0).toUpperCase() + type.slice(1).toLowerCase();
		var lowercase = type.toLowerCase();

		var getCurrentOrderIds = "getAccountCurrent" + uppercase + "OrderIds+";
		var orderIds = lowercase + "OrderIds";
		var getOrder = "get" + uppercase + "Order+";

		var orders = [];

		NRS.sendRequest(getCurrentOrderIds, {
			"account": NRS.account,
			"timestamp": 0
		}, function(response) {
			if (response[orderIds] && response[orderIds].length) {
				var nr_orders = 0;

				for (var i = 0; i < response[orderIds].length; i++) {
					NRS.sendRequest(getOrder, {
						"order": response[orderIds][i]
					}, function(order, input) {
						if (NRS.currentPage != "open_orders") {
							return;
						}

						order.order = input.order;
						orders.push(order);

						nr_orders++;

						if (nr_orders == response[orderIds].length) {
							var nr_orders_complete = 0;

							for (var i = 0; i < nr_orders; i++) {
								var order = orders[i];

								NRS.sendRequest("getAsset+", {
									"asset": order.asset,
									"_extra": {
										"id": i
									}
								}, function(asset, input) {
									if (NRS.currentPage != "open_orders") {
										return;
									}

									orders[input["_extra"].id].assetName = asset.name;
									orders[input["_extra"].id].decimals = asset.decimals;

									nr_orders_complete++;

									if (nr_orders_complete == nr_orders) {
										NRS.getUnconfirmedOrders(type, function(unconfirmedOrders) {
											NRS.openOrdersLoaded(orders.concat(unconfirmedOrders), lowercase, callback);
										});
									}
								});

								if (NRS.currentPage != "open_orders") {
									return;
								}
							}
						}
					});

					if (NRS.currentPage != "open_orders") {
						return;
					}
				}
			} else {
				NRS.getUnconfirmedOrders(type, function(unconfirmedOrders) {
					NRS.openOrdersLoaded(unconfirmedOrders, lowercase, callback);
				});
			}
		});
	}

	NRS.getUnconfirmedOrders = function(type, callback) {
		if (NRS.unconfirmedTransactions.length) {
			var unconfirmedOrders = [];

			for (var i = 0; i < NRS.unconfirmedTransactions.length; i++) {
				var unconfirmedTransaction = NRS.unconfirmedTransactions[i];

				if (unconfirmedTransaction.type == 2 && unconfirmedTransaction.subtype == (type == "ask" ? 2 : 3)) {
					unconfirmedOrders.push({
						"account": unconfirmedTransaction.sender,
						"asset": unconfirmedTransaction.attachment.asset,
						"assetName": "",
						"decimals": 0,
						"height": 0,
						"order": unconfirmedTransaction.id,
						"priceNQT": unconfirmedTransaction.attachment.priceNQT,
						"quantityQNT": unconfirmedTransaction.attachment.quantityQNT,
						"tentative": true
					})
				}
			}

			if (unconfirmedOrders.length == 0) {
				callback([]);
			} else {
				var nr_orders = 0;

				for (var i = 0; i < unconfirmedOrders.length; i++) {
					NRS.sendRequest("getAsset+", {
						"asset": unconfirmedOrders[i].asset,
						"_extra": {
							"id": i
						}
					}, function(asset, input) {
						unconfirmedOrders[input["_extra"].id].assetName = asset.name;
						unconfirmedOrders[input["_extra"].id].decimals = asset.decimals;

						nr_orders++;

						if (nr_orders == unconfirmedOrders.length) {
							callback(unconfirmedOrders);
						}
					});
				}
			}
		} else {
			callback([]);
		}
	}

	NRS.openOrdersLoaded = function(orders, type, callback) {
		if (!orders.length) {
			$("#open_" + type + "_orders_table tbody").empty();
			NRS.dataLoadFinished($("#open_" + type + "_orders_table"));

			callback();

			return;
		}

		orders.sort(function(a, b) {
			if (a.assetName.toLowerCase() > b.assetName.toLowerCase()) {
				return 1;
			} else if (a.assetName.toLowerCase() < b.assetName.toLowerCase()) {
				return -1;
			} else {
				if (a.quantity * a.price > b.quantity * b.price) {
					return 1;
				} else if (a.quantity * a.price < b.quantity * b.price) {
					return -1;
				} else {
					return 0;
				}
			}
		});

		var rows = "";

		for (var i = 0; i < orders.length; i++) {
			var completeOrder = orders[i];

			var cancelled = false;

			if (NRS.unconfirmedTransactions.length) {
				for (var j = 0; j < NRS.unconfirmedTransactions.length; j++) {
					var unconfirmedTransaction = NRS.unconfirmedTransactions[j];

					if (unconfirmedTransaction.type == 2 && unconfirmedTransaction.subtype == (type == "ask" ? 4 : 5) && unconfirmedTransaction.attachment.order == completeOrder.order) {
						cancelled = true;
						break;
					}
				}
			}

			completeOrder.priceNQT = new BigInteger(completeOrder.priceNQT);
			completeOrder.quantityQNT = new BigInteger(completeOrder.quantityQNT);
			completeOrder.totalNQT = new BigInteger(NRS.calculateOrderTotalNQT(completeOrder.quantityQNT, completeOrder.priceNQT));

			rows += "<tr data-order='" + String(completeOrder.order).escapeHTML() + "'" + (cancelled ? " class='tentative tentative-crossed'" : (completeOrder.tentative ? " class='tentative'" : "")) + "><td><a href='#' data-goto-asset='" + String(completeOrder.asset).escapeHTML() + "'>" + completeOrder.assetName.escapeHTML() + "</a></td><td>" + NRS.formatQuantity(completeOrder.quantityQNT, completeOrder.decimals) + "</td><td>" + NRS.formatOrderPricePerWholeQNT(completeOrder.priceNQT, completeOrder.decimals) + "</td><td>" + NRS.formatAmount(completeOrder.totalNQT) + "</td><td class='cancel'>" + (cancelled || completeOrder.tentative ? "/" : "<a href='#' data-toggle='modal' data-target='#cancel_order_modal' data-order='" + String(completeOrder.order).escapeHTML() + "' data-type='" + type + "'>Cancel</a>") + "</td></tr>";
		}

		$("#open_" + type + "_orders_table tbody").empty().append(rows);

		NRS.dataLoadFinished($("#open_" + type + "_orders_table"));
		orders = {};

		callback();
	}

	NRS.incoming.open_orders = function(transactions) {
		if (transactions || NRS.unconfirmedTransactionsChange) {
			NRS.pages.open_orders();
		}
	}

	$("#cancel_order_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var orderType = $invoker.data("type");
		var orderId = $invoker.data("order");

		if (orderType == "bid") {
			$("#cancel_order_type").val("cancelBidOrder");
		} else {
			$("#cancel_order_type").val("cancelAskOrder");
		}

		$("#cancel_order_order").val(orderId);
	});

	NRS.forms.cancelOrder = function($modal) {
		var orderType = $("#cancel_order_type").val();

		return {
			"requestType": orderType,
			"successMessage": $modal.find("input[name=success_message]").val().replace("__", (orderType == "cancelBidOrder" ? "buy" : "sell"))
		};
	}

	NRS.forms.cancelOrderComplete = function(response, data) {
		NRS.addUnconfirmedTransaction(response.transaction);

		$("#open_orders_page tr[data-order=" + String(data.order).escapeHTML() + "]").addClass("tentative tentative-crossed").find("td.cancel").html("/");
	}

	return NRS;
}(NRS || {}, jQuery));