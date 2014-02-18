package nxt.peer;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetInfo extends PeerServlet.PeerRequestHandler {

    static final GetInfo instance = new GetInfo();

    private GetInfo() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {
        PeerImpl peerImpl = (PeerImpl)peer;
        String announcedAddress = (String)request.get("announcedAddress");
        if (announcedAddress != null) {
            announcedAddress = announcedAddress.trim();
            if (announcedAddress.length() > 0) {
                peerImpl.setAnnouncedAddress(announcedAddress);
            }
        }
        String application = (String)request.get("application");
        if (application == null) {
            application = "?";
        }
        peerImpl.setApplication(application.trim());

        String version = (String)request.get("version");
        if (version == null) {
            version = "?";
        }
        peerImpl.setVersion(version.trim());

        String platform = (String)request.get("platform");
        if (platform == null) {
            platform = "?";
        }
        peerImpl.setPlatform(platform.trim());

        peerImpl.setShareAddress(Boolean.TRUE.equals(request.get("shareAddress")));

        peerImpl.setState(Peer.State.CONNECTED);

        return Peers.myPeerInfoResponse;

    }

}
