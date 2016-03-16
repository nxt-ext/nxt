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
            holdingFormatted: (function () {
                switch (response.holdingType) {
                    case 0: return 'NXT';
                    case 1: return NRS.getTransactionLink(response.holding) + " (" + $.t('asset') + ")";
                    case 2: return NRS.getTransactionLink(response.holding, response.holdingInfo.code)  + " (" + $.t('currency') + ")";
                }
            })(),
            property: response.property,
            amountFormatted: (function () {
                switch (response.holdingType) {
                    case 0: return NRS.formatAmount(response.amount);
                    case 1:
                    case 2: return NRS.formatQuantity(response.amount, response.holdingInfo.decimals);
                }
            })(),
            thresholdFormatted: (function () {
                switch (response.holdingType) {
                    case 0: return NRS.formatAmount(response.threshold);
                    case 1:
                    case 2: return NRS.formatQuantity(response.threshold, response.holdingInfo.decimals);
                }
            })(),
            interval: response.interval
        };
    };

    /**
     * Start monitor modal holding type onchange listener.
     * Hides holding field unless type is asset or currency.
     */
    $('#start_monitor_holding_type').change(function () {
        var holdingType = $("#start_monitor_holding_type");
        if(holdingType.val() == "0") {
            $("#start_monitor_asset_id_group").css("display", "none");
            $("#start_monitor_ms_currency_group").css("display", "none");
            $('#start_monitor_amount_unit').html($.t('amount'));
            $('#start_monitor_amount').attr('name', 'monitorAmountNXT');
            $('#start_monitor_threshold_unit').html($.t('amount'));
            $('#start_monitor_threshold').attr('name', 'monitorThresholdNXT');
        } if(holdingType.val() == "1") {
			$("#start_monitor_asset_id_group").css("display", "inline");
			$("#start_monitor_ms_currency_group").css("display", "none");
            $('#start_monitor_amount_unit').html($.t('quantity'));
            $('#start_monitor_amount').attr('name', 'amountQNTf');
            $('#start_monitor_threshold_unit').html($.t('quantity'));
            $('#start_monitor_threshold').attr('name', 'thresholdQNTf');
		} else if(holdingType.val() == "2") {
			$("#start_monitor_asset_id_group").css("display", "none");
			$("#start_monitor_ms_currency_group").css("display", "inline");
            $('#start_monitor_amount_unit').html($.t('units'));
            $('#start_monitor_amount').attr('name', 'amountQNTf');
            $('#start_monitor_threshold_unit').html($.t('units'));
            $('#start_monitor_threshold').attr('name', 'thresholdQNTf');
		}
    });

    NRS.forms.startAccountMonitor = function($modal) {
        var data = NRS.getFormData($modal.find("form:first"));
        switch (data.holdingType) {
            case '0':
                delete data.holding;
                break;
        }
        return {
            "data": data
        }
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

    $("#start_account_monitor_modal").on("show.bs.modal", function() {
   		var context = {
   			labelText: "Currency",
   			labelI18n: "currency",
   			inputCodeName: "monitor_ms_code",
   			inputIdName: "holding",
   			inputDecimalsName: "monitor_ms_decimals",
   			helpI18n: "add_currency_modal_help"
   		};
   		NRS.initModalUIElement($(this), '.start_monitor_holding_currency', 'add_currency_modal_ui_element', context);

   		context = {
   			labelText: "Asset",
   			labelI18n: "asset",
   			inputIdName: "holding",
   			inputDecimalsName: "monitor_asset_decimals",
   			helpI18n: "add_asset_modal_help"
   		};
   		NRS.initModalUIElement($(this), '.start_monitor_holding_asset', 'add_asset_modal_ui_element', context);
    });

    NRS.forms.startAccountMonitorComplete = function() {
        $.growl($.t("monitor_started"));
        NRS.loadPage("account_monitors");
    };

    return NRS;

}(NRS || {}, jQuery));