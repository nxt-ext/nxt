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

    NRS.isContactsPageAvailable = function() {
        return window.indexedDB ? true : false;
    };

    NRS.isCoinExchangePageAvailable = function() {
        return navigator.userAgent.indexOf("JavaFX") == -1; // JavaFX does not support cors required by ShapeShift
    };

    NRS.isWebWalletLinkVisible = function() {
        return navigator.userAgent.indexOf("JavaFX") >= 0; // When using JavaFX add a link to a web wallet
    };

    return NRS;
}(NRS || {}, jQuery));