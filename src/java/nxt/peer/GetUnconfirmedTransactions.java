package nxt.peer;

import nxt.Nxt;
import nxt.Transaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

final class GetUnconfirmedTransactions extends HttpJSONRequestHandler {

    static final GetUnconfirmedTransactions instance = new GetUnconfirmedTransactions();

    private GetUnconfirmedTransactions() {}


    @Override
    public JSONObject processJSONRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        JSONArray transactionsData = new JSONArray();
        for (Transaction transaction : Nxt.unconfirmedTransactions.values()) {

            transactionsData.add(transaction.getJSONObject());

        }
        response.put("unconfirmedTransactions", transactionsData);


        return response;
    }

}
