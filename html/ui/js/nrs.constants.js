/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
    NRS.constants = {
    	'MAX_INT_JAVA': 2147483647,
    	'SERVER': {}
    }

    NRS.loadServerConstants = function() {
    	NRS.sendRequest("getConstants", {}, function(response) {
			if (response.genesisAccountId) {
				NRS.constants.SERVER = response;
			}
		});
    }

    return NRS;
}(NRS || {}, jQuery));