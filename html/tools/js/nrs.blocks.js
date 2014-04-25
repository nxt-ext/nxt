var NRS = (function(NRS, $, undefined) {
	NRS.blocksPageType = null;
	NRS.tempBlocks = [];

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

	NRS.handleInitialBlocks = function(response) {
		if (response.errorCode) {
			return;
		}

		NRS.blocks.push(response);

		if (NRS.blocks.length < 10 && response.previousBlock) {
			NRS.getBlock(response.previousBlock, NRS.handleInitialBlocks);
		} else {
			NRS.lastBlockHeight = NRS.blocks[0].height;

			NRS.useNQT = (NRS.isTestNet && NRS.lastBlockHeight >= 76500) || (!NRS.isTestNet && NRS.lastBlockHeight >= 132000);

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

				rows += "<tr><td><a href='#' data-block='" + String(block.height).escapeHTML() + "' class='block'" + (block.numberOfTransactions > 0 ? " style='font-weight:bold'" : "") + ">" + String(block.height).escapeHTML() + "</a></td><td>" + NRS.formatTimestamp(block.timestamp) + "</td><td>" + NRS.formatAmount(block.totalAmountNQT) + " + " + NRS.formatAmount(block.totalFeeNQT) + "</td><td>" + NRS.formatAmount(block.numberOfTransactions) + "</td></tr>";
			}

			$("#dashboard_blocks_table tbody").empty().append(rows);
			NRS.dataLoadFinished($("#dashboard_blocks_table"));
		}
	}

	NRS.handleNewBlocks = function(response) {
		if (NRS.downloadingBlockchain) {
			//new round started...
			if (NRS.tempBlocks.length == 0 && NRS.state.lastBlock != response.id) {
				return;
			}
		}

		//we have all blocks 	
		if (response.height - 1 == NRS.lastBlockHeight || NRS.tempBlocks.length == 99) {
			var newBlocks = [];

			//there was only 1 new block (response)
			if (NRS.tempBlocks.length == 0) {
				//remove oldest block, add newest block
				NRS.blocks.unshift(response);
				newBlocks.push(response);
			} else {
				NRS.tempBlocks.push(response);
				//remove oldest blocks, add newest blocks
				[].unshift.apply(NRS.blocks, NRS.tempBlocks);
				newBlocks = NRS.tempBlocks;
				NRS.tempBlocks = [];
			}

			if (NRS.blocks.length > 100) {
				NRS.blocks = NRS.blocks.slice(0, 100);
			}

			//set new last block height
			NRS.lastBlockHeight = NRS.blocks[0].height;

			NRS.useNQT = (NRS.isTestNet && NRS.lastBlockHeight >= 76500) || (!NRS.isTestNet && NRS.lastBlockHeight >= 132000);

			NRS.incoming.updateDashboardBlocks(newBlocks);
		} else {
			NRS.tempBlocks.push(response);
			NRS.getBlock(response.previousBlock, NRS.handleNewBlocks);
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

			rows += "<tr><td><a href='#' data-block='" + String(block.height).escapeHTML() + "' class='block'" + (block.numberOfTransactions > 0 ? " style='font-weight:bold'" : "") + ">" + String(block.height).escapeHTML() + "</a></td><td>" + NRS.formatTimestamp(block.timestamp) + "</td><td>" + NRS.formatAmount(block.totalAmountNQT) + " + " + NRS.formatAmount(block.totalFeeNQT) + "</td><td>" + NRS.formatAmount(block.numberOfTransactions) + "</td></tr>";
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

	NRS.pages.blocks = function() {
		NRS.pageLoading();

		$("#forged_blocks_warning").hide();

		if (NRS.blocksPageType == "forged_blocks") {
			$("#forged_fees_total_box, #forged_blocks_total_box").show();
			$("#blocks_transactions_per_hour_box, #blocks_generation_time_box").hide();

			NRS.sendRequest("getAccountBlockIds+", {
				"account": NRS.account,
				"timestamp": 0
			}, function(response) {
				if (response.blockIds && response.blockIds.length) {
					var blocks = [];
					var nr_blocks = 0;

					var blockIds = response.blockIds.reverse().slice(0, 100);

					if (response.blockIds.length > 100) {
						$("#blocks_page_forged_warning").show();
					}

					for (var i = 0; i < blockIds.length; i++) {
						NRS.sendRequest("getBlock+", {
							"block": blockIds[i],
							"_extra": {
								"nr": i
							}
						}, function(block, input) {
							if (NRS.currentPage != "blocks") {
								blocks = {};
								return;
							}

							blocks[input["_extra"].nr] = block;
							nr_blocks++;

							if (nr_blocks == blockIds.length) {
								NRS.blocksPageLoaded(blocks);
							}
						});

						if (NRS.currentPage != "blocks") {
							blocks = {};
							return;
						}
					}
				} else {
					NRS.blocksPageLoaded({});
				}
			});
		} else {
			$("#forged_fees_total_box, #forged_blocks_total_box").hide();
			$("#blocks_transactions_per_hour_box, #blocks_generation_time_box").show();

			if (NRS.blocks.length < 100) {
				if (NRS.downloadingBlockchain) {
					NRS.blocksPageLoaded(NRS.blocks);
				} else {
					var previousBlock = NRS.blocks[NRS.blocks.length - 1].previousBlock;
					//if previous block is undefined, dont try add it
					if (typeof previousBlock !== "undefined")
						NRS.getBlock(previousBlock, NRS.finish100Blocks);
				}
			} else {
				NRS.blocksPageLoaded(NRS.blocks);
			}
		}
	}

	NRS.finish100Blocks = function(response) {
		NRS.blocks.push(response);
		if (NRS.blocks.length < 100 && typeof response.previousBlock !== "undefined") {
			NRS.getBlock(response.previousBlock, NRS.finish100Blocks);
		} else {
			NRS.blocksPageLoaded(NRS.blocks);
		}
	}

	NRS.blocksPageLoaded = function(blocks) {
		var rows = "";
		var totalAmount = new BigInteger();
		var totalFees = new BigInteger();
		var totalTransactions = 0;

		for (var i = 0; i < blocks.length; i++) {
			var block = blocks[i];

			totalAmount = totalAmount.add(new BigInteger(block.totalAmountNQT));
			totalFees = totalFees.add(new BigInteger(block.totalFeeNQT));

			totalTransactions += block.numberOfTransactions;

			var account = String(block.generator).escapeHTML();

			rows += "<tr><td><a href='#' data-block='" + String(block.height).escapeHTML() + "' class='block'" + (block.numberOfTransactions > 0 ? " style='font-weight:bold'" : "") + ">" + String(block.height).escapeHTML() + "</a></td><td>" + NRS.formatTimestamp(block.timestamp) + "</td><td>" + NRS.formatAmount(block.totalAmountNQT) + "</td><td>" + NRS.formatAmount(block.totalFeeNQT) + "</td><td>" + NRS.formatAmount(block.numberOfTransactions) + "</td><td>" + (account != NRS.genesis ? "<a href='#' data-user='" + account + "' class='user_info'>" + NRS.getAccountTitle(account) + "</a>" : "Genesis") + "</td><td>" + NRS.formatVolume(block.payloadLength) + "</td><td>" + Math.round(block.baseTarget / 153722867 * 100).pad(4) + " %</td></tr>";
		}

		var startingTime = NRS.blocks[NRS.blocks.length - 1].timestamp;
		var endingTime = NRS.blocks[0].timestamp;
		var time = endingTime - startingTime;

		$("#blocks_table tbody").empty().append(rows);
		NRS.dataLoadFinished($("#blocks_table"));

		var averageFee = new Big(totalFees.toString()).div(new Big("100000000")).div(new Big(String(blocks.length))).toFixed(2);
		var averageAmount = new Big(totalAmount.toString()).div(new Big("100000000")).div(new Big(String(blocks.length))).toFixed(2);

		averageFee = NRS.convertToNQT(averageFee);
		averageAmount = NRS.convertToNQT(averageAmount);

		$("#blocks_average_fee").html(NRS.formatAmount(averageFee)).removeClass("loading_dots");
		$("#blocks_average_amount").html(NRS.formatAmount(averageAmount)).removeClass("loading_dots");

		if (NRS.blocksPageType == "forged_blocks") {
			if (blocks.length == 100) {
				var blockCount = blocks.length + "+";
				var feeTotal = NRS.formatAmount(totalFees, false) + "+";
			} else {
				var blockCount = blocks.length;
				var feeTotal = NRS.formatAmount(totalFees, false);
			}

			$("#forged_blocks_total").html(blockCount).removeClass("loading_dots");
			$("#forged_fees_total").html(feeTotal).removeClass("loading_dots");
		} else {
			$("#blocks_transactions_per_hour").html(Math.round(totalTransactions / (time / 60) * 60)).removeClass("loading_dots");
			$("#blocks_average_generation_time").html(Math.round(time / 100) + "s").removeClass("loading_dots");
		}

		NRS.pageLoaded();
	}

	$("#blocks_page_type li a").click(function(e) {
		e.preventDefault();

		var type = $(this).data("type");

		if (type) {
			NRS.blocksPageType = type;
		} else {
			NRS.blocksPageType = null;
		}

		$(this).parents(".btn-group").find(".text").text($(this).text());

		NRS.pages.blocks();
	});


	return NRS;
}(NRS || {}, jQuery));