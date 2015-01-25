package nxt.peer;

import nxt.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class AddPeers extends PeerServlet.PeerRequestHandler {

    static final AddPeers instance = new AddPeers();

    private AddPeers() {}

    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {
        final JSONArray peers = (JSONArray)request.get("peers");
        if (peers != null && Peers.getMorePeers && !Peers.hasTooManyKnownPeers()) {
            Peers.peersService.submit(new Runnable() {
                @Override
                public void run() {
                    for (Object announcedAddress : peers) {
                        Peers.addPeer((String) announcedAddress);
                    }
                }
            });
        }
        return JSON.emptyJSON;
    }

}
