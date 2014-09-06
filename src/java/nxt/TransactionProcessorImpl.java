package nxt;

import nxt.db.Db;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.VersionedEntityDbTable;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class TransactionProcessorImpl implements TransactionProcessor {

    private static final TransactionProcessorImpl instance = new TransactionProcessorImpl();

    static TransactionProcessorImpl getInstance() {
        return instance;
    }

    final DbKey.LongKeyFactory<TransactionImpl> unconfirmedTransactionDbKeyFactory = new DbKey.LongKeyFactory<TransactionImpl>("id") {

        @Override
        public DbKey newKey(TransactionImpl transaction) {
            return transaction.getDbKey();
        }

    };

    private final VersionedEntityDbTable<TransactionImpl> unconfirmedTransactionTable = new VersionedEntityDbTable<TransactionImpl>(unconfirmedTransactionDbKeyFactory) {

        @Override
        protected TransactionImpl load(Connection con, ResultSet rs) throws SQLException {
            byte[] transactionBytes = rs.getBytes("transaction_bytes");
            try {
                return TransactionImpl.parseTransaction(transactionBytes);
            } catch (NxtException.ValidationException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        @Override
        protected void save(Connection con, TransactionImpl transaction) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO unconfirmed_transaction (id, expiration, transaction_bytes, height, latest) "
                    + "KEY (id, height) VALUES (?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, transaction.getId());
                pstmt.setInt(++i, transaction.getExpiration());
                pstmt.setBytes(++i, transaction.getBytes());
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        @Override
        protected String table() {
            return "unconfirmed_transaction";
        }

        @Override
        public void rollback(int height) {
            List<TransactionImpl> transactions = new ArrayList<>();
            try (Connection con = Db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT * FROM unconfirmed_transaction WHERE height > ?")) {
                pstmt.setInt(1, height);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        transactions.add(load(con, rs));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
            super.rollback(height);
            TransactionProcessorImpl.this.processLater(transactions);
        }

    };

    private final ConcurrentMap<Long, TransactionImpl> nonBroadcastedTransactions = new ConcurrentHashMap<>();
    private final Listeners<List<Transaction>,Event> transactionListeners = new Listeners<>();
    private final Set<TransactionImpl> lostTransactions = new HashSet<>();

    private final Runnable removeUnconfirmedTransactionsThread = new Runnable() {

        @Override
        public void run() {

            try {
                try (Connection con = Db.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("SELECT MIN(height) AS height FROM unconfirmed_transaction "
                             + "WHERE expiration < ?")) {
                    pstmt.setInt(1, Convert.getEpochTime());
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            int height = rs.getInt("height");
                            if (height > 0) {
                                Nxt.getBlockchainProcessor().scan(height - 1);
                            }
                        }
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
                    synchronized (BlockchainImpl.getInstance()) {
                        processTransactions(lostTransactions, false);
                        lostTransactions.clear();
                    }
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
                    try {
                        processPeerTransactions(transactionsData, false);
                    } catch (NxtException.ValidationException|RuntimeException e) {
                        peer.blacklist(e);
                    }
                } catch (Exception e) {
                    Logger.logDebugMessage("Error processing unconfirmed transactions", e);
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
    public DbIterator<TransactionImpl> getAllUnconfirmedTransactions() {
        return unconfirmedTransactionTable.getAll(0, -1);
    }

    @Override
    public Transaction getUnconfirmedTransaction(Long transactionId) {
        return unconfirmedTransactionTable.get(unconfirmedTransactionDbKeyFactory.newKey(transactionId));
    }

    public Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline,
                                                     Attachment attachment) throws NxtException.ValidationException {
        byte version = (byte) getTransactionVersion(Nxt.getBlockchain().getHeight());
        int timestamp = Convert.getEpochTime();
        TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl(version, senderPublicKey, amountNQT, feeNQT, timestamp,
                deadline, (Attachment.AbstractAttachment)attachment);
        if (version > 0) {
            Block ecBlock = EconomicClustering.getECBlockId(timestamp);
            builder.ecBlockHeight(ecBlock.getHeight());
            builder.ecBlockId(ecBlock.getId());
        }
        return builder;
    }

    @Override
    public void broadcast(Transaction transaction) throws NxtException.ValidationException {
        if (! transaction.verifySignature()) {
            throw new NxtException.NotValidException("Transaction signature verification failed");
        }
        List<Transaction> validTransactions = processTransactions(Collections.singleton((TransactionImpl) transaction), true);
        if (validTransactions.contains(transaction)) {
            nonBroadcastedTransactions.put(transaction.getId(), (TransactionImpl) transaction);
            Logger.logDebugMessage("Accepted new transaction " + transaction.getStringId());
        } else {
            Logger.logDebugMessage("Rejecting double spending transaction " + transaction.getStringId());
            throw new NxtException.NotValidException("Double spending transaction");
        }
    }

    @Override
    public void processPeerTransactions(JSONObject request) throws NxtException.ValidationException {
        JSONArray transactionsData = (JSONArray)request.get("transactions");
        processPeerTransactions(transactionsData, true);
    }

    @Override
    public Transaction parseTransaction(byte[] bytes) throws NxtException.ValidationException {
        return TransactionImpl.parseTransaction(bytes);
    }

    @Override
    public TransactionImpl parseTransaction(JSONObject transactionData) throws NxtException.NotValidException {
        return TransactionImpl.parseTransaction(transactionData);
    }

    void clear() {
        nonBroadcastedTransactions.clear();
    }

    void applyUnconfirmed(Set<TransactionImpl> unapplied) {
        List<Transaction> removedUnconfirmedTransactions = new ArrayList<>();
        for (TransactionImpl transaction : unapplied) {
            if (! transaction.applyUnconfirmed()) {
                unconfirmedTransactionTable.delete(transaction);
                removedUnconfirmedTransactions.add(transaction);
            }
        }
        if (removedUnconfirmedTransactions.size() > 0) {
            transactionListeners.notify(removedUnconfirmedTransactions, TransactionProcessor.Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
        }
    }

    Set<TransactionImpl> undoAllUnconfirmed() {
        Set<TransactionImpl> undone = new HashSet<>();
        try (DbIterator<TransactionImpl> transactions = unconfirmedTransactionTable.getAll(0, -1)) {
            while (transactions.hasNext()) {
                TransactionImpl transaction = transactions.next();
                transaction.undoUnconfirmed();
                undone.add(transaction);
            }
        }
        return undone;
    }

    void updateUnconfirmedTransactions(BlockImpl block) {
        List<Transaction> addedConfirmedTransactions = new ArrayList<>();
        List<Transaction> removedUnconfirmedTransactions = new ArrayList<>();

        for (TransactionImpl transaction : block.getTransactions()) {
            addedConfirmedTransactions.add(transaction);
            TransactionImpl unconfirmedTransaction = unconfirmedTransactionTable.get(transaction.getDbKey());
            if (unconfirmedTransaction != null) {
                unconfirmedTransactionTable.delete(unconfirmedTransaction);
                removedUnconfirmedTransactions.add(unconfirmedTransaction);
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
        synchronized (BlockchainImpl.getInstance()) {
            try {
                Db.beginTransaction();
                for (TransactionImpl transaction : transactions) {
                    if (unconfirmedTransactionTable.get(transaction.getDbKey()) != null) {
                        unconfirmedTransactionTable.delete(transaction);
                        transaction.undoUnconfirmed();
                        removedList.add(transaction);
                    }
                }
                Db.commitTransaction();
            } catch (Exception e) {
                Db.rollbackTransaction();
            } finally {
                Db.endTransaction();
            }
        }
        transactionListeners.notify(removedList, Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
    }

    void shutdown() {
        //removeUnconfirmedTransactions(new ArrayList<>(unconfirmedTransactionTable.getAll()));
    }

    int getTransactionVersion(int previousBlockHeight) {
        return previousBlockHeight < Constants.DIGITAL_GOODS_STORE_BLOCK ? 0 : 1;
    }

    void processLater(Collection<TransactionImpl> transactions) {
        synchronized (BlockchainImpl.getInstance()) {
            lostTransactions.addAll(transactions);
        }
    }

    private void processPeerTransactions(JSONArray transactionsData, final boolean sendToPeers) throws NxtException.ValidationException {
        if (Nxt.getBlockchainProcessor().isDownloading() || Nxt.getBlockchain().getHeight() < Constants.DIGITAL_GOODS_STORE_BLOCK) {
            return;
        }
        List<TransactionImpl> transactions = new ArrayList<>();
        for (Object transactionData : transactionsData) {
            try {
                TransactionImpl transaction = parseTransaction((JSONObject)transactionData);
                try {
                    transaction.validate();
                } catch (NxtException.NotCurrentlyValidException ignore) {}
                transactions.add(transaction);
            } catch (NxtException.NotValidException e) {
                Logger.logDebugMessage("Invalid transaction from peer: " + ((JSONObject) transactionData).toJSONString());
                throw e;
            }
        }
        processTransactions(transactions, sendToPeers);
        for (TransactionImpl transaction : transactions) {
            nonBroadcastedTransactions.remove(transaction.getId());
        }
    }

    List<Transaction> processTransactions(Collection<TransactionImpl> transactions, final boolean sendToPeers) {
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
                if (transaction.getVersion() < 1) {
                    continue;
                }

                synchronized (BlockchainImpl.getInstance()) {
                    try {
                        Db.beginTransaction();
                        if (Nxt.getBlockchain().getHeight() < Constants.NQT_BLOCK) {
                            break; // not ready to process transactions
                        }

                        Long id = transaction.getId();
                        if (TransactionDb.hasTransaction(id) || unconfirmedTransactionTable.get(transaction.getDbKey()) != null) {
                            continue;
                        }

                        if (! transaction.verifySignature()) {
                            if (Account.getAccount(transaction.getSenderId()) != null) {
                                Logger.logDebugMessage("Transaction " + transaction.getJSONObject().toJSONString() + " failed to verify");
                            }
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
                            unconfirmedTransactionTable.insert(transaction);
                            addedUnconfirmedTransactions.add(transaction);
                        } else {
                            addedDoubleSpendingTransactions.add(transaction);
                        }
                        Db.commitTransaction();
                    } catch (Exception e) {
                        Db.rollbackTransaction();
                        throw e;
                    } finally {
                        Db.endTransaction();
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
