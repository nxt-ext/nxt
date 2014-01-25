package nxt.peer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

final class GetPeers extends HttpJSONRequestHandler {

    static final GetPeers instance = new GetPeers();

    private GetPeers() {}


    @Override
    public JSONObject processJSONRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        JSONArray peers = new JSONArray();
        for (Peer otherPeer : Peer.peers.values()) {

            if (otherPeer.blacklistingTime == 0 && otherPeer.announcedAddress.length() > 0
                    && otherPeer.state == Peer.STATE_CONNECTED && otherPeer.shareAddress) {

                peers.add(otherPeer.announcedAddress);

            }

        }
        response.put("peers", peers);

        return response;
    }

}
