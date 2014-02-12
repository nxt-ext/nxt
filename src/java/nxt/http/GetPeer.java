package nxt.http;

import nxt.peer.Peer;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.MISSING_PEER;
import static nxt.http.JSONResponses.UNKNOWN_PEER;

public final class GetPeer extends HttpRequestDispatcher.HttpRequestHandler {

    static final GetPeer instance = new GetPeer();

    private GetPeer() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String peer = req.getParameter("peer");
        if (peer == null) {
            return MISSING_PEER;
        }

        Peer peerData = Peer.getPeer(peer);
        if (peerData == null) {
            return UNKNOWN_PEER;
        }

        JSONObject response = new JSONObject();
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

        return response;
    }

}
