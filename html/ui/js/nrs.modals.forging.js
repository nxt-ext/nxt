/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
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
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
	//todo: use a startForgingError function instead!

	NRS.forms.startForgingComplete = function(response, data) {
		if ("deadline" in response) {
            var forgingIndicator = $("#forging_indicator");
            forgingIndicator.addClass("forging");
			forgingIndicator.find("span").html($.t("forging")).attr("data-i18n", "forging");
			NRS.isForging = true;
			$.growl($.t("success_start_forging"), {
				type: "success"
			});
		} else {
			NRS.isForging = false;
			$.growl($.t("error_start_forging"), {
				type: 'danger'
			});
		}
	};

	NRS.forms.stopForgingComplete = function(response, data) {
		if ($("#stop_forging_modal").find(".show_logout").css("display") == "inline") {
			NRS.logout();
			return;
		}

        var forgingIndicator = $("#forging_indicator");
        forgingIndicator.removeClass("forging");
		forgingIndicator.find("span").html($.t("not_forging")).attr("data-i18n", "not_forging");

		NRS.isForging = false;

		if (response.foundAndStopped) {
			$.growl($.t("success_stop_forging"), {
				type: 'success'
			});
		} else {
			$.growl($.t("error_stop_forging"), {
				type: 'danger'
			});
		}
	};

	var forgingIndicator = $("#forging_indicator");
	forgingIndicator.click(function(e) {
		e.preventDefault();

		if (NRS.downloadingBlockchain) {
			$.growl($.t("error_forging_blockchain_downloading"), {
				"type": "danger"
			});
		} else if (NRS.state.isScanning) {
			$.growl($.t("error_forging_blockchain_rescanning"), {
				"type": "danger"
			});
		} else if (!NRS.accountInfo.publicKey) {
			$.growl($.t("error_forging_no_public_key"), {
				"type": "danger"
			});
		} else if (NRS.accountInfo.effectiveBalanceNXT == 0) {
			if (NRS.lastBlockHeight >= NRS.accountInfo.currentLeasingHeightFrom && NRS.lastBlockHeight <= NRS.accountInfo.currentLeasingHeightTo) {
				$.growl($.t("error_forging_lease"), {
					"type": "danger"
				});
			} else {
				$.growl($.t("error_forging_effective_balance"), {
					"type": "danger"
				});
			}
		} else if ($(this).hasClass("forging")) {
			$("#stop_forging_modal").modal("show");
		} else {
			$("#start_forging_modal").modal("show");
		}
	});

	forgingIndicator.hover(
		function(event) {
            NRS.updateForgingStatus();
        },
		function(event) {
			$("#forging_status").attr("title", "");
		}
	);

    NRS.updateForgingStatus = function() {
        var status;
        var tooltip;
        if (!NRS.accountInfo.publicKey) {
            status = "not_forging";
            tooltip = $.t("account has no public key");
        } else if (NRS.accountInfo.effectiveBalanceNXT == 0) {
            status = "not_forging";
            tooltip = $.t("effective balance is 0");
        } else if (NRS.downloadingBlockchain) {
            status = "not_forging";
            tooltip = $.t("blockchain currently downloading");
        } else if (NRS.isLeased) {
            status = "not_forging";
            tooltip = $.t("balance leased");
        } else if (NRS.settings.admin_password == "") {
            status = "unknown_forging_status";
            tooltip = $.t("cannot determine forging status") + "\n" +
                $.t("admin password not specified")
        } else {
            NRS.sendRequest("getForging", {
                "adminPassword": NRS.settings.admin_password
            }, function (response) {
                if ("account" in response) {
                    status = "forging";
                    tooltip = $.t("account") + " " + response.account + "\n" +
                        $.t("time [sec]") + " " + response.remaining + "\n" +
                        $.t("effective balance") + " " + NRS.accountInfo.effectiveBalanceNXT;
                } else if ("generators" in response) {
                    status = "forging";
                    if (response.generators.length == 1) {
                        tooltip = $.t("account") + " " + response.generators[0].account + "\n" +
                            $.t("time [sec]") + " " + response.generators[0].remaining + "\n" +
                            $.t("effective balance") + " " + NRS.accountInfo.effectiveBalanceNXT;
                    } else {
                        tooltip = $.t("number of forging accounts") + " " + response.generators.length;
                    }
                } else {
                    status = "not_forging";
                    tooltip = response.errorDescription;
                }
            }, false);
        }
        var forgingIndicator = $("#forging_indicator");
        forgingIndicator.removeClass("forging");
        forgingIndicator.removeClass("not_forging");
        forgingIndicator.removeClass("unknown_forging_status");
        forgingIndicator.addClass(status);
        forgingIndicator.find("span").html($.t(status)).attr("data-i18n", status);
        NRS.isForging = (status == "forging");
        $("#forging_status").attr("title", tooltip);
    };

	return NRS;
}(NRS || {}, jQuery));