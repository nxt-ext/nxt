var NRS = (function(NRS, $, undefined) {
	NRS.forms.setAccountInfoComplete = function(response, data) {
		var name = $.trim(String(data.name));
		if (name) {
			$("#account_name").html(name.escapeHTML());
		} else {
			$("#account_name").html("No name set");
		}
	}

	return NRS;
}(NRS || {}, jQuery));