/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

/**
 * @depends {nrs.js}
 */
var NRS = (function (NRS) {

    NRS.storageSelect = function(table, query, callback) {
        if (NRS.databaseSupport) {
            NRS.database.select(table, query, callback);
            return;
        }
        var item = NRS.getJSONItem(table);
        var response = [];
        if (!query) {
            for (var key in item) {
                if (!item.hasOwnProperty(key)) {
                    continue;
                }
                var entry = {};
                entry[key] = item[key];
                response.push(entry);
            }
        } else {
            for (var i = 0; i < query.length; i++) {
                if (item[query[i]]) {
                    response.push(item[query[i]]);
                }
            }
        }
        callback(null, response);
    };

    NRS.storageInsert = function(table, data, callback) {
        if (NRS.databaseSupport) {
            return NRS.database.insert(table, data, callback);
        }
        var item = NRS.getJSONItem(table);
        if (!item) {
            item = {};
        }
        for (var i=0; i<data.length; i++) {
            for (var key in data[i]) {
                if (!data[i].hasOwnProperty(key)) {
                    continue;
                }
                item[key] = data[i][key];
            }
        }
        NRS.setJSONItem(table, item);
        callback(null, data);
    };

    NRS.storageUpdate = function(table, data, query, callback) {
        if (NRS.databaseSupport) {
            return NRS.database.update(table, data, query, callback);
        }
        var item = NRS.getJSONItem(table);
        for (var i=0; i<query.length; i++) {
            if (item[query[i]]) {
                item[query[i]] = data;
            }
        }
        NRS.setJSONItem(table, item);
        callback(null, data);
    };

    NRS.storageDelete = function(table, query, callback) {
        if (NRS.databaseSupport) {
            return NRS.database.delete(table, query, callback);
        }
        var item = NRS.getJSONItem(table);
        for (var i=0; i<query.length; i++) {
            for (var key in item) {
                if (!item.hasOwnProperty(key)) {
                    continue;
                }
                if (query[i] == key) {
                    delete item[key];
                }
            }
        }
        NRS.setJSONItem(table, item);
        callback(null);
    };

    NRS.storageDrop = function(table) {
        NRS.removeItem(table);
    };

    NRS.getStrItem = function (key) {
        return localStorage.getItem(key);
    };

    NRS.setStrItem = function (key, data) {
        NRS.logConsole("NRS.setStrItem: key " + key + " data " + data);
        localStorage.setItem(key, data);
    };

    NRS.getJSONItem = function (key) {
        return JSON.parse(localStorage.getItem(key));
    };

    NRS.setJSONItem = function (key, data) {
        var jsonData = JSON.stringify(data);
        NRS.logConsole("NRS.setJSONItem: key " + key + " data " + jsonData);
        localStorage.setItem(key, jsonData);
    };

    NRS.removeItem = function (key) {
        NRS.logConsole("NRS.removeItem: key " + key);
        localStorage.removeItem(key);
    };

    NRS.getAccountJSONItem = function (key) {
        return NRS.getJSONItem(getAccountKey(key));
    };

    NRS.setAccountJSONItem = function (key, data) {
        NRS.setJSONItem(getAccountKey(key), data)
    };

    NRS.removeAccountItem = function (key) {
        NRS.removeItem(getAccountKey(key));
    };

    function getAccountKey(key) {
        if (NRS.account === "") {
            return key;
        }
        return key + "." + NRS.account;
    }

    return NRS;
}(NRS || {}, jQuery));