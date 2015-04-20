package nxt;

import nxt.db.DerivedDbTable;
import nxt.peer.Peer;
import nxt.util.Observable;
import org.json.simple.JSONObject;

import java.util.List;

public interface BlockchainProcessor extends Observable<Block,BlockchainProcessor.Event> {

    enum Event {
        BLOCK_PUSHED, BLOCK_POPPED, BLOCK_GENERATED, BLOCK_SCANNED,
        RESCAN_BEGIN, RESCAN_END,
        BEFORE_BLOCK_ACCEPT,
        BEFORE_BLOCK_APPLY, AFTER_BLOCK_APPLY
    }

    Peer getLastBlockchainFeeder();

    int getLastBlockchainFeederHeight();

    boolean isScanning();

    int getMinRollbackHeight();

    void processPeerBlock(JSONObject request) throws NxtException;

    void fullReset();

    void scan(int height, boolean validate);

    void fullScanWithShutdown();

    void setGetMoreBlocks(boolean getMoreBlocks);

    List<? extends Block> popOffTo(int height);

    void registerDerivedTable(DerivedDbTable table);

    void trimDerivedTables();

    class BlockNotAcceptedException extends NxtException {

        BlockNotAcceptedException(String message) {
            super(message);
        }

        BlockNotAcceptedException(Throwable cause) {
            super(cause);
        }

    }

    class TransactionNotAcceptedException extends BlockNotAcceptedException {

        private final TransactionImpl transaction;

        TransactionNotAcceptedException(String message, TransactionImpl transaction) {
            super(message);
            this.transaction = transaction;
        }

        TransactionNotAcceptedException(Throwable cause, TransactionImpl transaction) {
            super(cause);
            this.transaction = transaction;
        }

        public TransactionImpl getTransaction() {
            return transaction;
        }

        @Override
        public String getMessage() {
            return super.getMessage() + ", transaction " + transaction.getStringId() + ": " + transaction.getJSONObject().toJSONString();
        }
    }

    class BlockOutOfOrderException extends BlockNotAcceptedException {

        BlockOutOfOrderException(String message) {
            super(message);
        }

	}

}
