/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {

	NRS.plugins = {}


	NRS.checkForPluginManifest = function(pluginId) {
		var manifest = undefined;

		jQuery.ajaxSetup({ async: false });
    	$.ajax({
    		url: 'plugins/' + pluginId + '/manifest.json', 
    		success: function(data){
    			manifest = data;
    		}
		});
    	jQuery.ajaxSetup({ async: true });

    	return manifest;
	}

	NRS.checkPluginValidity = function(pluginId, manifest) {
		var plugin = NRS.plugins[pluginId];
		if (!manifest.pluginVersion) {
    		plugin['validity'] = NRS.constants.PV_UNKNOWN_MANIFEST_VERSION;
    		plugin['validity_msg'] = $.t('pv_unknown_manifest_version_msg', 'Unknown plugin manifest version');
    		return false;
    	}
    	if (manifest.pluginVersion != NRS.constants.PLUGIN_VERSION) {
    		plugin['validity'] = NRS.constants.PV_INCOMPATIBLE_MANIFEST_VERSION;
    		plugin['validity_msg'] = $.t('pv_incompatible_manifest_version_msg', 'Incompatible plugin manifest version');
    	}
    	return true;
	}

	NRS.checkPluginNRSCompatibility = function() {
		//'PNC_COMPATIBLE_MSG': $.t('pnc_compatible_msg', 'Plugin compatible with current NRS version'),
	}

	NRS.initializePlugins = function() {
		NRS.sendRequest("getPlugins", {}, function (response) {
			if(response.plugins && response.plugins.length >= 0) {
				for (var i=0; i<response.plugins.length; i++) {
					var manifest = NRS.checkForPluginManifest(response.plugins[i]);
					if (manifest) {
						NRS.plugins[response.plugins[i]] = {
							'validity': NRS.constants.PV_NOT_VALID,
							'validity_msg': $.t('pv_valid_msg', 'Plugin validation check passed'),
							'nrs_compatibility': NRS.constants.PNC_NOT_COMPATIBLE,
							'nrs_compatibility_msg': $.t('pnc_not_compatible_msg', 'Plugin incompatible with current NRS version'),
							'manifest': undefined
						}

						NRS.checkPluginValidity(response.plugins[i], manifest);
					}
				}
			}
		});
	}



	return NRS;
}(NRS || {}, jQuery));