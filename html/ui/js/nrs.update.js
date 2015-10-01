/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.isOutdated = false;

	NRS.checkAliasVersions = function() {
		if (NRS.downloadingBlockchain) {
			$("#nrs_update_explanation").find("span").hide();
			$("#nrs_update_explanation_blockchain_sync").show();
			return;
		}
		if (NRS.isTestNet) {
			$("#nrs_update_explanation").find("span").hide();
			$("#nrs_update_explanation_testnet").show();
			return;
		}

        // Load all version aliases in parallel and call NRS.checkForNewVersion() at the end
        async.parallel([
            function(callback){
                getVersionInfo("nrsVersion", callback);
            },
            function(callback){
                getVersionInfo("nrsBetaVersion", callback);
            },
            function(callback){
                getVersionInfo("nrsVersionWin", callback);
            },
            function(callback){
                getVersionInfo("nrsBetaVersionWin", callback);
            }
        ],
        function(err, results) {
            if (err == null) {
                NRS.logConsole("Version aliases: " + JSON.stringify(results));
            } else {
                NRS.logConsole("Version aliases lookup error " + err);
            }
            NRS.checkForNewVersion();
        });
	};

	NRS.checkForNewVersion = function() {
        var installVersusNormal, installVersusBeta;
        if (NRS.nrsVersion && NRS.nrsVersion.versionNr) {
			installVersusNormal = NRS.versionCompare(NRS.state.version, NRS.nrsVersion.versionNr);
		}
		if (NRS.nrsBetaVersion && NRS.nrsBetaVersion.versionNr) {
			installVersusBeta = NRS.versionCompare(NRS.state.version, NRS.nrsBetaVersion.versionNr);
		}

		$("#nrs_update_explanation").find("> span").hide();
		$("#nrs_update_explanation_wait").attr("style", "display: none !important");
		$(".nrs_new_version_nr").html(NRS.nrsVersion.versionNr).show();
		$(".nrs_beta_version_nr").html(NRS.nrsBetaVersion.versionNr).show();

		if (installVersusNormal == -1 && installVersusBeta == -1) {
			NRS.isOutdated = true;
			$("#nrs_update").html($.t("outdated")).show();
			$("#nrs_update_explanation_new_choice").show();
		} else if (installVersusBeta == -1) {
			NRS.isOutdated = false;
			$("#nrs_update").html($.t("new_beta")).show();
			$("#nrs_update_explanation_new_beta").show();
		} else if (installVersusNormal == -1) {
			NRS.isOutdated = true;
			$("#nrs_update").html($.t("outdated")).show();
			$("#nrs_update_explanation_new_release").show();
		} else {
			NRS.isOutdated = false;
			$("#nrs_update_explanation_up_to_date").show();
		}
	};

	NRS.verifyClientUpdate = function(e) {
		e.stopPropagation();
		e.preventDefault();
		var files = null;
		if (e.originalEvent.target.files && e.originalEvent.target.files.length) {
			files = e.originalEvent.target.files;
		} else if (e.originalEvent.dataTransfer.files && e.originalEvent.dataTransfer.files.length) {
			files = e.originalEvent.dataTransfer.files;
		}
		if (!files) {
			return;
		}
        var updateHashProgress = $("#nrs_update_hash_progress");
        updateHashProgress.css("width", "0%");
		updateHashProgress.show();
		var worker = new Worker("js/crypto/sha256worker.js");
		worker.onmessage = function(e) {
			if (e.data.progress) {
				$("#nrs_update_hash_progress").css("width", e.data.progress + "%");
			} else {
				$("#nrs_update_hash_progress").hide();
				$("#nrs_update_drop_zone").hide();

                var nrsUpdateResult = $("#nrs_update_result");
                if (e.data.sha256 == NRS.downloadedVersion.hash) {
					nrsUpdateResult.html($.t("success_hash_verification")).attr("class", " ");
				} else {
					nrsUpdateResult.html($.t("error_hash_verification")).attr("class", "incorrect");
				}

				$("#nrs_update_hash_version").html(NRS.downloadedVersion.versionNr);
				$("#nrs_update_hash_download").html(e.data.sha256);
				$("#nrs_update_hash_official").html(NRS.downloadedVersion.hash);
				$("#nrs_update_hashes").show();
				nrsUpdateResult.show();
				NRS.downloadedVersion = {};
				$("body").off("dragover.nrs, drop.nrs");
			}
		};

		worker.postMessage({
			file: files[0]
		});
	};

	NRS.downloadClientUpdate = function(version, type) {
		if (version == "release") {
			if (type == "exe") {
                NRS.downloadedVersion = NRS.nrsVersionWin;
            } else {
                NRS.downloadedVersion = NRS.nrsVersion;
            }
		} else {
            if (type == "exe") {
                NRS.downloadedVersion = NRS.nrsBetaVersionWin;
            } else {
                NRS.downloadedVersion = NRS.nrsBetaVersion;
            }
		}

        var filename = "nxt-client-" + NRS.downloadedVersion.versionNr + "." + type;
        var fileurl = "https://bitbucket.org/JeanLucPicard/nxt/downloads/" + filename;
        $("#nrs_update_iframe").attr("src", fileurl);
        $("#nrs_update_explanation").hide();
        var updateDropZone = $("#nrs_update_drop_zone");
        updateDropZone.html($.t("drop_update_v2", { filename: filename }));
        updateDropZone.show();

        var body = $("body");
        body.on("dragover.nrs", function(e) {
            e.preventDefault();
            e.stopPropagation();

            if (e.originalEvent && e.originalEvent.dataTransfer) {
                e.originalEvent.dataTransfer.dropEffect = "copy";
            }
        });

        body.on("drop.nrs", function(e) {
            NRS.verifyClientUpdate(e);
        });

        updateDropZone.on("click", function(e) {
            e.preventDefault();
            $("#nrs_update_file_select").trigger("click");
        });

        $("#nrs_update_file_select").on("change", function(e) {
            NRS.verifyClientUpdate(e);
        });

		return false;
	};

    NRS.versionCompare = function(v1, v2) {
   		if (v2 == undefined) {
   			return -1;
   		} else if (v1 == undefined) {
   			return -1;
   		}

   		//https://gist.github.com/TheDistantSea/8021359 (based on)
   		var v1last = v1.slice(-1);
   		var v2last = v2.slice(-1);

   		if (v1last == 'e') {
   			v1 = v1.substring(0, v1.length - 1);
   		} else {
   			v1last = '';
   		}

   		if (v2last == 'e') {
   			v2 = v2.substring(0, v2.length - 1);
   		} else {
   			v2last = '';
   		}

   		var v1parts = v1.split('.');
   		var v2parts = v2.split('.');

   		function isValidPart(x) {
   			return /^\d+$/.test(x);
   		}

   		if (!v1parts.every(isValidPart) || !v2parts.every(isValidPart)) {
   			return NaN;
   		}

   		v1parts = v1parts.map(Number);
   		v2parts = v2parts.map(Number);

   		for (var i = 0; i < v1parts.length; ++i) {
   			if (v2parts.length == i) {
   				return 1;
   			}
               if (v1parts[i] != v2parts[i]) {
                   if (v1parts[i] > v2parts[i]) {
                       return 1;
                   } else {
                       return -1;
                   }
               }
   		}

   		if (v1parts.length != v2parts.length) {
   			return -1;
   		}

   		if (v1last && v2last) {
   			return 0;
   		} else if (v1last) {
   			return 1;
   		} else if (v2last) {
   			return -1;
   		} else {
   			return 0;
   		}
   	};

    // Get latest version number and hash of version specified by the alias
    function getVersionInfo(aliasName, callback) {
        NRS.sendRequest("getAlias", {
            "aliasName": aliasName
        }, function (response) {
            if (response.aliasURI && (response = response.aliasURI.split(" "))) {
                NRS[aliasName] = { versionNr: response[0], hash: response[1] };
                callback(null, NRS[aliasName]);
            }
        });
    }
	return NRS;
}(NRS || {}, jQuery));