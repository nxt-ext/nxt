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

}
