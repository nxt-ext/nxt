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

            Peer peerData = Peer.getPeer(peer);
            if (peerData == null) {

                response.put("errorCode", 5);
                response.put("errorDescription", "Unknown peer");

            } else {

                response.put("state", peerData.getState().ordinal());
                response.put("announcedAddress", peerData.getAnnouncedAddress());
                if (peerData.getHallmark() != null) {

                    response.put("hallmark", peerData.getHallmark());

                }
                response.put("weight", peerData.getWeight());
                response.put("downloadedVolume", peerData.getDownloadedVolume());
                response.put("uploadedVolume", peerData.getUploadedVolume());
                response.put("application", peerData.getApplication());
                response.put("version", peerData.getVersion());
                response.put("platform", peerData.getPlatform());

            }

        }
        return response;
    }

}
