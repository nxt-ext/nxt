package nxt.http;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import nxt.NxtException;
import nxt.http.APIServlet.APIRequestHandler;
import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.peer.Peers.Event;
import nxt.util.Listener;
import nxt.util.Logger;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.MISSING_PEER;

public class AddPeer extends APIRequestHandler {

    static final AddPeer instance = new AddPeer();
    
    private AddPeer() {
        super(new APITag[] {APITag.NETWORK}, "peer");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest request)
            throws NxtException {
        JSONObject response = new JSONObject();
        
        final String peerAddress = request.getParameter("peer");
        if (peerAddress == null) {
            return MISSING_PEER;
        }
        
        final boolean isNewlyAdded[] = new boolean[1];
        Listener<Peer> listener = new Listener<Peer>() {
            @Override
            public void notify(Peer p) {
                if (!isNewlyAdded[0]) {
                    try {
                        InetAddress peerAddr = InetAddress.getByName(new URI("http://" + p.getAnnouncedAddress()).getHost());
                        isNewlyAdded[0] = peerAddr.equals(InetAddress.getByName(new URI("http://" + peerAddress).getHost()));
                    } catch (URISyntaxException | UnknownHostException e) {
                        Logger.logErrorMessage("Should never happen", e);
                    }
                }
            }
        };
        Peers.addListener(listener, Event.NEW_PEER);
        
        Peer peer = Peers.addPeer(peerAddress);
        
        Peers.removeListener(listener, Event.NEW_PEER);
        
        if (peer != null) {
            Peers.connectPeer(peer);
            response = JSONData.peer(peer);
            response.put("isNewlyAdded", isNewlyAdded[0]);
        } else {
            response.put("error", "Failed to add peer");
        }
        
        return response;
    }

}
