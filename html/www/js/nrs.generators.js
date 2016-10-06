/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 * Copyright © 2016 Jelurida IP B.V.                                          *
 *                                                                            *
 * See the LICENSE.txt file at the top-level directory of this distribution   *
 * for licensing information.                                                 *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,*
 * no part of the Nxt software, including this file, may be copied, modified, *
 * propagated, or distributed except according to the terms contained in the  *
 * LICENSE.txt file.                                                          *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS) {

	NRS.pages.generators = function() {
        NRS.renderGenerators();
	};

    NRS.renderGenerators = function() {
        NRS.hasMorePages = false;
        var view = NRS.simpleview.get('generators_page', {
            errorMessage: null,
            isLoading: true,
            isEmpty: false,
            generators: [],
            timeFormatted: ""
        });
        var params = {
            "limit": 10
        };
        NRS.sendRequest("getNextBlockGenerators+", params,
            function(response) {
                view.generators.length = 0;
                response.generators.forEach(
                    function(generatorsJson) {
                        view.generators.push(NRS.jsondata.generators(generatorsJson));
                    }
                );
                view.render({
                    isLoading: false,
                    isEmpty: view.generators.length == 0,
                    timeFormatted: NRS.formatTimestamp(response.timestamp)
                });
                NRS.pageLoaded();
            }
        );
    };

    NRS.jsondata.generators = function (response) {
        return {
            accountFormatted: NRS.getAccountLink(response, "account"),
            balanceFormatted: NRS.formatAmount(response.effectiveBalanceNXT),
            deadlineFormatted: NRS.escapeRespStr(response.deadline)
        };
    };

	NRS.incoming.generators = function() {
		NRS.loadPage("generators");
	};

	return NRS;
}(NRS || {}, jQuery));