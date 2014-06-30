/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.showRawTransactionModal = function(transaction) {
		$("#raw_transaction_modal_unsigned_transaction_bytes").val(transaction.unsignedTransactionBytes);
		$("#raw_transaction_modal_transaction_bytes").val(transaction.transactionBytes);
		$("#raw_transaction_modal_full_hash").val(transaction.fullHash);
		$("#raw_transaction_modal_signature_hash").val(transaction.signatureHash);

		$("#raw_transaction_modal").modal("show");
	}

	return NRS;
}(NRS || {}, jQuery));