/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {

	NRS.plugins = {}
	NRS.activePlugins = false;


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
    		return false;
    	}

    	var invalidManifestFileMsg = $.t('pv_invalid_manifest_file_msg', 'Invalid plugin manifest file');
    	var mandatoryManifestVars = ["name", "myVersion", "shortDescription", "infoUrl", "startPage", "nrsVersion"];
    	for (var i=0; i<mandatoryManifestVars.length; i++) {
    		var mvv = mandatoryManifestVars[i];
    		if (!manifest[mvv] || (manifest[mvv] && manifest[mvv].length == 0)) {
    			plugin['validity'] = NRS.constants.PV_INVALID_MANIFEST_FILE;
    			plugin['validity_msg'] = invalidManifestFileMsg;
    			console.log("Attribute '" + mvv + "' missing for '" + pluginId + "' plugin manifest file.");
    			return false;
    		}
    	}

    	var lengthRestrictions = [
    		["name", 20],
    		["myVersion", 16],
    		["shortDescription", 200],
    		["infoUrl", 200],
    		["startPage", 50],
    		["nrsVersion", 20]
    	]
    	for (var i=0; i<lengthRestrictions.length; i++) {
    		if (manifest[lengthRestrictions[i][0]].length > lengthRestrictions[i][1]) {
    			plugin['validity'] = NRS.constants.PV_INVALID_MANIFEST_FILE;
    			plugin['validity_msg'] = invalidManifestFileMsg;
    			console.log("'" + lengthRestrictions[i][0] + "' attribute too long in '" + pluginId + "' plugin manifest file.");
    			return false;
    		}
    	}

    	if (!(manifest["infoUrl"].substr(0, 7) == 'http://' || manifest["infoUrl"].substr(0, 8) == 'https://')) {
    		plugin['validity'] = NRS.constants.PV_INVALID_MANIFEST_FILE;
    		plugin['validity_msg'] = invalidManifestFileMsg;
    		console.log("'infoUrl' attribute in '" + pluginId + "' plugin manifest file is not a valid URL.");
    		return false;
    	}

    	if (manifest["nrsVersion"].split('.').length != 3 || !(/^[\d\.]+$/.test(manifest["nrsVersion"]))) {
    		plugin['validity'] = NRS.constants.PV_INVALID_MANIFEST_FILE;
    		plugin['validity_msg'] = invalidManifestFileMsg;
    		console.log("'nrsVersion' attribute in '" + pluginId + "' plugin manifest file is not in correct format ('x.y.z', no additions).");
    		return false;
    	}

    	if (manifest["deactivated"] != undefined && typeof(manifest["deactivated"]) != "boolean") {
    		plugin['validity'] = NRS.constants.PV_INVALID_MANIFEST_FILE;
    		plugin['validity_msg'] = invalidManifestFileMsg;
    		console.log("'deactivated' attribute in '" + pluginId + "' plugin manifest file must be boolean type.");
    		return false;
    	}

    	if (manifest["sidebarOptOut"] != undefined && typeof(manifest["sidebarOptOut"]) != "boolean") {
    		plugin['validity'] = NRS.constants.PV_INVALID_MANIFEST_FILE;
    		plugin['validity_msg'] = invalidManifestFileMsg;
    		console.log("'sidebarOptOut' attribute in '" + pluginId + "' plugin manifest file must be boolean type.");
    		return false;
    	}

    	var pluginPath = 'plugins/' + pluginId + '/';
    	var notFound = undefined;
    	var mandatoryFiles = [
    		pluginPath + 'html/pages/' + pluginId + '.html',
    		pluginPath + 'html/modals/' + pluginId + '.html',
    		pluginPath + 'js/nrs.' + pluginId + '.js',
    		pluginPath + 'css/' + pluginId + '.css'
    	]
    	jQuery.ajaxSetup({ async: false });
    	for (var i=0; i<mandatoryFiles.length; i++) {
			$.ajax({
    			url: mandatoryFiles[i],
    			type: 'HEAD',
    			success: function(data) {
    				//nothing to do
    			},
    			error: function(data) {
    				notFound = mandatoryFiles[i];
    			}
			});
    	}
    	jQuery.ajaxSetup({ async: true });

    	if (notFound) {
    		plugin['validity'] = NRS.constants.PV_INVALID_MISSING_FILES;
    		plugin['validity_msg'] = $.t('pv_invalid_missing_files_msg', 'Missing plugin files');
    		console.log("File '" + notFound + "' of plugin '" + pluginId + "' missing.");
    		return false;
    	}

    	plugin['validity'] = NRS.constants.PV_VALID;
    	plugin['validity_msg'] = $.t('pv_valid_msg', 'Plugin is valid');
    	return true;
	}

	NRS.checkPluginNRSCompatibility = function(pluginId) {
		var plugin = NRS.plugins[pluginId];
		var pluginNRSVersion = plugin.manifest["nrsVersion"];
		var pvList = pluginNRSVersion.split('.');
		var currentNRSVersion = NRS.state.version.replace(/[a-zA-Z]/g,'');
		var cvList = currentNRSVersion.split('.');
		if (pvList[0] == cvList[0] && pvList[1] == cvList[1]) {
			if (pvList[2] == cvList[2]) {
				plugin['nrs_compatibility'] = NRS.constants.PNC_COMPATIBLE;
    			plugin['nrs_compatibility_msg'] = $.t('pnc_compatible_msg', 'Plugin compatible with NRS version');	
			} else {
				plugin['nrs_compatibility'] = NRS.constants.PNC_COMPATIBILITY_WARNING;
    			plugin['nrs_compatibility_msg'] = $.t('pnc_compatibility_warning_msg', 'Plugin not build for current NRS version');
			}
		} else {
			plugin['nrs_compatibility'] = NRS.constants.PNC_NOT_COMPATIBLE;
			plugin['nrs_compatibility_msg'] = $.t('pnc_not_compatible_msg', 'Plugin incompatible with NRS version');
		}
	}

	NRS.determinePluginLaunchStatus = function(pluginId) {
		var plugin = NRS.plugins[pluginId];
		if (!((300 <= plugin['validity'] && plugin['validity'] < 400) || (300 <= plugin['nrs_compatibility'] && plugin['validity'] < 400))) {
			if (plugin['manifest']['deactivated']) {
				plugin['launch_status'] = NRS.constants.PL_DEACTIVATED;
				plugin['launch_status_msg'] = $.t('plugin_deactivated', 'Deactivated');
			} else {
				plugin['launch_status'] = NRS.constants.PL_PAUSED;
				plugin['launch_status_msg'] = $.t('plugin_paused', 'Paused');
				NRS.activePlugins = true;
			}
		}
	}

	NRS.initializePlugins = function() {
		NRS.sendRequest("getPlugins", {}, function (response) {
			if(response.plugins && response.plugins.length >= 0) {
				for (var i=0; i<response.plugins.length; i++) {
					var manifest = NRS.checkForPluginManifest(response.plugins[i]);
					if (manifest) {
						NRS.plugins[response.plugins[i]] = {
							'validity': NRS.constants.PV_NOT_VALID,
							'validity_msg': $.t('pv_not_valid_msg', 'Plugin invalid'),
							'nrs_compatibility': NRS.constants.PNC_COMPATIBILITY_UNKNOWN,
							'nrs_compatibility_msg': $.t('pnc_compatible_unknown_msg', 'Plugin compatibility with NRS version unknown'),
							'launch_status': NRS.constants.PL_ERROR,
							'launch_status_msg': $.t('plugin_error', 'Error'),
							'manifest': undefined
						}
						if (NRS.checkPluginValidity(response.plugins[i], manifest)) {
							NRS.plugins[response.plugins[i]]['manifest'] = manifest;
							NRS.checkPluginNRSCompatibility(response.plugins[i]);
							NRS.determinePluginLaunchStatus(response.plugins[i]);
						}
					}
				}
			}
            NRS.initPluginWarning();
            $('#login_password').prop("disabled", false);
		});
	}

	return NRS;
}(NRS || {}, jQuery));