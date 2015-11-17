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
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $) {
    var tokenModal = $("#token_modal");
    tokenModal.on("show.bs.modal", function() {
		$("#generate_token_output, #decode_token_output").html("").hide();

		$("#token_modal_generate_token").show();
		$("#generate_token_button").show();
		$("#validate_token_button").hide();
	});

	NRS.forms.decodeTokenComplete = function(response) {
		$("#token_modal").find(".error_message").hide();

		if (response.valid) {
			$("#decode_token_output").html($.t("success_valid_token", {
				"account_link": NRS.getAccountLink(response, "account"),
				"timestamp": NRS.formatTimestamp(response.timestamp)
			})).addClass("callout-info").removeClass("callout-danger").show();
		} else {
			$("#decode_token_output").html($.t("error_invalid_token", {
				"account_link": NRS.getAccountLink(response, "account"),
				"timestamp": NRS.formatTimestamp(response.timestamp)
			})).addClass("callout-danger").removeClass("callout-info").show();
		}
	};

	NRS.forms.decodeTokenError = function() {
		$("#decode_token_output").hide();
	};

    tokenModal.find("ul.nav li").click(function(e) {
		e.preventDefault();
		var tab = $(this).data("tab");
		$(this).siblings().removeClass("active");
		$(this).addClass("active");
		$(".token_modal_content").hide();
		var content = $("#token_modal_" + tab);
		if (tab == "generate_token") {
			$("#generate_token_button").show();
			$("#validate_token_button").hide();
		} else {
            $("#generate_token_button").hide();
            $("#validate_token_button").show();
		}

		$("#token_modal").find(".error_message").hide();
		content.show();
	});

	tokenModal.on("hidden.bs.modal", function() {
		$(this).find(".token_modal_content").hide();
		$(this).find("ul.nav li.active").removeClass("active");
		$("#generate_token_nav").addClass("active");
	});

    $("#generate_token_button").click(function (e) {
        var data = $.trim($("#generate_token_data").val());
        var tokenOutput = $("#generate_token_output");
        if (!data || data == "") {
            tokenOutput.html($.t("data_required_field"));
            tokenOutput.addClass("callout-danger").removeClass("callout-info").show();
            return;
        }
        var secretPhrase = $.trim($("#generate_token_password").val());
        var publicKey = NRS.getPublicKey(converters.stringToHexString(secretPhrase));
        if (publicKey != NRS.publicKey) {
            tokenOutput.html($.t("error_incorrect_passphrase"));
            tokenOutput.addClass("callout-danger").removeClass("callout-info").show();
            return;
        }
        var token = NRS.generateToken(data, secretPhrase);
        tokenOutput.html($.t("generated_token_is") + "<br /><br /><textarea readonly style='width:100%' rows='3'>" + String(token).escapeHTML() + "</textarea>");
        tokenOutput.addClass("callout-info").removeClass("callout-danger").show();
        e.preventDefault();
    });

	return NRS;
}(NRS || {}, jQuery));