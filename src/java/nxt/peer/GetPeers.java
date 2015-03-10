package nxt.peer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.stream.Collectors;

final class GetPeers extends PeerServlet.PeerRequestHandler {

    static final GetPeers instance = new GetPeers();

    private GetPeers() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        response.put("peers", Peers.getAllPeers().parallelStream().unordered()
                .filter(otherPeer -> ! otherPeer.isBlacklisted() && otherPeer.getAnnouncedAddress() != null
                        && otherPeer.getState() == Peer.State.CONNECTED && otherPeer.shareAddress())
                .map(Peer::getAnnouncedAddress)
                .collect(Collectors.toCollection(JSONArray::new)));

        return response;
    }

}
