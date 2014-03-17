(function($, NRS, undefined) {
    "use strict";
    
    function nl2br (str) {   
    	return (str + '').replace(/([^>\r\n]?)(\r\n|\n\r|\r|\n)/g, '$1<br />$2');
	}

	var __entityMap = {
	    "&": "&amp;",
	    "<": "&lt;",
	    ">": "&gt;",
	    '"': '&quot;',
	    "'": '&#39;',
	    "/": '&#x2F;'
	};
	
	String.prototype.escapeHTML = function() {
	    return String(this).replace(/[&<>"'\/]/g, function (s) {
	        return __entityMap[s];
	    });
	}
	
	String.prototype.unescapeHTML = function() {
		return String(this).replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace('&quot;', '"').replace('&#39;', "'").replace('&#x2F;', "/");
	}
	
	function escapeHtml(string) {
		return String(string).replace(/[&<>"'\/]/g, function (s) {
		  return entityMap[s];
		});
	}
	  
	Number.prototype.pad = function(size) {
		var s = String(this);
		if(typeof(size) !== "number"){size = 2;}
		
		while (s.length < size) {s = "0" + s;}
		return s;
	}
	    
	// save the original function object
	var _superModal = $.fn.modal;
	
	// add locked as a new option
	$.extend( _superModal.Constructor.DEFAULTS, {
	    locked: false
	});
	
	// capture the original hide
	var _hide = _superModal.Constructor.prototype.hide;
	
	// add the lock, unlock and override the hide of modal
	$.extend(_superModal.Constructor.prototype, {
	    // locks the dialog so that it cannot be hidden
	    lock: function() {
	        this.options.locked = true;
	    }
	    // unlocks the dialog so that it can be hidden by 'esc' or clicking on the backdrop (if not static)
	    ,unlock: function() {
	        this.options.locked = false;
	    }
	    // override the original hide so that the original is only called if the modal is unlocked
	    ,hide: function() {
	        if (this.options.locked) return;
	
	        _hide.apply(this, arguments);
	    }
	});
    
	var LOCALE_DATE_FORMATS = {
       "ar-SA" : "dd/MM/yy",
       "bg-BG" : "dd.M.yyyy",
       "ca-ES" : "dd/MM/yyyy",
       "zh-TW" : "yyyy/M/d",
       "cs-CZ" : "d.M.yyyy",
       "da-DK" : "dd-MM-yyyy",
       "de-DE" : "dd.MM.yyyy",
       "el-GR" : "d/M/yyyy",
       "en-US" : "M/d/yyyy",
       "fi-FI" : "d.M.yyyy",
       "fr-FR" : "dd/MM/yyyy",
       "he-IL" : "dd/MM/yyyy",
       "hu-HU" : "yyyy. MM. dd.",
       "is-IS" : "d.M.yyyy",
       "it-IT" : "dd/MM/yyyy",
       "ja-JP" : "yyyy/MM/dd",
       "ko-KR" : "yyyy-MM-dd",
       "nl-NL" : "d-M-yyyy",
       "nb-NO" : "dd.MM.yyyy",
       "pl-PL" : "yyyy-MM-dd",
       "pt-BR" : "d/M/yyyy",
       "ro-RO" : "dd.MM.yyyy",
       "ru-RU" : "dd.MM.yyyy",
       "hr-HR" : "d.M.yyyy",
       "sk-SK" : "d. M. yyyy",
       "sq-AL" : "yyyy-MM-dd",
       "sv-SE" : "yyyy-MM-dd",
       "th-TH" : "d/M/yyyy",
       "tr-TR" : "dd.MM.yyyy",
       "ur-PK" : "dd/MM/yyyy",
       "id-ID" : "dd/MM/yyyy",
       "uk-UA" : "dd.MM.yyyy",
       "be-BY" : "dd.MM.yyyy",
       "sl-SI" : "d.M.yyyy",
       "et-EE" : "d.MM.yyyy",
       "lv-LV" : "yyyy.MM.dd.",
       "lt-LT" : "yyyy.MM.dd",
       "fa-IR" : "MM/dd/yyyy",
       "vi-VN" : "dd/MM/yyyy",
       "hy-AM" : "dd.MM.yyyy",
       "az-Latn-AZ" : "dd.MM.yyyy",
       "eu-ES" : "yyyy/MM/dd",
       "mk-MK" : "dd.MM.yyyy",
       "af-ZA" : "yyyy/MM/dd",
       "ka-GE" : "dd.MM.yyyy",
       "fo-FO" : "dd-MM-yyyy",
       "hi-IN" : "dd-MM-yyyy",
       "ms-MY" : "dd/MM/yyyy",
       "kk-KZ" : "dd.MM.yyyy",
       "ky-KG" : "dd.MM.yy",
       "sw-KE" : "M/d/yyyy",
       "uz-Latn-UZ" : "dd/MM yyyy",
       "tt-RU" : "dd.MM.yyyy",
       "pa-IN" : "dd-MM-yy",
       "gu-IN" : "dd-MM-yy",
       "ta-IN" : "dd-MM-yyyy",
       "te-IN" : "dd-MM-yy",
       "kn-IN" : "dd-MM-yy",
       "mr-IN" : "dd-MM-yyyy",
       "sa-IN" : "dd-MM-yyyy",
       "mn-MN" : "yy.MM.dd",
       "gl-ES" : "dd/MM/yy",
       "kok-IN" : "dd-MM-yyyy",
       "syr-SY" : "dd/MM/yyyy",
       "dv-MV" : "dd/MM/yy",
       "ar-IQ" : "dd/MM/yyyy",
       "zh-CN" : "yyyy/M/d",
       "de-CH" : "dd.MM.yyyy",
       "en-GB" : "dd/MM/yyyy",
       "es-MX" : "dd/MM/yyyy",
       "fr-BE" : "d/MM/yyyy",
       "it-CH" : "dd.MM.yyyy",
       "nl-BE" : "d/MM/yyyy",
       "nn-NO" : "dd.MM.yyyy",
       "pt-PT" : "dd-MM-yyyy",
       "sr-Latn-CS" : "d.M.yyyy",
       "sv-FI" : "d.M.yyyy",
       "az-Cyrl-AZ" : "dd.MM.yyyy",
       "ms-BN" : "dd/MM/yyyy",
       "uz-Cyrl-UZ" : "dd.MM.yyyy",
       "ar-EG" : "dd/MM/yyyy",
       "zh-HK" : "d/M/yyyy",
       "de-AT" : "dd.MM.yyyy",
       "en-AU" : "d/MM/yyyy",
       "es-ES" : "dd/MM/yyyy",
       "fr-CA" : "yyyy-MM-dd",
       "sr-Cyrl-CS" : "d.M.yyyy",
       "ar-LY" : "dd/MM/yyyy",
       "zh-SG" : "d/M/yyyy",
       "de-LU" : "dd.MM.yyyy",
       "en-CA" : "dd/MM/yyyy",
       "es-GT" : "dd/MM/yyyy",
       "fr-CH" : "dd.MM.yyyy",
       "ar-DZ" : "dd-MM-yyyy",
       "zh-MO" : "d/M/yyyy",
       "de-LI" : "dd.MM.yyyy",
       "en-NZ" : "d/MM/yyyy",
       "es-CR" : "dd/MM/yyyy",
       "fr-LU" : "dd/MM/yyyy",
       "ar-MA" : "dd-MM-yyyy",
       "en-IE" : "dd/MM/yyyy",
       "es-PA" : "MM/dd/yyyy",
       "fr-MC" : "dd/MM/yyyy",
       "ar-TN" : "dd-MM-yyyy",
       "en-ZA" : "yyyy/MM/dd",
       "es-DO" : "dd/MM/yyyy",
       "ar-OM" : "dd/MM/yyyy",
       "en-JM" : "dd/MM/yyyy",
       "es-VE" : "dd/MM/yyyy",
       "ar-YE" : "dd/MM/yyyy",
       "en-029" : "MM/dd/yyyy",
       "es-CO" : "dd/MM/yyyy",
       "ar-SY" : "dd/MM/yyyy",
       "en-BZ" : "dd/MM/yyyy",
       "es-PE" : "dd/MM/yyyy",
       "ar-JO" : "dd/MM/yyyy",
       "en-TT" : "dd/MM/yyyy",
       "es-AR" : "dd/MM/yyyy",
       "ar-LB" : "dd/MM/yyyy",
       "en-ZW" : "M/d/yyyy",
       "es-EC" : "dd/MM/yyyy",
       "ar-KW" : "dd/MM/yyyy",
       "en-PH" : "M/d/yyyy",
       "es-CL" : "dd-MM-yyyy",
       "ar-AE" : "dd/MM/yyyy",
       "es-UY" : "dd/MM/yyyy",
       "ar-BH" : "dd/MM/yyyy",
       "es-PY" : "dd/MM/yyyy",
       "ar-QA" : "dd/MM/yyyy",
       "es-BO" : "dd/MM/yyyy",
       "es-SV" : "dd/MM/yyyy",
       "es-HN" : "dd/MM/yyyy",
       "es-NI" : "dd/MM/yyyy",
       "es-PR" : "dd/MM/yyyy",
       "am-ET" : "d/M/yyyy",
       "tzm-Latn-DZ" : "dd-MM-yyyy",
       "iu-Latn-CA" : "d/MM/yyyy",
       "sma-NO" : "dd.MM.yyyy",
       "mn-Mong-CN" : "yyyy/M/d",
       "gd-GB" : "dd/MM/yyyy",
       "en-MY" : "d/M/yyyy",
       "prs-AF" : "dd/MM/yy",
       "bn-BD" : "dd-MM-yy",
       "wo-SN" : "dd/MM/yyyy",
       "rw-RW" : "M/d/yyyy",
       "qut-GT" : "dd/MM/yyyy",
       "sah-RU" : "MM.dd.yyyy",
       "gsw-FR" : "dd/MM/yyyy",
       "co-FR" : "dd/MM/yyyy",
       "oc-FR" : "dd/MM/yyyy",
       "mi-NZ" : "dd/MM/yyyy",
       "ga-IE" : "dd/MM/yyyy",
       "se-SE" : "yyyy-MM-dd",
       "br-FR" : "dd/MM/yyyy",
       "smn-FI" : "d.M.yyyy",
       "moh-CA" : "M/d/yyyy",
       "arn-CL" : "dd-MM-yyyy",
       "ii-CN" : "yyyy/M/d",
       "dsb-DE" : "d. M. yyyy",
       "ig-NG" : "d/M/yyyy",
       "kl-GL" : "dd-MM-yyyy",
       "lb-LU" : "dd/MM/yyyy",
       "ba-RU" : "dd.MM.yy",
       "nso-ZA" : "yyyy/MM/dd",
       "quz-BO" : "dd/MM/yyyy",
       "yo-NG" : "d/M/yyyy",
       "ha-Latn-NG" : "d/M/yyyy",
       "fil-PH" : "M/d/yyyy",
       "ps-AF" : "dd/MM/yy",
       "fy-NL" : "d-M-yyyy",
       "ne-NP" : "M/d/yyyy",
       "se-NO" : "dd.MM.yyyy",
       "iu-Cans-CA" : "d/M/yyyy",
       "sr-Latn-RS" : "d.M.yyyy",
       "si-LK" : "yyyy-MM-dd",
       "sr-Cyrl-RS" : "d.M.yyyy",
       "lo-LA" : "dd/MM/yyyy",
       "km-KH" : "yyyy-MM-dd",
       "cy-GB" : "dd/MM/yyyy",
       "bo-CN" : "yyyy/M/d",
       "sms-FI" : "d.M.yyyy",
       "as-IN" : "dd-MM-yyyy",
       "ml-IN" : "dd-MM-yy",
       "en-IN" : "dd-MM-yyyy",
       "or-IN" : "dd-MM-yy",
       "bn-IN" : "dd-MM-yy",
       "tk-TM" : "dd.MM.yy",
       "bs-Latn-BA" : "d.M.yyyy",
       "mt-MT" : "dd/MM/yyyy",
       "sr-Cyrl-ME" : "d.M.yyyy",
       "se-FI" : "d.M.yyyy",
       "zu-ZA" : "yyyy/MM/dd",
       "xh-ZA" : "yyyy/MM/dd",
       "tn-ZA" : "yyyy/MM/dd",
       "hsb-DE" : "d. M. yyyy",
       "bs-Cyrl-BA" : "d.M.yyyy",
       "tg-Cyrl-TJ" : "dd.MM.yy",
       "sr-Latn-BA" : "d.M.yyyy",
       "smj-NO" : "dd.MM.yyyy",
       "rm-CH" : "dd/MM/yyyy",
       "smj-SE" : "yyyy-MM-dd",
       "quz-EC" : "dd/MM/yyyy",
       "quz-PE" : "dd/MM/yyyy",
       "hr-BA" : "d.M.yyyy.",
       "sr-Latn-ME" : "d.M.yyyy",
       "sma-SE" : "yyyy-MM-dd",
       "en-SG" : "d/M/yyyy",
       "ug-CN" : "yyyy-M-d",
       "sr-Cyrl-BA" : "d.M.yyyy",
       "es-US" : "M/d/yyyy"
    };
    
    var LANG = window.navigator.userLanguage || window.navigator.language;
    
    var LOCALE_DATE_FORMAT = LOCALE_DATE_FORMATS[LANG] || 'dd/MM/yyyy';   
    
    NRS.helpers      = {};
    NRS.state 		 = {};
    NRS.blocks		 = [];
    NRS.temp 		 = {"blocks": []};
	NRS.normalVersion = {};
	NRS.betaVersion = {};
	NRS.account = {};
	NRS.currentPage = "dashboard";
	NRS.newsRefresh = 0;
	NRS.messages = {};
	NRS.forms = {"errorMessages": {}};
	NRS.lastBlockHeight = 0;
	NRS.lastTransactionTimestamp = 0;
    NRS.account = "";
    NRS.server = "";
    NRS.pages = {};
    NRS.incoming = {};
    NRS.lastTransactionsTimestamp = 0;
    NRS.lastTransactions = "";
    NRS.xhrPool = [];
    NRS.genesis = "1739068987193023818";
    NRS.selectedContext = null;
    NRS.database = null;
    NRS.databaseSupport = false;
    NRS.assets = {};
    NRS.loadedBefore = [];
    NRS.contacts = {};
    NRS.tentative = {"aliases": [], "ask_orders": [], "bid_orders": [], "cancelled_orders": [], "messages": [], "polls": [], "transfer_assets": []};
    NRS.tentativeAge = {"cancelled_orders": []};
	NRS.accountBalance = {};
	NRS.transactionsPageType = null;
	NRS.downloadingBlockchain = false;
	NRS.blockchainCalculationServers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 12, 13, 14, 15, 16, 17];
	NRS.isTestNet = false;
	NRS.fetchingModalData = false;
	NRS.closedGroups = [];
	NRS.isLocalHost = false;
	
    NRS.init = function() {  
   	    if (location.port && location.port != "6876") {
		    $(".testnet_only").hide();
	    } else {
			NRS.isTestNet = true;
			NRS.blockchainCalculationServers = [9, 10];
		    $(".testnet_only, #testnet_login").show();
	    }
					
		if (!NRS.server && window.location.hostname.toLowerCase() == "localhost") {
			NRS.isLocalHost = true;
		}
		
    	NRS.createDatabase();
    	
    	NRS.getState(function() {
	    	NRS.checkAliasVersions();
    	});
    	
    	NRS.showLockscreen();
    	
    	NRS.checkServerTime();
    	
    	//every 30 seconds check for new block..
    	setInterval(function() {
    		NRS.getState();
    	}, 1000*30);
    	    	
    	setInterval(NRS.checkAliasVersions, 1000*60*60);
    	
    	$("#login_password").keypress(function(e) {
    	    if (e.which == '13') {
    	    	e.preventDefault();
    	    	var password = $("#login_password").val();
	    	    NRS.login(password);
    	    }
    	});
    	    	
		$(".modal form input").keydown(function(e){
			if (e.which == '13') {
				e.preventDefault();
		  		return false;
			}
		});
    	    	
    	$("#send_money_recipient, #transfer_asset_recipient, #send_message_recipient, #add_contact_account_id, #update_contact_account_id").blur(function() {
    		var value = $(this).val();
    		var modal = $(this).closest(".modal");
    		
    		if (value) {
    			NRS.checkRecipient(value, modal);
    		} else {
    			modal.find(".account_info").hide();
    		}
    	});
    }
    
    NRS.checkServerTime = function() {
    	$.ajax({
    	    url: 'http://www.convert-unix-time.com/api?timestamp=now&returnType=jsonp',
    	    dataType: 'jsonp'
    	})
    	.done(function(response) {
    		if (response.timestamp) {
    			var comparisonTimestamp = response.timestamp;
    			
    			NRS.sendRequest("getTime", function(response) {
    				var serverTimestamp = Math.round(new Date(Date.UTC(2013, 10, 24, 12, 0, 0, 0) + response.time * 1000).getTime() / 1000);
    			    var difference = Math.abs(serverTimestamp - comparisonTimestamp);
    			    
    			    if (difference > 16) {
    			    	//possibly out of date time... (15 sec difference max - allowing 1 sec extra for ajax request handling)
    			    	//todo
    			    }
    			});
    		}
    	});
    }
        
    NRS.showLoginOrWelcomeScreen = function() {
		if (localStorage.getItem("logged_in")) {
        	NRS.showLoginScreen();
        } else {
	        NRS.showWelcomeScreen();
        }
    }
    
    NRS.showLoginScreen = function() {
    	$("#account_phrase_custom_panel, #account_phrase_generator_panel, #welcome_panel, #custom_passphrase_link").hide();
    	$("#account_phrase_custom_panel :input:not(:button):not([type=submit])").val("");
    	$("#account_phrase_generator_panel :input:not(:button):not([type=submit])").val("");
    	$("#login_panel").show();
    	setTimeout(function() { 
			$("#login_password").focus() 
		}, 10);
    	$(".center").center();
    }
    
    NRS.showWelcomeScreen = function() {
    	$("#login_panel, account_phrase_custom_panel, #account_phrase_generator_panel, #welcome_panel, #custom_passphrase_link").hide();
    	$("#welcome_panel").show();
    	$(".center").center();
    }
    
    NRS.registerUserDefinedAccount = function() {
    	$("#account_phrase_generator_panel, #login_panel, #welcome_panel, #custom_passphrase_link").hide();
    	$("#account_phrase_custom_panel :input:not(:button):not([type=submit])").val("");
    	$("#account_phrase_generator_panel :input:not(:button):not([type=submit])").val("");
    	$("#account_phrase_custom_panel").show();
    	$("#registration_password").focus();
    	$(".center").center();
    }
    
    NRS.registerAccount = function() {	
    	$("#login_panel, #welcome_panel").hide();
    	$("#account_phrase_generator_panel").show();
    	$("#account_phrase_generator_panel step_3 .callout").hide();
    	
    	var $loading = $("#account_phrase_generator_loading");
    	var $loaded  = $("#account_phrase_generator_loaded");
    	
    	if (window.crypto || window.msCrypto) {
	    	$loading.find("span.loading_text").html("Generating your secret phrase. Please wait");
    	}
    	
    	$loading.show();
		$loaded.hide();
		
    	$(".center").center();

		if (typeof PassPhraseGenerator == "undefined") {
		    $.when(
			    $.getScript("js/seedrandom.js"),
			    $.getScript("js/passphrasegenerator.js")
			).done(function() {
				$loading.hide();
				$loaded.show();
					
				PassPhraseGenerator.generatePassPhrase("#account_phrase_generator_panel");
			}).fail(function(jqxhr, settings, exception) {
				alert("Could not load word list...");
			});
		} else {
			$loading.hide();
			$loaded.show();

			PassPhraseGenerator.generatePassPhrase("#account_phrase_generator_panel");
		}
    }
    
    NRS.verifyGeneratedPassphrase = function() {    	
    	var password = $.trim($("#account_phrase_generator_panel .step_3 textarea").val());
    	    	
    	if (password != PassPhraseGenerator.passPhrase) {    		
	    	$("#account_phrase_generator_panel .step_3 .callout").show();
    	} else {
	    	NRS.login(password, function() {
	   			$.growl("Secret phrase confirmed successfully, you are now logged in.", {"type": "success"});
	   		});
	    	PassPhraseGenerator.reset();
	    	$("#account_phrase_generator_panel textarea").val("");
    		$("#account_phrase_generator_panel .step_3 .callout").hide();
    	}    
    }
    
    $("#account_phrase_custom_panel form").submit(function(event) {
        event.preventDefault()
    
    	var password = $("#registration_password").val();
    	var repeat   = $("#registration_password_repeat").val();
    	
    	var error = "";
    	    	
    	if (password.length < 35) {
    		error = "Secret phrase must be at least 35 characters long.";
		} else if (password.length < 50 && (!password.match(/[A-Z]/) || !password.match(/[0-9]/))) {
			error = "Since your secret phrase is less than 50 characters long, it must contain numbers and uppercase letters.";
		} else if (password != repeat) {
    		error = "Secret phrases do not match.";
		} 
		        	
    	if (error) {
    		$("#account_phrase_custom_panel .callout").first().removeClass("callout-info").addClass("callout-danger").html(error);
    	} else {
    		$("#registration_password, #registration_password_repeat").val("");
    		NRS.login(password, function() {
				$.growl("Secret phrase confirmed successfully, you are now logged in.", {"type": "success"});
			});
    	}
    });
       
    NRS.getState = function(callback) {
    	NRS.sendRequest('getState', function(response) {	
    		if (response.errorCode) {
    			//todo
    		} else {
    			if (!("lastBlock" in NRS.state)) {
	    			//first time...
	    			NRS.state = response;
	    			
	    			$("#nrs_version").html(NRS.state.version).removeClass("loading_dots");
	    			
	    			NRS.getBlock(NRS.state.lastBlock, NRS.handleInitialBlocks);
	    		} else if (NRS.state.lastBlock != response.lastBlock) {	 
	    			NRS.temp.blocks = [];
					NRS.state = response;
					NRS.getAccountBalance();
					NRS.getBlock(NRS.state.lastBlock, NRS.handleNewBlocks);
					NRS.getNewTransactions();
	    		}
	    		
	    		if (callback) {
		    		callback();
	    		}
	    	}
    	});
    }
    
    NRS.checkAliasVersions = function() {
    	//Get latest version nr+hash of normal version
    	NRS.sendRequest("getAliasURI", {"alias": "nrsversion"}, function(response) {
    	    if (response.uri && (response = response.uri.split(" "))) {
    	        NRS.normalVersion.versionNr = response[0];
    	        NRS.normalVersion.hash = response[1];
    	        
    	        if (NRS.betaVersion.versionNr) {
    	        	NRS.checkForNewVersion();
    	        }
    	    }
    	});
    	
    	//Get latest version nr+hash of beta version
    	NRS.sendRequest("getAliasURI", {"alias" : "nrsbetaversion"}, function(response) {
    	    if (response.uri && (response = response.uri.split(" "))) {
    	        NRS.betaVersion.versionNr = response[0];
    	        NRS.betaVersion.hash = response[1];
    	       	    		        
    	        if (NRS.normalVersion.versionNr) {
    	        	NRS.checkForNewVersion();
    	        }
    	    }
    	});
    }
    
	$("#nrs_modal").on('show.bs.modal', function (e) {
   		for (var key in NRS.state) {
   			var el = $("#nrs_node_state_" + key);
   			if (el.length) {
   				if (key.indexOf("number") != -1) {
   					el.html(NRS.formatAmount(NRS.state[key]));
   				} else if (key.indexOf("Memory") != -1) {
   					el.html(NRS.formatVolume(NRS.state[key]));
   				} else if (key == "time") {
   					el.html(NRS.formatTimestamp(NRS.state[key]));
   				} else {
   					el.html(String(NRS.state[key]).escapeHTML());
   				}
   			}
   		}   
   		   		
   		$("#nrs_update_explanation").show();
   		$("#nrs_modal_state").show();

	});

	$("#nrs_modal").on('hide.bs.modal', function (e) {
		$("body").off("dragover.nrs, drop.nrs");
		
		$("#nrs_update_drop_zone, #nrs_update_result, #nrs_update_hashes, #nrs_update_hash_progress").hide();

		$(this).find("ul.nav li.active").removeClass("active");
		$("#nrs_modal_state_nav").addClass("active");
		
		$(".nrs_modal_content").hide();
	});
	
    $("#nrs_modal ul.nav li").click(function(e) {
	    e.preventDefault();
	    
	    var tab = $(this).data("tab");
	    
	    $(this).siblings().removeClass("active");
	    $(this).addClass("active");
	    
		$(".nrs_modal_content").hide();
		
		var content = $("#nrs_modal_" + tab);
		
		content.show();
    });

    NRS.checkForNewVersion = function() {
        var installVersusNormal, installVersusBeta, normalVersusBeta;
                               
        if (NRS.normalVersion && NRS.normalVersion.versionNr) {
            installVersusNormal = NRS.versionCompare(NRS.state.version, NRS.normalVersion.versionNr);
        }
        if (NRS.betaVersion && NRS.betaVersion.versionNr) {
            installVersusBeta = NRS.versionCompare(NRS.state.version, NRS.betaVersion.versionNr);
        }
                
        $("#nrs_update_explanation span").hide();
        $(".nrs_new_version_nr").html(NRS.normalVersion.versionNr).show();
        $(".nrs_beta_version_nr").html(NRS.betaVersion.versionNr).show();
                 
        if (installVersusNormal == -1 && installVersusBeta == -1) {
        	$("#nrs_update").html("Outdated!").show();
        	$("#nrs_update_explanation_new_choice").show();
        } else if (installVersusBeta == -1) {
        	$("#nrs_update").html("New Beta").show();
			$("#nrs_update_explanation_new_beta").show();
        } else if (installVersusNormal == -1) {
        	$("#nrs_update").html("Outdated!").show();
        	$("#nrs_update_explanation_new_release").show();
        } else {
        	$("#nrs_update_explanation_up_to_date").show();
        }
    }

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
            v1 = v1.substring(0, v1.length-1);
        } else {
            v1last = '';
        }
        
        if (v2last == 'e') {
            v2 = v2.substring(0, v2.length-1);
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
            if (v1parts[i] == v2parts[i]) {
                continue;
            } else if (v1parts[i] > v2parts[i]) {
                return 1;
            } else {
                return -1;
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
    }
    
    NRS.supportsUpdateVerification = function() {
    	if ((typeof File !== 'undefined') && !File.prototype.slice) {
	        if (File.prototype.webkitSlice) {
	            File.prototype.slice = File.prototype.webkitSlice;
	        }
	        
	        if (File.prototype.mozSlice) {
	            File.prototype.slice = File.prototype.mozSlice;
	        }
	    }
    
	    // Check for the various File API support.
	    if (!window.File || !window.FileReader || !window.FileList || !window.Blob || !File.prototype.slice || !window.Worker) {
	    	return false;
	    }
	    
	    return true;
	}
   
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
			
	    $("#nrs_update_hash_progress").css("width", "0%");
	    $("#nrs_update_hash_progress").show();
	    	    
	    	    

	    var worker = new Worker("js/worker_sha256.js");
	    
	    worker.onmessage = function(e) {
	        if (e.data.progress) {
	          	$("#nrs_update_hash_progress").css("width", e.data.progress + "%");
	        } else {
	           $("#nrs_update_hash_progress").hide();
	           $("#nrs_update_drop_zone").hide();
	                    
	           if (e.data.sha256 == NRS.downloadedVersion.hash) {
	               $("#nrs_update_result").html("The downloaded version has been verified, the hash is correct. You may proceed with the installation.").attr("class"," ");
	           } else {
	               $("#nrs_update_result").html("The downloaded version hash does not compare to the specified hash in the blockchain. DO NOT PROCEED.").attr("class", "incorrect");
	           }
	        
	           $("#nrs_update_hash_version").html(NRS.downloadedVersion.versionNr);
			   $("#nrs_update_hash_download").html(e.data.sha256);
			   $("#nrs_update_hash_official").html(NRS.downloadedVersion.hash);
			   $("#nrs_update_hashes").show();
	           $("#nrs_update_result").show();
	           
	           NRS.downloadedVersion = {};
	           
	           $("body").off("dragover.nrs, drop.nrs");
	        }
	    };
	
	    worker.postMessage({file: files[0]});
	}

	NRS.downloadClientUpdate = function(version) {
		if (version == "release") {
	        NRS.downloadedVersion = NRS.normalVersion;
	    } else {
	        NRS.downloadedVersion = NRS.betaVersion;
	    }
	    	    	    
	    $("#nrs_update_iframe").attr("src", "http://download.nxtcrypto.org/nxt-client-" + NRS.downloadedVersion.versionNr + ".zip");
	    	    
	    $("#nrs_update_explanation").hide();
	    $("#nrs_update_drop_zone").show();
	    
	    $("body").on("dragover.nrs", function(e) {
			e.preventDefault();
			e.stopPropagation();
			
			if (e.originalEvent && e.originalEvent.dataTransfer) {
				e.originalEvent.dataTransfer.dropEffect = "copy";
			}
	    });
	    
	    $("body").on("drop.nrs", function(e) {	 
			NRS.verifyClientUpdate(e); 
	    });
	    
	    $("#nrs_update_drop_zone").on("click", function(e) {
		    e.preventDefault();
		    
		    $("#nrs_update_file_select").trigger("click");
		    
	    });

	    $("#nrs_update_file_select").on("change", function(e) {		
			NRS.verifyClientUpdate(e); 
	    });
	    	    
	    return false;
	}
    
    //when modal closes remove all those events...
    
    NRS.calculateBlockchainDownloadTime = function(callback) {
    	if (!NRS.blockchainCalculationServers.length) {
	    	return;
    	}
    	
		var key = Math.floor((Math.random()*NRS.blockchainCalculationServers.length));
		var value = NRS.blockchainCalculationServers[key];
		
		NRS.blockchainCalculationServers.splice(key, 1);
		
    	try {
    		if (NRS.isTestNet) {
	    		var url = "http://node" + value + ".mynxtcoin.org:6876/nxt?requestType=getState";
    		} else {
    			var url = "http://vps" + value + ".nxtcrypto.org:7876/nxt?requestType=getState";
    		}
    		    		
			NRS.sendOutsideRequest(url, function(response) {
				if (response.numberOfBlocks && response.time && response.numberOfBlocks > NRS.state.numberOfBlocks && Math.abs(NRS.state.time - response.time) < 120) {
					NRS.blockchainExpectedBlocks = response.numberOfBlocks;
					if (callback) {
						callback();
					}
				} else if (callback) {
					NRS.calculateBlockchainDownloadTime(callback);
				}
	    	}, false);
    	} catch (err) {
    		if (callback) {
	    		NRS.calculateBlockchainDownloadTime(callback);
	    	}
    	}
    }
    
    NRS.updateBlockchainDownloadProgress = function() {
	    var percentage = parseInt(Math.round((NRS.state.numberOfBlocks / NRS.blockchainExpectedBlocks) * 100), 10);
	    
	    $("#downloading_blockchain .progress-bar").css("width", percentage + "%").prop("aria-valuenow", percentage);
	    $("#downloading_blockchain .sr-only").html(percentage + "% Complete");
    }
    
    NRS.handleInitialBlocks = function(response) {  
    	if (response.errorCode) {
	    	return;
    	}
		
	    NRS.blocks.push(response);
    	if (NRS.blocks.length < 10 && response.previousBlock) {
    		NRS.getBlock(response.previousBlock, NRS.handleInitialBlocks);
    	} else {    		
    		NRS.lastBlockHeight = NRS.blocks[0].height;
    		
			if (NRS.state && NRS.state.time - NRS.blocks[0].timestamp > 60*60*30) {
		    	NRS.downloadingBlockchain = true;
		    	$("#downloading_blockchain, #nrs_update_explanation_blockchain_sync").show();
		    	$("#show_console").hide();
		    	NRS.calculateBlockchainDownloadTime(function() {
		    		NRS.updateBlockchainDownloadProgress();
		    	});
	    	}

    		var rows = "";
    		
    		for (var i=0; i<NRS.blocks.length; i++) {
    			var block = NRS.blocks[i];
    			    			
				rows += "<tr><td>" + (block.numberOfTransactions > 0 ? "<a href='#' data-block='" + String(block.height).escapeHTML() + "' class='block' style='font-weight:bold'>" + String(block.height).escapeHTML() + "</a>" : String(block.height).escapeHTML()) + "</td><td>" + NRS.formatTimestamp(block.timestamp) + "</td><td>" + NRS.formatAmount(block.totalAmount) + " + " + NRS.formatAmount(block.totalFee) + "</td><td>" + block.numberOfTransactions + "</td></tr>";
    		}
    		    	
    		$("#dashboard_blocks_table tbody").empty().append(rows);
    		NRS.dataLoadFinished($("#dashboard_blocks_table"));
    	}
    }
    
    NRS.handleNewBlocks = function(response) {  
    		
    	if (NRS.downloadingBlockchain) {
    		//new round started...
    		if (NRS.temp.blocks.length == 0 && NRS.state.lastBlock != response.blockNr) {
	    		return;
    		}
    	}
    	
    	//we have all blocks 	
    	if (response.height -1 == NRS.lastBlockHeight || NRS.temp.blocks.length == 99) {
    		var newBlocks = [];
    		
    		//there was only 1 new block (response)
    		if (NRS.temp.blocks.length == 0) {
    			//remove oldest block, add newest block
	    		NRS.blocks.unshift(response);
	    		newBlocks.push(response);
	    	} else {
	    		NRS.temp.blocks.push(response);
	    		//remove oldest blocks, add newest blocks
	    		[].unshift.apply(NRS.blocks, NRS.temp.blocks);
	    		newBlocks = NRS.temp.blocks;
	    		NRS.temp.blocks = [];
	    	}
	    	
	    	if (NRS.blocks.length > 100) {
	    		NRS.blocks = NRS.blocks.slice(0, 100);
	    	}
	    	
	    	//set new last block height
	    	NRS.lastBlockHeight = NRS.blocks[0].height;
	    		    	
	    	NRS.incoming.updateDashboardBlocks(newBlocks);
    	} else {
    		NRS.temp.blocks.push(response);
    		NRS.getBlock(response.previousBlock, NRS.handleNewBlocks);
    	}    	
	}
	
	NRS.getNewTransactions = function() {				
		NRS.sendRequest("getAccountTransactionIds", {"account": NRS.account, "timestamp": NRS.lastTransactionsTimestamp}, function(response) {		
			if (response.transactionIds && response.transactionIds.length) {
				var transactionIds = response.transactionIds.reverse().slice(0, 10);
								
				if (transactionIds.toString() == NRS.lastTransactions) {
					NRS.handleNewTransactions({});
					return;
				}
					    		
	    		NRS.transactionIds = transactionIds;
	    		
				var nrTransactions = transactionIds.length;
				
				var transactionsChecked = 0;
				
				var newTransactions = [];
														
				//if we have a new transaction, we just get them all.. (10 max)
				for (var i=0; i<nrTransactions; i++) {
					NRS.sendRequest('getTransaction', {"transaction": transactionIds[i]}, function(transaction) {
						transactionsChecked++;
						
						newTransactions.push(transaction);
					
						if (transactionsChecked == nrTransactions) {
							NRS.handleNewTransactions(newTransactions, response.transactionIds);
						}
					});
				}
			} else {
				NRS.handleNewTransactions({});
			}
		});
	}
		
	NRS.handleNewTransactions = function(newTransactions, transactionIds) {		
		if (newTransactions.length) {
			newTransactions.sort(NRS.sortArray);
			
			NRS.lastTransactions = transactionIds.toString();
			NRS.lastTransactionsTimestamp = newTransactions[newTransactions.length-1].timestamp;
	
			NRS.incoming.updateDashboardTransactions(newTransactions, transactionIds);

			if (NRS.tentative) {
				for (var category in NRS.tentative) {
					var type = "object";
					var to_delete = [];

					if (category == "cancelled_orders") {
						type = "string";	
					}
										
					for (var key in NRS.tentative[category]) {													
						if (type == "string") {
							var transactionId = NRS.tentative[category][key];
						} else {
							var transactionId = NRS.tentative[category][key].transaction;
						}
											
						if ($.inArray(transactionId, transactionIds) != -1) {
							to_delete.push(key);
						} else {
							var age = 0;
							
							if (type == "string") {
								NRS.tentativeAge[category][key]++;
								age = NRS.tentativeAge[category][key];
							} else {								
								if (!("age" in NRS.tentative[category][key])) {
									NRS.tentative[category][key].age = 1;
								} else {
									NRS.tentative[category][key].age++;
								}
							
								age = NRS.tentative[category][key].age;
							}
							
							if (age >= 2) {
								to_delete.push(key);
							}
						}
						
					}
					
					if (to_delete.length) {											
						for (var i=to_delete.length-1; i>=0; i--) {
							NRS.tentative[category].splice(to_delete[i], 1);
							if (type == "string") {
								NRS.tentativeAge[category].splice(to_delete[i], 1);
							}
						}
					}
					
					to_delete = [];
				}
			} 	
		}
													
		if (NRS.incoming[NRS.currentPage]) {
			NRS.incoming[NRS.currentPage](newTransactions);
		}
	}	
	
	//we always update the dashboard page..
	NRS.incoming.updateDashboardBlocks = function(newBlocks) {
	    var newBlockCount = newBlocks.length;
	    
	    if (newBlockCount > 10) {
	    	newBlocks = newBlocks.slice(0, 10);
	    	newBlockCount  = newBlocks.length;
	    }
	    
	    if (NRS.downloadingBlockchain) {
		    if (NRS.state && NRS.state.time - NRS.blocks[0].timestamp < 60*60*30) {
		    	NRS.downloadingBlockchain = false;
		    	$("#downloading_blockchain, #nrs_update_explanation_blockchain_sync").hide();
		    	$("#show_console").show();
				$.growl("The block chain is now up to date.", {"type": "success"});
		    	NRS.checkAliasVersions();
	    	} else {
		    	NRS.updateBlockchainDownloadProgress();
	    	}
	    }
	    	
	    var rows = "";
	    
	    for (var i=0; i<newBlockCount; i++) {
	    	var block = newBlocks[i];

	    	rows += "<tr><td>" + (block.numberOfTransactions > 0 ? "<a href='#' data-block='" + String(block.height).escapeHTML() + "' class='block' style='font-weight:bold'>" + String(block.height).escapeHTML() + "</a>" : String(block.height).escapeHTML()) + "</td><td>" + NRS.formatTimestamp(block.timestamp) + "</td><td>" + NRS.formatAmount(block.totalAmount) + " + " + NRS.formatAmount(block.totalFee) + "</td><td>" + NRS.formatAmount(block.numberOfTransactions) + "</td></tr>";
	    }
	    
	    if (newBlockCount == 1) {
	    	$("#dashboard_blocks_table tbody tr:last").remove();
	    } else if (newBlockCount == 10){
	    	$("#dashboard_blocks_table tbody").empty();	    	
	    } else {
	    	$("#dashboard_blocks_table tbody tr").slice(10-newBlockCount).remove();
	    }	    	
	    
	    $("#dashboard_blocks_table tbody").prepend(rows);
	    	    
	    //update number of confirmations... perhaps we should also update it in tne NRS.transactions array
	    $("#dashboard_transactions_table td.confirmations").each(function() {
	    	if ($(this).data("incoming")) {
	    		$(this).removeData("incoming");
	    		return true;
	    	}
	    	
	    	var confirmations = parseInt($(this).data("confirmations"), 10);
	    	
	    	if (confirmations <= 10) {
	    		var nrConfirmations = confirmations + newBlocks.length;
	    		if (nrConfirmations > 10) {
	    			nrConfirmations = '10+';
	    		}
	    		$(this).html(nrConfirmations);
	    	}
	    });
	}
	
	NRS.incoming.updateDashboardTransactions = function(newTransactions, transactionIds) {
		var newTransactionCount = newTransactions.length;

		if (newTransactionCount) {
			var rows = "";
			
			for (var i=0; i<newTransactionCount; i++) {
				var transaction = newTransactions[i];
				
				var receiving = transaction.recipient == NRS.account;
				var account = (receiving ? String(transaction.sender).escapeHTML() : String(transaction.recipient).escapeHTML());

				rows += "<tr><td>" + (transaction.attachment ? "<a href='#' data-transaction='" + String(transactionIds[i]).escapeHTML() + "' style='font-weight:bold'>" + NRS.formatTimestamp(transaction.timestamp) + "</a>" : NRS.formatTimestamp(transaction.timestamp)) + "</td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fa fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fa fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td><span" + (transaction.type == 0 && receiving ? " style='color:#006400'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + NRS.formatAmount(transaction.amount) + "</span> <span" + ((!receiving && transaction.type == 0) ? " style='color:red'" : "") + ">+</span> <span" + (!receiving ? " style='color:red'" : "") + ">" + NRS.formatAmount(transaction.fee) +  "</span></td><td>" + (account != NRS.genesis ? "<a href='#' data-user='" + account + "' class='user_info'>" + NRS.getAccountTitle(account) + "</a>" : "Genesis") + "</td><td data-confirmations='" + String(transaction.confirmations).escapeHTML() + "' data-initial='true'>" + (transaction.confirmations > 10 ? "10+" : String(transaction.confirmations).escapeHTML()) + "</td></tr>";
			}
			
			$("#dashboard_transactions_table tbody").empty().append(rows);
			
			var $parent = $("#dashboard_transactions_table").parent();
			
			if ($parent.hasClass("data-empty")) {
				$parent.removeClass("data-empty");
	    		if ($parent.data("no-padding")) {
	    			$parent.parent().addClass("no-padding");
	    		}
			}
		}
	}
	
	$("#account_balance_modal").on("show.bs.modal", function(e) {
		if (NRS.accountBalance.errorCode) {
			$("#account_balance_table").hide();
			
			if (NRS.accountBalance.errorCode == 5) {
				$("#account_balance_warning").html("Your account is brand new. You should fund it with some coins. Your account ID is <strong>" + NRS.account + "</strong>").show();
			} else {
				$("#account_balance_warning").html(NRS.accountBalance.errorDescription.escapeHTML()).show();
			}
		} else {
			$("#account_balance_warning").hide();
			
			$("#account_balance_balance").html(NRS.formatAmount(NRS.accountBalance.balance/100) + " NXT");
			$("#account_balance_effective_balance").html(NRS.formatAmount(NRS.accountBalance.effectiveBalance/100) + " NXT");
			$("#account_balance_public_key").html(NRS.accountBalance.publicKey);
			$("#account_balance_account_id").html(NRS.account);

			if (!NRS.accountBalance.publicKey) {
				$("#account_balance_public_key").html("/");
				$("#account_balance_warning").html("Your account does not have a public key! This means it's not as protected as other accounts. You must make an outgoing transaction to fix this issue.").show();
			}
		}
	});
   
    NRS.getBlock = function(blockID, callback, async) {  
    	NRS.sendRequest('getBlock', {"block": blockID}, function(response) {
    		if (response.errorCode && response.errorCode == -1) {
    			NRS.getBlock(blockID, callback, async);
    		} else {
	    		if (callback) {
	    			response.blockNr = blockID;
    				callback(response);
    			}
    		}
    	}, (async == undefined ? true : async));
    }
        
    NRS.showLockscreen = function() {
        /* CENTER ELEMENTS IN THE SCREEN */
        $.fn.center = function() {
            this.css("position", "absolute");
            this.css("top", Math.max(0, (($(window).height() - $(this).outerHeight()) / 2) +
                    $(window).scrollTop()) - 30 + "px");
            this.css("left", Math.max(0, (($(window).width() - $(this).outerWidth()) / 2) +
                    $(window).scrollLeft()) + "px");
            return this;
        }  
    	              
      	if (localStorage.getItem("logged_in")) {
		  	setTimeout(function() { 
		  		$("#login_password").focus() 
		  	}, 10);
      	} else {
      		NRS.showWelcomeScreen();
      	}
      	
        $(".center").center().show();
        $(window).on("resize.lockscreen", function() {
        	$(".center").center();
        });  
    }
            
    NRS.unlock = function() {
    	if (!localStorage.getItem("logged_in")) {
	    	localStorage.setItem("logged_in", true);
		}
			
    	$("body").removeClass("lockscreen");
    	$("html").removeClass("lockscreen");
    	$("#lockscreen").hide();
    	$("window").off("resize.lockscreen");
    	
    	$("body").scrollTop(0);
    	
    	var userStyles = ["header", "sidebar", "page_header"];
    	
    	for (var i=0; i<userStyles.length; i++) {
	    	var color = localStorage.getItem(userStyles[i] + "_color");
	    	if (color) {
	    		NRS.updateStyle(userStyles[i], color);
	    	} 
    	}

    	var contentHeaderHeight = $(".content-header").height();
		var navBarHeight = $("nav.navbar").height();
        
		$(".content-splitter-right").css("bottom", (contentHeaderHeight+navBarHeight+10)+"px");
    }
    
    NRS.logout = function() {
    	window.location.reload();
    }

    $("#logo, .sidebar-menu a").click(function(event, data) {
    	if ($(this).hasClass("ignore")) {
    		$(this).removeClass("ignore");
    		return;
    	}
    	
    	event.preventDefault();
    	
    	if ($(this).data("toggle") == "modal") {
    		return;
    	}
    	
    	var page = $(this).data("page");
    	          	        	    	
    	if (page == NRS.currentPage) {
    		return;
    	}
    	    	
    	NRS.abortOutstandingRequests();
    	    	
    	$(".page").hide();
    	
    	$("body").scrollTop(0);
    	
    	$("#" + page + "_page").show();
    	
    	
    	$(".content-header h1").find(".loading_dots").remove();
    	    	        	   	
    	var changeActive = !($(this).closest("ul").hasClass("treeview-menu"));
    	
    	if (changeActive) {
	    	var currentActive = $("ul.sidebar-menu > li.active");
	    	
	    	if (currentActive.hasClass("treeview")) {	
	    		currentActive.children("a").first().addClass("ignore").click();
	    	} else {
	    		currentActive.removeClass("active");
	    	}	
    	
	    	if ($(this).attr("id") && $(this).attr("id") == "logo") {
	    		$("#dashboard_link").addClass("active");
	    	} else {
	        	$(this).parent().addClass("active");
	    	}
    	}
    	
    	if (NRS.currentPage != "messages") {
    		$("#inline_message_password").val("");
    	}
    	
    	//NRS.previousPage = NRS.currentPage;
    	NRS.currentPage = page;
    	NRS.currentSubPage = "";
    	  	    	  	
    	if (NRS.pages[page]) {
    		if (data && data.callback) {
    			NRS.pages[page](data.callback);
    		} else if (data) {
    			NRS.pages[page](data);
    		} else {
    			NRS.pages[page]();
    		}
    	}
    });
        
    NRS.pageLoading = function() {
    	if ($.inArray(NRS.currentPage, NRS.loadedBefore) != -1) {
	    	$("#" + NRS.currentPage + "_page .content-header h1").append("<span class='loading_dots'><span>.</span><span>.</span><span>.</span></span>");
	    } else {
		    NRS.loadedBefore.push(NRS.currentPage);
	    }
    }
    
    NRS.pageLoaded = function(callback) {
	    $("#" + NRS.currentPage + "_page .content-header h1").find(".loading_dots").remove();
	    if (callback) {
		    callback();
	    }
    }
    
    $("button.goto-page, a.goto-page").click(function(event) {
    	event.preventDefault();
    	
    	var page = $(this).data("page");
    	    	
    	var $link = $("ul.sidebar-menu a[data-page=" + page + "]");
    	
    	if ($link.length) {
	    	$link.trigger("click");
		} else {
			NRS.currentPage = page;
			$("ul.sidebar-menu a.active").removeClass("active");
			$(".page").hide();
			$("#" + page + "_page").show();
			if (NRS.pages[page]) {
				NRS.pages[page]();
			}
		}
    });
    
    NRS.userInfoModal = {"user": 0};
    
    $("#blocks_table, #polls_table, #contacts_table, #transactions_table, #dashboard_transactions_table, #asset_account, #asset_exchange_ask_orders_table, #asset_exchange_bid_orders_table").on("click", "a[data-user]", function(e) {
		e.preventDefault();
    	
    	if (NRS.fetchingModalData) {
	    	return;
    	}
    	
    	NRS.fetchingModalData = true;
    	
    	NRS.userInfoModal.user = $(this).data("user");
    	
    	$("#user_info_modal_account").html(String(NRS.userInfoModal.user).escapeHTML());
    	
    	$("#user_info_modal_actions button").data("account", NRS.userInfoModal.user);
    	
    	if (NRS.userInfoModal.user in NRS.contacts) {
	    	$("#user_info_modal_add_as_contact").hide();
    	} else {
	    	$("#user_info_modal_add_as_contact").show();
    	}

    	NRS.sendRequest("getAccount", {"account": NRS.userInfoModal.user}, function(response) {
	    	var balance;
	    	
	    	if (response.balance > 0) {
	    		balance = response.balance;
	    	} else {
	    		balance = response.effectiveBalance;
	    	}

	    	if (!balance) {
	    		balance = 0;
	    	}

	    	balance /= 100;

	    	if (balance == 0) {
	    		$("#user_info_modal_balance").html("0");
	    	} else {
	    		$("#user_info_modal_balance").html(NRS.formatAmount(balance) + " NXT");
	    	}	
	    		
	    	$("#user_info_modal").modal("show");
	    	
	    	NRS.fetchingModalData = false;
    	});
    	
		$("#user_info_modal_transactions").show();
    
    	NRS.userInfoModal.transactions();
    });   
    
    $("#user_info_modal").on("hidden.bs.modal", function(e) {
		$(this).find(".user_info_modal_content").hide();
		$(this).find(".user_info_modal_content table tbody").empty();
		$(this).find(".user_info_modal_content:not(.data-loading,.data-never-loading)").addClass("data-loading");
		$(this).find("ul.nav li.active").removeClass("active");
		$("#user_info_transactions").addClass("active");
		NRS.userInfoModal.user = 0;
    });
    
    $("#user_info_modal ul.nav li").click(function(e) {
	    e.preventDefault();
	    
	    var tab = $(this).data("tab");
	    
	    $(this).siblings().removeClass("active");
	    $(this).addClass("active");
	    
		$(".user_info_modal_content").hide();
		
		var content = $("#user_info_modal_" + tab);
		
		content.show();
		
		if (content.hasClass("data-loading")) {		
			NRS.userInfoModal[tab]();
		}
    });
    
    /*some duplicate methods here...*/
    NRS.userInfoModal.transactions = function(type) {	
    	NRS.sendRequest("getAccountTransactionIds",  {"account": NRS.userInfoModal.user, "timestamp": 0}, function(response) {
    		if (response.transactionIds && response.transactionIds.length) {
    			var transactions = {};
    			var nr_transactions = 0;
    			
    			var transactionIds = response.transactionIds.reverse().slice(0, 100);
    			    			
    			for (var i=0; i<transactionIds.length; i++) {
    				NRS.sendRequest("getTransaction", {"transaction": transactionIds[i]}, function(transaction, input) {
    					/*
    					if (NRS.currentPage != "transactions") {
    						transactions = {};
    						return;
    					}*/
    					    					    					
    					transactions[input.transaction] = transaction;
    					nr_transactions++;
    					
    					if (nr_transactions == transactionIds.length) {
    						var rows = "";
    						
    						for (var i=0; i<nr_transactions; i++) {
    							var transaction = transactions[transactionIds[i]];
    							
    							var transactionType = "Unknown";
    							
    							if (transaction.type == 0) {
    								transactionType = "Ordinary payment";
    							} else if (transaction.type == 1) {
    								switch (transaction.subtype) {
    									case 0:
    										transactionType = "Arbitrary message";
    										break;
    									case 1:
    										transactionType = "Alias assignment";
    										break;
    									case 2:
    										transactionType = "Poll creation";
    										break;
    									case 3:
    										transactionType = "Vote casting";
    										break;
    								}
    							} else if (transaction.type == 2) {
    								switch (transaction.subtype) {
    									case 0: 
    										transactionType = "Asset issuance";
    										break;
    									case 1: 
    										transactionType = "Asset transfer";
    										break;
    									case 2: 
    										transactionType = "Ask order placement";
    										break;
    									case 3: 
    										transactionType = "Bid order placement";
    										break;
    									case 4:
    										transactionType = "Ask order cancellation";
    										break;
    									case 5: 
    										transactionType = "Bid order cancellation";
    										break;
    								}
    							}
    						    
    						   	var receiving = transaction.recipient == NRS.userInfoModal.user;
    						   	var account = (receiving ? String(transaction.sender).escapeHTML() : String(transaction.recipient).escapeHTML());
    						   	
    							rows += "<tr><td>" + NRS.formatTimestamp(transaction.timestamp) + "</td><td>" + transactionType + "</td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fa fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fa fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td " + (transaction.type == 0 && receiving ? " style='color:#006400;'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + NRS.formatAmount(transaction.amount) + "</td><td " + (!receiving ? " style='color:red'" : "") + ">" + NRS.formatAmount(transaction.fee) + "</td><td>" + NRS.getAccountTitle(account) + "</td></tr>";
    						}
    						
    						$("#user_info_modal_transactions_table tbody").empty().append(rows);
    						NRS.dataLoadFinished($("#user_info_modal_transactions_table"));
    					}
    				});
    				
    				/*
    				if (NRS.currentPage != "transactions") {
    					transactions = {};
    					return;
    				}*/
    			}
    		} else {
    			$("#user_info_modal_transactions_table tbody").empty();
    			NRS.dataLoadFinished($("#user_info_modal_transactions_table"));
    		}
    	});
    }
    	
	NRS.userInfoModal.aliases = function() {
	    NRS.sendRequest("listAccountAliases", {"account": NRS.userInfoModal.user}, function(response) {
	    	if (response.aliases && response.aliases.length) {
		    	var aliases = response.aliases;
	    	
		    	aliases.sort(function(a, b) {
		    		if (a.alias.toLowerCase() > b.alias.toLowerCase()) {
		    			return 1;
		    		} else if (a.alias.toLowerCase() < b.alias.toLowerCase()) {
		    			return -1;
		    		} else {
		    			return 0;
		    		}
		    	});
	    	
		    	var rows = "";
		    	
		    	var alias_account_count = 0,  alias_uri_count = 0, empty_alias_count = 0, alias_count = aliases.length;
		    	
		    	for (var i=0; i<alias_count; i++) {
		    		var alias = aliases[i];
		    		
		    		rows += "<tr data-alias='" + alias.alias.toLowerCase().escapeHTML() + "'><td class='alias'>" + alias.alias.escapeHTML() + "</td><td class='uri'>" + (alias.uri.indexOf("http") === 0 ? "<a href='" + String(alias.uri).escapeHTML() + "' target='_blank'>" + String(alias.uri).escapeHTML() + "</a>" : String(alias.uri).escapeHTML()) + "</td></tr>";
		    		if (!alias.uri) {
		    			empty_alias_count++;
		    		} else if (alias.uri.indexOf("http") === 0) { 
		    			alias_uri_count++;
		    		} else if (alias.uri.indexOf("acct:") === 0 || alias.uri.indexOf("nacc:") === 0) {
		    			alias_account_count++;
		    		}
		    	}
		    	
		    	$("#user_info_modal_aliases_table tbody").empty().append(rows);
		    	NRS.dataLoadFinished($("#user_info_modal_aliases_table"));
		    } else {
			    $("#user_info_modal_aliases_table tbody").empty();
			    NRS.dataLoadFinished($("#user_info_modal_aliases_table"));
		    }
	    });
	}
	
	NRS.userInfoModal.assets = function() {
		NRS.sendRequest("getAccount", {"account": NRS.userInfoModal.user}, function(response) {
			/*
			if (NRS.currentPage != "my_assets") {
				return;
			}*/
		
			if (response.assetBalances && response.assetBalances.length) {
				var assets = [];
				var nr_assets = 0;
				var ignored_assets = 0;
								    			
				for (var i=0; i<response.assetBalances.length; i++) {
					if (response.assetBalances[i].balance == 0) {
						ignored_assets++;
						
						if (nr_assets + ignored_assets == response.assetBalances.length) {
							NRS.userInfoModal.assetsLoaded(assets);
						}
						continue;
					}
											
					NRS.sendRequest("getAsset", {"asset": response.assetBalances[i].asset, "_extra": {"balance": response.assetBalances[i].balance}}, function(asset, input) {
						/*
						if (NRS.currentPage != "my_assets") {
							return;
						}*/
						
					
					   	asset.asset = input.asset;
					   	asset.balance = input["_extra"].balance;
					   			
					   	assets[nr_assets] = asset;
					   	nr_assets++;
					   	
					   	if (nr_assets + ignored_assets == response.assetBalances.length) {
						   	NRS.userInfoModal.assetsLoaded(assets);
						}
					});
					
					/*
					if (NRS.currentPage != "my_assets") {
						return;
					}*/
				}
			} else {
				$("#user_info_modal_assets_table tbody").empty();
				NRS.dataLoadFinished($("#user_info_modal_assets_table"));
			}
		});
	}
	
	NRS.userInfoModal.assetsLoaded = function(assets) {
		var rows = "";
				
		assets.sort(function(a, b) {
			if (a.name.toLowerCase() > b.name.toLowerCase()) {
				return 1;
			} else if (a.name.toLowerCase() < b.name.toLowerCase()) {
				return -1;
			} else {
				return 0;
			}
		});
							   		
		for (var i=0; i<assets.length; i++) {
			var asset = assets[i];
			
			var percentageAsset = parseFloat(asset.balance / asset.quantity);
			percentageAsset = Math.round(percentageAsset * 10000000) / 100000;
		
			rows += "<tr><td>" + asset.name.escapeHTML() + "</td><td>" + NRS.formatAmount(asset.balance) + "</td><td>" + NRS.formatAmount(asset.quantity) + "</td><td>" + percentageAsset + "%</td></tr>";
		}
		
		$("#user_info_modal_assets_table tbody").empty().append(rows);
		NRS.dataLoadFinished($("#user_info_modal_assets_table"));
	}
   	    
    $("#blocks_table, #dashboard_blocks_table").on("click", "a[data-block]", function(event) {
    	event.preventDefault();
    	
    	if (NRS.fetchingModalData) {
	    	return;
    	}
    	
    	NRS.fetchingModalData = true;
    	
    	var blockHeight = $(this).data("block");
    	
    	var block = $(NRS.blocks).filter(function(){
    	   return parseInt(this.height) == parseInt(blockHeight);
    	}).get(0);
    		   		   			   	
	   	if (block.transactions.length) {
	   		var transactions = {};
	   		var nrTransactions = 0;

	   		for (var i=0; i<block.transactions.length; i++) {
	   			NRS.sendRequest("getTransaction", {"transaction": block.transactions[i]}, function(transaction, input) {	   
		   			nrTransactions++;
		   			transactions[input.transaction] = transaction;
				
		   			if (nrTransactions == block.transactions.length) {
			   			var rows = "";
			   			
			   			for (var i=0; i<nrTransactions; i++) {
							var transaction = transactions[block.transactions[i]];

							rows += "<tr><td>" + NRS.formatTime(transaction.timestamp) + "</td><td>" + NRS.formatAmount(transaction.amount) + "</td><td>" + NRS.formatAmount(transaction.fee) + "</td><td>" + NRS.getAccountTitle(transaction.recipient) + "</td><td>" + NRS.getAccountTitle(transaction.sender) + "</td></tr>";
						}

					   	$("#block_info_transactions_table tbody").empty().append(rows);
				    	$("#block_info_modal").modal("show");
				    	
				    	NRS.fetchingModalData = false;
		   			}
	   			});
	   		}
	   	} else {
		   	NRS.fetchingModalData = false;
	   	}
	}); 
          
    /* NEWS *PAGE */
    NRS.pages.news = function() {
    	var currentTime = new Date().getTime();
    	
    	if (currentTime - NRS.newsRefresh > 60*60*10) { //10 minutes before refreshing..
    		NRS.newsRefresh = currentTime;
    		
    		$(".rss_news").empty().addClass("data-loading").html("<img src='img/loading_indicator.gif' width='32' height='32' />");
    		
    		var settings = {
	    		"limit": 5, 
	    		"layoutTemplate": "<div class='list-group'>{entries}</div>",
	    		"entryTemplate": "<a href='{url}' target='_blank' class='list-group-item'><h4 class='list-group-item-heading'>{title}</h4><p class='list-group-item-text'>{shortBodyPlain}</p></a>"
    		};
    		
    		var settingsReddit = {
	    		"limit": 7,  
	    		"filterLimit": 5,
	    		"layoutTemplate": "<div class='list-group'>{entries}</div>",
	    		"entryTemplate": "<a href='{url}' target='_blank' class='list-group-item'><h4 class='list-group-item-heading'>{title}</h4><p class='list-group-item-text'>{shortBodyReddit}</p></a>",
	    		"tokens": {
	    			"shortBodyReddit": function(entry, tokens) {
	    				return entry.contentSnippet.replace("&lt;!-- SC_OFF --&gt;", "").replace("&lt;!-- SC_ON --&gt;", "").replace("[link]", "").replace("[comment]", "");
	    			}
	    		},
	    		"filter": function(entry, tokens) {
	    			return tokens.title.indexOf("Donations toward") == -1 && tokens.title.indexOf("NXT tipping bot has arrived") == -1
	    		}
	    	};
    		    		
    		$("#nxtcoins_news").rss("http://www.nxtcoins.nl/feed/?cat=-17", settings, NRS.newsLoaded);
    		$("#nxtcrypto_news").rss("http://info.nxtcrypto.org/feed/", settings, NRS.newsLoaded);
    		$("#reddit_news").rss("http://www.reddit.com/r/NXT/.rss", settingsReddit, NRS.newsLoaded);
    		$("#nxtcrypto_forums_news").rss("http://forums.nxtcrypto.org/feed.php?mode=topics", settings, NRS.newsLoaded);
    		$("#nxtcoin_blogspot_news").rss("http://nxtcoin.blogspot.com/feeds/posts/default", settings, NRS.newsLoaded);
    		$("#nextcoin_forums_news").rss("https://nextcoin.org/index.php?type=rss;action=.xml;sa=news;", settings, NRS.newsLoaded);
    		$("#nxter_news").rss("http://nxter.org/feed/", settings, NRS.newsLoaded);
    	}
    }
    
    NRS.newsLoaded = function($el) {
    	$el.removeClass("data-loading").find("img").remove();
    }
    
    /* ASSET EXCHANGE PAGE */
    NRS.pages.asset_exchange = function(callback) {   
    	NRS.pageLoading();
    	
    	var $active = $("#asset_exchange_sidebar a.active");
    	
    	var activeAsset = false;
    	
    	if ($active.length) {
	    	activeAsset = $active.data("asset");	
    	}
    	
        $(".content.content-stretch:visible").width($(".page:visible").width());
				
		if (NRS.databaseSupport) {
			NRS.database.select("assets", null, function(dbAssets) {			
				$.each(dbAssets, function(index, asset) {
					NRS.assets[asset.assetId] = asset;
				});
				
				dbAssets = null;
	
				NRS.loadAssetExchangeSidebar(callback, activeAsset);
			});
		} else {
			NRS.loadAssetExchangeSidebar(callback, activeAsset);
		}
    }    		
    
    NRS.loadAssetExchangeSidebar = function(callback, activeAsset) {
		NRS.sendRequest("getAssetIds+", function(response) {
    		if (response.assetIds && response.assetIds.length) {
	    		if (NRS.currentPage != "asset_exchange") {
	    			return;
	    		}
	    		
    			var assets = [];
    			var nr_assets = 0;
    			
    			for (var i=0; i<response.assetIds.length; i++) {
    				if (response.assetIds[i] in NRS.assets) {
    					assets[nr_assets] = {"id": response.assetIds[i], "name": NRS.assets[response.assetIds[i]].name.toLowerCase(), "groupName": NRS.assets[response.assetIds[i]].groupName.toLowerCase()};
    					
    					nr_assets++;
    					
    					if (nr_assets == response.assetIds.length) {
	    					NRS.assetExchangeSidebarLoaded(assets, callback, activeAsset);
    					}
	    				continue;
    				} else {
	    				NRS.sendRequest("getAsset+", {"asset": response.assetIds[i]}, function(asset, input) {
	    				   asset.groupName = "";
	    					
	    				   NRS.assets[input.asset] = asset;
	    				   assets[nr_assets] = {"id": input.asset, "name": asset.name.toLowerCase(), "groupName": asset.groupName.toLowerCase()};
	    				   
	    				   asset.assetId = input.asset;

						   if (NRS.databaseSupport) {
		    				   NRS.database.insert("assets", asset);
						   }
						   
	    				   nr_assets++;
	    				   
	    				   if (nr_assets == response.assetIds.length) {
	    				   		NRS.assetExchangeSidebarLoaded(assets, callback, activeAsset);
	    				   }
	    				});
	    				
	    				if (NRS.currentPage != "asset_exchange") {
	    					return;
	    				}
	    			}
	    		}
    		}
    	});
	}
    		
	NRS.assetExchangeSidebarLoaded = function(assets, callback, activeAsset) {
		var rows = "";
		    				   		
   		assets.sort(function(a, b) {
   			if (!a.groupName && !b.groupName) {
	   			if (a.name > b.name) {
	   				return 1;
	   			} else if (a.name < b.name) {
	   				return -1;
	   			} else {
	   				return 0;
	   			}
   			} else if (!a.groupName) {
	   			return 1;
   			} else if (!b.groupName) {
	   			return -1; 
   			} else if (a.groupName > b.groupName) {
	   			return 1;
   			} else if (a.groupName < b.groupName) {
	   			return -1;
   			} else {
	   			if (a.name > b.name) {
	   				return 1;
	   			} else if (a.name < b.name) {
	   				return -1;
	   			} else {
	   				return 0;
	   			}
	   		}
   		});
   		
   		var lastGroup = "";
   		var ungrouped = true;
   		var isClosedGroup = false;
   		   		
   		for (var i=0; i<assets.length; i++) {
   			var assetId = assets[i].id;
   			
   			var asset = NRS.assets[assetId];
   			
   			if (asset.groupName.toLowerCase() != lastGroup) {
   				var to_check = (asset.groupName ? asset.groupName : "undefined");
   				
   				if (NRS.closedGroups.indexOf(to_check) != -1) {
	   				isClosedGroup = true;
   				} else {
   					isClosedGroup = false;
   				}
   				   				   				
   				if (asset.groupName) {
   					ungrouped = false;
		   			rows += "<a href='#' class='list-group-item list-group-item-header' data-context='asset_exchange_sidebar_group_context' data-groupname='" + asset.groupName.escapeHTML() + "' data-closed='" + isClosedGroup + "'><h4 class='list-group-item-heading'>" + asset.groupName.toUpperCase().escapeHTML() + " <i class='fa pull-right fa-angle-" + (isClosedGroup ? "right" : "down") + "'></i></h4></a>";
   				} else {
   					ungrouped = true;
	   				rows += "<a href='#' class='list-group-item list-group-item-header no-context' data-closed='" + isClosedGroup + "'><h4 class='list-group-item-heading'>UNGROUPED <i class='fa pull-right fa-angle-" + (isClosedGroup ? "right" : "down") + "'></i></h4></a>";
   				}
	   			lastGroup = asset.groupName.toLowerCase();
   			}
   			    				   			
   			rows += "<a href='#' class='list-group-item list-group-item-" + (ungrouped ? "ungrouped" : "grouped") + "' data-asset='" + String(assetId).escapeHTML() + "'" + (!ungrouped ? " data-groupname='" + asset.groupName.escapeHTML() + "'" : "") + (isClosedGroup ? " style='display:none'" : "") + " data-wuuut='" + isClosedGroup + "'><h4 class='list-group-item-heading'>" + asset.name.escapeHTML() + "</h4><p class='list-group-item-text'>Quantity: " + NRS.formatAmount(asset.quantity) + "</p></a>";
   		}
   		
   		var currentActiveAsset = $("#asset_exchange_sidebar a.active");
		
		if (currentActiveAsset.length) {
			currentActiveAsset = currentActiveAsset.data("asset");
		} else {
			currentActiveAsset = false;
		}
		
   		$("#asset_exchange_sidebar").empty().append(rows);
   		   		
   		if (activeAsset !== false) {
	   		if (activeAsset !== currentActiveAsset) {
	   			$("#asset_exchange_sidebar a[data-asset=" + currentActiveAsset + "]").addClass("active");
	   		} else {
	   			$("#asset_exchange_sidebar a[data-asset=" + activeAsset + "]").addClass("active").trigger("click", [{"refresh": true}]);
	   		}
   		}
   		
   		NRS.pageLoaded(callback);
	}
      
    NRS.incoming.asset_exchange = function() {
    	var $active = $("#asset_exchange_sidebar a.active");
    	
    	if ($active.length) {
    		$active.trigger("click", [{"refresh": true}]);
    	}
    }
    
    $("#asset_exchange_sidebar").on("click", "a", function(e, data) {
    	e.preventDefault();
    	    	
    	var assetId = $(this).data("asset");
    	    	
    	if (!assetId) {
    		if (NRS.databaseSupport) {
	    	  	var group = $(this).data("groupname");
	    	  	var closed = $(this).data("closed");
	    	  	
	    	  	if (!group) {
	    	  		var $links = $("#asset_exchange_sidebar a.list-group-item-ungrouped");
	    	  	} else {
	    	  		var $links = $("#asset_exchange_sidebar a.list-group-item-grouped[data-groupname=" + group.escapeHTML() + "]");
	    	  	}
	    	  	
	    	  	if (!group) {
		    	  	group = "undefined";
	    	  	}
	    	  	
		  		if (closed) {
		  			var pos = NRS.closedGroups.indexOf(group);
		  			if (pos >= 0) {
			  			NRS.closedGroups.splice(pos);
		  			}
	    	  		$(this).data("closed", "");
	    	  		$(this).find("i").removeClass("fa-angle-right").addClass("fa-angle-down");
	    	  		$links.show();
		  		} else {
		  			NRS.closedGroups.push(group);
	    	  		$(this).data("closed", true);
	    	  		$(this).find("i").removeClass("fa-angle-down").addClass("fa-angle-right");
	    	  		$links.hide();
	    	  	}
	    	  	
	    	  	NRS.database.update("data", {"contents": NRS.closedGroups.join("#")}, [{"id": "closed_groups"}]);
			}
		  	
	    	return;
    	}
    	
    	assetId = assetId.escapeHTML();
    	
    	NRS.abortOutstandingRequests(true);
    	
    	var asset = NRS.assets[assetId];
    	    	
    	NRS.currentAsset = {"assetId": assetId};
		NRS.currentSubPage = assetId;
		
    	var asset_account = String(asset.account).escapeHTML();

		var refresh = (data && data.refresh);
				
    	if (!refresh) {
	    	$("#asset_exchange_sidebar a.active").removeClass("active");
	    	$(this).addClass("active");
	    	
	    	$("#no_asset_selected, #loading_asset_data").hide();
	    	$("#asset_details").show();
	    	
	    	$("#asset_details").parent().animate({"scrollTop": 0}, 0);
    	    	    	    
    	    $("#asset_account").html("<a href='#' data-user='" + asset_account + "' class='user_info'>" + asset_account + "</a>");
	    	$("#asset_id").html(assetId.escapeHTML());
	    	$("#asset_name").html(String(asset.name).escapeHTML());
	    	$("#asset_description").html(String(asset.description).escapeHTML());    	
	    	$(".asset_name").html(String(asset.name).escapeHTML());
	    	$("#sell_asset_button").data("asset", assetId);
	    	$("#buy_asset_button").data("asset", assetId);

	    	$("#sell_asset_price").val("");
			$("#buy_asset_price").val("");
			$("#buy_asset_quantity").val("0");
			$("#buy_asset_total").val("0");
			$("#sell_asset_quantity").val("0");
			$("#sell_asset_total").val("0");
    	
	    	$("#asset_exchange_ask_orders_table tbody").empty();
	     	$("#asset_exchange_bid_orders_table tbody").empty();
	     	$("#asset_exchange_trade_history_table tbody").empty();
	    	$("#asset_exchange_ask_orders_table").parent().addClass("data-loading").removeClass("data-empty");
	       	$("#asset_exchange_bid_orders_table").parent().addClass("data-loading").removeClass("data-empty");
	    	$("#asset_exchange_trade_history_table").parent().addClass("data-loading").removeClass("data-empty");
	    	
	    	$(".data-loading img.loading").hide();
    	
	    	setTimeout(function() {
	    		$(".data-loading img.loading").fadeIn(200);
	    	}, 200);
    	}
    	
    	var responsesReceived = 0;
    	var balance   		  = -1;
    	
    	NRS.sendRequest("getAccount+" + assetId, {"account": NRS.account}, function(response) {    		
			if (response.balance > 0) {
				balance = response.balance;
			} else {
				balance = response.effectiveBalance;
			}
			
			if (!balance) {
				balance = 0;
			}
						
			balance /= 100;
    		 		
    		if (balance == 0) {
	    		$("#your_nxt_balance").html("0");
    		} else {
	    		$("#your_nxt_balance").html(NRS.formatAmount(balance));
    		}		
    		
    		var foundAssetBalance = false;
    		
    		if (response.assetBalances) {
    			for (var i=0; i<response.assetBalances.length; i++) {
    				var asset = response.assetBalances[i];
    				
    				if (asset.asset == assetId) {
    					$("#your_asset_balance").html(NRS.formatAmount(asset.balance));
    					foundAssetBalance = true;
    					break;
    				}
    			}
    		}
    		
    		if (!foundAssetBalance) {
    			$("#your_asset_balance").html("0");
    		}
    	});
    						
    	NRS.sendRequest("getAskOrderIds+" + assetId, {"asset": assetId, "timestamp": 0, "limit": 50, "_extra": {"refresh": refresh}}, function(response, input) {
    		var refresh = input["_extra"].refresh;

    		if (response.askOrderIds && response.askOrderIds.length) {
    			var realNrAskOrders = response.askOrderIds.length;
    			var askOrderIds 	= response.askOrderIds;
    			var askOrders  		= {};
				var nrAskOrders 	= 0;

    			$("#sell_orders_count").html("(" + realNrAskOrders + (realNrAskOrders == 50 ? "+" : "") + ")");
    			
    			for (var i=0; i<askOrderIds.length; i++) {
    				NRS.sendRequest("getAskOrder+" + assetId, {"order": askOrderIds[i], "_extra": {"refresh": refresh}}, function(order, input) {    					
    					askOrders[input.order] = order;
    					nrAskOrders++;
    					
    					if (nrAskOrders == askOrderIds.length) {
	    					var rows = "";

							var refresh = input["_extra"].refresh;

    						for (var i=0; i<nrAskOrders; i++) {
    							var askOrder = askOrders[askOrderIds[i]];
								
								askOrder.price /= 100;
    				
								if (i==0) {
									if (!input["_extra"].refresh) {
										$("#buy_asset_price").val(askOrder.price);
									}
								} 
								
								var cancelled = false;
																
								if (askOrder.account == NRS.account && NRS.tentative.cancelled_orders.length && $.inArray(askOrderIds[i], NRS.tentative.cancelled_orders) != -1) {
									cancelled = true;	
								}
																
								var className = "";
								
								if (askOrder.account == NRS.account) {
									className += "your-order";
								} 
								
								if (cancelled) {
									className += " tentative tentative-crossed";
								}
																
								rows += "<tr class='" + className + "' data-quantity='" + String(askOrder.quantity).escapeHTML() + "' data-price='" + String(askOrder.price).escapeHTML() + "'><td>" + (askOrder.account == NRS.account ? "<strong>You</strong>" : "<a href='#' data-user='" + String(askOrder.account).escapeHTML() + "' class='user_info'>" +  (askOrder.account == asset_account ? "Asset Issuer" : NRS.getAccountTitle(askOrder.account)) + "</a>") + "</td><td>" + NRS.formatAmount(askOrder.quantity) + "</td><td>" + NRS.formatAmount(askOrder.price) + "</td><td>" + NRS.formatAmount(askOrder.price * askOrder.quantity, true) + "</tr>";
							}
							    			
			    			$("#asset_exchange_ask_orders_table tbody").empty().append(rows);
			    			
			    			if (NRS.tentative.ask_orders.length) {
			    				for (var key in NRS.tentative.ask_orders) {
			    					var ask_order = NRS.tentative.ask_orders[key];
			    					
			    					if (ask_order.asset != assetId) {
			    						continue;
			    					}
			    					
				    				var $rows = $("#asset_exchange_ask_orders_table tbody").find("tr");
				
									var rowToAdd = "<tr class='tentative' data-transaction='" + String(ask_order.transaction).escapeHTML() + "' data-quantity='" + String(ask_order.quantity).escapeHTML() + "' data-price='" + (ask_order.price/100) + "'><td>You - <strong>Tentative Order</strong></td><td>" + NRS.formatAmount(ask_order.quantity) + "</td><td>" + NRS.formatAmount(ask_order.price/100) + "</td><td>" + NRS.formatAmount((ask_order.price/100) * ask_order.quantity, true) + "</td></tr>";
		
									var rowAdded = false;
		
									if ($rows.length) {
										$rows.each(function() {
											var row_price = parseFloat($(this).data("price"));
											if (ask_order.price / 100 < row_price) {
												$(this).before(rowToAdd);
												rowAdded = true;
												return false;
											}
										});
									}
									
									if (!rowAdded) {
										$("#asset_exchange_ask_orders_table tbody").append(rowToAdd);
									}
								}
			    			}
			    			
			    			NRS.dataLoadFinished($("#asset_exchange_ask_orders_table"), !refresh);
    					}
    				});
    			}
    		} else {    	
			    if (NRS.tentative.ask_orders.length) {
			    	var rows = "";
			    	
					for (var key in NRS.tentative.ask_orders) {
						var ask_order = NRS.tentative.ask_orders[key];
						
						if (ask_order.asset != assetId) {
							continue;
						}
							
						rows += "<tr class='tentative' data-transaction='" + String(ask_order.transaction).escapeHTML() + "' data-quantity='" + String(ask_order.quantity).escapeHTML() + "' data-price='" + (ask_order.price/100) + "'><td>You - <strong>Tentative Order</strong></td><td>" + NRS.formatAmount(ask_order.quantity) + "</td><td>" + NRS.formatAmount(ask_order.price/100) + "</td><td>" + NRS.formatAmount((ask_order.price/100) * ask_order.quantity, true) + "</td></tr>";
					}
					
					$("#asset_exchange_ask_orders_table tbody").empty().append(rows);
				}

		
    			NRS.dataLoadFinished($("#asset_exchange_ask_orders_table"), !refresh);
    			//refresh
    			$("#buy_asset_price").val("0");
    			$("#sell_orders_count").html("");
    		}
    	});
    	
    	NRS.sendRequest("getBidOrderIds+" + assetId, {"asset": assetId, "timestamp": 0, "limit": 50, "_extra": {"refresh": refresh}}, function(response, input) {
			var refresh = input["_extra"].refresh;

    		if (response.bidOrderIds && response.bidOrderIds.length) {
    			var realNrBidOrders = response.bidOrderIds.length;
    			var bidOrderIds 	= response.bidOrderIds;
    			var bidOrders       = {};
    			var nrBidOrders	    = 0;
    			
    			$("#buy_orders_count").html("(" + realNrBidOrders + (realNrBidOrders == 50 ? "+" : "") + ")");
    			
    			for (var i=0; i<bidOrderIds.length; i++) {
    				NRS.sendRequest("getBidOrder+" + assetId, {"order": bidOrderIds[i], "_extra": {"refresh": refresh}}, function(order, input) {
    					bidOrders[input.order] = order;
    					nrBidOrders++;
    					
    					if (nrBidOrders == bidOrderIds.length) {
	    					var rows = "";
	    					
	    					var refresh = input["_extra"].refresh;
	    					
	    					for (var i=0; i<nrBidOrders; i++) {
		    					var bidOrder = bidOrders[bidOrderIds[i]];
		    					
		    					bidOrder.price /= 100;
		    					
		    					if (i==0) {
		    						if (!refresh) {
										$("#sell_asset_price").val(bidOrder.price);
									}
								}
								
								var cancelled = false;
								
								if (bidOrder.account == NRS.account && NRS.tentative.cancelled_orders.length && $.inArray(bidOrderIds[i], NRS.tentative.cancelled_orders) != -1) {
									cancelled = true;	
								 }
								 								
								var className = "";
								
								if (bidOrder.account == NRS.account) {
									className += "your-order";
								} 
								
								if (cancelled) {
									className += " tentative tentative-crossed";
								}
								
								rows += "<tr class='" + className + "' data-quantity='" + String(bidOrder.quantity).escapeHTML() + "' data-price='" + String(bidOrder.price).escapeHTML() + "'><td>" + (bidOrder.account == NRS.account ? "<strong>You</strong>" : "<a href='#' data-user='" + String(bidOrder.account).escapeHTML() + "' class='user_info'>" +  (bidOrder.account == asset_account ? "Asset Issuer" : NRS.getAccountTitle(bidOrder.account)) + "</a>") + "</td><td>" + NRS.formatAmount(bidOrder.quantity) + "</td><td>" + NRS.formatAmount(bidOrder.price) + "</td><td>" + NRS.formatAmount(bidOrder.price * bidOrder.quantity, true) + "</tr>";
								
	    					}
	    					
	    					$("#asset_exchange_bid_orders_table tbody").empty().append(rows);
	    						    					
	    					if (NRS.tentative.bid_orders.length) {
			    				for (var key in NRS.tentative.bid_orders) {
			    					var bid_order = NRS.tentative.bid_orders[key];
			    								  			    								    					
			    					if (bid_order.asset != assetId) {
			    						continue;
			    					}
			    								    					
				    				var $rows = $("#asset_exchange_bid_orders_table tbody").find("tr");
				
									var rowToAdd = "<tr class='tentative' data-transaction='" + String(bid_order.transaction).escapeHTML() + "' data-quantity='" + String(bid_order.quantity).escapeHTML() + "' data-price='" + (bid_order.price/100) + "'><td>You - <strong>Tentative Order</strong></td><td>" + NRS.formatAmount(bid_order.quantity) + "</td><td>" + NRS.formatAmount(bid_order.price/100) + "</td><td>" + NRS.formatAmount((bid_order.price/100) * bid_order.quantity, true) + "</td></tr>";
		
									var rowAdded = false;
		
									if ($rows.length) {
										$rows.each(function() {
											var row_price = parseFloat($(this).data("price"));
																						
											if (bid_order.price / 100 > row_price) {
												$(this).before(rowToAdd);
												rowAdded = true;
												return false;
											}
										});
									}
									
									if (!rowAdded) {
										$("#asset_exchange_bid_orders_table tbody").append(rowToAdd);
									}
								}
			    			}
			    			
							NRS.dataLoadFinished($("#asset_exchange_bid_orders_table"), !refresh);
						}
    				});
    			}
    		} else {    
    			if (NRS.tentative.bid_orders.length) {
			    	var rows = "";
			    	
					for (var key in NRS.tentative.bid_orders) {
						var bid_orders = NRS.tentative.bid_orders[key];
						
						if (bid_orders.asset != assetId) {
							continue;
						}
							
						rows += "<tr class='tentative' data-transaction='" + String(bid_orders.transaction).escapeHTML() + "' data-quantity='" + String(bid_orders.quantity).escapeHTML() + "' data-price='" + (bid_orders.price/100) + "'><td>You - <strong>Tentative Order</strong></td><td>" + NRS.formatAmount(bid_orders.quantity) + "</td><td>" + NRS.formatAmount(bid_orders.price/100) + "</td><td>" + NRS.formatAmount((bid_orders.price/100) * bid_orders.quantity, true) + "</td></tr>";
					}
					
					$("#asset_exchange_bid_orders_table tbody").empty().append(rows);
				}
			
    		   	NRS.dataLoadFinished($("#asset_exchange_bid_orders_table"), !refresh);
    		   	//refresh
    			$("#sell_asset_price").val("0");
    			$("#buy_orders_count").html("");
    		}
    	});
    	
    	NRS.sendRequest("getTrades+" + assetId, {"asset": assetId, "firstIndex": 0, "_extra": {"refresh": refresh}}, function(response, input) {    		
    		if (response.trades && response.trades.length) {
    			var trades = response.trades.reverse().slice(0, 50);
    			var nrTrades = trades.length; 
    			
				var refresh = input["_extra"].refresh;

    			var rows = "";
    			
    			for (var i=0; i<nrTrades; i++) {
    				trades[i].price /= 100;
    				
    				rows += "<tr><td>" + NRS.formatTimestamp(trades[i].timestamp) + "</td><td>" + NRS.formatAmount(trades[i].quantity) + "</td><td class='asset_price'>" + NRS.formatAmount(trades[i].price) + "</td><td>" + NRS.formatAmount(trades[i].price * trades[i].quantity, true) + "</td><td>" + String(trades[i].askOrderId).escapeHTML() + "</td><td>" + String(trades[i].bidOrderId).escapeHTML() + "</td></tr>";
    			}
    			
    			$("#asset_exchange_trade_history_table tbody").empty().append(rows);
    			NRS.dataLoadFinished($("#asset_exchange_trade_history_table"), !refresh);
    		} else {
    			$("#asset_exchange_trade_history_table tbody").empty();
    		   	NRS.dataLoadFinished($("#asset_exchange_trade_history_table"), !refresh);
    		}
    	});
    });
    
    $("#buy_asset_box .box-header, #sell_asset_box .box-header").click(function(e) {
    	e.preventDefault();
        //Find the box parent        
        var box = $(this).parents(".box").first();
        //Find the body and the footer
        var bf = box.find(".box-body, .box-footer");
        if (!box.hasClass("collapsed-box")) {
            box.addClass("collapsed-box");
            bf.slideUp();
        } else {
            box.removeClass("collapsed-box");
            bf.slideDown();
        }
    });
    
   	$("#asset_exchange_ask_orders_table tbody").on("click", "td", function(e) {   		
   		var $target = $(e.target);
   		   		
   		if ($target.prop("tagName").toLowerCase() == 'a') {
	   		return;
   		}
   		
	   	var $tr = $target.closest("tr");

		var price = $tr.data("price");
		var quantity = $tr.data("quantity");
				
		$("#sell_asset_price").val(price);
		$("#buy_asset_price").val(price);
		$("#buy_asset_quantity").val(quantity);
		$("#buy_asset_total").val(Math.round(price*quantity*100)/100);
		
		var box = $("#buy_asset_box");
		
		if (box.hasClass("collapsed-box")) {
			box.removeClass("collapsed-box");
			box.find(".box-body").slideDown();
		}
	});
	
	$("#asset_exchange_bid_orders_table tbody").on("click", "td", function(e) {	
	    var $target = $(e.target);

   		if ($target.prop("tagName").toLowerCase() == 'a') {
	   		return;
   		}
   		
   		var $tr = $target.closest("tr");

		var price = $tr.data("price");
		var quantity = $tr.data("quantity");
		
		$("#buy_asset_price").val(price);
		$("#sell_asset_price").val(price);
		$("#sell_asset_quantity").val(quantity);
		$("#sell_asset_total").val(Math.round(price*quantity*100)/100);
		
		var box = $("#sell_asset_box");
		
		if (box.hasClass("collapsed-box")) {
			box.removeClass("collapsed-box");
			box.find(".box-body").slideDown();
		}
	});
	
	function isControlKey(charCode) {
		if(charCode >= 32)
			return false;
		if(charCode == 10)
			return false;
		if(charCode == 13)
			return false;

		return true;
	}
	
	$("#buy_asset_quantity, #buy_asset_price, #sell_asset_quantity, #sell_asset_price, #buy_asset_fee, #sell_asset_fee").keydown(function(e) {
		var charCode = !e.charCode ? e.which : e.charCode;
				
		if (isControlKey(charCode) || e.ctrlKey || e.metaKey ) {
			return;
		}
		
		if (charCode == 190) {
			if ($(this).val().indexOf(".") != -1) {
				e.preventDefault();
				return false;
			}
		}
		
		//allow period
		if (charCode == 190 || (charCode >= 48 && charCode <= 57) || (charCode >= 96 && charCode <= 105)) {
			return;
		}
		
		var regex = new RegExp("^[0-9\.]+$");
		var key = String.fromCharCode(charCode);
				
		if (!regex.test(key)) {
			e.preventDefault();
			return false;
		} 
	});
	
	$("#buy_asset_quantity, #buy_asset_price").keyup(function(e) {
		var total = $("#buy_asset_quantity").val() * $("#buy_asset_price").val();
		
		if (!isNaN(total)) {
			var rounded = Math.round(total*100)/100;
			$("#buy_asset_total").val(rounded);
		} else {
			$("#buy_asset_total").val("0");
		}
	});
	
	$("#sell_asset_quantity, #sell_asset_price").keyup(function(e) {
		var total = $("#sell_asset_quantity").val() * $("#sell_asset_price").val();
		
		if (!isNaN(total)) {
			var rounded = Math.round(total*100)/100;
			$("#sell_asset_total").val(rounded);
		} else {
			$("#sell_asset_total").val("0");
		}
	});
	
	$("#asset_order_modal").on('show.bs.modal', function (e) {
		var $invoker = $(e.relatedTarget);
		
		var orderType = $invoker.data("type");
		var assetId = $invoker.data("asset");
		
		$("#asset_order_modal .asset_order_modal_type").html(orderType);
				
		if (orderType == "Buy") {
			var price = Math.round(parseFloat($("#buy_asset_price").val())*100)/100;
			var quantity = parseInt($("#buy_asset_quantity").val(), 10);
			var fee = parseInt($("#buy_asset_fee").val(), 10);
			
			var description = "Buy <strong>" + NRS.formatAmount(quantity) + " " + $("#asset_name").html() + "</strong> assets at <strong>" + NRS.formatAmount(price) + " NXT</strong> each.";
			var tooltipTitle = "As each asset is bought you will pay " + NRS.formatAmount(price) + " NXT, making a total of " + NRS.formatAmount(price*quantity, true) + " NXT once all assets have been bought.";
		} else {
			var price = Math.round(parseFloat($("#sell_asset_price").val())*100)/100;
			var quantity = parseInt($("#sell_asset_quantity").val(), 10);
			var fee = parseInt($("#sell_asset_fee").val(), 10);
		
			var description = "Sell <strong>" + NRS.formatAmount(quantity) + " " + $("#asset_name").html() + "</strong> assets at <strong>" + NRS.formatAmount(price) + " NXT</strong> each.";
			var tooltipTitle = "As each asset is sold you will receive " + NRS.formatAmount(price) + " NXT, making a total of " + NRS.formatAmount(price*quantity, true) + " NXT once all assets have been sold.";
		}
		
		if (isNaN(price)) {
			price = 0;
		}
		
		if (isNaN(quantity)) {
			quantity = 0;
		}
		
		if (price == 0 || quantity == 0) {
	   		$.growl("Please fill in an amount and price.", {"type": "danger"});
			return e.preventDefault();
		}
		
		if (isNaN(fee)) {
			fee = 1;
		}
		
		$("#asset_order_description").html(description);		
		$("#asset_order_total").html(NRS.formatAmount(price*quantity, true) + " NXT");
		$("#asset_order_fee_paid").html(NRS.formatAmount(fee) + " NXT");
				
		if (quantity > 1) {
			$("#asset_order_total_tooltip").show();
			$("#asset_order_total_tooltip").popover("destroy");
			$("#asset_order_total_tooltip").data("content", tooltipTitle);
			$("#asset_order_total_tooltip").popover({"content": tooltipTitle, "trigger": "hover"});
		} else {
			$("#asset_order_total_tooltip").hide();
		}
		
		$("#asset_order_type").val((orderType == "Buy" ? "placeBidOrder" : "placeAskOrder"));
		$("#asset_order_asset").val(assetId);
		$("#asset_order_quantity").val(quantity);
		$("#asset_order_price").val(Math.round(price*100));
		$("#asset_order_fee").val(fee);
	});
	
	//https://news.ycombinator.com/item?id=7224225
	NRS.forms.orderAsset = function($modal) {		
		var orderType = $("#asset_order_type").val();
				
		return {"requestType": orderType, "successMessage": $modal.find("input[name=success_message]").val().replace("__", (orderType == "placeBidOrder" ? "buy" : "sell"))};
	}
	
	NRS.forms.orderAssetComplete = function(response, data) {
		data.transaction = response.transaction;
		data.order = response.transaction;
		data.assetName = $("#asset_name").html();
		delete data.secretPhrase;
		
		if (data.requestType == "placeBidOrder") {
			NRS.tentative["bid_orders"].push(data);
			var $table = $("#asset_exchange_bid_orders_table tbody");
		} else {
			NRS.tentative["ask_orders"].push(data);
			var $table = $("#asset_exchange_ask_orders_table tbody");
		}
				
		var $rows = $table.find("tr");
				
		var rowToAdd = "<tr class='tentative' data-transaction='" + String(response.transaction).escapeHTML() + "' data-quantity='" + String(data.quantity).escapeHTML() + "' data-price='" + (data.price/100) + "'><td>You - <strong>Tentative Order</strong></td><td>" + NRS.formatAmount(data.quantity) + "</td><td>" + NRS.formatAmount(data.price/100) + "</td><td>" + NRS.formatAmount((data.price/100) * data.quantity, true) + "</td></tr>";
		
		var rowAdded = false;
		
		//TENTATIVE -- update both the asset exchange and open orders
		
		
		//update highest bid / lowest ask
		if ($rows.length) {
			$rows.each(function() {
				var row_price = parseFloat($(this).data("price"));
								
				if (data.requestType == "placeBidOrder" && data.price / 100 > row_price) {
					$(this).before(rowToAdd);
					rowAdded = true;
					return false;
				} else if (data.requestType == "placeAskOrder" && data.price / 100 < row_price) {
					$(this).before(rowToAdd);
					rowAdded = true;
					return false;
				}
			});
		}
		
		if (!rowAdded) {
			//if (data.requestType == "placeBidOrder") {
			$table.append(rowToAdd);
			$table.parent().parent().removeClass("data-empty").parent().addClass("no-padding");
		}
	}
	
	$("#asset_exchange_sidebar_group_context").on("click", "a", function(e) {
		e.preventDefault();
		
		var groupName = NRS.selectedContext.data("groupname");
		var option = $(this).data("option");
		
		if (option == "change_group_name") {			
			$("#asset_exchange_change_group_name_old_display").html(groupName.escapeHTML());
			$("#asset_exchange_change_group_name_old").val(groupName);
			$("#asset_exchange_change_group_name_new").val("");
			$("#asset_exchange_change_group_name_modal").modal("show");
		}
	});
	
	NRS.forms.assetExchangeChangeGroupName = function($modal) {   
	    var oldGroupName = $("#asset_exchange_change_group_name_old").val();
	    var newGroupName = $("#asset_exchange_change_group_name_new").val();
	    	    
		NRS.database.update("assets", {"groupName": newGroupName}, [{"groupName": oldGroupName}], function() {
			NRS.pages.asset_exchange();
		});
			    
	    $.growl("Group name updated successfully.", {"type": "success"});
		
	    return {"stop": true};
    }
    
	
	$("#asset_exchange_sidebar_context").on("click", "a", function(e) {
		e.preventDefault();
		
		var assetId = NRS.selectedContext.data("asset");
		var option = $(this).data("option");
		
		NRS.closeContextMenu();
		
		if (option == "add_to_group") {			
			$("#asset_exchange_group_asset").val(assetId);
																
			NRS.database.select("assets", [{"assetId": assetId}], function(asset) {				
				asset = asset[0];
												
				$("#asset_exchange_group_title").html(String(asset.name).escapeHTML());
			    	
				NRS.database.select("assets", [], function(assets) {
					
					//NRS.database.execute("SELECT DISTINCT groupName FROM assets", [], function(groupNames) {					
					var groupNames = [];
					
					$.each(assets, function(index, asset) {
						if (asset.groupName && $.inArray(asset.groupName, groupNames) == -1) {
							groupNames.push(asset.groupName);
						}	
					});
					
					assets = [];
										
					groupNames.sort(function(a, b) {
						if (a.toLowerCase() > b.toLowerCase()) {
							return 1;
						} else if (a.toLowerCase() < b.toLowerCase()) {
							return -1;
						} else {
							return 0;
						}
					});
										
					var groupSelect = $("#asset_exchange_group_group");
					
					groupSelect.empty();
	
					$.each(groupNames, function(index, groupName) {
						groupSelect.append("<option value='" + groupName.escapeHTML() + "'" + (asset.groupName && asset.groupName.toLowerCase() == groupName.toLowerCase() ? " selected='selected'" : "") + ">" + groupName.escapeHTML() + "</option>");
					});
					
					groupSelect.append("<option value='0'" + (!asset.groupName ? " selected='selected'" : "") + ">None</option>");
					groupSelect.append("<option value='-1'>New group</option>");
						
	    			$("#asset_exchange_group_modal").modal("show");
				});
			});
		} else if (option == "remove_from_group") {
			NRS.database.update("assets", {"groupName": ""}, [{"assetId": assetId}], function() {
		    	NRS.pages.asset_exchange();
				$.growl("Asset removed from group successfully.", {"type": "success"});
			});
	  	}
    });
        
    $("#asset_exchange_group_group").on("change", function() {
    	var value = $(this).val();
    	
    	if (value == -1) {
	    	$("#asset_exchange_group_new_group_div").show();
    	} else {
	    	$("#asset_exchange_group_new_group_div").hide();
    	}
    });
    
    NRS.forms.assetExchangeGroup = function($modal) {      	
    	var assetId = $("#asset_exchange_group_asset").val();
    	var groupName = $("#asset_exchange_group_group").val();
		
    	if (groupName == 0) {
    		groupName = "";
    	} else if (groupName == -1) {
	    	groupName = $("#asset_exchange_group_new_group").val();
	    } 
	    	    
	    NRS.database.update("assets", {"groupName": groupName}, [{"assetId": assetId}], function() {
		    NRS.pages.asset_exchange();
	   	    if (!groupName) {
		   		$.growl("Asset removed from group successfully.", {"type": "success"});
			} else {
		   		$.growl("Asset added to group successfully.", {"type": "success"});
		   	}    	    
	    });
	    		
	    return {"stop": true};
    }
    
    $("#asset_exchange_group_modal").on("hidden.bs.modal", function(e) {
    	$("#asset_exchange_group_new_group_div").val("").hide();
    });
    		
    /* MY ASSETS PAGE */
    NRS.pages.my_assets = function() {
    	NRS.pageLoading();
    	
    	NRS.sendRequest("getAccount+", {"account": NRS.account}, function(response) {
    		if (response.assetBalances && response.assetBalances.length) {
    			var result = {"assets": [], "bid_orders": {}, "ask_orders": {}};
    			var count = {"total_assets": response.assetBalances.length, "assets": 0, "ignored_assets": 0, "ask_orders": 0, "bid_orders": 0};
    			    			
    			for (var i=0; i<response.assetBalances.length; i++) {
    				if (response.assetBalances[i].balance == 0) {
    					count.ignored_assets++;
    					if (NRS.checkMyAssetsPageLoaded(count)) {
    						NRS.myAssetsPageLoaded(result);
    					}
    					continue;
    				}
    				
    				NRS.sendRequest("getAskOrderIds+", {"asset": response.assetBalances[i].asset, "limit": 1, "timestamp": 0}, function(response, input) {
    					if (NRS.currentPage != "my_assets") {
							return;
						}
    					
    					if (response.askOrderIds && response.askOrderIds.length) {
    						NRS.sendRequest("getAskOrder+", {"order": response.askOrderIds[0], "_extra": {"asset": input.asset}}, function(response, input) {
    							if (NRS.currentPage != "my_assets") {
    								return;
    							}
    						
    							response.price /= 100;
    							
    							result.ask_orders[input["_extra"].asset] = response.price;
    							count.ask_orders++;
    							if (NRS.checkMyAssetsPageLoaded(count)) {
    								NRS.myAssetsPageLoaded(result);
    							}
    						});			   				
    					} else {
    						result.ask_orders[input.asset] = -1;
    						count.ask_orders++;	
    						if (NRS.checkMyAssetsPageLoaded(count)) {
    							NRS.myAssetsPageLoaded(result);
    						}
    					}	
    				});
    				
    				NRS.sendRequest("getBidOrderIds+", {"asset": response.assetBalances[i].asset, "limit": 1, "timestamp": 0}, function(response, input) {
    					if (NRS.currentPage != "my_assets") {
							return;
						}
    					
    					if (response.bidOrderIds && response.bidOrderIds.length) {
    						NRS.sendRequest("getBidOrder+", {"order": response.bidOrderIds[0], "_extra": {"asset": input.asset}}, function(response, input) {
    							if (NRS.currentPage != "my_assets") {
    								return;
    							}
    						
    							response.price /= 100;
    							
    							result.bid_orders[input["_extra"].asset] = response.price;
    							count.bid_orders++;
    							if (NRS.checkMyAssetsPageLoaded(count)) {
    								NRS.myAssetsPageLoaded(result);
    							}
    						});			   				
    					} else {
    						result.bid_orders[input.asset] = -1;
    						count.bid_orders++;
    						if (NRS.checkMyAssetsPageLoaded(count)) {
    							NRS.myAssetsPageLoaded(result);
    						}
    					}
    				});
    					
					NRS.sendRequest("getAsset+", {"asset": response.assetBalances[i].asset, "_extra": {"balance": response.assetBalances[i].balance}}, function(asset, input) {
						if (NRS.currentPage != "my_assets") {
					   		return;
					   	}
					   	
					   	asset.asset = input.asset;
					   	asset.balance = input["_extra"].balance;
					   			
					   	result.assets[count.assets] = asset;
					   	count.assets++;
					   	
						if (NRS.checkMyAssetsPageLoaded(count)) {
							NRS.myAssetsPageLoaded(result);
						}
					});
					
					if (NRS.currentPage != "my_assets") {
						return;
					}
    			}
    		} else {
				$("#my_assets_table tbody").empty();
				NRS.dataLoadFinished($("#my_assets_table"));
    		}
    	});
    }
    
    NRS.checkMyAssetsPageLoaded = function(count) {
    	if ((count.assets + count.ignored_assets == count.total_assets) && (count.assets== count.ask_orders) && (count.assets == count.bid_orders)) {
    		return true;
       	} else {
       		return false;
       	}
    }
	
	NRS.myAssetsPageLoaded = function(result) {
		var rows = "";
				
		result.assets.sort(function(a, b) {
			if (a.name.toLowerCase() > b.name.toLowerCase()) {
				return 1;
			} else if (a.name.toLowerCase() < b.name.toLowerCase()) {
				return -1;
			} else {
				return 0;
			}
		});
		
		for (var i=0; i<result.assets.length; i++) {
			var asset = result.assets[i];
					
			var percentageAsset = parseFloat(asset.balance / asset.quantity);
			percentageAsset = Math.round(percentageAsset * 10000000) / 100000;
							
			var lowest_ask_order = result.ask_orders[asset.asset];
			var highest_bid_order = result.bid_orders[asset.asset];
			
			var tentative = 0;
			
			if (NRS.tentative.transfer_assets.length) {
				for (var key in NRS.tentative.transfer_assets) {					
					if (NRS.tentative.transfer_assets[key].asset  == asset.asset) {
						tentative += parseInt(NRS.tentative.transfer_assets[key].quantity, 10);
					}
				}
			}
									   			
			rows += "<tr" + (tentative ? " class='tentative tentative-allow-links'" : "") + " data-asset='" + String(asset.asset).escapeHTML() + "'><td><a href='#' data-goto-asset='" + String(asset.asset).escapeHTML() + "'>" + asset.name.escapeHTML() + "</a></td><td class='quantity'>" + NRS.formatAmount(asset.balance) + (tentative ? " - <span class='added_quantity'>" + NRS.formatAmount(tentative) + "</span>" : "") + "</td><td>" + NRS.formatAmount(asset.quantity) + "</td><td>" + percentageAsset + "%</td><td>" + (lowest_ask_order > 0 ? NRS.formatAmount(lowest_ask_order) : "/") + "</td><td>" + (highest_bid_order > 0 ? NRS.formatAmount(highest_bid_order) : "/") + "</td><td>" + (highest_bid_order > 0 ? NRS.formatAmount(highest_bid_order * asset.balance, true) : "/") + "</td><td><a href='#' data-toggle='modal' data-target='#transfer_asset_modal' data-asset='" + String(asset.asset).escapeHTML() + "'>Transfer</a></td></tr>";
		}
		
		$("#my_assets_table tbody").empty().append(rows);
		NRS.dataLoadFinished($("#my_assets_table"));
		
		NRS.pageLoaded();
	}
	
    NRS.incoming.my_assets = function() {
    	NRS.pages.my_assets();
    }
    
    $("#transfer_asset_modal").on('show.bs.modal', function (e) {
    	var $invoker = $(e.relatedTarget);
    	
    	var assetId = $invoker.data("asset");
    
    	$("#transfer_asset_asset").val(assetId);
    	$("#transfer_asset_name").html(assetId);
    });
    
    //TENTATIVE
    NRS.forms.transferAssetComplete = function(response, data) {
    	data.transaction = response.transaction;
    	delete data.secretPhrase;
    	
    	NRS.tentative["transfer_assets"].push(data);
    	    	
    	var $row = $("#my_assets_table tr[data-asset='" + String(data.asset).escapeHTML() + "']");
    	
    	if ($row.length) {
    		if ($row.hasClass("tentative")) {
	    		var currentQuantity = parseInt($row.find("td.quantity span.added_quantity").html().replace("'", ""), 10);	   
	    		$row.find("td.quantity span.added_quantity").html(NRS.formatAmount(parseInt(data.quantity, 10) + currentQuantity));
    		} else {	
		    	$row.addClass("tentative tentative-allow-links");
				$row.find("td.quantity").html($row.find("td.quantity").html() + " - <span class='added_quantity'>" + NRS.formatAmount(data.quantity) + "</span>");
			}
    	}
    }
    
    $("#my_assets_table, #open_ask_orders_table, #open_bid_orders_table").on("click", "a[data-goto-asset]", function(e) {
    	e.preventDefault();
    	
    	var asset = $(this).data("goto-asset");
    	
    	$("#asset_exchange_sidebar a.list-group-item.active").removeClass("active");
    	$("#no_asset_selected, #asset_details").hide();
    	$("#loading_asset_data").show();
    	    	
    	$("ul.sidebar-menu a[data-page=asset_exchange]").last().trigger("click", [{callback: function() {
    		var assetLink = $("#asset_exchange_sidebar a[data-asset=" + asset + "]");
    		
    		if (assetLink.length) {
    			assetLink.click();
    		} else {
    			$("#loading_asset_data").hide();
    			$("#no_asset_selected").show();
    		}
    	}
    	}]);    	
    });
    
    /* OPEN ORDERS PAGE */
    NRS.pages.open_orders = function() {
    	var loaded = 0;
    	
    	NRS.pageLoading();
    	
    	NRS.getOpenOrders("ask", function() {
    		loaded++;
    		if (loaded == 2) {
    			NRS.pageLoaded();
    		}
    	});
    	
    	NRS.getOpenOrders("bid", function() {
    		loaded++;
    		if (loaded == 2) {
	    		NRS.pageLoaded();
    		}
    	});
    }	
    
    NRS.getOpenOrders = function(type, callback) {
    	var uppercase = type.charAt(0).toUpperCase() + type.slice(1).toLowerCase();
    	var lowercase = type.toLowerCase();
    	
    	var getCurrentOrderIds = "getAccountCurrent" + uppercase + "OrderIds+";
    	var orderIds = lowercase + "OrderIds";
    	var getOrder = "get" + uppercase + "Order+";
    	
    	var orders = [];
    	    	
    	NRS.sendRequest(getCurrentOrderIds, {"account": NRS.account, "timestamp": 0}, function(response) {
    		if (response[orderIds] && response[orderIds].length) {
    			var nr_orders = 0;
    			    			
    			for (var i=0; i<response[orderIds].length; i++) {
    				NRS.sendRequest(getOrder, {"order": response[orderIds][i]}, function(order, input) {
    					if (NRS.currentPage != "open_orders") {
    						return;
    					}
    					
    					order.order = input.order;
    					orders.push(order);
    					
    					nr_orders++;
    					    					
    					if (nr_orders == response[orderIds].length) {
							var nr_orders_complete = 0;
							 		
    						for (var i=0; i<nr_orders; i++) {
    							var order = orders[i];
    							
    							NRS.sendRequest("getAsset+", {"asset": order.asset, "_extra": {"id": i}}, function(asset, input) {
	    							if (NRS.currentPage != "open_orders") {
	    								return;
	    							}
	    							
	    							orders[input["_extra"].id].assetName = asset.name;
	    							
    								nr_orders_complete++;
    								
    								if (nr_orders_complete == nr_orders) {
    									NRS.openOrdersLoaded(orders, lowercase, callback);
    								}
    							});
    							
    							if (NRS.currentPage != "open_orders") {
    								return;
    							}
    						}
    					}
    				});
    				
    				if (NRS.currentPage != "open_orders") {
    					return;
    				}
    			}
    		} else {
    			NRS.openOrdersLoaded([], lowercase, callback);
    		}
    	});
    }
    
    NRS.openOrdersLoaded = function(orders, type, callback) {
        var tentativeOrders = type + "_orders";

	    if (NRS.tentative[tentativeOrders].length) {
			for (var key in NRS.tentative[tentativeOrders]) {
				var order = NRS.tentative[tentativeOrders][key];
				order.tentative = true;
				orders.push(order);
			}
		}			
				
		if (!orders.length) {
		    $("#open_" + type + "_orders_table tbody").empty();
    		NRS.dataLoadFinished($("#open_" + type + "_orders_table"));
    			
    		callback();

			return;
		}
		
		orders.sort(function(a, b) {
			if (a.assetName.toLowerCase() > b.assetName.toLowerCase()) {
				return 1;
			} else if (a.assetName.toLowerCase() < b.assetName.toLowerCase()) {
				return -1;
			} else {
				if (a.quantity * a.price > b.quantity * b.price) {
					return 1;
				} else if (a.quantity * a.price < b.quantity * b.price) {
					return -1;
				} else {
					return 0;
				}
			}
		});
		
		var rows = "";
											
		for (var i=0; i<orders.length; i++) {
			var completeOrder = orders[i];
			
			var cancelled = false;
			
			if (NRS.tentative.cancelled_orders.length && $.inArray(completeOrder.order, NRS.tentative.cancelled_orders) != -1) {
				cancelled = true;	
			}
			
			rows += "<tr data-order='" + String(completeOrder.order).escapeHTML() + "'" + (cancelled ? " class='tentative tentative-crossed'" : (completeOrder.tentative ? " class='tentative'" : "")) + "><td><a href='#' data-goto-asset='" + String(completeOrder.asset).escapeHTML() + "'>" + completeOrder.assetName.escapeHTML() + "</a></td><td>" + NRS.formatAmount(completeOrder.quantity) + "</td><td>" + NRS.formatAmount(completeOrder.price / 100) + "</td><td>" + NRS.formatAmount(completeOrder.quantity * (completeOrder.price / 100)) + "</td><td class='cancel'>" + (cancelled || completeOrder.tentative ? "/" : "<a href='#' data-toggle='modal' data-target='#cancel_order_modal' data-order='" + String(completeOrder.order).escapeHTML() + "' data-type='" + type + "'>Cancel</a>") + "</td></tr>";
		}
											
		$("#open_" + type + "_orders_table tbody").empty().append(rows);
		
		NRS.dataLoadFinished($("#open_" + type + "_orders_table"));
		orders = {};
		
		callback();
    }
    
    NRS.incoming.open_orders = function(transactions) {
    	if (transactions) {
	    	NRS.pages.open_orders();
	    }
    }
    
    $("#cancel_order_modal").on("show.bs.modal", function(e) {
    	var $invoker = $(e.relatedTarget);
    	
    	var orderType = $invoker.data("type");
    	var orderId = $invoker.data("order");
    	    	
    	if (orderType == "bid") {
    		$("#cancel_order_type").val("cancelBidOrder");
    	} else {
    		$("#cancel_order_type").val("cancelAskOrder");
    	}
    	
    	$("#cancel_order_order").val(orderId);
    });
    
    NRS.forms.cancelOrder = function($modal) {
    	var orderType = $("#cancel_order_type").val();
    	
    	return {"requestType": orderType, "successMessage": $modal.find("input[name=success_message]").val().replace("__", (orderType == "cancelBidOrder" ? "buy" : "sell"))};
    }
        
    NRS.forms.cancelOrderComplete = function(response, data) {	        	
    	NRS.tentative["cancelled_orders"].push(data.order);
    	NRS.tentativeAge["cancelled_orders"].push(0);
    	
	    $("#open_orders_page tr[data-order=" + String(data.order).escapeHTML() + "]").addClass("tentative tentative-crossed").find("td.cancel").html("/");
    }
    
    /* VOTING PAGE */
    $("#create_poll_answers").on("click", "button.btn.remove_answer", function(e) {
    	e.preventDefault();
    	
    	if ($("#create_poll_answers > .form-group").length == 1) {
    		return;
    	}
    	
    	$(this).closest("div.form-group").remove();
    });
    
    $("#create_poll_answers_add").click(function(e) {
    	var $clone = $("#create_poll_answers > .form-group").first().clone();
    	
    	$clone.find("input").val("");
    	
    	$clone.appendTo("#create_poll_answers");
    }); 
    
    NRS.forms.createPoll = function($modal) {           	
        var options = new Array();
       
        $("#create_poll_answers input.create_poll_answers").each(function() {
        	var option = $.trim($(this).val());
        	
        	if (option) {
        		options.push(option);
        	}
        });
        
        if (!options.length) {
        	//...
        }
           	
        var data = {"name"		  	     : $("#create_poll_name").val(),
        			"description" 	     : $("#create_poll_description").val(),
        			"optionsAreBinary"   : "0", 
        			"minNumberOfOptions" : $("#create_poll_min_options").val(),
        			"maxNumberOfOptions" : $("#create_poll_max_options").val(),
        			"fee"			     : "1",
        			"deadline"		     : "24",
        			"secretPhrase"	     : $("#create_poll_password").val()};
        		
		for (var i=0; i<options.length; i++) {
			data["option" + i] = options[i];
		}
       	         			        			
		return {"requestType": "createPoll", "data": data};
	}
	
	NRS.forms.createPollComplete = function(response, data) {
		var date = new Date(Date.UTC(2013, 10, 24, 12, 0, 0, 0)).getTime();
        var now = parseInt(((new Date().getTime()) - date)/1000, 10);

		data.transaction = response.transaction;
		data.timestamp = now;
		delete data.secretPhrase;
		
		NRS.tentative["polls"].push(data);
		
		if (NRS.currentPage == "polls") {
			var $table = $("#polls_table tbody");
			
			var date = new Date(Date.UTC(2013, 10, 24, 12, 0, 0, 0)).getTime();
        	        
	        var now = parseInt(((new Date().getTime()) - date)/1000, 10);

			var rowToAdd = "<tr class='tentative'><td>" + String(data.name).escapeHTML() + " - <strong>Tentative</strong></td><td>" + String(data.description).escapeHTML() + "</td><td><a href='#' data-user='" + String(NRS.account).escapeHTML() + "' class='user_info'>" + NRS.getAccountTitle(NRS.account) + "</a></td><td>" + NRS.formatTimestamp(now) + "</td><td>/</td></tr>";
	    	
	    	$table.prepend(rowToAdd);
	    	
	    	if ($("#polls_table").parent().hasClass("data-empty")) {
		    	$("#polls_table").parent().removeClass("data-empty");
	    	}
    	}
    }
	
	NRS.forms.castVote = function($modal) {
		
	}
        
    /* MESSAGES PAGE */
    NRS.pages.messages = function(callback) {
    	NRS.pageLoading();
    	
   		$(".content.content-stretch:visible").width($(".page:visible").width());
    	    	
    	NRS.sendRequest("getAccountTransactionIds+", {"account": NRS.account, "timestamp": 0, "type": 1, "subtype": 0}, function(response) {    		
    		if (response.transactionIds && response.transactionIds.length) {
    			var transactionIds = response.transactionIds.reverse().slice(0, 100);
    			var nrTransactions = transactionIds.length;
    			    		
    			NRS.messages = {};
    			    		
    			var transactionsChecked = 0;
    				    		    				    			
    			for (var i=0; i<nrTransactions; i++) {
    				NRS.sendRequest("getTransaction+", {"transaction": transactionIds[i]}, function(response) {
						if (NRS.currentPage != "messages") {
							return;
						}
						
						transactionsChecked++;
					
						var other_user = (response.recipient == NRS.account ? response.sender : response.recipient);
						    					
						if (!(other_user in NRS.messages)) {
							NRS.messages[other_user] = [];
						}
						
						NRS.messages[other_user].push(response);
						
						if (transactionsChecked == nrTransactions) {
						   	var rows = "";
						    var menu = "";
						    
						   	var sorted_messages = [];
						   						   							   							   	
						   	for (var other_user in NRS.messages) {
							   	NRS.messages[other_user].sort(function(a, b) {
									if (a.timestamp > b.timestamp) {
										return 1;
									} else if (a.timestamp < b.timestamp) {
										return -1;
									} else {
										return 0;
									}
							   	});
							   	
							   	sorted_messages.push({"timestamp": NRS.messages[other_user][NRS.messages[other_user].length-1].timestamp, "user": other_user});
							}
							
							sorted_messages.sort(function(a, b) {
								if (a.timestamp < b.timestamp) {
									return 1;
								} else if (a.timestamp > b.timestamp) {
									return -1;
								} else {
									return 0;
								}
							});

							for (var i=0; i<sorted_messages.length; i++) {
								var sorted_message = sorted_messages[i];	
								
								var extra = "";
								
								if (sorted_message.user in NRS.contacts) {
									extra = " data-contact='" + NRS.getAccountTitle(sorted_message.user) + "' data-context='messages_sidebar_update_context'";	
								}
				                    
				                menu += "<li><a href='#' data-account='" + String(sorted_message.user).escapeHTML() + "'><strong>" + NRS.getAccountTitle(sorted_message.user) + "</strong><br />" + NRS.formatTimestamp(sorted_message.timestamp) + "</a></li>";
				                
								rows += "<a href='#' class='list-group-item' data-account='" + String(sorted_message.user).escapeHTML() + "'" + extra + "><h4 class='list-group-item-heading'>" + NRS.getAccountTitle(sorted_message.user) + "</h4><p class='list-group-item-text'>" + NRS.formatTimestamp(sorted_message.timestamp) + "</p></a>";
							}
						
							$("#messages_sidebar").empty().append(rows);
							$("#messages_sidebar_menu").empty().append(menu);
							
							NRS.pageLoaded(callback);
						}
					});
				
					if (NRS.currentPage != "messages") {
						return;
					}
   			    }		    			
    		} else {
    		    $("#no_message_selected").hide();
				$("#no_messages_available").show();
	    		NRS.pageLoaded(callback);
    		}
    	});
    }
        
    NRS.incoming.messages = function(transactions) {
    	if (transactions) {
	    	//save current scrollTop    	
			var activeAccount = $("#messages_sidebar a.active");
    	
			if (activeAccount.length) {
    			activeAccount = activeAccount.data("account");
			} else {
				activeAccount = -1;
			}
    	    	
			NRS.pages.messages(function() {
    			$("#messages_sidebar a[data-account=" + activeAccount + "]").trigger("click");
			});
		}
    }
        
    $("#messages_sidebar").on("click", "a", function(event) {
    	event.preventDefault();
    	
    	$("#inline_message_text").val("");
    	
    	$("#messages_sidebar a.active").removeClass("active");
    	$(this).addClass("active");
    	
    	var otherUser = $(this).data("account");
    	
    	var messages = NRS.messages[otherUser];
	
    	$("#no_message_selected, #no_messages_available").hide();
    	    	
    	$("#inline_message_recipient").val(otherUser);
    	$("#inline_message_form").show();
    	    	
    	var last_day = "";
    	var output = "<dl class='chat'>";
    	
    	if (messages) {
    		for (var i=0; i<messages.length; i++) {
    			
    			var hex = messages[i].attachment.message;
    			
    			if (hex.indexOf("feff") === 0) {
    				var decoded = NRS.convertFromHex16(hex);
    			} else {
    				var decoded = NRS.convertFromHex8(hex);
    			}
    			
    			var day = NRS.formatTimestamp(messages[i].timestamp, true);
    			    			
    			if (day != last_day) {
					output += "<dt><strong>" + day + "</strong></dt>";
					last_day = day;
    			}
    			
    			output += "<dd class='" + (messages[i].recipient == NRS.account ? "from" : "to") + "'><p>" + nl2br(decoded.escapeHTML()) +  "</p></dd>";
    		}
    	}    	
    	
    	if (NRS.tentative.messages.length) {
    		for (var key in NRS.tentative.messages) {
    			var message = NRS.tentative.messages[key];
    			if (message.recipient == otherUser) {
    				output += "<dd class='to tentative'><p>" + nl2br(message.message) +  "</p></dd>";
    			}
    		}
    	}
    	
    	output += "</dl>";
    	
    	$("#message_details").empty().append(output);
    }); 
        
    $("#messages_sidebar_context").on("click", "a", function(e) {
    	e.preventDefault();
    	
    	var account = NRS.selectedContext.data("account");
    	var option = $(this).data("option");
    	    
    	NRS.closeContextMenu();
    	
    	if (option == "add_contact") {
    		$("#add_contact_account_id").val(account).trigger("blur");
	    	$("#add_contact_modal").modal("show");
		} else if (option == "send_nxt") {
			$("#send_money_recipient").val(account).trigger("blur");
			$("#send_money_modal").modal("show");
		}
    });
    
    $("#messages_sidebar_update_context").on("click", "a", function(e) {
	    e.preventDefault();
	    
	    var account = NRS.selectedContext.data("account");
    	var option = $(this).data("option");
		
		NRS.closeContextMenu();
		
		if (option == "update_contact") {
			$("#update_contact_modal").modal("show");
		} else if (option == "send_nxt") {
			$("#send_money_recipient").val(NRS.selectedContext.data("contact")).trigger("blur");
			$("#send_money_modal").modal("show");
		}

    });
    
    $("#inline_message_form").submit(function(e) {
    	e.preventDefault();
    	
    	if ($("#inline_message_password").val() == "") {
    		$.growl("Secret phrase is a required field.", { "type": "danger" });
    		return;
    	}
    	
		var message = $.trim($("#inline_message_text").val());

		if (!message) {
			$.growl("Message is a required field.", {"type": "danger"});
			return;
		}

    	var $btn = $("#inline_message_submit");
    	
    	$btn.button("loading");
    	    	
    	var hex = NRS.convertToHex8(message);
    	var back = NRS.convertFromHex8(hex);
    	   	
    	if (back != message) {
    	   	hex =  NRS.convertToHex16("\uFEFF" + message);
    	}
    	   	
    	var data = {"recipient": $("#inline_message_recipient").val(), 
    				"message": hex,
    				"fee": 1,
    				"deadline": 1440,
    				"_extra": {"message": message},
    				"secretPhrase": $("#inline_message_password").val()};
    	
    	NRS.sendRequest("sendMessage", data, function(response, input) {
    		if (response.errorCode) {
    			$.growl(response.errorDescription ? response.errorDescription.escapeHTML() : "Unknown error occured.", { type: "danger" });
    		} else if (response.transaction) {
    			var message = input;
    			message.message = data["_extra"].message;
    			message.transaction = response.transaction;
    			NRS.tentative.messages.push(message);
    			
    		 	$.growl("Message sent.", { type: "success" });
    		 	$("#inline_message_text").val("");
    		 	$("#message_details dl.chat").append("<dd class='to tentative'><p>" + data["_extra"].message.escapeHTML() + "</p></dd>");
    		 	//leave password alone until user moves to another page.
    		} else {
    			$.growl("An unknown error occured. Your message may or may not have been sent.", { type: "danger" });
    		}
    		$btn.button("reset");
    	});
    });
    
    NRS.forms.sendMessage = function($modal) {
        var message = $.trim($("#send_message_message").val());
        
        if (!message) {
	        return {"error": "Message is a required field."};
        }
		
        var hex = NRS.convertToHex8(message);
        var back = NRS.convertFromHex8(hex);
           	
        if (back != message) {
           	hex =  NRS.convertToHex16("\uFEFF" + message);
        }
           	
        var data = {"recipient": $("#send_message_recipient").val(), 
        			"message": hex,    				
        			"_extra": {"message": message},
        			"fee": $("#send_message_fee").val(),
        			"deadline": $("#send_message_deadline").val(),
        			"secretPhrase": $("#send_message_password").val()};
        			
       return {"requestType": "sendMessage", "data": data};
    }
    
    NRS.forms.sendMoneyComplete = function(response, data) {  
    	if (!(data["_extra"] && data["_extra"].convertedAccount)) {
	    	$.growl("NXT has been sent! <a href='#' data-account='" + String(data.recipient).escapeHTML() + "' data-toggle='modal' data-target='#add_contact_modal' style='text-decoration:underline'>Add recipient to contacts?</a>", {"type": "success"});
    	} else {
    		$.growl("NXT has been sent!", {"type": "success"});
    	}
    }
    
    NRS.forms.sendMessageComplete = function(response, data) {    	
    	data.transaction = response.transaction;
    	data.message = data._extra.message;
    	delete data["_extra"];
    	delete data.secretPhrase;
    	
    	if (!(data["_extra"] && data["_extra"].convertedAccount)) {
	    	$.growl("Your message has been sent! <a href='#' data-account='" + String(data.recipient).escapeHTML() + "' data-toggle='modal' data-target='#add_contact_modal' style='text-decoration:underline'>Add recipient to contacts?</a>", {"type": "success"});
    	} else {
    		$.growl("Your message has been sent!", {"type": "success"});
    	}
    	
    	NRS.tentative.messages.push(data);
    	
	    if (NRS.currentPage == "messages") {
	        var date = new Date(Date.UTC(2013, 10, 24, 12, 0, 0, 0)).getTime();
        	        
	        var now = parseInt(((new Date().getTime()) - date)/1000, 10);
			
	    	var $sidebar = $("#messages_sidebar");
	    		    	
	    	var $existing = $sidebar.find("a.list-group-item[data-account=" + String(data.recipient).escapeHTML() + "]");
	    	
	    	if ($existing.length) {
		    	$sidebar.prepend($existing);
		    	$existing.find("p.list-group-item-text").html(NRS.formatTimestamp(now));
		    	
		    	if ($existing.hasClass("active")) {
		    	   	$("#message_details dl.chat").append("<dd class='to tentative'><p>" + data.message.escapeHTML() + "</p></dd>");
		    	}
	    	} else {
	    		var accountTitle = NRS.getAccountTitle(data.recipient);
	    		
	    		var extra = "";
	    		
	    		if (accountTitle != data.recipient) {
		    		extra = " data-context='messages_sidebar_update_context'";	
	    		}
	    		
	    		var listGroupItem = "<a href='#' class='list-group-item' data-account='" + String(data.recipient).escapeHTML() + "'" + extra + "><h4 class='list-group-item-heading'>" + accountTitle + "</h4><p class='list-group-item-text'>" + NRS.formatTimestamp(now) + "</p></a>";
				$("#messages_sidebar").prepend(listGroupItem);
		    }
	    }
    }
    
    /* ALIASES PAGE */
	NRS.pages.aliases = function() {
		NRS.pageLoading();
		
	    NRS.sendRequest("listAccountAliases+", {"account": NRS.account}, function(response) {
	    	if (response.aliases && response.aliases.length) {
		    	var aliases = response.aliases;
	    	
	    		if (NRS.tentative.aliases.length) {
	    			for (var key in NRS.tentative.aliases) {
		    			var alias = NRS.tentative.aliases[key];
		    			alias.tentative = true;
		    			aliases.push(alias);
	    			}
		    	}
		    	
		    	aliases.sort(function(a, b) {
		    		if (a.alias.toLowerCase() > b.alias.toLowerCase()) {
		    			return 1;
		    		} else if (a.alias.toLowerCase() < b.alias.toLowerCase()) {
		    			return -1;
		    		} else {
		    			return 0;
		    		}
		    	});
	    	
		    	var rows = "";
		    	
		    	var alias_account_count = 0,  alias_uri_count = 0, empty_alias_count = 0, alias_count = aliases.length;
		    			    	
		    	for (var i=0; i<alias_count; i++) {
		    		var alias = aliases[i];
		    		
		    		rows += "<tr" + (alias.tentative ? " class='tentative'" : "") + " data-alias='" + alias.alias.toLowerCase().escapeHTML() + "'><td class='alias'>" + alias.alias.escapeHTML() + (alias.tentative ? " -  <strong>Tentative</strong>" : "") + "</td><td>" + (alias.uri.indexOf("http") === 0 ? "<a href='" + alias.uri.escapeHTML() + "' target='_blank'>" + alias.uri.escapeHTML() + "</a>" : alias.uri.escapeHTML()) + "</td><td><a href='#' data-toggle='modal' data-alias='" + alias.alias.escapeHTML() + "' data-target='#register_alias_modal'>Edit</a></td></tr>";
		    		if (!alias.uri) {
		    			empty_alias_count++;
		    		} else if (alias.uri.indexOf("http") === 0) { 
		    			alias_uri_count++;
		    		} else if (alias.uri.indexOf("acct:") === 0 || alias.uri.indexOf("nacc:") === 0) {
		    			alias_account_count++;
		    		}
		    	}
		    	
		    	$("#aliases_table tbody").empty().append(rows);
		    	NRS.dataLoadFinished($("#aliases_table"));
		    	
		    	$("#alias_account_count").html(alias_account_count).removeClass("loading_dots");
		    	$("#alias_uri_count").html(alias_uri_count).removeClass("loading_dots");
		    	$("#empty_alias_count").html(empty_alias_count).removeClass("loading_dots");
		    	$("#alias_count").html(alias_count).removeClass("loading_dots");
		    } else {
			    $("#aliases_table tbody").empty();
			    NRS.dataLoadFinished($("#aliases_table"));
		    
		   		$("#alias_account_count, #alias_uri_count, #empty_alias_count, #alias_count").html("0").removeClass("loading_dots");
		    }
		    
		    NRS.pageLoaded();
	    });
	}
    
    $("#register_alias_modal").on("show.bs.modal", function(e) {
    	var $invoker = $(e.relatedTarget);
    	
    	var alias = $invoker.data("alias");
    	    	 
    	if (alias) {
    		NRS.sendRequest ("getAliasURI", {"alias": alias}, function(response) {
	    	    if (/http:\/\//i.test(response.uri)) {
	    			NRS.forms.setAliasType("uri");
	    		} else if (/acct:(\d+)@nxt/.test(response.uri) || /nacc:(\d+)/.test(response.uri)) {
		    		NRS.forms.setAliasType("account");
	    		} else {
		    		NRS.forms.setAliasType("general");
	    		}
	    		
	    		$("#register_alias_modal h4.modal-title").html("Update Alias");	
	    		$("#register_alias_modal .btn-primary").html("Update");
	    		$("#register_alias_alias").val(alias.escapeHTML()).hide();
	    		$("#register_alias_alias_noneditable").html(alias.escapeHTML()).show();
	    		$("#register_alias_alias_update").val(1);
	    		$("#register_alias_uri").val(response.uri);
	    	});
    	} else {
    		$("#register_alias_modal h4.modal-title").html("Register Alias");
    		$("#register_alias_modal .btn-primary").html("Register");
    		$("#register_alias_alias").val("").show();
    		$("#register_alias_alias_noneditable").html("").hide();
    		$("#register_alias_alias_update").val(0);
    		NRS.forms.setAliasType("uri");
    	}
    });
    
    NRS.incoming.aliases = function(transactions) {
    	if (transactions) {
	    	NRS.pages.aliases();
	    }
    }
    
    NRS.forms.assignAlias = function($modal) {
    	var data = NRS.getFormData($modal.find("form:first"));
    	
    	data.uri = $.trim(data.uri);
    	
    	if (data.type == "account") {
    		if (!(/acct:(\d+)@nxt/.test(data.uri)) && !(/nacc:(\d+)/.test(data.uri))) {
    			if (/^\d+$/.test(data.uri)) {
	    			data.uri = "acct:" + data.uri + "@nxt";
    			} else {
	    			return {"error": "Invalid account ID."};
    			}
    		}

    	}
    	delete data["type"];
    	
    	if ($("#register_alias_alias_update").val() == 1) {
	    	return {"data": data, "successMessage": "Alias updated successfully"};
    	} else {
	    	return {"data": data};
    	}
    }
    
    NRS.forms.setAliasType = function(type, uri) {
        $("#register_alias_type").val(type);

		if (type == "uri") {
			$("#register_alias_uri_label").html("URI");
			$("#register_alias_uri").prop("placeholder", "URI");
			if (uri) {
				if (!/https?:\/\//i.test(uri)) {
					$("#register_alias_uri").val("http://" + uri);	
				} else {
					$("#register_alias_uri").val(uri);
				}
			} else {
				$("#register_alias_uri").val("http://");
			}
		} else if (type == "account") {
			$("#register_alias_uri_label").html("Account ID");
			$("#register_alias_uri").prop("placeholder", "Account ID");
			$("#register_alias_uri").val("");
			if (uri) {
				if (!(/acct:(\d+)@nxt/.test(uri)) && !(/nacc:(\d+)/.test(uri))) {
					if (/^\d+$/.test(uri)) {
						$("#register_alias_uri").val("acct:" + uri + "@nxt");
					} else {
	    				$("#register_alias_uri").val("");
					}
				} else {
	    			$("#register_alias_uri").val("");
				}
			} else {
				$("#register_alias_uri").val("");
			}
		} else {
			$("#register_alias_uri_label").html("Data");
			$("#register_alias_uri").prop("placeholder", "Data");
			if (uri) {
				$("#register_alias_uri").val(uri);
			} else {
				$("#register_alias_uri").val("");
			}
		}
    }
    
    $("#register_alias_type").on("change", function() {
    	var type = $(this).val();
    	NRS.forms.setAliasType(type, $("#register_alias_uri").val());
    });
        
    NRS.forms.assignAliasComplete = function(response, data) {
    	data.transaction = response.transaction;
    	delete data.secretPhrase;
    	
    	NRS.tentative["aliases"].push(data);
    	
    	if (NRS.currentPage == "aliases") {
    		var $table = $("#aliases_table tbody");
    		
    		var $row = $table.find("tr[data-alias=" + data.alias.toLowerCase().escapeHTML() + "]");
    		
    		if ($row.length) {
	    		$row.addClass("tentative");
	    		$row.find("td.alias").html(data.alias.escapeHTML()+ " - <strong>Tentative</strong>");
	    		
	    		if (data.uri && data.uri.indexOf("http") === 0) {
	    			$row.find("td.uri").html("<a href='" + String(data.uri).escapeHTML() + "' target='_blank'>" + String(data.uri).escapeHTML() + "</a>");
	    		} else {
	    			$row.find("td.uri").html(String(data.uri).escapeHTML());
	    		}
    		} else {
	    		var $rows = $table.find("tr");
	    	
	    		var rowToAdd = "<tr class='tentative' data-alias='" + data.alias.toLowerCase().escapeHTML() + "'><td class='alias'>" + data.alias.escapeHTML() + " -  <strong>Tentative</strong></td><td class='uri'>" + (data.uri && data.uri.indexOf("http") === 0 ? "<a href='" + String(data.uri).escapeHTML() + "' target='_blank'>" + data.uri.escapeHTML() + "</a>" : String(data.uri).escapeHTML()) + "</td><td>Edit</td></tr>";
	    	
	    		var rowAdded = false;
	    	
	    		var newAlias = data.alias.toLowerCase();
	    		
		    	if ($rows.length) {
		    		$rows.each(function() {
		    			var alias = $(this).data("alias");
		    			
		    			if (newAlias < alias) {
		    				$(this).before(rowToAdd);
		    				rowAdded = true;
		    				return false;
		    			}
		    		});
		    	}
	    	
		    	if (!rowAdded) {
		    		$table.append(rowToAdd);
		    	}
		    	
		    	if ($("#aliases_table").parent().hasClass("data-empty")) {
			    	$("#aliases_table").parent().removeClass("data-empty");
		    	}
    		}
    	}
    }
    
	/* BLOCKS PAGE */
    NRS.pages.blocks = function() {    
    	NRS.pageLoading();
   	    		    	   	    
    	if (NRS.blocks.length < 100) {   	
    		if (NRS.downloadingBlockchain) {
    			NRS.blocksPageLoaded();
    		} else {
     			var previousBlock = NRS.blocks[NRS.blocks.length-1].previousBlock;
	 			NRS.getBlock(previousBlock, NRS.finish100Blocks);
	 		}
    	} else {
    		NRS.blocksPageLoaded();
    	}	
    }

    NRS.finish100Blocks = function(response) {
        NRS.blocks.push(response);
    	if (NRS.blocks.length < 100) {
    		NRS.getBlock(response.previousBlock, NRS.finish100Blocks);
    	} else {
    		NRS.blocksPageLoaded();
    	}
    }
    
    NRS.blocksPageLoaded = function() {    	
	    var rows = "";
	    var total_amount = 0;
	    var total_fees = 0;
	    var total_transactions = 0;
    		
		for (var i=0; i<NRS.blocks.length; i++) {
			var block = NRS.blocks[i];
			
			total_amount += block.totalAmount;
			total_fees += block.totalFee;
			total_transactions += block.numberOfTransactions;
			
			var account = String(block.generator).escapeHTML();
			
			rows += "<tr><td>" + (block.numberOfTransactions > 0 ? "<a href='#' data-block='" + String(block.height).escapeHTML() + "' class='block' style='font-weight:bold'>" + String(block.height).escapeHTML() + "</a>" : String(block.height).escapeHTML()) + "</td><td>" + NRS.formatTimestamp(block.timestamp) + "</td><td>" + NRS.formatAmount(block.totalAmount) + "</td><td>" + NRS.formatAmount(block.totalFee) + "</td><td>" + NRS.formatAmount(block.numberOfTransactions) + "</td><td>" + (account != NRS.genesis ? "<a href='#' data-user='" + account + "' class='user_info'>" + NRS.getAccountTitle(account) + "</a>" : "Genesis") + "</td><td>" + NRS.formatVolume(block.payloadLength) + "</td><td>" + Math.round(block.baseTarget / 153722867 * 100).pad(4) + " %</td></tr>";
		}
		
		var startingTime = NRS.blocks[NRS.blocks.length-1].timestamp;
		var endingTime = NRS.blocks[0].timestamp;
		var time = endingTime - startingTime; 
	
		$("#blocks_table tbody").empty().append(rows);
		NRS.dataLoadFinished($("#blocks_table"));
		
		$("#blocks_average_fee").html(total_fees/100).removeClass("loading_dots");
		$("#blocks_average_amount").html(NRS.formatAmount(Math.round(total_amount/100))).removeClass("loading_dots");
		$("#blocks_transactions_per_hour").html(Math.round(total_transactions/(time/60)*60)).removeClass("loading_dots");
		$("#blocks_average_generation_time").html(Math.round(time/100) + "s").removeClass("loading_dots");
		
		NRS.pageLoaded();
    }
    
    /* TRANSACTIONS PAGE */
    NRS.pages.transactions = function() {    	
    	NRS.pageLoading();
    	
    	var params = {"account": NRS.account, "timestamp": 0};
    	
    	if (NRS.transactionsPageType) {
    		params.type = NRS.transactionsPageType.type;
    		params.subtype = NRS.transactionsPageType.subtype;
    	}
    	
    	NRS.sendRequest("getAccountTransactionIds+", params, function(response) {
    		if (response.transactionIds && response.transactionIds.length) {
    			var transactions = {};
    			var nr_transactions = 0;
    			
    			var transactionIds = response.transactionIds.reverse().slice(0, 100);
    			    			
    			for (var i=0; i<transactionIds.length; i++) {
    				NRS.sendRequest("getTransaction+", {"transaction": transactionIds[i]}, function(transaction, input) {
    					if (NRS.currentPage != "transactions") {
    						transactions = {};
    						return;
    					}
    					    					    					
    					transactions[input.transaction] = transaction;
    					nr_transactions++;
    					
    					if (nr_transactions == transactionIds.length) {
    						var rows = "";
    						
    						for (var i=0; i<nr_transactions; i++) {
    							var transaction = transactions[transactionIds[i]];
    							
    							var transactionType = "Unknown";
    							
    							if (transaction.type == 0) {
    								transactionType = "Ordinary payment";
    							} else if (transaction.type == 1) {
    								switch (transaction.subtype) {
    									case 0:
    										transactionType = "Arbitrary message";
    										break;
    									case 1:
    										transactionType = "Alias assignment";
    										break;
    									case 2:
    										transactionType = "Poll creation";
    										break;
    									case 3:
    										transactionType = "Vote casting";
    										break;
    								}
    							} else if (transaction.type == 2) {
    								switch (transaction.subtype) {
    									case 0: 
    										transactionType = "Asset issuance";
    										break;
    									case 1: 
    										transactionType = "Asset transfer";
    										break;
    									case 2: 
    										transactionType = "Ask order placement";
    										break;
    									case 3: 
    										transactionType = "Bid order placement";
    										break;
    									case 4:
    										transactionType = "Ask order cancellation";
    										break;
    									case 5: 
    										transactionType = "Bid order cancellation";
    										break;
    								}
    							}
    						    
    						   	var receiving = transaction.recipient == NRS.account;
    						   	var account = (receiving ? String(transaction.sender).escapeHTML() : String(transaction.recipient).escapeHTML());
    						   	    						   	
    							rows += "<tr><td>" + (transaction.attachment ? "<a href='#' data-transaction='" + String(transactionIds[i]).escapeHTML() + "' style='font-weight:bold'>" + String(transactionIds[i]).escapeHTML() + "</a>" : String(transactionIds[i]).escapeHTML()) + "</td><td>" + NRS.formatTimestamp(transaction.timestamp) + "</td><td>" + transactionType + "</td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fa fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fa fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td " + (transaction.type == 0 && receiving ? " style='color:#006400;'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + NRS.formatAmount(transaction.amount) + "</td><td " + (!receiving ? " style='color:red'" : "") + ">" + NRS.formatAmount(transaction.fee) + "</td><td>" + (account != NRS.genesis ? "<a href='#' data-user='" + account + "' class='user_info'>" + NRS.getAccountTitle(account) + "</a>" : "Genesis") + "</td><td>" + (transaction.confirmations > 1000 ? "1000+" : NRS.formatAmount(transaction.confirmations)) + "</td></tr>";
    						}
    						    						
    						$("#transactions_table tbody").empty().append(rows);
    						NRS.dataLoadFinished($("#transactions_table"));
    						
    						NRS.pageLoaded();
    					}
    				});
    				
    				if (NRS.currentPage != "transactions") {
    					transactions = {};
    					return;
    				}
    			}
    		} else {
				$("#transactions_table tbody").empty();
				NRS.dataLoadFinished($("#transactions_table"));
				
				NRS.pageLoaded();
    		}
    	});
    }
    
    NRS.incoming.transactions = function(transactions) {
    	NRS.pages.transactions();
    }
    
    NRS.defaultColors = {"background": "#F9F9F9", "header": "#408EBA", "sidebar": "#F4F4F4", "page_header": "#FBFBFB", "box": "#fff", "table": "#F3F4F5"};
    
    NRS.userStyles = {};
    
    NRS.styleOptions = {};
    
    NRS.styleOptions.header = ["header_bg", 
    						  {"key": "header_bg_gradient", "type": "gradient", "optional": true},
    						  {"key": "logo_bg", "optional": "true"},
    						  {"key": "logo_bg_gradient", "type": "gradient", "optional": true},
    						  "link_txt", 
    						  "link_txt_hover", 
    						  "link_bg_hover", 
    						  {"key": "link_bg_hover_gradient", "type": "gradient", "optional": true},
    						  "toggle_icon", 
    						  {"key": "toggle_icon_hover", "optional": true},
    						  {"key": "link_border", "optional": true},
    						  {"key": "link_border_inset", "optional": true},
    						  {"key": "header_border", "optional": true}];
    
    NRS.styleOptions.sidebar = ["sidebar_bg", 
    							{"key": "user_panel_bg", "optional": true},
    							{"key": "user_panel_bg_gradient", "type": "gradient", "optional": true},
    							"user_panel_txt", 
    							"user_panel_link",
    						    {"key": "sidebar_top_border", "optional": true}, 
    						    {"key": "sidebar_bottom_border", "optional": true}, 
    						    {"key": "menu_item_top_border", "optional": true}, 
    						    {"key": "menu_item_bottom_border", "optional": true}, 
    						    "menu_item_txt", 
    						    "menu_item_bg",
    						    {"key": "menu_item_bg_gradient", "type": "gradient", "optional": true},
    						    "menu_item_txt_hover",
    						    "menu_item_bg_hover", 
    						    {"key": "menu_item_bg_hover_gradient", "type": "gradient", "optional": true},
    						    "menu_item_txt_active",
    						    "menu_item_bg_active", 
    						    {"key": "menu_item_bg_active_gradient", "type": "gradient", "optional": true},
    						    {"key": "menu_item_border_active", "optional": true}, 
    						    {"key": "menu_item_border_hover", "optional": true},
    						    {"key": "menu_item_border_size", "type": "number", "optional": "true"},
    						    {"key": "submenu_item_top_border", "optional": true}, 
    						    {"key": "submenu_item_bottom_border", "optional": true}, 
    						    "submenu_item_txt", 
    						    "submenu_item_bg", 
    						    {"key": "submenu_item_bg_gradient", "type": "gradient", "optional": true},
    						    "submenu_item_txt_hover",
    						    "submenu_item_bg_hover", 
    						    {"key": "submenu_item_bg_hover_gradient", "type": "gradient", "optional": true}];

    NRS.styleOptions.background = ["bg", 
    							  {"key": "bg_image", "type": "select", "values": ["always_grey", "back_pattern", "blu_stripes", "brickwall", "bright_squares", "carbon_fibre_v2", "circles", "climpek", "cubes", "dark_matter", "denim", "ecailles", "escheresque_ste", "escheresque", "furley_bg", "gplaypattern", "grey_sandbag", "grey", "grid_noise", "gun_metal", "hexellence", "hoffman", "knitting250px",  "light_grey", "lil_fiber", "noisy_grid", "old_moon", "pixel_weave", "polaroid", "ps_neutral", "pw_maze_white", "px_by_Gre3g", "random_grey_variations", "ricepaper_v3", "scribble_light", "shinedotted", "square_bg", "swirl", "tiny_grid", "weave", "white_brick_wall", "white_leather", "worn_dots"], "optional": true},
    							  "txt", 
    							  "link"];
    
    NRS.styleOptions.page_header = ["bg", 
    							   {"key": "bg_gradient", "type": "gradient", "optional": true},
    							   "txt", 
    							   {"key": "border", "optional": "true"}];
    
    NRS.styleOptions.box = ["bg", 
    						"txt",
    						{"key": "border_size", "type": "number", "optional": true}, 
    						"border_color", 
    						{"key": "rounded_corners", "optional": true, "type": "number"}, 
    						"header_background", 
    						"header_txt"];
    						
    NRS.styleOptions.table = ["bg", 
    						  "header_txt", 
    						  "rows_txt", 
    						  "row_separator", 
    						  "header_separator", 
    						  {"key": "row_separator_size", "type": "number"}, 
    						  {"key": "header_separator_size", "type": "number"}, 
    						  {"key": "header_bold", "type": "boolean"}];
    
    NRS.userStyles.header = {"green"  	   : {"header_bg": "#29BB9C", "logo_bg": "#26AE91", "link_bg_hover": "#1F8E77"},
    						 "red"    	   : {"header_bg": "#cb4040", "logo_bg": "#9e2b2b", "link_bg_hover": "#9e2b2b", "toggle_icon": "#d97474"},
        					 "brown"  	   : {"header_bg": "#ba5d32", "logo_bg": "#864324", "link_bg_hover": "#864324", "toggle_icon": "#d3815b"},
        					 "purple" 	   : {"header_bg": "#86618f", "logo_bg": "#614667", "link_bg_hover": "#614667", "toggle_icon": "#a586ad"},
        					 "gray"  	   : {"header_bg": "#575757", "logo_bg": "#363636", "link_bg_hover": "#363636", "toggle_icon": "#787878"},
        					 "pink"   	   : {"header_bg": "#b94b6f", "logo_bg": "#8b3652", "link_bg_hover": "#8b3652", "toggle_icon": "#cc7b95"},
        					 "bright-blue" : {"header_bg": "#2494F2", "logo_bg": "#2380cf", "link_bg_hover": "#36a3ff", "toggle_icon": "#AEBECD"},
        					 "dark-blue"   : {"header_bg": "#25313e", "logo_bg": "#1b252e", "link_txt": "#AEBECD", "link_bg_hover": "#1b252e", "link_txt_hover": "#fff", "toggle_icon": "#AEBECD"}};							 
	 NRS.userStyles.sidebar = {"dark-gray": {"sidebar_bg": "#272930", "user_panel_txt" : "#fff", "sidebar_top_border": "#1a1c20", "sidebar_bottom_border": "#2f323a", "menu_item_top_border": "#32353e", "menu_item_bottom_border": "#1a1c20", "menu_item_txt": "#c9d4f6", "menu_item_bg_hover": "#2a2c34", "menu_item_border_active": "#2494F2", "submenu_item_bg": "#2A2A2A", "submenu_item_txt": "#fff", "submenu_item_bg_hover": "#222222"},
	  						  "dark-blue": {"sidebar_bg": "#34495e", "user_panel_txt": "#fff", "sidebar_top_border": "#142638", "sidebar_bottom_border": "#54677a", "menu_item_top_border": "#54677a", "menu_item_bottom_border": "#142638", "menu_item_txt": "#fff", "menu_item_bg_hover": "#3d566e", "menu_item_bg_active": "#2c3e50", "submenu_item_bg": "#ECF0F1", "submenu_item_bg_hover": "#E0E7E8", 
	 "submenu_item_txt": "#333333"}};

	 NRS.userStyles.background = {"black": {"bg": "#000", "txt": "#fff", "link": "#fff"}, 
	 							 "light-gray": {"bg" : "#f9f9f9", "txt": "#000"},
	 							 "light-gray-2": {"bg": "#ECF0F1", "txt": "#000"},
	 							 "white": {"bg": "#fff", "txt": "#000"},	 
	 							 "dark-blue": {"bg": "#3E4649", "txt": "#fff"},
	 							 "dark-gray": {"bg": "#333333", "txt": "#fff"},
	 							 "blue": {"bg": "#58C0D4", "txt": "#fff"},
	 							 "light-blue": {"bg": "#D7DDE2", "txt": "#000"}};
	 
	 NRS.userStyles.page_header = {"light-gray": {"bg": "#ECF0F1", "txt": "#000"}};
	 	 							 
	 NRS.userStyles.box = {"black": {"bg": "#000", "txt": "#fff", "border_size": "2", "border_color": "red", "rounded_corners" : "2", "header_background": "#F3F3F3", "header_txt": "#333"}};
	 
	 NRS.userStyles.table = {"light_gray": {"bg": "#FAFAFA", "header_txt": "#000", "rows_txt": "#949494", "row_separator": "#EBEBEB", "header_separator": "#EBEBEB", "row_separator_size": "1", "header_separator_size": "3", "header_bold": true},
							 "black": {"bg": "#000", "header_txt": "#fff", "rows_bg": "#000", "rows_txt": "#fff", "row_separator_size": "1", "row_separator": "#ADD0E4", "header_bold": true},
	 
	 
	 };
	 	 
	 NRS.pages.settings = function() {
		 for (var style in NRS.userStyles) {
			var $dropdown = $("#" + style + "_color_scheme");
			
			$dropdown.empty();
			
			$dropdown.append("<li><a href='#' data-color=''><span class='color' style='background-color:" + NRS.defaultColors[style] + ";border:1px solid black;'></span>Default</a></li>");
			
			$.each(NRS.userStyles[style], function(key, value) {
				var bg = "";
				if (value.bg) {
			 		bg = value.bg;
				} else if (value.header_bg) {
			 		bg = value.header_bg;
				} else if (value.sidebar_bg) {
			 		bg = value.sidebar_bg;
				}
				
				$dropdown.append("<li><a href='#' data-color='" + key + "'><span class='color' style='background-color: " + bg + ";border:1px solid black;'></span> " + key.replace("-", " ") + "</a></li>");
			});
			
			$dropdown.append("<li><a href='#' data-color='custom'><span class='color'></span>Custom...</a></li>");
			
			var $span = $dropdown.closest(".btn-group.colors").find("span.text");
						
			var color = localStorage.getItem(style + "_color");
		   			   	 
			if (!color) {
				colorTitle = "Default";
			} else {
				var colorTitle = color.replace(/-/g, " ");
				colorTitle = colorTitle.replace(/\w\S*/g, function(txt) { 
					return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
				});
			}
		
			$span.html(colorTitle);
		 }
     }
     
     NRS.cssGradient = function(start, stop) {
     	var output = "";
     	
	    output += "background-image: -moz-linear-gradient(top, " + start + ", " + stop + ");";
	    output += "background-image: -ms-linear-gradient(top, " + start + ", " + stop + ");";
	    output += "background-image: -webkit-gradient(linear, 0 0, 0 100%, from(" + start + "), to(" + stop + "));";
	    output += "background-image: -webkit-linear-gradient(top, " + start + ", " + stop + ");";
	    output += "background-image: -o-linear-gradient(top, " + start + ", " + stop + ");";
	    output += "background-image: linear-gradient(top, " + start + ", " + stop + ");";
	    output += "filter: progid:dximagetransform.microsoft.gradient(startColorstr='" + start + "', endColorstr='" + stop + "', GradientType=0);";

		return output;
     }
    
     NRS.updateStyle = function(type, color) {
    	var css = "";
    	    	
		if ($.isPlainObject(color)) {
			var colors = color;
		} else {
	    	var colors = NRS.userStyles[type][color];
		}
		
		if (colors) {
	    	switch (type) {
	    		case "table":     			
	    			if (!colors.header_bg) {
		    			colors.header_bg = colors.bg;
	    			}
	    			if (!colors.rows_bg) {
		    			colors.rows_bg = colors.bg;
	    			}
	    			
	    			css += ".table > thead > tr > th { background: " + colors.header_bg + "; color: " + colors.header_txt +  (colors.header_bold ? "; font-weight:bold" : "; font-weight:normal") + " }";
	    			
					if (!colors.rows_even_bg && !colors.rows_odd_bg) {
						css += ".table > tbody > tr > td { background: " + colors.rows_bg + " !important; color: " + colors.rows_txt + " !important }";
					} else {
						if (!colors.rows_even_txt && !colors.rows_odd_txt) {
							colors.rows_even_txt = colors.rows_txt;
							colors.rows_odd_txt = colors.rows_txt;
						}
						
						css += ".table > tbody > tr >td { background: " + colors.rows_even_bg + "; color: " + colors.rows_even_txt + " }";
						css += ".table > tbody > tr:nth-child(odd) > td { background: "  + colors.rows_odd_bg + "; color: " + colors.rows_odd_txt + " }";
					}
					
					if (colors.header_separator) {
						css += ".table > thead > tr > th { border-bottom: " + colors.header_separator_size + "px solid " + colors.header_separator + " }";
					} else {
						css += ".table > thead > tr > th { border-bottom: none !important; border-top:none !important; }";
					}
					
					if (colors.row_separator) {
						css += ".table > tbody > tr > td { border-bottom: " + colors.row_separator_size + "px solid " + colors.row_separator + " }";				
					} else {
						css += ".table > tbody > tr > td { border-bottom: none !important; border-top:none !important; }";
					}
	    			
	    			break;
	    		case "box":     			
	    			css += ".box { background: " + colors.bg + "; color: " + colors.txt + "; -moz-border-radius: " + colors.rounded_corners + "px; -webkit-border-radius: " + colors.rounded_corners + "px; border-radius: " + colors.rounded_corners + "px; border: " + colors.border_size + "px solid " + colors.border_color + " !important }";
	    			
	    			if (colors.header_background) {
		    			css += ".box .box-header { background: " + colors.header_background + (colors.header_txt ? "; color: " + colors.header_txt + "; " : "") + " }";
	    			}
	    			
					//box-shadow: 0px 1px 3px rgba(0, 0, 0, 0.1);
					break;
	    	   	case "page_header":     			
	    			if (!colors.link) {
		    			colors.link = colors.txt;
	    			}
	    			
	    			css += ".right-side > .page > .content-header { background: " + colors.bg + "; color: " + colors.txt + (colors.border ? "; border-bottom: 1px solid " + colors.border : "") + " }";
	    			
	    			if (colors.bg_gradient) {
		    			css += ".right-side > .page > .content-header { " + NRS.cssGradient(colors.bg, colors.bg_gradient) + " }";
	    			}
	    		
	    			break;
	    		case "background":     			
	    			if (!colors.link) {
		    			colors.link = colors.txt;
	    			}
	    			
	    			css += "body, html, .content { background: " + colors.bg + "; color: " + colors.txt + " }";
	    			css += "a, a:active { color: " + colors.link + " }";
	    			
	    			if (colors.bg_image) {
	    				css += "body, html, .content { background-image: url('http://subtlepatterns.com/patterns/" + colors.bg_image + ".png') }";
	    			}
	    			break;
		    	case "header": 	    		
		    		if (!colors.link_txt) {
		    			colors.link_txt = "#fff";
		    		}
		    		if (!colors.toggle_icon) {
		    			colors.toggle_icon = "#fff";
		    		}
		    		if (!colors.toggle_icon_hover) {
		    			colors.toggle_icon_hover = "#fff";
		    		}
		    		if (!colors.link_txt_hover) {
			    		colors.link_txt_hover = colors.link_txt;
		    		}
		    		if (!colors.link_bg_hover && colors.link_bg) {
			    		colors.link_bg_hover = colors.link_bg;
		    		}
		    	
					
					if (!colors.logo_bg) {
						css += ".header { background:" + colors.header_bg + " }";
						if (colors.header_bg_gradient) {
				 			css += ".header { " + NRS.cssGradient(colors.header_bg, colors.header_bg_gradient) + " }";
						}
						css += ".header .navbar { background: inherit }";
						css += ".header .logo { background: inherit }";
					} else {
			    		css += ".header .navbar { background:" + colors.header_bg + " }";
			 
			 			if (colors.header_bg_gradient) {
				 			css += ".header .navbar { " + NRS.cssGradient(colors.header_bg, colors.header_bg_gradient) + " }";
			 			}   		
	
		    			css += ".header .logo { background: " + colors.logo_bg + " }";
		    			
		    			if (colors.logo_bg_gradient) {
			    			css += ".header .logo { " + NRS.cssGradient(colors.logo_bg, colors.logo_bg_gradient) + " }";
						}
					}
			    	
		    		css += ".header .navbar .nav a { color: " + colors.link_txt + (colors.link_bg ? "; background:" + colors.link_bg : "") + " }";
		    		css += ".header .navbar .nav > li > a:hover { color: " + colors.link_txt_hover + (colors.link_bg_hover ? "; background:" + colors.link_bg_hover : "") + " }";
		    		
		    		if (colors.link_bg_hover) {
		    			css += ".header .navbar .nav > li > a:hover { " + NRS.cssGradient(colors.link_bg_hover, colors.link_bg_hover_gradient) + " }";
		    		}
		    		
		    		css += ".header .navbar .sidebar-toggle .icon-bar { background: " + colors.toggle_icon + " }";
		    		css += ".header .navbar .sidebar-toggle:hover .icon-bar { background: " + colors.toggle_icon_hover + " }";
		    		
		    		if (colors.link_border) {
			    		css += ".header .navbar .nav > li { border-left: 1px solid " + colors.link_border +  " }";
			    	}
			    	
			    	if (colors.link_border_inset) {
				    	css += ".header .navbar .nav > li { border-right: 1px solid " + colors.link_border_inset + " }";
			    		css += ".header .navbar .nav > li:last-child { border-right:none }";
			    		css += ".header .navbar .nav { border-left: 1px solid " + colors.link_border_inset + " }";
			    	}
			    	
			    	/*
			    	if (colors.logo_border && colors.link_border) {
			    		if (colors.link_border_inset) {
			    			css += ".header .logo { border-right: 1px solid " + colors.link_border_inset + "}";
				    		css += ".header .navbar { border-left: 1px solid " + colors.link_border + " }";
			    		} else {
			    			css += ".header .navbar { border-left: 1px solid " + colors.link_border + " }";
			    		}
			    	}*/
			    	
			    	if (colors.header_border) {
				    		css += ".header { border-bottom: 1px solid " + colors.header_border + " }";
			    	}
		    		break;
		    	case "sidebar": 
					if (!colors.user_panel_link) {
						colors.user_panel_link = colors.user_panel_txt;
					}
					if (!colors.menu_item_bg) {
						colors.menu_item_bg = colors.sidebar_bg;
					}
					if (!colors.menu_item_bg_active) {
						colors.menu_item_bg_active = colors.menu_item_bg_hover;
					}
					if (!colors.menu_item_txt_hover) {
						colors.menu_item_txt_hover = colors.menu_item_txt;
					}
					if (!colors.menu_item_txt_active) {
						colors.menu_item_txt_active = colors.menu_item_txt_hover;
					}
					if (!colors.menu_item_border_active && colors.menu_item_border_hover) {
						colors.menu_item_border_active = colors.menu_item_border_hover;
					}
					if (!colors.menu_item_border_size) {
						colors.menu_item_border_size = 1;
					}
					
					css += ".left-side { background: " + colors.sidebar_bg + " }";
					
					css += ".left-side .user-panel > .info { color: " + colors.user_panel_txt + " }";
					
					if (colors.user_panel_bg) {
						css += ".left-side .user-panel { background: " + colors.user_panel_bg + " }";
						if (colors.user_panel_bg_gradient) {
							css += ".left-side .user-panel { " + NRS.cssGradient(colors.user_panel_bg, colors.user_panel_bg_gradient) + " }";	
						}
					}
					
					css += ".left-side .user-panel a { color:" + colors.user_panel_link + " }";
					
					if (colors.sidebar_top_border || colors.sidebar_bottom_border) {
						css += ".left-side .sidebar > .sidebar-menu { " + (colors.sidebar_top_border ? "border-top: 1px solid " + colors.sidebar_top_border + "; " : "") + (colors.sidebar_bottom_border ? "border-bottom: 1px solid " + colors.sidebar_bottom_border : "") + " }";
					}
					
					css += ".left-side .sidebar > .sidebar-menu > li > a { background: " + colors.menu_item_bg + "; color: " + colors.menu_item_txt + (colors.menu_item_top_border ? "; border-top:1px solid " + colors.menu_item_top_border : "") + (colors.menu_item_bottom_border ? "; border-bottom: 1px solid " + colors.menu_item_bottom_border : "") + " }";
					
					if (colors.menu_item_bg_gradient) {
						css += ".left-side .sidebar > .sidebar-menu > li > a { " + NRS.cssGradient(colors.menu_item_bg, colors.menu_item_bg_gradient) + " }";
					}
					
					css += ".left-side .sidebar > .sidebar-menu > li.active > a { background: " + colors.menu_item_bg_active + "; color: " + colors.menu_item_txt_active + (colors.menu_item_border_active ? "; border-left: " + colors.menu_item_border_size + "px solid " + colors.menu_item_border_active : "") + " }";
					
					if (colors.menu_item_border_hover || colors.menu_item_border_active) {
						css += ".left-side .sidebar > .sidebar-menu > li > a { border-left: " + colors.menu_item_border_size + "px solid transparent }";
					}
					
					if (colors.menu_item_bg_active_gradient) {
						css += ".left-side .sidebar > .sidebar-menu > li.active > a { " + NRS.cssGradient(colors.menu_item_bg_active, colors.menu_item_bg_active_gradient) + " }";
					}
					
					css += ".left-side .sidebar > .sidebar-menu > li > a:hover { background: " + colors.menu_item_bg_hover + "; color: " + colors.menu_item_txt_hover + (colors.menu_item_border_hover ? "; border-left: " + colors.menu_item_border_size + "px solid " + colors.menu_item_border_hover : "") + " }";
					
					if (colors.menu_item_bg_hover_gradient) {
						css += ".left-side .sidebar > .sidebar-menu > li > a:hover { " + NRS.cssGradient(colors.menu_item_bg_hover, colors.menu_item_bg_hover_gradient) + " }";
					}
					
					css += ".sidebar .sidebar-menu .treeview-menu > li > a { background: " + colors.submenu_item_bg + "; color: " + colors.submenu_item_txt + (colors.submenu_item_top_border ? "; border-top:1px solid " + colors.submenu_item_top_border : "") + (colors.submenu_item_bottom_border ? "; border-bottom: 1px solid " + colors.submenu_item_bottom_border : "") + " }";

					if (colors.submenu_item_bg_gradient) {
						css += ".sidebar .sidebar-menu .treeview-menu > li > a { " + NRS.cssGradient(colors.submenu_item_bg, colors.submenu_item_bg_gradient) + " }";
					}
					
					css += ".sidebar .sidebar-menu .treeview-menu > li > a:hover { background: " + colors.submenu_item_bg_hover + "; color: " + colors.submenu_item_txt_hover + " }";
					
					if (colors.submenu_item_bg_hover_gradient) {
						css += ".sidebar .sidebar-menu .treeview-menu > li > a:hover { " + NRS.cssGradient(colors.submenu_item_bg_hover, colors.submenu_item_bg_hover_gradient) + " }";
					}
		
		    		break;
		   	}
	   	}
	   	
		var $style = $("#user_" + type + "_style");
		
		if ($style[0].styleSheet) {
			$style[0].styleSheet.cssText = css;
		} else {
			$style.text(css);
		}
    }
    
    $("ul.color_scheme_editor").on("click", "li a", function(e) {
	   	e.preventDefault();
	   		   		   		   		   		   		   		   		   	
	   	var color = $(this).data("color");
	   	
	   	var scheme = $(this).closest("ul").data("scheme");
	   	
	   	var $span = $(this).closest(".btn-group.colors").find("span.text");
	   	
	   	if (!color) {
		   	colorTitle = "Default";
	   	} else {
		   	var colorTitle = color.replace(/-/g, " ");
		   	colorTitle = colorTitle.replace(/\w\S*/g, function(txt) { 
				return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
			});
		}
		
	   	$span.html(colorTitle);
	   		   		   		   	
	   	if (color == "custom") { 
		   	color = localStorage.getItem(scheme + "_color");
		   	
		   	if (!color) {
			   	color = "default"; //how??...
		   	}
		   		   	
		   	var style = NRS.userStyles[scheme][color];
		   			   	
		   	var output = "";
		   	
		   	var sorted_keys = [];
		   	
		   	for (var key in style) {
		    	sorted_keys.push(key);
		    }

			sorted_keys.sort();
			
			var options = NRS.styleOptions[scheme];
			
			$.each(options, function(i, definition) {
				var value = "";
				var optional = false;
				var has_value = false;
				var type = "color";
				var key = "";
				
				if ($.isPlainObject(definition)) {
					key = definition.key;
					
					if (key in style) {
						value = style[key];
						has_value = true;
					}
					
					if ("type" in definition) {
						type = definition["type"];
						
						if (value === "") {
							if (type == "number") {
								value = 0;
							} else if (type == "boolean") {
								value = false;
							} else if (type == "select") {
								value = "";
							} else {
								value = "#fff";
							}
						} else {
							has_value = false;
						}
					} else {
						type = "color";
						if (value === "") {
							value = "#fff";
						} else {
							has_value = true;
						}
					}
					
					if ("optional" in definition) {
						optional = true;
					}
				} else {
					key = definition;
					type = "color";
					
					if (key in style) {
						value = style[key];
						has_value = true;
					} else {
						value = "#fff";
					}
				}
		   				   		
				var title = key.replace(/_/g, " ");
				title = title.replace(/\w\S*/g, function(txt) { 
					return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
				});

				title = title.replace("Bg", "BG");
				title = title.replace("Txt", "Text");
									
				if (type == "boolean") {
					output += "<div class='form-group'><label class='control-label' style='text-align:left;width:160px;font-weight:normal;float:left'>" + title + "</label><div class='input-group' style='float:left'><input type='checkbox' name='" + key + "' value='1' class='form-control' " + (value ? " checked='checked'" : "") + " /></div></div>";
				} else if (type == "number") {
					output += "<div class='form-group'><label class='control-label' style='text-align:left;width:160px;font-weight:normal;float:left'>" + title + "</label><div class='input-group' style='float:left'><input type='" + type + "' name='" + key + "' value='" + value + "' class='form-control' style='width:140px' " + (optional && !has_value ? " disabled" : "") + " />" + (optional ? " <input type=checkbox style='margin-left:10px' class='color_scheme_enable' " + (has_value ? " checked='checked'" : "") + " />" : "") + "</div></div>";
				} else if (type == "select") {
					output += "<div class='form-group'><label class='control-label' style='text-align:left;width:160px;font-weight:normal;float:left'>" + title + "</label><div class='input-group' style='float:left'><select name='" + key + "' class='form-control' style='width:140px' " + (optional && !has_value ? " disabled" : "") + ">";
					for (var i=0; i<definition.values.length; i++) {
						output += "<option value='" + definition.values[i] + "'>" + definition.values[i] + "</option>";
					}
					output += "</select>" + (optional ? " <input type=checkbox style='margin-left:10px' class='color_scheme_enable' " + (has_value ? " checked='checked'" : "") + " />" : "") + "</div></div>";
				} else {
					output += "<div class='form-group'><label class='control-label' style='text-align:left;width:160px;font-weight:normal;float:left'>" + title + "</label><div class='input-group color_scheme_picker' style='float:left'><input type='text' name='" + key + "' value='" + value + "' class='form-control'" + (optional && !has_value ? " disabled" : "") + " style='width:100px' /><span class='input-group-addon'"  + (optional && !has_value ? " disabled" : "") + "><i></i></span>" + (optional ? " <input type=checkbox style='margin-left:10px' class='color_scheme_enable' " + (has_value ? " checked='checked'" : "") + " />" : "") + "</div></div>";
				}
		   	});
		   			   			   	
		   	$("#" + scheme +  "_custom_scheme").empty().append(output);
		   	$("#" + scheme + "_custom_scheme .color_scheme_picker").colorpicker().on("changeColor", function(e) {
			   	NRS.updateColorScheme(e);
		   	});

		   	$("#" + scheme + "_custom_scheme_group").show();
	   	} else {
	   		$("#" + scheme + "_custom_scheme_group").hide();
	   		
		   	if (color) {
			   	localStorage.setItem(scheme + "_color", color);
			   	NRS.updateStyle(scheme, color);
			}  else {
			   	localStorage.removeItem(scheme + "_color");
			   	NRS.updateStyle(scheme);
		   }
		}
    });
           
    $(".custom_color_scheme").on("change", ".color_scheme_enable", function(e) {
	   e.preventDefault();
	   
	   var $field = $(this).closest(".input-group").find(":input.form-control", 0);
	   var $color = $(this).closest(".input-group").find("span.input-group-addon", 0);
	   	   
	   $field.prop("disabled", !this.checked);
	   if ($color) {
		   $color.prop("disabled", !this.checked);
	   }
	   
	   NRS.updateColorScheme(e);
    });
    
    NRS.updateColorScheme = function(e) {
		var $color_scheme = $(e.target).closest(".custom_color_scheme");
   		
   		var scheme = $color_scheme.data("scheme");
   		
   		var $inputs = $color_scheme.find(":input:enabled");
   		
   		var values = {};
   		
	    $inputs.each(function() {
	        values[this.name] = $(this).val();
	    });
	    
	    NRS.updateStyle(scheme, values);
    }
    
    $("#transactions_page_type li a").click(function(e) {
    	e.preventDefault();
    	
    	var type = $(this).data("type");
    	
    	if (type) {
	    	type = type.split(":");
			NRS.transactionsPageType = {"type": type[0], "subtype": type[1]};
    	} else {
	    	NRS.transactionsPageType = null;
    	}
    	
    	$(this).parents(".btn-group").find(".text").text($(this).text());
    	
    	NRS.pages.transactions();
    });
        
    NRS.createInfoTable = function(data) {
    	var rows = "";
    	
    	for (var key in data) {
    		var value = data[key];
    		
    		if (key == "Quantity") {
    			value = NRS.formatAmount(value);
    		} else if (key == "Price" || key == "Total") {
    			value = NRS.formatAmount(value/100, true) + " NXT";
    		} else {
    			value = String(value).escapeHTML();
    		}
    		
    		rows += "<tr><td style='font-weight:bold;white-space:nowrap'>" + String(key).escapeHTML() + ":</td><td style='width:90%'>" + value + "</td></tr>";	
    	}
    	    	
    	return rows;
    }
    
	$("#transactions_table, #dashboard_transactions_table").on("click", "a[data-transaction]", function(e) {
		e.preventDefault();
		
		if (NRS.fetchingModalData) {
			return;
		}
		
		NRS.fetchingModalData = true;
		
		var async = false;
		
		var transactionId = $(this).data("transaction");
		
		$("#transaction_info_output").html("").hide();
		$("#transaction_info_table").hide();
		$("#transaction_info_table tbody").empty();
		
		NRS.sendRequest("getTransaction", {"transaction": transactionId}, function(transaction) {
			var transactionType = "";
			
			if (transaction.type == 1) {
				switch (transaction.subtype) {
					case 0:
						transactionType = "Arbitrary message";
						
						var hex = transaction.attachment.message;
		
						var message = "";
						
						if (hex.indexOf("feff") === 0) {
							message = NRS.convertFromHex16(hex);
						} else {
							message = NRS.convertFromHex8(hex);
						}
						
						var sender_info = "";
						
						if (transaction.sender == NRS.account) {
							sender_info = "<strong>To</strong>: " + NRS.getAccountTitle(transaction.recipient);
						} else {
							sender_info = "<strong>From</strong>: " + NRS.getAccountTitle(transaction.sender);
						}
						
						$("#transaction_info_output").html(nl2br(message.escapeHTML()) + "<br /><br />" + sender_info).show();
						break;
					case 1:
						transactionType = "Alias assignment";
						
						var data = {"Alias": transaction.attachment.alias, "URI": transaction.attachment.uri};
						
						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();
						
						break;
					case 2:
						transactionType = "Poll creation";
												
						var data = {"Name": transaction.attachment.name, "Description": transaction.attachment.description};
						
						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();
						
						break;
					case 3:
						transactionType = "Vote casting";
						break;
				}
			} else if (transaction.type == 2) {
				switch (transaction.subtype) {
					case 0: 
						transactionType = "Asset issuance";
						
						var data = {"Name": transaction.attachment.name, "Quantity": transaction.attachment.quantity, "Description": transaction.attachment.description};
						
						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();
						
						break;
					case 1: 
						transactionType = "Asset transfer";
						async = true;
						
						NRS.sendRequest("getAsset", {"asset": transaction.attachment.asset}, function(asset, input) {
							var data = {"Asset Name": asset.name, "Quantity": transaction.attachment.quantity};
							
							if (transaction.sender == NRS.account) {
								data.To = transaction.recipient;
							} else {
								data.From = transaction.sender;
							}
							
							$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
							$("#transaction_info_table").show();
						
							$("#transaction_info_title").html(transactionType);
							$("#transaction_info_modal").modal("show");
							NRS.fetchingModalData = false;
						});
						
						break;
					case 2: 
						transactionType = "Ask order placement";
						async = true;
						
						NRS.sendRequest("getAsset", {"asset": transaction.attachment.asset}, function(asset, input) {
							var data = {"Asset Name": asset.name, "Quantity": transaction.attachment.quantity, "Price": transaction.attachment.price, "Total": transaction.attachment.quantity*transaction.attachment.price};
							
							$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
							$("#transaction_info_table").show();
							
							$("#transaction_info_title").html(transactionType);
							$("#transaction_info_modal").modal("show");
							NRS.fetchingModalData = false;
						});
						
						break;
					case 3: 
						transactionType = "Bid order placement";
						async = true;
						
						NRS.sendRequest("getAsset", {"asset": transaction.attachment.asset}, function(asset, input) {							
							var data = {"Asset Name": asset.name, "Quantity": transaction.attachment.quantity, "Price": transaction.attachment.price, "Total": transaction.attachment.quantity*transaction.attachment.price};
							
							$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
							$("#transaction_info_table").show();
							
							$("#transaction_info_title").html(transactionType);
							$("#transaction_info_modal").modal("show");
							NRS.fetchingModalData = false;
						});
												
						break;
					case 4:
						transactionType = "Ask order cancellation";
						async = true;
						
						NRS.sendRequest("getTransaction", {"transaction": transaction.attachment.order}, function(transaction, input) {
							if (transaction.attachment.asset) {
								NRS.sendRequest("getAsset", {"asset": transaction.attachment.asset}, function(asset) {									
									var data = {"Asset Name": asset.name, "Quantity": transaction.attachment.quantity, "Price": transaction.attachment.price, "Total": transaction.attachment.quantity*transaction.attachment.price};
																		
									$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
									$("#transaction_info_table").show();
									
									$("#transaction_info_title").html(transactionType);
									$("#transaction_info_modal").modal("show");
									NRS.fetchingModalData = false;
								});
							} else {
								NRS.fetchingModalData = false;
							}
						});
						
						break;
					case 5: 
						transactionType = "Bid order cancellation";
						async = true;
						
						NRS.sendRequest("getTransaction", {"transaction": transaction.attachment.order}, function(transaction) {
							if (transaction.attachment.asset) {
								NRS.sendRequest("getAsset", {"asset": transaction.attachment.asset}, function(asset) {
									var data = {"Asset Name": asset.name, "Quantity": transaction.attachment.quantity, "Price": transaction.attachment.price, "Total": transaction.attachment.quantity*transaction.attachment.price};
									
									$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
									$("#transaction_info_table").show();
									
									$("#transaction_info_title").html(transactionType);
									$("#transaction_info_modal").modal("show");
									NRS.fetchingModalData = false;
								});
							} else {
								NRS.fetchingModalData = false;
							}
						});
						
						break;
				}
			}
		
			if (!transactionType) {
				NRS.fetchingModalData = false;
				return;
			}
			
			if (!async) {
				$("#transaction_info_title").html(transactionType);
				$("#transaction_info_modal").modal("show");
				NRS.fetchingModalData = false;
			}
		});
	}); 
	
	NRS.getAccountTitle = function(accountId) {
		if (accountId in NRS.contacts) {
			return NRS.contacts[accountId].name.escapeHTML();
		} else if (accountId == NRS.account) {
			return "You";
		} else {
			return String(accountId).escapeHTML();
		}
	}
	
	NRS.loadContacts = function() {
		NRS.contacts = {};
		
		NRS.database.select("contacts", null, function(contacts) {			
			if (contacts.length) {
				$.each(contacts, function(index, contact) {
					NRS.contacts[contact.accountId] = contact;
				});
			}
		});
	}
	
	NRS.pages.contacts = function() {
		NRS.pageLoading();
		
		if (!NRS.databaseSupport) {
			$("#contact_page_database_error").show();
			$("#contacts_table_container").hide();
			$("#add_contact_button").hide();
			NRS.pageLoaded();
			return;
		}
		
		$("#contacts_table_container").show();
		$("#contact_page_database_error").hide();
		
		NRS.database.select("contacts", null, function(contacts) {			
			if (contacts.length) {
				var rows = "";
				
				contacts.sort(function(a, b) {
		    		if (a.name.toLowerCase() > b.name.toLowerCase()) {
		    			return 1;
		    		} else if (a.name.toLowerCase() < b.name.toLowerCase()) {
		    			return -1;
		    		} else {
		    			return 0;
		    		}
		    	});
				
				$.each(contacts, function(index, contact) {  
	    			var contactDescription = contact.description;
				
					if (contactDescription.length > 100) {
						contactDescription = contactDescription.substring(0, 100) + "...";
					} else if (!contactDescription) {
						contactDescription = "/";
					}
    							    	
    							    	  							
					rows += "<tr><td><a href='#' data-toggle='modal' data-target='#update_contact_modal' data-contact='" + String(contact.id).escapeHTML() + "'>" + contact.name.escapeHTML() + "</a></td><td><a href='#' data-user='" + String(contact.accountId).escapeHTML() + "' class='user_info'>" + String(contact.accountId).escapeHTML() + "</a></td><td>" + contact.email.escapeHTML() + "</td><td>" + contactDescription.escapeHTML() + "</td><td><a href='#' data-toggle='modal' data-target='#delete_contact_modal' data-contact='" + String(contact.id).escapeHTML() + "'><i class='fa fa-times-circle text-danger' title='Delete'></i></a></td></tr>";
				});

				$("#contacts_table tbody").empty().append(rows);
				NRS.dataLoadFinished($("#contacts_table"));
				
				NRS.pageLoaded();
			} else {
				$("#contacts_table tbody").empty();
				NRS.dataLoadFinished($("#contacts_table"));
				
				NRS.pageLoaded();
			}
		});
	}
	
	NRS.forms.addContact = function($modal) {   
		var data = NRS.getFormData($modal.find("form:first"));
		
		if (!data.name) {
			return {"error": "Contact name is a required field."};
		} else if (!data.account_id) {
			return {"error": "Account ID is a required field."};
		}
		    	
    	if (/^\d+$/.test(data.name)) {
    		return {"error": "Contact name must contain alphabetic characters."};
    	}
				
		if (data.account_id.charAt(0) == '@') {
			var convertedAccountId = $modal.find("input[name=converted_account_id]").val();
			if (convertedAccountId) {	
				data.account_id = convertedAccountId;
			} else {
				return {"error": "Invalid account ID."};
			}
		}
		
		var $btn = $modal.find("button.btn-primary:not([data-dismiss=modal], .ignore)");
		
		NRS.database.select("contacts", [{"accountId": data.account_id}], function(contacts) {
			if (contacts.length) {
			    $modal.find(".error_message").html("A contact with this account ID already exists.").show();
    			$btn.button("reset");
    			$modal.modal("unlock");
			} else {
		    	NRS.database.insert("contacts", {name: data.name, email: data.email, accountId: data.account_id, description: data.description}, function() {
		    		NRS.contacts[data.account_id] = {name: data.name, email: data.email, accountId: data.account_id, description: data.description};
		    		
		    		$btn.button("reset");
		    		$modal.modal("unlock");
		    		$modal.modal("hide");
			    	$.growl("Contact added successfully.", {"type": "success"});
			    	
			    	if (NRS.currentPage == "contacts") {
			    		NRS.pages.contacts();
			    	} else if (NRS.currentPage == "messages" && NRS.selectedContext) {	
			    		var heading = NRS.selectedContext.find("h4.list-group-item-heading");
			    		if (heading.length) {
				    		heading.html(data.name.escapeHTML());
			    		}
			    		NRS.selectedContext.data("context", "messages_sidebar_update_context");
			    	}
		    	});
				
			    return {"stop": true};
			}
		});
	}
	
	$("#update_contact_modal").on('show.bs.modal', function (e) {
    	var $invoker = $(e.relatedTarget);
    	
    	var contactId = $invoker.data("contact");
    	
    	if (!contactId && NRS.selectedContext) {
	    	var accountId = NRS.selectedContext.data("account");
	    		    	
	    	NRS.database.select("contacts", [{"accountId": accountId}], function(contact) {
		    	contact = contact[0];
		    	
		    	$("#update_contact_id").val(contact.id);
				$("#update_contact_name").val(contact.name);
				$("#update_contact_email").val(contact.email);
				$("#update_contact_account_id").val(contact.accountId);
				$("#update_contact_description").val(contact.description);
	    	});
	    } else {
	    	$("#update_contact_id").val(contactId);
	    	    	    	
			NRS.database.select("contacts", [{"id": contactId}], function(contact) {
				contact = contact[0];
				
				$("#update_contact_name").val(contact.name);
				$("#update_contact_email").val(contact.email);
				$("#update_contact_account_id").val(contact.accountId);
				$("#update_contact_description").val(contact.description);
			});
		}
    });

	NRS.forms.updateContact = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));
		
		if (!data.name) {
			return {"error": "Contact name is a required field."};
		} else if (!data.account_id) {
			return {"error": "Account ID is a required field."};
		}
				
		if (data.account_id.charAt(0) == '@') {
			var convertedAccountId = $modal.find("input[name=converted_account_id]").val();
			if (convertedAccountId) {	
				data.account_id = convertedAccountId;
			} else {
				return {"error": "Invalid account ID."};
			}
		}
		
		var contactId = $("#update_contact_id").val();
				
		if (!contactId) {
			return {"error": "Invalid contact."};
		}
		
		var $btn = $modal.find("button.btn-primary:not([data-dismiss=modal])");
		
		NRS.database.select("contacts", [{"accountId": data.account_id}], function(contacts) {
			if (contacts.length && contacts[0].id != contactId) {
			    $modal.find(".error_message").html("A contact with this account ID already exists.").show();
    			$btn.button("reset");
    			$modal.modal("unlock");
			} else {
		    	NRS.database.update("contacts", {name: data.name, email: data.email, accountId: data.account_id, description: data.description}, [{id: contactId}], function() {		    		
		    		if (contacts.length && data.account_id != contacts[0].accountId) {
			    		delete NRS.contacts[contacts[0].accountId];
		    		}
		    		
		    		NRS.contacts[data.account_id] = {name: data.name, email: data.email, accountId: data.account_id, description: data.description};

		    		$btn.button("reset");
		    		$modal.modal("unlock");
		    		$modal.modal("hide");
			    	$.growl("Contact updated successfully.", {"type": "success"});
			    	
			    	if (NRS.currentPage == "contacts") {
				    	NRS.pages.contacts();
				    } else if (NRS.currentPage == "messages" && NRS.selectedContext) {	
			    		var heading = NRS.selectedContext.find("h4.list-group-item-heading");
			    		if (heading.length) {
				    		heading.html(data.name.escapeHTML());
			    		}
			    	}
		    	});
				
			    return {"stop": true};
			}
		});
	}
	
	$("#delete_contact_modal").on('show.bs.modal', function (e) {
    	var $invoker = $(e.relatedTarget);
    	
    	var contactId = $invoker.data("contact");
    
    	$("#delete_contact_id").val(contactId);
    	    	
		NRS.database.select("contacts", [{"id": contactId}], function(contact) {
			contact = contact[0];
			
			$("#delete_contact_name").html(contact.name.escapeHTML());
			$("#delete_contact_account_id").val(contact.accountId);
		});
    });

	NRS.forms.deleteContact = function($modal) {
		var id = $("#delete_contact_id").val();
		
		NRS.database.delete("contacts", [{"id": id}], function() {
			delete NRS.contacts[$("#delete_contact_account_id").val()];
			
			$.growl("Contact deleted successfully.", {"type": "success"});
			
			if (NRS.currentPage == "contacts") {
				NRS.pages.contacts();
			}
		});
		
		return {"stop": true};
	}
	
	//todo later: http://twitter.github.io/typeahead.js/
	$("span.recipient_selector button").on("click", function(e) {	
		if (!Object.keys(NRS.contacts).length) {
			e.preventDefault();
			e.stopPropagation();
			return;
		}
		
		var $list = $(this).parent().find("ul");
				
		$list.empty();
				
		for (var accountId in NRS.contacts) {
			$list.append("<li><a href='#' data-contact='" + NRS.contacts[accountId].name.escapeHTML() + "'>" + NRS.contacts[accountId].name.escapeHTML() + "</a></li>");	
		}
	});
	
	$("span.recipient_selector").on("click", "ul li a", function(e) {
		e.preventDefault();
		$(this).closest("form").find("input[name=recipient],input[name=account_id]").val($(this).data("contact")).trigger("blur");
	});
	
    NRS.pages.polls = function() {
    	NRS.pageLoading();
    	
    	NRS.sendRequest("getPollIds+", function(response) {
    		if (response.pollIds && response.pollIds.length) {
    			var polls = {};
    			var nr_polls = 0;
    			    
    			for (var i=0; i<response.pollIds.length; i++) {
    				NRS.sendRequest("getTransaction+", {"transaction": response.pollIds[i]}, function(poll, input) {
    					if (NRS.currentPage != "polls") {
    						polls = {};
    						return;
    					}
    					    					
    					if (!poll.errorCode) {
    						polls[input.transaction] = poll;
    					}
    					
    					nr_polls++;
    					
    					if (nr_polls == response.pollIds.length) {
    						var rows = "";
    						    			
    						if (NRS.tentative.polls.length) {
    							for (var key in NRS.tentative.polls) {
	    							var poll = NRS.tentative.polls[key];
	    							
	    							var pollDescription = poll.description;
    							
	    							if (pollDescription.length > 100) {
	    								pollDescription = pollDescription.substring(0, 100) + "...";
	    							}
    							    	
	    							rows += "<tr class='tentative'><td>" + poll.name.escapeHTML() + " - <strong>Tentative</strong></td><td>" + pollDescription.escapeHTML() + "</td><td><a href='#' data-user='" + String(NRS.account).escapeHTML() + "' class='user_info'>" + NRS.getAccountTitle(NRS.account) + "</a></td><td>" + NRS.formatTimestamp(poll.timestamp) + "</td><td><a href='#'>Vote (todo)</td></tr>";
    							}
    						}	
    							    			    						
    						for (var i=0; i<nr_polls; i++) {
    							var poll = polls[response.pollIds[i]];
    							    							
    							if (!poll) {
    								continue;
    							}
    							
    							var pollDescription = poll.attachment.description;
    							
    							if (pollDescription.length > 100) {
    								pollDescription = pollDescription.substring(0, 100) + "...";
    							}
    							    							
    							rows += "<tr><td>" + poll.attachment.name.escapeHTML() + "</td><td>" + pollDescription.escapeHTML() + "</td><td>" + (poll.sender != NRS.genesis ? "<a href='#' data-user='" + String(poll.sender).escapeHTML() + "' class='user_info'>" + NRS.getAccountTitle(poll.sender) + "</a>" : "Genesis") + "</td><td>" + NRS.formatTimestamp(poll.timestamp) + "</td><td><a href='#'>Vote (todo)</td></tr>";
    						}
    						    						
    						$("#polls_table tbody").empty().append(rows);
    						NRS.dataLoadFinished($("#polls_table"));
    						
    						NRS.pageLoaded();

    						polls = {};
    					}
    				});
    				
    				if (NRS.currentPage != "polls") {
    					polls = {};
    					return;
    				}
    			}
    		} else {
				$("#polls_table tbody").empty();
				NRS.dataLoadFinished($("#polls_table"));
				
				NRS.pageLoaded();
    		}
    	});
    }
        
    NRS.incoming.polls = function() {
	    NRS.pages.polls();
    }
    
    /* PEERS PAGE */
    NRS.pages.peers = function() {
    	var response;
    	
    	NRS.pageLoading();
    	
    	NRS.sendRequest("getPeers+", function(response) {
    		if (response.peers && response.peers.length) {
    			var peers = {};
    			var nr_peers = 0;
    			   			    			    			
    			for (var i=0; i<response.peers.length; i++) {
    				NRS.sendRequest("getPeer+", {"peer": response.peers[i]}, function(peer, input) {
	    				if (NRS.currentPage != "peers") {
	    					peers = {};
	    					return;
	    				}
    					
    					if (!peer.errorCode) {
	    					peers[input.peer] = peer;
	    				} 
	    				
    					nr_peers++;
    					
    					if (nr_peers == response.peers.length) {
    					    var rows = "";
	    					var uploaded = 0;
	    					var downloaded = 0;
	    					var connected = 0;
	    					var up_to_date = 0;
	    					var active_peers = 0;
    					
    						for (var i=0; i<nr_peers; i++) {
    							var peer = peers[response.peers[i]];
    							    	
    							if (!peer) {
    								continue;
    							}	
    												
	    						if (peer.state != 0) {
	    							active_peers++;
	    							downloaded += peer.downloadedVolume;
	    							uploaded += peer.uploadedVolume;
	    							if (peer.state == 1) { 
	    								connected++;
	    							}
	    							
	    							//todo check if response.version ends with "e" then we compare with betaversion instead..
	    							if (NRS.versionCompare(peer.version, NRS.normalVersion.versionNr) >= 0) {
	    								up_to_date++;
	    							}
	    							
	    							rows += "<tr><td>" + (peer.state == 1 ? "<i class='fa fa-check-circle' style='color:#5cb85c' title='Connected'></i>" : "<i class='fa fa-times-circle' style='color:#f0ad4e' title='Disconnected'></i>") + "&nbsp;&nbsp;" + (peer.announcedAddress ? String(peer.announcedAddress).escapeHTML() : "No name") + "</td><td" + (peer.weight > 0 ? " style='font-weight:bold'" : "") + ">" + NRS.formatWeight(peer.weight) + "</td><td>" + NRS.formatVolume(peer.downloadedVolume) + "</td><td>" + NRS.formatVolume(peer.uploadedVolume) + "</td><td><span class='label label-" + 
	    							  (NRS.versionCompare(peer.version, NRS.normalVersion.versionNr) >= 0 ? "success": "danger") + "'>" + (peer.application && peer.version ? String(peer.application).escapeHTML() + " " + String(peer.version).escapeHTML() : "?") + "</label></td><td>" + (peer.platform ? String(peer.platform).escapeHTML() : "?") + "</td></tr>";
	    						}
	    					}	
    						
    						$("#peers_table tbody").empty().append(rows);
    						NRS.dataLoadFinished($("#peers_table"));
    						$("#peers_uploaded_volume").html(NRS.formatVolume(uploaded)).removeClass("loading_dots");
    						$("#peers_downloaded_volume").html(NRS.formatVolume(downloaded)).removeClass("loading_dots");
    						$("#peers_connected").html(connected).removeClass("loading_dots");
    						$("#peers_up_to_date").html(up_to_date + '/' + active_peers).removeClass("loading_dots");
    						
    						peers = {};
    						
    						NRS.pageLoaded();
    					}
    				});
    				    				    				
    				if (NRS.currentPage != "peers") {
    					peers = {};
    					return;
    				}
    			}
    		} else {
				$("#peers_table tbody").empty();
				NRS.dataLoadFinished($("#peers_table"));

    			$("#peers_uploaded_volume, #peers_downloaded_volume, #peers_connected, #peers_up_to_date").html("0").removeClass("loading_dots");
    			
    			NRS.pageLoaded();
    		}
    	});
    }
    
    /* GENERATE TOKEN */
    $("#generate_token_modal").on("show.bs.modal", function(e) {
    	$("#generate_token_website").val("http://");    
    	$("#generate_token_token").html("").hide();
    });
    
	NRS.forms.generateToken = function($modal) {		
		var url = $.trim($("#generate_token_website").val());
				
		if (!url || url == "http://") {
			return {"error": "Website is a required field."};
			$("#generate_token_token").html("").hide();
		} else {
			return {};
		}
	}
	
    NRS.forms.generateTokenComplete = function(response, data) {
    	$("#generate_token_modal").find(".error_message").hide();
    	
    	if (response.token) {
    		$("#generate_token_token").html("The generated token for <strong>" + data.website.escapeHTML() + "</strong> is: <br /><br /><textarea style='width:100%' rows='3'>" + response.token.escapeHTML() + "</textarea>").show();
    	} else {
	    	$.growl("Could not generate token.", {"type": "danger"});
	    	$("#generate_token_modal").modal("hide");
    	}
    }
    
    //hide modal when another one is activated.
    $(".modal").on("show.bs.modal", function(e) {
    	var $visible_modal = $(".modal.in");
    	
    	if ($visible_modal.length) {
	    	$visible_modal.modal("hide");
    	}
    });
        
    
    $(".modal button.btn-primary:not([data-dismiss=modal])").click(function() {
    	var $btn = $(this);
    	
    	var $modal = $btn.closest(".modal");
    	
    	$modal.modal("lock");
    	$btn.button("loading");    	
    	
    	var requestType    = $modal.find("input[name=request_type]").val();
    	var successMessage = $modal.find("input[name=success_message]").val();
    	var errorMessage   = $modal.find("input[name=error_message]").val();
    	var data		   = null;
    	    	
    	var formFunction = NRS["forms"][requestType];
    	
    	var originalRequestType = requestType;
    	
    	if (typeof formFunction == 'function') {
    		var output = formFunction($modal);
    	    		
			if (!output) {
				return;
			} else if (output.error) {
    			$modal.find(".error_message").html(output.error.escapeHTML()).show();
    			$btn.button("reset");
    			$modal.modal("unlock");
    			return;
    		} else {
    			if (output.requestType) {
    				requestType = output.requestType;
    			}
    			if (output.data) {
    				data = output.data;
    			}
    			if (output.successMessage) {
    				successMessage = output.successMessage;
    			}
    			if (output.errorMessage) {
    				errorMessage = output.errorMessage;
    			}
    			if (output.stop) {
    				$btn.button("reset");
    				$modal.modal("unlock");
    				$modal.modal("hide");
	    			return;
    			}
    		}
    	}
    	
    	if (!data) {
    		data = NRS.getFormData($modal.find("form:first"));
    	}

		if (data.deadline) {
			data.deadline = String(data.deadline * 60); //hours to minutes
		}    
		
		if (data.recipient) {
			data.recipient = $.trim(data.recipient);
			if (!/^\d+$/.test(data.recipient)) {
				var convertedAccountId = $modal.find("input[name=converted_account_id]").val();
				if (!convertedAccountId || !/^\d+$/.test(convertedAccountId)) {
					$modal.find(".error_message").html("Invalid account ID.").show();
					$btn.button("reset");
					$modal.modal("unlock");
					return;
				} else {
					data.recipient = convertedAccountId;
					data["_extra"] = {"convertedAccount": true};
				}
			}
		}
		
		if ("secretPhrase" in data && !data.secretPhrase.length) {
			$modal.find(".error_message").html("Secret phrase is a required field.").show();
			$btn.button("reset");
			$modal.modal("unlock");
			return;
		}
			    	
    	NRS.sendRequest(requestType, data, function(response) {      		  		
    		if (response.errorCode) {   
    		    if (NRS.forms.errorMessages[requestType] && NRS.forms.errorMessages[requestType][response.errorCode]) {
    				$modal.find(".error_message").html(NRS.forms.errorMessages[requestType][response.errorCode].escapeHTML()).show();
    			} else if (NRS.forms.errorMessages[originalRequestType] && NRS.forms.errorMessages[originalRequestType][response.errorCode]) {
    				$modal.find(".error_message").html(NRS.forms.errorMessages[originalRequestType][response.errorCode].escapeHTML()).show();
    			} else {
    				$modal.find(".error_message").html(response.errorDescription ? response.errorDescription.escapeHTML() : "Unknown error occured.").show();
    			}
    			$btn.button("reset");
    			$modal.modal("unlock");
    		} else if (response.hash) {
    			//should we add a fake transaction to the recent transactions?? or just wait until the next block comes!??
    			$btn.button("reset");
    			$modal.modal("unlock");
    			
    			if (!$modal.hasClass("modal-no-hide")) {
	    		 	$modal.modal("hide");	
				}
				
				if (successMessage) {
	    		 	$.growl(successMessage.escapeHTML(), { type: 'success' });
				}
				
    		 	var formCompleteFunction = NRS["forms"][originalRequestType + "Complete"];
    		 	
    		 	if (typeof formCompleteFunction == 'function') {
    		 		data.requestType = requestType;
    		 		formCompleteFunction(response, data);
    		 	}
    		} else {
    			var sentToFunction = false;
    			
    			if (!errorMessage) {
					var formCompleteFunction = NRS["forms"][originalRequestType + "Complete"];
					
					if (typeof formCompleteFunction == 'function') {
						sentToFunction = true;
						data.requestType = requestType;
						
						$btn.button("reset");
						$modal.modal("unlock");
						
						if (!$modal.hasClass("modal-no-hide")) {
							$modal.modal("hide");	
						}
						formCompleteFunction(response, data);
					} else {
						errorMessage = "An unknown error occured.";
					}
    			}
    			
    			if (!sentToFunction) {
	    			$btn.button("reset");
	    			$modal.modal("unlock");
	    			$modal.modal("hide");
	    				
	    			$.growl(errorMessage.escapeHTML(), { type: 'danger' });
	    		}
    		}
    	});
    });
    
    $("#send_message_modal, #send_money_modal, #add_contact_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);
		
		var account = $invoker.data("account");
		
		if (account) {
			account = String(account);
			$(this).find("input[name=recipient], input[name=account_id]").val(account.unescapeHTML()).trigger("blur");
		}
    });
    
    $("#send_money_amount").on("input", function(e) {
	    var amount = parseInt($(this).val(), 10);
        $("#send_money_fee").val(isNaN(amount) ? "1" : (amount < 500 ? 1 : Math.round(amount / 1000)));
    });
    
    NRS.sendMoneyShowAccountInformation = function(accountId) {
    	NRS.getAccountError(accountId, function(response) {
    		if (response.type == "success") {
    			$("#send_money_account_info").hide();
    		} else {
    			$("#send_money_account_info").html(response.message).show();
    			
    		}
    	});
    }
    
    NRS.getAccountError = function(accountId, callback) {    	
		NRS.sendRequest("getAccount", {"account": accountId}, function(response) {						
			if (response.publicKey) {
				callback({"type": "success"});
			} else {
				if (response.errorCode) {
					if (response.errorCode == 4) {
						callback({"type": "danger", "message": "The recipient account is malformed, please adjust. If you want to type an alias, prepend it with the @ character."});
					} else if (response.errorCode == 5) {
						callback({"type": "warning", "message": "The recipient account is an unknown account, meaning it has never had an incoming or outgoing transaction. Please double check your recipient address before submitting."});
					} else {
						callback({"type": "danger", "message": "There is a problem with the recipient account: " + response.errorDescription});
					}
				} else {
					callback({"type": "warning", "message": "The recipient account does not have a public key, meaning it has never had an outgoing transaction." + (response.balance == 0 ? " The account has a zero balance." : " The account has a balance of " + NRS.formatAmount(response.balance/100).unescapeHTML() + " NXT.") + " Please double check your recipient address before submitting."});
				}
			}
		});
    }    
    
    NRS.checkRecipient = function(account, modal) {
    	var classes = "callout-info callout-danger callout-warning";
    	
    	var callout = modal.find(".account_info").first();
    	var accountInputField = modal.find("input[name=converted_account_id]");
    	
    	accountInputField.val("");
    	
    	account = $.trim(account);
    	    	
		if (!(/^\d+$/.test(account))) {
			if (NRS.databaseSupport && account.charAt(0) != '@') {
				NRS.database.select("contacts", [{"name": account}], function(contact) {	
					if (contact.length) {
						contact = contact[0];
						NRS.getAccountError(contact.accountId, function(response) {
							if (response.type == "success") {
								callout.removeClass(classes).addClass("callout-info").html("The contact links to account <strong>" + String(contact.accountId).escapeHTML() + "</strong>, which has a public key.").show();
							} else {
								var message = "The contact links to account <strong>" + String(contact.accountId).escapeHTML() + "</strong>. " + response.message.escapeHTML();
								
								callout.removeClass(classes).addClass("callout-" + response.type).html(message).show();
							}
							
							if (response.type == "success" || response.type == "warning") {
								accountInputField.val(contact.accountId);
							}
						});
					} else {
						NRS.checkRecipientAlias(account, modal);
					}
				});
			} else {
				if (account.charAt(0) == '@') {
					account = account.substring(1);
					NRS.checkRecipientAlias(account, modal);
				}
			}
		} else {
			NRS.getAccountError(account, function(response) {
				if (response.type == "success") {
					callout.removeClass(classes).addClass("callout-info").html("The account has a public key.").show();
				} else {
					callout.removeClass(classes).addClass("callout-" + response.type).html(response.message.escapeHTML()).show();
				}
			});
		}
    }
	
	NRS.checkRecipientAlias = function(account, modal) {
	    var classes = "callout-info callout-danger callout-warning";
	    var callout = modal.find(".account_info").first();
    	var accountInputField = modal.find("input[name=converted_account_id]");

		NRS.sendRequest("getAliasId", {"alias": account}, function(response) {
			if (response.id) {
				NRS.sendRequest("getAlias", {"alias": response.id}, function(response) {
					if (response.errorCode) {
						callout.removeClass(classes).addClass("callout-danger").html(response.errorDescription ? "Error: " + response.errorDescription.escapeHTML() : "The alias does not exist.").show();
					} else {
						/*
						if (response.timestamp < currentTime - 60*60*24) {
						
						}*/
						
						if (response.uri) {
							var alias = response.uri;
							var timestamp = response.timestamp;
							
							var regex_1 = /acct:(\d+)@nxt/;
							var regex_2 = /nacc:(\d+)/;
							
							var match = alias.match(regex_1);
							
							if (!match) {
								match = alias.match(regex_2);
							}
							
							if (match && match[1]) {
								NRS.getAccountError(match[1], function(response) {
									accountInputField.val(match[1].escapeHTML());
									if (response.type == "success") {
										callout.html("The alias links to account <strong>" + match[1].escapeHTML() + "</strong>, which has a public key. The alias was last adjusted on " + NRS.formatTimestamp(timestamp) + ".").removeClass(classes).addClass("callout-info").show();
									} else {
										var message = "The alias links to account <strong>" + match[1].escapeHTML() + "</strong> and was last adjusted on " + NRS.formatTimestamp(timestamp) + ". " + response.message.escapeHTML();
										
										callout.removeClass(classes).addClass("callout-" + response.type).html(message).show();
									}
								});
							} else {
								callout.removeClass(classes).addClass("callout-danger").html("The alias does not link to an account. " + (!alias ? "The URI is empty." : "The URI is '" + alias.escapeHTML() + "'")).show();
							}
						} else if (response.alias) {
							callout.removeClass(classes).addClass("callout-danger").html("The alias links to an empty URI.").show();
						} else {
							callout.removeClass(classes).addClass("callout-danger").html(response.errorDescription ? "Error: " + response.errorDescription.escapeHTML() : "The alias does not exist.").show();
						}
					}
				});
			} else {
				callout.removeClass(classes).addClass("callout-danger").html(response.errorDescription ? "Error: " + response.errorDescription.escapeHTML() : "The alias does not exist.").show();
			}
		});    
	}
	
	NRS.convertToHex16 = function(str) {
	    var hex, i;
	    var result = "";
	    for (i=0; i<str.length; i++) {
	      hex = str.charCodeAt(i).toString(16);
	      result += ("000"+hex).slice(-4);
	    }
	    
	    return result;
	}
	
	NRS.convertFromHex16 = function(hex) {
		var j;
		var hexes = hex.match(/.{1,4}/g) || [];
		var back = "";
		for(j = 0; j<hexes.length; j++) {
		  back += String.fromCharCode(parseInt(hexes[j], 16));
		}
		
		return back;
	}

	NRS.convertFromHex8 = function(hex) {
	    var hex = hex.toString();//force conversion
	    var str = '';
	    for (var i = 0; i < hex.length; i += 2)
	        str += String.fromCharCode(parseInt(hex.substr(i, 2), 16));
	    return str;
	}
	
	NRS.convertToHex8 = function(str) {
	    var hex = '';
	    for(var i=0;i<str.length;i++) {
	        hex += ''+str.charCodeAt(i).toString(16);
	    }
	    return hex;
	}
	
	NRS.createDatabase = function() {
		if (indexedDB) {
			indexedDB.deleteDatabase("NRS");
		}
		var schema = {
		    contacts:{
		    	id: "INTEGER PRIMARY KEY AUTOINCREMENT",
		        name: "VARCHAR(100) COLLATE NOCASE",
		        email: "VARCHAR(200)",
		        accountId: "VARCHAR(100)",
		        description: "TEXT"
		    },
		    assets: {
		    	account: "VARCHAR(100)",
		        assetId: "VARCHAR(100)",
		        description: "TEXT",
		        name: "VARCHAR(10)",
		        quantity: "NUMBER",
		        groupName: "VARCHAR(100)"
		    },
		    data: {
		    	id: "VARCHAR(100)",
		    	contents: "TEXT"
		    }
		};
		
		try {
			NRS.database = new WebDB("NXT", schema, 2, 4, function() {
				NRS.databaseSupport = true;
				NRS.loadContacts();
    			NRS.database.select("data", [{"id": "closed_groups"}], function(result) {
    				if (result.length) {
	    				NRS.closedGroups = result[0].contents.split("#");
    				} else {
					    NRS.database.insert("data", {id: "closed_groups", contents: ""});
    				}
    			});
			});
		} catch (err) {
			NRS.database = null;
			NRS.databaseSupport = false;
		}
	}

    NRS.login = function(password, callback) {
    	$("#login_password, #registration_password, #registration_password_repeat").val("");
    	    	
    	if (!password.length) {
    		$.growl("You must enter your secret phrase. If you don't have one, click the registration button below.", {"type": "danger", "offset": 10});
    		return;
    	}	
 
    	NRS.sendRequest("getAccountId", {"secretPhrase": password}, function(response) {
    		if (!response.errorCode) {
    			NRS.account = String(response.accountId).escapeHTML();
    		}
    		
    		if (!NRS.account) {
	    		return;
	    	}
	    	
	    	NRS.sendRequest("getAccountPublicKey", {"account": NRS.account}, function(response) {
	    		if (response && response.publicKey && response.publicKey != NRS.generatePublicKey(password)) {
					$.growl("This account is already taken. Please choose another pass phrase.", {"type": "danger", "offset": 10});
					return;	
				}
				
		    	$("#account_id").html(NRS.account);
		    		    	
		    	var passwordNotice = "";
		    	
		    	 if (password.length < 35) {
				   	passwordNotice = "Your secret phrase is less than 35 characters long. This is not secure.";
				 } else if (password.length < 50 && (!password.match(/[A-Z]/) || !password.match(/[0-9]/))) {
					 passwordNotice = "Your secret phrase does not contain numbers and uppercase letters. This is not secure.";
				 } 
					    
				if (passwordNotice) {
					$.growl("<strong>Warning</strong>: " + passwordNotice, {"type": "danger"});
				}
				
				NRS.getAccountBalance(true);
		    		    	 
		    	NRS.sendRequest("startForging", {"secretPhrase": password}, function(response) {
		    		if ("deadline" in response) {
						$("#forging_indicator i.fa").removeClass("text-danger").addClass("text-success");
						$("#forging_indicator span").html("Forging");
		    		} else {
						$("#forging_indicator i.fa").removeClass("text-success").addClass("text-danger");
						$("#forging_indicator span").html("Not Forging");
		    		}
		    		$("#forging_indicator").show();
		    	});
		    	    	 
		    	//NRS.getAccountAliases();
		    	    	    
		    	NRS.unlock();
		    	
		    	NRS.setupClipboardFunctionality();
		    	
		    	if (callback) {
			    	callback();
		    	}
		    	
		    	NRS.checkLocationHash(password);
		    		       		
				$(window).on("hashchange", NRS.checkLocationHash);
				
		    	NRS.sendRequest('getAccountTransactionIds', {"account": NRS.account, "timestamp": 0}, function(response) {
					if (response.transactionIds && response.transactionIds.length) {
			    		var transactionIds = response.transactionIds.reverse().slice(0, 10);
			    		var nrTransactions = 0;
			    		var transactions = {};
			    				    				    				
			    		for (var i=0; i<transactionIds.length; i++) {
			    			NRS.sendRequest('getTransaction', {"transaction": transactionIds[i]}, function(transaction, input) {		    				
			    				nrTransactions++;
			    				transactions[input.transaction] = transaction;
			    						    				
			    				if (nrTransactions == transactionIds.length) {
				    				var transactionsArray = [];
				    				
				    				for (var i=0; i<nrTransactions; i++) {
					    				transactionsArray.push(transactions[transactionIds[i]]);
				    				}
				    				
				    				NRS.handleInitialTransactions(transactionsArray, transactionIds);
			    				}
			    			});
			    		}
		    		} else {
		    			NRS.dataLoadFinished($("#dashboard_transactions_table"));
		    		}
		    	});
	    	});
	    });
    }
    
    NRS.setupClipboardFunctionality = function() {
    	if (!NRS.isLocalHost) {
	    	var $el = $("#account_id_dropdown .dropdown-menu a");
    	} else {
	    	var $el = $("#account_id");
    	}
    	
		var clipboard = new ZeroClipboard($el, {
			moviePath: "js/ZeroClipboard.swf"
		});
		
    	if (!NRS.isLocalHost) {    		
			clipboard.on("dataRequested", function (client, args) {
				switch ($(this).data("type")) {
					case "account_id": 
						client.setText(NRS.account);
						break;
					case "message_link": 
						client.setText(document.URL.replace(/#.*$/, "") + "#message:" + NRS.account);
						break;
					case "send_link":
						client.setText(document.URL.replace(/#.*$/, "") + "#send:" + NRS.account);
						break;
				}
			});
    	} else {
    		$el.removeClass("dropdown-toggle").data("toggle", "");
    		$("#account_id_dropdown").remove(".dropdown-menu");

    		clipboard.on("dataRequested", function(client, args) {
	    		client.setText(NRS.account);
    		});
    	}
    		
		clipboard.on("complete", function(client, args) {
			$.growl("Copied to the clipboard successfully.", {"type": "success"});
		});

		clipboard.on("noflash", function (client, args) {
			$.growl("Your browser doesn't support flash, therefore copy to clipboard functionality will not work.", {"type": "danger"});
		});
		clipboard.on("wrongflash", function(client, args) {
			$.growl("Your browser flash version is too old. The copy to clipboard functionality needs version 10 or newer.");
		});
    }
    
    NRS.checkLocationHash = function(password) {    	
    	if (window.location.hash) {	    		
	    	var hash = window.location.hash.replace("#", "").split(":")
	    			 
	   		if (hash.length == 2) {
		    	if (hash[0] == "message") {
		    		var $modal = $("#send_message_modal");
		    	} else if (hash[0] == "send") {
			    	var $modal = $("#send_money_modal");
		    	} else {
			    	var $modal = "";
		    	}
		    				    	
		    	if ($modal) {
		    		var account_id = String($.trim(hash[1]));
		    		if (!/^\d+$/.test(account_id) && account_id.indexOf("@") !== 0) {
			    		account_id = "@" + account_id;
		    		}
		    		
					$modal.find("input[name=recipient]").val(account_id.unescapeHTML()).trigger("blur");
					if (password && typeof password == "string") {
						$modal.find("input[name=secretPhrase]").val(password);
					}
					$modal.modal("show");
				}
			}
			
	    	window.location.hash = "#";
		}
	}
    
    NRS.getAccountBalance = function(firstRun) {
    	NRS.sendRequest("getAccount", {"account": NRS.account}, function(response) {    
    		var previousAccountBalance = NRS.accountBalance;
    				
    		NRS.accountBalance = response;
    		    		
    		if (response.errorCode) {
	    		$("#account_balance").html("0");
	    		$("#account_nr_assets").html("0");
	    		
				if (NRS.accountBalance.errorCode == 5) {
					$("#dashboard_message").addClass("alert-success").removeClass("alert-danger").html("Welcome to your brand new account. You should fund it with some coins. Your account ID is: <strong>" + NRS.account + "</strong>").show();
				} else {
					$("#dashboard_message").addClass("alert-danger").removeClass("alert-success").html(NRS.accountBalance.errorDescription ? NRS.accountBalance.errorDescription.escapeHTML() : "An unknown error occured.").show();
				}
    		} else {    			
    			if (!NRS.accountBalance.publicKey) {
					$("#dashboard_message").addClass("alert-danger").removeClass("alert-success").html("<b>Warning!</b>: Your account does not have a public key! This means it's not as protected as other accounts. You must make an outgoing transaction to fix this issue.").show();
    			} else {
	    			$("#dashboard_message").hide();
    			}
    			
    			if (NRS.databaseSupport) {
    				NRS.database.select("data", [{"id": "asset_balances_" + NRS.account}], function(asset_balance) {
						if (asset_balance.length) {
						    var previous_balances = asset_balance[0].contents;
						    var current_balances = JSON.stringify(NRS.accountBalance.assetBalances);
						    						    
						    if (previous_balances != current_balances) {
							    previous_balances = JSON.parse(previous_balances);
								NRS.database.update("data", {contents: current_balances}, [{id: "asset_balances_" + NRS.account}]);
								NRS.checkAssetDifferences(NRS.accountBalance.assetBalances, previous_balances);
						    } 
						} else {
					    	NRS.database.insert("data", {id: "asset_balances_" + NRS.account, contents: JSON.stringify(NRS.accountBalance.assetBalances)});
						}
					});
				} else if (previousAccountBalance && previousAccountBalance.assetBalances) {
					var previous_balances = JSON.stringify(previousAccountBalance.assetBalances);
					var current_balances = JSON.stringify(NRS.accountBalance.assetBalances);
					
					if (previous_balances != current_balances) {
						NRS.checkAssetDifferences(NRS.accountBalance.assetBalances, previousAccountBalance.assetBalances);
					}
				}

	    		if (response.balance > 0) {
	    			var balance = response.balance;
	    		} else {
	    			var balance = response.effectiveBalance;
	    		}
	    	    
	    	    if (!balance) {
	    	    	balance = 0;
	    	    }	    		
	    	    
	    		if (balance) {
		    		$("#account_balance").html(NRS.formatAmount(balance/100));
	    		} else {
	    			$("#account_balance").html("0");
	    		}		
	    		
	    		var nr_assets = (response.assetBalances ? response.assetBalances.length : 0);
	    		$("#account_nr_assets").html(nr_assets);
	    	}
	    	
	    	if (firstRun) {
		    	$("#account_balance, #account_nr_assets").removeClass("loading_dots");
	    	}
    	});
    }
    
    NRS.checkAssetDifferences = function(current_balances, previous_balances) {
    	var current_balances_  = {};
    	var previous_balances_ = {};
    		    
	    for (var k in previous_balances) {
		    previous_balances_[previous_balances[k].asset] = previous_balances[k].balance;
	    }
	    
	    for (var k in current_balances) {
		    current_balances_[current_balances[k].asset] = current_balances[k].balance;
	    }
	    
	    var diff = {};
	    
	    for (var k in previous_balances_) {
			if (!(k in current_balances_)) {
				diff[k] = -(previous_balances_[k]);
			} else if (previous_balances_[k] !== current_balances_[k]) {
				var change = current_balances_[k] - previous_balances_[k];
				diff[k] = change;
			}
		}
		
		for (k in current_balances_) {
			if (!(k in previous_balances_)) {
				diff[k] = current_balances_[k]; // property is new
			}
		}
				
		var nr = Object.keys(diff).length;
			
		if (nr == 0) {
			return;
		} else if (nr <= 3) {
			for (k in diff) {
				NRS.sendRequest("getAsset", {"asset": k, "_extra": {"difference": diff[k]}}, function(asset, input) {					
					asset.difference = input["_extra"].difference;
					
					if (asset.difference > 0) {
						$.growl("You received " + NRS.formatAmount(asset.difference) + " " + asset.name.escapeHTML() + (asset.difference == 1 ? " asset" : " assets") + ".", {"type": "success"});
					} else {
						$.growl("You sold " + NRS.formatAmount(Math.abs(asset.difference)) + " " +  asset.name.escapeHTML() + ( asset.difference == 1 ? " asset" : "assets") + ".", {"type": "success"});
					}
				});
			}
		} else {
			$.growl("Multiple different assets have been sold and/or bought.", {"type": "success"});
		}
    }
    
    NRS.handleInitialTransactions = function(transactions, transactionIds) {       	 	
    	if (transactions.length) {
    	   	var rows = "";
    	
	    	transactions.sort(NRS.sortArray);
	    		    		    	
	    	if (transactions.length >= 1) {
	    		NRS.lastTransactionsTimestamp = transactions[transactions.length-1].timestamp;	//we take oldest timestamp, not newest!
	    		NRS.lastTransactions = transactionIds.toString();
	    	}
	    	
	    	for (var i=0; i<transactions.length; i++) {
	    		var transaction = transactions[i];
	    		    			
				var receiving = transaction.recipient == NRS.account;
				var account = (receiving ? String(transaction.sender).escapeHTML() : String(transaction.recipient).escapeHTML());

				rows += "<tr><td>" + (transaction.attachment ? "<a href='#' data-transaction='" + String(transactionIds[i]).escapeHTML() + "' style='font-weight:bold'>" + NRS.formatTimestamp(transaction.timestamp) + "</a>" : NRS.formatTimestamp(transaction.timestamp)) + "</td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fa fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fa fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td><span" + (transaction.type == 0 && receiving ? " style='color:#006400'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + NRS.formatAmount(transaction.amount) + "</span> <span" + ((!receiving && transaction.type == 0) ? " style='color:red'" : "") + ">+</span> <span" + (!receiving ? " style='color:red'" : "") + ">" + NRS.formatAmount(transaction.fee) +  "</span></td><td>" + (account != NRS.genesis ? "<a href='#' data-user='" + account + "' class='user_info'>" + NRS.getAccountTitle(account) + "</a>" : "Genesis") + "</td><td data-confirmations='" + String(transaction.confirmations).escapeHTML() + "' data-initial='true'>" + (transaction.confirmations > 10 ? "10+" : String(transaction.confirmations).escapeHTML()) + "</td></tr>";
			}
			
			$("#dashboard_transactions_table tbody").empty().append(rows);
		}
		
		NRS.dataLoadFinished($("#dashboard_transactions_table"));
    }
        
    
    NRS.sortArray = function(a, b) {
    	return b.timestamp - a.timestamp;
    }
           
    NRS.forms.errorMessages.startForging = {"5": "You cannot forge. Either your balance is 0 or your account is too new (you must wait a day or so)."};

    NRS.forms.startForgingComplete = function(response, data) {
    	if ("deadline" in response) {
			$("#forging_indicator i.fa").removeClass("text-danger").addClass("text-success");
			$("#forging_indicator span").html("Forging");
			$.growl("Forging started successfully.", { type: "success" });
    	} else {
    		$.growl("Couldn't start forging, unknown error.", { type: 'danger' });
    	}
    }
    
    NRS.forms.stopForgingComplete = function(response, data) {
	    $("#forging_indicator i.fa").removeClass("text-success").addClass("text-danger");
	    $("#forging_indicator span").html("Not forging");
    
    	if (response.foundAndStopped) {
    		$.growl("Forging stopped successfully.", { type: 'success' });
    		
    	} else {
    		$.growl("You weren't forging to begin with.", { type: 'danger' });
		}    	
    }
    
    $("#forging_indicator").click(function(e) {
    	e.preventDefault();
    	
    	var $forgingIndicator = $(this).find("i.fa-circle");
    	
    	if ($forgingIndicator.hasClass("text-success")) {
    		$("#stop_forging_modal").modal("show");
    	} else {
    	   	$("#start_forging_modal").modal("show");
    	}
    });
    
    NRS.showConsole = function() {	    
	    NRS.console = window.open("", "console", "width=750,height=400,menubar=no,scrollbars=yes,status=no,toolbar=no,resizable=yes");
	    $(NRS.console.document.head).html("<title>Console</title><style type='text/css'>body { background:black; color:white; font-family:courier-new,courier;font-size:14px; } pre { font-size:14px; }</style>");
	    $(NRS.console.document.body).html("Console opened. Logging started...<div id='console'></div>");
    }
    
    NRS.addToConsole = function(url, type, data, response, error) {
    	if (!NRS.console) {
	    	return;
    	}
    	
    	if (!NRS.console.document || !NRS.console.document.body) {
	    	NRS.console = null;
	    	return;
    	}
    	
    	url = url.replace(/&random=[\.\d]+/, "", url);

		NRS.addToConsoleBody(url + " (" + type + ") " + new Date().toString(), "url");
		
		if (data) {			
			if (typeof data == "string") {
				var d = NRS.queryStringToObject(data);
				NRS.addToConsoleBody(JSON.stringify(d, null, "\t"), "post");
			} else {
				NRS.addToConsoleBody(JSON.stringify(data, null, "\t"), "post");
			}
		}
		
		if (error) {
			NRS.addToConsoleBody(response, "error");
		} else {
			NRS.addToConsoleBody(JSON.stringify(response, null, "\t"), (response.errorCode ? "error" : ""));
		}
    } 
    
    NRS.addToConsoleBody = function(text, type) {    	
    	var color = "";
    	
    	switch (type) {
	    	case "url": 
	    		color = "#29FD2F";
	    		break;
	    	case "post":
	    		color = "lightgray";
	    		break;
	    	case "error":
	    		color = "red";
	    		break;
    	}

	    $(NRS.console.document.body).find("#console").append("<pre" + (color ? " style='color:" + color + "'" : "") + ">" + text.escapeHTML() + "</pre>");
    }
    
    NRS.queryStringToObject = function(qs) {    	
    	qs = qs.split("&");
    	
    	if (!qs) {
    		return {};
    	}    	
    		    
	    var obj = {};
	    
	    for (var i=0; i <qs.length; ++i) {
	        var p = qs[i].split('=');
	        	        
	        if (p.length != 2) {
	        	continue;
	        }
	        
	        obj[p[0]] = decodeURIComponent(p[1].replace(/\+/g, " "));
	    }
	    
	    if ("secretPhrase" in obj) {
		    obj.secretPhrase = "***";
	    }
	    	    
	    return obj;
	}
    
    $(document).ajaxComplete(function(event, xhr, settings) {	
    	if (xhr._page && xhr.statusText != "abort") {
    		var index = $.inArray(xhr, NRS.xhrPool);
    		if (index > -1) {
    			NRS.xhrPool.splice(index, 1);
    		}
    	}
    });
    
    NRS.abortOutstandingRequests = function(subPage) {
	    $(NRS.xhrPool).each(function(id, xhr) {
	    	if (subPage) {
		    	if (xhr._subPage) {
			    	xhr.abort();
		    	}
	    	} else {
		    	xhr.abort();
	    	}
	    });
	    
	    if (!subPage) {
	   		NRS.xhrPool = [];
	   	}
    }
    
    NRS.beforeSendRequest = function(xhr) {
    	xhr._page = true;
    	if (NRS.currentSubPage) {
	    	xhr._subPage = true;
    	}
    	NRS.xhrPool.push(xhr);
    }
    
    NRS.sendOutsideRequest = function(url, data, callback, async) {
	    if ($.isFunction(data)) { 
	    	async = callback;
    		callback = data;
    		data = {};
    	} else {
	        data = data || {};
        }

	    $.support.cors = true;
	    
	    $.ajax({
          url: url,
          crossDomain: true,
          dataType: "json",
          type: "GET",
          timeout: 10000,
          async: (async === undefined ? true : async),
          data: data
        }).done(function(json) {  
        	if (json.errorCode && !json.errorDescription) {
        		json.errorDescription = (json.errorMessage ? json.errorMessage : "Unknown error occured.");
        	}    
        	if (callback) {
        		callback(json, data);
        	}
        }).fail(function(xhr, textStatus, error) {
        	if (callback) {
        		callback({"errorCode": -1, "errorDescription": error}, {});
        	}
        });
    }
    
    NRS.sendRequest = function(requestType, data, callback, async) {     
    	if (requestType == undefined) {
    		return;
    	}    	  	
    	
    	if ($.isFunction(data)) {
    		async = callback;
    		callback = data;
    		data = {};
    	} else {
	        data = data || {};
        }
                
        $.each(data, function(key, val) {
	    	if (key != "secretPhrase") {
	    		if (typeof val == "string") {
	    			data[key] = $.trim(val);
	    		}
	    	} 
        });
                        
        //gets account id from secret phrase client side, used only for login.
        if (requestType == "getAccountId") {
        	var accountId = NRS.generateAccountId(data.secretPhrase, true);
        	        	
        	if (callback) {
	        	callback({"accountId": accountId});
        	}
        	return;
        }
             	
        //check to see if secretPhrase supplied matches logged in account, if not - show error.
        if ("secretPhrase" in data) {
		    var accountId = NRS.generateAccountId(data.secretPhrase);
	    	
	    	if (accountId != NRS.account) {		    		
	        	if (callback) {
		        	callback({"errorCode": 1, "errorDescription": "Incorrect secret phrase."});
	        	}
	        	return;
	    	} else {
	    		//ok, accountId matches..continue with the real request.
	        	NRS.processAjaxRequest(requestType, data, callback, async);
	    	}
        } else {
	     	NRS.processAjaxRequest(requestType, data, callback, async);
        }
    }
    
    NRS.processAjaxRequest = function(requestType, data, callback, async) {
		if (data["_extra"]) {
			var extra = data["_extra"];
			delete data["_extra"];
		} else {
			var extra = null;
		}
		
		var beforeSend = null;
		    
		//means it is a page request, not a global request.. Page requests can be aborted.
		if (requestType.slice(-1) == "+") {
			requestType = requestType.slice(0, -1);
			
			beforeSend = NRS.beforeSendRequest;
		} else {
			//not really necessary... we can just use the above code..
			var plusCharacter = requestType.indexOf("+");
			
			if (plusCharacter > 0) {
				var subType = requestType.substr(plusCharacter);
				requestType = requestType.substr(0, plusCharacter);
		 	 	beforeSend = NRS.beforeSendRequest;
			}
		}
     	
     	var type = ("secretPhrase" in data ? "POST" : "GET");
     	var url = NRS.server + "/nxt?requestType=" + requestType;
	 	
	 	if (type == "GET") {
		 	if (typeof data == "string") {
				data += "&random=" + Math.random();
			} else {
				data.random = Math.random();
			}
	 	}
	 	
	 	var secretPhrase = "";
	 	
	 	if (!NRS.isLocalHost && type == "POST") {
		 	secretPhrase = data.secretPhrase;
		 	delete data.secretPhrase;
		 	data.publicKey = NRS.accountBalance.publicKey;
	 	}
	 	
     	$.support.cors = true;
     		 	     		 	
		$.ajax({
			url: url,
			crossDomain: true,
			dataType: "json",
			type: type,
			timeout: 10000, //10 seconds
			async: (async === undefined ? true : async),
			beforeSend: beforeSend,
			data: data
		}).done(function(response, status, xhr) {  
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, response);
			}
												
			if (secretPhrase && response.transactionBytes && !response.errorCode) {
				var publicKey = NRS.generatePublicKey(secretPhrase);
				var signature = nxtCrypto.sign(response.transactionBytes, converters.stringToHexString(secretPhrase));
				
				if (!nxtCrypto.verify(signature, response.transactionBytes, publicKey)) {
					if (callback) {						
						callback({"errorCode": 1, "errorDescription": "Could not verify signature (client side)."}, data);
					} else {
						$.growl("Could not verify signature.", {"type": "danger"});
					}
					return;
				} else {
					var payload = response.transactionBytes.substr(0,128) + signature + response.transactionBytes.substr(256);
					
					if (!NRS.verifyTransactionBytes(payload, requestType, data)) {
						if (callback) {
							callback({"errorCode": 1, "errorDescription": "Could not verify transaction bytes (server side)."}, data);
						} else {
							$.growl("Could not verify transaction bytes.", {"type": "danger"});
						}
						return;
					} else {
						if (callback) {
							if (extra) {
								data["_extra"] = extra;
							}
							
							NRS.broadcastTransactionBytes(payload, callback, response, data);
						} else {
							NRS.broadcastTransactionBytes(payload);
						}
					}
				}
			} else {
				if (response.errorCode && !response.errorDescription) {
					response.errorDescription = (response.errorMessage ? response.errorMessage : "Unknown error occured.");
				}    
				        	
				if (callback) {
					if (extra) {
						data["_extra"] = extra;
					}
					callback(response, data);
				}
			}
		}).fail(function(xhr, textStatus, error) {
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, error, true);
			}

			if ((error == "error" || textStatus == "error") && (xhr.status == 404 || xhr.status == 0)) {
				if (type == "POST") {
		    		$.growl("Could not connect.", {"type": "danger", "offset": 10});
		    	}
			} 
			        	
			if (error == "abort") {
				return;
			} else if (callback) {
				if (error == "timeout") {
		    		error = "The request timed out. Warning: This does not mean the request did not go through. You should wait for the next block and see if your request has been processed.";
				}
		 		callback({"errorCode": -1, "errorDescription": error}, {});
			}
		});
    }

    NRS.verifyTransactionBytes = function(transactionBytes, requestType, data) {	    
	    var transaction = {};
	    
	    var currentPosition = 0;
	    
		var byteArray = converters.hexStringToByteArray(transactionBytes);
				
		transaction.type      = byteArray[0];
		transaction.subType   = byteArray[1];
		transaction.timestamp = String(converters.byteArrayToSignedInt32(byteArray, 2));
		transaction.deadline  = String(converters.byteArrayToSignedShort(byteArray, 6));
		//sender public key == bytes 8 - 39
		transaction.recipient = String(converters.byteArrayToBigInteger(byteArray, 40));
		transaction.amount    = String(converters.byteArrayToSignedInt32(byteArray, 48));
		transaction.fee 	  = String(converters.byteArrayToSignedInt32(byteArray, 52));
		transaction.referencedTransaction = String(converters.byteArrayToBigInteger(byteArray, 56));
				
		if (transaction.referencedTransaction == "0") {
			transaction.referencedTransaction = null;
		}
		
		//signature == 64 - 127

		if (!("amount" in data)) {
			data.amount = "0";
		}
		
		if (!("recipient" in data)) {
			//recipient == genesis
			data.recipient = "1739068987193023818";
		}
		
		if (transaction.deadline !== data.deadline || transaction.recipient !== data.recipient || transaction.amount !== data.amount || transaction.fee !== data.fee) {
			return false;
		}
		
		if ("referencedTransaction" in data && transaction.referencedTransaction != data.referencedTransaction) {
			return false;
		}
		
		var pos = 128;
		
		switch (requestType) {
			case "sendMoney":
				if (transaction.type !== 0 || transaction.subType !== 0) {
					return false;
				}
				break;
			case "sendMessage":	
				if (transaction.type !== 1 || transaction.subType !== 0) {
					return false;
				}
			
				var message_length  = String(converters.byteArrayToSignedInt32(byteArray, pos));
								
				pos += 4;
								
				var slice = byteArray.slice(pos, pos+message_length);
								
				transaction.message = converters.byteArrayToHexString(slice);
									
				if (transaction.message !== data.message) {
					return false;
				}
				break;
			case "assignAlias":
				if (transaction.type != 1 || transaction.subType != 1) {
					return false;
				}
				
				var alias_length  = parseInt(byteArray[pos], 10);
				
				pos++;
				
				transaction.alias = converters.byteArrayToString(byteArray, pos, alias_length);
				
				pos += alias_length;
				
				var uri_length = converters.byteArrayToSignedShort(byteArray, pos);
				
				pos+= 2;
				
				transaction.uri = converters.byteArrayToString(byteArray, pos, uri_length);
				
				if (transaction.alias !== data.alias || transaction.uri !== data.uri) {
					return false;
				}
				break;
			case "createPoll":
				if (transaction.type !== 1 || transaction.subType !== 2) {
					return false;
				}
				
				var name_length = converters.byteArrayToSignedShort(byteArray, pos);
				
				pos += 2;
				
				transaction.name = converters.byteArrayToString(byteArray, pos, name_length);
				
				pos += name_length;
				
				var description_length = converters.byteArrayToSignedShort(byteArray, pos);
				
				pos += 2;
				
				transaction.description = converters.byteArrayToString(byteArray, pos, description_length);
				
				pos += description_length;
				
				var nr_options = byteArray[pos];
											
				pos++;
	
				for (var i=0; i<nr_options; i++) {
					var option_length = converters.byteArrayToSignedShort(byteArray, pos);
					
					pos += 2;
					
					transaction["option" + i] = converters.byteArrayToString(byteArray, pos, option_length);
					
					pos += option_length;
				}
										
				transaction.minNumberOfOptions = String(byteArray[pos]);
				
				pos++;
				
				transaction.maxNumberOfOptions = String(byteArray[pos]);
				
				pos++;
				
				transaction.optionsAreBinary = String(byteArray[pos]);
																
				if (transaction.name !== data.name || transaction.description !== data.description || transaction.minNumberOfOptions !== data.minNumberOfOptions || transaction.maxNumberOfOptions !== data.maxNumberOfOptions || transaction.optionsAreBinary !== data.optionsAreBinary) {
					return false;
				}
				
				for (var i=0; i<nr_options; i++) {
					if (transaction["option" + i] !== data["option" + i]) {
						return false;
					}
				}
				
				if (("option" + i) in data) {
					return false;
				}
				
				break;
			case "castVote":	
				if (transaction.type !== 1 || transaction.subType !== 3) {
					return false;
				}
								
				transaction.poll = String(converters.byteArrayToBigInteger(byteArray, pos));
				
				pos += 8;
				
				var vote_length = byteArray[pos];
				
				pos++;
							
				transaction.votes = [];
				
				for (var i=0; i<vote_length; i++) {
					transaction.votes.push(bytesArray[pos]);
						
					pos++;
				}
								
				return false;
				break;
			case "issueAsset":	
				if (transaction.type !== 2 || transaction.subType !== 0) {
					return false;
				}
					
				var name_length = byteArray[pos];
				
				pos++;
				
				transaction.name = converters.byteArrayToString(byteArray, pos, name_length);
				
				pos += name_length;
				
				var description_length = converters.byteArrayToSignedShort(byteArray, pos); //6-7
	
				pos += 2;
				
				transaction.description = converters.byteArrayToString(byteArray, pos, description_length);
	
				pos += description_length;
										
				transaction.quantity = String(converters.byteArrayToSignedInt32(byteArray, pos)); 
				
				if (transaction.name != data.name || transaction.description != data.description || transaction.quantity != data.quantity) {
					return false;
				}
				break;
			case "transferAsset":	
				if (transaction.type !== 2 || transaction.subType !== 1) {
					return false;
				}
				
				transaction.asset  = String(converters.byteArrayToBigInteger(byteArray, pos));
				
				pos += 8;
				
				transaction.quantity = String(converters.byteArrayToSignedInt32(byteArray, pos));
				
				if (transaction.asset != data.asset || transaction.quantity != data.quantity) {
					return false;
				}
				break;
			case "placeAskOrder":
			case "placeBidOrder":
				if (transaction.type !== 2) {
					return false;
				} else if (requestType == "placeAskOrder" && transaction.subType !== 2) {
					return false;
				} else if (requestType == "placeBidOrder" && transaction.subType !== 3) {
					return false;
				}
				
	        	transaction.asset = String(converters.byteArrayToBigInteger(byteArray, pos));	
        	
	        	pos += 8;
	        	
				transaction.quantity = String(converters.byteArrayToSignedInt32(byteArray, pos));
				
				pos += 4;
				
				transaction.price = String(converters.byteArrayToBigInteger(byteArray, pos));
								
				if (transaction.asset !== data.asset || transaction.quantity !== data.quantity || transaction.price !== data.price) {
					return false;
				}
				break;
			case "cancelAskOrder":
			case "cancelBidOrder":
				if (transaction.type !== 2) {
					return false;
				} else if (requestType == "cancelAskOrder" && transaction.subType !== 4) {
					return false;
				} else if (requestType == "cancelBidOrder" && transaction.subType !== 5) {
					return false;
				}
				
				transaction.order = String(converters.byteArrayToBigInteger(byteArray, pos));
        	
	        	if (transaction.order !== data.order) {
		        	return false;
	        	}
	        	
	        	break;
	        default:
	        	//invalid requestType..
	        	return false;
		}
		
		return true;
    }
    
    NRS.broadcastTransactionBytes = function(transactionData, callback, original_response, original_data) {
	   	$.ajax({
			url: NRS.server + "/nxt?requestType=broadcastTransaction",
			crossDomain: true,
			dataType: "json",
			type: "POST",
			timeout: 20000, //20 seconds
			async: true,
			data: {"transactionBytes": transactionData}
		}).done(function(response, status, xhr) {  			
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, response);
			}

			if (callback) {
				if (response.errorCode && !response.errorDescription) {
					response.errorDescription = (response.errorMessage ? response.errorMessage : "Unknown error occured.");
					callback(response, original_data);
				} else {
					callback(original_response, original_data);
				}    
			}
		}).fail(function(xhr, textStatus, error) {
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, error, true);
			}
			
			if (callback) {
				if (error == "timeout") {
		    		error = "The request timed out. Warning: This does not mean the request did not go through. You should wait for the next block and see if your request has been processed.";
				}
		 		callback({"errorCode": -1, "errorDescription": error}, {});
		 	}
		});
    }
        
    NRS.generatePublicKey = function(secretPhrase) {
    	return nxtCrypto.getPublicKey(converters.stringToHexString(secretPhrase));
    }   
        
    NRS.generateAccountId = function(secretPhrase) {   
    	return nxtCrypto.getAccountId(secretPhrase);
    }
    
    $(".modal").on("shown.bs.modal", function() {
    	$(this).find("input[type=text]:first, input[type=password]:first").first().focus();
    	$(this).find("input[name=converted_account_id]").val("");
    });
    
    $(".modal").on("hidden.bs.modal", function(e) {
    	$(this).find(":input:not([type=hidden])").each(function(index) {
    		var default_value = $(this).data("default");
    		if (default_value) {
    			$(this).val(default_value);
    		} else {
    			$(this).val("");	
    		}		
    	});
    	$(this).find("input[name=converted_account_id]").val("");
    	$(this).find(".callout-danger, .error_message, .account_info").html("").hide();
    });
    
    $(".sidebar_context").on("contextmenu", "a", function(e) {
	 	e.preventDefault();
	 	
	 	if (!NRS.databaseSupport) {
	    	return;
    	}
    	
	 	NRS.closeContextMenu();
	 	
	 	if ($(this).hasClass("no-context")) {
		 	return;
	 	}
	 	
	 	NRS.selectedContext = $(this);
	 	
	 	NRS.selectedContext.addClass("context");
	 		 		 		 	
	 	$(document).on("click.contextmenu", NRS.closeContextMenu);
	 	
	 	var contextMenu = $(this).data("context");
	 	
	 	if (!contextMenu) {
		 	contextMenu = $(this).closest(".list-group").attr("id") + "_context";
		}
		
		var $contextMenu = $("#" + contextMenu);
		
		if ($contextMenu.length) {
			var $options = $contextMenu.find("ul.dropdown-menu a");
			
			$.each($options, function() {
				var requiredClass = $(this).data("class");
				
				if (!requiredClass) {
					$(this).show();
				} else if (NRS.selectedContext.hasClass(requiredClass)) {
					$(this).show();
				} else {
					$(this).hide();
				}
			});
			
			$contextMenu.css({display: "block", left: e.pageX, top: e.pageY});
		}
	 	
	 	return false; 
    });
    
    NRS.closeContextMenu = function(e) {
    	if (!e || e.which == 3) {
	    	return;
    	}

        $(".context_menu").hide();
        
        if (NRS.selectedContext) {
	        NRS.selectedContext.removeClass("context");
			//NRS.selectedContext = null;
        }
        
		$(document).off("click.contextmenu");
    }
        
    NRS.dataLoadFinished = function($table, fadeIn) {
    	var $parent = $table.parent();
    	
    	if (fadeIn) {
    		$parent.hide();
    	}
  
      	$parent.removeClass("data-loading");
    	
	    var extra = $parent.data("extra");
    	
    	if ($table.find("tbody tr").length > 0) {
    		$parent.removeClass("data-empty");
    		if ($parent.data("no-padding")) {
    			$parent.parent().addClass("no-padding");
    		}
    		
    		if (extra) {
    			$(extra).show();
    		}
    	} else {
    		$parent.addClass("data-empty");
    		if ($parent.data("no-padding")) {
    			$parent.parent().removeClass("no-padding");
    		}
    		if (extra) {
    			$(extra).hide();
    		}
    	}
    	
    	if (fadeIn) {
    		$parent.fadeIn();
    	}
    }
    
    NRS.getFormData = function($form) {
    	var serialized = $form.serializeArray();
        var data = {};
        
        for (var s in serialized){
            data[serialized[s]['name']] = serialized[s]['value']
        }
        
        return data;
    }
   
    NRS.formatVolume = function(volume) {    	
    	var sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    	if (volume == 0) return '0 B';
   		var i = parseInt(Math.floor(Math.log(volume) / Math.log(1024)));
   		
   		volume = Math.round(volume / Math.pow(1024, i), 2);
   		var size = sizes[i];
        	
        var digits=[], formattedVolume = "", i;
        do {
            digits[digits.length] = volume % 10;
            volume = Math.floor(volume / 10);
        } while (volume > 0);
        for (i = 0; i < digits.length; i++) {
            if (i > 0 && i % 3 == 0) {
                formattedVolume = "'" + formattedVolume;
            }
            formattedVolume = digits[i] + formattedVolume;
        }
        return formattedVolume + " " + size;
    }
    
    NRS.formatWeight = function(weight) {
        var digits=[], formattedWeight = "", i;
        do {
            digits[digits.length] = weight % 10;
            weight = Math.floor(weight / 10);
        } while (weight > 0);
        for (i = 0; i < digits.length; i++) {
            if (i > 0 && i % 3 == 0) {
                formattedWeight = "'" + formattedWeight;
            }
            formattedWeight = digits[i] + formattedWeight;
        }
        return formattedWeight.escapeHTML();
    }
    
    NRS.formatAmount = function(amount, round) {
    	if (round) {
    		amount = (Math.round(amount*100)/100);
    	}
    	
    	amount = "" + amount;
    	
    	if (amount.indexOf(".") !== -1) {
    		var afterComma = amount.substr(amount.indexOf("."));
    		amount = amount.replace(afterComma, "");
    	} else {
    		var afterComma = "";
    	}
    
        var digits=[], formattedAmount = "", i;
        do {
            digits[digits.length] = amount % 10;
            amount = Math.floor(amount / 10);
        } while (amount > 0);
        
        for (i = 0; i < digits.length; i++) {
            if (i > 0 && i % 3 == 0) {
                formattedAmount = "'" + formattedAmount;
            }
            formattedAmount = digits[i] + formattedAmount;
        }
        return (formattedAmount + afterComma).escapeHTML();
    }
				
    NRS.formatTimestamp = function(timestamp, date_only) {
        var date = new Date(Date.UTC(2013, 10, 24, 12, 0, 0, 0) + timestamp * 1000);
    
        if (!isNaN(date) && typeof(date.getFullYear) == 'function') {
            var d = date.getDate();
            var dd = d < 10 ? '0' + d : d;
            var M = date.getMonth() + 1;
            var MM = M < 10 ? '0' + M : M;
            var yyyy = date.getFullYear();
            var yy = new String(yyyy).substring(2);
    
            var format = LOCALE_DATE_FORMAT;
    
            var res = format
                .replace(/dd/g, dd)
                .replace(/d/g, d)
                .replace(/MM/g, MM)
                .replace(/M/g, M)
                .replace(/yyyy/g, yyyy)
                .replace(/yy/g, yy);
            
            if (!date_only) {
	            var hours = date.getHours();
	            var minutes = date.getMinutes();
	            var seconds = date.getSeconds();
	            
	            if (hours < 10) {
	            	hours = "0" + hours;
	            }
	            if (minutes < 10) {
	            	minutes = "0" + minutes;
	            }
	            if (seconds < 10) {
	            	seconds = "0" + seconds;
	            }
	            res += " " + hours + ":" + minutes + ":" + seconds;
            }
             
            return res;
        } else {
        	return date.toLocaleString();
        }
    }   
    		
   	NRS.formatTime = function(timestamp) {
	    var date = new Date(Date.UTC(2013, 10, 24, 12, 0, 0, 0) + timestamp * 1000);
	
	    if (!isNaN(date) && typeof(date.getFullYear) == 'function') {
	    	var res = "";
	    	
		    var hours = date.getHours();
		    var minutes = date.getMinutes();
		    var seconds = date.getSeconds();
		    
		    if (hours < 10) {
		    	hours = "0" + hours;
		    }
		    if (minutes < 10) {
		    	minutes = "0" + minutes;
		    }
		    if (seconds < 10) {
		    	seconds = "0" + seconds;
		    }
		    res += " " + hours + ":" + minutes + ":" + seconds;
	    
	    	return res;
	    } else {
        	return date.toLocaleString();
	    }
   	}
})(jQuery, window.NRS = {});
    
$(document).ready(function() {
    NRS.init();
});

window.addEventListener("message", receiveMessage, false);
function receiveMessage(event)
{
	if (event.origin != "file://") {
		return;
	}
				
	//parent.postMessage("from iframe", "file://");
}