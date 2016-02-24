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

    NRS.storageSelect = function(table, key, query, callback) {
        if (NRS.databaseSupport) {
            NRS.database.select(table, query, callback);
            return;
        }
        var items = NRS.getJSONItem(table);
        if (!items) {
            callback("No items to select", []);
            return;
        }
        var response = [];
        for (var i=0; i<items.length; i++) {
            if (!query) {
                response.push(items[i]);
                continue;
            }
            for (var j=0; j<query.length; j++) {
                if (items[i][key] == query[j][key]) {
                    response.push(items[i]);
                }
            }
        }
        callback(null, response);
    };

    NRS.storageInsert = function(table, key, data, callback) {
        if (NRS.databaseSupport) {
            return NRS.database.insert(items, data, callback);
        }
        var items = NRS.getJSONItem(table);
        if (!items) {
            items = [];
        }
        for (var i=0; i<items.length; i++) {
            for (var j=0; j<data.length; j++) {
                if (items[i][key] == data[j][key]) {
                    callback("Key already exists: " + items[i][key], []);
                    return;
                }
            }
        }
        for (i=0; i<data.length; i++) {
            items.push(data[i]);
        }
        NRS.setJSONItem(table, items);
        callback(null, items);
    };

    NRS.storageUpdate = function(table, key, data, query, callback) {
        if (NRS.databaseSupport) {
            return NRS.database.update(table, data, query, callback);
        }
        var items = NRS.getJSONItem(table);
        if (!items) {
            callback("No items to update", []);
            return;
        }
        if (!query) {
            callback("No update query", []);
            return;
        }
        for (var i=0; i<items.length; i++) {
            for (var j=0; j<query.length; j++) {
                if (items[i][key] == query[j][key]) {
                    items[i] = data[0];
                }
            }
        }
        NRS.setJSONItem(table, items);
        callback(null, items);
    };

    NRS.storageDelete = function(table, key, query, callback) {
        if (NRS.databaseSupport) {
            return NRS.database.delete(table, query, callback);
        }
        var items = NRS.getJSONItem(table);
        if (!items) {
            callback("No items to delete", []);
            return;
        }
        for (var i=0; i<items.length; i++) {
            for (var j=0; j<query.length; j++) {
                if (items[i][key] == query[j][key]) {
                    items.splice(i, 1);
                }
            }
        }
        NRS.setJSONItem(table, items);
        callback(null, items);
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