/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {

	NRS.pages.p_hello_world = function() {
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

	NRS.setup.p_hello_world = function() {
		//Do one-time initialization stuff here
		$('#p_hello_world_startup_date_time').html(moment().format('LLL'));

	}

	return NRS;
}(NRS || {}, jQuery));

//File name for debugging (Chrome/Firefox)
//@ sourceURL=nrs.hello_world.js