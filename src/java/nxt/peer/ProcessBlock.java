package nxt.peer;

import nxt.Blockchain;
import nxt.NxtException;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class ProcessBlock extends HttpJSONRequestHandler {

    static final ProcessBlock instance = new ProcessBlock();

    private ProcessBlock() {}

    private static final JSONStreamAware ACCEPTED;
    static {
        JSONObject response = new JSONObject();
        response.put("accepted", true);
        ACCEPTED = JSON.prepare(response);
    }

    private static final JSONStreamAware NOT_ACCEPTED;
    static {
        JSONObject response = new JSONObject();
        response.put("accepted", false);
        NOT_ACCEPTED = JSON.prepare(response);
    }

    @Override
    JSONStreamAware processJSONRequest(JSONObject request, Peer peer) {

        try {

            boolean accepted = Blockchain.pushBlock(request);
            if (!accepted) {
                Logger.logDebugMessage("Rejecting block from peer " + (peer != null ? peer.getPeerAddress() : ""));
            }
            return accepted ? ACCEPTED : NOT_ACCEPTED;

        } catch (NxtException.ValidationException e) {
            if (peer != null) {
                peer.blacklist(e);
            }
            return NOT_ACCEPTED;
        }

    }

}
