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

    NRS.isIndexedDBSupported = function() {
        return window.indexedDB !== undefined;
    };

    NRS.isCoinExchangePageAvailable = function() {
        return !isDesktopApplication; // JavaFX does not support CORS required by ShapeShift
    };

    NRS.isWebWalletLinkVisible = function() {
        return isDesktopApplication; // When using JavaFX add a link to a web wallet
    };

    NRS.isPollGetState = function() {
        return !isDesktopApplication; // When using JavaFX do not poll the server
    };

    NRS.isExportContactsAvailable = function() {
        return !isDesktopApplication; // When using JavaFX you cannot export the contact list
    };

    return NRS;
}(NRS || {}, jQuery));