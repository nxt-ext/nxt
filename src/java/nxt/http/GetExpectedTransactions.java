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

import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.util.Convert;
import nxt.util.Filter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

public final class GetExpectedTransactions extends APIServlet.APIRequestHandler {

    static final GetExpectedTransactions instance = new GetExpectedTransactions();

    private GetExpectedTransactions() {
        super(new APITag[] {APITag.TRANSACTIONS}, "account", "account", "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Set<Long> accountIds = Convert.toSet(ParameterParser.getAccountIds(req, false));
        Filter<Transaction> filter = accountIds.isEmpty() ? transaction -> true :
                transaction -> accountIds.contains(transaction.getSenderId()) || accountIds.contains(transaction.getRecipientId());

        List<? extends Transaction> transactions = Nxt.getBlockchain().getExpectedTransactions(filter);

        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        transactions.forEach(transaction -> jsonArray.add(JSONData.unconfirmedTransaction(transaction)));
        response.put("expectedTransactions", jsonArray);

        return response;
    }

}
