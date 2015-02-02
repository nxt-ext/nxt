package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.EntityDbTable;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class TransactionProcessorImpl implements TransactionProcessor {

    private static final boolean enableTransactionRebroadcasting = Nxt.getBooleanProperty("nxt.enableTransactionRebroadcasting");
    private static final boolean testUnconfirmedTransactions = Nxt.getBooleanProperty("nxt.testUnconfirmedTransactions");

    private static final TransactionProcessorImpl instance = new TransactionProcessorImpl();

    static TransactionProcessorImpl getInstance() {
        return instance;
    }

    final DbKey.LongKeyFactory<UnconfirmedTransaction> unconfirmedTransactionDbKeyFactory = new DbKey.LongKeyFactory<UnconfirmedTransaction>("id") {

        @Override
        public DbKey newKey(UnconfirmedTransaction unconfirmedTransaction) {
            return unconfirmedTransaction.getTransaction().getDbKey();
        }

    };

    private final EntityDbTable<UnconfirmedTransaction> unconfirmedTransactionTable =
            new EntityDbTable<UnconfirmedTransaction>("unconfirmed_transaction", unconfirmedTransactionDbKeyFactory) {

        @Override
        protected UnconfirmedTransaction load(Connection con, ResultSet rs) throws SQLException {
            return new UnconfirmedTransaction(rs);
        }

        @Override
        protected void save(Connection con, UnconfirmedTransaction unconfirmedTransaction) throws SQLException {
            unconfirmedTransaction.save(con);
        }

        @Override
        public void rollback(int height) {
            try (Connection con = Db.db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT * FROM unconfirmed_transaction WHERE height > ?")) {
                pstmt.setInt(1, height);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        lostTransactions.add(load(con, rs));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
            super.rollback(height);
            unconfirmedDuplicates.clear();
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY transaction_height ASC, fee_per_byte DESC, arrival_timestamp ASC, id ASC ";
        }

    };

    private final Set<TransactionImpl> broadcastedTransactions = Collections.newSetFromMap(new ConcurrentHashMap<TransactionImpl,Boolean>());
    private final Listeners<List<? extends Transaction>,Event> transactionListeners = new Listeners<>();
    private final Set<UnconfirmedTransaction> lostTransactions = new HashSet<>();
    private final Map<TransactionType, Map<String, Boolean>> unconfirmedDuplicates = new HashMap<>();


    private final Runnable removeUnconfirmedTransactionsThread = new Runnable() {

        private final DbClause expiredClause = new DbClause(" expiration < ? ") {
            @Override
            protected int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setInt(index, Nxt.getEpochTime());
                return index + 1;
            }
        };

        @Override
        public void run() {

            try {
                try {
                    List<UnconfirmedTransaction> expiredTransactions = new ArrayList<>();
                    try (DbIterator<UnconfirmedTransaction> iterator = unconfirmedTransactionTable.getManyBy(expiredClause, 0, -1, "")) {
                        while (iterator.hasNext()) {
                            expiredTransactions.add(iterator.next());
                        }
                    }
                    if (expiredTransactions.size() > 0) {
                        synchronized (BlockchainImpl.getInstance()) {
                            try {
                                Db.db.beginTransaction();
                                for (UnconfirmedTransaction unconfirmedTransaction : expiredTransactions) {
                                    removeUnconfirmedTransaction(unconfirmedTransaction.getTransaction());
                                }
                                Db.db.commitTransaction();
                            } catch (Exception e) {
                                Logger.logErrorMessage(e.toString(), e);
                                Db.db.rollbackTransaction();
                                throw e;
                            } finally {
                                Db.db.endTransaction();
                            }
                        } // synchronized
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
                    int curTime = Nxt.getEpochTime();
                    for (TransactionImpl transaction : broadcastedTransactions) {
                        if (TransactionDb.hasTransaction(transaction.getId()) || transaction.getExpiration() < curTime) {
                            broadcastedTransactions.remove(transaction);
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
                    processLostTransactions();
                    Peer peer = Peers.getAnyPeer(Peer.State.CONNECTED, true);
                    if (peer == null) {
                        return;
                    }
                    JSONObject response = peer.send(getUnconfirmedTransactionsRequest, 10 * 1024 * 1024);
                    if (response == null) {
                        return;
                    }
                    JSONArray transactionsData = (JSONArray)response.get("unconfirmedTransactions");
                    if (transactionsData == null || transactionsData.size() == 0) {
                        return;
                    }
                    try {
                        processPeerTransactions(transactionsData);
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
        ThreadPool.scheduleThread("ProcessTransactions", processTransactionsThread, 5);
        ThreadPool.scheduleThread("RemoveUnconfirmedTransactions", removeUnconfirmedTransactionsThread, 1);
        ThreadPool.runAfterStart(new Runnable() {
            @Override
            public void run() {
                synchronized (BlockchainImpl.getInstance()) {
                    try (DbIterator<UnconfirmedTransaction> oldNonBroadcastedTransactions = getAllUnconfirmedTransactions()) {
                        for (UnconfirmedTransaction unconfirmedTransaction : oldNonBroadcastedTransactions) {
                            if (unconfirmedTransaction.getTransaction().isUnconfirmedDuplicate(unconfirmedDuplicates)) {
                                Logger.logDebugMessage("Skipping duplicate unconfirmed transaction " + unconfirmedTransaction.getTransaction().getJSONObject().toString());
                            } else if (enableTransactionRebroadcasting) {
                                broadcastedTransactions.add(unconfirmedTransaction.getTransaction());
                            }
                        }
                    }
                }
            }
        });
        if (enableTransactionRebroadcasting) {
            ThreadPool.scheduleThread("RebroadcastTransactions", rebroadcastTransactionsThread, 60);
        }
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
    public DbIterator<UnconfirmedTransaction> getAllUnconfirmedTransactions() {
        return unconfirmedTransactionTable.getAll(0, -1);
    }

    @Override
    public Transaction getUnconfirmedTransaction(long transactionId) {
        return unconfirmedTransactionTable.get(unconfirmedTransactionDbKeyFactory.newKey(transactionId));
    }

    @Override
    public void broadcast(Transaction transaction) throws NxtException.ValidationException {
        synchronized (BlockchainImpl.getInstance()) {
            if (TransactionDb.hasTransaction(transaction.getId())) {
                Logger.logMessage("Transaction " + transaction.getStringId() + " already in blockchain, will not broadcast again");
                return;
            }
            if (unconfirmedTransactionTable.get(((TransactionImpl) transaction).getDbKey()) != null) {
                if (enableTransactionRebroadcasting) {
                    broadcastedTransactions.add((TransactionImpl) transaction);
                    Logger.logMessage("Transaction " + transaction.getStringId() + " already in unconfirmed pool, will re-broadcast");
                } else {
                    Logger.logMessage("Transaction " + transaction.getStringId() + " already in unconfirmed pool, will not broadcast again");
                }
                return;
            }
            transaction.validate();
            processTransaction(new UnconfirmedTransaction((TransactionImpl) transaction, System.currentTimeMillis()));
            Logger.logDebugMessage("Accepted new transaction " + transaction.getStringId());
            List<Transaction> acceptedTransactions = Collections.singletonList(transaction);
            Peers.sendToSomePeers(acceptedTransactions);
            transactionListeners.notify(acceptedTransactions, Event.ADDED_UNCONFIRMED_TRANSACTIONS);
            if (enableTransactionRebroadcasting) {
                broadcastedTransactions.add((TransactionImpl) transaction);
            }
        } // synchronized
    }

    @Override
    public void processPeerTransactions(JSONObject request) throws NxtException.ValidationException {
        JSONArray transactionsData = (JSONArray)request.get("transactions");
        processPeerTransactions(transactionsData);
    }

    @Override
    public Transaction parseTransaction(byte[] bytes) throws NxtException.ValidationException {
        return TransactionImpl.parseTransaction(bytes);
    }

    @Override
    public TransactionImpl parseTransaction(JSONObject transactionData) throws NxtException.NotValidException {
        return TransactionImpl.parseTransaction(transactionData);
    }

    @Override
    public void clearUnconfirmedTransactions() {
        synchronized (BlockchainImpl.getInstance()) {
            List<Transaction> removed = new ArrayList<>();
            try {
                Db.db.beginTransaction();
                try (DbIterator<UnconfirmedTransaction> unconfirmedTransactions = getAllUnconfirmedTransactions()) {
                    for (UnconfirmedTransaction unconfirmedTransaction : unconfirmedTransactions) {
                        unconfirmedTransaction.getTransaction().undoUnconfirmed();
                        removed.add(unconfirmedTransaction.getTransaction());
                    }
                }
                unconfirmedTransactionTable.truncate();
                Db.db.commitTransaction();
            } catch (Exception e) {
                Logger.logErrorMessage(e.toString(), e);
                Db.db.rollbackTransaction();
                throw e;
            } finally {
                Db.db.endTransaction();
            }
            unconfirmedDuplicates.clear();
            transactionListeners.notify(removed, Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
        }
    }

    void requeueAllUnconfirmedTransactions() {
        List<Transaction> removed = new ArrayList<>();
        try (DbIterator<UnconfirmedTransaction> unconfirmedTransactions = getAllUnconfirmedTransactions()) {
            for (UnconfirmedTransaction unconfirmedTransaction : unconfirmedTransactions) {
                unconfirmedTransaction.getTransaction().undoUnconfirmed();
                removed.add(unconfirmedTransaction.getTransaction());
                lostTransactions.add(unconfirmedTransaction);
            }
        }
        unconfirmedTransactionTable.truncate();
        unconfirmedDuplicates.clear();
        transactionListeners.notify(removed, Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
    }

    void removeUnconfirmedTransaction(TransactionImpl transaction) {
        if (!Db.db.isInTransaction()) {
            try {
                Db.db.beginTransaction();
                removeUnconfirmedTransaction(transaction);
                Db.db.commitTransaction();
            } catch (Exception e) {
                Logger.logErrorMessage(e.toString(), e);
                Db.db.rollbackTransaction();
                throw e;
            } finally {
                Db.db.endTransaction();
            }
            return;
        }
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM unconfirmed_transaction WHERE id = ?")) {
            pstmt.setLong(1, transaction.getId());
            int deleted = pstmt.executeUpdate();
            if (deleted > 0) {
                transaction.undoUnconfirmed();
                transactionListeners.notify(Collections.singletonList(transaction), Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
            }
        } catch (SQLException e) {
            Logger.logErrorMessage(e.toString(), e);
            throw new RuntimeException(e.toString(), e);
        }
    }

    int getTransactionVersion(int previousBlockHeight) {
        return previousBlockHeight < Constants.DIGITAL_GOODS_STORE_BLOCK ? 0 : 1;
    }

    void processLater(Collection<TransactionImpl> transactions) {
        long currentTime = System.currentTimeMillis();
        synchronized (BlockchainImpl.getInstance()) {
            for (TransactionImpl transaction : transactions) {
                transaction.unsetBlock();
                lostTransactions.add(new UnconfirmedTransaction(transaction, Math.min(currentTime, Convert.fromEpochTime(transaction.getTimestamp()))));
            }
        }
    }

    private void processLostTransactions() {
        synchronized (BlockchainImpl.getInstance()) {
            if (lostTransactions.size() > 0) {
                List<Transaction> addedUnconfirmedTransactions = new ArrayList<>();
                Iterator<UnconfirmedTransaction> iterator = lostTransactions.iterator();
                while (iterator.hasNext()) {
                    UnconfirmedTransaction unconfirmedTransaction = iterator.next();
                    try {
                        processTransaction(unconfirmedTransaction);
                        iterator.remove();
                        addedUnconfirmedTransactions.add(unconfirmedTransaction.getTransaction());
                    } catch (NxtException.NotCurrentlyValidException ignore) {
                        if (unconfirmedTransaction.getExpiration() < Nxt.getEpochTime()) {
                            iterator.remove();
                        }
                    } catch (NxtException.ValidationException|RuntimeException e) {
                        iterator.remove();
                    }
                }
                if (addedUnconfirmedTransactions.size() > 0) {
                    transactionListeners.notify(addedUnconfirmedTransactions, Event.ADDED_UNCONFIRMED_TRANSACTIONS);
                }
            }
        }
    }

    private void processPeerTransactions(JSONArray transactionsData) throws NxtException.NotValidException {
        if (Nxt.getBlockchain().getLastBlock().getTimestamp() < Nxt.getEpochTime() - 60 * 1440 && ! testUnconfirmedTransactions) {
            return;
        }
        if (Nxt.getBlockchain().getHeight() <= Constants.NQT_BLOCK) {
            return;
        }
        if (transactionsData == null || transactionsData.isEmpty()) {
            return;
        }
        long arrivalTimestamp = System.currentTimeMillis();
        List<TransactionImpl> receivedTransactions = new ArrayList<>();
        List<TransactionImpl> sendToPeersTransactions = new ArrayList<>();
        List<TransactionImpl> addedUnconfirmedTransactions = new ArrayList<>();
        boolean invalidTransactionsFound = false;
        for (Object transactionData : transactionsData) {
            try {
                TransactionImpl transaction = parseTransaction((JSONObject) transactionData);
                receivedTransactions.add(transaction);
                if (TransactionDb.hasTransaction(transaction.getId()) || unconfirmedTransactionTable.get(transaction.getDbKey()) != null) {
                    continue;
                }
                transaction.validate();
                UnconfirmedTransaction unconfirmedTransaction = new UnconfirmedTransaction(transaction, arrivalTimestamp);
                processTransaction(unconfirmedTransaction);
                if (broadcastedTransactions.contains(transaction)) {
                    Logger.logDebugMessage("Received back transaction " + transaction.getStringId()
                            + " that we broadcasted, will not forward again to peers");
                } else {
                    sendToPeersTransactions.add(transaction);
                }
                addedUnconfirmedTransactions.add(transaction);

            } catch (NxtException.NotCurrentlyValidException ignore) {
            } catch (NxtException.ValidationException|RuntimeException e) {
                Logger.logDebugMessage(String.format("Invalid transaction from peer: %s", ((JSONObject) transactionData).toJSONString()), e);
                invalidTransactionsFound = true;
            }
        }
        if (sendToPeersTransactions.size() > 0) {
            Peers.sendToSomePeers(sendToPeersTransactions);
        }
        if (addedUnconfirmedTransactions.size() > 0) {
            transactionListeners.notify(addedUnconfirmedTransactions, Event.ADDED_UNCONFIRMED_TRANSACTIONS);
        }
        for (TransactionImpl transaction : receivedTransactions) {
            broadcastedTransactions.remove(transaction);
        }
        if (invalidTransactionsFound) {
            throw new NxtException.NotValidException("Peer sends invalid transactions");
        }
    }

    private void processTransaction(UnconfirmedTransaction unconfirmedTransaction) throws NxtException.ValidationException {
        TransactionImpl transaction = unconfirmedTransaction.getTransaction();
        int curTime = Nxt.getEpochTime();
        if (transaction.getTimestamp() > curTime + Constants.MAX_TIMEDRIFT || transaction.getDeadline() > 1440 || transaction.getExpiration() < curTime) {
            throw new NxtException.NotCurrentlyValidException("Invalid transaction timestamp");
        }
        if (transaction.getVersion() < 1) {
            throw new NxtException.NotValidException("Invalid transaction version");
        }

        synchronized (BlockchainImpl.getInstance()) {
            try {
                Db.db.beginTransaction();
                if (Nxt.getBlockchain().getHeight() < Constants.NQT_BLOCK) {
                    throw new NxtException.NotCurrentlyValidException("Blockchain not ready to accept transactions");
                }

                if (TransactionDb.hasTransaction(transaction.getId()) || unconfirmedTransactionTable.get(transaction.getDbKey()) != null) {
                    throw new NxtException.NotCurrentlyValidException("Transaction already processed");
                }

                if (! transaction.verifySignature()) {
                    if (Account.getAccount(transaction.getSenderId()) != null) {
                        throw new NxtException.NotValidException("Transaction signature verification failed");
                    } else {
                        throw new NxtException.NotCurrentlyValidException("Unknown transaction sender");
                    }
                }

                if (! transaction.applyUnconfirmed()) {
                    throw new NxtException.NotCurrentlyValidException("Double spending or insufficient balance");
                }

                if (transaction.isUnconfirmedDuplicate(unconfirmedDuplicates)) {
                    throw new NxtException.NotCurrentlyValidException("Duplicate unconfirmed transaction");
                }

                unconfirmedTransactionTable.insert(unconfirmedTransaction);

                Db.db.commitTransaction();
            } catch (Exception e) {
                Db.db.rollbackTransaction();
                throw e;
            } finally {
                Db.db.endTransaction();
            }
        } // synchronized
    }

}
