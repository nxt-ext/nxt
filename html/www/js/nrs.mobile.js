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

    $("#mobile_settings_modal").on("show.bs.modal", function() {
        $(".info_message").html($.t("remote_node_url", { url: NRS.getRemoteNodeUrl() }));
        if (NRS.mobileSettings.is_simulate_app) {
            $("#mobile_is_simulate_app").prop('checked', true);
        } else {
            $("#mobile_is_simulate_app").prop('checked', false);
        }
        if (NRS.mobileSettings.is_testnet) {
            $("#mobile_is_testnet").prop('checked', true);
        } else {
            $("#mobile_is_testnet").prop('checked', false);
        }
    });

    NRS.forms.setMobileSettings = function() {
        NRS.mobileSettings.is_simulate_app = $("#mobile_is_simulate_app").prop('checked');
        NRS.mobileSettings.is_testnet = $("#mobile_is_testnet").prop('checked');
        NRS.setJSONItem("mobile_settings", NRS.mobileSettings);
        return { stop: true };
    };

    return NRS;

}(NRS || {}, jQuery));