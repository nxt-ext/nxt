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
	$("body").on("click", ".show_ledger_modal_action", function(event) {
		event.preventDefault();
		if (NRS.fetchingModalData) {
			return;
		}
		NRS.fetchingModalData = true;
		var entryId = $(this).data("entry");
		var change = $(this).data("change");
		var balance = $(this).data("balance");
        NRS.sendRequest("getAccountLedgerEntry+", { ledgerId: entryId }, function(response) {
			NRS.showLedgerEntryModal(response, change, balance);
		});
	});

	NRS.showLedgerEntryModal = function(entry, change, balance) {
        try {
            var entryDetails = $.extend({}, entry);
            if (entryDetails.timestamp) {
                entryDetails.entryTime = NRS.formatTimestamp(entryDetails.timestamp);
            }
            if (entryDetails.holding) {
                entryDetails.holding_formatted_html = "<a href='#' class='show_transaction_modal_action' data-transaction='" + String(entry.holding).escapeHTML() + "'>" + String(entry.holding).escapeHTML() + "</a>";
                delete entryDetails.holding;
            }
            entryDetails.height_formatted_html = "<a href='#' class='show_block_modal_action' data-block='" + String(entry.height).escapeHTML() + "'>" + String(entry.height).escapeHTML() + "</a>";
            delete entryDetails.block;
            delete entryDetails.height;
            entryDetails.transaction_formatted_html = "<a href='#' class='show_transaction_modal_action' data-transaction='" + String(entry.event).escapeHTML() + "'>" + String(entry.event).escapeHTML() + "</a>";
            delete entryDetails.event;
            delete entryDetails.isTransactionEvent;
            entryDetails.change_formatted_html = change;
            delete entryDetails.change;
            entryDetails.balance_formatted_html = balance;
            delete entryDetails.balance;
            var detailsTable = $("#ledger_info_details_table");
            detailsTable.find("tbody").empty().append(NRS.createInfoTable(entryDetails));
            detailsTable.show();
            $("#ledger_info_modal").modal("show");
        } finally {
            NRS.fetchingModalData = false;
        }
	};

	return NRS;
}(NRS || {}, jQuery));