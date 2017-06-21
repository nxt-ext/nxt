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

    NRS.invert = function(rate) {
        return Math.round(100000000 / parseFloat(rate)) / 100000000;
    };

    NRS.getCoins = function() {
        var coins = [];
        for (var i=0; i<3; i++) {
            coins.push(NRS.settings["exchange_coin" + i]);
        }
        return coins;
    };

    NRS.setCoins = function(coins) {
        for (var i=0; i<coins.length; i++) {
            NRS.updateSettings("exchange_coin" + i, coins[i]);
        }
    };


    NRS.addDepositAddress = function(address, from, to, key) {
        var json = localStorage[key];
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
        localStorage[key] = JSON.stringify(addresses);
    };

    NRS.getExchangeAddressLink = function (address, coin) {
        if (coin.toUpperCase() === "NXT") {
            return NRS.getAccountLink({ accountRS: address }, "account");
        }
        if (coin.toUpperCase() === "BTC") {
            return "<a target='_blank' href='https://blockchain.info/address/" + address + "'>" + address + "</a>";
        }
        return address;
    };

    NRS.getExchangeTransactionLink = function (transaction, coin) {
        if (coin.toUpperCase() === "NXT") {
            return "<a href='#' class='show_transaction_modal_action' data-transaction='" + transaction + "'>" + transaction + "</a>";
        }
        if (coin.toUpperCase() === "BTC") {
            return "<a target='_blank' href='https://blockchain.info/tx/" + transaction + "'>" + transaction + "</a>";
        }
        return transaction;
    };

    return NRS;
}(NRS || {}, jQuery));

