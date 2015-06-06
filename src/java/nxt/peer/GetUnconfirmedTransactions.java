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

package nxt.peer;

import nxt.Nxt;
import nxt.Transaction;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.Collections;
import java.util.List;

final class GetUnconfirmedTransactions extends PeerServlet.PeerRequestHandler {

    static final GetUnconfirmedTransactions instance = new GetUnconfirmedTransactions();

    private GetUnconfirmedTransactions() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();
        List<String> exclude = (List<String>)request.get("exclude");
        boolean supportsExclude = exclude != null;

        JSONArray transactionsData = new JSONArray();
        try (DbIterator<? extends Transaction> transactions = Nxt.getTransactionProcessor().getAllUnconfirmedTransactions()) {
            while (transactions.hasNext()) {
                if (supportsExclude && transactionsData.size() >= 100) { //TODO: always limit to 100 after VS block
                    break;
                }
                Transaction transaction = transactions.next();
                if (!supportsExclude || Collections.binarySearch(exclude, transaction.getStringId()) < 0) {
                    transactionsData.add(transaction.getJSONObject());
                }
            }
        }
        response.put("unconfirmedTransactions", transactionsData);


        return response;
    }

    @Override
    boolean rejectWhileDownloading() {
        return true;
    }

}
