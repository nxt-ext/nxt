package nxt;

import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Convert;
import nxt.util.DbIterator;
import nxt.util.DbUtils;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import nxt.util.ThreadPool;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

public final class Blockchain {

    public static enum Event {
        BLOCK_PUSHED, BLOCK_POPPED,
        REMOVED_UNCONFIRMED_TRANSACTIONS,
        ADDED_UNCONFIRMED_TRANSACTIONS,
        ADDED_CONFIRMED_TRANSACTIONS,
        ADDED_DOUBLESPENDING_TRANSACTIONS
    }

    private static final Listeners<Block,Event> blockListeners = new Listeners<>();

    private static final byte[] CHECKSUM_TRANSPARENT_FORGING = new byte[]{27, -54, -59, -98, 49, -42, 48, -68, -112, 49, 41, 94, -41, 78, -84, 27, -87, -22, -28, 36, -34, -90, 112, -50, -9, 5, 89, -35, 80, -121, -128, 112};

    private static volatile Peer lastBlockchainFeeder;

    private static final AtomicReference<BlockImpl> lastBlock = new AtomicReference<>();

    static final Runnable getMoreBlocksThread = new Runnable() {

        private final JSONStreamAware getCumulativeDifficultyRequest;
        {
            JSONObject request = new JSONObject();
            request.put("requestType", "getCumulativeDifficulty");
            getCumulativeDifficultyRequest = JSON.prepareRequest(request);
        }

        private boolean peerHasMore;

        @Override
        public void run() {

            try {
                try {
                    peerHasMore = true;
                    Peer peer = Peers.getAnyPeer(Peer.State.CONNECTED, true);
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
                    if (commonBlockId == null || ! peerHasMore) {
                        return;
                    }

                    commonBlockId = getCommonBlockId(peer, commonBlockId);
                    if (commonBlockId == null || ! peerHasMore) {
                        return;
                    }

                    final Block commonBlock = Blocks.findBlock(commonBlockId);
                    if (lastBlock.get().getHeight() - commonBlock.getHeight() >= 720) {
                        return;
                    }

                    Long currentBlockId = commonBlockId;
                    List<BlockImpl> forkBlocks = new ArrayList<>();

                    while (true) {

                        JSONArray nextBlocks = getNextBlocks(peer, currentBlockId);
                        if (nextBlocks == null || nextBlocks.size() == 0) {
                            break;
                        }

                        synchronized (Blockchain.class) {

                            for (Object o : nextBlocks) {
                                JSONObject blockData = (JSONObject)o;
                                BlockImpl block;
                                try {
                                    block = Blocks.getBlock(blockData);
                                } catch (NxtException.ValidationException e) {
                                    peer.blacklist(e);
                                    return;
                                }
                                currentBlockId = block.getId();

                                if (lastBlock.get().getId().equals(block.getPreviousBlockId())) {
                                    try {

                                        pushBlock(block);

                                    } catch (BlockNotAcceptedException e) {
                                        Logger.logDebugMessage("Failed to accept block " + block.getStringId()
                                                + " at height " + lastBlock.get().getHeight()
                                                + " received from " + peer.getPeerAddress() + ", blacklisting");
                                        peer.blacklist(e);
                                        return;
                                    }
                                } else if (! Blocks.hasBlock(block.getId())) {

                                    forkBlocks.add(block);

                                }

                            }

                        } //synchronized

                    }

                    if (! forkBlocks.isEmpty() && lastBlock.get().getHeight() - commonBlock.getHeight() < 720) {
                        processFork(peer, forkBlocks, commonBlock);
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

        private Long getCommonMilestoneBlockId(Peer peer) {

            String lastMilestoneBlockId = null;

            while (true) {
                JSONObject milestoneBlockIdsRequest = new JSONObject();
                milestoneBlockIdsRequest.put("requestType", "getMilestoneBlockIds");
                if (lastMilestoneBlockId == null) {
                    milestoneBlockIdsRequest.put("lastBlockId", getLastBlock().getStringId());
                } else {
                    milestoneBlockIdsRequest.put("lastMilestoneBlockId", lastMilestoneBlockId);
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
                // prevent overloading with blockIds
                if (milestoneBlockIds.size() > 20) {
                    Logger.logDebugMessage("Obsolete or rogue peer " + peer.getPeerAddress() + " sends too many milestoneBlockIds, blacklisting");
                    peer.blacklist();
                    return null;
                }
                if (Boolean.TRUE.equals(response.get("last"))) {
                    peerHasMore = false;
                }
                for (Object milestoneBlockId : milestoneBlockIds) {
                    Long blockId = Convert.parseUnsignedLong((String)milestoneBlockId);
                    if (Blocks.hasBlock(blockId)) {
                        if (lastMilestoneBlockId == null && milestoneBlockIds.size() > 1) {
                            peerHasMore = false;
                        }
                        return blockId;
                    }
                    lastMilestoneBlockId = (String) milestoneBlockId;
                }
            }

        }

        private Long getCommonBlockId(Peer peer, Long commonBlockId) {

            while (true) {
                JSONObject request = new JSONObject();
                request.put("requestType", "getNextBlockIds");
                request.put("blockId", Convert.toUnsignedLong(commonBlockId));
                JSONObject response = peer.send(JSON.prepareRequest(request));
                if (response == null) {
                    return null;
                }
                JSONArray nextBlockIds = (JSONArray)response.get("nextBlockIds");
                if (nextBlockIds == null || nextBlockIds.size() == 0) {
                    return null;
                }
                // prevent overloading with blockIds
                if (nextBlockIds.size() > 1440) {
                    Logger.logDebugMessage("Obsolete or rogue peer " + peer.getPeerAddress() + " sends too many nextBlockIds, blacklisting");
                    peer.blacklist();
                    return null;
                }

                for (Object nextBlockId : nextBlockIds) {
                    Long blockId = Convert.parseUnsignedLong((String) nextBlockId);
                    if (! Blocks.hasBlock(blockId)) {
                        return commonBlockId;
                    }
                    commonBlockId = blockId;
                }
            }

        }

        private JSONArray getNextBlocks(Peer peer, Long curBlockId) {

            JSONObject request = new JSONObject();
            request.put("requestType", "getNextBlocks");
            request.put("blockId", Convert.toUnsignedLong(curBlockId));
            JSONObject response = peer.send(JSON.prepareRequest(request));
            if (response == null) {
                return null;
            }

            JSONArray nextBlocks = (JSONArray)response.get("nextBlocks");
            // prevent overloading with blocks
            if (nextBlocks.size() > 1440) {
                Logger.logDebugMessage("Obsolete or rogue peer " + peer.getPeerAddress() + " sends too many nextBlocks, blacklisting");
                peer.blacklist();
                return null;
            }

            return nextBlocks;

        }

        private void processFork(Peer peer, final List<BlockImpl> forkBlocks, final Block commonBlock) {

            synchronized (Blockchain.class) {
                BigInteger curCumulativeDifficulty = lastBlock.get().getCumulativeDifficulty();
                boolean needsRescan;

                try {
                    while (!lastBlock.get().getId().equals(commonBlock.getId()) && popLastBlock()) {}

                    if (lastBlock.get().getId().equals(commonBlock.getId())) {
                        for (BlockImpl block : forkBlocks) {
                            if (lastBlock.get().getId().equals(block.getPreviousBlockId())) {
                                try {
                                    pushBlock(block);
                                } catch (BlockNotAcceptedException e) {
                                    Logger.logDebugMessage("Failed to push fork block " + block.getStringId()
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
                } catch (TransactionType.UndoNotSupportedException e) {
                    Logger.logDebugMessage(e.getMessage());
                    Logger.logDebugMessage("Popping off last block not possible, will do a rescan");
                    needsRescan = true;
                }

                if (needsRescan) {
                    // this relies on the database cascade trigger to delete all blocks after commonBlock
                    if (commonBlock.getNextBlockId() != null) {
                        Logger.logDebugMessage("Last block is " + lastBlock.get().getStringId() + " at " + lastBlock.get().getHeight());
                        Logger.logDebugMessage("Deleting blocks after height " + commonBlock.getHeight());
                        Blocks.deleteBlock(commonBlock.getNextBlockId());
                    }
                    Logger.logMessage("Re-scanning blockchain...");
                    scan();
                    Logger.logMessage("...Done re-scanning");
                    Logger.logDebugMessage("Last block is " + lastBlock.get().getStringId() + " at " + lastBlock.get().getHeight());
                }
            }

        }

    };

    static {
        if (! Blocks.hasBlock(Genesis.GENESIS_BLOCK_ID)) {
            Logger.logMessage("Genesis block not in database, starting from scratch");

            try {
                SortedMap<Long,TransactionImpl> transactionsMap = new TreeMap<>();

                for (int i = 0; i < Genesis.GENESIS_RECIPIENTS.length; i++) {
                    TransactionImpl transaction = TransactionProcessor.newTransaction(0, (short) 0, Genesis.CREATOR_PUBLIC_KEY,
                            Genesis.GENESIS_RECIPIENTS[i], Genesis.GENESIS_AMOUNTS[i], 0, null, Genesis.GENESIS_SIGNATURES[i]);
                    transactionsMap.put(transaction.getId(), transaction);
                }

                MessageDigest digest = Crypto.sha256();
                for (Transaction transaction : transactionsMap.values()) {
                    digest.update(transaction.getBytes());
                }

                BlockImpl genesisBlock = new BlockImpl(-1, 0, null, 1000000000, 0, transactionsMap.size() * 128, digest.digest(),
                        Genesis.CREATOR_PUBLIC_KEY, new byte[64], Genesis.GENESIS_BLOCK_SIGNATURE, null, new ArrayList<>(transactionsMap.values()));

                genesisBlock.setPrevious(null);

                addBlock(genesisBlock);

            } catch (NxtException.ValidationException e) {
                Logger.logMessage(e.getMessage());
                throw new RuntimeException(e.toString(), e);
            }
        }

        Logger.logMessage("Scanning blockchain...");
        Blockchain.scan();
        Logger.logMessage("...Done scanning");
    }

    static {
        ThreadPool.scheduleThread(TransactionProcessor.processTransactionsThread, 5);
        ThreadPool.scheduleThread(TransactionProcessor.removeUnconfirmedTransactionsThread, 1);
        ThreadPool.scheduleThread(getMoreBlocksThread, 1);
        ThreadPool.scheduleThread(TransactionProcessor.rebroadcastTransactionsThread, 60);
    }

    public static boolean addBlockListener(Listener<Block> listener, Event eventType) {
        return blockListeners.addListener(listener, eventType);
    }

    public static boolean removeBlockListener(Listener<Block> listener, Event eventType) {
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
                    return Blocks.getBlock(con, rs);
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
                    return Blocks.getBlock(con, rs);
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
                    return Transactions.getTransaction(con, rs);
                }
            });
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<Transaction> getAllTransactions(Account account, byte type, byte subtype, int timestamp) {
        return getAllTransactions(account, type, subtype, timestamp, Boolean.TRUE);
    }

    public static DbIterator<Transaction> getAllTransactions(Account account, byte type, byte subtype, int timestamp, Boolean orderAscending) {
        Connection con = null;
        try {
            StringBuilder buf = new StringBuilder();
            if (orderAscending != null) {
                buf.append("SELECT * FROM (");
            }
            buf.append("SELECT * FROM transaction WHERE recipient_id = ? ");
            if (timestamp > 0) {
                buf.append("AND timestamp >= ? ");
            }
            if (type >= 0) {
                buf.append("AND type = ? ");
                if (subtype >= 0) {
                    buf.append("AND subtype = ? ");
                }
            }
            buf.append("UNION SELECT * FROM transaction WHERE sender_id = ? ");
            if (timestamp > 0) {
                buf.append("AND timestamp >= ? ");
            }
            if (type >= 0) {
                buf.append("AND type = ? ");
                if (subtype >= 0) {
                    buf.append("AND subtype = ? ");
                }
            }
            if (Boolean.TRUE.equals(orderAscending)) {
                buf.append(") ORDER BY timestamp ASC");
            } else if (Boolean.FALSE.equals(orderAscending)) {
                buf.append(") ORDER BY timestamp DESC");
            }
            con = Db.getConnection();
            PreparedStatement pstmt;
            int i = 0;
            pstmt = con.prepareStatement(buf.toString());
            pstmt.setLong(++i, account.getId());
            if (timestamp > 0) {
                pstmt.setInt(++i, timestamp);
            }
            if (type >= 0) {
                pstmt.setByte(++i, type);
                if (subtype >= 0) {
                    pstmt.setByte(++i, subtype);
                }
            }
            pstmt.setLong(++i, account.getId());
            if (timestamp > 0) {
                pstmt.setInt(++i, timestamp);
            }
            if (type >= 0) {
                pstmt.setByte(++i, type);
                if (subtype >= 0) {
                    pstmt.setByte(++i, subtype);
                }
            }
            return new DbIterator<>(con, pstmt, new DbIterator.ResultSetReader<Transaction>() {
                @Override
                public Transaction get(Connection con, ResultSet rs) throws NxtException.ValidationException {
                    return Transactions.getTransaction(con, rs);
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
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE db_id > (SELECT db_id FROM block WHERE id = ?) ORDER BY db_id ASC LIMIT ?")) {
            List<Block> result = new ArrayList<>();
            pstmt.setLong(1, blockId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(Blocks.getBlock(con, rs));
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
        return Blocks.findBlockIdAtHeight(height);
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
                result.add(Blocks.getBlock(con, rs));
            }
            return result;
        } catch (SQLException|NxtException.ValidationException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static Block getLastBlock() {
        return lastBlock.get();
    }

    public static Block getBlock(Long blockId) {
        return Blocks.findBlock(blockId);
    }

    public static boolean hasBlock(Long blockId) {
        return Blocks.hasBlock(blockId);
    }

    public static Transaction getTransaction(Long transactionId) {
        return Transactions.findTransaction(transactionId);
    }

    public static Peer getLastBlockchainFeeder() {
        return lastBlockchainFeeder;
    }

    public static boolean pushBlock(JSONObject request) throws NxtException {
        BlockImpl block = Blocks.getBlock(request);
        try {
            pushBlock(block);
            return true;
        } catch (BlockNotAcceptedException e) {
            Logger.logDebugMessage("Block " + block.getStringId() + " not accepted: " + e.getMessage());
            throw e;
        }
    }

    static void addBlock(BlockImpl block) {
        try (Connection con = Db.getConnection()) {
            try {
                Blocks.saveBlock(con, block);
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

    private static void pushBlock(final BlockImpl block) throws BlockNotAcceptedException {

        List<Transaction> addedConfirmedTransactions;
        List<Transaction> removedUnconfirmedTransactions;
        int curTime = Convert.getEpochTime();

        synchronized (Blockchain.class) {
            try {

                BlockImpl previousLastBlock = lastBlock.get();

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
                    throw new BlockOutOfOrderException("Invalid timestamp: " + block.getTimestamp()
                            + " current time is " + curTime + ", previous block timestamp is " + previousLastBlock.getTimestamp());
                }
                if (block.getId().equals(Long.valueOf(0L)) || Blocks.hasBlock(block.getId())) {
                    throw new BlockNotAcceptedException("Duplicate block or invalid id");
                }
                if (! block.verifyGenerationSignature() || ! block.verifyBlockSignature()) {
                    throw new BlockNotAcceptedException("Signature verification failed");
                }

                Map<TransactionType, Set<String>> duplicates = new HashMap<>();
                Map<Long, Long> accumulatedAmounts = new HashMap<>();
                Map<Long, Map<Long, Long>> accumulatedAssetQuantities = new HashMap<>();
                int calculatedTotalAmount = 0, calculatedTotalFee = 0;
                MessageDigest digest = Crypto.sha256();

                for (TransactionImpl transaction : block.getTransactions()) {

                    // cfb: Block 303 contains a transaction which expired before the block timestamp
                    if (transaction.getTimestamp() > curTime + 15 || transaction.getTimestamp() > block.getTimestamp() + 15
                            || (transaction.getExpiration() < block.getTimestamp() && previousLastBlock.getHeight() != 303)) {
                        throw new BlockNotAcceptedException("Invalid transaction timestamp " + transaction.getTimestamp()
                                + " for transaction " + transaction.getStringId() + ", current time is " + curTime
                                + ", block timestamp is " + block.getTimestamp());
                    }
                    if (Transactions.hasTransaction(transaction.getId())) {
                        throw new BlockNotAcceptedException("Transaction " + transaction.getStringId() + " is already in the blockchain");
                    }
                    if ((transaction.getReferencedTransactionId() != null
                            && ! Transactions.hasTransaction(transaction.getReferencedTransactionId())
                            && Collections.binarySearch(block.getTransactionIds(), transaction.getReferencedTransactionId()) < 0)) {
                        throw new BlockNotAcceptedException("Missing referenced transaction " + Convert.toUnsignedLong(transaction.getReferencedTransactionId())
                                +" for transaction " + transaction.getStringId());
                    }
                    if ((TransactionProcessor.unconfirmedTransactions.get(transaction.getId()) == null && !transaction.verify())) {
                        throw new BlockNotAcceptedException("Signature verification failed for transaction " + transaction.getStringId());
                    }
                    if (transaction.getId().equals(Long.valueOf(0L))) {
                        throw new BlockNotAcceptedException("Invalid transaction id");
                    }
                    if (transaction.isDuplicate(duplicates)) {
                        throw new BlockNotAcceptedException("Transaction is a duplicate: " + transaction.getStringId());
                    }
                    try {
                        transaction.validateAttachment();
                    } catch (NxtException.ValidationException e) {
                        throw new BlockNotAcceptedException(e.getMessage());
                    }

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
                        throw new BlockNotAcceptedException("Not enough funds in sender account: " + Convert.toUnsignedLong(senderAccount.getId()));
                    }
                }

                for (Map.Entry<Long, Map<Long, Long>> accumulatedAssetQuantitiesEntry : accumulatedAssetQuantities.entrySet()) {
                    Account senderAccount = Account.getAccount(accumulatedAssetQuantitiesEntry.getKey());
                    for (Map.Entry<Long, Long> accountAccumulatedAssetQuantitiesEntry : accumulatedAssetQuantitiesEntry.getValue().entrySet()) {
                        Long assetId = accountAccumulatedAssetQuantitiesEntry.getKey();
                        Long quantity = accountAccumulatedAssetQuantitiesEntry.getValue();
                        if (senderAccount.getAssetBalance(assetId) < quantity) {
                            throw new BlockNotAcceptedException("Asset balance not sufficient in sender account " + Convert.toUnsignedLong(senderAccount.getId()));
                        }
                    }
                }

                block.setPrevious(previousLastBlock);

                Transaction duplicateTransaction = null;
                for (Transaction transaction : block.getTransactions()) {
                    if (TransactionProcessor.transactionHashes.putIfAbsent(transaction.getHash(), transaction) != null && block.getHeight() != 58294) {
                        duplicateTransaction = transaction;
                        break;
                    }
                }

                if (duplicateTransaction != null) {
                    for (Transaction transaction : block.getTransactions()) {
                        if (! transaction.equals(duplicateTransaction)) {
                            Transaction hashTransaction = TransactionProcessor.transactionHashes.get(transaction.getHash());
                            if (hashTransaction != null && hashTransaction.equals(transaction)) {
                                TransactionProcessor.transactionHashes.remove(transaction.getHash());
                            }
                        }
                    }
                    throw new BlockNotAcceptedException("Duplicate hash of transaction " + duplicateTransaction.getStringId());
                }

                addBlock(block);

                block.apply();

                addedConfirmedTransactions = new ArrayList<>();
                removedUnconfirmedTransactions = new ArrayList<>();

                for (Transaction transaction : block.getTransactions()) {
                    addedConfirmedTransactions.add(transaction);
                    Transaction removedTransaction = TransactionProcessor.unconfirmedTransactions.remove(transaction.getId());
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
            Peers.sendToSomePeers(request);
        }

        if (removedUnconfirmedTransactions.size() > 0) {
            TransactionProcessor.transactionListeners.notify(removedUnconfirmedTransactions, Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
        }
        if (addedConfirmedTransactions.size() > 0) {
            TransactionProcessor.transactionListeners.notify(addedConfirmedTransactions, Event.ADDED_CONFIRMED_TRANSACTIONS);
        }
        blockListeners.notify(block, Event.BLOCK_PUSHED);
    }

    private static boolean popLastBlock() throws TransactionType.UndoNotSupportedException {

        try {

            List<Transaction> addedUnconfirmedTransactions = new ArrayList<>();
            BlockImpl block;

            synchronized (Blockchain.class) {
                block = lastBlock.get();
                Logger.logDebugMessage("Will pop block " + block.getStringId() + " at height " + block.getHeight());
                if (block.getId().equals(Genesis.GENESIS_BLOCK_ID)) {
                    return false;
                }
                BlockImpl previousBlock = Blocks.findBlock(block.getPreviousBlockId());
                if (previousBlock == null) {
                    Logger.logMessage("Previous block is null");
                    throw new IllegalStateException();
                }
                if (! lastBlock.compareAndSet(block, previousBlock)) {
                    Logger.logMessage("This block is no longer last block");
                    throw new IllegalStateException();
                }
                Account generatorAccount = Account.getAccount(block.getGeneratorId());
                generatorAccount.undo(block.getHeight());
                generatorAccount.addToBalanceAndUnconfirmedBalance(-block.getTotalFee() * 100L);
                for (TransactionImpl transaction : block.getTransactions()) {
                    Transaction hashTransaction = TransactionProcessor.transactionHashes.get(transaction.getHash());
                    if (hashTransaction != null && hashTransaction.equals(transaction)) {
                        TransactionProcessor.transactionHashes.remove(transaction.getHash());
                    }
                    TransactionProcessor.unconfirmedTransactions.put(transaction.getId(), transaction);
                    transaction.undo();
                    addedUnconfirmedTransactions.add(transaction);
                }
                Blocks.deleteBlock(block.getId());
            } // synchronized

            if (addedUnconfirmedTransactions.size() > 0) {
                TransactionProcessor.transactionListeners.notify(addedUnconfirmedTransactions, Event.ADDED_UNCONFIRMED_TRANSACTIONS);
            }

            blockListeners.notify(block, Event.BLOCK_POPPED);

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
        Poll.clear();
        Trade.clear();
        Vote.clear();
        TransactionProcessor.unconfirmedTransactions.clear();
        TransactionProcessor.doubleSpendingTransactions.clear();
        TransactionProcessor.nonBroadcastedTransactions.clear();
        TransactionProcessor.transactionHashes.clear();
        try (Connection con = Db.getConnection(); PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block ORDER BY db_id ASC")) {
            Long currentBlockId = Genesis.GENESIS_BLOCK_ID;
            BlockImpl currentBlock;
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                currentBlock = Blocks.getBlock(con, rs);
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

        Set<TransactionImpl> sortedTransactions = new TreeSet<>();

        for (TransactionImpl transaction : TransactionProcessor.unconfirmedTransactions.values()) {
            if (transaction.getReferencedTransactionId() == null || Transactions.hasTransaction(transaction.getReferencedTransactionId())) {
                sortedTransactions.add(transaction);
            }
        }

        SortedMap<Long, TransactionImpl> newTransactions = new TreeMap<>();
        Map<TransactionType, Set<String>> duplicates = new HashMap<>();
        Map<Long, Long> accumulatedAmounts = new HashMap<>();

        int totalAmount = 0;
        int totalFee = 0;
        int payloadLength = 0;

        int blockTimestamp = Convert.getEpochTime();

        while (payloadLength <= Nxt.MAX_PAYLOAD_LENGTH) {

            int prevNumberOfNewTransactions = newTransactions.size();

            for (TransactionImpl transaction : sortedTransactions) {

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

                        try {
                            transaction.validateAttachment();
                        } catch (NxtException.ValidationException e) {
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

        MessageDigest digest = Crypto.sha256();
        for (Transaction transaction : newTransactions.values()) {
            digest.update(transaction.getBytes());
        }

        byte[] payloadHash = digest.digest();
        byte[] generationSignature;

        BlockImpl previousBlock = lastBlock.get();
        if (previousBlock.getHeight() < Nxt.TRANSPARENT_FORGING_BLOCK) {
            generationSignature = Crypto.sign(previousBlock.getGenerationSignature(), secretPhrase);
        } else {
            digest.update(previousBlock.getGenerationSignature());
            generationSignature = digest.digest(publicKey);
        }

        BlockImpl block;

        try {
            if (previousBlock.getHeight() < Nxt.TRANSPARENT_FORGING_BLOCK) {
                block = new BlockImpl(1, blockTimestamp, previousBlock.getId(), totalAmount, totalFee,payloadLength,
                        payloadHash, publicKey, generationSignature, null, null, new ArrayList<>(newTransactions.values()));
            } else {
                byte[] previousBlockHash = Crypto.sha256().digest(previousBlock.getBytes());
                block = new BlockImpl(2, blockTimestamp, previousBlock.getId(), totalAmount, totalFee, payloadLength,
                        payloadHash, publicKey, generationSignature, null, previousBlockHash, new ArrayList<>(newTransactions.values()));
            }
        } catch (NxtException.ValidationException e) {
            // shouldn't happen because all transactions are already validated
            Logger.logMessage("Error generating block", e);
            return;
        }

        block.sign(secretPhrase);

        block.setPrevious(previousBlock);

        try {
            if (block.verifyBlockSignature() && block.verifyGenerationSignature()) {
                pushBlock(block);
                Logger.logDebugMessage("Account " + Convert.toUnsignedLong(block.getGeneratorId()) + " generated block " + block.getStringId());
            } else {
                Logger.logDebugMessage("Account " + Convert.toUnsignedLong(block.getGeneratorId()) + " generated an incorrect block.");
            }
        } catch (BlockNotAcceptedException e) {
            Logger.logDebugMessage("Generate block failed: " + e.getMessage());
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
