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
    function isErrorResponse(response) {
        return response.errorCode || response.errorDescription || response.errorMessage || response.error;
    }

    function getErrorMessage(response) {
        return response.errorDescription || response.errorMessage || response.error;
    } 

    NRS.jsondata = NRS.jsondata||{};

    NRS.jsondata.monitors = function (response) {
        return {
            accountFormatted: NRS.getAccountLink(response, "account"),
            property: response.property,
            amountFormatted: NRS.formatAmount(response.amount),
            thresholdFormatted: NRS.formatAmount(response.threshold),
            interval: response.interval,
            stopLinkFormatted: "<a href='#' class='btn btn-xs' data-toggle='modal' data-target='#stop_account_monitor_modal' " +
            "data-account='" + response.accountRS + "' " +
            "data-property='" + response.property + "'>" + $.t("stop") + "</a>"
        };
    };

    NRS.incoming.account_monitors = function() {
        NRS.loadPage("account_monitors");
    };

    NRS.pages.account_monitors = function () {
        NRS.hasMorePages = false;
        var view = NRS.simpleview.get('account_monitors_page', {
            errorMessage: null,
            isLoading: true,
            isEmpty: false,
            monitors: []
        });
        var params = {
            "adminPassword": NRS.settings.admin_password,
            "firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
            "lastIndex": NRS.pageNumber * NRS.itemsPerPage
        };
        NRS.sendRequest("getAccountMonitor", params,
            function (response) {
                if (isErrorResponse(response)) {
                    view.render({
                        errorMessage: getErrorMessage(response),
                        isLoading: false,
                        isEmpty: false
                    });
                    return;
                }
                if (response.monitors.length > NRS.itemsPerPage) {
                    NRS.hasMorePages = true;
                    response.monitors.pop();
                }
                view.monitors.length = 0;
                response.monitors.forEach(
                    function (monitorsJson) {
                        view.monitors.push(NRS.jsondata.monitors(monitorsJson))
                    }
                );
                view.render({
                    isLoading: false,
                    isEmpty: view.monitors.length == 0
                });
                NRS.pageLoaded();
            }
        )
    };

    NRS.forms.startAccountMonitorComplete = function() {
        $.growl($.t("monitor_started"));
        NRS.loadPage("account_monitors");
    };

    $("#stop_account_monitor_modal").on("show.bs.modal", function(e) {
        var $invoker = $(e.relatedTarget);
        var account = $invoker.data("account");
        if (account) {
            $("#stop_monitor_account").val(account);
        }
        var property = $invoker.data("property");
        if (property) {
            $("#stop_monitor_property").val(property);
        }
        if (NRS.settings.admin_password) {
            $("#stop_monitor_admin_password").val(NRS.settings.admin_password);
        }
    });

    NRS.forms.stopAccountMonitorComplete = function() {
        $.growl($.t("monitor_stopped"));
        NRS.loadPage("account_monitors");
    };

    return NRS;

}(NRS || {}, jQuery));