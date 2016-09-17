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
        if (!NRS.isFundingMonitorSupported()) {
            $("#funding_monitor_menu_item").hide();
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
            var now = moment();
            var nextSnapshot = NRS.lastBlockHeight + response.distributionStep - ((NRS.lastBlockHeight - response.distributionStart) % response.distributionStep);
            $("#ardor_distribution_next_snapshot").html(moment.duration(NRS.getBlockHeightMoment(nextSnapshot).diff(now)).humanize());
            var nextUpdate = NRS.lastBlockHeight + response.distributionFrequency - ((NRS.lastBlockHeight - response.distributionStart) % response.distributionFrequency);
            $("#ardor_distribution_next_balance_update").html(moment.duration(NRS.getBlockHeightMoment(nextUpdate).diff(now)).humanize());

            var duration;
            if (response.distributionStart > NRS.lastBlockHeight) {
                duration = moment.duration(NRS.getBlockHeightMoment(response.distributionStart).diff(now));
                $("#ardor_distribution_modal").find(".fomo_message").html($.t("distribution_starts_in", { interval: duration.humanize() }));
            } else {
                duration = moment.duration(NRS.getBlockHeightMoment(response.distributionEnd).diff(now));
                $("#ardor_distribution_modal").find(".fomo_message").html($.t("distribution_ends_in", {interval: duration.humanize()}));
            }
        });
    });

    $("#client_status_modal").on("show.bs.modal", function() {
        if (NRS.isMobileApp()) {
            $("#client_status_description").text($.t("mobile_client_description", { url: NRS.getRemoteNodeUrl() }));
            $("#client_status_set_peer").hide();
            $("#client_status_remote_peer_container").hide();
            $("#client_status_blacklist_peer").hide();
            return;
        } else if (NRS.state.isLightClient) {
            $("#client_status_description").text($.t("light_client_description"));
        } else {
            $("#client_status_description").text($.t("api_proxy_description"));
        }
        if (NRS.state.apiProxyPeer) {
            $("#client_status_remote_peer").val(String(NRS.state.apiProxyPeer).escapeHTML());
            $("#client_status_set_peer").prop('disabled', true);
            $("#client_status_blacklist_peer").prop('disabled', false);
        } else {
            $("#client_status_remote_peer").val("");
            $("#client_status_set_peer").prop('disabled', false);
            $("#client_status_blacklist_peer").prop('disabled', true);
        }
    });

    $("#client_status_remote_peer").keydown(function() {
        if ($(this).val() == NRS.state.apiProxyPeer) {
            $("#client_status_set_peer").prop('disabled', true);
            $("#client_status_blacklist_peer").prop('disabled', false);
        } else {
            $("#client_status_set_peer").prop('disabled', false);
            $("#client_status_blacklist_peer").prop('disabled', true);
        }
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
            NRS.state.apiProxyPeer = announcedAddress;
            $.growl($.t("remote_peer_updated", { peer: String(announcedAddress).escapeHTML() }));
        } else {
            $.growl($.t("remote_peer_selected_by_server"));
        }
        NRS.updateDashboardMessage();
    };

    NRS.forms.blacklistAPIProxyPeer = function ($modal) {
        var data = NRS.getFormData($modal.find("form:first"));
        data.adminPassword = NRS.getAdminPassword();
        return {
            "data": data
        };
    };

    NRS.forms.blacklistAPIProxyPeerComplete = function(response) {
        if (response.done) {
            NRS.state.apiProxyPeer = null;
            $.growl($.t("remote_peer_blacklisted"));
        }
        NRS.updateDashboardMessage();
    };

    return NRS;
}(NRS || {}, jQuery));