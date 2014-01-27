package nxt;

import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.user.User;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class Blockchain {

    private static final byte[] CHECKSUM_TRANSPARENT_FORGING = new byte[]{27, -54, -59, -98, 49, -42, 48, -68, -112, 49, 41, 94, -41, 78, -84, 27, -87, -22, -28, 36, -34, -90, 112, -50, -9, 5, 89, -35, 80, -121, -128, 112};

    private static volatile Peer lastBlockchainFeeder;

    private static final AtomicInteger blockCounter = new AtomicInteger();
    private static final AtomicReference<Block> lastBlock = new AtomicReference<>();
    private static final ConcurrentMap<Long, Block> blocks = new ConcurrentHashMap<>();

    private static final AtomicInteger transactionCounter = new AtomicInteger();
    private static final ConcurrentMap<Long, Transaction> doubleSpendingTransactions = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, Transaction> unconfirmedTransactions = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, Transaction> nonBroadcastedTransactions = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, Transaction> transactions = new ConcurrentHashMap<>();

    public static final Collection<Block> allBlocks = Collections.unmodifiableCollection(blocks.values());
    public static final Collection<Transaction> allTransactions = Collections.unmodifiableCollection(transactions.values());
    public static final Collection<Transaction> allUnconfirmedTransactions = Collections.unmodifiableCollection(unconfirmedTransactions.values());

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

                Peer peer = Peer.getAnyPeer(Peer.State.CONNECTED, true);
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

                Iterator<Transaction> iterator = unconfirmedTransactions.values().iterator();
                while (iterator.hasNext()) {

                    Transaction transaction = iterator.next();
                    if (transaction.getTimestamp() + transaction.deadline * 60 < curTime || !transaction.validateAttachment()) {

                        iterator.remove();

                        Account account = Account.getAccount(transaction.getSenderAccountId());
                        account.addToUnconfirmedBalance((transaction.amount + transaction.fee) * 100L);

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

                                JSONArray milestoneBlockIds = (JSONArray)response.get("milestoneBlockIds");
                                for (Object milestoneBlockId : milestoneBlockIds) {

                                    Long blockId = Convert.parseUnsignedLong((String) milestoneBlockId);
                                    Block block = blocks.get(blockId);
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
                                    response = peer.send(JSON.prepareRequest(request));
                                    if (response == null) {

                                        return;

                                    } else {

                                        JSONArray nextBlockIds = (JSONArray)response.get("nextBlockIds");
                                        numberOfBlocks = nextBlockIds.size();
                                        if (numberOfBlocks == 0) {

                                            return;

                                        } else {

                                            Long blockId;
                                            for (i = 0; i < numberOfBlocks; i++) {

                                                blockId = Convert.parseUnsignedLong((String) nextBlockIds.get(i));
                                                if (blocks.get(blockId) == null) {

                                                    break;

                                                }

                                                commonBlockId = blockId;

                                            }

                                        }

                                    }

                                } while (i == numberOfBlocks);

                                if (lastBlock.get().getHeight() - blocks.get(commonBlockId).getHeight() < 720) {

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
                                                        if (lastBlock.get().getId().equals(block.previousBlock)) {

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
                                                        if (!alreadyPushed && blocks.get(block.getId()) == null && block.transactions.length <= Nxt.MAX_NUMBER_OF_TRANSACTIONS) {

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

                                    if (!futureBlocks.isEmpty() && lastBlock.get().getHeight() - blocks.get(commonBlockId).getHeight() < 720) {

                                        synchronized (Blockchain.class) {

                                            saveBlocks("blocks.nxt.bak");
                                            saveTransactions("transactions.nxt.bak");

                                            curCumulativeDifficulty = lastBlock.get().getCumulativeDifficulty();

                                            while (!lastBlock.get().getId().equals(commonBlockId) && Blockchain.popLastBlock()) {}

                                            if (lastBlock.get().getId().equals(commonBlockId)) {

                                                for (Block block : futureBlocks) {

                                                    if (lastBlock.get().getId().equals(block.previousBlock)) {

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

                                            if (lastBlock.get().getCumulativeDifficulty().compareTo(curCumulativeDifficulty) < 0) {

                                                loadBlocks("blocks.nxt.bak");
                                                loadTransactions("transactions.nxt.bak");

                                                peer.blacklist();

                                                Account.clear();
                                                Alias.clear();
                                                unconfirmedTransactions.clear();
                                                doubleSpendingTransactions.clear();
                                                Logger.logMessage("Re-scanning blockchain...");
                                                Blockchain.scan();
                                                Logger.logMessage("...Done");


                                            }

                                        }

                                    }

                                    synchronized (Blockchain.class) {
                                        saveBlocks("blocks.nxt");
                                        saveTransactions("transactions.nxt");
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

                Map<Account,User> unlockedAccounts = new HashMap<>();
                for (User user : User.allUsers) {
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
                        response.put("deadline", hit.divide(BigInteger.valueOf(lastBlock.baseTarget).multiply(BigInteger.valueOf(effectiveBalance))).longValue() - (Convert.getEpochTime() - lastBlock.timestamp));

                        user.send(response);

                    }

                    int elapsedTime = Convert.getEpochTime() - lastBlock.timestamp;
                    if (elapsedTime > 0) {

                        BigInteger target = BigInteger.valueOf(lastBlock.getBaseTarget()).multiply(BigInteger.valueOf(account.getEffectiveBalance())).multiply(BigInteger.valueOf(elapsedTime));
                        if (hits.get(account).compareTo(target) < 0) {

                            Blockchain.generateBlock(user.getSecretPhrase());

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

                for (Transaction transaction : nonBroadcastedTransactions.values()) {

                    if (unconfirmedTransactions.get(transaction.getId()) == null && transactions.get(transaction.getId()) == null) {

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
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    public static Block getLastBlock() {
        return lastBlock.get();
    }

    public static Block getBlock(Long id) {
        return blocks.get(id);
    }

    public static Transaction getTransaction(Long transactionId) {
        return transactions.get(transactionId);
    }

    public static Transaction getUnconfirmedTransaction(Long transactionId) {
        return unconfirmedTransactions.get(transactionId);
    }

    public static void broadcast(Transaction transaction) {
        nonBroadcastedTransactions.put(transaction.getId(), transaction);
    }

    public static Peer getLastBlockchainFeeder() {
        return lastBlockchainFeeder;
    }

    public static void processTransactions(JSONObject request, String parameterName) {

        JSONArray transactionsData = (JSONArray)request.get(parameterName);
        JSONArray validTransactionsData = new JSONArray();

        for (Object transactionData : transactionsData) {

            Transaction transaction = Transaction.getTransaction((JSONObject) transactionData);

            try {

                int curTime = Convert.getEpochTime();
                if (transaction.getTimestamp() > curTime + 15 || transaction.deadline < 1
                        || transaction.getTimestamp() + transaction.deadline * 60 < curTime
                        || transaction.fee <= 0 || !transaction.validateAttachment()) {

                    continue;

                }

                boolean doubleSpendingTransaction;

                synchronized (Blockchain.class) {

                    Long id = transaction.getId();
                    if (transactions.containsKey(id) || unconfirmedTransactions.containsKey(id)
                            || doubleSpendingTransactions.containsKey(id) || !transaction.verify()) {
                        continue;
                    }

                    doubleSpendingTransaction = transaction.preProcess();

                    transaction.setIndex(transactionCounter.incrementAndGet());

                    if (doubleSpendingTransaction) {

                        doubleSpendingTransactions.put(id, transaction);

                    } else {

                        unconfirmedTransactions.put(id, transaction);

                        if (parameterName.equals("transactions")) {

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
                newTransaction.put("deadline", transaction.deadline);
                newTransaction.put("recipient", Convert.convert(transaction.recipient));
                newTransaction.put("amount", transaction.amount);
                newTransaction.put("fee", transaction.fee);
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

    public static boolean pushBlock(ByteBuffer buffer, boolean savingFlag) {

        Block block;
        JSONArray addedConfirmedTransactions;
        JSONArray removedUnconfirmedTransactions;
        int curTime = Convert.getEpochTime();

        synchronized (Blockchain.class) {
            try {

                Block previousLastBlock = lastBlock.get();
                buffer.flip();

                int version = buffer.getInt();
                if (version != (previousLastBlock.getHeight() < Nxt.TRANSPARENT_FORGING_BLOCK ? 1 : 2)) {

                    return false;

                }

                if (previousLastBlock.getHeight() == Nxt.TRANSPARENT_FORGING_BLOCK) {

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
                Long previousBlock = buffer.getLong();
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

                block = new Block(version, blockTimestamp, previousBlock, numberOfTransactions, totalAmount, totalFee,
                        payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash);

                if (block.transactions.length > Nxt.MAX_NUMBER_OF_TRANSACTIONS || !previousLastBlock.getId().equals(block.previousBlock)
                        || block.getId().equals(Long.valueOf(0L)) || blocks.containsKey(block.getId()) || !block.verifyGenerationSignature() || !block.verifyBlockSignature()) {

                    return false;

                }

                block.setIndex(blockCounter.incrementAndGet());

                HashMap<Long, Transaction> blockTransactions = new HashMap<>();
                HashSet<String> blockAliases = new HashSet<>();
                for (int i = 0; i < block.transactions.length; i++) {

                    Transaction transaction = Transaction.getTransaction(buffer);
                    transaction.setIndex(transactionCounter.incrementAndGet());

                    if (blockTransactions.put(block.transactions[i] = transaction.getId(), transaction) != null) {

                        return false;

                    }

                    if (transaction.getType() == Transaction.Type.Messaging.ALIAS_ASSIGNMENT) {
                        if (!blockAliases.add(((Attachment.MessagingAliasAssignment)transaction.getAttachment()).alias.toLowerCase())) {
                            return false;
                        }
                    }

                }
                Arrays.sort(block.transactions);

                Map<Long, Long> accumulatedAmounts = new HashMap<>();
                Map<Long, Map<Long, Long>> accumulatedAssetQuantities = new HashMap<>();
                int calculatedTotalAmount = 0, calculatedTotalFee = 0;
                MessageDigest digest = Crypto.sha256();
                int i;
                for (i = 0; i < block.transactions.length; i++) {

                    Transaction transaction = blockTransactions.get(block.transactions[i]);
                    // cfb: Block 303 contains a transaction which expired before the block timestamp
                    //TODO: similar transaction validation is done in several places, refactor common code out
                    if (transaction.getTimestamp() > curTime + 15 || transaction.deadline < 1
                            || (transaction.getTimestamp() + transaction.deadline * 60 < blockTimestamp && previousLastBlock.getHeight() > 303)
                            || transaction.fee <= 0 || transaction.fee > Nxt.MAX_BALANCE || transaction.amount < 0
                            || transaction.amount > Nxt.MAX_BALANCE || !transaction.validateAttachment() || transactions.get(block.transactions[i]) != null
                            || (transaction.referencedTransaction != null && transactions.get(transaction.referencedTransaction) == null && blockTransactions.get(transaction.referencedTransaction) == null)
                            || (unconfirmedTransactions.get(block.transactions[i]) == null && !transaction.verify())
                            || transaction.getId().equals(Long.valueOf(0L))) {

                        break;

                    }

                    calculatedTotalAmount += transaction.amount;

                    transaction.updateTotals(accumulatedAmounts, accumulatedAssetQuantities);

                    calculatedTotalFee += transaction.fee;

                    digest.update(transaction.getBytes());

                }

                if (i != block.transactions.length || calculatedTotalAmount != block.totalAmount || calculatedTotalFee != block.totalFee) {

                    return false;

                }

                if (!Arrays.equals(digest.digest(), block.getPayloadHash())) {

                    return false;

                }

                for (Map.Entry<Long, Long> accumulatedAmountEntry : accumulatedAmounts.entrySet()) {

                    Account senderAccount = Account.getAccount(accumulatedAmountEntry.getKey());
                    if (senderAccount.getBalance() < accumulatedAmountEntry.getValue()) {

                        return false;

                    }

                }

                for (Map.Entry<Long, Map<Long, Long>> accumulatedAssetQuantitiesEntry : accumulatedAssetQuantities.entrySet()) {

                    Account senderAccount = Account.getAccount(accumulatedAssetQuantitiesEntry.getKey());
                    for (Map.Entry<Long, Long> accountAccumulatedAssetQuantitiesEntry : accumulatedAssetQuantitiesEntry.getValue().entrySet()) {

                        long asset = accountAccumulatedAssetQuantitiesEntry.getKey();
                        long quantity = accountAccumulatedAssetQuantitiesEntry.getValue();
                        if (senderAccount.getAssetBalance(asset) < quantity) {

                            return false;

                        }

                    }

                }

                block.setHeight(previousLastBlock.getHeight() + 1);

                for (Map.Entry<Long, Transaction> transactionEntry : blockTransactions.entrySet()) {

                    Transaction transaction = transactionEntry.getValue();
                    transaction.setHeight(block.getHeight());
                    transaction.setBlock(block.getId());

                    if (transactions.putIfAbsent(transactionEntry.getKey(), transaction) != null) {
                        Logger.logMessage("duplicate transaction id " + transactionEntry.getKey());
                        return false;
                    }

                }

                block.apply();

                addedConfirmedTransactions = new JSONArray();
                removedUnconfirmedTransactions = new JSONArray();

                for (Map.Entry<Long, Transaction> transactionEntry : blockTransactions.entrySet()) {

                    Transaction transaction = transactionEntry.getValue();

                    JSONObject addedConfirmedTransaction = new JSONObject();
                    addedConfirmedTransaction.put("index", transaction.getIndex());
                    addedConfirmedTransaction.put("blockTimestamp", block.timestamp);
                    addedConfirmedTransaction.put("transactionTimestamp", transaction.getTimestamp());
                    addedConfirmedTransaction.put("sender", Convert.convert(transaction.getSenderAccountId()));
                    addedConfirmedTransaction.put("recipient", Convert.convert(transaction.recipient));
                    addedConfirmedTransaction.put("amount", transaction.amount);
                    addedConfirmedTransaction.put("fee", transaction.fee);
                    addedConfirmedTransaction.put("id", transaction.getStringId());
                    addedConfirmedTransactions.add(addedConfirmedTransaction);

                    Transaction removedTransaction = unconfirmedTransactions.remove(transactionEntry.getKey());
                    if (removedTransaction != null) {

                        JSONObject removedUnconfirmedTransaction = new JSONObject();
                        removedUnconfirmedTransaction.put("index", removedTransaction.getIndex());
                        removedUnconfirmedTransactions.add(removedUnconfirmedTransaction);

                        Account senderAccount = Account.getAccount(removedTransaction.getSenderAccountId());
                        senderAccount.addToUnconfirmedBalance((removedTransaction.amount + removedTransaction.fee) * 100L);

                    }

                    // TODO: Remove from double-spending transactions

                }

                if (savingFlag) {

                    saveTransactions("transactions.nxt");
                    saveBlocks("blocks.nxt");

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
        addedRecentBlock.put("index", block.getIndex());
        addedRecentBlock.put("timestamp", block.timestamp);
        addedRecentBlock.put("numberOfTransactions", block.transactions.length);
        addedRecentBlock.put("totalAmount", block.totalAmount);
        addedRecentBlock.put("totalFee", block.totalFee);
        addedRecentBlock.put("payloadLength", block.payloadLength);
        addedRecentBlock.put("generator", Convert.convert(block.getGeneratorAccountId()));
        addedRecentBlock.put("height", block.getHeight());
        addedRecentBlock.put("version", block.version);
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

        return true;


    }

    static void addBlock(Block block) {
        if (block.previousBlock == null) {
            blocks.put(block.getId(), block);
            lastBlock.set(block);
        } else {
            if (! lastBlock.compareAndSet(blocks.get(block.previousBlock), block)) {
                throw new IllegalStateException("Last block not equal to this.previousBlock"); // shouldn't happen
            }
            if (blocks.putIfAbsent(block.getId(), block) != null) {
                throw new IllegalStateException("duplicate block id: " + block.getId()); // shouldn't happen
            }
        }
    }

    static ArrayList<Block> getLastBlocks(int numberOfBlocks) {

        ArrayList<Block> lastBlocks = new ArrayList<>(numberOfBlocks);

        Long curBlock = lastBlock.get().getId();
        do {

            Block block = blocks.get(curBlock);
            if (block == null) {
                break;
            }
            lastBlocks.add(block);
            curBlock = block.previousBlock;

        } while (lastBlocks.size() < numberOfBlocks && curBlock != null);

        return lastBlocks;

    }

    static void init() {

        try {

            Logger.logMessage("Loading transactions...");
            loadTransactions("transactions.nxt");
            Logger.logMessage("...Done");

        } catch (FileNotFoundException e) {
            Logger.logMessage("transactions.nxt not found, starting from scratch");
            transactions.clear();

            for (int i = 0; i < Genesis.GENESIS_RECIPIENTS.length; i++) {

                Transaction transaction = Transaction.newTransaction(0, (short)0, Genesis.CREATOR_PUBLIC_KEY,
                        Genesis.GENESIS_RECIPIENTS[i], Genesis.GENESIS_AMOUNTS[i], 0, null, Genesis.GENESIS_SIGNATURES[i]);

                transactions.put(transaction.getId(), transaction);

            }

            for (Transaction transaction : transactions.values()) {
                transaction.setIndex(transactionCounter.incrementAndGet());
                transaction.setBlock(Genesis.GENESIS_BLOCK_ID);

            }

            saveTransactions("transactions.nxt");

        }

        try {

            Logger.logMessage("Loading blocks...");
            loadBlocks("blocks.nxt");
            Logger.logMessage("...Done");

        } catch (FileNotFoundException e) {
            Logger.logMessage("blocks.nxt not found, starting from scratch");
            blocks.clear();

            Block block = new Block(-1, 0, null, transactions.size(), 1000000000, 0, transactions.size() * 128, null,
                    Genesis.CREATOR_PUBLIC_KEY, new byte[64], Genesis.GENESIS_BLOCK_SIGNATURE);
            block.setIndex(blockCounter.incrementAndGet());

            int i = 0;
            for (Long transactionId : transactions.keySet()) {

                block.transactions[i++] = transactionId;

            }
            Arrays.sort(block.transactions);
            MessageDigest digest = Crypto.sha256();
            for (i = 0; i < block.transactions.length; i++) {
                Transaction transaction = transactions.get(block.transactions[i]);
                digest.update(transaction.getBytes());
                block.blockTransactions[i] = transaction;
            }
            block.setPayloadHash(digest.digest());

            blocks.put(Genesis.GENESIS_BLOCK_ID, block);
            lastBlock.set(block);

            saveBlocks("blocks.nxt");

        }

        Logger.logMessage("Scanning blockchain...");
        Blockchain.scan();
        Logger.logMessage("...Done");
    }

    static void shutdown() {
        try {
            saveBlocks("blocks.nxt");
            Logger.logMessage("Saved blocks.nxt");
        } catch (RuntimeException e) {
            Logger.logMessage("Error saving blocks", e);
        }

        try {
            saveTransactions("transactions.nxt");
            Logger.logMessage("Saved transactions.nxt");
        } catch (RuntimeException e) {
            Logger.logMessage("Error saving transactions", e);
        }
    }

    private synchronized static byte[] calculateTransactionsChecksum() {
        PriorityQueue<Transaction> sortedTransactions = new PriorityQueue<>(transactions.size(), new Comparator<Transaction>() {
            @Override
            public int compare(Transaction o1, Transaction o2) {
                long id1 = o1.getId();
                long id2 = o2.getId();
                return id1 < id2 ? -1 : (id1 > id2 ? 1 : (o1.getTimestamp() < o2.getTimestamp() ? -1 : (o1.getTimestamp() > o2.getTimestamp() ? 1 : 0)));
            }
        });
        sortedTransactions.addAll(transactions.values());
        MessageDigest digest = Crypto.sha256();
        while (! sortedTransactions.isEmpty()) {
            digest.update(sortedTransactions.poll().getBytes());
        }
        return digest.digest();
    }

    private static boolean popLastBlock() {

        try {

            JSONObject response = new JSONObject();
            response.put("response", "processNewData");

            JSONArray addedUnconfirmedTransactions = new JSONArray();

            Block block;

            synchronized (Blockchain.class) {

                block = lastBlock.get();

                if (block.getId().equals(Genesis.GENESIS_BLOCK_ID)) {
                    return false;
                }

                Block previousBlock = blocks.get(block.previousBlock);
                if (previousBlock == null) {
                    Logger.logMessage("Previous block is null");
                    throw new IllegalStateException();
                }
                if (! lastBlock.compareAndSet(block, previousBlock)) {
                    Logger.logMessage("This block is no longer last block");
                    throw new IllegalStateException();
                }

                Account generatorAccount = Account.getAccount(block.getGeneratorAccountId());
                generatorAccount.addToBalanceAndUnconfirmedBalance(- block.totalFee * 100L);

                for (Long transactionId : block.transactions) {

                    Transaction transaction = transactions.remove(transactionId);
                    unconfirmedTransactions.put(transactionId, transaction);

                    Account senderAccount = Account.getAccount(transaction.getSenderAccountId());
                    senderAccount.addToBalance((transaction.amount + transaction.fee) * 100L);

                    Account recipientAccount = Account.getAccount(transaction.recipient);
                    recipientAccount.addToBalanceAndUnconfirmedBalance(- transaction.amount * 100L);

                    JSONObject addedUnconfirmedTransaction = new JSONObject();
                    addedUnconfirmedTransaction.put("index", transaction.getIndex());
                    addedUnconfirmedTransaction.put("timestamp", transaction.getTimestamp());
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
            addedOrphanedBlock.put("index", block.getIndex());
            addedOrphanedBlock.put("timestamp", block.timestamp);
            addedOrphanedBlock.put("numberOfTransactions", block.transactions.length);
            addedOrphanedBlock.put("totalAmount", block.totalAmount);
            addedOrphanedBlock.put("totalFee", block.totalFee);
            addedOrphanedBlock.put("payloadLength", block.payloadLength);
            addedOrphanedBlock.put("generator", Convert.convert(block.getGeneratorAccountId()));
            addedOrphanedBlock.put("height", block.getHeight());
            addedOrphanedBlock.put("version", block.version);
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
        Map<Long,Block> loadedBlocks = new HashMap<>(blocks);
        blocks.clear();
        Long currentBlockId = Genesis.GENESIS_BLOCK_ID;
        Block currentBlock;
        while ((currentBlock = loadedBlocks.get(currentBlockId)) != null) {
            currentBlock.apply();
            currentBlockId = currentBlock.getNextBlock();
        }
    }

    private static void generateBlock(String secretPhrase) {

        Set<Transaction> sortedTransactions = new TreeSet<>();

        for (Transaction transaction : unconfirmedTransactions.values()) {

            if (transaction.referencedTransaction == null || transactions.get(transaction.referencedTransaction) != null) {

                sortedTransactions.add(transaction);

            }

        }

        Map<Long, Transaction> newTransactions = new HashMap<>();
        Set<String> newAliases = new HashSet<>();
        Map<Long, Long> accumulatedAmounts = new HashMap<>();

        int totalAmount = 0;
        int totalFee = 0;
        int payloadLength = 0;

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

                    long amount = (transaction.amount + transaction.fee) * 100L;
                    if (accumulatedAmount + amount <= Account.getAccount(sender).getBalance() && transaction.validateAttachment()) {

                        if (transaction.getType() == Transaction.Type.Messaging.ALIAS_ASSIGNMENT) {
                            if (!newAliases.add(((Attachment.MessagingAliasAssignment)transaction.getAttachment()).alias.toLowerCase())) {
                                continue;
                            }
                        }

                        accumulatedAmounts.put(sender, accumulatedAmount + amount);

                        newTransactions.put(transaction.getId(), transaction);
                        payloadLength += transactionLength;
                        totalAmount += transaction.amount;
                        totalFee += transaction.fee;

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
        if (previousBlock.getHeight() < Nxt.TRANSPARENT_FORGING_BLOCK) {

            block = new Block(1, Convert.getEpochTime(), previousBlock.getId(), newTransactions.size(),
                    totalAmount, totalFee, payloadLength, null, publicKey, null, new byte[64]);

        } else {

            byte[] previousBlockHash = Crypto.sha256().digest(previousBlock.getBytes());
            block = new Block(2, Convert.getEpochTime(), previousBlock.getId(), newTransactions.size(),
                    totalAmount, totalFee, payloadLength, null, publicKey, null, new byte[64], previousBlockHash);

        }

        int i = 0;
        for (Long transactionId : newTransactions.keySet()) {
            block.transactions[i++] = transactionId;
        }

        Arrays.sort(block.transactions);
        MessageDigest digest = Crypto.sha256();
        for (i = 0; i < block.transactions.length; i++) {
            Transaction transaction = newTransactions.get(block.transactions[i]);
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

    private static void loadTransactions(String fileName) throws FileNotFoundException {

        try (FileInputStream fileInputStream = new FileInputStream(fileName);
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
            transactionCounter.set(objectInputStream.readInt());
            transactions.clear();
            transactions.putAll((HashMap<Long, Transaction>) objectInputStream.readObject());
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException |ClassNotFoundException e) {
            Logger.logMessage("Error loading transactions from " + fileName, e);
            System.exit(1);
        }

    }

    private static void saveTransactions(String fileName) {

        try (FileOutputStream fileOutputStream = new FileOutputStream(fileName);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)
        ) {
            objectOutputStream.writeInt(transactionCounter.get());
            objectOutputStream.writeObject(new HashMap(transactions));
            objectOutputStream.close();
        } catch (IOException e) {
            Logger.logMessage("Error saving transactions to " + fileName, e);
            throw new RuntimeException(e);
        }

    }

    private static void loadBlocks(String fileName) throws FileNotFoundException {

        try (FileInputStream fileInputStream = new FileInputStream(fileName);
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)
        ) {
            blockCounter.set(objectInputStream.readInt());
            blocks.clear();
            blocks.putAll((HashMap<Long, Block>) objectInputStream.readObject());
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException|ClassNotFoundException e) {
            Logger.logMessage("Error loading blocks from " + fileName, e);
            System.exit(1);
        }

    }

    private static void saveBlocks(String fileName) {

        try (FileOutputStream fileOutputStream = new FileOutputStream(fileName);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)
        ) {
            objectOutputStream.writeInt(blockCounter.get());
            objectOutputStream.writeObject(new HashMap<>(blocks));
        } catch (IOException e) {
            Logger.logMessage("Error saving blocks to " + fileName, e);
            throw new RuntimeException(e);
        }

    }

    private Blockchain() {} // never, yet

}
