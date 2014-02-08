package nxt.peer;

import nxt.Blockchain;
import nxt.Nxt;
import org.json.simple.JSONObject;

final class GetInfo extends HttpJSONRequestHandler {

    static final GetInfo instance = new GetInfo();

    private GetInfo() {}


    @Override
    JSONObject processJSONRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        if (peer != null) {
            String announcedAddress = (String)request.get("announcedAddress");
            if (announcedAddress != null) {
                announcedAddress = announcedAddress.trim();
                if (announcedAddress.length() > 0) {
                    peer.setAnnouncedAddress(announcedAddress);
                }
            }
            String application = (String)request.get("application");
            if (application == null) {
                application = "?";
            }
            peer.setApplication(application.trim());

            String version = (String)request.get("version");
            if (version == null) {
                version = "?";
            }
            peer.setVersion(version.trim());

            String platform = (String)request.get("platform");
            if (platform == null) {
                platform = "?";
            }
            peer.setPlatform(platform.trim());

            peer.setShareAddress(Boolean.TRUE.equals(request.get("shareAddress")));

        }

        if (Nxt.myHallmark != null && Nxt.myHallmark.length() > 0) {

            response.put("hallmark", Nxt.myHallmark);

        }
        response.put("application", "NRS");
        response.put("version", Nxt.VERSION);
        response.put("platform", Nxt.myPlatform);
        response.put("shareAddress", Nxt.shareMyAddress);

        response.put("cumulativeDifficulty", Blockchain.getLastBlock().getCumulativeDifficulty().toString());

        return response;
    }

}
