package nxt.peer;

import nxt.Blockchain;
import nxt.Nxt;
import org.json.simple.JSONObject;

final class GetInfo extends HttpJSONRequestHandler {

    static final GetInfo instance = new GetInfo();

    private GetInfo() {}


    @Override
    public JSONObject processJSONRequest(JSONObject request, Peer peer) {

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

            } else {

                application = application.trim();
                if (application.length() > 20) {

                    application = application.substring(0, 20) + "...";

                }

            }
            peer.setApplication(application);

            String version = (String)request.get("version");
            if (version == null) {

                version = "?";

            } else {

                version = version.trim();
                if (version.length() > 10) {

                    version = version.substring(0, 10) + "...";

                }

            }
            peer.setVersion(version);

            String platform = (String)request.get("platform");
            if (platform == null) {

                platform = "?";

            } else {

                platform = platform.trim();
                if (platform.length() > 10) {

                    platform = platform.substring(0, 10) + "...";

                }

            }
            peer.setPlatform(platform);

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
