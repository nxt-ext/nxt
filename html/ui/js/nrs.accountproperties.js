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
var NRS = (function(NRS, $) {
	var INCOMING = "incoming";

	NRS.pages.account_properties = function() {
        NRS.renderAccountProperties($("#account_properties_page_type").find(".active").data("type"));
	};

    NRS.renderAccountProperties = function(type) {
        NRS.hasMorePages = false;
        var view = NRS.simpleview.get('account_properties_section', {
            errorMessage: null,
            isLoading: true,
            isEmpty: false,
            properties: []
        });
        var params = {
            "firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
            "lastIndex": NRS.pageNumber * NRS.itemsPerPage
        };
        if (type == INCOMING) {
            params.account = NRS.account;
        } else {
            params.setter = NRS.account;
        }
        NRS.sendRequest("getAccountProperties+", params,
            function(response) {
                if (response.properties.length > NRS.itemsPerPage) {
                    NRS.hasMorePages = true;
                    response.properties.pop();
                }
                view.properties.length = 0;
                response.properties.forEach(
                    function (propertiesJson) {
                        view.properties.push( NRS.jsondata.properties(propertiesJson, type) );
                    }
                );
                view.render({
                    isLoading: false,
                    isEmpty: view.properties.length == 0,
                    header: type == INCOMING ? $.t("sender") : $.t("recipient")
                });
                NRS.pageLoaded();
            }
        );
    };

    NRS.jsondata.properties = function (response, type) {
        return {
            accountFormatted: type == INCOMING ? NRS.getAccountLink(response, "setter") : NRS.getAccountLink(response, "account"),
            property: String(response.property).escapeHTML(),
            value: String(response.value).escapeHTML()
        };
    };

	NRS.incoming.account_properties = function() {
		NRS.loadPage("account_properties");
	};

    $("#account_properties_page_type").find(".btn").click(function (e) {
        e.preventDefault();
        var propertiesTable = $("#account_properties_table");
        propertiesTable.find("tbody").empty();
        propertiesTable.parent().addClass("data-loading").removeClass("data-empty");
        NRS.renderAccountProperties($(this).data("type"));
    });

    NRS.forms.setAccountProperty = function($modal) {
        var data = NRS.getFormData($modal.find("form:first"));
        if (data.recipient == NRS.accountRS) {
            data.recipient = "";
        }
        return {
            "data": data
        }
    };

	return NRS;
}(NRS || {}, jQuery));