package nxt;

import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.user.User;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Blockchain {

    private static final byte[] CHECKSUM_TRANSPARENT_FORGING = new byte[]{27, -54, -59, -98, 49, -42, 48, -68, -112, 49, 41, 94, -41, 78, -84, 27, -87, -22, -28, 36, -34, -90, 112, -50, -9, 5, 89, -35, 80, -121, -128, 112};
    public static final ConcurrentMap<Long, AskOrder> askOrders = new ConcurrentHashMap<>();
    public static final ConcurrentMap<Long, BidOrder> bidOrders = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, TreeSet<AskOrder>> sortedAskOrders = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, TreeSet<BidOrder>> sortedBidOrders = new ConcurrentHashMap<>();

    static final Runnable processTransactionsThread = new Runnable() {

        private final JSONObject getUnconfirmedTransactionsRequest = new JSONObject();
        {
            getUnconfirmedTransactionsRequest.put("requestType", "getUnconfirmedTransactions");
        }

        @Override
        public void run() {

            try {

                Peer peer = Peer.getAnyPeer(Peer.STATE_CONNECTED, true);
                if (peer != null) {

                    JSONObject response = peer.send(getUnconfirmedTransactionsRequest);
                    if (response != null) {

                        Blockchain.processTransactions(response, "unconfirmedTransactions");

                    }

                }

            } catch (Exception e) {
                Logger.logDebugMessage("Error processing unconfirmed transactions from peer", e);
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

                int curTime = Convert.getEpochTime();
                JSONArray removedUnconfirmedTransactions = new JSONArray();

                Iterator<Transaction> iterator = Nxt.unconfirmedTransactions.values().iterator();
                while (iterator.hasNext()) {

                    Transaction transaction = iterator.next();
                    if (transaction.timestamp + transaction.deadline * 60 < curTime || !transaction.validateAttachment()) {

                        iterator.remove();

                        Account account = Nxt.accounts.get(transaction.getSenderAccountId());
                        account.addToUnconfirmedBalance((transaction.amount + transaction.fee) * 100L);

                        JSONObject removedUnconfirmedTransaction = new JSONObject();
                        removedUnconfirmedTransaction.put("index", transaction.index);
                        removedUnconfirmedTransactions.add(removedUnconfirmedTransaction);

                    }

                }

                if (removedUnconfirmedTransactions.size() > 0) {

                    JSONObject response = new JSONObject();
                    response.put("response", "processNewData");

                    response.put("removedUnconfirmedTransactions", removedUnconfirmedTransactions);

                    for (User user : Nxt.users.values()) {

                        user.send(response);

                    }

                }

            } catch (Exception e) {
                Logger.logDebugMessage("Error removing unconfirmed transactions", e);
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    static final Runnable getMoreBlocksThread = new Runnable() {

        private final JSONObject getCumulativeDifficultyRequest = new JSONObject();
        private final JSONObject getMilestoneBlockIdsRequest = new JSONObject();
        {
            getCumulativeDifficultyRequest.put("requestType", "getCumulativeDifficulty");
            getMilestoneBlockIdsRequest.put("requestType", "getMilestoneBlockIds");
        }

        @Override
        public void run() {

            try {

                Peer peer = Peer.getAnyPeer(Peer.STATE_CONNECTED, true);
                if (peer != null) {

                    Nxt.lastBlockchainFeeder = peer;

                    JSONObject response = peer.send(getCumulativeDifficultyRequest);
                    if (response != null) {

                        BigInteger curCumulativeDifficulty = Nxt.lastBlock.get().cumulativeDifficulty;
                        String peerCumulativeDifficulty = (String)response.get("cumulativeDifficulty");
                        if (peerCumulativeDifficulty == null) {
                            return;
                        }
                        BigInteger betterCumulativeDifficulty = new BigInteger(peerCumulativeDifficulty);
                        if (betterCumulativeDifficulty.compareTo(curCumulativeDifficulty) > 0) {

                            response = peer.send(getMilestoneBlockIdsRequest);
                            if (response != null) {

                                long commonBlockId = Genesis.GENESIS_BLOCK_ID;

                                JSONArray milestoneBlockIds = (JSONArray)response.get("milestoneBlockIds");
                                for (Object milestoneBlockId : milestoneBlockIds) {

                                    long blockId = Convert.parseUnsignedLong((String) milestoneBlockId);
                                    Block block = Nxt.blocks.get(blockId);
                                    if (block != null) {

                                        commonBlockId = blockId;

                                        break;

                                    }

                                }

                                int i, numberOfBlocks;
                                do {

                                    JSONObject request = new JSONObject();
                                    request.put("requestType", "getNextBlockIds");
                                    request.put("blockId", Convert.convert(commonBlockId));
                                    response = peer.send(request);
                                    if (response == null) {

                                        return;

                                    } else {

                                        JSONArray nextBlockIds = (JSONArray)response.get("nextBlockIds");
                                        numberOfBlocks = nextBlockIds.size();
                                        if (numberOfBlocks == 0) {

                                            return;

                                        } else {

                                            long blockId;
                                            for (i = 0; i < numberOfBlocks; i++) {

                                                blockId = Convert.parseUnsignedLong((String) nextBlockIds.get(i));
                                                if (Nxt.blocks.get(blockId) == null) {

                                                    break;

                                                }

                                                commonBlockId = blockId;

                                            }

                                        }

                                    }

                                } while (i == numberOfBlocks);

                                if (Nxt.lastBlock.get().height - Nxt.blocks.get(commonBlockId).height < 720) {

                                    long curBlockId = commonBlockId;
                                    LinkedList<Block> futureBlocks = new LinkedList<>();
                                    HashMap<Long, Transaction> futureTransactions = new HashMap<>();

                                    do {

                                        JSONObject request = new JSONObject();
                                        request.put("requestType", "getNextBlocks");
                                        request.put("blockId", Convert.convert(curBlockId));
                                        response = peer.send(request);
                                        if (response == null) {

                                            break;

                                        } else {

                                            JSONArray nextBlocks = (JSONArray)response.get("nextBlocks");
                                            numberOfBlocks = nextBlocks.size();
                                            if (numberOfBlocks == 0) {

                                                break;

                                            } else {

                                                synchronized (Blockchain.class) {
                                                    for (i = 0; i < numberOfBlocks; i++) {

                                                        JSONObject blockData = (JSONObject)nextBlocks.get(i);
                                                        Block block = Block.getBlock(blockData);
                                                        if (block == null) {

                                                            // peer tried to send us invalid transactions length or payload parameters
                                                            peer.blacklist();
                                                            return;

                                                        }

                                                        curBlockId = block.getId();

                                                        boolean alreadyPushed = false;
                                                        if (block.previousBlock == Nxt.lastBlock.get().getId()) {

                                                            ByteBuffer buffer = ByteBuffer.allocate(Nxt.BLOCK_HEADER_LENGTH + block.payloadLength);
                                                            buffer.order(ByteOrder.LITTLE_ENDIAN);
                                                            buffer.put(block.getBytes());

                                                            JSONArray transactionsData = (JSONArray)blockData.get("transactions");
                                                            for (Object transaction : transactionsData) {

                                                                buffer.put(Transaction.getTransaction((JSONObject)transaction).getBytes());

                                                            }

                                                            if (Blockchain.pushBlock(buffer, false)) {

                                                                alreadyPushed = true;

                                                            } else {

                                                                peer.blacklist();

                                                                return;

                                                            }

                                                        }
                                                        if (!alreadyPushed && Nxt.blocks.get(block.getId()) == null && block.transactions.length <= Nxt.MAX_NUMBER_OF_TRANSACTIONS) {

                                                            futureBlocks.add(block);

                                                            JSONArray transactionsData = (JSONArray)blockData.get("transactions");
                                                            for (int j = 0; j < block.transactions.length; j++) {

                                                                Transaction transaction = Transaction.getTransaction((JSONObject)transactionsData.get(j));
                                                                block.transactions[j] = transaction.getId();
                                                                block.blockTransactions[j] = transaction;
                                                                futureTransactions.put(block.transactions[j], transaction);

                                                            }

                                                        }

                                                    }

                                                } //synchronized
                                            }

                                        }

                                    } while (true);

                                    if (!futureBlocks.isEmpty() && Nxt.lastBlock.get().height - Nxt.blocks.get(commonBlockId).height < 720) {

                                        synchronized (Blockchain.class) {

                                            Block.saveBlocks("blocks.nxt.bak");
                                            Transaction.saveTransactions("transactions.nxt.bak");

                                            curCumulativeDifficulty = Nxt.lastBlock.get().cumulativeDifficulty;

                                            while (Nxt.lastBlock.get().getId() != commonBlockId && Blockchain.popLastBlock()) {}

                                            if (Nxt.lastBlock.get().getId() == commonBlockId) {

                                                for (Block block : futureBlocks) {

                                                    if (block.previousBlock == Nxt.lastBlock.get().getId()) {

                                                        ByteBuffer buffer = ByteBuffer.allocate(Nxt.BLOCK_HEADER_LENGTH + block.payloadLength);
                                                        buffer.order(ByteOrder.LITTLE_ENDIAN);
                                                        buffer.put(block.getBytes());

                                                        for (Transaction transaction : block.blockTransactions) {

                                                            buffer.put(transaction.getBytes());

                                                        }

                                                        if (!Blockchain.pushBlock(buffer, false)) {

                                                            break;

                                                        }

                                                    }

                                                }

                                            }

                                            if (Nxt.lastBlock.get().cumulativeDifficulty.compareTo(curCumulativeDifficulty) < 0) {

                                                Block.loadBlocks("blocks.nxt.bak");
                                                Transaction.loadTransactions("transactions.nxt.bak");

                                                peer.blacklist();

                                                Nxt.accounts.clear();
                                                Nxt.aliases.clear();
                                                Nxt.aliasIdToAliasMappings.clear();
                                                Nxt.unconfirmedTransactions.clear();
                                                Nxt.doubleSpendingTransactions.clear();
                                                Logger.logMessage("Re-scanning blockchain...");
                                                Blockchain.scan();
                                                Logger.logMessage("...Done");


                                            }

                                        }

                                    }

                                    synchronized (Blockchain.class) {
                                        Block.saveBlocks("blocks.nxt");
                                        Transaction.saveTransactions("transactions.nxt");
                                    }

                                }

                            }

                        }

                    }

                }

            } catch (Exception e) {
                Logger.logDebugMessage("Error in milestone blocks processing thread", e);
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

                HashMap<Account, User> unlockedAccounts = new HashMap<>();
                for (User user : Nxt.users.values()) {

                    if (user.secretPhrase != null) {

                        Account account = Nxt.accounts.get(Account.getId(user.publicKey));
                        if (account != null && account.getEffectiveBalance() > 0) {

                            unlockedAccounts.put(account, user);

                        }

                    }

                }

                for (Map.Entry<Account, User> unlockedAccountEntry : unlockedAccounts.entrySet()) {

                    Account account = unlockedAccountEntry.getKey();
                    User user = unlockedAccountEntry.getValue();
                    Block lastBlock = Nxt.lastBlock.get();
                    if (lastBlocks.get(account) != lastBlock) {

                        long effectiveBalance = account.getEffectiveBalance();
                        if (effectiveBalance <= 0) {
                            continue;
                        }
                        MessageDigest digest = Crypto.sha256();
                        byte[] generationSignatureHash;
                        if (lastBlock.height < Nxt.TRANSPARENT_FORGING_BLOCK) {

                            byte[] generationSignature = Crypto.sign(lastBlock.generationSignature, user.secretPhrase);
                            generationSignatureHash = digest.digest(generationSignature);

                        } else {

                            digest.update(lastBlock.generationSignature);
                            generationSignatureHash = digest.digest(user.publicKey);

                        }
                        BigInteger hit = new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});

                        lastBlocks.put(account, lastBlock);
                        hits.put(account, hit);

                        JSONObject response = new JSONObject();
                        response.put("response", "setBlockGenerationDeadline");
                        response.put("deadline", hit.divide(BigInteger.valueOf(lastBlock.baseTarget).multiply(BigInteger.valueOf(effectiveBalance))).longValue() - (Convert.getEpochTime() - lastBlock.timestamp));

                        user.send(response);

                    }

                    int elapsedTime = Convert.getEpochTime() - lastBlock.timestamp;
                    if (elapsedTime > 0) {

                        BigInteger target = BigInteger.valueOf(lastBlock.baseTarget).multiply(BigInteger.valueOf(account.getEffectiveBalance())).multiply(BigInteger.valueOf(elapsedTime));
                        if (hits.get(account).compareTo(target) < 0) {

                            Blockchain.generateBlock(user.secretPhrase);

                        }

                    }

                }

            } catch (Exception e) {
                Logger.logDebugMessage("Error in block generation thread", e);
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

                JSONArray transactionsData = new JSONArray();

                for (Transaction transaction : Nxt.nonBroadcastedTransactions.values()) {

                    if (Nxt.unconfirmedTransactions.get(transaction.id) == null && Nxt.transactions.get(transaction.id) == null) {

                        transactionsData.add(transaction.getJSONObject());

                    } else {

                        Nxt.nonBroadcastedTransactions.remove(transaction.id);

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
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    public static void processTransactions(JSONObject request, String parameterName) {

        JSONArray transactionsData = (JSONArray)request.get(parameterName);
        JSONArray validTransactionsData = new JSONArray();

        for (Object transactionData : transactionsData) {

            Transaction transaction = Transaction.getTransaction((JSONObject) transactionData);

            try {

                int curTime = Convert.getEpochTime();
                if (transaction.timestamp > curTime + 15 || transaction.deadline < 1 || transaction.timestamp + transaction.deadline * 60 < curTime || transaction.fee <= 0 || !transaction.validateAttachment()) {

                    continue;

                }

                long senderId;
                boolean doubleSpendingTransaction;

                synchronized (Blockchain.class) {

                    long id = transaction.getId();
                    if (Nxt.transactions.get(id) != null || Nxt.unconfirmedTransactions.get(id) != null || Nxt.doubleSpendingTransactions.get(id) != null || !transaction.verify()) {

                        continue;

                    }

                    senderId = transaction.getSenderAccountId();
                    Account account = Nxt.accounts.get(senderId);
                    if (account == null) {

                        doubleSpendingTransaction = true;

                    } else {

                        int amount = transaction.amount + transaction.fee;
                        synchronized (account) {

                            if (account.getUnconfirmedBalance() < amount * 100L) {

                                doubleSpendingTransaction = true;

                            } else {

                                doubleSpendingTransaction = false;

                                account.addToUnconfirmedBalance(- amount * 100L);

                                if (transaction.type == Transaction.TYPE_COLORED_COINS) {

                                    if (transaction.subtype == Transaction.SUBTYPE_COLORED_COINS_ASSET_TRANSFER) {

                                        Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.attachment;
                                        Integer unconfirmedAssetBalance = account.getUnconfirmedAssetBalance(attachment.asset);
                                        if (unconfirmedAssetBalance == null || unconfirmedAssetBalance < attachment.quantity) {

                                            doubleSpendingTransaction = true;

                                            account.addToUnconfirmedBalance(amount * 100L);

                                        } else {

                                            account.addToUnconfirmedAssetBalance(attachment.asset, -attachment.quantity);

                                        }

                                    } else if (transaction.subtype == Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT) {

                                        Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement)transaction.attachment;
                                        Integer unconfirmedAssetBalance = account.getUnconfirmedAssetBalance(attachment.asset);
                                        if (unconfirmedAssetBalance == null || unconfirmedAssetBalance < attachment.quantity) {

                                            doubleSpendingTransaction = true;

                                            account.addToUnconfirmedBalance(amount * 100L);

                                        } else {

                                            account.addToUnconfirmedAssetBalance(attachment.asset, -attachment.quantity);

                                        }

                                    } else if (transaction.subtype == Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT) {

                                        Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement)transaction.attachment;
                                        if (account.getUnconfirmedBalance() < attachment.quantity * attachment.price) {

                                            doubleSpendingTransaction = true;

                                            account.addToUnconfirmedBalance(amount * 100L);

                                        } else {

                                            account.addToUnconfirmedBalance(- attachment.quantity * attachment.price);

                                        }

                                    }

                                }

                            }

                        }

                    }

                    transaction.index = Nxt.transactionCounter.incrementAndGet();

                    if (doubleSpendingTransaction) {

                        Nxt.doubleSpendingTransactions.put(transaction.getId(), transaction);

                    } else {

                        Nxt.unconfirmedTransactions.put(transaction.getId(), transaction);

                        if (parameterName.equals("transactions")) {

                            validTransactionsData.add(transactionData);

                        }

                    }

                }

                JSONObject response = new JSONObject();
                response.put("response", "processNewData");

                JSONArray newTransactions = new JSONArray();
                JSONObject newTransaction = new JSONObject();
                newTransaction.put("index", transaction.index);
                newTransaction.put("timestamp", transaction.timestamp);
                newTransaction.put("deadline", transaction.deadline);
                newTransaction.put("recipient", Convert.convert(transaction.recipient));
                newTransaction.put("amount", transaction.amount);
                newTransaction.put("fee", transaction.fee);
                newTransaction.put("sender", Convert.convert(senderId));
                newTransaction.put("id", transaction.getStringId());
                newTransactions.add(newTransaction);

                if (doubleSpendingTransaction) {

                    response.put("addedDoubleSpendingTransactions", newTransactions);

                } else {

                    response.put("addedUnconfirmedTransactions", newTransactions);

                }

                for (User user : Nxt.users.values()) {

                    user.send(response);

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

    }

    private synchronized static byte[] calculateTransactionsChecksum() {
        PriorityQueue<Transaction> sortedTransactions = new PriorityQueue<>(Nxt.transactions.size(), new Comparator<Transaction>() {
            @Override
            public int compare(Transaction o1, Transaction o2) {
                long id1 = o1.getId();
                long id2 = o2.getId();
                return id1 < id2 ? -1 : (id1 > id2 ? 1 : (o1.timestamp < o2.timestamp ? -1 : (o1.timestamp > o2.timestamp ? 1 : 0)));
            }
        });
        sortedTransactions.addAll(Nxt.transactions.values());
        MessageDigest digest = Crypto.sha256();
        while (! sortedTransactions.isEmpty()) {
            digest.update(sortedTransactions.poll().getBytes());
        }
        return digest.digest();
    }

    static ArrayList<Block> getLastBlocks(int numberOfBlocks) {

        ArrayList<Block> lastBlocks = new ArrayList<>(numberOfBlocks);

        long curBlock = Nxt.lastBlock.get().getId();
        do {

            Block block = Nxt.blocks.get(curBlock);
            lastBlocks.add(block);
            curBlock = block.previousBlock;

        } while (lastBlocks.size() < numberOfBlocks && curBlock != 0);

        return lastBlocks;

    }

    private static boolean popLastBlock() {

        try {

            JSONObject response = new JSONObject();
            response.put("response", "processNewData");

            JSONArray addedUnconfirmedTransactions = new JSONArray();

            Block block;

            synchronized (Blockchain.class) {

                block = Nxt.lastBlock.get();

                if (block.getId() == Genesis.GENESIS_BLOCK_ID) {
                    return false;
                }

                Block previousBlock = Nxt.blocks.get(block.previousBlock);
                if (previousBlock == null) {
                    Logger.logMessage("Previous block is null");
                    throw new IllegalStateException();
                }
                if (! Nxt.lastBlock.compareAndSet(block, previousBlock)) {
                    Logger.logMessage("This block is no longer last block");
                    throw new IllegalStateException();
                }

                Account generatorAccount = Nxt.accounts.get(block.getGeneratorAccountId());
                generatorAccount.addToBalanceAndUnconfirmedBalance(- block.totalFee * 100L);

                for (long transactionId : block.transactions) {

                    Transaction transaction = Nxt.transactions.remove(transactionId);
                    Nxt.unconfirmedTransactions.put(transactionId, transaction);

                    Account senderAccount = Nxt.accounts.get(transaction.getSenderAccountId());
                    senderAccount.addToBalance((transaction.amount + transaction.fee) * 100L);

                    Account recipientAccount = Nxt.accounts.get(transaction.recipient);
                    recipientAccount.addToBalanceAndUnconfirmedBalance(- transaction.amount * 100L);

                    JSONObject addedUnconfirmedTransaction = new JSONObject();
                    addedUnconfirmedTransaction.put("index", transaction.index);
                    addedUnconfirmedTransaction.put("timestamp", transaction.timestamp);
                    addedUnconfirmedTransaction.put("deadline", transaction.deadline);
                    addedUnconfirmedTransaction.put("recipient", Convert.convert(transaction.recipient));
                    addedUnconfirmedTransaction.put("amount", transaction.amount);
                    addedUnconfirmedTransaction.put("fee", transaction.fee);
                    addedUnconfirmedTransaction.put("sender", Convert.convert(transaction.getSenderAccountId()));
                    addedUnconfirmedTransaction.put("id", transaction.getStringId());
                    addedUnconfirmedTransactions.add(addedUnconfirmedTransaction);

                }

            } // synchronized

            JSONArray addedOrphanedBlocks = new JSONArray();
            JSONObject addedOrphanedBlock = new JSONObject();
            addedOrphanedBlock.put("index", block.index);
            addedOrphanedBlock.put("timestamp", block.timestamp);
            addedOrphanedBlock.put("numberOfTransactions", block.transactions.length);
            addedOrphanedBlock.put("totalAmount", block.totalAmount);
            addedOrphanedBlock.put("totalFee", block.totalFee);
            addedOrphanedBlock.put("payloadLength", block.payloadLength);
            addedOrphanedBlock.put("generator", Convert.convert(block.getGeneratorAccountId()));
            addedOrphanedBlock.put("height", block.height);
            addedOrphanedBlock.put("version", block.version);
            addedOrphanedBlock.put("block", block.getStringId());
            addedOrphanedBlock.put("baseTarget", BigInteger.valueOf(block.baseTarget).multiply(BigInteger.valueOf(100000)).divide(BigInteger.valueOf(Nxt.initialBaseTarget)));
            addedOrphanedBlocks.add(addedOrphanedBlock);
            response.put("addedOrphanedBlocks", addedOrphanedBlocks);

            if (addedUnconfirmedTransactions.size() > 0) {

                response.put("addedUnconfirmedTransactions", addedUnconfirmedTransactions);

            }

            for (User user : Nxt.users.values()) {

                user.send(response);

            }

        } catch (RuntimeException e) {

            Logger.logMessage("Error popping last block", e);

            return false;

        }

        return true;

    }

    public static boolean pushBlock(ByteBuffer buffer, boolean savingFlag) {

        Block block;
        JSONArray addedConfirmedTransactions;
        JSONArray removedUnconfirmedTransactions;
        int curTime = Convert.getEpochTime();

        synchronized (Blockchain.class) {
            try {

                Block previousLastBlock = Nxt.lastBlock.get();
                buffer.flip();

                int version = buffer.getInt();
                if (version != (previousLastBlock.height < Nxt.TRANSPARENT_FORGING_BLOCK ? 1 : 2)) {

                    return false;

                }

                if (previousLastBlock.height == Nxt.TRANSPARENT_FORGING_BLOCK) {

                    byte[] checksum = calculateTransactionsChecksum();
                    if (CHECKSUM_TRANSPARENT_FORGING == null) {
                        System.out.println(Arrays.toString(checksum));
                    } else if (!Arrays.equals(checksum, CHECKSUM_TRANSPARENT_FORGING)) {
                        Logger.logMessage("Checksum failed at block " + Nxt.TRANSPARENT_FORGING_BLOCK);
                        return false;
                    } else {
                        Logger.logMessage("Checksum passed at block " + Nxt.TRANSPARENT_FORGING_BLOCK);
                    }

                }

                int blockTimestamp = buffer.getInt();
                long previousBlock = buffer.getLong();
                int numberOfTransactions = buffer.getInt();
                int totalAmount = buffer.getInt();
                int totalFee = buffer.getInt();
                int payloadLength = buffer.getInt();
                byte[] payloadHash = new byte[32];
                buffer.get(payloadHash);
                byte[] generatorPublicKey = new byte[32];
                buffer.get(generatorPublicKey);
                byte[] generationSignature;
                byte[] previousBlockHash;
                if (version == 1) {

                    generationSignature = new byte[64];
                    buffer.get(generationSignature);
                    previousBlockHash = null;

                } else {

                    generationSignature = new byte[32];
                    buffer.get(generationSignature);
                    previousBlockHash = new byte[32];
                    buffer.get(previousBlockHash);

                    if (!Arrays.equals(Crypto.sha256().digest(previousLastBlock.getBytes()), previousBlockHash)) {

                        return false;

                    }

                }
                byte[] blockSignature = new byte[64];
                buffer.get(blockSignature);

                if (blockTimestamp > curTime + 15 || blockTimestamp <= previousLastBlock.timestamp) {

                    return false;

                }

                if (payloadLength > Nxt.MAX_PAYLOAD_LENGTH || Nxt.BLOCK_HEADER_LENGTH + payloadLength != buffer.capacity() || numberOfTransactions > Nxt.MAX_NUMBER_OF_TRANSACTIONS) {

                    return false;

                }

                block = new Block(version, blockTimestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash);

                if (block.transactions.length > Nxt.MAX_NUMBER_OF_TRANSACTIONS || block.previousBlock != previousLastBlock.getId() || block.getId() == 0L || Nxt.blocks.get(block.getId()) != null || !block.verifyGenerationSignature() || !block.verifyBlockSignature()) {

                    return false;

                }

                block.index = Nxt.blockCounter.incrementAndGet();

                HashMap<Long, Transaction> blockTransactions = new HashMap<>();
                HashSet<String> blockAliases = new HashSet<>();
                for (int i = 0; i < block.transactions.length; i++) {

                    Transaction transaction = Transaction.getTransaction(buffer);
                    transaction.index = Nxt.transactionCounter.incrementAndGet();

                    if (blockTransactions.put(block.transactions[i] = transaction.getId(), transaction) != null) {

                        return false;

                    }

                    switch (transaction.type) {

                        case Transaction.TYPE_MESSAGING:
                        {

                            switch (transaction.subtype) {

                                case Transaction.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
                                {

                                    if (!blockAliases.add(((Attachment.MessagingAliasAssignment)transaction.attachment).alias.toLowerCase())) {

                                        return false;

                                    }

                                }
                                break;

                            }

                        }
                        break;

                    }

                }
                Arrays.sort(block.transactions);

                HashMap<Long, Long> accumulatedAmounts = new HashMap<>();
                HashMap<Long, HashMap<Long, Long>> accumulatedAssetQuantities = new HashMap<>();
                int calculatedTotalAmount = 0, calculatedTotalFee = 0;
                MessageDigest digest = Crypto.sha256();
                int i;
                for (i = 0; i < block.transactions.length; i++) {

                    Transaction transaction = blockTransactions.get(block.transactions[i]);
                    // cfb: Block 303 contains a transaction which expired before the block timestamp
                    //TODO: similar transaction validation is done in several places, refactor common code out
                    if (transaction.timestamp > curTime + 15 || transaction.deadline < 1 || (transaction.timestamp + transaction.deadline * 60 < blockTimestamp && previousLastBlock.height > 303) || transaction.fee <= 0 || transaction.fee > Nxt.MAX_BALANCE || transaction.amount < 0 || transaction.amount > Nxt.MAX_BALANCE || !transaction.validateAttachment() || Nxt.transactions.get(block.transactions[i]) != null || (transaction.referencedTransaction != 0 && Nxt.transactions.get(transaction.referencedTransaction) == null && blockTransactions.get(transaction.referencedTransaction) == null) || (Nxt.unconfirmedTransactions.get(block.transactions[i]) == null && !transaction.verify())) {

                        break;

                    }

                    long sender = transaction.getSenderAccountId();
                    Long accumulatedAmount = accumulatedAmounts.get(sender);
                    if (accumulatedAmount == null) {

                        accumulatedAmount = 0L;

                    }
                    accumulatedAmounts.put(sender, accumulatedAmount + (transaction.amount + transaction.fee) * 100L);
                    if (transaction.type == Transaction.TYPE_PAYMENT) {

                        if (transaction.subtype == Transaction.SUBTYPE_PAYMENT_ORDINARY_PAYMENT) {

                            calculatedTotalAmount += transaction.amount;

                        } else {

                            break;

                        }

                    } else if (transaction.type == Transaction.TYPE_MESSAGING) {

                        if (transaction.subtype != Transaction.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE && transaction.subtype != Transaction.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT) {

                            break;

                        }

                    } else if (transaction.type == Transaction.TYPE_COLORED_COINS) {

                        if (transaction.subtype == Transaction.SUBTYPE_COLORED_COINS_ASSET_TRANSFER) {

                            Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.attachment;
                            HashMap<Long, Long> accountAccumulatedAssetQuantities = accumulatedAssetQuantities.get(sender);
                            if (accountAccumulatedAssetQuantities == null) {

                                accountAccumulatedAssetQuantities = new HashMap<>();
                                accumulatedAssetQuantities.put(sender, accountAccumulatedAssetQuantities);

                            }
                            Long assetAccumulatedAssetQuantities = accountAccumulatedAssetQuantities.get(attachment.asset);
                            if (assetAccumulatedAssetQuantities == null) {

                                assetAccumulatedAssetQuantities = 0L;

                            }
                            accountAccumulatedAssetQuantities.put(attachment.asset, assetAccumulatedAssetQuantities + attachment.quantity);

                        } else if (transaction.subtype == Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT) {

                            Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement)transaction.attachment;
                            HashMap<Long, Long> accountAccumulatedAssetQuantities = accumulatedAssetQuantities.get(sender);
                            if (accountAccumulatedAssetQuantities == null) {

                                accountAccumulatedAssetQuantities = new HashMap<>();
                                accumulatedAssetQuantities.put(sender, accountAccumulatedAssetQuantities);

                            }
                            Long assetAccumulatedAssetQuantities = accountAccumulatedAssetQuantities.get(attachment.asset);
                            if (assetAccumulatedAssetQuantities == null) {

                                assetAccumulatedAssetQuantities = 0L;

                            }
                            accountAccumulatedAssetQuantities.put(attachment.asset, assetAccumulatedAssetQuantities + attachment.quantity);

                        } else if (transaction.subtype == Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT) {

                            Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement)transaction.attachment;
                            accumulatedAmounts.put(sender, accumulatedAmount + attachment.quantity * attachment.price);

                        } else if (transaction.subtype != Transaction.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE && transaction.subtype != Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION && transaction.subtype != Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION) {

                            break;

                        }

                    } else {

                        break;

                    }
                    calculatedTotalFee += transaction.fee;

                    digest.update(transaction.getBytes());

                }

                if (i != block.transactions.length || calculatedTotalAmount != block.totalAmount || calculatedTotalFee != block.totalFee) {

                    return false;

                }

                if (!Arrays.equals(digest.digest(), block.payloadHash)) {

                    return false;

                }

                for (Map.Entry<Long, Long> accumulatedAmountEntry : accumulatedAmounts.entrySet()) {

                    Account senderAccount = Nxt.accounts.get(accumulatedAmountEntry.getKey());
                    if (senderAccount.getBalance() < accumulatedAmountEntry.getValue()) {

                        return false;

                    }

                }

                for (Map.Entry<Long, HashMap<Long, Long>> accumulatedAssetQuantitiesEntry : accumulatedAssetQuantities.entrySet()) {

                    Account senderAccount = Nxt.accounts.get(accumulatedAssetQuantitiesEntry.getKey());
                    for (Map.Entry<Long, Long> accountAccumulatedAssetQuantitiesEntry : accumulatedAssetQuantitiesEntry.getValue().entrySet()) {

                        long asset = accountAccumulatedAssetQuantitiesEntry.getKey();
                        long quantity = accountAccumulatedAssetQuantitiesEntry.getValue();
                        if (senderAccount.getAssetBalance(asset) < quantity) {

                            return false;

                        }

                    }

                }

                block.height = previousLastBlock.height + 1;

                for (Map.Entry<Long, Transaction> transactionEntry : blockTransactions.entrySet()) {

                    Transaction transaction = transactionEntry.getValue();
                    transaction.height = block.height;
                    transaction.block = block.getId();

                    if (Nxt.transactions.putIfAbsent(transactionEntry.getKey(), transaction) != null) {
                        Logger.logMessage("duplicate transaction id " + transactionEntry.getKey());
                        return false;
                    }

                }

                analyze(block);

                addedConfirmedTransactions = new JSONArray();
                removedUnconfirmedTransactions = new JSONArray();

                for (Map.Entry<Long, Transaction> transactionEntry : blockTransactions.entrySet()) {

                    Transaction transaction = transactionEntry.getValue();

                    JSONObject addedConfirmedTransaction = new JSONObject();
                    addedConfirmedTransaction.put("index", transaction.index);
                    addedConfirmedTransaction.put("blockTimestamp", block.timestamp);
                    addedConfirmedTransaction.put("transactionTimestamp", transaction.timestamp);
                    addedConfirmedTransaction.put("sender", Convert.convert(transaction.getSenderAccountId()));
                    addedConfirmedTransaction.put("recipient", Convert.convert(transaction.recipient));
                    addedConfirmedTransaction.put("amount", transaction.amount);
                    addedConfirmedTransaction.put("fee", transaction.fee);
                    addedConfirmedTransaction.put("id", transaction.getStringId());
                    addedConfirmedTransactions.add(addedConfirmedTransaction);

                    Transaction removedTransaction = Nxt.unconfirmedTransactions.remove(transactionEntry.getKey());
                    if (removedTransaction != null) {

                        JSONObject removedUnconfirmedTransaction = new JSONObject();
                        removedUnconfirmedTransaction.put("index", removedTransaction.index);
                        removedUnconfirmedTransactions.add(removedUnconfirmedTransaction);

                        Account senderAccount = Nxt.accounts.get(removedTransaction.getSenderAccountId());
                        senderAccount.addToUnconfirmedBalance((removedTransaction.amount + removedTransaction.fee) * 100L);

                    }

                    // TODO: Remove from double-spending transactions

                }

                if (savingFlag) {

                    Transaction.saveTransactions("transactions.nxt");
                    Block.saveBlocks("blocks.nxt");

                }

            } catch (RuntimeException e) {
                Logger.logMessage("Error pushing block", e);
                return false;
            }
        } // synchronized
        if (block.timestamp >= curTime - 15) {

            JSONObject request = block.getJSONObject();
            request.put("requestType", "processBlock");

            Peer.sendToSomePeers(request);

        }

        JSONArray addedRecentBlocks = new JSONArray();
        JSONObject addedRecentBlock = new JSONObject();
        addedRecentBlock.put("index", block.index);
        addedRecentBlock.put("timestamp", block.timestamp);
        addedRecentBlock.put("numberOfTransactions", block.transactions.length);
        addedRecentBlock.put("totalAmount", block.totalAmount);
        addedRecentBlock.put("totalFee", block.totalFee);
        addedRecentBlock.put("payloadLength", block.payloadLength);
        addedRecentBlock.put("generator", Convert.convert(block.getGeneratorAccountId()));
        addedRecentBlock.put("height", block.height);
        addedRecentBlock.put("version", block.version);
        addedRecentBlock.put("block", block.getStringId());
        addedRecentBlock.put("baseTarget", BigInteger.valueOf(block.baseTarget).multiply(BigInteger.valueOf(100000)).divide(BigInteger.valueOf(Nxt.initialBaseTarget)));
        addedRecentBlocks.add(addedRecentBlock);

        JSONObject response = new JSONObject();
        response.put("response", "processNewData");
        response.put("addedConfirmedTransactions", addedConfirmedTransactions);
        if (removedUnconfirmedTransactions.size() > 0) {

            response.put("removedUnconfirmedTransactions", removedUnconfirmedTransactions);

        }
        response.put("addedRecentBlocks", addedRecentBlocks);

        for (User user : Nxt.users.values()) {

            user.send(response);

        }

        return true;


    }

    private synchronized static void analyze(Block block) {

        for (int i = 0; i < block.transactions.length; i++) {
            block.blockTransactions[i] = Nxt.transactions.get(block.transactions[i]);
            if (block.blockTransactions[i] == null) {
                throw new IllegalStateException("Missing transaction " + Convert.convert(block.transactions[i]));
            }
        }
        if (block.previousBlock == 0) {

            block.baseTarget = Nxt.initialBaseTarget;
            block.cumulativeDifficulty = BigInteger.ZERO;
            Nxt.blocks.put(Genesis.GENESIS_BLOCK_ID, block);
            Nxt.lastBlock.set(block);

            Account.addAccount(Genesis.CREATOR_ID);

        } else {

            Block previousLastBlock = Nxt.lastBlock.get();

            previousLastBlock.nextBlock = block.getId();
            block.height = previousLastBlock.height + 1;
            block.baseTarget = calculateBaseTarget(block);
            block.cumulativeDifficulty = previousLastBlock.cumulativeDifficulty.add(Convert.two64.divide(BigInteger.valueOf(block.baseTarget)));
            if (! (previousLastBlock.getId() == block.previousBlock && Nxt.lastBlock.compareAndSet(previousLastBlock, block))) {
                throw new IllegalStateException("Last block not equal to this.previousBlock"); // shouldn't happen
            }
            Account generatorAccount = Nxt.accounts.get(block.getGeneratorAccountId());
            generatorAccount.addToBalanceAndUnconfirmedBalance(block.totalFee * 100L);
            if (Nxt.blocks.putIfAbsent(block.getId(), block) != null) {
                throw new IllegalStateException("duplicate block id: " + block.getId()); // shouldn't happen
            }
        }

        for (Transaction transaction : block.blockTransactions) {

            transaction.height = block.height;

            long sender = transaction.getSenderAccountId();
            Account senderAccount = Nxt.accounts.get(sender);
            if (! senderAccount.setOrVerify(transaction.senderPublicKey)) {

                throw new RuntimeException("sender public key mismatch");
                // shouldn't happen, because transactions are already verified somewhere higher in pushBlock...

            }
            senderAccount.addToBalanceAndUnconfirmedBalance(- (transaction.amount + transaction.fee) * 100L);

            Account recipientAccount = Nxt.accounts.get(transaction.recipient);
            if (recipientAccount == null) {

                recipientAccount = Account.addAccount(transaction.recipient);

            }

            //TODO: refactor, don't use switch but create e.g. transaction handler class for each case
            switch (transaction.type) {

                case Transaction.TYPE_PAYMENT:
                {

                    switch (transaction.subtype) {

                        case Transaction.SUBTYPE_PAYMENT_ORDINARY_PAYMENT:
                        {

                            recipientAccount.addToBalanceAndUnconfirmedBalance(transaction.amount * 100L);

                        }
                        break;

                    }

                }
                break;

                case Transaction.TYPE_MESSAGING:
                {

                    switch (transaction.subtype) {

                        case Transaction.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
                        {

                            Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment)transaction.attachment;

                            String normalizedAlias = attachment.alias.toLowerCase();

                            Alias alias = Nxt.aliases.get(normalizedAlias);
                            if (alias == null) {

                                long aliasId = transaction.getId();
                                alias = new Alias(senderAccount, aliasId, attachment.alias, attachment.uri, block.timestamp);
                                Nxt.aliases.put(normalizedAlias, alias);
                                Nxt.aliasIdToAliasMappings.put(aliasId, alias);

                            } else {

                                alias.uri = attachment.uri;
                                alias.timestamp = block.timestamp;

                            }

                        }
                        break;

                    }

                }
                break;

                case Transaction.TYPE_COLORED_COINS:
                {

                    switch (transaction.subtype) {

                        case Transaction.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE:
                        {

                            Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.attachment;

                            long assetId = transaction.getId();
                            Asset asset = new Asset(sender, attachment.name, attachment.description, attachment.quantity);
                            Nxt.assets.put(assetId, asset);
                            Nxt.assetNameToIdMappings.put(attachment.name.toLowerCase(), assetId);
                            sortedAskOrders.put(assetId, new TreeSet<AskOrder>());
                            sortedBidOrders.put(assetId, new TreeSet<BidOrder>());
                            senderAccount.addToAssetAndUnconfirmedAssetBalance(assetId, attachment.quantity);

                        }
                        break;

                        case Transaction.SUBTYPE_COLORED_COINS_ASSET_TRANSFER:
                        {

                            Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.attachment;

                            senderAccount.addToAssetAndUnconfirmedAssetBalance(attachment.asset, -attachment.quantity);
                            recipientAccount.addToAssetAndUnconfirmedAssetBalance(attachment.asset, attachment.quantity);

                        }
                        break;

                        case Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
                        {

                            Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement)transaction.attachment;

                            AskOrder order = new AskOrder(transaction.getId(), senderAccount, attachment.asset, attachment.quantity, attachment.price);
                            senderAccount.addToAssetAndUnconfirmedAssetBalance(attachment.asset, -attachment.quantity);
                            askOrders.put(order.id, order);
                            sortedAskOrders.get(attachment.asset).add(order);
                            matchOrders(attachment.asset);

                        }
                        break;

                        case Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT:
                        {

                            Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement)transaction.attachment;

                            BidOrder order = new BidOrder(transaction.getId(), senderAccount, attachment.asset, attachment.quantity, attachment.price);

                            senderAccount.addToBalanceAndUnconfirmedBalance(- attachment.quantity * attachment.price);

                            bidOrders.put(order.id, order);
                            sortedBidOrders.get(attachment.asset).add(order);

                            matchOrders(attachment.asset);

                        }
                        break;

                        case Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
                        {

                            Attachment.ColoredCoinsAskOrderCancellation attachment = (Attachment.ColoredCoinsAskOrderCancellation)transaction.attachment;

                            AskOrder order;
                            order = askOrders.remove(attachment.order);
                            sortedAskOrders.get(order.asset).remove(order);
                            senderAccount.addToAssetAndUnconfirmedAssetBalance(order.asset, order.quantity);

                        }
                        break;

                        case Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
                        {

                            Attachment.ColoredCoinsBidOrderCancellation attachment = (Attachment.ColoredCoinsBidOrderCancellation)transaction.attachment;

                            BidOrder order;
                            order = bidOrders.remove(attachment.order);
                            sortedBidOrders.get(order.asset).remove(order);
                            senderAccount.addToBalanceAndUnconfirmedBalance(order.quantity * order.price);

                        }
                        break;

                    }

                }
                break;

            }

        }

    }

    private synchronized static void scan() {
        Map<Long,Block> loadedBlocks = new HashMap<>(Nxt.blocks);
        Nxt.blocks.clear();
        long currentBlockId = Genesis.GENESIS_BLOCK_ID;
        Block currentBlock;
        while ((currentBlock = loadedBlocks.get(currentBlockId)) != null) {
            Blockchain.analyze(currentBlock);
            currentBlockId = currentBlock.nextBlock;
        }
    }


    private static long calculateBaseTarget(Block block) {

        if (block.getId() == Genesis.GENESIS_BLOCK_ID) {

            return Nxt.initialBaseTarget;

        }

        Block previousBlock = Nxt.blocks.get(block.previousBlock);
        long curBaseTarget = previousBlock.baseTarget;
        long newBaseTarget = BigInteger.valueOf(curBaseTarget)
                .multiply(BigInteger.valueOf(block.timestamp - previousBlock.timestamp))
                .divide(BigInteger.valueOf(60)).longValue();
        if (newBaseTarget < 0 || newBaseTarget > Nxt.maxBaseTarget) {

            newBaseTarget = Nxt.maxBaseTarget;

        }

        if (newBaseTarget < curBaseTarget / 2) {

            newBaseTarget = curBaseTarget / 2;

        }
        if (newBaseTarget == 0) {

            newBaseTarget = 1;

        }

        long twofoldCurBaseTarget = curBaseTarget * 2;
        if (twofoldCurBaseTarget < 0) {

            twofoldCurBaseTarget = Nxt.maxBaseTarget;

        }
        if (newBaseTarget > twofoldCurBaseTarget) {

            newBaseTarget = twofoldCurBaseTarget;

        }

        return newBaseTarget;

    }

    private synchronized static void matchOrders(long assetId) {

        TreeSet<AskOrder> sortedAssetAskOrders = Blockchain.sortedAskOrders.get(assetId);
        TreeSet<BidOrder> sortedAssetBidOrders = Blockchain.sortedBidOrders.get(assetId);

        while (!sortedAssetAskOrders.isEmpty() && !sortedAssetBidOrders.isEmpty()) {

            AskOrder askOrder = sortedAssetAskOrders.first();
            BidOrder bidOrder = sortedAssetBidOrders.first();

            if (askOrder.price > bidOrder.price) {

                break;

            }

            int quantity = askOrder.quantity < bidOrder.quantity ? askOrder.quantity : bidOrder.quantity;
            long price = askOrder.height < bidOrder.height || (askOrder.height == bidOrder.height && askOrder.id < bidOrder.id) ? askOrder.price : bidOrder.price;

            if ((askOrder.quantity -= quantity) == 0) {

                askOrders.remove(askOrder.id);
                sortedAssetAskOrders.remove(askOrder);

            }

            askOrder.account.addToBalanceAndUnconfirmedBalance(quantity * price);

            if ((bidOrder.quantity -= quantity) == 0) {

                bidOrders.remove(bidOrder.id);
                sortedAssetBidOrders.remove(bidOrder);

            }

            bidOrder.account.addToAssetAndUnconfirmedAssetBalance(assetId, quantity);

        }

    }

    private static void generateBlock(String secretPhrase) {

        Set<Transaction> sortedTransactions = new TreeSet<>();

        for (Transaction transaction : Nxt.unconfirmedTransactions.values()) {

            if (transaction.referencedTransaction == 0 || Nxt.transactions.get(transaction.referencedTransaction) != null) {

                sortedTransactions.add(transaction);

            }

        }

        Map<Long, Transaction> newTransactions = new HashMap<>();
        Set<String> newAliases = new HashSet<>();
        Map<Long, Long> accumulatedAmounts = new HashMap<>();
        int payloadLength = 0;

        while (payloadLength <= Nxt.MAX_PAYLOAD_LENGTH) {

            int prevNumberOfNewTransactions = newTransactions.size();

            for (Transaction transaction : sortedTransactions) {

                int transactionLength = transaction.getSize();
                if (newTransactions.get(transaction.getId()) == null && payloadLength + transactionLength <= Nxt.MAX_PAYLOAD_LENGTH) {

                    long sender = transaction.getSenderAccountId();
                    Long accumulatedAmount = accumulatedAmounts.get(sender);
                    if (accumulatedAmount == null) {

                        accumulatedAmount = 0L;

                    }

                    long amount = (transaction.amount + transaction.fee) * 100L;
                    if (accumulatedAmount + amount <= Nxt.accounts.get(sender).getBalance() && transaction.validateAttachment()) {

                        switch (transaction.type) {

                            case Transaction.TYPE_MESSAGING:
                            {

                                switch (transaction.subtype) {

                                    case Transaction.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
                                    {

                                        if (!newAliases.add(((Attachment.MessagingAliasAssignment)transaction.attachment).alias.toLowerCase())) {

                                            continue;

                                        }

                                    }
                                    break;

                                }

                            }
                            break;

                        }

                        accumulatedAmounts.put(sender, accumulatedAmount + amount);

                        newTransactions.put(transaction.getId(), transaction);
                        payloadLength += transactionLength;

                    }

                }

            }

            if (newTransactions.size() == prevNumberOfNewTransactions) {

                break;

            }

        }

        final byte[] publicKey = Crypto.getPublicKey(secretPhrase);

        Block block;
        Block previousBlock = Nxt.lastBlock.get();
        if (previousBlock.height < Nxt.TRANSPARENT_FORGING_BLOCK) {

            block = new Block(1, Convert.getEpochTime(), previousBlock.getId(), newTransactions.size(),
                    0, 0, 0, null, publicKey, null, new byte[64]);

        } else {

            byte[] previousBlockHash = Crypto.sha256().digest(previousBlock.getBytes());
            block = new Block(2, Convert.getEpochTime(), previousBlock.getId(), newTransactions.size(),
                    0, 0, 0, null, publicKey, null, new byte[64], previousBlockHash);

        }
        int i = 0;
        for (Map.Entry<Long, Transaction> transactionEntry : newTransactions.entrySet()) {

            Transaction transaction = transactionEntry.getValue();
            block.totalAmount += transaction.amount;
            block.totalFee += transaction.fee;
            block.payloadLength += transaction.getSize();
            block.transactions[i++] = transactionEntry.getKey();

        }

        Arrays.sort(block.transactions);
        MessageDigest digest = Crypto.sha256();
        for (i = 0; i < block.transactions.length; i++) {
            Transaction transaction = newTransactions.get(block.transactions[i]);
            digest.update(transaction.getBytes());
            block.blockTransactions[i] = transaction;
        }
        block.payloadHash = digest.digest();

        if (previousBlock.height < Nxt.TRANSPARENT_FORGING_BLOCK) {

            block.generationSignature = Crypto.sign(previousBlock.generationSignature, secretPhrase);

        } else {

            digest.update(previousBlock.generationSignature);
            block.generationSignature = digest.digest(publicKey);

        }

        byte[] data = block.getBytes();
        byte[] data2 = new byte[data.length - 64];
        System.arraycopy(data, 0, data2, 0, data2.length);
        block.blockSignature = Crypto.sign(data2, secretPhrase);

        if (block.verifyBlockSignature() && block.verifyGenerationSignature()) {

            JSONObject request = block.getJSONObject();
            request.put("requestType", "processBlock");
            Peer.sendToSomePeers(request);

        } else {

            Logger.logMessage("Generated an incorrect block. Waiting for the next one...");

        }

    }

    static void init() {

        try {

            Logger.logMessage("Loading transactions...");
            Transaction.loadTransactions("transactions.nxt");
            Logger.logMessage("...Done");

        } catch (FileNotFoundException e) {
            Logger.logMessage("transactions.nxt not found, starting from scratch");
            Nxt.transactions.clear();

            for (int i = 0; i < Genesis.GENESIS_RECIPIENTS.length; i++) {

                Transaction transaction = new Transaction(Transaction.TYPE_PAYMENT, Transaction.SUBTYPE_PAYMENT_ORDINARY_PAYMENT,
                        0, (short)0, Genesis.CREATOR_PUBLIC_KEY, Genesis.GENESIS_RECIPIENTS[i], Genesis.GENESIS_AMOUNTS[i],
                        0, 0, Genesis.GENESIS_SIGNATURES[i]);

                Nxt.transactions.put(transaction.getId(), transaction);

            }

            for (Transaction transaction : Nxt.transactions.values()) {
                transaction.index = Nxt.transactionCounter.incrementAndGet();
                transaction.block = Genesis.GENESIS_BLOCK_ID;

            }

            nxt.Transaction.saveTransactions("transactions.nxt");

        }

        try {

            Logger.logMessage("Loading blocks...");
            Block.loadBlocks("blocks.nxt");
            Logger.logMessage("...Done");

        } catch (FileNotFoundException e) {
            Logger.logMessage("blocks.nxt not found, starting from scratch");
            Nxt.blocks.clear();

            Block block = new Block(-1, 0, 0, Nxt.transactions.size(), 1000000000, 0, Nxt.transactions.size() * 128, null,
                    Genesis.CREATOR_PUBLIC_KEY, new byte[64], Genesis.GENESIS_BLOCK_SIGNATURE);
            block.index = Nxt.blockCounter.incrementAndGet();
            Nxt.blocks.put(Genesis.GENESIS_BLOCK_ID, block);

            int i = 0;
            for (long transaction : Nxt.transactions.keySet()) {

                block.transactions[i++] = transaction;

            }
            Arrays.sort(block.transactions);
            MessageDigest digest = Crypto.sha256();
            for (i = 0; i < block.transactions.length; i++) {
                Transaction transaction = Nxt.transactions.get(block.transactions[i]);
                digest.update(transaction.getBytes());
                block.blockTransactions[i] = transaction;
            }
            block.payloadHash = digest.digest();

            block.baseTarget = Nxt.initialBaseTarget;
            block.cumulativeDifficulty = BigInteger.ZERO;
            Nxt.lastBlock.set(block);

            Block.saveBlocks("blocks.nxt");

        }

        Logger.logMessage("Scanning blockchain...");
        Blockchain.scan();
        Logger.logMessage("...Done");
    }

}
