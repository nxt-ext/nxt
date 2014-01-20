package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
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
import java.io.Serializable;
import java.io.Writer;
import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

public class Block implements Serializable {

    static final long serialVersionUID = 0;
    static final long[] emptyLong = new long[0];
    static final Transaction[] emptyTransactions = new Transaction[0];

    final int version;
    final int timestamp;
    final long previousBlock;
    int totalAmount, totalFee;
    int payloadLength;
    byte[] payloadHash;
    final byte[] generatorPublicKey;
    byte[] generationSignature;
    byte[] blockSignature;

    final byte[] previousBlockHash;

    public int index;
    public final long[] transactions;
    public long baseTarget;
    public int height;
    public volatile long nextBlock;
    public BigInteger cumulativeDifficulty;

    transient Transaction[] blockTransactions;

    Block(int version, int timestamp, long previousBlock, int numberOfTransactions, int totalAmount, int totalFee, int payloadLength, byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature) {

        this(version, timestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, null);

    }

    public Block(int version, int timestamp, long previousBlock, int numberOfTransactions, int totalAmount, int totalFee, int payloadLength, byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash) {

        if (numberOfTransactions > Nxt.MAX_NUMBER_OF_TRANSACTIONS || numberOfTransactions < 0) {
            throw new IllegalArgumentException("attempted to create a block with " + numberOfTransactions + " transactions");
        }

        if (payloadLength > Nxt.MAX_PAYLOAD_LENGTH || payloadLength < 0) {
            throw new IllegalArgumentException("attempted to create a block with payloadLength " + payloadLength);
        }

        this.version = version;
        this.timestamp = timestamp;
        this.previousBlock = previousBlock;
        this.totalAmount = totalAmount;
        this.totalFee = totalFee;
        this.payloadLength = payloadLength;
        this.payloadHash = payloadHash;
        this.generatorPublicKey = generatorPublicKey;
        this.generationSignature = generationSignature;
        this.blockSignature = blockSignature;

        this.previousBlockHash = previousBlockHash;
        this.transactions = numberOfTransactions == 0 ? emptyLong : new long[numberOfTransactions];
        this.blockTransactions = numberOfTransactions == 0 ? emptyTransactions : new Transaction[numberOfTransactions];

    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.blockTransactions = transactions.length == 0 ? emptyTransactions : new Transaction[transactions.length];
    }

