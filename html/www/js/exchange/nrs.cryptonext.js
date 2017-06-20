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

    var key;
    var secret;
    var url;

    function get_signature(nonce, key, secret) {
        var message = nonce + '-' + key;
        var hmac = CryptoJS.algo.HMAC.create(CryptoJS.algo.SHA512, secret);
        hmac.update(message);
        var signature = hmac.finalize();
        return String(signature);
    }

    function __construct($key, $secret) {
        url = 'https://www.cryptonext.net';
        key = $key;
        secret = $secret;
    }

    /**
     * @param $command method/command call for the api to execute
     * @return array All data returned from API call
     * @throws Exception
     */
    function cn_api_query($command) {
        var $req = {};
        var $private = true;
        var $url = url + '/ws.php';
        var postData = "";
        if ($private) {
            $req.nonce = "1";
            $req.key = key;
            $req.signature = get_signature($req.nonce, key, secret);
            $req.command = $command;
            postData = "command=" + $req.command + "&nonce=" + $req.nonce + "&key=" + $req.key + "&signature=" + $req.signature;
        } else {
            $url += '?command=' + $command;
        }
        apiCall($url, postData, 'POST', function(response) {
            console.log(response);
        });
    }

    NRS.getInfo = function() {
        __construct("AK-124-477109-326958", "ee15e37d953de1889ccca25a8dbe3e19");
        cn_api_query("GetInfo");
    };

    var apiCall = function(url, requestData, method, doneCallback) {
        NRS.logConsole("api url: " + url + " ,data: " + requestData + " ,method: " + method);
        $.ajax({
            url: url,
            beforeSend: function(xhr) {
                xhr.setRequestHeader("$key", key);
                xhr.setRequestHeader("Content-Type", "application/json; charset=utf-8");
            },
            crossDomain: true,
            dataType: "json",
            type: method,
            timeout: 30000,
            async: true,
            data: requestData
        }).done(function(response, status) {
            if (status !== "success") {
                console.log(url + ' status ' + status);
                return;
            }
            doneCallback(response);
        }).fail(function (xhr, textStatus, error) {
            var message = "Request failed, url " + url + " method " + method + " status " + textStatus + " error " + error;
            console.log(message);
            throw message;
        })
    };

    NRS.pages.exchange_cryptonext = function() {
        var exchangeDisabled = $("#exchange_disabled");
        var exchangePageHeader = $("#exchange_page_header");
        var exchangePageContent = $("#exchange_page_content");
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
        getInfo();
        NRS.pageLoaded();
    };


	return NRS;
}(Object.assign(NRS || {}, isNode ? global.client : {}), jQuery));

if (isNode) {
    module.exports = NRS;
}