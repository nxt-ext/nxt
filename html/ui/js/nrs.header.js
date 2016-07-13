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
        if (!NRS.isExternalLinkVisible()) {
            $("#web_wallet_li").remove();
            $("#api_console_li").hide();
            $("#database_shell_li").hide();
        }
    });

    $("#refreshSearchIndex").on("click", function() {
        NRS.sendRequest("luceneReindex", {
            adminPassword: NRS.getAdminPassword()
        }, function (response) {
            if (response.errorCode) {
                $.growl(NRS.escapeRespStr(response.errorDescription));
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

    $("#ardor_distribution_modal").on("show.bs.modal", function() {
        NRS.sendRequest("getFxtQuantity", {
            "account": NRS.account
        }, function (response) {
            $("#ardor_distribution_start_height").html(response.distributionStart);
            $("#ardor_distribution_start_time").html(NRS.getBlockHeightTimeEstimate(response.distributionStart));
            $("#ardor_distribution_end_height").html(response.distributionEnd);
            $("#ardor_distribution_end_time").html(NRS.getBlockHeightTimeEstimate(response.distributionEnd));
            $("#ardor_distribution_current_balance").html(NRS.formatQuantity(response.quantityQNT, 4));
            $("#ardor_distribution_expected_balance").html(NRS.formatQuantity(response.totalExpectedQuantityQNT, 4));

            var duration;
            if (response.distributionStart > NRS.lastBlockHeight) {
                duration = moment.duration(NRS.getBlockHeightMoment(response.distributionStart).diff(moment()));
                $("#ardor_distribution_modal").find(".fomo_message").html($.t("distribution_starts_in", { interval: duration.humanize() }));
            } else {
                duration = moment.duration(NRS.getBlockHeightMoment(response.distributionEnd).diff(moment()));
                $("#ardor_distribution_modal").find(".fomo_message").html($.t("distribution_ends_in", {interval: duration.humanize()}));
            }
        });
    });

    $("#client_status_modal").on("show.bs.modal", function() {
        if (NRS.state.isLightClient) {
            $("#client_status_description").text($.t("light_client_description"));
        } else {
            $("#client_status_description").text($.t("api_proxy_description"));
        }
        $("#client_status_remote_peer").val(String(NRS.state.apiProxy).escapeHTML());
    });

    NRS.forms.setAPIProxyPeer = function ($modal) {
        var data = NRS.getFormData($modal.find("form:first"));
        data.adminPassword = NRS.getAdminPassword();
        return {
            "data": data
        };
    };

    NRS.forms.setAPIProxyPeerComplete = function(response) {
        var announcedAddress = response.announcedAddress;
        if (announcedAddress) {
            NRS.state.apiProxy = announcedAddress;
            $.growl($.t("remote_peer_updated", { peer: String(announcedAddress).escapeHTML() }));
        } else {
            $.growl($.t("remote_peer_selected_by_server"));
        }
        $("#dashboard_message").addClass("alert-success").removeClass("alert-danger").html(NRS.blockchainDownloadingMessage());
    };

    return NRS;
}(NRS || {}, jQuery));