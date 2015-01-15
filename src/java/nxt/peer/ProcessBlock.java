package nxt.peer;

import nxt.Nxt;
import nxt.NxtException;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class ProcessBlock extends PeerServlet.PeerRequestHandler {

    static final ProcessBlock instance = new ProcessBlock();

    private ProcessBlock() {}

    @Override
    JSONStreamAware processRequest(final JSONObject request, final Peer peer) {
        if (Nxt.getBlockchain().getLastBlock().getStringId().equals(request.get("previousBlock"))) {
            Peers.peersService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        Nxt.getBlockchainProcessor().processPeerBlock(request);
                    } catch (NxtException | RuntimeException e) {
                        if (peer != null) {
                            peer.blacklist(e);
                        }
                    }
                }
            });
        }
        return JSON.emptyJSON;
    }

}
