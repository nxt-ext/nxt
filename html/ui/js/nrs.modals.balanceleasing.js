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
	NRS.forms.leaseBalanceComplete = function(response, data) {
		NRS.getAccountInfo();
	}

	$("#lease_balance_modal").on("show.bs.modal", function() {
		$("#lease_balance_help").html($.t("lease_balance_help_2"));
	});

	$("#lease_balance_period").on("change", function() {
		if ($(this).val() > 32767) {
			$("#lease_balance_help").html($.t("error_lease_balance_period"));
		} else {
			var days = Math.round($(this).val() / 900);
			$("#lease_balance_help").html($.t("lease_balance_help_var", {
				"blocks": String($(this).val()).escapeHTML(),
				"days": String(Math.round(days)).escapeHTML()
			}));
		}
	});

	return NRS;
}(NRS || {}, jQuery));