package nxt.http;

import nxt.peer.Peer;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetPeer extends HttpRequestHandler {

    static final GetPeer instance = new GetPeer();

    private GetPeer() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String peer = req.getParameter("peer");
        if (peer == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"peer\" not specified");

        } else {

            Peer peerData = Peer.peers.get(peer);
            if (peerData == null) {

                response.put("errorCode", 5);
                response.put("errorDescription", "Unknown peer");

            } else {

                response.put("state", peerData.state);
                response.put("announcedAddress", peerData.announcedAddress);
                if (peerData.hallmark != null) {

                    response.put("hallmark", peerData.hallmark);

                }
                response.put("weight", peerData.getWeight());
                response.put("downloadedVolume", peerData.downloadedVolume);
                response.put("uploadedVolume", peerData.uploadedVolume);
                response.put("application", peerData.application);
                response.put("version", peerData.version);
                response.put("platform", peerData.platform);

            }

        }
        return response;
    }

}
