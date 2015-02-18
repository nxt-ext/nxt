/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {

	NRS.pages.p_hello_world = function() {
		var rows = "";
		
		NRS.sendRequest("getBlockchainStatus", {}, function(response) {
			if (response.lastBlock != undefined) {
				$.each(response, function(fieldName, value) {
					rows += "<tr>";
					rows += "<td>" + String(fieldName).escapeHTML() + "</td>";
					if (fieldName == "lastBlockchainFeederHeight" && value) {
						//Making use of existing client modals and functionality
						var valueHTML = "<a href='#' data-block='" + String(value).escapeHTML() + "' class='show_block_modal_action'>";
						valueHTML += String(value).escapeHTML() + "</a>";
					} else {
						var valueHTML = String(value).escapeHTML();
					}

					rows += "<td>" + valueHTML + "</td>";
					rows += "</tr>"; 
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