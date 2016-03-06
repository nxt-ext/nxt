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

    NRS.storageSelect = function (table, query, callback) {
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
                Object.keys(query[j]).forEach(function(key) {
                    if (items[i][key] == query[j][key]) {
                        response.push(items[i]);
                    }
                })
            }
        }
        callback(null, response);
    };

    NRS.storageInsert = function(table, key, data, callback, isAutoIncrement) {
        if (NRS.databaseSupport) {
            return NRS.database.insert(table, data, callback);
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

        if ($.isArray(data)) {
            for (i = 0; i < data.length; i++) {
                insertItem(data[i]);
            }
        } else {
            insertItem(data);
        }
        NRS.setJSONItem(table, items);
        callback(null, items);

        function insertItem(item) {
            if (!isAutoIncrement) {
                items.push(item);
                return;
            }
            if (item.id) {
                callback("Cannot use auto increment id since data already contains id value", []);
                return;
            }
            if (items.length == 0) {
                item.id = 1;
            } else {
                item.id = items[items.length - 1].id + 1;
            }
            items.push(item);
        }
    };

    NRS.storageUpdate = function (table, data, query, callback) {
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
                Object.keys(query[j]).forEach(function(key) {
                    if (items[i][key] == query[j][key]) {
                        var id = items[i].id;
                        items[i] = data;
                        if (id && !data.id) {
                            items[i].id = id;
                        }
                    }
                });
            }
        }
        NRS.setJSONItem(table, items);
        callback(null, items);
    };

    NRS.storageDelete = function (table, query, callback) {
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
                Object.keys(query[j]).forEach(function(key) {
                    if (items[i][key] == query[j][key]) {
                        items.splice(i, 1);
                    }
                })
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