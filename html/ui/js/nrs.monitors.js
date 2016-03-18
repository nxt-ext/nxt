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
            statusLinkFormatted: "<a href='#' class='btn btn-xs' " +
                        "onClick='NRS.goToMonitor(" + JSON.stringify(response) + ");'>" +
                         $.t("status") + "</a>",
            stopLinkFormatted: "<a href='#' class='btn btn-xs' data-toggle='modal' data-target='#stop_funding_monitor_modal' " +
                        "data-account='" + response.accountRS + "' " +
                        "data-property='" + response.property + "'>" + $.t("stop") + "</a>"
        };
    };

    NRS.jsondata.properties = function (response) {
        try {
            var value = JSON.parse(response.value);
        } catch (e) {
            NRS.logConsole(e.message);
        }
        return {
            accountFormatted: NRS.getAccountLink(response, "recipient"),
            property: response.property,
            amountFormatted: value.amount ? NRS.formatAmount(value.amount) : "?",
            thresholdFormatted: value.threshold ? NRS.formatAmount(value.amount) : "?",
            interval: value.interval ? value.interval : "?"
        };
    };

    NRS.incoming.funding_monitors = function() {
        NRS.loadPage("funding_monitors");
    };

    NRS.pages.funding_monitors = function () {
        NRS.hasMorePages = false;
        var view = NRS.simpleview.get('funding_monitors_page', {
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
        NRS.sendRequest("getFundingMonitor", params,
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
                    function (monitorJson) {
                        view.monitors.push(NRS.jsondata.monitors(monitorJson))
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

    NRS.forms.startFundingMonitorComplete = function() {
        $.growl($.t("monitor_started"));
        NRS.loadPage("funding_monitors");
    };

    $("#stop_funding_monitor_modal").on("show.bs.modal", function(e) {
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

    NRS.forms.stopFundingMonitorComplete = function() {
        $.growl($.t("monitor_stopped"));
        NRS.loadPage("funding_monitors");
    };

    NRS.goToMonitor = function(monitor) {
   		NRS.goToPage("funding_monitor_status", function() {
            return monitor;
        });
   	};

    NRS.pages.funding_monitor_status = function (callback) {
        var monitor = callback();
        $("#monitor_funding_account").html(monitor.account);
        $("#monitor_control_property").html(monitor.property);
        NRS.hasMorePages = false;
        var view = NRS.simpleview.get('funding_monitor_status_page', {
            errorMessage: null,
            isLoading: true,
            isEmpty: false,
            properties: []
        });
        var params = {
            "setter": monitor.account,
            "property": monitor.property,
            "firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
            "lastIndex": NRS.pageNumber * NRS.itemsPerPage
        };
        NRS.sendRequest("getAccountProperties", params,
            function (response) {
                if (isErrorResponse(response)) {
                    view.render({
                        errorMessage: getErrorMessage(response),
                        isLoading: false,
                        isEmpty: false
                    });
                    return;
                }
                if (response.properties.length > NRS.itemsPerPage) {
                    NRS.hasMorePages = true;
                    response.properties.pop();
                }
                view.properties.length = 0;
                response.properties.forEach(
                    function (propertiesJson) {
                        view.properties.push(NRS.jsondata.properties(propertiesJson))
                    }
                );
                view.render({
                    isLoading: false,
                    isEmpty: view.properties.length == 0
                });
                NRS.pageLoaded();
            }
        )
    };

    return NRS;

}(NRS || {}, jQuery));