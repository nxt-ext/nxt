/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.forms.leaseBalanceComplete = function(response, data) {
		NRS.getAccountInfo();
	}

	return NRS;
}(NRS || {}, jQuery));