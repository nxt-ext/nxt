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
var NRS = (function(NRS, $) {
	//todo: use a startForgingError function instead!

	NRS.forms.startForgingComplete = function(response, data) {
		if ("deadline" in response) {
            var forgingIndicator = $("#forging_indicator");
            forgingIndicator.addClass("forging");
			forgingIndicator.find("span").html($.t("forging")).attr("data-i18n", "forging");
			NRS.forgingStatus = NRS.constants.FORGING;
            NRS.updateForgingTooltip(NRS.getForgingTooltip);

			$.growl($.t("success_start_forging"), {
				type: "success"
			});
		} else {
			NRS.forgingStatus = NRS.constants.NOT_FORGING;
            NRS.updateForgingTooltip(response.errorDescription);
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

		NRS.forgingStatus = NRS.constants.NOT_FORGING;
        NRS.updateForgingTooltip($.t("forging_stopped_tooltip"));
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
		function() {
            NRS.updateForgingStatus();
        }
	);

    NRS.getForgingTooltip = function(data) {
        if (!data || data.account == NRS.accountInfo.account) {
            return $.t("forging_tooltip", {"balance": NRS.accountInfo.effectiveBalanceNXT});
        }
        return $.t("forging_another_account_tooltip", {"accountRS": data.accountRS });
    };

    NRS.updateForgingTooltip = function(tooltip) {
        $("#forging_status").attr('title', tooltip).tooltip('fixTitle');
    };

    NRS.updateForgingStatus = function(secretPhrase) {
        var status = NRS.forgingStatus;
        var tooltip = $("#forging_status").attr('title');
        if (!NRS.accountInfo.publicKey) {
            status = NRS.constants.NOT_FORGING;
            tooltip = $.t("error_forging_no_public_key");
        } else if (NRS.accountInfo.effectiveBalanceNXT == 0) {
            status = NRS.constants.NOT_FORGING;
            tooltip = $.t("error_forging_effective_balance");
        } else if (NRS.downloadingBlockchain) {
            status = NRS.constants.NOT_FORGING;
            tooltip = $.t("error_forging_blockchain_downloading");
        } else if (NRS.state.isScanning) {
            status = NRS.constants.NOT_FORGING;
            tooltip = $.t("error_forging_blockchain_rescanning");
        } else if (NRS.isLeased) {
            status = NRS.constants.NOT_FORGING;
            tooltip = $.t("error_forging_lease");
        } else if (NRS.needsAdminPassword && NRS.settings.admin_password == "" && (!secretPhrase || !NRS.isLocalHost)) {
            // do not change forging status
        } else {
            var params = {};
            if (NRS.needsAdminPassword && NRS.settings.admin_password != "") {
                params["adminPassword"] = NRS.settings.admin_password;
            }
            if (secretPhrase && NRS.needsAdminPassword && NRS.settings.admin_password == "") {
                params["secretPhrase"] = secretPhrase;
            }
            NRS.sendRequest("getForging", params, function (response) {
                if ("account" in response) {
                    status = NRS.constants.FORGING;
                    tooltip = NRS.getForgingTooltip(response);
                } else if ("generators" in response) {
                    if (response.generators.length == 0) {
                        status = NRS.constants.NOT_FORGING;
                        tooltip = $.t("not_forging_not_started_tooltip");
                    } else {
                        status = NRS.constants.FORGING;
                        if (response.generators.length == 1) {
                            tooltip = NRS.getForgingTooltip(response.generators[0]);
                        } else {
                            tooltip = $.t("forging_more_than_one_tooltip", { "generators": response.generators.length });
                        }
                    }
                } else {
                    status = NRS.constants.UNKNOWN;
                    tooltip = response.errorDescription;
                }
            }, false);
        }
        var forgingIndicator = $("#forging_indicator");
        forgingIndicator.removeClass(NRS.constants.FORGING);
        forgingIndicator.removeClass(NRS.constants.NOT_FORGING);
        forgingIndicator.removeClass(NRS.constants.UNKNOWN);
        forgingIndicator.addClass(status);
        forgingIndicator.find("span").html($.t(status)).attr("data-i18n", status);
        forgingIndicator.show();
        NRS.forgingStatus = status;
        NRS.updateForgingTooltip(tooltip);
    };

	return NRS;
}(NRS || {}, jQuery));