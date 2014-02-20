package nxt;

import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class TransactionProcessor {


    static final ConcurrentMap<Long, TransactionImpl> doubleSpendingTransactions = new ConcurrentHashMap<>();
    static final ConcurrentMap<Long, TransactionImpl> unconfirmedTransactions = new ConcurrentHashMap<>();
    private static final Collection<? extends Transaction> allUnconfirmedTransactions = Collections.unmodifiableCollection(unconfirmedTransactions.values());
    static final ConcurrentMap<Long, TransactionImpl> nonBroadcastedTransactions = new ConcurrentHashMap<>();
    static final ConcurrentMap<String, Transaction> transactionHashes = new ConcurrentHashMap<>();
    static final Listeners<List<Transaction>,Blockchain.Event> transactionListeners = new Listeners<>();



    static final Runnable removeUnconfirmedTransactionsThread = new Runnable() {

        @Override
        public void run() {

            try {
                try {

                    int curTime = Convert.getEpochTime();
                    List<Transaction> removedUnconfirmedTransactions = new ArrayList<>();

                    Iterator<TransactionImpl> iterator = unconfirmedTransactions.values().iterator();
                    while (iterator.hasNext()) {

                        Transaction transaction = iterator.next();
                        if (transaction.getExpiration() < curTime) {
                            iterator.remove();
                            Account account = Account.getAccount(transaction.getSenderId());
                            account.addToUnconfirmedBalance((transaction.getAmount() + transaction.getFee()) * 100L);
                            removedUnconfirmedTransactions.add(transaction);
                        }
                    }

                    if (removedUnconfirmedTransactions.size() > 0) {
                        transactionListeners.notify(removedUnconfirmedTransactions, Blockchain.Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error removing unconfirmed transactions", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    static final Runnable rebroadcastTransactionsThread = new Runnable() {

        @Override
        public void run() {

            try {
                try {
                    JSONArray transactionsData = new JSONArray();

                    for (Transaction transaction : nonBroadcastedTransactions.values()) {
                        if (unconfirmedTransactions.get(transaction.getId()) == null && ! Transactions.hasTransaction(transaction.getId())) {
                            transactionsData.add(transaction.getJSONObject());
                        } else {
                            nonBroadcastedTransactions.remove(transaction.getId());
                        }
                    }

                    if (transactionsData.size() > 0) {
                        JSONObject peerRequest = new JSONObject();
                        peerRequest.put("requestType", "processTransactions");
                        peerRequest.put("transactions", transactionsData);
                        Peers.sendToSomePeers(peerRequest);
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error in transaction re-broadcasting thread", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    static final Runnable processTransactionsThread = new Runnable() {

        private final JSONStreamAware getUnconfirmedTransactionsRequest;
        {
            JSONObject request = new JSONObject();
            request.put("requestType", "getUnconfirmedTransactions");
            getUnconfirmedTransactionsRequest = JSON.prepareRequest(request);
        }

        @Override
        public void run() {
            try {
                try {
                    Peer peer = Peers.getAnyPeer(Peer.State.CONNECTED, true);
                    if (peer == null) {
                        return;
                    }
                    JSONObject response = peer.send(getUnconfirmedTransactionsRequest);
                    if (response == null) {
                        return;
                    }
                    JSONArray transactionsData = (JSONArray)response.get("unconfirmedTransactions");
                    processJSONTransactions(transactionsData, false);
                } catch (Exception e) {
                    Logger.logDebugMessage("Error processing unconfirmed transactions from peer", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }
        }

    };

    private TransactionProcessor() {}

    public static Collection<? extends Transaction> getAllUnconfirmedTransactions() {
        return allUnconfirmedTransactions;
    }

    public static Transaction getUnconfirmedTransaction(Long transactionId) {
        return unconfirmedTransactions.get(transactionId);
    }

    public static void broadcast(Transaction transaction) {

        processTransactions(Arrays.asList((TransactionImpl)transaction), true);
        nonBroadcastedTransactions.put(transaction.getId(), (TransactionImpl) transaction);
        Logger.logDebugMessage("Accepted new transaction " + transaction.getStringId());

    }

    public static Transaction newTransaction(int timestamp, short deadline, byte[] senderPublicKey, Long recipientId,
                                             int amount, int fee, Long referencedTransactionId) throws NxtException.ValidationException {
        return new TransactionImpl(TransactionType.Payment.ORDINARY, timestamp, deadline, senderPublicKey, recipientId, amount, fee, referencedTransactionId, null);
    }

    public static Transaction newTransaction(int timestamp, short deadline, byte[] senderPublicKey, Long recipientId,
                                             int amount, int fee, Long referencedTransactionId, Attachment attachment)
            throws NxtException.ValidationException {
        TransactionImpl transaction = new TransactionImpl(attachment.getTransactionType(), timestamp, deadline, senderPublicKey, recipientId, amount, fee,
                referencedTransactionId, null);
        transaction.setAttachment(attachment);
        return transaction;
    }

    static TransactionImpl newTransaction(int timestamp, short deadline, byte[] senderPublicKey, Long recipientId,
                                          int amount, int fee, Long referencedTransactionId, byte[] signature) throws NxtException.ValidationException {
        return new TransactionImpl(TransactionType.Payment.ORDINARY, timestamp, deadline, senderPublicKey, recipientId, amount, fee, referencedTransactionId, signature);
    }

    public static Transaction getTransaction(byte[] bytes) throws NxtException.ValidationException {
        return Transactions.getTransaction(bytes);
    }

    public static void processTransactions(JSONObject request) {
        JSONArray transactionsData = (JSONArray)request.get("transactions");
        processJSONTransactions(transactionsData, true);
    }

    static void init() {}

    private static void processJSONTransactions(JSONArray transactionsData, final boolean sendToPeers) {
        List<TransactionImpl> transactions = new ArrayList<>();
        for (Object transactionData : transactionsData) {
            try {
                transactions.add(Transactions.getTransaction((JSONObject) transactionData));
            } catch (NxtException.ValidationException e) {
                if (! (e instanceof TransactionType.NotYetEnabledException)) {
                    Logger.logDebugMessage("Dropping invalid transaction", e);
                }
            }
        }
        processTransactions(transactions, sendToPeers);
    }

    private static void processTransactions(List<TransactionImpl> transactions, final boolean sendToPeers) {
        JSONArray validTransactionsData = new JSONArray();
        List<Transaction> addedUnconfirmedTransactions = new ArrayList<>();
        List<Transaction> addedDoubleSpendingTransactions = new ArrayList<>();

        for (TransactionImpl transaction : transactions) {

            try {

                int curTime = Convert.getEpochTime();
                if (transaction.getTimestamp() > curTime + 15 || transaction.getExpiration() < curTime
                        || transaction.getDeadline() > 1440) {
                    continue;
                }

                boolean doubleSpendingTransaction;

                synchronized (Blockchain.class) {

                    Long id = transaction.getId();
                    if (Transactions.hasTransaction(id) || unconfirmedTransactions.containsKey(id)
                            || doubleSpendingTransactions.containsKey(id) || !transaction.verify()) {
                        continue;
                    }

                    if (transactionHashes.containsKey(transaction.getHash())) {
                        continue;
                    }

                    doubleSpendingTransaction = transaction.isDoubleSpending();

                    if (doubleSpendingTransaction) {
                        doubleSpendingTransactions.put(id, transaction);
                    } else {
                        if (sendToPeers) {
                            if (nonBroadcastedTransactions.containsKey(id)) {
                                Logger.logDebugMessage("Received back transaction " + transaction.getStringId()
                                        + " that we generated, will not forward to peers");
                            } else {
                                validTransactionsData.add(transaction.getJSONObject());
                            }
                        }
                        unconfirmedTransactions.put(id, transaction);
                    }
                }

                if (doubleSpendingTransaction) {
                    addedDoubleSpendingTransactions.add(transaction);
                } else {
                    addedUnconfirmedTransactions.add(transaction);
                }

            } catch (RuntimeException e) {
                Logger.logMessage("Error processing transaction", e);
            }

        }

        if (validTransactionsData.size() > 0) {
            JSONObject peerRequest = new JSONObject();
            peerRequest.put("requestType", "processTransactions");
            peerRequest.put("transactions", validTransactionsData);
            Peers.sendToSomePeers(peerRequest);
        }

        if (addedUnconfirmedTransactions.size() > 0) {
            transactionListeners.notify(addedUnconfirmedTransactions, Blockchain.Event.ADDED_UNCONFIRMED_TRANSACTIONS);
        }
        if (addedDoubleSpendingTransactions.size() > 0) {
            transactionListeners.notify(addedDoubleSpendingTransactions, Blockchain.Event.ADDED_DOUBLESPENDING_TRANSACTIONS);
        }


    }

    static void purgeExpiredHashes(int blockTimestamp) {
        Iterator<Map.Entry<String, Transaction>> iterator = transactionHashes.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().getExpiration() < blockTimestamp) {
                iterator.remove();
            }
        }
    }

    public static boolean addTransactionListener(Listener<List<Transaction>> listener, Blockchain.Event eventType) {
        return transactionListeners.addListener(listener, eventType);
    }

    public static boolean removeTransactionListener(Listener<List<Transaction>> listener, Blockchain.Event eventType) {
        return transactionListeners.removeListener(listener, eventType);
    }
}
