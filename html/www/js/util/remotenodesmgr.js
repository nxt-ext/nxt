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
    this.announcedAddress = peerData.announcedAddress;
    if (peerData.apiSSLPort) {
        this.port = peerData.apiSSLPort;
        this.isSSL = true;
    } else {
        this.port = peerData.apiPort;
        this.isSSL = false;
    }
    this.isCORSSupported = peerData.services instanceof Array && peerData.services.indexOf("CORS") >= 0;


}

RemoteNode.prototype.getUrl = function () {
    return (this.isSSL ? "https://" : "http://") + this.announcedAddress + ":" + this.port;
};

RemoteNode.prototype.sendRequest = function(requestType, data, callback, isAsync) {
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

    }).fail(function (xhr, textStatus, error) {

    });
};

function RemoteNodesManager(isTestnet, isMobileApp) {
    this.isTestnet = isTestnet;
    this.isMobileApp = isMobileApp;
    this.nodes = {};

    this.init();
}

RemoteNodesManager.prototype.addRemoteNodes = function (peersData) {
    $.each(peersData, function(index, peerData) {
        this.nodes[peerData.address] = new RemoteNode(peerData);
    });
};

RemoteNodesManager.prototype.getRandomNode = function () {
    var addresses = Object.keys(this.nodes);
    return this.nodes[addresses[Math.floor((Math.random() * addresses.length))]];
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
