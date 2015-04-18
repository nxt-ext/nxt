package nxt;

import nxt.db.DbIterator;
import nxt.util.Observable;
import org.json.simple.JSONObject;

import java.util.List;

public interface TransactionProcessor extends Observable<List<? extends Transaction>,TransactionProcessor.Event> {

    enum Event {
        REMOVED_UNCONFIRMED_TRANSACTIONS,
        ADDED_UNCONFIRMED_TRANSACTIONS,
        ADDED_CONFIRMED_TRANSACTIONS,
        RELEASE_PHASED_TRANSACTION,
        REJECT_PHASED_TRANSACTION
    }

    DbIterator<? extends Transaction> getAllUnconfirmedTransactions();

    Transaction getUnconfirmedTransaction(long transactionId);

    Transaction[] getAllLostTransactions();

    Transaction[] getAllBroadcastedTransactions();

    void clearUnconfirmedTransactions();

    void requeueAllUnconfirmedTransactions();

    void rebroadcastAllUnconfirmedTransactions();

    void broadcast(Transaction transaction) throws NxtException.ValidationException;

    void processPeerTransactions(JSONObject request) throws NxtException.ValidationException;

}
