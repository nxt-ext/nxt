var NRS = (function(NRS, $, undefined) {
	$("#account_balance_modal").on("show.bs.modal", function(e) {
		if (NRS.accountBalance.errorCode) {
			$("#account_balance_table").hide();

			if (NRS.accountBalance.errorCode == 5) {
				$("#account_balance_warning").html("Your account is brand new. You should fund it with some coins. Your account ID is <strong>" + NRS.account + "</strong>").show();
			} else {
				$("#account_balance_warning").html(NRS.accountBalance.errorDescription.escapeHTML()).show();
			}
		} else {
			$("#account_balance_warning").hide();

			if (NRS.useNQT) {
				$("#account_balance_balance").html(NRS.formatAmount(new BigInteger(NRS.accountBalance.balanceNQT)) + " NXT");
				$("#account_balance_unconfirmed_balance").html(NRS.formatAmount(new BigInteger(NRS.accountBalance.unconfirmedBalanceNQT)) + " NXT");
				$("#account_balance_effective_balance").html(NRS.formatAmount(NRS.accountBalance.effectiveBalanceNXT) + " NXT");
			} else {
				$("#account_balance_balance").html(NRS.formatAmount(NRS.accountBalance.balance / 100) + " NXT");
				$("#account_balance_unconfirmed_balance").html(NRS.formatAmount(NRS.accountBalance.unconfirmedBalance / 100) + " NXT");
				$("#account_balance_effective_balance").html(NRS.formatAmount(NRS.accountBalance.effectiveBalance / 100) + " NXT");
			}

			$("#account_balance_public_key").html(String(NRS.accountBalance.publicKey).escapeHTML());
			$("#account_balance_account_id").html(String(NRS.account).escapeHTML());

			var address = new NxtAddress();

			/*
			if (address.set(NRS.account, true)) {
				$("#account_balance_new_address_format").html(address.toString().escapeHTML());
			} else {
				$("#account_balance_new_address_format").html("/");
			}*/

			if (!NRS.accountBalance.publicKey) {
				$("#account_balance_public_key").html("/");
				$("#account_balance_warning").html("Your account does not have a public key! This means it's not as protected as other accounts. You must make an outgoing transaction to fix this issue. (<a href='#' data-toggle='modal' data-target='#send_message_modal'>send a message</a>, <a href='#' data-toggle='modal' data-target='#register_alias_modal'>buy an alias</a>, <a href='#' data-toggle='modal' data-target='#send_money_modal'>send Nxt</a>, ...)").show();
			}
		}
	});

	return NRS;
}(NRS || {}, jQuery));