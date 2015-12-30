/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
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

import nxt.CurrencyTransfer;
import nxt.NxtException;
import nxt.db.DbIterator;
import nxt.db.DbUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetCurrencyTransfers extends APIServlet.APIRequestHandler {

    static final GetCurrencyTransfers instance = new GetCurrencyTransfers();

    private GetCurrencyTransfers() {
        super(new APITag[] {APITag.MS}, "currency", "account", "firstIndex", "lastIndex", "timestamp", "includeCurrencyInfo");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long currencyId = ParameterParser.getUnsignedLong(req, "currency", false);
        long accountId = ParameterParser.getAccountId(req, false);
        if (currencyId == 0 && accountId == 0) {
            return JSONResponses.MISSING_CURRENCY_ACCOUNT;
        }
        boolean includeCurrencyInfo = "true".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));
        int timestamp = ParameterParser.getTimestamp(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray transfersData = new JSONArray();
        DbIterator<CurrencyTransfer> transfers = null;
        try {
            if (accountId == 0) {
                transfers = CurrencyTransfer.getCurrencyTransfers(currencyId, firstIndex, lastIndex);
            } else if (currencyId == 0) {
                transfers = CurrencyTransfer.getAccountCurrencyTransfers(accountId, firstIndex, lastIndex);
            } else {
                transfers = CurrencyTransfer.getAccountCurrencyTransfers(accountId, currencyId, firstIndex, lastIndex);
            }
            while (transfers.hasNext()) {
                CurrencyTransfer currencyTransfer = transfers.next();
                if (currencyTransfer.getTimestamp() < timestamp) {
                    break;
                }
                transfersData.add(JSONData.currencyTransfer(currencyTransfer, includeCurrencyInfo));
            }
        } finally {
            DbUtils.close(transfers);
        }
        response.put("transfers", transfersData);

        return response;
    }

    @Override
    boolean startDbTransaction() {
        return true;
    }

}
