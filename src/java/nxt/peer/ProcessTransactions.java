package nxt.peer;

import nxt.Nxt;
import nxt.NxtException;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class ProcessTransactions extends PeerServlet.PeerRequestHandler {

    static final ProcessTransactions instance = new ProcessTransactions();

    private ProcessTransactions() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        try {
            Nxt.getTransactionProcessor().processPeerTransactions(request);
        } catch (RuntimeException | NxtException.ValidationException e) {
            peer.blacklist(e);
        }

        return JSON.emptyJSON;
    }

}
