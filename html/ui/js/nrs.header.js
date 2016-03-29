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

    function widgetVisibility(widget, depends) {
        if (NRS.isApiEnabled(depends)) {
            widget.show();
        } else {
            widget.hide();
        }
    }

    $(window).load(function() {
        widgetVisibility($("#header_send_money"), { apis: [NRS.constants.REQUEST_TYPES.sendMoney] });
        widgetVisibility($("#header_transfer_currency"), { apis: [NRS.constants.REQUEST_TYPES.transferCurrency] });
        widgetVisibility($("#header_send_message"), { apis: [NRS.constants.REQUEST_TYPES.sendMessage] });
        if (!NRS.isCoinExchangePageAvailable()) {
            $("#exchange_menu_li").remove();
        }
        if (!NRS.isWebWalletLinkVisible()) {
            $("#web_wallet_li").remove();
        }
        if (NRS.isOpenLinkInNewTabSupported()) {
            $("#api_console_li").show();
            $("#database_shell_li").show();
        }
    });

    $("#refreshSearchIndex").on("click", function() {
        NRS.sendRequest("luceneReindex", {
            adminPassword: NRS.settings.admin_password
        }, function (response) {
            if (response.errorCode) {
                $.growl(response.errorDescription.escapeHTML());
            } else {
                $.growl($.t("search_index_refreshed"));
            }
        })
    });

    $("#header_open_web_wallet").on("click", function() {
        if (java) {
            java.openBrowser(NRS.accountRS);
        }
    });

    return NRS;
}(NRS || {}, jQuery));