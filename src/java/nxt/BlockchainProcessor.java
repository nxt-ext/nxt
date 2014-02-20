package nxt;

import nxt.peer.Peer;
import nxt.util.Listener;
import org.json.simple.JSONObject;

public interface BlockchainProcessor {

    public static enum Event {
        BLOCK_PUSHED, BLOCK_POPPED, BLOCK_GENERATED
    }

    boolean addBlockListener(Listener<Block> listener, Event eventType);

    boolean removeBlockListener(Listener<Block> listener, Event eventType);

    Peer getLastBlockchainFeeder();

    boolean pushBlock(JSONObject request) throws NxtException;

    void fullReset();


    public static class BlockNotAcceptedException extends NxtException {

        BlockNotAcceptedException(String message) {
            super(message);
        }

    }

    public static class BlockOutOfOrderException extends BlockNotAcceptedException {

        BlockOutOfOrderException(String message) {
            super(message);
        }

	}

}
