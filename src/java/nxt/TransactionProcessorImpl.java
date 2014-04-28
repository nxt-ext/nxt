package nxt;

import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import nxt.util.ThreadPool;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class TransactionProcessorImpl implements TransactionProcessor {

    private static final TransactionProcessorImpl instance = new TransactionProcessorImpl();

    static TransactionProcessorImpl getInstance() {
        return instance;
    }

    private final ConcurrentMap<Long, TransactionImpl> unconfirmedTransactions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TransactionImpl> unconfirmedTransactionHashes = new ConcurrentHashMap<>();
    private final Collection<TransactionImpl> allUnconfirmedTransactions = Collections.unmodifiableCollection(unconfirmedTransactions.values());
    private final ConcurrentMap<Long, TransactionImpl> nonBroadcastedTransactions = new ConcurrentHashMap<>();
    private static class TransactionHashInfo {
        private final Long transactionId;
        private final int expiration;
        private TransactionHashInfo(Transaction transaction) {
            this.transactionId = transaction.getId();
            this.expiration = transaction.getExpiration();
        }
    }
    private final ConcurrentMap<String, TransactionHashInfo> transactionHashes = new ConcurrentHashMap<>();
    private final Listeners<List<Transaction>,Event> transactionListeners = new Listeners<>();

    private final Runnable removeUnconfirmedTransactionsThread = new Runnable() {

        @Override
        public void run() {

            try {
                try {

                    int curTime = Convert.getEpochTime();
                    List<Transaction> removedUnconfirmedTransactions = new ArrayList<>();

                    synchronized (BlockchainImpl.getInstance()) {
                        Iterator<TransactionImpl> iterator = unconfirmedTransactions.values().iterator();
                        while (iterator.hasNext()) {
                            TransactionImpl transaction = iterator.next();
                            if (transaction.getExpiration() < curTime) {
                                iterator.remove();
                                unconfirmedTransactionHashes.remove(transaction.getHash());
                                transaction.undoUnconfirmed();
                                removedUnconfirmedTransactions.add(transaction);
                            }
                        }
                    }

                    if (removedUnconfirmedTransactions.size() > 0) {
                        transactionListeners.notify(removedUnconfirmedTransactions, Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
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

    private final Runnable rebroadcastTransactionsThread = new Runnable() {

        @Override
        public void run() {

            try {
                try {
                    List<Transaction> transactionList = new ArrayList<>();
                    int curTime = Convert.getEpochTime();
                    for (TransactionImpl transaction : nonBroadcastedTransactions.values()) {
                        if (TransactionDb.hasTransaction(transaction.getId()) || transaction.getExpiration() < curTime) {
                            nonBroadcastedTransactions.remove(transaction.getId());
                        } else if (transaction.getTimestamp() < curTime - 30) {
                            transactionList.add(transaction);
                        }
                    }

                    if (transactionList.size() > 0) {
                        Peers.sendToSomePeers(transactionList);
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

    private final Runnable processTransactionsThread = new Runnable() {

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
                    if (transactionsData == null || transactionsData.size() == 0) {
                        return;
                    }
                    processPeerTransactions(transactionsData, false);
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

    private TransactionProcessorImpl() {
        ThreadPool.scheduleThread(processTransactionsThread, 5);
        ThreadPool.scheduleThread(removeUnconfirmedTransactionsThread, 1);
        ThreadPool.scheduleThread(rebroadcastTransactionsThread, 60);
    }

    @Override
    public boolean addListener(Listener<List<Transaction>> listener, Event eventType) {
        return transactionListeners.addListener(listener, eventType);
    }

    @Override
    public boolean removeListener(Listener<List<Transaction>> listener, Event eventType) {
        return transactionListeners.removeListener(listener, eventType);
    }

    @Override
    public Collection<TransactionImpl> getAllUnconfirmedTransactions() {
        return allUnconfirmedTransactions;
    }

    @Override
    public Transaction getUnconfirmedTransaction(Long transactionId) {
        return unconfirmedTransactions.get(transactionId);
    }

    @Override
    public Transaction newTransaction(short deadline, byte[] senderPublicKey, Long recipientId,
                                      long amountNQT, long feeNQT, String referencedTransactionFullHash)
            throws NxtException.ValidationException {
        TransactionImpl transaction = new TransactionImpl(TransactionType.Payment.ORDINARY, Convert.getEpochTime(), deadline, senderPublicKey,
                recipientId, amountNQT, feeNQT, referencedTransactionFullHash, null);
        transaction.validateAttachment();
        return transaction;
    }

    @Override
    public Transaction newTransaction(short deadline, byte[] senderPublicKey, Long recipientId,
                                      long amountNQT, long feeNQT, String referencedTransactionFullHash, Attachment attachment)
            throws NxtException.ValidationException {
        TransactionImpl transaction = new TransactionImpl(attachment.getTransactionType(), Convert.getEpochTime(), deadline,
                senderPublicKey, recipientId, amountNQT, feeNQT, referencedTransactionFullHash, null);
        transaction.setAttachment(attachment);
        transaction.validateAttachment();
        return transaction;
    }

    @Override
    public void broadcast(Transaction transaction) throws NxtException.ValidationException {
        if (! transaction.verify()) {
            throw new NxtException.ValidationException("Transaction signature verification failed");
        }
        List<Transaction> validTransactions = processTransactions(Collections.singletonList((TransactionImpl) transaction), true);
        if (validTransactions.contains(transaction)) {
            nonBroadcastedTransactions.put(transaction.getId(), (TransactionImpl) transaction);
            Logger.logDebugMessage("Accepted new transaction " + transaction.getStringId());
        } else {
            Logger.logDebugMessage("Rejecting double spending transaction " + transaction.getStringId());
            throw new NxtException.ValidationException("Double spending transaction");
        }
    }

    @Override
    public void processPeerTransactions(JSONObject request) {
        JSONArray transactionsData = (JSONArray)request.get("transactions");
        processPeerTransactions(transactionsData, true);
    }

    @Override
    public Transaction parseTransaction(byte[] bytes) throws NxtException.ValidationException {

        try {
            boolean useNQT = Nxt.getBlockchain().getLastBlock().getHeight() >= Constants.NQT_BLOCK;
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            byte type = buffer.get();
            byte subtype = buffer.get();
            int timestamp = buffer.getInt();
            short deadline = buffer.getShort();
            byte[] senderPublicKey = new byte[32];
            buffer.get(senderPublicKey);
            Long recipientId = buffer.getLong();
            long amountNQT;
            long feeNQT;
            String referencedTransactionFullHash = null;
            Long referencedTransactionId = null;
            if (! useNQT) {
                amountNQT = ((long)buffer.getInt()) * Constants.ONE_NXT;
                feeNQT = ((long)buffer.getInt()) * Constants.ONE_NXT;
                referencedTransactionId = Convert.zeroToNull(buffer.getLong());
            } else {
                amountNQT = buffer.getLong();
                feeNQT = buffer.getLong();
                byte[] referencedTransactionFullHashBytes = new byte[32];
                buffer.get(referencedTransactionFullHashBytes);
                if (Convert.emptyToNull(referencedTransactionFullHashBytes) != null) {
                    referencedTransactionFullHash = Convert.toHexString(referencedTransactionFullHashBytes);
                    referencedTransactionId = Convert.fullHashToId(referencedTransactionFullHash);
                }
            }
            byte[] signature = new byte[64];
            buffer.get(signature);
            signature = Convert.emptyToNull(signature);

            TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
            TransactionImpl transaction;
            if (! useNQT) {
                transaction = new TransactionImpl(transactionType, timestamp, deadline, senderPublicKey, recipientId,
                        amountNQT, feeNQT, referencedTransactionId, signature);
            } else {
                transaction = new TransactionImpl(transactionType, timestamp, deadline, senderPublicKey, recipientId,
                        amountNQT, feeNQT, referencedTransactionFullHash, signature);
            }
            transactionType.loadAttachment(transaction, buffer);

            return transaction;

        } catch (RuntimeException e) {
            throw new NxtException.ValidationException(e.toString(), e);
        }
    }

    TransactionImpl parseTransaction(JSONObject transactionData) throws NxtException.ValidationException {

        try {

            byte type = ((Long)transactionData.get("type")).byteValue();
            byte subtype = ((Long)transactionData.get("subtype")).byteValue();
            int timestamp = ((Long)transactionData.get("timestamp")).intValue();
            short deadline = ((Long)transactionData.get("deadline")).shortValue();
            byte[] senderPublicKey = Convert.parseHexString((String) transactionData.get("senderPublicKey"));
            Long recipientId = Convert.parseUnsignedLong((String) transactionData.get("recipient"));
            if (recipientId == null) recipientId = 0L; // ugly
            long amountNQT;
            long feeNQT;
            if (transactionData.get("amountNQT") != null) {
                amountNQT = ((Long) transactionData.get("amountNQT"));
                feeNQT = ((Long) transactionData.get("feeNQT"));
            } else {
                amountNQT = Convert.safeMultiply(((Long) transactionData.get("amount")), Constants.ONE_NXT);
                feeNQT = Convert.safeMultiply(((Long) transactionData.get("fee")), Constants.ONE_NXT);
            }
            String referencedTransactionFullHash = (String) transactionData.get("referencedTransactionFullHash");
            Long referencedTransactionId = Convert.parseUnsignedLong((String) transactionData.get("referencedTransaction"));
            byte[] signature = Convert.parseHexString((String) transactionData.get("signature"));

            TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
            TransactionImpl transaction;
            if (referencedTransactionFullHash != null) {
                transaction = new TransactionImpl(transactionType, timestamp, deadline, senderPublicKey, recipientId,
                        amountNQT, feeNQT, referencedTransactionFullHash, signature);
            } else {
                transaction = new TransactionImpl(transactionType, timestamp, deadline, senderPublicKey, recipientId,
                        amountNQT, feeNQT, referencedTransactionId, signature);
            }

            JSONObject attachmentData = (JSONObject)transactionData.get("attachment");

            transactionType.loadAttachment(transaction, attachmentData);

            return transaction;

        } catch (RuntimeException e) {
            Logger.logDebugMessage(e.toString(), e);
            throw new NxtException.ValidationException(e.toString());
        }
    }

    void clear() {
        unconfirmedTransactions.clear();
        unconfirmedTransactionHashes.clear();
        nonBroadcastedTransactions.clear();
        transactionHashes.clear();
    }

    void apply(BlockImpl block) {
        block.apply();
        for (TransactionImpl transaction : block.getTransactions()) {
            if (! unconfirmedTransactions.containsKey(transaction.getId())) {
                transaction.applyUnconfirmed();
            }
            //TODO: Phaser not yet implemented
            //Phaser.processTransaction(transaction);
            transaction.apply();
            transactionHashes.put(transaction.getHash(), new TransactionHashInfo(transaction));
        }
        purgeExpiredHashes(block.getTimestamp());
    }

    void undo(BlockImpl block) throws TransactionType.UndoNotSupportedException {
        block.undo();
        List<Transaction> addedUnconfirmedTransactions = new ArrayList<>();
        for (TransactionImpl transaction : block.getTransactions()) {
            TransactionHashInfo transactionHashInfo = transactionHashes.get(transaction.getHash());
            if (transactionHashInfo != null && transactionHashInfo.transactionId.equals(transaction.getId())) {
                transactionHashes.remove(transaction.getHash());
            }
            unconfirmedTransactions.put(transaction.getId(), transaction);
            unconfirmedTransactionHashes.put(transaction.getHash(), transaction);
            transaction.undo();
            addedUnconfirmedTransactions.add(transaction);
        }
        if (addedUnconfirmedTransactions.size() > 0) {
            transactionListeners.notify(addedUnconfirmedTransactions, TransactionProcessor.Event.ADDED_UNCONFIRMED_TRANSACTIONS);
        }
    }

    TransactionImpl checkTransactionHashes(BlockImpl block) {
        TransactionImpl duplicateTransaction = null;
        for (TransactionImpl transaction : block.getTransactions()) {
            if (transactionHashes.putIfAbsent(transaction.getHash(), new TransactionHashInfo(transaction)) != null && block.getHeight() != 58294) {
                duplicateTransaction = transaction;
                break;
            }
        }

        if (duplicateTransaction != null) {
            for (TransactionImpl transaction : block.getTransactions()) {
                if (! transaction.equals(duplicateTransaction)) {
                    TransactionHashInfo transactionHashInfo = transactionHashes.get(transaction.getHash());
                    if (transactionHashInfo != null && transactionHashInfo.transactionId.equals(transaction.getId())) {
                        transactionHashes.remove(transaction.getHash());
                    }
                }
            }
        }
        return duplicateTransaction;
    }

    void updateUnconfirmedTransactions(BlockImpl block) {
        List<Transaction> addedConfirmedTransactions = new ArrayList<>();
        List<Transaction> removedUnconfirmedTransactions = new ArrayList<>();

        for (Transaction transaction : block.getTransactions()) {
            addedConfirmedTransactions.add(transaction);
            Transaction removedTransaction = unconfirmedTransactions.remove(transaction.getId());
            if (removedTransaction != null) {
                unconfirmedTransactionHashes.remove(transaction.getHash());
                removedUnconfirmedTransactions.add(removedTransaction);
            }
        }

        if (removedUnconfirmedTransactions.size() > 0) {
            transactionListeners.notify(removedUnconfirmedTransactions, TransactionProcessor.Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
        }
        if (addedConfirmedTransactions.size() > 0) {
            transactionListeners.notify(addedConfirmedTransactions, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
        }

    }

    void removeUnconfirmedTransactions(Collection<TransactionImpl> transactions) {
        List<Transaction> removedList = new ArrayList<>();
        for (TransactionImpl transaction : transactions) {
            if (unconfirmedTransactions.remove(transaction.getId()) != null) {
                transaction.undoUnconfirmed();
                unconfirmedTransactionHashes.remove(transaction.getHash());
                removedList.add(transaction);
            }
        }
        transactionListeners.notify(removedList, Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
    }

    void shutdown() {
        removeUnconfirmedTransactions(new ArrayList<>(unconfirmedTransactions.values()));
    }

    private void purgeExpiredHashes(int blockTimestamp) {
        Iterator<Map.Entry<String, TransactionHashInfo>> iterator = transactionHashes.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiration < blockTimestamp) {
                iterator.remove();
            }
        }
    }

    private void processPeerTransactions(JSONArray transactionsData, final boolean sendToPeers) {
        List<TransactionImpl> transactions = new ArrayList<>();
        for (Object transactionData : transactionsData) {
            try {
                transactions.add(parseTransaction((JSONObject) transactionData));
            } catch (NxtException.ValidationException e) {
                //if (! (e instanceof TransactionType.NotYetEnabledException)) {
                //    Logger.logDebugMessage("Dropping invalid transaction: " + e.getMessage());
                //}
            }
        }
        processTransactions(transactions, sendToPeers);
        for (TransactionImpl transaction : transactions) {
            nonBroadcastedTransactions.remove(transaction.getId());
        }
    }

    private List<Transaction> processTransactions(List<TransactionImpl> transactions, final boolean sendToPeers) {
        List<Transaction> sendToPeersTransactions = new ArrayList<>();
        List<Transaction> addedUnconfirmedTransactions = new ArrayList<>();
        List<Transaction> addedDoubleSpendingTransactions = new ArrayList<>();

        for (TransactionImpl transaction : transactions) {

            try {

                int curTime = Convert.getEpochTime();
                if (transaction.getTimestamp() > curTime + 15 || transaction.getExpiration() < curTime
                        || transaction.getDeadline() > 1440) {
                    continue;
                }

                synchronized (BlockchainImpl.getInstance()) {

                    Long id = transaction.getId();
                    if (TransactionDb.hasTransaction(id) || unconfirmedTransactions.containsKey(id)
                            || ! transaction.verify()) {
                        continue;
                    }

                    if (transactionHashes.containsKey(transaction.getHash())
                            || unconfirmedTransactionHashes.containsKey(transaction.getHash())) {
                        continue;
                    }

                    if (transaction.applyUnconfirmed()) {
                        if (sendToPeers) {
                            if (nonBroadcastedTransactions.containsKey(id)) {
                                Logger.logDebugMessage("Received back transaction " + transaction.getStringId()
                                        + " that we generated, will not forward to peers");
                                nonBroadcastedTransactions.remove(id);
                            } else {
                                sendToPeersTransactions.add(transaction);
                            }
                        }
                        unconfirmedTransactions.put(id, transaction);
                        unconfirmedTransactionHashes.put(transaction.getHash(), transaction);
                        addedUnconfirmedTransactions.add(transaction);
                    } else {
                        addedDoubleSpendingTransactions.add(transaction);
                    }
                }

            } catch (RuntimeException e) {
                Logger.logMessage("Error processing transaction", e);
            }

        }

        if (sendToPeersTransactions.size() > 0) {
            Peers.sendToSomePeers(sendToPeersTransactions);
        }

        if (addedUnconfirmedTransactions.size() > 0) {
            transactionListeners.notify(addedUnconfirmedTransactions, Event.ADDED_UNCONFIRMED_TRANSACTIONS);
        }
        if (addedDoubleSpendingTransactions.size() > 0) {
            transactionListeners.notify(addedDoubleSpendingTransactions, Event.ADDED_DOUBLESPENDING_TRANSACTIONS);
        }
        return addedUnconfirmedTransactions;
    }

}
