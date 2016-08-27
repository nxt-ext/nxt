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

    NRS.initRemoteNodesMgr = function(isTestnet, isMobileApp) {
        NRS.remoteNodesMgr = new RemoteNodesManager(isTestnet, isMobileApp);
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