/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 * Copyright © 2016-2017 Jelurida IP B.V.                                     *
 *                                                                            *
 * See the LICENSE.txt file at the top-level directory of this distribution   *
 * for licensing information.                                                 *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,*
 * no part of the Nxt software, including this file, may be copied, modified, *
 * propagated, or distributed except according to the terms contained in the  *
 * LICENSE.txt file.                                                          *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $) {
    var DEPOSIT_ADDRESSES_KEY = "changelly.depositAddresses.";
    var SUPPORTED_COINS = {};

    var coinToPair = function (op, coin) {
        return (op === "buy") ? "NXT_" + coin : coin + "_NXT";
    };

    var pairToCoin = function (pair) {
        if (pair.indexOf("NXT_") === 0) {
            return pair.substring("NXT_".length);
        }
        if (pair.indexOf("_NXT") === pair.length - "_NXT".length) {
            return pair.substring(0, pair.indexOf("_NXT"));
        }
        throw "illegal pair " + pair;
    };

    var reversePair = function (pair) {
        var pairParts = pair.split('_');
        return pairParts[1] + '_' + pairParts[0];
    };

    var getCoins = function() {
        var coins = [];
        for (var i=0; i<3; i++) {
            coins.push(NRS.settings["exchange_coin" + i]);
        }
        return coins;
    };

    var setCoins = function(coins) {
        for (var i=0; i<coins.length; i++) {
            NRS.updateSettings("exchange_coin" + i, coins[i]);
        }
    };

    var addDepositAddress = function(address, from, to) {
        var json = localStorage[DEPOSIT_ADDRESSES_KEY + NRS.accountRS];
        var addresses;
        if (json === undefined) {
            addresses = [];
        } else {
            addresses = JSON.parse(json);
            if (addresses.length > 5) {
                addresses.splice(5, addresses.length - 5);
            }
        }
        addresses.splice(0, 0, { address: address, from: from, to: to, time: Date.now() });
        NRS.logConsole("deposit address " + address + " from " + from + " to " + to + " added");
        localStorage[DEPOSIT_ADDRESSES_KEY + NRS.accountRS] = JSON.stringify(addresses);
    };

    var apiCall = function (method, params, doneCallback, ignoreError, modal) {
        var postData = {};
        postData.method = method;
        postData.jsonrpc = "2.0";
        postData.params = params;
        postData.id = "1";
        var hmac = CryptoJS.algo.HMAC.create(CryptoJS.algo.SHA512, NRS.settings.changelly_api_secret);
        hmac.update(JSON.stringify(postData));
        var signature = hmac.finalize();
        NRS.logConsole("changelly api call method: " + method + " post data: " + JSON.stringify(postData) + " api-key: " + NRS.settings.changelly_api_key + " signature:" + signature +
            (ignoreError ? " ignore " + ignoreError : "") + (modal ? " modal " + modal : ""));
        $.ajax({
            url: NRS.getChangellyUrl(),
            beforeSend: function(xhr) {
                xhr.setRequestHeader("api-key", NRS.settings.changelly_api_key);
                xhr.setRequestHeader("sign", signature);
                xhr.setRequestHeader("Content-Type", "application/json; charset=utf-8");
            },
            crossDomain: true,
            dataType: "json",
            type: "POST",
            timeout: 30000,
            async: true,
            data: JSON.stringify(postData)
        }).done(function(response, status) {
            if (status !== "success") {
                NRS.logConsole(method + ' status ' + status);
                if (modal) {
                    NRS.showModalError(status, modal);
                }
            }
            if (response.error) {
                var error = response.error;
                var msg;
                if (error.code) {
                    msg = ' code ' + error.code + ' message ' + error.message;
                    NRS.logConsole(method + msg);
                } else {
                    msg = error;
                    NRS.logConsole(method + ' error ' + error);
                }
                if (ignoreError === false) {
                    return;
                }
                if (modal) {
                    NRS.showModalError(msg, modal);
                }
                // TODO adapt for changelly
                if (method.indexOf("txStat/") !== 0 && method.indexOf("cancelpending") !== 0) {
                    $("#changelly_status").html($.t("error"));
                }
            }
            doneCallback(response);
        }).fail(function (xhr, textStatus, error) {
            var message = "Request failed, action " + method + " method " + method + " status " + textStatus + " error " + error;
            NRS.logConsole(message);
            throw message;
        })
    };

    function invert(rate) {
        return Math.round(100000000 / parseFloat(rate)) / 100000000;
    }

    var renderExchangeTable = function (op) {
        var coins = getCoins();
        var tasks = [];
        for (var i = 0; i < coins.length; i++) {
            tasks.push((function (i) {
                return function (callback) {
                    var from, to;
                    if (op == "buy") {
                        from = "NXT";
                        to = coins[i];
                    } else {
                        from = coins[i];
                        to = "NXT";
                    }
                    async.waterfall([
                        function(callback) {
                            apiCall("getMinAmount", { from: from, to: to }, function (response) {
                                callback(response.error, response);
                            })
                        },
                        function(minAmount, callback) {
                            apiCall("getExchangeAmount", { from: from, to: to, amount: "1" }, function (response) {
                                response.minAmount = minAmount.result;
                                response.rate = response.result;
                                delete response.result;
                                callback(null, response);
                            })
                        }
                    ], function(err, response){
                        if (err) {
                            callback(err, err);
                            return;
                        }
                        var rate;
                        var symbol;
                        if (op === "sell") {
                            rate = invert(response.rate);
                            symbol = coins[i];
                        } else {
                            rate = response.rate;
                            symbol = "NXT";
                        }
                        var row = "<tr><td>" + coins[i] + "</td>";
                        row += "<td><span>" + String(response.minAmount).escapeHTML() + "</span>&nbsp<span>" + symbol + "</span></td>";
                        row += "<td>" + String(rate).escapeHTML() + "</td>";
                        row += "<td><a href='#' class='btn btn-xs btn-default' data-toggle='modal' data-target='#m_changelly_" + op + "_modal' " +
                            "data-from='" + from + "' data-to='" + to + "' data-rate='" + response.rate + "' data-min='" + response.minAmount + "'>" + $.t(op) + "</a>";
                        NRS.logConsole(row);
                        callback(null, row);
                    });
                }
            })(i));
        }
        NRS.logConsole(tasks.length + " tasks ready to run");
        async.series(tasks, function (err, results) {
            var table = $("#p_changelly_" + op + "_nxt");
            if (err) {
                NRS.logConsole("Err: ", err, "\nResults:", results);
                table.find("tbody").empty();
                NRS.dataLoadFinished(table);
                return;
            }
            NRS.logConsole("results", results);
            var rows = "";
            for (i = 0; i < results.length; i++) {
                rows += results[i];
            }
            NRS.logConsole("rows " + rows);
            table.find("tbody").empty().append(rows);
            NRS.dataLoadFinished(table);
        });
    };

    var getAddressLink = function (address, coin) {
        if (coin.toUpperCase() === "NXT") {
            return NRS.getAccountLink({ accountRS: address }, "account");
        }
        if (coin.toUpperCase() === "BTC") {
            return "<a target='_blank' href='https://blockchain.info/address/" + address + "'>" + address + "</a>";
        }
        return address;
    };

    var getTransactionLink = function (transaction, coin) {
        if (coin.toUpperCase() === "NXT") {
            return "<a href='#' class='show_transaction_modal_action' data-transaction='" + transaction + "'>" + transaction + "</a>";
        }
        if (coin.toUpperCase() === "BTC") {
            return "<a target='_blank' href='https://blockchain.info/tx/" + transaction + "'>" + transaction + "</a>";
        }
        return transaction;
    };

    var renderMyExchangesTable = function () {
        var depositAddressesJSON = localStorage[DEPOSIT_ADDRESSES_KEY + NRS.accountRS];
        var depositAddresses = [];
        if (depositAddressesJSON) {
            depositAddresses = JSON.parse(depositAddressesJSON);
        }
        var tasks = [];
        var empty = "<td></td>";
        for (var i = 0; i < depositAddresses.length; i++) {
            tasks.push((function (i) {
                return function (callback) {
                    NRS.logConsole("my exchanges iteration " + i);
                    apiCall("getTransactions", {address: depositAddresses[i].address}, function(response) {
                        var rows = "";
                        for (var j=0; j < response.result.length; j++) {
                            var transaction = response.result[j];
                            var row = "";
                            row += "<tr>";
                            row += "<td>" + NRS.formatTimestamp(transaction.createdAt, false, true) + "</td>";
                            row += "<td>" + transaction.status + "</td>";
                            if (transaction.status === "failed") {
                                row += "<td>" + "Print Error Here" + "</td>"; // TODO
                                row += empty + empty + empty + empty + empty + empty;
                                NRS.logConsole(row);
                                continue;
                            }
                            row += "<td>" + getAddressLink(transaction.payinAddress, transaction.currencyFrom) + "</td>";
                            row += "<td>" + transaction.amountFrom + "</td>";
                            row += "<td>" + transaction.currencyFrom + "</td>";
                            row += "<td>" + getAddressLink(transaction.payoutAddress, transaction.currencyTo) + "</td>";
                            row += "<td>" + transaction.amountTo + "</td>";
                            row += "<td>" + transaction.currencyTo + "</td>";
                            row += "<td>" + getTransactionLink(transaction.payoutHash, transaction.currencyTo) + "</td>";
                            NRS.logConsole(row);
                            rows += row;
                        }
                        callback(null, rows);
                    }, true);
                }
            })(i));
        }
        NRS.logConsole(tasks.length + " tasks ready to run");
        var table = $("#p_changelly_my_table");
        if (tasks.length === 0) {
            table.find("tbody").empty();
            NRS.dataLoadFinished(table);
            return;
        }
        async.series(tasks, function (err, results) {
            if (err) {
                NRS.logConsole("Err: ", err, "\nResults:", results);
                table.find("tbody").empty();
                NRS.dataLoadFinished(table);
                return;
            }
            NRS.logConsole("results", results);
            var rows = "";
            for (i = 0; i < results.length; i++) {
                rows += results[i];
            }
            NRS.logConsole("rows " + rows);
            table.find("tbody").empty().append(rows);
            NRS.dataLoadFinished(table);
        });
    };

    function loadCoins() {
        var inputFields = [];
        inputFields.push($('#changelly_coin_0'));
        inputFields.push($('#changelly_coin_1'));
        inputFields.push($('#changelly_coin_2'));
        var selectedCoins = [];
        selectedCoins.push(NRS.settings.exchange_coin0);
        selectedCoins.push(NRS.settings.exchange_coin1);
        selectedCoins.push(NRS.settings.exchange_coin2);
        apiCall('getCurrencies', {}, function (data) {
            SUPPORTED_COINS = data.result;
            for (var i = 0; i < inputFields.length; i++) {
                inputFields[i].empty();
                var isSelectionAvailable = false;
                for (var j=0; j < data.result.length; j++) {
                    var code = String(data.result[j]).toUpperCase();
                    if (code !== 'NXT') {
                        inputFields[i].append('<option value="' + code + '">' + code + '</option>');
                        SUPPORTED_COINS[code] = code;
                    }
                    if (selectedCoins[i] === code) {
                        isSelectionAvailable = true;
                    }
                }
                if (isSelectionAvailable) {
                    inputFields[i].val(selectedCoins[i]);
                }
            }
            $('#changelly_status').html('ok');
        });
    }

    NRS.pages.exchange_changelly = function() {
        var exchangeDisabled = $(".exchange_disabled");
        var exchangePageHeader = $(".exchange_page_header");
        var exchangePageContent = $(".exchange_page_content");
        if (NRS.settings.exchange !== "1") {
			exchangeDisabled.show();
            exchangePageHeader.hide();
            exchangePageContent.hide();
            return;
		}
        exchangeDisabled.hide();
        exchangePageHeader.show();
        exchangePageContent.show();
        NRS.pageLoading();
        loadCoins();
        renderExchangeTable("buy");
        renderExchangeTable("sell");
        renderMyExchangesTable();
        NRS.pageLoaded();
        setTimeout(refreshPage, 60000);
    };

    refreshPage = function() {
        if (NRS.currentPage === "exchange_changelly") {
            NRS.pages.exchange_changelly();
        }
    };

    $("#changelly_accept_exchange_link").on("click", function(e) {
   		e.preventDefault();
   		NRS.updateSettings("exchange", "1");
        NRS.pages.exchange_changelly();
   	});

    $("#clear_my_exchanges").on("click", function(e) {
   		e.preventDefault();
   		localStorage.removeItem(DEPOSIT_ADDRESSES_KEY + NRS.accountRS);
        renderMyExchangesTable();
   	});

    NRS.getFundAccountLink = function() {
        return "<div class='callout callout-danger'>" +
            "<span>" + $.t("fund_account_warning_1") + "</span><br>" +
            "<span>" + $.t("fund_account_warning_2") + "</span><br>" +
            "<span>" + $.t("fund_account_warning_3") + "</span><br>" +
            "</div>" +
            "<a href='#' class='btn btn-xs btn-default' data-toggle='modal' data-target='#m_send_amount_sell_modal' " +
            "data-pair='BTC_NXT'>" + $.t("fund_account_message") + "</a>";
    };

    $('.coin-select').change(function() {
        var id = $(this).attr('id');
        var coins = getCoins();
        coins[parseInt(id.slice(-1))] = $(this).val();
        setCoins(coins);
        renderExchangeTable('buy');
        renderExchangeTable('sell');
    });

	NRS.setup.exchange = function() {
        // Do not implement connection to a 3rd party site here to prevent privacy leak
    };

    $("#m_changelly_buy_modal").on("show.bs.modal", function (e) {
        var invoker = $(e.relatedTarget);
        var pair = invoker.data("pair");
        var from = invoker.data("from");
        var to = invoker.data("to");
        var min = invoker.data("min");
        $("#m_changelly_buy_from").val(from);
        $("#m_changelly_buy_to").val(to);
        NRS.logConsole("modal invoked from " + from + " to " + to);
        $("#m_changelly_buy_title").html($.t("exchange_nxt_to_coin", { coin: to }));
        $("#m_changelly_buy_min").val(invoker.data("min"));
        $("#m_changelly_buy_min_coin").html("NXT");
        $("#m_changelly_buy_rate").val(invoker.data("rate"));
        $("#m_changelly_buy_rate_text").html("NXT/" + to);
        $("#m_changelly_withdrawal_address_coin").html(to);
    });

    $("#m_changelly_buy_submit").on("click", function(e) {
        e.preventDefault();
        var modal = $(this).closest(".modal");
        var amountNXT = $("#m_changelly_buy_amount").val();
        var minAmount = $("#m_changelly_buy_min").val();
        if (parseFloat(amountNXT) <= parseFloat(minAmount)) {
            msg = "amount is lower tham minimum amount " + minAmount;
            NRS.logConsole(msg);
            NRS.showModalError(msg, modal);
            return;
        }
        var amountNQT = NRS.convertToNQT(amountNXT);
        var withdrawal = $("#m_changelly_buy_withdrawal_address").val();
        var from = $("#m_changelly_buy_from").val();
        var to = $("#m_changelly_buy_to").val();
        NRS.logConsole('changelly withdrawal to address ' + withdrawal + " coin " + to);
        apiCall('generateAddress', {
            from: from,
            to: to,
            address: withdrawal
        }, function (data) {
            var msg;
            if (data.error) {
                NRS.logConsole("Changelly generateAddress error " + data.error.code + " " + data.error.message);
                return;
            }
            var depositAddress = data.result.address;
            if (!depositAddress) {
                msg = "changelly did not return a deposit address for id " + data.id;
                NRS.logConsole(msg);
                NRS.showModalError(msg, modal);
                return;
            }

            NRS.logConsole("NXT deposit address " + depositAddress);
            NRS.sendRequest("sendMoney", {
                "recipient": depositAddress,
                "amountNQT": amountNQT,
                "secretPhrase": $("#m_changelly_buy_password").val(),
                "deadline": "1440",
                "feeNQT": NRS.convertToNQT(1)
            }, function (response) {
                if (response.errorCode) {
                    NRS.logConsole("sendMoney response " + response.errorCode + " " + response.errorDescription.escapeHTML());
                    NRS.showModalError(NRS.translateServerError(response), modal);
                    return;
                }
                addDepositAddress(depositAddress, from, to);
                renderMyExchangesTable();
                $("#m_changelly_buy_passpharse").val("");
                modal.modal("hide");
            })
        }, true, modal);
    });

    $("#m_send_amount_buy_modal").on("show.bs.modal", function (e) {
        var invoker = $(e.relatedTarget);
        var pair = invoker.data("pair");
        var coin = pairToCoin(pair);
        NRS.logConsole("modal invoked pair " + pair + " coin " + coin);
        $("#m_send_amount_buy_title").html($.t("exchange_nxt_to_coin_send_amount", { coin: coin }));
        $("#m_send_amount_buy_withdrawal_amount_coin").html(coin);
        $("#m_send_amount_buy_rate_text").html("NXT/" + coin);
        $("#m_send_amount_withdrawal_address_coin").html(coin + " address");
        $("#m_send_amount_buy_fee_coin").html(coin);
        $("#m_send_amount_buy_pair").val(pair);
        $("#m_send_amount_buy_submit").prop('disabled', true);
    });

    $('#m_send_amount_buy_withdrawal_amount, #m_send_amount_buy_withdrawal_address').change(function () {
        var modal = $(this).closest(".modal");
        var amount = $('#m_send_amount_buy_withdrawal_amount').val();
        var withdrawal = $('#m_send_amount_buy_withdrawal_address').val();
        var pair = $("#m_send_amount_buy_pair").val();
        var buySubmit = $("#m_send_amount_buy_submit");
        buySubmit.prop('disabled', true);
        if (amount === "" || withdrawal === "") {
            return;
        }
        modal.css('cursor','wait');
        apiCall('sendamount', {
            amount: amount,
            withdrawal: withdrawal,
            pair: pair,
            apiKey: NRS.settings.exchange_api_key
        }, function (data) {
            try {
                var rate = $("#m_send_amount_buy_rate");
                var fee = $("#m_send_amount_buy_fee");
                var depositAmount = $("#m_send_amount_buy_deposit_amount");
                var depositAddress = $("#m_send_amount_buy_deposit_address");
                var expiration = $("#m_send_amount_buy_expiration");
                if (data.error) {
                    rate.val("");
                    fee.val("");
                    depositAmount.val("");
                    depositAddress.val("");
                    expiration.val("");
                    buySubmit.prop('disabled', true);
                    return;
                }
                if (amount !== data.success.withdrawalAmount) {
                    NRS.showModalError("amount returned from shapeshift " + data.success.withdrawalAmount +
                        " differs from requested amount " + amount, modal);
                    return;
                }
                if (withdrawal !== data.success.withdrawal) {
                    NRS.showModalError("withdrawal address returned from shapeshift " + data.success.withdrawal +
                        " differs from requested address " + withdrawal, modal);
                    return;
                }
                modal.find(".error_message").html("").hide();
                rate.val(data.success.quotedRate);
                fee.val(data.success.minerFee);
// add 1 NXT fee to make sure the net amount is what requested by shape shift
                depositAmount.val(parseFloat(data.success.depositAmount) + 1);
                depositAddress.val(data.success.deposit);
                expiration.val(NRS.formatTimestamp(data.success.expiration, false, true));
                buySubmit.prop('disabled', false);
            } finally {
                modal.css('cursor', 'default');
            }
        }, true, modal)
    });

    $("#m_send_amount_buy_submit").on("click", function(e) {
        e.preventDefault();
        var modal = $(this).closest(".modal");
        var pair = $("#m_send_amount_buy_pair").val();
        var depositAddress = $("#m_send_amount_buy_deposit_address").val();
        NRS.logConsole("pay request submitted, deposit address " + depositAddress);
        var amountNQT = NRS.convertToNQT($("#m_send_amount_buy_deposit_amount").val());
        NRS.sendRequest("sendMoney", {
            "recipient": depositAddress,
            "amountNQT": amountNQT,
            "secretPhrase": $("#m_send_amount_buy_password").val(),
            "deadline": "1440",
            "feeNQT": NRS.convertToNQT(1)
        }, function (response) {
            if (response.errorCode) {
                NRS.logConsole('sendMoney error ' + response.errorDescription.escapeHTML());
                NRS.showModalError(response.errorDescription.escapeHTML(), modal);
                return;
            }
            addDepositAddress(depositAddress, pair);
            renderMyExchangesTable();
            $("#m_send_amount_buy_passpharse").val("");
            modal.modal("hide");
        });
    });

    $("#m_changelly_sell_modal").on("show.bs.modal", function (e) {
        var invoker = $(e.relatedTarget);
        var modal = $(this).closest(".modal");
        var pair = invoker.data("pair");
        var coin = pairToCoin(pair);
        NRS.logConsole("modal invoked pair " + pair + " coin " + coin);
        $("#m_changelly_sell_title").html($.t("exchange_coin_to_nxt_shift", { coin: coin }));
        $("#m_changelly_sell_qr_code").html("");
        var data = invoker.data;
        modal.css('cursor','wait');
        async.waterfall([
            function(callback) {
                if (data.rate) {
                    callback(null);
                } else {
                    apiCall("marketinfo/" + pair, {}, function (response) {
                        data.rate = response.rate;
                        data.min = response.minimum;
                        data.max = response.limit;
                        data.fee = response.minerFee;
                        callback(null);
                    })
                }
            },
            function(callback) {
                $("#m_changelly_sell_min").val(data.min);
                $("#m_changelly_sell_min_coin").html(coin);
                $("#m_changelly_sell_max").val(data.max);
                $("#m_changelly_sell_max_coin").html(coin);
                $("#m_changelly_sell_rate").val(data.rate);
                $("#m_changelly_sell_rate_text").html(coin + "/NXT");
                $("#m_changelly_sell_fee").val(data.fee);
                $("#m_changelly_sell_fee_coin").html("NXT");
                $("#m_changelly_sell_pair").val(pair);
                var publicKey = NRS.publicKey;
                if (publicKey === "" && NRS.accountInfo) {
                    publicKey = NRS.accountInfo.publicKey;
                }
                if (!publicKey || publicKey === "") {
                    NRS.showModalError("Account has no public key, please login using your passphrase", modal);
                    return;
                }
                apiCall('shift', {
                    withdrawal: NRS.accountRS,
                    rsAddress: publicKey,
                    pair: pair,
                    apiKey: NRS.settings.exchange_api_key
                }, function (data) {
                    NRS.logConsole("shift request done");
                    var msg;
                    if (data.depositType !== coin) {
                        msg = "incorrect deposit coin " + data.depositType;
                        NRS.logConsole(msg);
                        NRS.showModalError(msg, modal);
                        callback(null);
                        return;
                    }
                    if (data.withdrawalType !== "NXT") {
                        msg = "incorrect withdrawal coin " + data.withdrawalType;
                        NRS.logConsole(msg);
                        NRS.showModalError(msg, modal);
                        callback(null);
                        return;
                    }
                    if (data.withdrawal !== NRS.accountRS) {
                        msg = "incorrect withdrawal address " + data.withdrawal;
                        NRS.logConsole(msg);
                        NRS.showModalError(msg, modal);
                        callback(null);
                        return;
                    }
                    NRS.logConsole("shift request done, deposit address " + data.deposit);
                    $("#m_changelly_sell_deposit_address").html(data.deposit);
                    NRS.generateQRCode("#m_changelly_sell_qr_code", data.deposit);
                    callback(null);
                })
            }
        ], function (err, result) {
            modal.css('cursor', 'default');
        })
    });

    $("#m_changelly_sell_done").on("click", function(e) {
        e.preventDefault();
        var pair = $("#m_changelly_sell_pair").val();
        var deposit = $("#m_changelly_sell_deposit_address").html();
        if (deposit !== "") {
            addDepositAddress(deposit, pair);
            renderMyExchangesTable();
            $(this).closest(".modal").modal("hide");
        }
    });

    $("#m_changelly_sell_cancel").on("click", function(e) {
        e.preventDefault();
        var deposit = $("#m_changelly_sell_deposit_address").html();
        if (deposit !== "") {
            apiCall('cancelpending', {address: deposit}, function (data) {
                var msg = data.success ? data.success : data.err;
                NRS.logConsole("sell cancelled response: " + msg);
            })
        }
    });

    $("#m_send_amount_sell_modal").on("show.bs.modal", function (e) {
        var invoker = $(e.relatedTarget);
        var modal = $(this).closest(".modal");
        var pair = invoker.data("pair");
        var coin = pairToCoin(pair);
        NRS.logConsole("modal invoked pair " + pair + " coin " + coin);
        $("#m_send_amount_sell_title").html($.t("exchange_coin_to_nxt_send_amount", { coin: coin }));
        $("#m_send_amount_sell_rate_text").html("NXT/" + coin);
        $("#m_send_amount_sell_fee_coin").html("NXT");
        $("#m_send_amount_sell_withdrawal_amount_coin").html("NXT");
        $("#m_send_amount_sell_deposit_amount_coin").html(coin);
        $("#m_send_amount_sell_deposit_address").html("");
        $("#m_send_amount_sell_qr_code").html("<span style='color: blue'>" + $.t("please_enter_withdrawal_amount") + "</span>");
        $("#m_send_amount_sell_pair").val(pair);
        $("#m_send_amount_sell_done").prop('disabled', true);
    });

    $('#m_send_amount_sell_withdrawal_amount').change(function () {
        var modal = $(this).closest(".modal");
        var amount = $('#m_send_amount_sell_withdrawal_amount').val();
        var pair = $('#m_send_amount_sell_pair').val();
        $("#m_send_amount_sell_done").prop('disabled', true);
        var publicKey = NRS.publicKey;
        if (publicKey === "" && NRS.accountInfo) {
            publicKey = NRS.accountInfo.publicKey;
        }
        if (!publicKey || publicKey === "") {
            NRS.showModalError("Account has no public key, please login using your passphrase", modal);
            return;
        }
        $("#m_send_amount_sell_qr_code").html("<span style='color: blue'>" + $.t("please_enter_withdrawal_amount") + "</span>");
        modal.css('cursor','wait');
        apiCall('sendamount', {
            amount: amount,
            withdrawal: NRS.accountRS,
            pubKey: publicKey,
            pair: pair,
            apiKey: NRS.settings.exchange_api_key
        }, function (data) {
            try {
                var rate = $("#m_send_amount_sell_rate");
                var fee = $("#m_send_amount_sell_fee");
                var depositAmount = $("#m_send_amount_sell_deposit_amount");
                var depositAddress = $("#m_send_amount_sell_deposit_address");
                var expiration = $("#m_send_amount_sell_expiration");
                if (data.error) {
                    rate.val("");
                    fee.val("");
                    depositAmount.val("");
                    depositAddress.html("");
                    expiration.val("");
                    return;
                }
                if (amount !== data.success.withdrawalAmount) {
                    NRS.showModalError("amount returned from shapeshift " + data.success.withdrawalAmount +
                        " differs from requested amount " + amount, modal);
                    return;
                }
                if (NRS.accountRS !== data.success.withdrawal) {
                    NRS.showModalError("withdrawal address returned from shapeshift " + data.success.withdrawal +
                        " differs from requested address " + NRS.accountRS, modal);
                    return;
                }
                modal.find(".error_message").html("").hide();
                rate.val(invert(data.success.quotedRate));
                fee.val(data.success.minerFee);
                depositAmount.val(parseFloat(data.success.depositAmount));
                depositAddress.html(data.success.deposit);
                expiration.val(NRS.formatTimestamp(data.success.expiration, false, true));
                NRS.logConsole("sendamount request done, deposit address " + data.success.deposit);
                NRS.generateQRCode("#m_send_amount_sell_qr_code", "bitcoin:" + data.success.deposit + "?amount=" + data.success.depositAmount);
                $("#m_send_amount_sell_done").prop('disabled', false);
            } finally {
                modal.css('cursor', 'default');
            }
        }, true, modal)
    });

    $("#m_send_amount_sell_done").on("click", function(e) {
        e.preventDefault();
        var pair = $("#m_send_amount_sell_pair").val();
        var deposit = $("#m_send_amount_sell_deposit_address").html();
        if (deposit !== "") {
            addDepositAddress(deposit, pair);
            renderMyExchangesTable();
            $(this).closest(".modal").modal("hide");
        }
    });

    $("#m_send_amount_sell_cancel").on("click", function(e) {
        e.preventDefault();
        var deposit = $("#m_send_amount_sell_deposit_address").html();
        if (deposit !== "") {
            apiCall('cancelpending', {address: deposit}, function (data) {
                var msg = data.success ? data.success : data.err;
                NRS.logConsole("sell cancelled response: " + msg);
            })
        }
    });

	return NRS;
}(NRS || {}, jQuery));