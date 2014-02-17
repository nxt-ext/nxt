package nxt.peer;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetInfo extends HttpJSONRequestHandler {

    static final GetInfo instance = new GetInfo();

    private GetInfo() {}


    @Override
    JSONStreamAware processJSONRequest(JSONObject request, Peer peer) {

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

        peer.setState(Peer.State.CONNECTED);

        return Peer.myPeerInfoResponse;

    }

}
