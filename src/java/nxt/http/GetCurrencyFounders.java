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

package nxt.http;

import nxt.CurrencyFounder;
import nxt.NxtException;
import nxt.db.DbIterator;
import nxt.db.DbUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetCurrencyFounders extends APIServlet.APIRequestHandler {

    static final GetCurrencyFounders instance = new GetCurrencyFounders();

    private GetCurrencyFounders() {
        super(new APITag[] {APITag.MS}, "currency", "account", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        long currencyId = ParameterParser.getUnsignedLong(req, "currency", false);
        long accountId = ParameterParser.getAccountId(req, false);
        if (currencyId == 0 && accountId == 0) {
            return JSONResponses.MISSING_CURRENCY_ACCOUNT;
        }
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray foundersJSONArray = new JSONArray();
        response.put("founders", foundersJSONArray);

        if (currencyId != 0 && accountId != 0) {
            CurrencyFounder currencyFounder = CurrencyFounder.getFounder(currencyId, accountId);
            if (currencyFounder != null) {
                foundersJSONArray.add(JSONData.currencyFounder(currencyFounder));
            }
            return response;
        }

        DbIterator<CurrencyFounder> founders = null;
        try {
            if (accountId == 0) {
                founders = CurrencyFounder.getCurrencyFounders(currencyId, firstIndex, lastIndex);
            } else if (currencyId == 0) {
                founders = CurrencyFounder.getFounderCurrencies(accountId, firstIndex, lastIndex);
            }
            for (CurrencyFounder founder : founders) {
                foundersJSONArray.add(JSONData.currencyFounder(founder));
            }
        } finally {
            DbUtils.close(founders);
        }
        return response;
    }
}
