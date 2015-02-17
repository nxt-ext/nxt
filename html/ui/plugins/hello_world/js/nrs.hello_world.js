/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {

	NRS.pages.plugin_hello_world = function() {
		NRS.pageLoaded();
	}

	return NRS;
}(NRS || {}, jQuery));