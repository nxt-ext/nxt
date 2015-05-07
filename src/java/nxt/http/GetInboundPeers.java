package nxt.http;

import nxt.peer.Peer;
import nxt.peer.Peers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

/**
 * <p>The GetInboundPeers API will return a list of inbound peers.
 * An inbound peer is a peer that has sent a request to this peer
 * within the previous 30 minutes.</p>
 *
 * <p>Request parameters:</p>
 * <ul>
 * <li>includePeerInfo - Specify 'true' to include the peer information
 * or 'false' to include just the peer address.  The default is 'false'.</li>
 * </ul>
 *
 * <p>Response parameters:</p>
 * <ul>
 * <li>peers - An array of peers</li>
 * </ul>
 *
 * <p>Error Response parameters:</p>
 * <ul>
 * <li>errorCode - API error code</li>
 * <li>errorDescription - API error description</li>
 * </ul>
 */
public final class GetInboundPeers extends APIServlet.APIRequestHandler {

    /** GetInboundPeers instance */
    static final GetInboundPeers instance = new GetInboundPeers();

    /**
     * Create the GetInboundPeers instance
     */
    private GetInboundPeers() {
        super(new APITag[] {APITag.NETWORK}, "includePeerInfo");
    }

    /**
     * Process the GetInboundPeers API request
     *
     * @param   req                 API request
     * @return                      API response or null
     */
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {
        boolean includePeerInfo = "true".equalsIgnoreCase(req.getParameter("includePeerInfo"));
        Collection<? extends Peer> peers = Peers.getInboundPeers();
        JSONArray peersJSON = new JSONArray();
        peers.stream().forEach(peer -> peersJSON.add(includePeerInfo ? JSONData.peer(peer) : peer.getHost()));
        JSONObject response = new JSONObject();
        response.put("peers", peersJSON);
        return response;
    }
}
