package nxt.peer;

import nxt.Blockchain;
import nxt.NxtException;
import nxt.util.JSON;
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
    public JSONStreamAware processJSONRequest(JSONObject request, Peer peer) {

        try {

            return Blockchain.pushBlock(request) ? ACCEPTED : NOT_ACCEPTED;

        } catch (NxtException.ValidationFailure e) {
            if (peer != null) {
                peer.blacklist(e);
            }
            return NOT_ACCEPTED;
        }

    }

}
