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

    $("#mobile_settings_modal").on("show.bs.modal", function(e) {
        $(".info_message").html($.t("remote_node_url", { url: NRS.getRemoteNodeUrl() }));
        if (NRS.mobileSettings.is_testnet) {
            $("#mobile_is_testnet").prop('checked', true);
        } else {
            $("#mobile_is_testnet").prop('checked', false);
        }
        if (NRS.mobileSettings.is_ssl) {
            $("#mobile_is_ssl").prop('checked', true);
        } else {
            $("#mobile_is_ssl").prop('checked', false);
        }
    });

    NRS.forms.setMobileSettings = function() {
        if ($("#mobile_is_testnet").prop('checked') == NRS.mobileSettings.is_testnet &&
            $("#mobile_is_ssl").prop('checked') == NRS.mobileSettings.is_ssl) {
            return { stop: true };
        }
        NRS.mobileSettings.is_testnet = $("#mobile_is_testnet").prop('checked');
        NRS.mobileSettings.is_ssl = $("#mobile_is_ssl").prop('checked');
        NRS.setJSONItem("mobile_settings", NRS.mobileSettings);
        NRS.resetRemoteNode();
        NRS.getRemoteNodeUrl();
        return { stop: true };
    };

    return NRS;

}(NRS || {}, jQuery));