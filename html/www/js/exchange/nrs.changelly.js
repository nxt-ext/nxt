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
                    NRS.logConsole(method + msg + " params:" + JSON.stringify(params));
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
            }
            doneCallback(response);
        }).fail(function (xhr, textStatus, error) {
            var message = "Request failed, method " + method + " status " + textStatus + " error " + error;
            NRS.logConsole(message);
            throw message;
        })
    };

    var renderExchangeTable = function (op) {
        var coins = NRS.getCoins();
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
                            rate = NRS.invert(response.rate);
                            symbol = coins[i];
                        } else {
                            rate = response.rate;
                            symbol = "NXT";
                        }
                        var row = "<tr><td>" + coins[i] + "</td>";
                        row += "<td><span>" + String(response.minAmount).escapeHTML() + "</span>&nbsp<span>" + symbol + "</span></td>";
                        row += "<td>" + String(rate).escapeHTML() + "</td>";
                        row += "<td><a href='#' class='btn btn-xs btn-default' data-toggle='modal' data-target='#changelly_" + op + "_modal' " +
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
                    apiCall("getTransactions", {address: depositAddresses[i].address}, function(response) {
                        NRS.logConsole("my exchanges iteration " + i + " address " + depositAddresses[i].address);
                        var rows = "";
                        for (var j=0; j < response.result.length; j++) {
                            var transaction = response.result[j];
                            var row = "";
                            row += "<tr>";
                            var date = parseInt(transaction.createdAt) * 1000;
                            row += "<td>" + NRS.formatTimestamp(date, false, true) + "</td>";
                            row += "<td>" + transaction.status + "</td>";
                            row += "<td>" + NRS.getExchangeAddressLink(transaction.payinAddress, transaction.currencyFrom) + "</td>";
                            row += "<td>" + transaction.amountFrom + "</td>";
                            row += "<td>" + transaction.currencyFrom + "</td>";
                            row += "<td>" + NRS.getExchangeAddressLink(transaction.payoutAddress, transaction.currencyTo) + "</td>";
                            row += "<td>" + transaction.amountTo + "</td>";
                            row += "<td>" + transaction.currencyTo + "</td>";
                            var transactionLink;
                            if (transaction.payoutHash) {
                                transactionLink = NRS.getExchangeTransactionLink(transaction.payoutHash, transaction.currencyTo);
                            } else {
                                transactionLink = "N/A";
                            }
                            row += "<td>" + transactionLink + "</td>";
                            row += "<td><a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#changelly_view_transaction' " +
                                "data-id='" + transaction.id + "' data-content='" + JSON.stringify(transaction) + "'>" + $.t("view") + "</a></td>";
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
        var coins = NRS.getCoins();
        coins[parseInt(id.slice(-1))] = $(this).val();
        NRS.setCoins(coins);
        renderExchangeTable('buy');
        renderExchangeTable('sell');
    });

	NRS.setup.exchange = function() {
        // Do not implement connection to a 3rd party site here to prevent privacy leak
    };

    $("#changelly_buy_modal").on("show.bs.modal", function (e) {
        var invoker = $(e.relatedTarget);
        var pair = invoker.data("pair");
        var from = invoker.data("from");
        var to = invoker.data("to");
        var min = invoker.data("min");
        $("#changelly_buy_from").val(from);
        $("#changelly_buy_to").val(to);
        NRS.logConsole("modal invoked from " + from + " to " + to);
        $("#changelly_buy_title").html($.t("exchange_nxt_to_coin", { coin: to }));
        $("#changelly_buy_min").val(invoker.data("min"));
        $("#changelly_buy_min_coin").html("NXT");
        $("#changelly_buy_rate").val(invoker.data("rate"));
        $("#changelly_buy_rate_text").html("NXT/" + to);
        $("#changelly_withdrawal_address_coin").html(to);
    });

    $("#changelly_buy_submit").on("click", function(e) {
        e.preventDefault();
        var modal = $(this).closest(".modal");
        var amountNXT = $("#changelly_buy_amount").val();
        var minAmount = $("#changelly_buy_min").val();
        if (parseFloat(amountNXT) <= parseFloat(minAmount)) {
            msg = "amount is lower tham minimum amount " + minAmount;
            NRS.logConsole(msg);
            NRS.showModalError(msg, modal);
            return;
        }
        var amountNQT = NRS.convertToNQT(amountNXT);
        var withdrawal = $("#changelly_buy_withdrawal_address").val();
        var from = $("#changelly_buy_from").val();
        var to = $("#changelly_buy_to").val();
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
                "secretPhrase": $("#changelly_buy_password").val(),
                "deadline": "1440",
                "feeNQT": NRS.convertToNQT(1)
            }, function (response) {
                if (response.errorCode) {
                    NRS.logConsole("sendMoney response " + response.errorCode + " " + response.errorDescription.escapeHTML());
                    NRS.showModalError(NRS.translateServerError(response), modal);
                    return;
                }
                NRS.addDepositAddress(depositAddress, from, to, DEPOSIT_ADDRESSES_KEY + NRS.accountRS);
                renderMyExchangesTable();
                $("#changelly_buy_passpharse").val("");
                modal.modal("hide");
            })
        }, true, modal);
    });

    $("#changelly_sell_modal").on("show.bs.modal", function (e) {
        var invoker = $(e.relatedTarget);
        var modal = $(this).closest(".modal");
        var from = invoker.data("from");
        var to = invoker.data("to");
        var rate = invoker.data("rate");
        var min = invoker.data("min");
        NRS.logConsole("sell modal exchange from " + from + " to " + to);
        $("#changelly_sell_title").html($.t("exchange_coin_to_nxt_shift", { coin: from }));
        $("#changelly_sell_qr_code").html("");
        $("#changelly_sell_min").val(min);
        $("#changelly_sell_min_coin").html(from);
        $("#changelly_sell_rate").val(rate);
        $("#changelly_sell_rate_text").html(from + "/NXT");
        $("#changelly_sell_from").val(from);
        $("#changelly_sell_to").val(to);
        var publicKey = NRS.publicKey;
        if (publicKey === "" && NRS.accountInfo) {
            publicKey = NRS.accountInfo.publicKey;
        }
        if (!publicKey || publicKey === "") {
            NRS.showModalError("Account has no public key, please login using your passphrase", modal);
            return;
        }
        modal.css('cursor','wait');
        apiCall('generateAddress', {
            from: from,
            to: to,
            address: NRS.accountRS,
            extraId: publicKey
        }, function (data) {
            modal.css('cursor', 'default');
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
            NRS.logConsole(from + " deposit address " + depositAddress);
            $("#changelly_sell_deposit_address").html(depositAddress);
            NRS.generateQRCode("#changelly_sell_qr_code", depositAddress);
        })
    });

    $("#changelly_sell_done").on("click", function(e) {
        e.preventDefault();
        var from = $("#changelly_sell_from").val();
        var to = $("#changelly_sell_to").val();
        var deposit = $("#changelly_sell_deposit_address").html();
        if (deposit !== "") {
            NRS.addDepositAddress(deposit, from, to, DEPOSIT_ADDRESSES_KEY + NRS.accountRS);
            renderMyExchangesTable();
            $(this).closest(".modal").modal("hide");
        }
    });

    $("#changelly_view_transaction").on("show.bs.modal", function(e) {
        var $invoker = $(e.relatedTarget);
        var id = $invoker.data("id");
        var content = $invoker.data("content");
        $("#changelly_identifier").val(id);
        $("#changelly_view_content").val(JSON.stringify(content, null, 2));
    });

    return NRS;
}(NRS || {}, jQuery));