    void analyze() {

        // analyze is only called with the blocksAndTransactionsLock already held (except in init, where it doesn't matter)
        // but a lot of thread safety depends on that fact, so let's make that explicit here by obtaining this lock again, at no extra cost
        synchronized (Nxt.blocksAndTransactionsLock) {
            for (int i = 0; i < this.transactions.length; i++) {
                this.blockTransactions[i] = Nxt.transactions.get(this.transactions[i]);
                if (this.blockTransactions[i] == null) {
                    throw new IllegalStateException("Missing transaction " + Convert.convert(this.transactions[i]));
                }
            }
            if (previousBlock == 0) {

                baseTarget = Nxt.initialBaseTarget;
                cumulativeDifficulty = BigInteger.ZERO;
                Nxt.blocks.put(Nxt.GENESIS_BLOCK_ID, this);
                Nxt.lastBlock.set(this);

                Account.addAccount(Nxt.CREATOR_ID);

            } else {

                Block previousLastBlock = Nxt.lastBlock.get();

                previousLastBlock.nextBlock = getId();
                height = previousLastBlock.height + 1;
                baseTarget = calculateBaseTarget();
                cumulativeDifficulty = previousLastBlock.cumulativeDifficulty.add(Convert.two64.divide(BigInteger.valueOf(baseTarget)));
                if (! (previousLastBlock.getId() == previousBlock && Nxt.lastBlock.compareAndSet(previousLastBlock, this))) {
                    throw new IllegalStateException("Last block not equal to this.previousBlock"); // shouldn't happen
                }
                Account generatorAccount = Nxt.accounts.get(getGeneratorAccountId());
                generatorAccount.addToBalanceAndUnconfirmedBalance(totalFee * 100L);
                if (Nxt.blocks.putIfAbsent(getId(), this) != null) {
                    throw new IllegalStateException("duplicate block id: " + getId()); // shouldn't happen
                }
            }

            for (Transaction transaction : this.blockTransactions) {

                transaction.height = this.height;

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

                                Transaction.MessagingAliasAssignmentAttachment attachment = (Transaction.MessagingAliasAssignmentAttachment)transaction.attachment;

                                String normalizedAlias = attachment.alias.toLowerCase();

                                Alias alias = Nxt.aliases.get(normalizedAlias);
                                if (alias == null) {

                                    long aliasId = transaction.getId();
                                    alias = new Alias(senderAccount, aliasId, attachment.alias, attachment.uri, timestamp);
                                    Nxt.aliases.put(normalizedAlias, alias);
                                    Nxt.aliasIdToAliasMappings.put(aliasId, alias);

                                } else {

                                    alias.uri = attachment.uri;
                                    alias.timestamp = timestamp;

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

                                Transaction.ColoredCoinsAssetIssuanceAttachment attachment = (Transaction.ColoredCoinsAssetIssuanceAttachment)transaction.attachment;

                                long assetId = transaction.getId();
                                Asset asset = new Asset(sender, attachment.name, attachment.description, attachment.quantity);
                                Nxt.assets.put(assetId, asset);
                                Nxt.assetNameToIdMappings.put(attachment.name.toLowerCase(), assetId);
                                Nxt.sortedAskOrders.put(assetId, new TreeSet<AskOrder>());
                                Nxt.sortedBidOrders.put(assetId, new TreeSet<BidOrder>());
                                senderAccount.addToAssetAndUnconfirmedAssetBalance(assetId, attachment.quantity);

                            }
                            break;

                            case Transaction.SUBTYPE_COLORED_COINS_ASSET_TRANSFER:
                            {

                                Transaction.ColoredCoinsAssetTransferAttachment attachment = (Transaction.ColoredCoinsAssetTransferAttachment)transaction.attachment;

                                senderAccount.addToAssetAndUnconfirmedAssetBalance(attachment.asset, -attachment.quantity);
                                recipientAccount.addToAssetAndUnconfirmedAssetBalance(attachment.asset, attachment.quantity);

                            }
                            break;

                            case Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
                            {

                                Transaction.ColoredCoinsAskOrderPlacementAttachment attachment = (Transaction.ColoredCoinsAskOrderPlacementAttachment)transaction.attachment;

                                AskOrder order = new AskOrder(transaction.getId(), senderAccount, attachment.asset, attachment.quantity, attachment.price);
                                senderAccount.addToAssetAndUnconfirmedAssetBalance(attachment.asset, -attachment.quantity);
                                Nxt.askOrders.put(order.id, order);
                                Nxt.sortedAskOrders.get(attachment.asset).add(order);
                                Nxt.matchOrders(attachment.asset);

                            }
                            break;

                            case Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT:
                            {

                                Transaction.ColoredCoinsBidOrderPlacementAttachment attachment = (Transaction.ColoredCoinsBidOrderPlacementAttachment)transaction.attachment;

                                BidOrder order = new BidOrder(transaction.getId(), senderAccount, attachment.asset, attachment.quantity, attachment.price);

                                senderAccount.addToBalanceAndUnconfirmedBalance(- attachment.quantity * attachment.price);

                                Nxt.bidOrders.put(order.id, order);
                                Nxt.sortedBidOrders.get(attachment.asset).add(order);

                                Nxt.matchOrders(attachment.asset);

                            }
                            break;

                            case Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
                            {

                                Transaction.ColoredCoinsAskOrderCancellationAttachment attachment = (Transaction.ColoredCoinsAskOrderCancellationAttachment)transaction.attachment;

                                AskOrder order;
                                order = Nxt.askOrders.remove(attachment.order);
                                Nxt.sortedAskOrders.get(order.asset).remove(order);
                                senderAccount.addToAssetAndUnconfirmedAssetBalance(order.asset, order.quantity);

                            }
                            break;

                            case Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
                            {

                                Transaction.ColoredCoinsBidOrderCancellationAttachment attachment = (Transaction.ColoredCoinsBidOrderCancellationAttachment)transaction.attachment;

                                BidOrder order;
                                order = Nxt.bidOrders.remove(attachment.order);
                                Nxt.sortedBidOrders.get(order.asset).remove(order);
                                senderAccount.addToBalanceAndUnconfirmedBalance(order.quantity * order.price);

                            }
                            break;

                        }

                    }
                    break;

                }

            }
        }

    }

    private long calculateBaseTarget() {

        if (getId() == Nxt.GENESIS_BLOCK_ID) {

            return Nxt.initialBaseTarget;

        }

        Block previousBlock = Nxt.blocks.get(this.previousBlock);
        long curBaseTarget = previousBlock.baseTarget;
        long newBaseTarget = BigInteger.valueOf(curBaseTarget).multiply(BigInteger.valueOf(timestamp - previousBlock.timestamp)).divide(BigInteger.valueOf(60)).longValue();
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

    static Block getBlock(JSONObject blockData) {

        try {
            int version = ((Long)blockData.get("version")).intValue();
            int timestamp = ((Long)blockData.get("timestamp")).intValue();
            long previousBlock = Convert.parseUnsignedLong((String) blockData.get("previousBlock"));
            int numberOfTransactions = ((Long)blockData.get("numberOfTransactions")).intValue();
            int totalAmount = ((Long)blockData.get("totalAmount")).intValue();
            int totalFee = ((Long)blockData.get("totalFee")).intValue();
            int payloadLength = ((Long)blockData.get("payloadLength")).intValue();
            byte[] payloadHash = Convert.convert((String) blockData.get("payloadHash"));
            byte[] generatorPublicKey = Convert.convert((String) blockData.get("generatorPublicKey"));
            byte[] generationSignature = Convert.convert((String) blockData.get("generationSignature"));
            byte[] blockSignature = Convert.convert((String) blockData.get("blockSignature"));

            byte[] previousBlockHash = version == 1 ? null : Convert.convert((String) blockData.get("previousBlockHash"));

            if (numberOfTransactions > Nxt.MAX_NUMBER_OF_TRANSACTIONS || payloadLength > Nxt.MAX_PAYLOAD_LENGTH) {

                return null;

            }
            return new Block(version, timestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash);
        } catch (RuntimeException e) {
            //logDebugMessage("Failed to parse JSON block data");
            //logDebugMessage(blockData.toJSONString());
            return null;
        }
    }

    byte[] getBytes() {

        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 8 + 4 + 4 + 4 + 4 + 32 + 32 + (32 + 32) + 64);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(version);
        buffer.putInt(timestamp);
        buffer.putLong(previousBlock);
        buffer.putInt(this.transactions.length);
        buffer.putInt(totalAmount);
        buffer.putInt(totalFee);
        buffer.putInt(payloadLength);
        buffer.put(payloadHash);
        buffer.put(generatorPublicKey);
        buffer.put(generationSignature);
        if (version > 1) {

            buffer.put(previousBlockHash);

        }
        buffer.put(blockSignature);

        return buffer.array();

    }

    transient volatile long id;
    transient volatile String stringId = null;
    transient volatile long generatorAccountId;

    long getId() {
        calculateIds();
        return id;
    }


    String getStringId() {
        calculateIds();
        return stringId;
    }

    long getGeneratorAccountId() {
        calculateIds();
        return generatorAccountId;
    }

    private void calculateIds() {
        if (stringId != null) {
            return;
        }
        byte[] hash = Crypto.getMessageDigest("SHA-256").digest(getBytes());
        BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
        id = bigInteger.longValue();
        stringId = bigInteger.toString();
        generatorAccountId = Account.getId(generatorPublicKey);
    }

    JSONObject getJSONObject() {

        JSONObject block = new JSONObject();

        block.put("version", version);
        block.put("timestamp", timestamp);
        block.put("previousBlock", Convert.convert(previousBlock));
        block.put("numberOfTransactions", this.transactions.length);
        block.put("totalAmount", totalAmount);
        block.put("totalFee", totalFee);
        block.put("payloadLength", payloadLength);
        block.put("payloadHash", Convert.convert(payloadHash));
        block.put("generatorPublicKey", Convert.convert(generatorPublicKey));
        block.put("generationSignature", Convert.convert(generationSignature));
        if (version > 1) {

            block.put("previousBlockHash", Convert.convert(previousBlockHash));

        }
        block.put("blockSignature", Convert.convert(blockSignature));

        JSONArray transactionsData = new JSONArray();
        for (Transaction transaction : this.blockTransactions) {

            transactionsData.add(transaction.getJSONObject());

        }
        block.put("transactions", transactionsData);

        return block;

    }

    private transient SoftReference<JSONStreamAware> jsonRef;

    synchronized JSONStreamAware getJSONStreamAware() {
        JSONStreamAware json;
        if (jsonRef != null) {
            json = jsonRef.get();
            if (json != null) {
                return json;
            }
        }
        json = new JSONStreamAware() {
            private char[] jsonChars = getJSONObject().toJSONString().toCharArray();
            @Override
            public void writeJSONString(Writer out) throws IOException {
                out.write(jsonChars);
            }
        };
        jsonRef = new SoftReference<>(json);
        return json;
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

    public static final Comparator<Block> heightComparator = new Comparator<Block>() {
        @Override
        public int compare(Block o1, Block o2) {
            return o1.height < o2.height ? -1 : (o1.height > o2.height ? 1 : 0);
        }
    };

    static void loadBlocks(String fileName) throws FileNotFoundException {

        try (FileInputStream fileInputStream = new FileInputStream(fileName);
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)
        ) {
            Nxt.blockCounter.set(objectInputStream.readInt());
            Nxt.blocks.clear();
            Nxt.blocks.putAll((HashMap<Long, Block>) objectInputStream.readObject());
            //lastBlock.set(blocks.get(objectInputStream.readLong()));
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException|ClassNotFoundException e) {
            Logger.logMessage("Error loading blocks from " + fileName, e);
            System.exit(1);
        }

    }

    static boolean popLastBlock() {

        try {

            JSONObject response = new JSONObject();
            response.put("response", "processNewData");

            JSONArray addedUnconfirmedTransactions = new JSONArray();

            Block block;

            synchronized (Nxt.blocksAndTransactionsLock) {

                block = Nxt.lastBlock.get();

                if (block.getId() == Nxt.GENESIS_BLOCK_ID) {
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

    static boolean pushBlock(ByteBuffer buffer, boolean savingFlag) {

        Block block;
        JSONArray addedConfirmedTransactions;
        JSONArray removedUnconfirmedTransactions;
        int curTime = Nxt.getEpochTime(System.currentTimeMillis());

        synchronized (Nxt.blocksAndTransactionsLock) {
            try {

                Block previousLastBlock = Nxt.lastBlock.get();
                buffer.flip();

                int version = buffer.getInt();
                if (version != (previousLastBlock.height < Nxt.TRANSPARENT_FORGING_BLOCK ? 1 : 2)) {

                    return false;

                }

                if (previousLastBlock.height == Nxt.TRANSPARENT_FORGING_BLOCK) {

                    byte[] checksum = Transaction.calculateTransactionsChecksum();
                    if (Nxt.CHECKSUM_TRANSPARENT_FORGING == null) {
                        System.out.println(Arrays.toString(checksum));
                    } else if (!Arrays.equals(checksum, Nxt.CHECKSUM_TRANSPARENT_FORGING)) {
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

                    if (!Arrays.equals(Crypto.getMessageDigest("SHA-256").digest(previousLastBlock.getBytes()), previousBlockHash)) {

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

                                    if (!blockAliases.add(((Transaction.MessagingAliasAssignmentAttachment)transaction.attachment).alias.toLowerCase())) {

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
                MessageDigest digest = Crypto.getMessageDigest("SHA-256");
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

                            Transaction.ColoredCoinsAssetTransferAttachment attachment = (Transaction.ColoredCoinsAssetTransferAttachment)transaction.attachment;
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

                            Transaction.ColoredCoinsAskOrderPlacementAttachment attachment = (Transaction.ColoredCoinsAskOrderPlacementAttachment)transaction.attachment;
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

                            Transaction.ColoredCoinsBidOrderPlacementAttachment attachment = (Transaction.ColoredCoinsBidOrderPlacementAttachment)transaction.attachment;
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

                //synchronized (blocksAndTransactionsLock) {

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

                block.analyze();

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
                /*
                long blockId = block.getId();
                for (long transactionId : block.transactions) {

                    Nxt.transactions.get(transactionId).block = blockId;

                }
                */

                if (savingFlag) {

                    Transaction.saveTransactions("transactions.nxt");
                    Block.saveBlocks("blocks.nxt", false);

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

    static void saveBlocks(String fileName, boolean flag) {

        try (FileOutputStream fileOutputStream = new FileOutputStream(fileName);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)
        ) {
            objectOutputStream.writeInt(Nxt.blockCounter.get());
            objectOutputStream.writeObject(new HashMap<>(Nxt.blocks));
            //objectOutputStream.writeLong(lastBlock.get().getId());
        } catch (IOException e) {
            Logger.logMessage("Error saving blocks to " + fileName, e);
            throw new RuntimeException(e);
        }

            /*if (flag) {

                ByteBuffer buffer = ByteBuffer.allocate(BLOCK_HEADER_LENGTH + MAX_PAYLOAD_LENGTH);
                buffer.order(ByteOrder.LITTLE_ENDIAN);

                long curBlockId = GENESIS_BLOCK_ID;
                long prevBlockPtr = -1, curBlockPtr = 0;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                do {

                    Block block = blocks.get(curBlockId);
                    buffer.clear();
                    buffer.putLong(prevBlockPtr);
                    buffer.put(block.getBytes());
                    for (int i = 0; i < block.numberOfTransactions; i++) {

                        buffer.put(Nxt.transactions.get(block.transactions[i]).getBytes());

                    }
                    buffer.flip();
                    byte[] rawBytes = new byte[buffer.limit()];
                    buffer.get(rawBytes);

                    MappedByteBuffer window = blockchainChannel.map(FileChannel.MapMode.READ_WRITE, curBlockPtr, 32 + rawBytes.length);
                    window.put(digest.digest(rawBytes));
                    window.put(rawBytes);

                    prevBlockPtr = curBlockPtr;
                    curBlockPtr += 32 + rawBytes.length;
                    curBlockId = block.nextBlock;

                } while (curBlockId != 0);

            }*/

    }

    boolean verifyBlockSignature() {

        Account account = Nxt.accounts.get(getGeneratorAccountId());
        if (account == null) {

            return false;

        }

        byte[] data = getBytes();
        byte[] data2 = new byte[data.length - 64];
        System.arraycopy(data, 0, data2, 0, data2.length);

        return Crypto.verify(blockSignature, data2, generatorPublicKey) && account.setOrVerify(generatorPublicKey);

    }

    boolean verifyGenerationSignature() {

        try {

            Block previousBlock = Nxt.blocks.get(this.previousBlock);
            if (previousBlock == null) {

                return false;

            }

            if (version == 1 && !Crypto.verify(generationSignature, previousBlock.generationSignature, generatorPublicKey)) {

                return false;

            }

            Account account = Nxt.accounts.get(getGeneratorAccountId());
            if (account == null || account.getEffectiveBalance() <= 0) {

                return false;

            }

            int elapsedTime = timestamp - previousBlock.timestamp;
            BigInteger target = BigInteger.valueOf(Nxt.lastBlock.get().baseTarget).multiply(BigInteger.valueOf(account.getEffectiveBalance())).multiply(BigInteger.valueOf(elapsedTime));

            MessageDigest digest = Crypto.getMessageDigest("SHA-256");
            byte[] generationSignatureHash;
            if (version == 1) {

                generationSignatureHash = digest.digest(generationSignature);

            } else {

                digest.update(previousBlock.generationSignature);
                generationSignatureHash = digest.digest(generatorPublicKey);
                if (!Arrays.equals(generationSignature, generationSignatureHash)) {

                    return false;

                }

            }

            BigInteger hit = new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});

            return hit.compareTo(target) < 0;

        } catch (RuntimeException e) {

            Logger.logMessage("Error verifying block generation signature", e);
            return false;

        }

    }

}
