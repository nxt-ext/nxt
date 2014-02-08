package nxt;

import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.user.User;
import nxt.util.Convert;
import nxt.util.DbIterator;
import nxt.util.DbUtils;
import nxt.util.JSON;
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
                    if (peer != null) {

                        JSONObject response = peer.send(getUnconfirmedTransactionsRequest);
                        if (response != null) {
                            try {
                                Blockchain.processUnconfirmedTransactions(response);
                            } catch (NxtException.ValidationException e) {
                                peer.blacklist(e);
                            }
                        }

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
                    JSONArray removedUnconfirmedTransactions = new JSONArray();

                    Iterator<Transaction> iterator = unconfirmedTransactions.values().iterator();
                    while (iterator.hasNext()) {

                        Transaction transaction = iterator.next();
                        if (transaction.getExpiration() < curTime) {

                            iterator.remove();

                            Account account = Account.getAccount(transaction.getSenderAccountId());
                            account.addToUnconfirmedBalance((transaction.getAmount() + transaction.getFee()) * 100L);

                            JSONObject removedUnconfirmedTransaction = new JSONObject();
                            removedUnconfirmedTransaction.put("index", transaction.getIndex());
                            removedUnconfirmedTransactions.add(removedUnconfirmedTransaction);

                        }

                    }

                    if (removedUnconfirmedTransactions.size() > 0) {

                        JSONObject response = new JSONObject();
                        response.put("response", "processNewData");

                        response.put("removedUnconfirmedTransactions", removedUnconfirmedTransactions);


                        User.sendToAll(response);

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

        private final JSONStreamAware getMilestoneBlockIdsRequest;
        {
            JSONObject request = new JSONObject();
            request.put("requestType", "getMilestoneBlockIds");
            getMilestoneBlockIdsRequest = JSON.prepareRequest(request);
        }

        @Override
        public void run() {

            try {
                try {
                    Peer peer = Peer.getAnyPeer(Peer.State.CONNECTED, true);
                    if (peer != null) {

                        lastBlockchainFeeder = peer;

                        JSONObject response = peer.send(getCumulativeDifficultyRequest);
                        if (response != null) {

                            BigInteger curCumulativeDifficulty = lastBlock.get().getCumulativeDifficulty();
                            String peerCumulativeDifficulty = (String)response.get("cumulativeDifficulty");
                            if (peerCumulativeDifficulty == null) {
                                return;
                            }
                            BigInteger betterCumulativeDifficulty = new BigInteger(peerCumulativeDifficulty);
                            if (betterCumulativeDifficulty.compareTo(curCumulativeDifficulty) > 0) {

                                response = peer.send(getMilestoneBlockIdsRequest);
                                if (response != null) {

                                    Long commonBlockId = Genesis.GENESIS_BLOCK_ID;

                                    //TODO: change to support getting only a few milestoneBlockIds at a time
                                    JSONArray milestoneBlockIds = (JSONArray)response.get("milestoneBlockIds");
                                    if (milestoneBlockIds == null) {
                                        return;
                                    }

                                    for (Object milestoneBlockId : milestoneBlockIds) {
                                        Long blockId = Convert.parseUnsignedLong((String) milestoneBlockId);
                                        if (Block.hasBlock(blockId)) {
                                            commonBlockId = blockId;
                                            break;
                                        }
                                    }

                                    {
                                        int i, numberOfBlocks;
                                        do {

                                            JSONObject request = new JSONObject();
                                            request.put("requestType", "getNextBlockIds");
                                            request.put("blockId", Convert.convert(commonBlockId));
                                            response = peer.send(JSON.prepareRequest(request));
                                            if (response == null) {
                                                return;
                                            }

                                            JSONArray nextBlockIds = (JSONArray)response.get("nextBlockIds");
                                            if (nextBlockIds == null || (numberOfBlocks = nextBlockIds.size()) == 0) {
                                                return;
                                            }

                                            Long blockId;
                                            for (i = 0; i < numberOfBlocks; i++) {
                                                blockId = Convert.parseUnsignedLong((String) nextBlockIds.get(i));
                                                if (! Block.hasBlock(blockId)) {
                                                    break;
                                                }
                                                commonBlockId = blockId;
                                            }

                                        } while (i == numberOfBlocks);
                                    }

                                    final Block commonBlock = Block.findBlock(commonBlockId);
                                    if (lastBlock.get().getHeight() - commonBlock.getHeight() < 720) {

                                        Long curBlockId = commonBlockId;
                                        LinkedList<Block> futureBlocks = new LinkedList<>();
                                        HashMap<Long, Transaction> futureTransactions = new HashMap<>();

                                        do {

                                            JSONObject request = new JSONObject();
                                            request.put("requestType", "getNextBlocks");
                                            request.put("blockId", Convert.convert(curBlockId));
                                            response = peer.send(JSON.prepareRequest(request));
                                            if (response == null) {
                                                break;
                                            }

                                            JSONArray nextBlocks = (JSONArray)response.get("nextBlocks");
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
                                                            Transaction[] transactions = new Transaction[transactionData.size()];
                                                            for (int j = 0; j < transactions.length; j++) {
                                                                transactions[j] = Transaction.getTransaction((JSONObject)transactionData.get(j));
                                                            }
                                                            try {
                                                                Blockchain.pushBlock(block, transactions);
                                                            } catch (BlockNotAcceptedException e) {
                                                                Logger.logDebugMessage("Failed to accept block " + block.getStringId()
                                                                        + " at height " + lastBlock.get().getHeight()
                                                                        + " received from " + peer.getPeerAddress()+ ", blacklisting");
                                                                Logger.logDebugMessage("Reason: " + e.getMessage());
                                                                peer.blacklist();
                                                                return;
                                                            }
                                                        } catch (NxtException.ValidationException e) {
                                                            peer.blacklist(e);
                                                            return;
                                                        }

                                                    } else if (! Block.hasBlock(block.getId()) && block.transactionIds.length <= Nxt.MAX_NUMBER_OF_TRANSACTIONS) {

                                                        futureBlocks.add(block);

                                                        JSONArray transactionsData = (JSONArray)blockData.get("transactions");
                                                        try {
                                                            for (int j = 0; j < block.transactionIds.length; j++) {

                                                                Transaction transaction = Transaction.getTransaction((JSONObject)transactionsData.get(j));
                                                                block.transactionIds[j] = transaction.getId();
                                                                block.blockTransactions[j] = transaction;
                                                                futureTransactions.put(block.transactionIds[j], transaction);

                                                            }
                                                        } catch (NxtException.ValidationException e) {
                                                            peer.blacklist(e);
                                                            return;
                                                        }
                                                    }

                                                }

                                            } //synchronized

                                        } while (true);

                                        if (!futureBlocks.isEmpty() && lastBlock.get().getHeight() - commonBlock.getHeight() < 720) {

                                            synchronized (Blockchain.class) {
                                                curCumulativeDifficulty = lastBlock.get().getCumulativeDifficulty();
                                                boolean needsRescan;

                                                try {
                                                    while (!lastBlock.get().getId().equals(commonBlockId) && Blockchain.popLastBlock()) {}

                                                    if (lastBlock.get().getId().equals(commonBlockId)) {
                                                        for (Block block : futureBlocks) {
                                                            if (lastBlock.get().getId().equals(block.getPreviousBlockId())) {
                                                                try {
                                                                    Blockchain.pushBlock(block, block.blockTransactions);
                                                                } catch (BlockNotAcceptedException e) {
                                                                    Logger.logDebugMessage("Failed to push future block " + block.getStringId());
                                                                    Logger.logDebugMessage("Reasin: " + e.getMessage());
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
                                    }
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error in milestone blocks processing thread", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    static final Runnable generateBlockThread = new Runnable() {

        private final ConcurrentMap<Account, Block> lastBlocks = new ConcurrentHashMap<>();
        private final ConcurrentMap<Account, BigInteger> hits = new ConcurrentHashMap<>();


        @Override
        public void run() {

            try {
                try {
                    Map<Account,User> unlockedAccounts = new HashMap<>();
                    for (User user : User.getAllUsers()) {
                        if (user.getSecretPhrase() != null) {
                            Account account = Account.getAccount(user.getPublicKey());
                            if (account != null && account.getEffectiveBalance() > 0) {
                                unlockedAccounts.put(account, user);
                            }
                        }
                    }

                    for (Map.Entry<Account, User> unlockedAccountEntry : unlockedAccounts.entrySet()) {

                        Account account = unlockedAccountEntry.getKey();
                        User user = unlockedAccountEntry.getValue();
                        Block lastBlock = Blockchain.lastBlock.get();
                        if (lastBlocks.get(account) != lastBlock) {

                            long effectiveBalance = account.getEffectiveBalance();
                            if (effectiveBalance <= 0) {
                                continue;
                            }
                            MessageDigest digest = Crypto.sha256();
                            byte[] generationSignatureHash;
                            if (lastBlock.getHeight() < Nxt.TRANSPARENT_FORGING_BLOCK) {

                                byte[] generationSignature = Crypto.sign(lastBlock.getGenerationSignature(), user.getSecretPhrase());
                                generationSignatureHash = digest.digest(generationSignature);

                            } else {

                                digest.update(lastBlock.getGenerationSignature());
                                generationSignatureHash = digest.digest(user.getPublicKey());

                            }
                            BigInteger hit = new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});

                            lastBlocks.put(account, lastBlock);
                            hits.put(account, hit);

                            JSONObject response = new JSONObject();
                            response.put("response", "setBlockGenerationDeadline");
                            response.put("deadline", hit.divide(BigInteger.valueOf(lastBlock.getBaseTarget()).multiply(BigInteger.valueOf(effectiveBalance))).longValue() - (Convert.getEpochTime() - lastBlock.getTimestamp()));

                            user.send(response);

                        }

                        int elapsedTime = Convert.getEpochTime() - lastBlock.getTimestamp();
                        if (elapsedTime > 0) {

                            BigInteger target = BigInteger.valueOf(lastBlock.getBaseTarget()).multiply(BigInteger.valueOf(account.getEffectiveBalance())).multiply(BigInteger.valueOf(elapsedTime));
                            if (hits.get(account).compareTo(target) < 0) {

                                Blockchain.generateBlock(user.getSecretPhrase());

                            }

                        }

                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error in block generation thread", e);
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
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE timestamp >= ? AND generator_public_key = ? ORDER BY db_id ASC");
            pstmt.setInt(1, timestamp);
            pstmt.setBytes(2, account.getPublicKey());
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
                    pstmt = con.prepareStatement("SELECT * FROM transaction WHERE timestamp >= ? AND (recipient_id = ? OR sender_account_id = ?) AND type = ? AND subtype = ? ORDER BY timestamp ASC");
                    pstmt.setInt(1, timestamp);
                    pstmt.setLong(2, account.getId());
                    pstmt.setLong(3, account.getId());
                    pstmt.setByte(4, type);
                    pstmt.setByte(5, subtype);
                } else {
                    pstmt = con.prepareStatement("SELECT * FROM transaction WHERE timestamp >= ? AND (recipient_id = ? OR sender_account_id = ?) AND type = ? ORDER BY timestamp ASC");
                    pstmt.setInt(1, timestamp);
                    pstmt.setLong(2, account.getId());
                    pstmt.setLong(3, account.getId());
                    pstmt.setByte(4, type);
                }
            } else {
                pstmt = con.prepareStatement("SELECT * FROM transaction WHERE timestamp >= ? AND (recipient_id = ? OR sender_account_id = ?) ORDER BY timestamp ASC");
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

        Peer.sendToSomePeers(peerRequest);

        nonBroadcastedTransactions.put(transaction.getId(), transaction);
    }

    public static Peer getLastBlockchainFeeder() {
        return lastBlockchainFeeder;
    }

    public static void processTransactions(JSONObject request) throws NxtException.ValidationException {
        JSONArray transactionsData = (JSONArray)request.get("transactions");
        processTransactions(transactionsData, false);
    }

    public static boolean pushBlock(JSONObject request) throws NxtException.ValidationException {

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
            return false;
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

                Block genesisBlock = new Block(-1, 0, null, transactionsMap.size(), 1000000000, 0, transactionsMap.size() * 128, null,
                        Genesis.CREATOR_PUBLIC_KEY, new byte[64], Genesis.GENESIS_BLOCK_SIGNATURE);
                genesisBlock.setIndex(blockCounter.incrementAndGet());

                Transaction[] transactions = transactionsMap.values().toArray(new Transaction[transactionsMap.size()]);
                MessageDigest digest = Crypto.sha256();
                for (int i = 0; i < transactions.length; i++) {
                    Transaction transaction = transactions[i];
                    genesisBlock.transactionIds[i] = transaction.getId();
                    genesisBlock.blockTransactions[i] = transaction;
                    digest.update(transaction.getBytes());
                }

                genesisBlock.setPayloadHash(digest.digest());

                for (Transaction transaction : genesisBlock.blockTransactions) {
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

    private static void processUnconfirmedTransactions(JSONObject request) throws NxtException.ValidationException {
        JSONArray transactionsData = (JSONArray)request.get("unconfirmedTransactions");
        processTransactions(transactionsData, true);
    }

    private static void processTransactions(JSONArray transactionsData, final boolean unconfirmed) throws NxtException.ValidationException {

        JSONArray validTransactionsData = new JSONArray();

        for (Object transactionData : transactionsData) {

            try {

                Transaction transaction = Transaction.getTransaction((JSONObject) transactionData);

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

                        unconfirmedTransactions.put(id, transaction);

                        if (! unconfirmed) {

                            validTransactionsData.add(transactionData);

                        }

                    }

                }

                JSONObject response = new JSONObject();
                response.put("response", "processNewData");

                JSONArray newTransactions = new JSONArray();
                JSONObject newTransaction = new JSONObject();
                newTransaction.put("index", transaction.getIndex());
                newTransaction.put("timestamp", transaction.getTimestamp());
                newTransaction.put("deadline", transaction.getDeadline());
                newTransaction.put("recipient", Convert.convert(transaction.getRecipientId()));
                newTransaction.put("amount", transaction.getAmount());
                newTransaction.put("fee", transaction.getFee());
                newTransaction.put("sender", Convert.convert(transaction.getSenderAccountId()));
                newTransaction.put("id", transaction.getStringId());
                newTransactions.add(newTransaction);

                if (doubleSpendingTransaction) {

                    response.put("addedDoubleSpendingTransactions", newTransactions);

                } else {

                    response.put("addedUnconfirmedTransactions", newTransactions);

                }

                User.sendToAll(response);

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

        JSONArray addedConfirmedTransactions;
        JSONArray removedUnconfirmedTransactions;
        int curTime = Convert.getEpochTime();

        synchronized (Blockchain.class) {
            try {

                Block previousLastBlock = lastBlock.get();

                if (block.getVersion() != (previousLastBlock.getHeight() < Nxt.TRANSPARENT_FORGING_BLOCK ? 1 : 2)) {
                    throw new BlockNotAcceptedException("Invalid version " + block.getVersion());
                }

                if (previousLastBlock.getHeight() == Nxt.TRANSPARENT_FORGING_BLOCK) {
                    byte[] checksum = calculateTransactionsChecksum();
                    if (CHECKSUM_TRANSPARENT_FORGING == null) {
                        Logger.logMessage("Checksum calculated:\n" + Arrays.toString(checksum));
                    } else if (!Arrays.equals(checksum, CHECKSUM_TRANSPARENT_FORGING)) {
                        Logger.logMessage("Checksum failed at block " + Nxt.TRANSPARENT_FORGING_BLOCK);
                        throw new BlockNotAcceptedException("Checksum faied");
                    } else {
                        Logger.logMessage("Checksum passed at block " + Nxt.TRANSPARENT_FORGING_BLOCK);
                    }
                }

                if (block.getVersion() != 1 && ! Arrays.equals(Crypto.sha256().digest(previousLastBlock.getBytes()), block.getPreviousBlockHash())) {
                    throw new BlockNotAcceptedException("Previos block hash doesn't match");
                }
                if (block.getTimestamp() > curTime + 15 || block.getTimestamp() <= previousLastBlock.getTimestamp()) {
                    throw new BlockNotAcceptedException("Invalid timestamp: " + block.getTimestamp()
                            + " current time is " + curTime + ", previous block timestamp is " + previousLastBlock.getTimestamp());
                }
                if (! previousLastBlock.getId().equals(block.getPreviousBlockId())) {
                    throw new BlockNotAcceptedException("Previous block id doesn't match");
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

                block.setHeight(previousLastBlock.getHeight() + 1);

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

                block.calculateBaseTarget();

                addBlock(block);

                block.apply();

                addedConfirmedTransactions = new JSONArray();
                removedUnconfirmedTransactions = new JSONArray();

                for (Map.Entry<Long, Transaction> transactionEntry : blockTransactions.entrySet()) {

                    Transaction transaction = transactionEntry.getValue();

                    JSONObject addedConfirmedTransaction = new JSONObject();
                    addedConfirmedTransaction.put("index", transaction.getIndex());
                    addedConfirmedTransaction.put("blockTimestamp", block.getTimestamp());
                    addedConfirmedTransaction.put("transactionTimestamp", transaction.getTimestamp());
                    addedConfirmedTransaction.put("sender", Convert.convert(transaction.getSenderAccountId()));
                    addedConfirmedTransaction.put("recipient", Convert.convert(transaction.getRecipientId()));
                    addedConfirmedTransaction.put("amount", transaction.getAmount());
                    addedConfirmedTransaction.put("fee", transaction.getFee());
                    addedConfirmedTransaction.put("id", transaction.getStringId());
                    addedConfirmedTransactions.add(addedConfirmedTransaction);

                    Transaction removedTransaction = unconfirmedTransactions.remove(transactionEntry.getKey());
                    if (removedTransaction != null) {
                        JSONObject removedUnconfirmedTransaction = new JSONObject();
                        removedUnconfirmedTransaction.put("index", removedTransaction.getIndex());
                        removedUnconfirmedTransactions.add(removedUnconfirmedTransaction);

                        Account senderAccount = Account.getAccount(removedTransaction.getSenderAccountId());
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

        JSONArray addedRecentBlocks = new JSONArray();
        JSONObject addedRecentBlock = new JSONObject();
        addedRecentBlock.put("index", block.getIndex());
        addedRecentBlock.put("timestamp", block.getTimestamp());
        addedRecentBlock.put("numberOfTransactions", block.transactionIds.length);
        addedRecentBlock.put("totalAmount", block.getTotalAmount());
        addedRecentBlock.put("totalFee", block.getTotalFee());
        addedRecentBlock.put("payloadLength", block.getPayloadLength());
        addedRecentBlock.put("generator", Convert.convert(block.getGeneratorAccountId()));
        addedRecentBlock.put("height", block.getHeight());
        addedRecentBlock.put("version", block.getVersion());
        addedRecentBlock.put("block", block.getStringId());
        addedRecentBlock.put("baseTarget", BigInteger.valueOf(block.getBaseTarget()).multiply(BigInteger.valueOf(100000)).divide(BigInteger.valueOf(Nxt.initialBaseTarget)));
        addedRecentBlocks.add(addedRecentBlock);

        JSONObject response = new JSONObject();
        response.put("response", "processNewData");
        response.put("addedConfirmedTransactions", addedConfirmedTransactions);
        if (removedUnconfirmedTransactions.size() > 0) {
            response.put("removedUnconfirmedTransactions", removedUnconfirmedTransactions);
        }
        response.put("addedRecentBlocks", addedRecentBlocks);

        User.sendToAll(response);

    }

    private static boolean popLastBlock() throws Transaction.UndoNotSupportedException {

        try {

            JSONObject response = new JSONObject();
            response.put("response", "processNewData");

            JSONArray addedUnconfirmedTransactions = new JSONArray();

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

                Account generatorAccount = Account.getAccount(block.getGeneratorAccountId());
                generatorAccount.addToBalanceAndUnconfirmedBalance(-block.getTotalFee() * 100L);

                for (Transaction transaction : block.blockTransactions) {

                    Transaction hashTransaction = transactionHashes.get(transaction.getHash());
                    if (hashTransaction != null && hashTransaction.equals(transaction)) {
                        transactionHashes.remove(transaction.getHash());
                    }
                    
                    unconfirmedTransactions.put(transaction.getId(), transaction);

                    transaction.undo();

                    JSONObject addedUnconfirmedTransaction = new JSONObject();
                    addedUnconfirmedTransaction.put("index", transaction.getIndex());
                    addedUnconfirmedTransaction.put("timestamp", transaction.getTimestamp());
                    addedUnconfirmedTransaction.put("deadline", transaction.getDeadline());
                    addedUnconfirmedTransaction.put("recipient", Convert.convert(transaction.getRecipientId()));
                    addedUnconfirmedTransaction.put("amount", transaction.getAmount());
                    addedUnconfirmedTransaction.put("fee", transaction.getFee());
                    addedUnconfirmedTransaction.put("sender", Convert.convert(transaction.getSenderAccountId()));
                    addedUnconfirmedTransaction.put("id", transaction.getStringId());
                    addedUnconfirmedTransactions.add(addedUnconfirmedTransaction);

                }

                Block.deleteBlock(block.getId());

            } // synchronized

            JSONArray addedOrphanedBlocks = new JSONArray();
            JSONObject addedOrphanedBlock = new JSONObject();
            addedOrphanedBlock.put("index", block.getIndex());
            addedOrphanedBlock.put("timestamp", block.getTimestamp());
            addedOrphanedBlock.put("numberOfTransactions", block.transactionIds.length);
            addedOrphanedBlock.put("totalAmount", block.getTotalAmount());
            addedOrphanedBlock.put("totalFee", block.getTotalFee());
            addedOrphanedBlock.put("payloadLength", block.getPayloadLength());
            addedOrphanedBlock.put("generator", Convert.convert(block.getGeneratorAccountId()));
            addedOrphanedBlock.put("height", block.getHeight());
            addedOrphanedBlock.put("version", block.getVersion());
            addedOrphanedBlock.put("block", block.getStringId());
            addedOrphanedBlock.put("baseTarget", BigInteger.valueOf(block.getBaseTarget()).multiply(BigInteger.valueOf(100000)).divide(BigInteger.valueOf(Nxt.initialBaseTarget)));
            addedOrphanedBlocks.add(addedOrphanedBlock);
            response.put("addedOrphanedBlocks", addedOrphanedBlocks);

            if (addedUnconfirmedTransactions.size() > 0) {
                response.put("addedUnconfirmedTransactions", addedUnconfirmedTransactions);
            }

            User.sendToAll(response);

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

    private static void generateBlock(String secretPhrase) {

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

                    Long sender = transaction.getSenderAccountId();
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

        Block block;
        Block previousBlock = lastBlock.get();

        try {
            if (previousBlock.getHeight() < Nxt.TRANSPARENT_FORGING_BLOCK) {

                block = new Block(1, blockTimestamp, previousBlock.getId(), newTransactions.size(),
                        totalAmount, totalFee, payloadLength, null, publicKey, null, new byte[64]);

            } else {

                byte[] previousBlockHash = Crypto.sha256().digest(previousBlock.getBytes());
                block = new Block(2, blockTimestamp, previousBlock.getId(), newTransactions.size(),
                        totalAmount, totalFee, payloadLength, null, publicKey, null, new byte[64], previousBlockHash);

            }
        } catch (NxtException.ValidationException e) {
            // shouldn't happen because all transactions are already validated
            Logger.logMessage("Error generating block", e);
            return;
        }

        int i = 0;
        for (Long transactionId : newTransactions.keySet()) {
            block.transactionIds[i++] = transactionId;
        }

        Arrays.sort(block.transactionIds);
        MessageDigest digest = Crypto.sha256();
        for (i = 0; i < block.transactionIds.length; i++) {
            Transaction transaction = newTransactions.get(block.transactionIds[i]);
            digest.update(transaction.getBytes());
            block.blockTransactions[i] = transaction;
        }
        block.setPayloadHash(digest.digest());

        if (previousBlock.getHeight() < Nxt.TRANSPARENT_FORGING_BLOCK) {

            block.setGenerationSignature(Crypto.sign(previousBlock.getGenerationSignature(), secretPhrase));

        } else {

            digest.update(previousBlock.getGenerationSignature());
            block.setGenerationSignature(digest.digest(publicKey));

        }

        byte[] data = block.getBytes();
        byte[] data2 = new byte[data.length - 64];
        System.arraycopy(data, 0, data2, 0, data2.length);
        block.setBlockSignature(Crypto.sign(data2, secretPhrase));

        if (block.verifyBlockSignature() && block.verifyGenerationSignature()) {

            JSONObject request = block.getJSONObject();
            request.put("requestType", "processBlock");
            Peer.sendToSomePeers(request);

        } else {

            Logger.logMessage("Generated an incorrect block. Waiting for the next one...");

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

    private static class BlockNotAcceptedException extends NxtException {

        private BlockNotAcceptedException(String message) {
            super(message);
        }

    }

    private Blockchain() {} // never, yet

}
