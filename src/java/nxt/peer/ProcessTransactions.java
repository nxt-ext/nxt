package nxt.peer;

import nxt.Blockchain;
import org.json.simple.JSONObject;

final class ProcessTransactions extends HttpJSONRequestHandler {

    static final ProcessTransactions instance = new ProcessTransactions();

    private ProcessTransactions() {}


    @Override
    public JSONObject processJSONRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        Blockchain.processTransactions(request, "transactions");

        return response;
    }

}
