/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.newlyCreatedAccount = false;

	NRS.allowLoginViaEnter = function() {
		$("#login_account_other").keypress(function(e) {
			if (e.which == '13') {
				e.preventDefault();
				var account = $("#login_account_other").val();
				NRS.login(false,account);
			}
		});
		$("#login_password").keypress(function(e) {
			if (e.which == '13') {
				e.preventDefault();
				var password = $("#login_password").val();
				NRS.login(true,password);
			}
		});
	}

	NRS.showLoginOrWelcomeScreen = function() {
		if (NRS.hasLocalStorage && localStorage.getItem("logged_in")) {
			NRS.showLoginScreen();
		} else {
			NRS.showWelcomeScreen();
		}
	}

	NRS.showLoginScreen = function() {
		$("#account_phrase_custom_panel, #account_phrase_generator_panel, #welcome_panel, #custom_passphrase_link").hide();
		$("#account_phrase_custom_panel :input:not(:button):not([type=submit])").val("");
		$("#account_phrase_generator_panel :input:not(:button):not([type=submit])").val("");
        $("#login_account_other").mask("NXT-****-****-****-*****");
        
		$("#login_panel").show();
		setTimeout(function() {
			$("#login_password").focus()
		}, 10);
	}

	NRS.showWelcomeScreen = function() {
		$("#login_panel, account_phrase_custom_panel, #account_phrase_generator_panel, #account_phrase_custom_panel, #welcome_panel, #custom_passphrase_link").hide();
		$("#welcome_panel").show();
	}

	NRS.registerUserDefinedAccount = function() {
		$("#account_phrase_generator_panel, #login_panel, #welcome_panel, #custom_passphrase_link").hide();
		$("#account_phrase_custom_panel :input:not(:button):not([type=submit])").val("");
		$("#account_phrase_generator_panel :input:not(:button):not([type=submit])").val("");
		$("#account_phrase_custom_panel").show();
		$("#registration_password").focus();
	}

	NRS.registerAccount = function() {
		$("#login_panel, #welcome_panel").hide();
		$("#account_phrase_generator_panel").show();
		$("#account_phrase_generator_panel step_3 .callout").hide();

		var $loading = $("#account_phrase_generator_loading");
		var $loaded = $("#account_phrase_generator_loaded");

		if (window.crypto || window.msCrypto) {
			$loading.find("span.loading_text").html($.t("generating_passphrase_wait"));
		}

		$loading.show();
		$loaded.hide();

		if (typeof PassPhraseGenerator == "undefined") {
			$.when(
				$.getScript("js/crypto/3rdparty/seedrandom.js"),
				$.getScript("js/crypto/passphrasegenerator.js")
			).done(function() {
				$loading.hide();
				$loaded.show();

				PassPhraseGenerator.generatePassPhrase("#account_phrase_generator_panel");
			}).fail(function(jqxhr, settings, exception) {
				alert($.t("error_word_list"));
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
			NRS.newlyCreatedAccount = true;
			NRS.login(true,password);
			PassPhraseGenerator.reset();
			$("#account_phrase_generator_panel textarea").val("");
			$("#account_phrase_generator_panel .step_3 .callout").hide();
		}
	}

	$("#account_phrase_custom_panel form").submit(function(event) {
		event.preventDefault()

		var password = $("#registration_password").val();
		var repeat = $("#registration_password_repeat").val();

		var error = "";

		if (password.length < 35) {
			error = $.t("error_passphrase_length");
		} else if (password.length < 50 && (!password.match(/[A-Z]/) || !password.match(/[0-9]/))) {
			error = $.t("error_passphrase_strength");
		} else if (password != repeat) {
			error = $.t("error_passphrase_match");
		}

		if (error) {
			$("#account_phrase_custom_panel .callout").first().removeClass("callout-info").addClass("callout-danger").html(error);
		} else {
			$("#registration_password, #registration_password_repeat").val("");
			NRS.login(true,password);
		}
	});
	
	NRS.listAccounts = function() {
		$('#login_account').empty();
		if (NRS.getCookie("savedNxtAccounts") && NRS.getCookie("savedNxtAccounts")!=""){
			$('#login_account_container').show();
			$('#login_account_container_other').hide();
			var accounts = NRS.getCookie("savedNxtAccounts").split(";");
			$.each(accounts, function(index, account) {
				if (account != ''){
					$('#login_account')
					.append($("<li></li>")
						.append($("<a></a>")
							.attr("href","#")
							.attr("style","display: inline-block;width: 360px;")
							.attr("onClick","NRS.login(false,'"+account+"')")
							.text(account))
						.append($('<button aria-hidden="true" data-dismiss="modal" class="close" type="button">×</button>')
							.attr("onClick","NRS.removeAccount('"+account+"')")
							.attr("style","margin-right:5px"))
					);
				}
			});
			$('#login_account')
			.append($("<li></li>")
				.append($("<a></a>")
					.attr("href","#")
					.attr("style","display: inline-block;width: 380px;")
					.attr("onClick","$('#login_account_container').hide();$('#login_account_container_other').show();")
					.text("Other"))
			);
		}
		else{
			$('#login_account_container').hide();
			$('#login_account_container_other').show();
		}
	}
	
	NRS.switchAccount = function(account) {
		NRS.setDecryptionPassword("");
		NRS.setPassword("");
		var url = window.location.pathname;    
		url += '?account='+account;
		window.location.href = url;
	}
	
	$("#loginButtons").on('click',function(e) {
		e.preventDefault();
		if ($(this).data( "login-type" ) == "password") {
            NRS.listAccounts();
			$('#login_password').parent().hide();
			$('#remember_password_container').hide();
			$(this).html('<input type="hidden" name="loginType" id="accountLogin" value="account" autocomplete="off" /><i class="fa fa-male"></i>');
			$(this).data( "login-type","account");
        }
        else {
            $('#login_account_container').hide();
			$('#login_account_container_other').hide();
			$('#login_password').parent().show();
			$('#remember_password_container').show();
			$(this).html('<input type="hidden" name="loginType" id="accountLogin" value="passwordLogin" autocomplete="off" /><i class="fa fa-key"></i>');
			$(this).data( "login-type","password");
        }
	});
	
	NRS.removeAccount = function(account) {
		var accounts = NRS.getCookie("savedNxtAccounts").replace(account+';','');
		if (accounts == '')
			NRS.deleteCookie('savedNxtAccounts');
		else 
			NRS.setCookie("savedNxtAccounts",accounts,30);
		NRS.listAccounts();
	}

	NRS.login = function(passLogin, password, callback) {
		if (passLogin){
			if (!password.length) {
				$.growl($.t("error_passphrase_required_login"), {
					"type": "danger",
					"offset": 10
				});
				return;
			} else if (!NRS.isTestNet && password.length < 12 && $("#login_check_password_length").val() == 1) {
				$("#login_check_password_length").val(0);
				$("#login_error .callout").html($.t("error_passphrase_login_length"));
				$("#login_error").show();
				return;
			}

			$("#login_password, #registration_password, #registration_password_repeat").val("");
			$("#login_check_password_length").val(1);
		}

		NRS.sendRequest("getBlockchainStatus", function(response) {
			if (response.errorCode) {
				$.growl($.t("error_server_connect"), {
					"type": "danger",
					"offset": 10
				});

				return;
			}
			
			NRS.state = response;
			if (passLogin) {
				var accountRequest = "getAccountId";
				var requestVariable = {secretPhrase: password};
			}
			else {
				var accountRequest = "getAccount";
				var requestVariable = {account: password};
			}

			//this is done locally..
			NRS.sendRequest(accountRequest, requestVariable, function(response) {
				if (!response.errorCode) {
					NRS.account = String(response.account).escapeHTML();
					NRS.accountRS = String(response.accountRS).escapeHTML();
					if (passLogin) {
                        NRS.publicKey = NRS.getPublicKey(converters.stringToHexString(password));
                    } else {
                        NRS.publicKey = String(response.publicKey).escapeHTML();
                    }
				}
				if (!passLogin && response.errorCode == 5) {
					NRS.account = String(response.account).escapeHTML();
					NRS.accountRS = String(response.accountRS).escapeHTML();
				}
				if (!NRS.account) {
					$.growl($.t("error_find_account_id"), {
						"type": "danger",
						"offset": 10
					});
					return;
				} else if (!NRS.accountRS) {
					$.growl($.t("error_generate_account_id"), {
						"type": "danger",
						"offset": 10
					});
					return;
				}

				NRS.sendRequest("getAccountPublicKey", {
					"account": NRS.account
				}, function(response) {
					if (response && response.publicKey && response.publicKey != NRS.generatePublicKey(password) && passLogin) {
						$.growl($.t("error_account_taken"), {
							"type": "danger",
							"offset": 10
						});
						return;
					}

					if ($("#remember_password").is(":checked") && passLogin) {
						NRS.rememberPassword = true;
						$("#remember_password").prop("checked", false);
						NRS.setPassword(password);
						$(".secret_phrase, .show_secret_phrase").hide();
						$(".hide_secret_phrase").show();
					}
					if ($("#disable_all_plugins").length == 1 && !($("#disable_all_plugins").is(":checked"))) {
						NRS.disableAllPlugins = false;
					} else {
						NRS.disableAllPlugins = true;
					}

					$("#account_id").html(String(NRS.accountRS).escapeHTML()).css("font-size", "12px");

					var passwordNotice = "";

					if (password.length < 35 && passLogin) {
						passwordNotice = $.t("error_passphrase_length_secure");
					} else if (passLogin && password.length < 50 && (!password.match(/[A-Z]/) || !password.match(/[0-9]/))) {
						passwordNotice = $.t("error_passphrase_strength_secure");
					}

					if (passwordNotice) {
						$.growl("<strong>" + $.t("warning") + "</strong>: " + passwordNotice, {
							"type": "danger"
						});
					}

					if (NRS.state) {
						NRS.checkBlockHeight();
					}

					NRS.getAccountInfo(true, function() {
						if (NRS.accountInfo.currentLeasingHeightFrom) {
							NRS.isLeased = (NRS.lastBlockHeight >= NRS.accountInfo.currentLeasingHeightFrom && NRS.lastBlockHeight <= NRS.accountInfo.currentLeasingHeightTo);
						} else {
							NRS.isLeased = false;
						}

						//forging requires password to be sent to the server, so we don't do it automatically if not localhost
						if (!NRS.accountInfo.publicKey || NRS.accountInfo.effectiveBalanceNXT == 0 || !NRS.isLocalHost || NRS.downloadingBlockchain || NRS.isLeased) {
							$("#forging_indicator").removeClass("forging");
							$("#forging_indicator span").html($.t("not_forging")).attr("data-i18n", "not_forging");
							$("#forging_indicator").show();
							NRS.isForging = false;
						} else if (NRS.isLocalHost && passLogin) {
							NRS.sendRequest("startForging", {
								"secretPhrase": password
							}, function(response) {
								if ("deadline" in response) {
									$("#forging_indicator").addClass("forging");
									$("#forging_indicator span").html($.t("forging")).attr("data-i18n", "forging");
									NRS.isForging = true;
								} else {
									$("#forging_indicator").removeClass("forging");
									$("#forging_indicator span").html($.t("not_forging")).attr("data-i18n", "not_forging");
									NRS.isForging = false;
								}
								$("#forging_indicator").show();
							});
						}
					});

					//NRS.getAccountAliases();

					NRS.unlock();

					if (NRS.isOutdated) {
						$.growl($.t("nrs_update_available"), {
							"type": "danger"
						});
					}

					if (!NRS.downloadingBlockchain) {
						NRS.checkIfOnAFork();
					}
					if(navigator.userAgent.indexOf('Safari') != -1 && navigator.userAgent.indexOf('Chrome') == -1) {
						// Don't use account based DB in Safari due to a buggy indexedDB implementation (2015-02-24)
						NRS.createDatabase("NRS_USER_DB");
						$.growl($.t("nrs_safari_no_account_based_db"), {
							"type": "danger"
						});
					} else {
						NRS.createDatabase("NRS_USER_DB_" + String(NRS.account));
					}

					NRS.setupClipboardFunctionality();

					if (callback) {
						callback();
					}
					
					if (passLogin) {
						NRS.checkLocationHash(password);
						$(window).on("hashchange", NRS.checkLocationHash);
					}

					$.each(NRS.pages, function(key, value) {
						if(key in NRS.setup) {
							NRS.setup[key]();
						}
					});
					
					NRS.loadPlugins();
					$(".sidebar .treeview").tree();
					$('#dashboard_link a').addClass("ignore").click();

					if ($("#remember_account").is(":checked")) {
						var accountExists = 0;
						if (NRS.getCookie("savedNxtAccounts")){
							var accounts = NRS.getCookie("savedNxtAccounts").split(";");
							$.each(accounts, function(index, account) {
								if (account == NRS.accountRS)
									accountExists = 1;
							});
						}
						if (!accountExists){
							if (NRS.getCookie("savedNxtAccounts") && NRS.getCookie("savedNxtAccounts")!=""){
								var accounts=NRS.getCookie("savedNxtAccounts") + NRS.accountRS + ";";
								NRS.setCookie("savedNxtAccounts",accounts,30);
							}
							else
								NRS.setCookie("savedNxtAccounts",NRS.accountRS + ";",30);
						}
					}

					$("[data-i18n]").i18n();
					
					/* Add accounts to dropdown for quick switching */
					$("#account_id_dropdown .dropdown-menu .switchAccount").remove();
					if (NRS.getCookie("savedNxtAccounts") && NRS.getCookie("savedNxtAccounts")!=""){
						$("#account_id_dropdown .dropdown-menu").append("<li class='switchAccount' style='padding-left:2px;'><b>Switch Account to</b></li>")
						var accounts = NRS.getCookie("savedNxtAccounts").split(";");
						$.each(accounts, function(index, account) {
							if (account != ''){
								$('#account_id_dropdown .dropdown-menu')
								.append($("<li class='switchAccount'></li>")
									.append($("<a></a>")
										.attr("href","#")
										.attr("style","font-size: 85%;")
										.attr("onClick","NRS.switchAccount('"+account+"')")
										.text(account))
								);
							}
						});
					}

					NRS.getInitialTransactions();
					NRS.updateApprovalRequests();
				});
			});
		});
	}

	$("#logout_button_container").on("show.bs.dropdown", function(e) {
		
		if (!NRS.isForging) {
			//e.preventDefault();
			$(this).find("[data-i18n='logout_stop_forging']").hide();
		}
	});

	NRS.initPluginWarning = function() {
		if (NRS.activePlugins) {
			var html = "";
			html += "<div style='font-size:13px;'>";
			html += "<div style='background-color:#e6e6e6;padding:12px;'>";
			html += "<span data-i18n='following_plugins_detected'>";
			html += "The following active plugins have been detected:</span>";
			html += "</div>";
			html += "<ul class='list-unstyled' style='padding:11px;border:1px solid #e0e0e0;margin-top:8px;'>";
			$.each(NRS.plugins, function(pluginId, pluginDict) {
				if (pluginDict["launch_status"] == NRS.constants.PL_PAUSED) {
					html += "<li style='font-weight:bold;'>" + pluginDict["manifest"]["name"] + "</li>";
				}
			});
			html += "</ul>";
			html += "</div>";

			$('#lockscreen_active_plugins_overview').popover({
				"html": true,
				"content": html,
				"trigger": "hover"
			});

			html = "";
			html += "<div style='font-size:13px;padding:5px;'>";
			html += "<p data-i18n='plugin_security_notice_full_access'>";
			html += "Plugins are not sandboxed or restricted in any way and have full accesss to your client system including your Nxt passphrase.";
			html += "</p>";
			html += "<p data-i18n='plugin_security_notice_trusted_sources'>";
			html += "Make sure to only run plugins downloaded from trusted sources, otherwise ";
			html += "you can loose your NXT! In doubt don't log into the client with an account ";
			html += "used to store larger amounts of NXT now or in the future while plugins ";
			html += "are active."
			html += "</p>";
			html += "<p data-i18n='plugin_security_notice_remember_passphrase'>";
			html += "Logging into the client without remembering the passphrase won't make ";
			html += "the process more secure.";
			html += "</p>";
			html += "</div>";

			$('#lockscreen_active_plugins_security').popover({
				"html": true,
				"content": html,
				"trigger": "hover"
			});

			$("#lockscreen_active_plugins_warning").show();
		} else {
			$("#lockscreen_active_plugins_warning").hide();
		}
	}

	NRS.showLockscreen = function() {
		NRS.listAccounts();
		if (NRS.hasLocalStorage && localStorage.getItem("logged_in")) {
			NRS.showLoginScreen();
		} else {
			NRS.showWelcomeScreen();
		}

		$("#center").show();
	}

	NRS.unlock = function() {
		if (NRS.hasLocalStorage && !localStorage.getItem("logged_in")) {
			localStorage.setItem("logged_in", true);
		}

		var userStyles = ["header", "sidebar", "boxes"];

		for (var i = 0; i < userStyles.length; i++) {
			var color = NRS.settings[userStyles[i] + "_color"];
			if (color) {
				NRS.updateStyle(userStyles[i], color);
			}
		}

		var contentHeaderHeight = $(".content-header").height();
		var navBarHeight = $("nav.navbar").height();

		//	$(".content-splitter-right").css("bottom", (contentHeaderHeight + navBarHeight + 10) + "px");

		$("#lockscreen").hide();
		$("body, html").removeClass("lockscreen");

		$("#login_error").html("").hide();

		$(document.documentElement).scrollTop(0);
	}

	/*$("#logout_button").click(function(e) {
		if (!NRS.isForging) {
			e.preventDefault();
			NRS.logout();
		}
	});*/

	NRS.logout = function(stopForging) {
		if (stopForging && NRS.isForging) {
			$("#stop_forging_modal .show_logout").show();
			$("#stop_forging_modal").modal("show");
		} else {
			NRS.setDecryptionPassword("");
			NRS.setPassword("");
			//window.location.reload();
			window.location.href = window.location.pathname;    
		}
	}

	$("#logout_clear_user_data_confirm_btn").click(function(e) {
		e.preventDefault();
		if (NRS.database) {
			indexedDB.deleteDatabase(NRS.database.name);
		}
		if (NRS.legacyDatabase) {
			indexedDB.deleteDatabase(NRS.legacyDatabase.name);
		}
		if (NRS.hasLocalStorage) {
			localStorage.removeItem("logged_in");
			localStorage.removeItem("settings")
		}
		var cookies = document.cookie.split(";");
		for (var i = 0; i < cookies.length; i++) {
			NRS.deleteCookie(cookies[i].split("=")[0]);
		}

		NRS.logout();
	})

	NRS.setPassword = function(password) {
		NRS.setEncryptionPassword(password);
		NRS.setServerPassword(password);
	}
	return NRS;
}(NRS || {}, jQuery));