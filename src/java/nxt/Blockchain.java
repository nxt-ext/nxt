package nxt;

import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.util.Convert;
import nxt.util.DbIterator;
import nxt.util.DbUtils;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class Blockchain {

    public static enum Event {
        BLOCK_PUSHED, BLOCK_POPPED,
        REMOVED_UNCONFIRMED_TRANSACTIONS,
        ADDED_UNCONFIRMED_TRANSACTIONS,
        ADDED_CONFIRMED_TRANSACTIONS,
        ADDED_DOUBLESPENDING_TRANSACTIONS
    }

    private static final Listeners<List<Block>,Event> blockListeners = new Listeners<>();
    private static final Listeners<List<Transaction>,Event> transactionListeners = new Listeners<>();

    private static final byte[] CHECKSUM_TRANSPARENT_FORGING = new byte[]{27, -54, -59, -98, 49, -42, 48, -68, -112, 49, 41, 94, -41, 78, -84, 27, -87, -22, -28, 36, -34, -90, 112, -50, -9, 5, 89, -35, 80, -121, -128, 112};

    private static volatile Peer lastBlockchainFeeder;

    private static final AtomicInteger blockCounter = new AtomicInteger();
    private static final AtomicReference<Block> lastBlock = new AtomicReference<>();

    private static final AtomicInteger transactionCounter = new AtomicInteger();
    private static final ConcurrentMap<Long, Transaction> doubleSpendingTransactions = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, Transaction> unconfirmedTransactions = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, Transaction> nonBroadcastedTransactions = new ConcurrentHashMap<>();

    private static final Collection<Transaction> allUnconfirmedTransactions = Collections.unmodifiableCollection(unconfirmedTransactions.values());

    static final ConcurrentMap<String, Transaction> transactionHashes = new ConcurrentHashMap<>();

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
                    Peer peer = Peer.getAnyPeer(Peer.State.CONNECTED, true);
                    if (peer == null) {
                        return;
                    }
                    JSONObject response = peer.send(getUnconfirmedTransactionsRequest);
                    if (response == null) {
                        return;
                    }
                    try {
                        JSONArray transactionsData = (JSONArray)response.get("unconfirmedTransactions");
                        processTransactions(transactionsData, false);
                    } catch (NxtException.ValidationException e) {
                        peer.blacklist(e);
                    }
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

    static final Runnable removeUnconfirmedTransactionsThread = new Runnable() {

        @Override
        public void run() {

            try {
                try {

                    int curTime = Convert.getEpochTime();
                    List<Transaction> removedUnconfirmedTransactions = new ArrayList<>();

                    Iterator<Transaction> iterator = unconfirmedTransactions.values().iterator();
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

    static final Runnable getMoreBlocksThread = new Runnable() {

        private final JSONStreamAware getCumulativeDifficultyRequest;
        {
            JSONObject request = new JSONObject();
            request.put("requestType", "getCumulativeDifficulty");
            getCumulativeDifficultyRequest = JSON.prepareRequest(request);
        }

        @Override
        public void run() {

            try {
                try {
                    Peer peer = Peer.getAnyPeer(Peer.State.CONNECTED, true);
                    if (peer == null) {
                        return;
                    }
                    lastBlockchainFeeder = peer;
                    JSONObject response = peer.send(getCumulativeDifficultyRequest);
                    if (response == null) {
                        return;
                    }
                    BigInteger curCumulativeDifficulty = lastBlock.get().getCumulativeDifficulty();
                    String peerCumulativeDifficulty = (String)response.get("cumulativeDifficulty");
                    if (peerCumulativeDifficulty == null) {
                        return;
                    }
                    BigInteger betterCumulativeDifficulty = new BigInteger(peerCumulativeDifficulty);
                    if (betterCumulativeDifficulty.compareTo(curCumulativeDifficulty) <= 0) {
                        return;
                    }

                    Long commonBlockId = Genesis.GENESIS_BLOCK_ID;

                    if (! getLastBlock().getId().equals(Genesis.GENESIS_BLOCK_ID)) {
                        commonBlockId = getCommonMilestoneBlockId(peer);
                    }
                    if (commonBlockId == null) {
                        return;
                    }

                    commonBlockId = getCommonBlockId(peer, commonBlockId);
                    if (commonBlockId == null) {
                        return;
                    }

                    final Block commonBlock = Block.findBlock(commonBlockId);
                    if (lastBlock.get().getHeight() - commonBlock.getHeight() >= 720) {
                        return;
                    }

                    Long curBlockId = commonBlockId;
                    List<Block> futureBlocks = new LinkedList<>();

                    while (true) {

                        JSONArray nextBlocks = getNextBlocks(peer, curBlockId);
                        if (nextBlocks == null || nextBlocks.size() == 0) {
                            break;
                        }

                        synchronized (Blockchain.class) {

                            for (Object o : nextBlocks) {
                                JSONObject blockData = (JSONObject)o;
                                Block block;
                                try {
                                    block = Block.getBlock(blockData);
                                } catch (NxtException.ValidationException e) {
                                    peer.blacklist(e);
                                    return;
                                }
                                curBlockId = block.getId();

                                if (lastBlock.get().getId().equals(block.getPreviousBlockId())) {

                                    JSONArray transactionData = (JSONArray)blockData.get("transactions");
                                    try {
                                        Transaction[] blockTransactions = new Transaction[transactionData.size()];
                                        for (int j = 0; j < blockTransactions.length; j++) {
                                            blockTransactions[j] = Transaction.getTransaction((JSONObject)transactionData.get(j));
                                        }
                                        try {
                                            Blockchain.pushBlock(block, blockTransactions);
                                        } catch (BlockNotAcceptedException e) {
                                            Logger.logDebugMessage("Failed to accept block " + block.getStringId()
                                                    + " at height " + lastBlock.get().getHeight()
                                                    + " received from " + peer.getPeerAddress() + ", blacklisting");
                                            peer.blacklist(e);
                                            return;
                                        }
                                    } catch (NxtException.ValidationException e) {
                                        peer.blacklist(e);
                                        return;
                                    }

                                } else if (! Block.hasBlock(block.getId()) && block.transactionIds.length <= Nxt.MAX_NUMBER_OF_TRANSACTIONS) {

                                    JSONArray transactionsData = (JSONArray)blockData.get("transactions");
                                    try {
                                        for (int j = 0; j < block.transactionIds.length; j++) {

                                            Transaction transaction = Transaction.getTransaction((JSONObject)transactionsData.get(j));
                                            block.transactionIds[j] = transaction.getId();
                                            block.blockTransactions[j] = transaction;

                                        }
                                    } catch (NxtException.ValidationException e) {
                                        peer.blacklist(e);
                                        return;
                                    }

                                    futureBlocks.add(block);

                                }

                            }

                        } //synchronized

                    }

                    if (futureBlocks.isEmpty() || lastBlock.get().getHeight() - commonBlock.getHeight() >= 720) {
                        return;
                    }

                    processFutureBlocks(peer, futureBlocks, commonBlock);

                } catch (Exception e) {
                    Logger.logDebugMessage("Error in milestone blocks processing thread", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

        private Long getCommonMilestoneBlockId(Peer peer) {

            String lastMilestoneBlockId = null;

            while (true) {
                JSONObject milestoneBlockIdsRequest = new JSONObject();
                milestoneBlockIdsRequest.put("requestType", "getMilestoneBlockIds");
                if (lastMilestoneBlockId != null) {
                    milestoneBlockIdsRequest.put("lastMilestoneBlockId", lastMilestoneBlockId);
                } else {
                    milestoneBlockIdsRequest.put("lastBlockId", Blockchain.getLastBlock().getStringId());
                }

                JSONObject response = peer.send(JSON.prepareRequest(milestoneBlockIdsRequest));
                if (response == null) {
                    return null;
                }
                JSONArray milestoneBlockIds = (JSONArray)response.get("milestoneBlockIds");
                if (milestoneBlockIds == null) {
                    return null;
                }
                if (milestoneBlockIds.isEmpty()) {
                    return Genesis.GENESIS_BLOCK_ID;
                }
                /* prevent overloading with blockIds
                if (milestoneBlockIds.size() > 20) {
                    Logger.logDebugMessage("Obsolete or rogue peer " + peer.getPeerAddress() + " sends too many milestoneBlockIds, blacklisting");
                    peer.blacklist();
                    return null;
                }
                */
                for (Object milestoneBlockId : milestoneBlockIds) {
                    lastMilestoneBlockId = (String) milestoneBlockId;
                    Long blockId = Convert.parseUnsignedLong(lastMilestoneBlockId);
                    if (Block.hasBlock(blockId)) {
                        return blockId;
                    }
                }
            }

        }

        private Long getCommonBlockId(Peer peer, Long commonBlockId) {

            while (true) {
                JSONObject request = new JSONObject();
                request.put("requestType", "getNextBlockIds");
                request.put("blockId", Convert.convert(commonBlockId));
                JSONObject response = peer.send(JSON.prepareRequest(request));
                if (response == null) {
                    return null;
                }
                JSONArray nextBlockIds = (JSONArray)response.get("nextBlockIds");
                if (nextBlockIds == null || nextBlockIds.size() == 0) {
                    return null;
                }
                /*
                if (nextBlockIds.size() > 1440) {
                    Logger.logDebugMessage("Obsolete or rogue peer " + peer.getPeerAddress() + " sends too many nextBlockIds, blacklisting");
                    peer.blacklist();
                    return null;
                }
                */
                for (Object nextBlockId : nextBlockIds) {
                    Long blockId = Convert.parseUnsignedLong((String) nextBlockId);
                    if (! Block.hasBlock(blockId)) {
                        return commonBlockId;
                    }
                    commonBlockId = blockId;
                }
            }

        }

        private JSONArray getNextBlocks(Peer peer, Long curBlockId) {

            JSONObject request = new JSONObject();
            request.put("requestType", "getNextBlocks");
            request.put("blockId", Convert.convert(curBlockId));
            JSONObject response = peer.send(JSON.prepareRequest(request));
            if (response == null) {
                return null;
            }

            JSONArray nextBlocks = (JSONArray)response.get("nextBlocks");
            /*
            if (nextBlocks.size() > 1440) {
                Logger.logDebugMessage("Obsolete or rogue peer " + peer.getPeerAddress() + " sends too many nextBlocks, blacklisting");
                peer.blacklist();
                return null;
            }
            */
            return nextBlocks;

        }

        private void processFutureBlocks(Peer peer, final List<Block> futureBlocks, final Block commonBlock) {

            synchronized (Blockchain.class) {
                BigInteger curCumulativeDifficulty = lastBlock.get().getCumulativeDifficulty();
                boolean needsRescan;

                try {
                    while (!lastBlock.get().getId().equals(commonBlock.getId()) && Blockchain.popLastBlock()) {}

                    if (lastBlock.get().getId().equals(commonBlock.getId())) {
                        for (Block block : futureBlocks) {
                            if (lastBlock.get().getId().equals(block.getPreviousBlockId())) {
                                try {
                                    Blockchain.pushBlock(block, block.blockTransactions);
                                } catch (BlockNotAcceptedException e) {
                                    Logger.logDebugMessage("Failed to push future block " + block.getStringId()
                                            + " received from " + peer.getPeerAddress() + ", blacklisting");
                                    peer.blacklist(e);
                                    break;
                                }
                            }
                        }
                    }

                    needsRescan = lastBlock.get().getCumulativeDifficulty().compareTo(curCumulativeDifficulty) < 0;
                    if (needsRescan) {
                        Logger.logDebugMessage("Rescan caused by peer " + peer.getPeerAddress()+ ", blacklisting");
                        peer.blacklist();
                    }
                } catch (Transaction.UndoNotSupportedException e) {
                    Logger.logDebugMessage(e.getMessage());
                    Logger.logDebugMessage("Popping off last block not possible, will do a rescan");
                    needsRescan = true;
                }

                if (needsRescan) {
                    // this relies on the database cascade trigger to delete all blocks after commonBlock
                    if (commonBlock.getNextBlockId() != null) {
                        Block.deleteBlock(commonBlock.getNextBlockId());
                    }
                    Logger.logMessage("Re-scanning blockchain...");
                    Blockchain.scan();
                    Logger.logMessage("...Done");
                }
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
                        if (unconfirmedTransactions.get(transaction.getId()) == null && ! Transaction.hasTransaction(transaction.getId())) {
                            transactionsData.add(transaction.getJSONObject());
                        } else {
                            nonBroadcastedTransactions.remove(transaction.getId());
                        }
                    }

                    if (transactionsData.size() > 0) {
                        JSONObject peerRequest = new JSONObject();
                        peerRequest.put("requestType", "processTransactions");
                        peerRequest.put("transactions", transactionsData);
                        Peer.sendToSomePeers(peerRequest);
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

    public static boolean addTransactionListener(Listener<List<Transaction>> listener, Event eventType) {
        return transactionListeners.addListener(listener, eventType);
    }

    public static boolean removeTransactionListener(Listener<List<Transaction>> listener, Event eventType) {
        return transactionListeners.removeListener(listener, eventType);
    }

    public static boolean addBlockListener(Listener<List<Block>> listener, Event eventType) {
        return blockListeners.addListener(listener, eventType);
    }

    public static boolean removeBlockListener(Listener<List<Block>> listener, Event eventType) {
        return blockListeners.removeListener(listener, eventType);
    }

    public static DbIterator<Block> getAllBlocks() {
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block ORDER BY db_id ASC");
            return new DbIterator<>(con, pstmt, new DbIterator.ResultSetReader<Block>() {
                @Override
                public Block get(Connection con, ResultSet rs) throws NxtException.ValidationException {
                    return Block.getBlock(con, rs);
                }
            });
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<Block> getAllBlocks(Account account, int timestamp) {
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE timestamp >= ? AND generator_id = ? ORDER BY db_id ASC");
            pstmt.setInt(1, timestamp);
            pstmt.setLong(2, account.getId());
            return new DbIterator<>(con, pstmt, new DbIterator.ResultSetReader<Block>() {
                @Override
                public Block get(Connection con, ResultSet rs) throws NxtException.ValidationException {
                    return Block.getBlock(con, rs);
                }
            });
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static int getBlockCount() {
        try (Connection con = Db.getConnection(); PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM block")) {
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<Transaction> getAllTransactions() {
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction ORDER BY db_id ASC");
            return new DbIterator<>(con, pstmt, new DbIterator.ResultSetReader<Transaction>() {
                @Override
                public Transaction get(Connection con, ResultSet rs) throws NxtException.ValidationException {
                    return Transaction.getTransaction(con, rs);
                }
            });
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<Transaction> getAllTransactions(Account account, byte type, byte subtype, int timestamp) {
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt;
            if (type >= 0) {
                if (subtype >= 0) {
                    pstmt = con.prepareStatement("SELECT * FROM transaction WHERE timestamp >= ? AND (recipient_id = ? OR sender_id = ?) AND type = ? AND subtype = ? ORDER BY timestamp ASC");
                    pstmt.setInt(1, timestamp);
                    pstmt.setLong(2, account.getId());
                    pstmt.setLong(3, account.getId());
                    pstmt.setByte(4, type);
                    pstmt.setByte(5, subtype);
                } else {
                    pstmt = con.prepareStatement("SELECT * FROM transaction WHERE timestamp >= ? AND (recipient_id = ? OR sender_id = ?) AND type = ? ORDER BY timestamp ASC");
                    pstmt.setInt(1, timestamp);
                    pstmt.setLong(2, account.getId());
                    pstmt.setLong(3, account.getId());
                    pstmt.setByte(4, type);
                }
            } else {
                pstmt = con.prepareStatement("SELECT * FROM transaction WHERE timestamp >= ? AND (recipient_id = ? OR sender_id = ?) ORDER BY timestamp ASC");
                pstmt.setInt(1, timestamp);
                pstmt.setLong(2, account.getId());
                pstmt.setLong(3, account.getId());
            }
            return new DbIterator<>(con, pstmt, new DbIterator.ResultSetReader<Transaction>() {
                @Override
                public Transaction get(Connection con, ResultSet rs) throws NxtException.ValidationException {
                    return Transaction.getTransaction(con, rs);
                }
            });
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static int getTransactionCount() {
        try (Connection con = Db.getConnection(); PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM transaction")) {
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static List<Long> getBlockIdsAfter(Long blockId, int limit) {
        if (limit > 1440) {
            throw new IllegalArgumentException("Can't get more than 1440 blocks at a time");
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt1 = con.prepareStatement("SELECT db_id FROM block WHERE id = ?");
             PreparedStatement pstmt2 = con.prepareStatement("SELECT id FROM block WHERE db_id > ? ORDER BY db_id ASC LIMIT ?")) {
            pstmt1.setLong(1, blockId);
            ResultSet rs = pstmt1.executeQuery();
            if (! rs.next()) {
                rs.close();
                return Collections.emptyList();
            }
            List<Long> result = new ArrayList<>();
            int dbId = rs.getInt("db_id");
            pstmt2.setInt(1, dbId);
            pstmt2.setInt(2, limit);
            rs = pstmt2.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong("id"));
            }
            rs.close();
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static List<Block> getBlocksAfter(Long blockId, int limit) {
        if (limit > 1440) {
            throw new IllegalArgumentException("Can't get more than 1440 blocks at a time");
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt1 = con.prepareStatement("SELECT db_id FROM block WHERE id = ?");
             PreparedStatement pstmt2 = con.prepareStatement("SELECT * FROM block WHERE db_id > ? ORDER BY db_id ASC LIMIT ?")) {
            pstmt1.setLong(1, blockId);
            ResultSet rs = pstmt1.executeQuery();
            if (! rs.next()) {
                rs.close();
                return Collections.emptyList();
            }
            List<Block> result = new ArrayList<>();
            int dbId = rs.getInt("db_id");
            pstmt2.setInt(1, dbId);
            pstmt2.setInt(2, limit);
            rs = pstmt2.executeQuery();
            while (rs.next()) {
                result.add(Block.getBlock(con, rs));
            }
            rs.close();
            return result;
        } catch (NxtException.ValidationException|SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static long getBlockIdAtHeight(int height) {
        Block block = lastBlock.get();
        if (height > block.getHeight()) {
            throw new IllegalArgumentException("Invalid height " + height + ", current blockchain is at " + block.getHeight());
        }
        if (height == block.getHeight()) {
            return block.getId();
        }
        return Block.findBlockIdAtHeight(height);
    }

    public static List<Block> getBlocksFromHeight(int height) {
        if (height < 0 || lastBlock.get().getHeight() - height > 1440) {
            throw new IllegalArgumentException("Can't go back more than 1440 blocks");
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE height >= ? ORDER BY height ASC")) {
            pstmt.setInt(1, height);
            ResultSet rs = pstmt.executeQuery();
            List<Block> result = new ArrayList<>();
            while (rs.next()) {
                result.add(Block.getBlock(con, rs));
            }
            return result;
        } catch (SQLException|NxtException.ValidationException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static Collection<Transaction> getAllUnconfirmedTransactions() {
        return allUnconfirmedTransactions;
    }

    public static Block getLastBlock() {
        return lastBlock.get();
    }

    public static Block getBlock(Long blockId) {
        return Block.findBlock(blockId);
    }

    public static boolean hasBlock(Long blockId) {
        return Block.hasBlock(blockId);
    }

    public static Transaction getTransaction(Long transactionId) {
        return Transaction.findTransaction(transactionId);
    }

    public static Transaction getUnconfirmedTransaction(Long transactionId) {
        return unconfirmedTransactions.get(transactionId);
    }

    public static void broadcast(Transaction transaction) {

        JSONObject peerRequest = new JSONObject();
        peerRequest.put("requestType", "processTransactions");
        JSONArray transactionsData = new JSONArray();
        transactionsData.add(transaction.getJSONObject());
        peerRequest.put("transactions", transactionsData);

        nonBroadcastedTransactions.put(transaction.getId(), transaction);

        Peer.sendToSomePeers(peerRequest);
        Logger.logDebugMessage("Broadcasted new transaction " + transaction.getStringId());

    }

    public static Peer getLastBlockchainFeeder() {
        return lastBlockchainFeeder;
    }

    public static void processTransactions(JSONObject request) throws NxtException.ValidationException {
        JSONArray transactionsData = (JSONArray)request.get("transactions");
        processTransactions(transactionsData, true);
    }

    public static boolean pushBlock(JSONObject request) throws NxtException {

        Block block = Block.getBlock(request);
        if (!lastBlock.get().getId().equals(block.getPreviousBlockId())) {
            // do this check first to avoid validation failures of future blocks and transactions
            // when loading blockchain from scratch
            return false;
        }
        JSONArray transactionsData = (JSONArray)request.get("transactions");
        Transaction[] transactions = new Transaction[transactionsData.size()];
        for (int i = 0; i < transactions.length; i++) {
            transactions[i] = Transaction.getTransaction((JSONObject)transactionsData.get(i));
        }
        try {
            pushBlock(block, transactions);
            return true;
        } catch (BlockNotAcceptedException e) {
            Logger.logDebugMessage("Block " + block.getStringId() + " not accepted: " + e.getMessage());
            throw e;
        }
    }

    static void addBlock(Block block) {
        try (Connection con = Db.getConnection()) {
            try {
                Block.saveBlock(con, block);
                lastBlock.set(block);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static void init() {

        if (! Block.hasBlock(Genesis.GENESIS_BLOCK_ID)) {
            Logger.logMessage("Genesis block not in database, starting from scratch");

            SortedMap<Long,Transaction> transactionsMap = new TreeMap<>();

            try {

                for (int i = 0; i < Genesis.GENESIS_RECIPIENTS.length; i++) {
                    Transaction transaction = Transaction.newTransaction(0, (short)0, Genesis.CREATOR_PUBLIC_KEY,
                            Genesis.GENESIS_RECIPIENTS[i], Genesis.GENESIS_AMOUNTS[i], 0, null, Genesis.GENESIS_SIGNATURES[i]);
                    transaction.setIndex(transactionCounter.incrementAndGet());
                    transactionsMap.put(transaction.getId(), transaction);
                }

                MessageDigest digest = Crypto.sha256();
                for (Transaction transaction : transactionsMap.values()) {
                    digest.update(transaction.getBytes());
                }

                Block genesisBlock = new Block(-1, 0, null, transactionsMap.size(), 1000000000, 0, transactionsMap.size() * 128, digest.digest(),
                        Genesis.CREATOR_PUBLIC_KEY, new byte[64], Genesis.GENESIS_BLOCK_SIGNATURE, null);
                genesisBlock.setIndex(blockCounter.incrementAndGet());

                Transaction[] transactions = transactionsMap.values().toArray(new Transaction[transactionsMap.size()]);
                for (int i = 0; i < transactions.length; i++) {
                    Transaction transaction = transactions[i];
                    genesisBlock.transactionIds[i] = transaction.getId();
                    genesisBlock.blockTransactions[i] = transaction;
                    transaction.setBlock(genesisBlock);
                }

                addBlock(genesisBlock);

            } catch (NxtException.ValidationException validationException) {
                Logger.logMessage(validationException.getMessage());
                System.exit(1);
            }
        }

        Logger.logMessage("Scanning blockchain...");
        Blockchain.scan();
        Logger.logMessage("...Done");
    }

    private static void processTransactions(JSONArray transactionsData, final boolean sendToPeers) throws NxtException.ValidationException {
        Transaction[] transactions = new Transaction[transactionsData.size()];
        for (int i = 0; i < transactions.length; i++) {
            transactions[i] = Transaction.getTransaction((JSONObject)transactionsData.get(i));
        }
        processTransactions(transactions, sendToPeers);

    }

    private static void processTransactions(Transaction[] transactions, final boolean sendToPeers) throws NxtException.ValidationException {
        JSONArray validTransactionsData = new JSONArray();
        List<Transaction> addedUnconfirmedTransactions = new ArrayList<>();
        List<Transaction> addedDoubleSpendingTransactions = new ArrayList<>();

        for (Transaction transaction : transactions) {

            try {

                int curTime = Convert.getEpochTime();
                if (transaction.getTimestamp() > curTime + 15 || transaction.getExpiration() < curTime
                        || transaction.getDeadline() > 1440) {
                    continue;
                }

                boolean doubleSpendingTransaction;

                synchronized (Blockchain.class) {

                    Long id = transaction.getId();
                    if (Transaction.hasTransaction(id) || unconfirmedTransactions.containsKey(id)
                            || doubleSpendingTransactions.containsKey(id) || !transaction.verify()) {
                        continue;
                    }

                    if (transactionHashes.containsKey(transaction.getHash())) {
                        continue;
                    }

                    doubleSpendingTransaction = transaction.isDoubleSpending();

                    transaction.setIndex(transactionCounter.incrementAndGet());

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
            Peer.sendToSomePeers(peerRequest);
        }

        if (addedUnconfirmedTransactions.size() > 0) {
            transactionListeners.notify(addedUnconfirmedTransactions, Event.ADDED_UNCONFIRMED_TRANSACTIONS);
        }
        if (addedDoubleSpendingTransactions.size() > 0) {
            transactionListeners.notify(addedDoubleSpendingTransactions, Event.ADDED_DOUBLESPENDING_TRANSACTIONS);
        }


    }

    private synchronized static byte[] calculateTransactionsChecksum() {
        PriorityQueue<Transaction> sortedTransactions = new PriorityQueue<>(getTransactionCount(), new Comparator<Transaction>() {
            @Override
            public int compare(Transaction o1, Transaction o2) {
                long id1 = o1.getId();
                long id2 = o2.getId();
                return id1 < id2 ? -1 : (id1 > id2 ? 1 : (o1.getTimestamp() < o2.getTimestamp() ? -1 : (o1.getTimestamp() > o2.getTimestamp() ? 1 : 0)));
            }
        });
        try (DbIterator<Transaction> iterator = getAllTransactions()) {
            while (iterator.hasNext()) {
                sortedTransactions.add(iterator.next());
            }
        }
        MessageDigest digest = Crypto.sha256();
        while (! sortedTransactions.isEmpty()) {
            digest.update(sortedTransactions.poll().getBytes());
        }
        return digest.digest();
    }

    private static void pushBlock(final Block block, final Transaction[] trans) throws BlockNotAcceptedException {

        List<Transaction> addedConfirmedTransactions;
        List<Transaction> removedUnconfirmedTransactions;
        int curTime = Convert.getEpochTime();

        synchronized (Blockchain.class) {
            try {

                Block previousLastBlock = lastBlock.get();

                if (! previousLastBlock.getId().equals(block.getPreviousBlockId())) {
                    throw new BlockOutOfOrderException("Previous block id doesn't match");
                }

                if (block.getVersion() != (previousLastBlock.getHeight() < Nxt.TRANSPARENT_FORGING_BLOCK ? 1 : 2)) {
                    throw new BlockNotAcceptedException("Invalid version " + block.getVersion());
                }

                if (previousLastBlock.getHeight() == Nxt.TRANSPARENT_FORGING_BLOCK) {
                    byte[] checksum = calculateTransactionsChecksum();
                    if (CHECKSUM_TRANSPARENT_FORGING == null) {
                        Logger.logMessage("Checksum calculated:\n" + Arrays.toString(checksum));
                    } else if (!Arrays.equals(checksum, CHECKSUM_TRANSPARENT_FORGING)) {
                        Logger.logMessage("Checksum failed at block " + Nxt.TRANSPARENT_FORGING_BLOCK);
                        throw new BlockNotAcceptedException("Checksum failed");
                    } else {
                        Logger.logMessage("Checksum passed at block " + Nxt.TRANSPARENT_FORGING_BLOCK);
                    }
                }

                if (block.getVersion() != 1 && ! Arrays.equals(Crypto.sha256().digest(previousLastBlock.getBytes()), block.getPreviousBlockHash())) {
                    throw new BlockNotAcceptedException("Previous block hash doesn't match");
                }
                if (block.getTimestamp() > curTime + 15 || block.getTimestamp() <= previousLastBlock.getTimestamp()) {
                    throw new BlockNotAcceptedException("Invalid timestamp: " + block.getTimestamp()
                            + " current time is " + curTime + ", previous block timestamp is " + previousLastBlock.getTimestamp());
                }
                if (block.getId().equals(Long.valueOf(0L)) || Block.hasBlock(block.getId())) {
                    throw new BlockNotAcceptedException("Duplicate block or invalid id");
                }
                if (! block.verifyGenerationSignature() || ! block.verifyBlockSignature()) {
                    throw new BlockNotAcceptedException("Signature verification failed");
                }

                block.setIndex(blockCounter.incrementAndGet());

                Map<Long, Transaction> blockTransactions = new HashMap<>();
                for (int i = 0; i < block.transactionIds.length; i++) {
                    Transaction transaction = trans[i];
                    transaction.setIndex(transactionCounter.incrementAndGet());
                    if (blockTransactions.put(block.transactionIds[i] = transaction.getId(), transaction) != null) {
                        throw new BlockNotAcceptedException("Block contains duplicate transactions: " + transaction.getStringId());
                    }
                }

                Arrays.sort(block.transactionIds);

                Map<Transaction.Type, Set<String>> duplicates = new HashMap<>();
                Map<Long, Long> accumulatedAmounts = new HashMap<>();
                Map<Long, Map<Long, Long>> accumulatedAssetQuantities = new HashMap<>();
                int calculatedTotalAmount = 0, calculatedTotalFee = 0;
                MessageDigest digest = Crypto.sha256();
                for (int i = 0; i < block.transactionIds.length; i++) {

                    Long transactionId = block.transactionIds[i];
                    Transaction transaction = blockTransactions.get(transactionId);
                    // cfb: Block 303 contains a transaction which expired before the block timestamp
                    if (transaction.getTimestamp() > curTime + 15 || transaction.getTimestamp() > block.getTimestamp() + 15
                            || (transaction.getExpiration() < block.getTimestamp() && previousLastBlock.getHeight() != 303)) {
                        throw new BlockNotAcceptedException("Invalid transaction timestamp " + transaction.getTimestamp()
                                + " for transaction " + transaction.getStringId() + ", current time is " + curTime
                                + ", block timestamp is " + block.getTimestamp());
                    }
                    if (Transaction.hasTransaction(transactionId)) {
                        throw new BlockNotAcceptedException("Transaction " + transaction.getStringId() + " is already in the blockchain");
                    }
                    if ((transaction.getReferencedTransactionId() != null
                            &&  ! Transaction.hasTransaction(transaction.getReferencedTransactionId())
                            && blockTransactions.get(transaction.getReferencedTransactionId()) == null)) {
                        throw new BlockNotAcceptedException("Missing referenced transaction " + Convert.convert(transaction.getReferencedTransactionId())
                                +" for transaction " + transaction.getStringId());
                    }
                    if ((unconfirmedTransactions.get(transactionId) == null && !transaction.verify())) {
                        throw new BlockNotAcceptedException("Signature verification failed for transaction " + transaction.getStringId());
                    }
                    if (transaction.getId().equals(Long.valueOf(0L))) {
                        throw new BlockNotAcceptedException("Invalid transaction id");
                    }
                    if (transaction.isDuplicate(duplicates)) {
                        throw new BlockNotAcceptedException("Transaction is a duplicate: " + transaction.getStringId());
                    }

                    block.blockTransactions[i] = transaction;

                    calculatedTotalAmount += transaction.getAmount();

                    transaction.updateTotals(accumulatedAmounts, accumulatedAssetQuantities);

                    calculatedTotalFee += transaction.getFee();

                    digest.update(transaction.getBytes());

                }

                if (calculatedTotalAmount != block.getTotalAmount() || calculatedTotalFee != block.getTotalFee()) {
                    throw new BlockNotAcceptedException("Total amount or fee don't match transaction totals");
                }
                if (!Arrays.equals(digest.digest(), block.getPayloadHash())) {
                    throw new BlockNotAcceptedException("Payload hash doesn't match");
                }
                for (Map.Entry<Long, Long> accumulatedAmountEntry : accumulatedAmounts.entrySet()) {
                    Account senderAccount = Account.getAccount(accumulatedAmountEntry.getKey());
                    if (senderAccount.getBalance() < accumulatedAmountEntry.getValue()) {
                        throw new BlockNotAcceptedException("Not enough funds in sender account: " + Convert.convert(senderAccount.getId()));
                    }
                }

                for (Map.Entry<Long, Map<Long, Long>> accumulatedAssetQuantitiesEntry : accumulatedAssetQuantities.entrySet()) {
                    Account senderAccount = Account.getAccount(accumulatedAssetQuantitiesEntry.getKey());
                    for (Map.Entry<Long, Long> accountAccumulatedAssetQuantitiesEntry : accumulatedAssetQuantitiesEntry.getValue().entrySet()) {
                        Long assetId = accountAccumulatedAssetQuantitiesEntry.getKey();
                        Long quantity = accountAccumulatedAssetQuantitiesEntry.getValue();
                        if (senderAccount.getAssetBalance(assetId) < quantity) {
                            throw new BlockNotAcceptedException("Asset balance not sufficient in sender account " + Convert.convert(senderAccount.getId()));
                        }
                    }
                }

                block.setPrevious(previousLastBlock);

                Transaction duplicateTransaction = null;
                for (Transaction transaction : block.blockTransactions) {
                    transaction.setBlock(block);

                    if (transactionHashes.putIfAbsent(transaction.getHash(), transaction) != null && block.getHeight() != 58294) {
                        duplicateTransaction = transaction;
                        break;
                    }
                }

                if (duplicateTransaction != null) {
                    for (Transaction transaction : block.blockTransactions) {
                        if (! transaction.equals(duplicateTransaction)) {
                            Transaction hashTransaction = transactionHashes.get(transaction.getHash());
                            if (hashTransaction != null && hashTransaction.equals(transaction)) {
                                transactionHashes.remove(transaction.getHash());
                            }
                        }
                    }
                    throw new BlockNotAcceptedException("Duplicate hash of transaction " + duplicateTransaction.getStringId());
                }

                addBlock(block);

                block.apply();

                addedConfirmedTransactions = new JSONArray();
                removedUnconfirmedTransactions = new JSONArray();

                for (Map.Entry<Long, Transaction> transactionEntry : blockTransactions.entrySet()) {

                    Transaction transaction = transactionEntry.getValue();
                    addedConfirmedTransactions.add(transaction);

                    Transaction removedTransaction = unconfirmedTransactions.remove(transactionEntry.getKey());
                    if (removedTransaction != null) {
                        removedUnconfirmedTransactions.add(removedTransaction);
                        Account senderAccount = Account.getAccount(removedTransaction.getSenderId());
                        senderAccount.addToUnconfirmedBalance((removedTransaction.getAmount() + removedTransaction.getFee()) * 100L);
                    }

                    // TODO: Remove from double-spending transactions
                }

            } catch (RuntimeException e) {
                Logger.logMessage("Error pushing block", e);
                throw new BlockNotAcceptedException(e.toString());
            }
        } // synchronized

        if (block.getTimestamp() >= curTime - 15) {
            JSONObject request = block.getJSONObject();
            request.put("requestType", "processBlock");
            Peer.sendToSomePeers(request);
        }

        if (removedUnconfirmedTransactions.size() > 0) {
            transactionListeners.notify(removedUnconfirmedTransactions, Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
        }
        if (addedConfirmedTransactions.size() > 0) {
            transactionListeners.notify(addedConfirmedTransactions, Event.ADDED_CONFIRMED_TRANSACTIONS);
        }
        blockListeners.notify(Arrays.asList(block), Event.BLOCK_PUSHED);
    }

    private static boolean popLastBlock() throws Transaction.UndoNotSupportedException {

        try {

            List<Transaction> addedUnconfirmedTransactions = new ArrayList<>();
            Block block;

            synchronized (Blockchain.class) {
                block = lastBlock.get();
                Logger.logDebugMessage("Will pop block " + block.getStringId() + " at height " + block.getHeight());
                if (block.getId().equals(Genesis.GENESIS_BLOCK_ID)) {
                    return false;
                }
                Block previousBlock = Block.findBlock(block.getPreviousBlockId());
                if (previousBlock == null) {
                    Logger.logMessage("Previous block is null");
                    throw new IllegalStateException();
                }
                if (! lastBlock.compareAndSet(block, previousBlock)) {
                    Logger.logMessage("This block is no longer last block");
                    throw new IllegalStateException();
                }
                Account generatorAccount = Account.getAccount(block.getGeneratorId());
                generatorAccount.addToBalanceAndUnconfirmedBalance(-block.getTotalFee() * 100L);
                for (Transaction transaction : block.blockTransactions) {
                    Transaction hashTransaction = transactionHashes.get(transaction.getHash());
                    if (hashTransaction != null && hashTransaction.equals(transaction)) {
                        transactionHashes.remove(transaction.getHash());
                    }
                    unconfirmedTransactions.put(transaction.getId(), transaction);
                    transaction.undo();
                    addedUnconfirmedTransactions.add(transaction);
                }
                Block.deleteBlock(block.getId());
            } // synchronized

            if (addedUnconfirmedTransactions.size() > 0) {
                transactionListeners.notify(addedUnconfirmedTransactions, Event.ADDED_UNCONFIRMED_TRANSACTIONS);
            }

            blockListeners.notify(Arrays.asList(block), Event.BLOCK_POPPED);

        } catch (RuntimeException e) {
            Logger.logMessage("Error popping last block", e);
            return false;
        }
        return true;
    }

    private synchronized static void scan() {
        Account.clear();
        Alias.clear();
        Asset.clear();
        Order.clear();
        unconfirmedTransactions.clear();
        doubleSpendingTransactions.clear();
        nonBroadcastedTransactions.clear();
        transactionHashes.clear();
        try (Connection con = Db.getConnection(); PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block ORDER BY db_id ASC")) {
            Long currentBlockId = Genesis.GENESIS_BLOCK_ID;
            Block currentBlock;
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                currentBlock = Block.getBlock(con, rs);
                if (! currentBlock.getId().equals(currentBlockId)) {
                    throw new NxtException.ValidationException("Database blocks in the wrong order!");
                }
                lastBlock.set(currentBlock);
                currentBlock.apply();
                currentBlockId = currentBlock.getNextBlockId();
            }
        } catch (NxtException.ValidationException|SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static void generateBlock(String secretPhrase) {

        Set<Transaction> sortedTransactions = new TreeSet<>();

        for (Transaction transaction : unconfirmedTransactions.values()) {
            if (transaction.getReferencedTransactionId() == null || Transaction.hasTransaction(transaction.getReferencedTransactionId())) {
                sortedTransactions.add(transaction);
            }
        }

        Map<Long, Transaction> newTransactions = new HashMap<>();
        Map<Transaction.Type, Set<String>> duplicates = new HashMap<>();
        Map<Long, Long> accumulatedAmounts = new HashMap<>();

        int totalAmount = 0;
        int totalFee = 0;
        int payloadLength = 0;

        int blockTimestamp = Convert.getEpochTime();

        while (payloadLength <= Nxt.MAX_PAYLOAD_LENGTH) {

            int prevNumberOfNewTransactions = newTransactions.size();

            for (Transaction transaction : sortedTransactions) {

                int transactionLength = transaction.getSize();
                if (newTransactions.get(transaction.getId()) == null && payloadLength + transactionLength <= Nxt.MAX_PAYLOAD_LENGTH) {

                    Long sender = transaction.getSenderId();
                    Long accumulatedAmount = accumulatedAmounts.get(sender);
                    if (accumulatedAmount == null) {
                        accumulatedAmount = 0L;
                    }

                    long amount = (transaction.getAmount() + transaction.getFee()) * 100L;
                    if (accumulatedAmount + amount <= Account.getAccount(sender).getBalance()) {

                        if (transaction.getTimestamp() > blockTimestamp + 15 || (transaction.getExpiration() < blockTimestamp)) {
                            continue;
                        }

                        if (transaction.isDuplicate(duplicates)) {
                            continue;
                        }

                        accumulatedAmounts.put(sender, accumulatedAmount + amount);

                        newTransactions.put(transaction.getId(), transaction);
                        payloadLength += transactionLength;
                        totalAmount += transaction.getAmount();
                        totalFee += transaction.getFee();

                    }
                }
            }
            if (newTransactions.size() == prevNumberOfNewTransactions) {
                break;
            }
        }

        final byte[] publicKey = Crypto.getPublicKey(secretPhrase);

        Long[] transactionIds = newTransactions.keySet().toArray(new Long[newTransactions.size()]);
        Arrays.sort(transactionIds);
        MessageDigest digest = Crypto.sha256();
        for (Long transactionId : transactionIds) {
            digest.update(newTransactions.get(transactionId).getBytes());
        }

        byte[] payloadHash = digest.digest();
        byte[] generationSignature;

        Block previousBlock = lastBlock.get();
        if (previousBlock.getHeight() < Nxt.TRANSPARENT_FORGING_BLOCK) {
            generationSignature = Crypto.sign(previousBlock.getGenerationSignature(), secretPhrase);
        } else {
            digest.update(previousBlock.getGenerationSignature());
            generationSignature = digest.digest(publicKey);
        }

        Block block;

        try {
            if (previousBlock.getHeight() < Nxt.TRANSPARENT_FORGING_BLOCK) {
                block = new Block(1, blockTimestamp, previousBlock.getId(), newTransactions.size(),
                        totalAmount, totalFee, payloadLength, payloadHash, publicKey, generationSignature, null, null);
            } else {
                byte[] previousBlockHash = Crypto.sha256().digest(previousBlock.getBytes());
                block = new Block(2, blockTimestamp, previousBlock.getId(), newTransactions.size(),
                        totalAmount, totalFee, payloadLength, payloadHash, publicKey, generationSignature, null, previousBlockHash);
            }
        } catch (NxtException.ValidationException e) {
            // shouldn't happen because all transactions are already validated
            Logger.logMessage("Error generating block", e);
            return;
        }

        block.sign(secretPhrase);

        block.setPrevious(previousBlock);

        for (int i = 0 ; i < transactionIds.length; i++) {
            block.transactionIds[i] = transactionIds[i];
            block.blockTransactions[i] = newTransactions.get(transactionIds[i]);
            block.blockTransactions[i].setBlock(block);
        }

        try {
            if (block.verifyBlockSignature() && block.verifyGenerationSignature()) {
                pushBlock(block, block.blockTransactions);
                Logger.logDebugMessage("Account " + Convert.convert(block.getGeneratorId()) +" generated block " + block.getStringId());
            } else {
                Logger.logMessage("Generated an incorrect block. Waiting for the next one...");
            }
        } catch (BlockNotAcceptedException e) {
            Logger.logDebugMessage("Generate block failed: " + e.getMessage());
        }

    }

    static void purgeExpiredHashes(int blockTimestamp) {
        Iterator<Map.Entry<String, Transaction>> iterator = Blockchain.transactionHashes.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().getExpiration() < blockTimestamp) {
                iterator.remove();
            }
        }
    }

    public static class BlockNotAcceptedException extends NxtException {

        private BlockNotAcceptedException(String message) {
            super(message);
        }

    }

    public static class BlockOutOfOrderException extends BlockNotAcceptedException {

        BlockOutOfOrderException(String message) {
            super(message);
        }
	}

    private Blockchain() {} // never, yet

}
