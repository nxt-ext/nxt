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

    var isDesktopApplication = navigator.userAgent.indexOf("JavaFX") >= 0;
    var isPromiseSupported = (typeof Promise !== "undefined" && Promise.toString().indexOf("[native code]") !== -1);
    var isMobileDevice = window["cordova"] !== undefined;
    var remoteNode = null;

    NRS.isIndexedDBSupported = function() {
        return window.indexedDB !== undefined;
    };

    NRS.isExternalLinkVisible = function() {
        // When using JavaFX add a link to a web wallet except on Linux since on Ubuntu it sometimes hangs
        return isDesktopApplication && navigator.userAgent.indexOf("Linux") == -1;
    };

    NRS.isMobileApp = function () {
        return isMobileDevice || NRS.mobileSettings.is_simulate_app;
    };

    NRS.isRequireCors = function () {
        return !isMobileDevice;
    };

    NRS.isPollGetState = function() {
        // When using JavaFX do not poll the server unless it's a working as a proxy
        return !isDesktopApplication || NRS.state && NRS.state.apiProxy;
    };

    NRS.isExportContactsAvailable = function() {
        return !isDesktopApplication; // When using JavaFX you cannot export the contact list
    };

    NRS.isShowDummyCheckbox = function() {
        return isDesktopApplication && navigator.userAgent.indexOf("Linux") >= 0; // Correct rendering problem of checkboxes on Linux
    };

    NRS.isDecodePeerHallmark = function() {
        return isPromiseSupported;
    };

    NRS.getRemoteNodeUrl = function() {
        if (!NRS.isMobileApp()) {
            return "";
        }
        if (remoteNode) {
            return remoteNode.getUrl();
        }
        remoteNode = NRS.remoteNodesMgr.getRandomNode();
        if (remoteNode) {
            var url = remoteNode.getUrl();
            NRS.logConsole("Remote node url: " + url);
            return url;
        } else {
            NRS.logConsole("No available remote nodes, retry bootstrap nodes");
            $.growl($.t("no_available_remote_nodes"));
            NRS.initRemoteNodesMgr(NRS.mobileSettings.is_testnet, true);
            return NRS.getRemoteNodeUrl();
        }
    };

    NRS.getRemoteNode = function () {
        return remoteNode;
    };

    NRS.resetRemoteNode = function(blacklist) {
        if (remoteNode && blacklist) {
            remoteNode.blacklist();
        }
        remoteNode = null;
    };

    NRS.getDownloadLink = function(url, link) {
        if (NRS.isMobileApp()) {
            var script = "NRS.openMobileBrowser(\"" + url + "\");";
            if (link) {
                link.attr("onclick", script);
                return;
            }
            return "<a onclick='" + script +"' class='btn btn-xs btn-default'>" + $.t("download") + "</a>";
        } else {
            if (link) {
                link.attr("href", url);
                return;
            }
            return "<a href='" + url + "' class='btn btn-xs btn-default'>" + $.t("download") + "</a>";
        }
    };

    NRS.openMobileBrowser = function(url) {
        try {
            // Works on Android 6.0 (does not work in 5.1)
            cordova.InAppBrowser.open(url, '_system');
        } catch(e) {
            NRS.logConsole(e.message);
        }
    };

    NRS.isCodeScanningEnabled = function () {
        return NRS.isMobileApp();
    };

    NRS.getShapeShiftUrl = function() {
        if (isDesktopApplication) {
            return location.origin + "/shapeshift/";
        } else {
            return NRS.settings.exchange_url;
        }
    };

    NRS.isForgingSupported = function() {
        return !NRS.isMobileApp() && !(NRS.state && NRS.state.apiProxy);
    };

    return NRS;
}(NRS || {}, jQuery));