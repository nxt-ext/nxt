package nxt;

import nxt.peer.Peer;
import nxt.util.Observable;
import org.json.simple.JSONObject;

public interface BlockchainProcessor extends Observable<Block,BlockchainProcessor.Event> {

    public static enum Event {
        BLOCK_PUSHED, BLOCK_POPPED, BLOCK_GENERATED, BLOCK_SCANNED
    }

    Peer getLastBlockchainFeeder();

    void pushBlock(JSONObject request) throws NxtException;

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
