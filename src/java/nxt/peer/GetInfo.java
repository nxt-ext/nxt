package nxt.peer;

import nxt.Nxt;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetInfo extends PeerServlet.PeerRequestHandler {

    static final GetInfo instance = new GetInfo();

    private static final JSONStreamAware INVALID_ANNOUNCED_ADDRESS;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.INVALID_ANNOUNCED_ADDRESS);
        INVALID_ANNOUNCED_ADDRESS = JSON.prepare(response);
    }

    private GetInfo() {}

    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {
        PeerImpl peerImpl = (PeerImpl)peer;
        peerImpl.analyzeHallmark((String)request.get("hallmark"));
        if (!Peers.ignorePeerAnnouncedAddress) {
            String announcedAddress = (String) request.get("announcedAddress");
            if (announcedAddress != null && (announcedAddress = announcedAddress.trim()).length() > 0) {
                announcedAddress = Peers.addressWithPort(announcedAddress.toLowerCase());
                if (announcedAddress != null && !announcedAddress.equals(peerImpl.getAnnouncedAddress())) {
                    if (!peerImpl.verifyAnnouncedAddress(announcedAddress)) {
                        Logger.logDebugMessage("Ignoring invalid announced address for " + peerImpl.getHost());
                        return INVALID_ANNOUNCED_ADDRESS;
                    }
                    // force checking connectivity to new announced address
                    Logger.logDebugMessage("Peer " + peer.getHost() + " changed announced address from " + peer.getAnnouncedAddress() + " to " + announcedAddress);
                    peerImpl.setState(Peer.State.NON_CONNECTED);
                }
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
        peerImpl.setLastUpdated(Nxt.getEpochTime());

        Peers.addPeer(peerImpl);

        return Peers.myPeerInfoResponse;

    }

}
