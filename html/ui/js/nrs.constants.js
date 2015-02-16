/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
    NRS.constants = {
        'PLUGIN_VERSION': 1,
    	'MAX_INT_JAVA': 2147483647,

        //Plugin validity status codes
        'PV_VALID': 100,
        'PV_NOT_VALID': 300,
        'PV_UNKNOWN_MANIFEST_VERSION': 301,
        'PV_INCOMPATIBLE_MANIFEST_VERSION': 302,
        'PV_INVALID_MANIFEST_FILE': 303,

        //Plugin NRS compatibility status codes
        'PNC_COMPATIBLE': 100,
        'PNC_NOT_COMPATIBLE': 300,

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