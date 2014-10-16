package nxt;

import nxt.db.*;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class TransactionProcessorImpl implements TransactionProcessor {

    private static final TransactionProcessorImpl instance = new TransactionProcessorImpl();

    static TransactionProcessorImpl getInstance() {
        return instance;
    }

    static {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                int height = block.getHeight();
                if(height >= Constants.TWO_PHASED_TRANSACTIONS_BLOCK){
                    DbIterator<Long> txIdsToRefuse =
                            PhasedTransactionPoll.pendingTransactionsTable.finishing(height);
                    for(Long txId:txIdsToRefuse){
                        Transaction tx = TransactionDb.findTransaction(txId);
                        try {
                            tx.refuse();
                        } catch (NxtException.NotValidException e) {
                            e.printStackTrace();  // todo:  better handling?
                        }
                    }
                }
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);     //todo: rescans, popoffs?
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
                TransactionImpl transaction = TransactionImpl.parseTransaction(transactionBytes);
                transaction.setHeight(rs.getInt("transaction_height"));
                return transaction;
            } catch (NxtException.ValidationException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        @Override
        protected void save(Connection con, TransactionImpl transaction) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO unconfirmed_transaction (id, transaction_height, "
                    + "fee_per_byte, timestamp, expiration, transaction_bytes, height, latest) "
                    + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, transaction.getId());
                pstmt.setInt(++i, transaction.getHeight());
                pstmt.setLong(++i, transaction.getFeeNQT() / transaction.getSize());
                pstmt.setInt(++i, transaction.getTimestamp());
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
                 PreparedStatement pstmt = con.prepareStatement("SELECT * FROM unconfirmed_transaction WHERE height > ? AND latest = TRUE")) {
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
            processLater(transactions);
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY transaction_height ASC, fee_per_byte DESC, timestamp ASC, id ASC ";
        }
    };

    private final ConcurrentMap<Long, TransactionImpl> nonBroadcastedTransactions = new ConcurrentHashMap<>();
    private final Listeners<List<? extends Transaction>,Event> transactionListeners = new Listeners<>();
    private final Set<TransactionImpl> lostTransactions = new HashSet<>();

    private final Runnable removeUnconfirmedTransactionsThread = new Runnable() {

        @Override
        public void run() {

            try {
                try {
                    synchronized (BlockchainImpl.getInstance()) {
                        try {
                            Db.beginTransaction();
                            try (Connection con = Db.getConnection();
                                 PreparedStatement pstmt = con.prepareStatement("SELECT * FROM unconfirmed_transaction WHERE expiration < ? AND latest = TRUE")) {
                                pstmt.setInt(1, Convert.getEpochTime());
                                try (DbIterator<TransactionImpl> iterator = unconfirmedTransactionTable.getManyBy(con, pstmt, true)) {
                                    for (TransactionImpl transaction : iterator) {
                                        unconfirmedTransactionTable.delete(transaction);
                                        transaction.undoUnconfirmed();
                                    }
                                }
                            }
                            Db.commitTransaction();
                        } catch (Exception e) {
                            Db.rollbackTransaction();
                            throw e;
                        } finally {
                            Db.endTransaction();
                        }
                    } // synchronized
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
    public boolean addListener(Listener<List<? extends Transaction>> listener, Event eventType) {
        return transactionListeners.addListener(listener, eventType);
    }

    @Override
    public boolean removeListener(Listener<List<? extends Transaction>> listener, Event eventType) {
        return transactionListeners.removeListener(listener, eventType);
    }

    void notifyListeners(List<? extends Transaction> transactions, Event eventType) {
        transactionListeners.notify(transactions, eventType);
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

    void removeUnconfirmedTransactions(Iterable<TransactionImpl> transactions, boolean processLater) {
        synchronized (BlockchainImpl.getInstance()) {
            if (!Db.isInTransaction()) {
                try {
                    Db.beginTransaction();
                    removeUnconfirmedTransactions(transactions, processLater);
                    Db.commitTransaction();
                } catch (Exception e) {
                    Logger.logErrorMessage(e.toString(), e);
                    Db.rollbackTransaction();
                    throw e;
                } finally {
                    Db.endTransaction();
                }
                return;
            }
            List<Transaction> removed = new ArrayList<>();
            for (TransactionImpl transaction : transactions) {
                if (unconfirmedTransactionTable.delete(transaction)) {
                    transaction.undoUnconfirmed();
                    removed.add(transaction);
                }
                if (processLater) {
                    lostTransactions.add(transaction);
                }
            }
            transactionListeners.notify(removed, Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
        } // synchronized
    }

    void shutdown() {
        try (DbIterator<TransactionImpl> transactions = unconfirmedTransactionTable.getAll(0, -1)) {
            removeUnconfirmedTransactions(transactions, false);
        }
    }

    int getTransactionVersion(int previousBlockHeight) {
        return previousBlockHeight < Constants.DIGITAL_GOODS_STORE_BLOCK ? 0 : 1;
    }

    void processLater(Collection<TransactionImpl> transactions) {
        synchronized (BlockchainImpl.getInstance()) {
            for (TransactionImpl transaction : transactions) {
                lostTransactions.add(transaction);
            }
        }
    }

    private void processPeerTransactions(JSONArray transactionsData, final boolean sendToPeers) throws NxtException.ValidationException {
        if (Nxt.getBlockchainProcessor().isDownloading() || Nxt.getBlockchain().getHeight() < Constants.DIGITAL_GOODS_STORE_BLOCK) {
            return;
        }
        List<TransactionImpl> transactions = new ArrayList<>();
        for (Object transactionData : transactionsData) {
            try {
                TransactionImpl transaction = parseTransaction((JSONObject) transactionData);
                transaction.validate();
                transactions.add(transaction);
            } catch (NxtException.NotCurrentlyValidException ignore) {
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
