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
    if (peerData.apiSSLPort) {
        this.port = peerData.apiSSLPort;
        this.isSSL = true;
    } else {
        this.port = peerData.apiPort;
        this.isSSL = false;
    }
    this.isCORSSupported = peerData.services instanceof Array && peerData.services.indexOf("CORS") >= 0;
    this.blacklistedUntil = 0;
}

RemoteNode.prototype.getUrl = function () {
    return (this.isSSL ? "https://" : "http://") + this.address + ":" + this.port;
};

RemoteNode.prototype.sendRequest = function(requestType, data, callback, isAsync) {
    var node = this;
    var url = this.getUrl();
    url += "?requestType=" + requestType;

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
        callback(response);
    }).fail(function (xhr, textStatus, error) {
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
        mgr.nodes[peerData.address] = new RemoteNode(peerData);
    });
};

RemoteNodesManager.prototype.getRandomNode = function () {
    var addresses = Object.keys(this.nodes);
    var index = Math.floor((Math.random() * addresses.length));
    var node;
    do {
        node = this.nodes[addresses[index]];
        index = (index+1) % addresses.length;
    } while(node.isBlacklisted());
    return node;
};

RemoteNodesManager.prototype.findMoreNodes = function () {
    var nodesMgr = this;
    var node = this.getRandomNode();
    var data = {active: true, includePeerInfo: true};
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
        $.getScript(this.isTestnet ? "js/util/remotenodesbootstrap.testnet.js" : "js/util/remotenodesbootstrap.mainnet.js");
        jQuery.ajaxSetup({async: true});

        this.addRemoteNodes(this.REMOTE_NODES_BOOTSTRAP);
        this.findMoreNodes();
    }
};
