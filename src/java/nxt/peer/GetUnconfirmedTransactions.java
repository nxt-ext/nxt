package nxt.peer;

import nxt.Blockchain;
import nxt.Transaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetUnconfirmedTransactions extends HttpJSONRequestHandler {

    static final GetUnconfirmedTransactions instance = new GetUnconfirmedTransactions();

    private GetUnconfirmedTransactions() {}


    @Override
    JSONStreamAware processJSONRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        JSONArray transactionsData = new JSONArray();
        for (Transaction transaction : Blockchain.getAllUnconfirmedTransactions()) {

            transactionsData.add(transaction.getJSONObject());

        }
        response.put("unconfirmedTransactions", transactionsData);


        return response;
    }

}
