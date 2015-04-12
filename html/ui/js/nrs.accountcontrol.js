/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $) {

	NRS.pages.account_control = function(callback) {
		
	}
	
	NRS.setup.account_control = function() {
		var sidebarId = 'sidebar_account_control';
		var options = {
			"id": sidebarId,
			"titleHTML": '<i class="fa ion-locked"></i> <span data-i18n="account_control">Account Control</span>',
			"page": 'account_control',
			"desiredPosition": 80
		};
		NRS.addTreeviewSidebarMenuItem(options);
		options = {
			"titleHTML": '<span data-i18n="phasing_only">Phasing Only</span>',
			"type": 'MODAL',
			"modalId": 'set_phasing_only_modal'
		};
		NRS.appendMenuItemToTSMenuItem(sidebarId, options);
	};


	return NRS;
}(NRS || {}, jQuery));