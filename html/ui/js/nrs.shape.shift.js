/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $) {

    var API_KEY = "773ecd081abd54e760a45b3551bbd4d725cf788590619e3f4bdeb81d01994d1dcad8a1d35771f669cfa47742af38e2207e297bc0eeeaea733853c2235548fba3";
    var SHAPE_SHIFT_URL = "https://cors.shapeshift.io/";
    var DEF_CURRENCIES = ["BTC", "LTC", "DOGE"];
    var SUPPORTED_CURRENCIES = {};

    var currencyToPair = function (op, currency) {
        return (op == "buy") ? "NXT_" + currency : currency + "_NXT";
    };

    var pairToCurrency = function (pair) {
        if (pair.indexOf("NXT_") == 0) {
            return pair.substring("NXT_".length);
        }
        if (pair.indexOf("_NXT") == pair.length - "_NXT".length) {
            return pair.substring(0, pair.indexOf("_NXT"));
        }
        throw "illegal pair " + pair;
    };

    var reversePair = function (pair) {
        var pairParts = pair.split('_');
        return pairParts[1] + '_' + pairParts[0];
    };

    var formatTimestamp = function (timestamp) {
        var date = new Date(timestamp);
        var d = date.getDate();
        var dd = d < 10 ? '0' + d : d;
        var M = date.getMonth() + 1;
        var MM = M < 10 ? '0' + M : M;
        var yyyy = date.getFullYear();

        var res = 'dd/MM/yyyy'
            .replace(/dd/g, dd)
            .replace(/MM/g, MM)
            .replace(/yyyy/g, yyyy);

        var hours = date.getHours();
        var minutes = date.getMinutes();
        if (minutes < 10) {
            minutes = "0" + minutes;
        }
        var seconds = date.getSeconds();
        if (seconds < 10) {
            seconds = "0" + seconds;
        }
        res += " " + hours + ":" + minutes + ":" + seconds;
        return res;
    };

    var getCurrencies = function() {
        // TODO add to settings
        return DEF_CURRENCIES;
    };

    var setCurrencies = function(currencies) {
        localStorage["shapeshift.currencies." + NRS.accountRS] = JSON.stringify(currencies);
    };

    var addDepositAddress = function(address, pair) {
        var json = localStorage["shapeshift.depositAddresses." + NRS.accountRS];
        var addresses;
        if (json == undefined) {
            addresses = [];
        } else {
            addresses = JSON.parse(json);
            if (addresses.length > 5) {
                addresses.splice(5, addresses.length - 5);
            }
        }
        addresses.splice(0, 0, { address: address, pair: pair, time: Date.now() });
        console.log("deposit address " + address + " pair " + pair + " added");
        localStorage["shapeshift.depositAddresses." + NRS.accountRS] = JSON.stringify(addresses);
    };

    var apiCall = function(action, requestData, method, doneCallback, ignoreError, modal) {
        $.ajax({
            url: SHAPE_SHIFT_URL + action,
            crossDomain: true,
            dataType: "json",
            type: method,
            timeout: 30000,
            async: true,
            data: requestData
        }).done(function(response, status) {
            if (status != "success") {
                console.log(action + ' status ' + status);
                if (modal) {
                    NRS.showModalError(status, modal);
                }
            }
            if (response.error) {
                var error = response.error;
                var msg;
                if (error.code) {
                    msg = ' code ' + error.code + ' errno ' + error.errno + ' syscall ' + error.syscall;
                    console.log(action + msg);
                } else {
                    msg = error;
                    console.log(action + ' error ' + error);
                }
                if (!ignoreError) {
                    return;
                }
                if (modal) {
                    NRS.showModalError(msg, modal);
                }
            }
            doneCallback(response);
        }).fail(function (xhr, textStatus, error) {
            var message = "Request failed, action " + action + " method " + method + " status " + textStatus + " error " + error;
            console.log(message);
            throw message;
        })
    };

    function invert(rate) {
        return Math.round(100000000 / parseFloat(rate)) / 100000000;
    }

    var renderExchangeTable = function (op) {
        var currencies = getCurrencies();
        var tasks = [];
        for (var i = 0; i < currencies.length; i++) {
            tasks.push((function (i) {
                return function (callback) {
                    console.log("marketinfo iteration " + i);
                    var pair = currencyToPair(op, currencies[i]);
                    var counterPair = reversePair(pair);
                    console.log("counterPair " + counterPair);
                    apiCall("marketinfo/" + pair, {}, "GET", function(data) {
                        var row = "";
                        row += "<tr>";
                        row += "<td>" + SUPPORTED_CURRENCIES[currencies[i]].name + " " +
                            "<img src='" + SUPPORTED_CURRENCIES[currencies[i]].image + "' width='16px' height='16px'/>" +
                        "</td>";
                        row += "<td>" + currencies[i] + "</td>";
                        var rate;
                        if (op == "sell") {
                            if (parseFloat(data.rate) == 0) {
                                rate = "N/A";
                            } else {
                                rate = invert(data.rate);
                            }
                        } else {
                            rate = data.rate;
                        }
                        row += "<td>" + String(rate).escapeHTML() + "</td>";
                        row += "<td><a href='#' class='btn btn-xs btn-default' data-toggle='modal' data-target='#m_shape_shift_" + op + "_modal' " +
                            "data-pair='" + pair + "' data-rate='" + data.rate + "' data-min='" + data.minimum + "' data-max='" + data.limit +
                            "' data-fee='" + data.minerFee + "'>Shift</a>";
                        row += "<a href='#' class='btn btn-xs btn-default' data-toggle='modal' data-target='#m_send_amount_" + op + "_modal' " +
                            "data-pair='" + pair + "' data-rate='" + data.rate + "' data-min='" + data.minimum + "' data-max='" + data.limit +
                            "' data-fee='" + data.minerFee + "'>Pay</a></td>";
                        row += "</tr>";
                        console.log(row);
                        callback(null, row);
                    });
                }
            })(i));
        }
        console.log(tasks.length + " tasks ready to run");
        async.series(tasks, function (err, results) {
            var table = $("#p_shape_shift_" + op + "_nxt");
            if (err) {
                console.log("Err: ", err, "\nResults:", results);
                table.find("tbody").empty();
                NRS.dataLoadFinished(table);
                return;
            }
            console.log("results", results);
            var rows = "";
            for (i = 0; i < results.length; i++) {
                rows += results[i];
            }
            console.log("rows " + rows);
            table.find("tbody").empty().append(rows);
            NRS.dataLoadFinished(table);
        });
    };

    var getAddressLink = function (address, currency) {
        if (currency == "NXT") {
            return NRS.getAccountLink({ accountRS: address }, "account");
        }
        if (currency == "BTC") {
            return "<a target='_blank' href='https://blockchain.info/address/" + address + "'>" + address + "</a>";
        }
        return address;
    };

    var getTransactionLink = function (transaction, currency) {
        if (currency == "NXT") {
            return "<a href='#' class='show_transaction_modal_action' data-transaction='" + transaction + "'>" + transaction + "</a>";
        }
        if (currency == "BTC") {
            return "<a target='_blank' href='https://blockchain.info/tx/" + transaction + "'>" + transaction + "</a>";
        }
        return transaction;
    };

    var renderMyExchangesTable = function () {
        var depositAddressesJSON = localStorage["shapeshift.depositAddresses." + NRS.accountRS];
        var depositAddresses = [];
        if (depositAddressesJSON) {
            depositAddresses = JSON.parse(depositAddressesJSON);
        }
        var tasks = [];
        var empty = "<td></td>";
        for (var i = 0; i < depositAddresses.length; i++) {
            tasks.push((function (i) {
                return function (callback) {
                    console.log("txStat iteration " + i);
                    apiCall("txStat/" + depositAddresses[i].address, {}, "GET", function(data) {
                        var row = "";
                        row += "<tr>";
                        row += "<td>" + formatTimestamp(depositAddresses[i].time) + "</td>";
                        row += "<td>" + data.status + "</td>";
                        if (data.status == "failed") {
                            row += "<td>" + data.error + "</td>";
                            row += empty + empty + empty + empty + empty + empty;
                            console.log(row);
                            callback(null, row);
                            return;
                        }
                        row += "<td>" + getAddressLink(data.address, depositAddresses[i].pair.split('_')[0]) + "</td>";
                        if (data.status == "no_deposits") {
                            row += empty + empty + empty + empty + empty + empty;
                            console.log(row);
                            callback(null, row);
                            return;
                        }
                        row += "<td>" + data.incomingCoin + "</td>";
                        row += "<td>" + data.incomingType + "</td>";
                        if (data.status == "received") {
                            row += empty + empty + empty + empty;
                            console.log(row);
                            callback(null, row);
                            return;
                        }
                        row += "<td>" + getAddressLink(data.withdraw, depositAddresses[i].pair.split('_')[1]) + "</td>";
                        row += "<td>" + data.outgoingCoin + "</td>";
                        row += "<td>" + data.outgoingType + "</td>";
                        row += "<td>" + getTransactionLink(data.transaction, depositAddresses[i].pair.split('_')[1]) + "</td>";
                        console.log(row);
                        callback(null, row);
                    }, true);
                }
            })(i));
        }
        console.log(tasks.length + " tasks ready to run");
        var table = $("#p_shape_shift_my_table");
        if (tasks.length == 0) {
            table.find("tbody").empty();
            NRS.dataLoadFinished(table);
        }
        async.series(tasks, function (err, results) {
            if (err) {
                console.log("Err: ", err, "\nResults:", results);
                table.find("tbody").empty();
                NRS.dataLoadFinished(table);
                return;
            }
            console.log("results", results);
            var rows = "";
            for (i = 0; i < results.length; i++) {
                rows += results[i];
            }
            console.log("rows " + rows);
            table.find("tbody").empty().append(rows);
            NRS.dataLoadFinished(table);
        });
    };

    function renderRecentTable() {
        apiCall('recenttx/50', {}, 'GET', function (data) {
            console.log("recent");
            var rows = "";
            if (data) {
                for (var i = 0; i < data.length; i++) {
                    var transaction = data[i];
                    if (String(transaction.curIn).escapeHTML() != "NXT" && String(transaction.curOut).escapeHTML() != "NXT") {
                        continue;
                    }
                    rows += "<tr>";
                    rows += "<td>" + String(transaction.curIn).escapeHTML() + "</td>";
                    rows += "<td>" + String(transaction.curOut).escapeHTML() + "</td>";
                    rows += "<td>" + formatTimestamp(1000 * transaction.timestamp) + "</td>";
                    rows += "<td>" + transaction.amount + "</td>";
                    rows += "</tr>";
                }
            }
            console.log("recent rows " + rows);
            var table = $("#p_shape_shift_table");
            table.find("tbody").empty().append(rows);
            NRS.dataLoadFinished(table);
        });
    }

    function renderNxtLimit() {
        apiCall('limit/nxt_btc', {}, 'GET', function (data) {
            console.log("limit1 " + data.limit);
            $('#shape_shift_nxt_avail').html(String(data.limit).escapeHTML());
        });
    }

    NRS.pages.exchange = function() {
        NRS.pageLoading();
        renderNxtLimit();
        renderExchangeTable("buy");
        renderExchangeTable("sell");
        renderMyExchangesTable();
        renderRecentTable();
        NRS.pageLoaded();
        setTimeout(NRS.pages.p_shape_shift, 60000);
    };

    $('.currency-select').change(function() {
        var id = $(this).attr('id');
        var currencies = getCurrencies();
        currencies[parseInt(id.slice(-1))] = $(this).val();
        setCurrencies(currencies);
        renderExchangeTable('buy');
        renderExchangeTable('sell');
    });

	NRS.setup.exchange = function() {
        var select = [];
        select.push($('#shape_shift_currency_0'));
        select.push($('#shape_shift_currency_1'));
        select.push($('#shape_shift_currency_2'));
      	apiCall('getcoins', {}, 'GET', function(data) {
            var selectedCurrencies = getCurrencies();
            SUPPORTED_CURRENCIES = data;
            for (var i=0; i<select.length; i++) {
                select[i].empty();
                $.each(data, function(code, currency) {
                    if (code != 'NXT' && currency['status'] == 'available') {
                        select[i].append('<option value="' + code + '">' + currency['name'] + '</option>');
                        SUPPORTED_CURRENCIES[code] = currency;
                    }
                });
                select[i].val(selectedCurrencies[i]);
            }
        });
	};

    $("#m_shape_shift_buy_modal").on("show.bs.modal", function (e) {
        var invoker = $(e.relatedTarget);
        var pair = invoker.data("pair");
        $("#m_shape_shift_buy_pair").val(pair);
        var currency = pairToCurrency(pair);
        console.log("modal invoked pair " + pair + " currency " + currency);
        $("#m_shape_shift_buy_title").html("Exchange NXT to " + currency);
        $("#m_shape_shift_buy_min").val(invoker.data("min"));
        $("#m_shape_shift_buy_min_currency").html("NXT");
        $("#m_shape_shift_buy_max").val(invoker.data("max"));
        $("#m_shape_shift_buy_max_currency").html("NXT");
        $("#m_shape_shift_buy_rate").val(invoker.data("rate"));
        $("#m_shape_shift_buy_rate_text").html(currency + " per 1 NXT");
        $("#m_shape_shift_withdrawal_address_currency").html(currency);
        $("#m_shape_shift_buy_fee").val(invoker.data("fee"));
        $("#m_shape_shift_buy_fee_currency").html(currency);
    });

    $("#m_shape_shift_buy_submit").on("click", function(e) {
        e.preventDefault();
        var modal = $(this).closest(".modal");
        var amountNQT = NRS.convertToNQT($("#m_shape_shift_buy_amount").val());
        var withdrawal = $("#m_shape_shift_buy_withdrawal_address").val();
        var pair = $("#m_shape_shift_buy_pair").val();
        console.log('shift withdrawal ' + withdrawal + " pair " + pair);
        apiCall('shift', {
            withdrawal: withdrawal,
            pair: pair,
            returnAddress: NRS.accountRS,
            apiKey: API_KEY
        }, 'POST', function (data) {
            console.log("shift response");
            var msg;
            if (data.error) {
                return;
            }
            if (data.depositType != "NXT") {
                msg = "incorrect deposit currency " + data.depositType;
                console.log(msg);
                NRS.showModalError(msg, modal);
                return;
            }
            if (data.withdrawalType != pairToCurrency(pair)) {
                msg = "incorrect withdrawal currency " + data.withdrawalType;
                console.log(msg);
                NRS.showModalError(msg, modal);
                return;
            }
            if (data.withdrawal != withdrawal) {
                msg = "incorrect withdrawal address " + data.withdrawal;
                console.log(msg);
                NRS.showModalError(msg, modal);
                return;
            }
            console.log("shift request done, deposit address " + data.deposit);
            NRS.sendRequest("sendMoney", {
                "recipient": data.deposit,
                "amountNQT": amountNQT,
                "secretPhrase": $("#m_shape_shift_buy_passpharse").val(),
                "deadline": 1440,
                "feeNQT": NRS.convertToNQT(1)
            }, function (response) {
                if (response.errorCode) {
                    console.log("sendMoney response " + response.errorCode + " " + response.errorDescription);
                    NRS.showModalError(NRS.translateServerError(response), modal);
                    return;
                }
                addDepositAddress(data.deposit, pair);
                renderMyExchangesTable();
                $("#m_shape_shift_buy_passpharse").val("");
                modal.modal("hide");
            })
        }, true, modal);
    });

    $("#m_send_amount_buy_modal").on("show.bs.modal", function (e) {
        var invoker = $(e.relatedTarget);
        var pair = invoker.data("pair");
        var currency = pairToCurrency(pair);
        console.log("modal invoked pair " + pair + " currency " + currency);
        $("#m_send_amount_buy_title").html("Pay " + currency + " with NXT");
        $("#m_send_amount_buy_withdrawal_amount_currency").html(currency);
        $("#m_send_amount_buy_rate_text").html(currency + " per 1 NXT");
        $("#m_send_amount_withdrawal_address_currency").html(currency + " address");
        $("#m_send_amount_buy_fee_currency").html(currency);
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
        if (amount == "" || withdrawal == "") {
            return;
        }
        apiCall('sendamount', {
            amount: amount,
            withdrawal: withdrawal,
            pair: pair,
            apiKey: API_KEY
        }, "POST", function(data) {
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
            if (amount != data.success.withdrawalAmount) {
                NRS.showModalError("amount returned from shapeshift " + data.success.withdrawalAmount +
                    " differs from requested amount " + amount, modal);
                return;
            }
            if (withdrawal != data.success.withdrawal) {
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
            expiration.val(formatTimestamp(data.success.expiration));
            buySubmit.prop('disabled', false);
        }, true, modal)
    });

    $("#m_send_amount_buy_submit").on("click", function(e) {
        e.preventDefault();
        var modal = $(this).closest(".modal");
        var pair = $("#m_send_amount_buy_pair").val();
        var depositAddress = $("#m_send_amount_buy_deposit_address").val();
        console.log("pay request submitted, deposit address " + depositAddress);
        var amountNQT = NRS.convertToNQT($("#m_send_amount_buy_deposit_amount").val());
        NRS.sendRequest("sendMoney", {
            "recipient": depositAddress,
            "amountNQT": amountNQT,
            "secretPhrase": $("#m_send_amount_buy_passpharse").val(),
            "deadline": 1440,
            "feeNQT": NRS.convertToNQT(1)
        }, function (response) {
            if (response.errorCode) {
                console.log('sendMoney error ' + response.errorDescription);
                NRS.showModalError(response.errorDescription, modal);
                return;
            }
            addDepositAddress(depositAddress, pair);
            renderMyExchangesTable();
            $("#m_send_amount_buy_passpharse").val("");
            modal.modal("hide");
        });
    });

    $("#m_shape_shift_sell_modal").on("show.bs.modal", function (e) {
        var invoker = $(e.relatedTarget);
        var modal = $(this).closest(".modal");
        var pair = invoker.data("pair");
        var currency = pairToCurrency(pair);
        console.log("modal invoked pair " + pair + " currency " + currency);
        $("#m_shape_shift_sell_title").html("Exchange " + currency + " to NXT");
        $("#m_shape_shift_sell_min").val(invoker.data("min"));
        $("#m_shape_shift_sell_min_currency").html(currency);
        $("#m_shape_shift_sell_max").val(invoker.data("max"));
        $("#m_shape_shift_sell_max_currency").html(currency);
        $("#m_shape_shift_sell_rate").val(invoker.data("rate"));
        $("#m_shape_shift_sell_rate_text").html("NXT per 1 " + currency);
        $("#m_shape_shift_sell_fee").val(invoker.data("fee"));
        $("#m_shape_shift_sell_fee_currency").html("NXT");
        $("#m_shape_shift_sell_pair").val(pair);
        var publicKey = NRS.publicKey;
        if (publicKey == "" && NRS.accountInfo) {
            publicKey = NRS.accountInfo.publicKey;
        }
        if (!publicKey || publicKey == "") {
            NRS.showModalError("Account has no public key, please login using your passphrase", modal);
            return;
        }
        apiCall('shift', { withdrawal: NRS.accountRS, rsAddress: publicKey, pair: pair, apiKey: API_KEY }, "POST", function (data) {
            console.log("shift request done");
            var msg;
            if (data.depositType != currency) {
                msg = "incorrect deposit currency " + data.depositType;
                console.log(msg);
                NRS.showModalError(msg, modal);
                return;
            }
            if (data.withdrawalType != "NXT") {
                msg = "incorrect withdrawal currency " + data.withdrawalType;
                console.log(msg);
                NRS.showModalError(msg, modal);
                return;
            }
            if (data.withdrawal != NRS.accountRS) {
                msg = "incorrect withdrawal address " + data.withdrawal;
                console.log(msg);
                NRS.showModalError(msg, modal);
                return;
            }
            console.log("shift request done, deposit address " + data.deposit);
            $("#m_shape_shift_sell_deposit_address").html(data.deposit);
            $("#m_shape_shift_sell_qr_code").empty().qrcode({
                "text": data.deposit,
                "width": 128,
                "height": 128
            });
        })
    });

    $("#m_shape_shift_sell_done").on("click", function(e) {
        e.preventDefault();
        var pair = $("#m_shape_shift_sell_pair").val();
        var deposit = $("#m_shape_shift_sell_deposit_address").html();
        if (deposit != "") {
            addDepositAddress(deposit, pair);
            renderMyExchangesTable();
            $(this).closest(".modal").modal("hide");
        }
    });

    $("#m_shape_shift_sell_cancel").on("click", function(e) {
        e.preventDefault();
        var deposit = $("#m_shape_shift_sell_deposit_address").html();
        if (deposit != "") {
            apiCall('cancelpending', { address: deposit }, 'POST', function(data) {
                var msg = data.success ? data.success : data.err;
                console.log("sell cancelled response: " + msg);
            })
        }
    });

    $("#m_send_amount_sell_modal").on("show.bs.modal", function (e) {
        var invoker = $(e.relatedTarget);
        var modal = $(this).closest(".modal");
        var pair = invoker.data("pair");
        var currency = pairToCurrency(pair);
        console.log("modal invoked pair " + pair + " currency " + currency);
        $("#m_send_amount_sell_title").html("Pay NXT with " + currency);
        $("#m_send_amount_sell_rate_text").html(currency + " per 1 NXT");
        $("#m_send_amount_sell_fee_currency").html("NXT");
        $("#m_send_amount_sell_withdrawal_amount_currency").html("NXT");
        $("#m_send_amount_sell_deposit_amount_currency").html(currency);
        $("#m_send_amount_sell_deposit_address").html("");
        $("#m_send_amount_sell_qr_code").html("");
        $("#m_send_amount_sell_pair").val(pair);
        $("#m_send_amount_sell_done").prop('disabled', true);
    });

    $('#m_send_amount_sell_withdrawal_amount').change(function () {
        var modal = $(this).closest(".modal");
        var amount = $('#m_send_amount_sell_withdrawal_amount').val();
        var pair = $('#m_send_amount_sell_pair').val();
        $("#m_send_amount_sell_done").prop('disabled', true);
        var publicKey = NRS.publicKey;
        if (publicKey == "" && NRS.accountInfo) {
            publicKey = NRS.accountInfo.publicKey;
        }
        if (!publicKey || publicKey == "") {
            NRS.showModalError("Account has no public key, please login using your passphrase", modal);
            return;
        }
        apiCall('sendamount', { amount: amount, withdrawal: NRS.accountRS, rsAddress: publicKey, pair: pair, apiKey: API_KEY }, "POST", function (data) {
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
            if (amount != data.success.withdrawalAmount) {
                NRS.showModalError("amount returned from shapeshift " + data.success.withdrawalAmount +
                    " differs from requested amount " + amount, modal);
                return;
            }
            if (NRS.accountRS != data.success.withdrawal) {
                NRS.showModalError("withdrawal address returned from shapeshift " + data.success.withdrawal +
                    " differs from requested address " + NRS.accountRS, modal);
                return;
            }
            modal.find(".error_message").html("").hide();
            rate.val(invert(data.success.quotedRate));
            fee.val(data.success.minerFee);
            depositAmount.val(parseFloat(data.success.depositAmount));
            depositAddress.html(data.success.deposit);
            expiration.val(formatTimestamp(data.success.expiration));
            console.log("sendamount request done, deposit address " + data.success.deposit);
            $("#m_send_amount_sell_qr_code").empty().qrcode({
                "text": "bitcoin:" + data.success.deposit + "?amount=" + data.success.depositAmount,
                "width": 128,
                "height": 128
            });
            $("#m_send_amount_sell_done").prop('disabled', false);
        }, true, modal)
    });

    $("#m_send_amount_sell_done").on("click", function(e) {
        e.preventDefault();
        var pair = $("#m_send_amount_sell_pair").val();
        var deposit = $("#m_send_amount_sell_deposit_address").html();
        if (deposit != "") {
            addDepositAddress(deposit, pair);
            renderMyExchangesTable();
            $(this).closest(".modal").modal("hide");
        }
    });

    $("#m_send_amount_sell_cancel").on("click", function(e) {
        e.preventDefault();
        var deposit = $("#m_send_amount_sell_deposit_address").html();
        if (deposit != "") {
            apiCall('cancelpending', { address: deposit }, 'POST', function(data) {
                var msg = data.success ? data.success : data.err;
                console.log("sell cancelled response: " + msg);
            })
        }
    });

	return NRS;
}(NRS || {}, jQuery));