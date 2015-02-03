package nxt.peer;

import nxt.Block;
import nxt.Nxt;
import nxt.NxtException;
import nxt.util.Convert;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class ProcessBlock extends PeerServlet.PeerRequestHandler {

    static final ProcessBlock instance = new ProcessBlock();

    private ProcessBlock() {}

    @Override
    JSONStreamAware processRequest(final JSONObject request, final Peer peer) {
        String previousBlockId = (String)request.get("previousBlock");
        Block lastBlock = Nxt.getBlockchain().getLastBlock();
        if (lastBlock.getStringId().equals(previousBlockId) ||
                (Convert.parseUnsignedLong(previousBlockId) == lastBlock.getPreviousBlockId()
                        && lastBlock.getTimestamp() > Convert.parseLong(request.get("timestamp")))) {
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
