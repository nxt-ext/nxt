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

function RemoteNode(peerData) {
    this.address = peerData.address;
    this.announcedAddress = peerData.announcedAddress;
    /*if (peerData.apiSSLPort) {
        this.port = peerData.apiSSLPort;
        this.isSSL = true;
    } else*/ {
        this.port = peerData.apiPort;
        this.isSSL = false;
    }
    //this.isCORSSupported = peerData.services instanceof Array && peerData.services.indexOf("CORS") >= 0;
    this.blacklistedUntil = 0;
}

RemoteNode.prototype.getUrl = function () {
    return (this.isSSL ? "https://" : "http://") + this.address + ":" + this.port;
};

RemoteNode.prototype.sendRequest = function(requestType, data, callback, isAsync) {
    var node = this;
    var url = this.getUrl();
    url += "/nxt?requestType=" + requestType;

    $.ajax({
        url: url,
        crossDomain: true,
        dataType: "json",
        type: "GET",
        timeout: 30000,
        async: (typeof isAsync == 'undefined' ? true : isAsync),
        traditional: true,
        data: data
    }).done(function (response) {
        callback.call(node, response);
    }).fail(function (xhr, textStatus, error) {
        callback.call(node, {
            "errorCode": -1,
            "errorDescription": error
        });
        node.blacklist();
    });
};

RemoteNode.prototype.isBlacklisted = function () {
    return new Date().getTime() < this.blacklistedUntil;
};

RemoteNode.prototype.blacklist = function () {
    this.blacklistedUntil = new Date().getTime() + 10 * 60 * 1000;
};

function RemoteNodesManager(isTestnet, isMobileApp) {
    this.isTestnet = isTestnet;
    this.isMobileApp = isMobileApp;
    this.nodes = {};

    this.init();
}

RemoteNodesManager.prototype.addRemoteNodes = function (peersData) {
    var mgr = this;
    $.each(peersData, function(index, peerData) {
        if (peerData.services instanceof Array && (peerData.services.indexOf("API") >= 0)) {
            if (this.isMobileApp || true || peerData.services.indexOf("CORS") >= 0) {
                var oldNode = mgr.nodes[peerData.address];
                var newNode = new RemoteNode(peerData);
                if (oldNode) {
                    newNode.blacklistedUntil = oldNode.blacklistedUntil;
                }
                mgr.nodes[peerData.address] = newNode;
            }
        }
    });
};

RemoteNodesManager.prototype.setRemoteNodes = function (peersData) {
    this.nodes = {};
    this.addRemoteNodes(peersData);
};

RemoteNodesManager.prototype.getRandomNode = function (ignoredAddresses) {
    var addresses = Object.keys(this.nodes);
    var index = Math.floor((Math.random() * addresses.length));
    var startIndex = index;
    var node;
    do {
        var address = addresses[index];
        if (ignoredAddresses instanceof Array && ignoredAddresses.indexOf(address) >= 0) {
            node = null;
        } else {
            node = this.nodes[address];
            if (node.isBlacklisted()) {
                node = null;
            }
        }
        index = (index+1) % addresses.length;
    } while(node == null && index != startIndex);

    return node;
};

RemoteNodesManager.prototype.getRandomNodes = function (count, ignoredAddresses) {
    var processedAddresses = [];
    if (ignoredAddresses instanceof Array) {
        processedAddresses.concat(ignoredAddresses)
    }

    var result = [];
    for (var i = 0; i < count; i++) {
        var node = this.getRandomNode(processedAddresses);
        if (node) {
            processedAddresses.push(node.address);
            result.push(node);
        }
    }
    return result;
};

RemoteNodesManager.prototype.findMoreNodes = function () {
    var nodesMgr = this;
    var node = this.getRandomNode();
    var data = {state: "CONNECTED", includePeerInfo: true};
    node.sendRequest("getPeers", data, function (response) {
        if (response.peers) {
            nodesMgr.addRemoteNodes(response.peers);
        }
        setTimeout(function () {
            nodesMgr.findMoreNodes();
        }, 30000);
    });
};

RemoteNodesManager.prototype.init = function () {
    if (this.isMobileApp) {
        //load the remote nodes bootstrap file only for mobile wallet
        jQuery.ajaxSetup({ async: false });
        $.getScript(this.isTestnet ? "js/data/remotenodesbootstrap.testnet.js" : "js/data/remotenodesbootstrap.mainnet.js");
        jQuery.ajaxSetup({async: true});

        this.addRemoteNodes(this.REMOTE_NODES_BOOTSTRAP);
        this.findMoreNodes();
    }
};
