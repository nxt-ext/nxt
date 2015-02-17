/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {

	NRS.pages.plugin_hello_world = function() {
		var rows = "";
		
		NRS.sendRequest("getBlockchainStatus", {}, function(response) {
			if (response.lastBlock != undefined) {
				$.each(response, function(fieldName, value) {
					rows += "<tr><td>" + String(fieldName).escapeHTML() + "</td><td>" + String(value).escapeHTML() + "</td></tr>"; 
				});
			}
			NRS.dataLoaded(rows);
		});
	}

	return NRS;
}(NRS || {}, jQuery));

//File name for debugging (Chrome/Firefox)
//@ sourceURL=nrs.hello_world.js