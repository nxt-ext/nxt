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
var NRS = (function(NRS, $) {
    var testnetApiNodes = {"peers":["95.183.52.46","107.170.3.62"]};
    var testnetApiSSLNodes = {"peers":["174.140.168.136","178.150.207.53"]};
    var mainnetApiNodes = {"peers":["212.47.228.107","163.172.159.234","52.20.107.161","80.112.151.134","95.29.206.31","174.140.168.163","163.172.166.207","178.79.128.235","80.150.243.88","78.94.2.74","212.47.243.151","163.172.214.102","5.135.181.38","80.150.243.97","80.150.243.98","163.172.25.123","128.199.120.129","212.47.243.70","148.251.57.155","163.172.160.35","80.150.243.13","163.172.161.7","136.243.249.132","23.92.65.249","163.172.137.61","37.97.136.254","91.116.242.163","192.241.251.114","69.163.47.173","175.156.97.96","46.101.137.39","85.214.243.229","78.244.125.22","163.172.157.87"]};
    var mainnetApiSSLNodes = {"peers":["212.47.228.107","163.172.159.234","52.20.107.161","80.42.27.239","174.140.168.163","163.172.166.207","78.46.233.109","78.94.2.74","31.128.67.247","163.172.25.123","128.199.120.129","212.47.243.70","163.172.160.35","163.172.161.7","163.172.137.61","188.226.174.169","162.243.145.83","91.116.242.163","192.241.251.114","163.172.157.87"]};

    NRS.initRemoteNodesMgr = function(isTestnet, isMobileApp) {
        NRS.remoteNodesMgr = new RemoteNodesManager(isTestnet, isMobileApp);
    };

    NRS.getRandomNodeUrl = function(isTestNet, isSSL) {
    	var nodes;
    	if (isTestNet && isSSL) {
    		nodes = testnetApiSSLNodes.peers;
		} else if (isTestNet) {
			nodes = testnetApiNodes.peers;
		} else if (isSSL) {
			nodes = mainnetApiSSLNodes.peers;
		} else {
			nodes = mainnetApiNodes.peers;
		}
		var protocol = isSSL ? "https://" : "http://";
		var port = isTestNet ? 6876 : 7876;
		var address = nodes[Math.floor((Math.random() * nodes.length))];
		return protocol + address + ":" + port;
	};

    NRS.requestNeedsConfirmation = function (requestType) {
        var plusIndex = requestType.indexOf("+");
        if (plusIndex > 0) {
            requestType = requestType.substring(0, plusIndex);
        }
        return !NRS.isRequirePost(requestType) && NRS.isRequestForwardable(requestType)
    };

    NRS.confirmRequest = function(url, requestType, postData) {

    };

	return NRS;
}(NRS || {}, jQuery));