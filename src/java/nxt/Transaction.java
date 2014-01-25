package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public final class Transaction implements Comparable<Transaction>, Serializable {

    static final long serialVersionUID = 0;

    private static final byte TYPE_PAYMENT = 0;
    private static final byte TYPE_MESSAGING = 1;
    private static final byte TYPE_COLORED_COINS = 2;

    private static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;

    private static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
    private static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;

    private static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
    private static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
    private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
    private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
    private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
    private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;

    public static final int ASSET_ISSUANCE_FEE = 1000;

    public int timestamp;
    public final short deadline;
    public final byte[] senderPublicKey;
    public final long recipient;
    public final int amount;
    public final int fee;
    final long referencedTransaction;
    public byte[] signature;
    Attachment attachment;
    private transient Type type;

    public int index;
    public long block;
    public int height;

    private Transaction(Type type, int timestamp, short deadline, byte[] senderPublicKey, long recipient,
                       int amount, int fee, long referencedTransaction, byte[] signature) {

        if (type == null) {
            throw new IllegalArgumentException("Invalid transaction type or subtype");
        }

        this.timestamp = timestamp;
        this.deadline = deadline;
        this.senderPublicKey = senderPublicKey;
        this.recipient = recipient;
        this.amount = amount;
        this.fee = fee;
        this.referencedTransaction = referencedTransaction;
        this.signature = signature;
        this.type = type;
        height = Integer.MAX_VALUE;

    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.write(type.getType());
        out.write(type.getSubtype());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.type = findTransactionType(in.readByte(), in.readByte());
    }

    public Type getType() {
        return type;
    }

    @Override
    public final int compareTo(Transaction o) {

        if (height < o.height) {

            return -1;

        } else if (height > o.height) {

            return 1;

        } else {

            // equivalent to: fee * 1048576L / getSize() > o.fee * 1048576L / o.getSize()
            if (fee * o.getSize() > o.fee * getSize()) {

                return -1;

            } else if (fee * o.getSize() < o.fee * getSize()) {

                return 1;

            } else {

                if (timestamp < o.timestamp) {

                    return -1;

                } else if (timestamp > o.timestamp) {

                    return 1;

                } else {

                    if (index < o.index) {

                        return -1;

                    } else if (index > o.index) {

                        return 1;

                    } else {

                        return 0;

                    }

                }

            }

        }

    }

    final void loadAttachment(ByteBuffer buffer) {
        type.loadAttachment(this, buffer);
    }

    final void loadAttachment(JSONObject attachmentData) {
        type.loadAttachment(this, attachmentData);
    }

    // returns true iff double spending
    final boolean preProcess() {
        Account senderAccount = Nxt.accounts.get(getSenderAccountId());
        if (senderAccount == null) {
            return true;
        }
        synchronized(senderAccount) {
            return type.preProcess(this, senderAccount, this.amount + this.fee);
        }
    }

    final void apply() {
        Account senderAccount = Nxt.accounts.get(getSenderAccountId());
        if (! senderAccount.setOrVerify(senderPublicKey)) {
            throw new RuntimeException("sender public key mismatch");
            // shouldn't happen, because transactions are already verified somewhere higher in pushBlock...
        }
        Account recipientAccount = Nxt.accounts.get(recipient);
        if (recipientAccount == null) {
            recipientAccount = Account.addAccount(recipient);
        }
        senderAccount.addToBalanceAndUnconfirmedBalance(- (amount + fee) * 100L);
        type.apply(this, senderAccount, recipientAccount);
    }

    final void updateTotals(Map<Long,Long> accumulatedAmounts, Map<Long,Map<Long,Long>> accumulatedAssetQuantities) {
        long sender = getSenderAccountId();
        Long accumulatedAmount = accumulatedAmounts.get(sender);
        if (accumulatedAmount == null) {
            accumulatedAmount = 0L;
        }
        accumulatedAmounts.put(sender, accumulatedAmount + (amount + fee) * 100L);
        type.updateTotals(this, accumulatedAmounts, accumulatedAssetQuantities, accumulatedAmount);
    }

    public static final Comparator<Transaction> timestampComparator = new Comparator<Transaction>() {
        @Override
        public int compare(Transaction o1, Transaction o2) {
            return o1.timestamp < o2.timestamp ? -1 : (o1.timestamp > o2.timestamp ? 1 : 0);
        }
    };

    private static final int TRANSACTION_BYTES_LENGTH = 1 + 1 + 4 + 2 + 32 + 8 + 4 + 4 + 8 + 64;

    final int getSize() {
        return TRANSACTION_BYTES_LENGTH + (attachment == null ? 0 : attachment.getSize());
    }

    public final byte[] getBytes() {

        ByteBuffer buffer = ByteBuffer.allocate(getSize());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(type.getType());
        buffer.put(type.getSubtype());
        buffer.putInt(timestamp);
        buffer.putShort(deadline);
        buffer.put(senderPublicKey);
        buffer.putLong(recipient);
        buffer.putInt(amount);
        buffer.putInt(fee);
        buffer.putLong(referencedTransaction);
        buffer.put(signature);
        if (attachment != null) {
            buffer.put(attachment.getBytes());
        }
        return buffer.array();

    }

    private transient volatile long id;
    private transient volatile String stringId = null;
    private transient volatile long senderAccountId;

    public final long getId() {
        calculateIds();
        return id;
    }


    public final String getStringId() {
        calculateIds();
        return stringId;
    }

    public final long getSenderAccountId() {
        calculateIds();
        return senderAccountId;
    }

    private void calculateIds() {
        if (stringId != null) {
            return;
        }
        byte[] hash = Crypto.sha256().digest(getBytes());
        BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
        id = bigInteger.longValue();
        senderAccountId = Account.getId(senderPublicKey);
        stringId = bigInteger.toString();
    }


    public final JSONObject getJSONObject() {

        JSONObject transaction = new JSONObject();

        transaction.put("type", type.getType());
        transaction.put("subtype", type.getSubtype());
        transaction.put("timestamp", timestamp);
        transaction.put("deadline", deadline);
        transaction.put("senderPublicKey", Convert.convert(senderPublicKey));
        transaction.put("recipient", Convert.convert(recipient));
        transaction.put("amount", amount);
        transaction.put("fee", fee);
        transaction.put("referencedTransaction", Convert.convert(referencedTransaction));
        transaction.put("signature", Convert.convert(signature));
        if (attachment != null) {
            transaction.put("attachment", attachment.getJSONObject());
        }

        return transaction;
    }

    final long getRecipientDeltaBalance() {
        return amount * 100L + type.getRecipientDeltaBalance(this);
    }

    final long getSenderDeltaBalance() {
        return -(amount + fee) * 100L + type.getSenderDeltaBalance(this);
    }

    public static Transaction getTransaction(ByteBuffer buffer) {

        byte type = buffer.get();
        byte subtype = buffer.get();
        int timestamp = buffer.getInt();
        short deadline = buffer.getShort();
        byte[] senderPublicKey = new byte[32];
        buffer.get(senderPublicKey);
        long recipient = buffer.getLong();
        int amount = buffer.getInt();
        int fee = buffer.getInt();
        long referencedTransaction = buffer.getLong();
        byte[] signature = new byte[64];
        buffer.get(signature);

        Transaction transaction = new Transaction(findTransactionType(type, subtype), timestamp, deadline, senderPublicKey, recipient, amount,
                fee, referencedTransaction, signature);
        transaction.loadAttachment(buffer);

        return transaction;
    }

    public static Transaction getTransaction(JSONObject transactionData) {

        byte type = ((Long)transactionData.get("type")).byteValue();
        byte subtype = ((Long)transactionData.get("subtype")).byteValue();
        int timestamp = ((Long)transactionData.get("timestamp")).intValue();
        short deadline = ((Long)transactionData.get("deadline")).shortValue();
        byte[] senderPublicKey = Convert.convert((String) transactionData.get("senderPublicKey"));
        long recipient = Convert.parseUnsignedLong((String) transactionData.get("recipient"));
        int amount = ((Long)transactionData.get("amount")).intValue();
        int fee = ((Long)transactionData.get("fee")).intValue();
        long referencedTransaction = Convert.parseUnsignedLong((String) transactionData.get("referencedTransaction"));
        byte[] signature = Convert.convert((String) transactionData.get("signature"));

        Transaction transaction = new Transaction(findTransactionType(type, subtype), timestamp, deadline, senderPublicKey, recipient, amount, fee,
                referencedTransaction, signature);

        JSONObject attachmentData = (JSONObject)transactionData.get("attachment");
        transaction.loadAttachment(attachmentData);

        return transaction;
    }

    public final void sign(String secretPhrase) {

        if (signature != null) {
            throw new IllegalStateException("Transaction already signed");
        }

        signature = new byte[64]; // ugly but signature is needed by getBytes()
        signature = Crypto.sign(getBytes(), secretPhrase);

        try {

            while (!verify()) {

                timestamp++;
                // cfb: Sometimes EC-KCDSA generates unverifiable signatures (X*0 == Y*0 case), Crypto.sign() will be rewritten later
                signature = new byte[64];
                signature = Crypto.sign(getBytes(), secretPhrase);

            }

        } catch (RuntimeException e) {

            Logger.logMessage("Error signing transaction", e);

        }

    }

    final boolean validateAttachment() {
        return type.validateAttachment(this);
    }

    final boolean verify() {

        Account account = Nxt.accounts.get(getSenderAccountId());
        if (account == null) {

            return false;

        }

        byte[] data = getBytes();
        for (int i = 64; i < 128; i++) {

            data[i] = 0;

        }

        return Crypto.verify(signature, data, senderPublicKey) && account.setOrVerify(senderPublicKey);


    }

    public static Transaction newTransaction(int timestamp, short deadline, byte[] senderPublicKey, long recipient,
                                             int amount, int fee, long referencedTransaction) {
        return new Transaction(Type.Payment.ORDINARY, timestamp, deadline, senderPublicKey, recipient, amount, fee, referencedTransaction, null);
    }

    public static Transaction newTransaction(int timestamp, short deadline, byte[] senderPublicKey, long recipient,
                                                   int amount, int fee, long referencedTransaction, Attachment attachment) {
        Transaction transaction = new Transaction(attachment.getTransactionType(), timestamp, deadline, senderPublicKey, recipient, amount, fee,
                referencedTransaction, null);
        transaction.attachment = attachment;
        return transaction;
    }

    static Transaction newTransaction(int timestamp, short deadline, byte[] senderPublicKey, long recipient,
                                             int amount, int fee, long referencedTransaction, byte[] signature) {
        return new Transaction(Type.Payment.ORDINARY, timestamp, deadline, senderPublicKey, recipient, amount, fee, referencedTransaction, signature);
    }

    private static Type findTransactionType(byte type, byte subtype) {
        switch (type) {
            case TYPE_PAYMENT:
                switch (subtype) {
                    case SUBTYPE_PAYMENT_ORDINARY_PAYMENT:
                        return Type.Payment.ORDINARY;
                    default:
                        return null;
                }
            case TYPE_MESSAGING:
                switch (subtype) {
                    case SUBTYPE_MESSAGING_ARBITRARY_MESSAGE:
                        return Type.Messaging.ARBITRARY_MESSAGE;
                    case SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
                        return Type.Messaging.ALIAS_ASSIGNMENT;
                    default:
                        return null;
                }
            case TYPE_COLORED_COINS:
                switch (subtype) {
                    case SUBTYPE_COLORED_COINS_ASSET_ISSUANCE:
                        return Type.ColoredCoins.ASSET_ISSUANCE;
                    case SUBTYPE_COLORED_COINS_ASSET_TRANSFER:
                        return Type.ColoredCoins.ASSET_TRANSFER;
                    case SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
                        return Type.ColoredCoins.ASK_ORDER_PLACEMENT;
                    case SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT:
                        return Type.ColoredCoins.BID_ORDER_PLACEMENT;
                    case SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
                        return Type.ColoredCoins.ASK_ORDER_CANCELLATION;
                    case SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
                        return Type.ColoredCoins.BID_ORDER_CANCELLATION;
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    public static abstract class Type {

        public abstract byte getType();

        public abstract byte getSubtype();

        abstract void loadAttachment(Transaction transaction, ByteBuffer buffer);

        abstract void loadAttachment(Transaction transaction, JSONObject attachmentData);

        final boolean validateAttachment(Transaction transaction) {
            //TODO: this check may no longer be needed here now
            return transaction.fee <= Nxt.MAX_BALANCE && doValidateAttachment(transaction);
        }

        abstract boolean doValidateAttachment(Transaction transaction);

        // return true iff double spending
        final boolean preProcess(Transaction transaction, Account senderAccount, int totalAmount) {
            if (senderAccount.getUnconfirmedBalance() < totalAmount * 100L) {
                return true;
            }
            senderAccount.addToUnconfirmedBalance(- totalAmount * 100L);
            return doPreProcess(transaction, senderAccount, totalAmount);
        }

        abstract boolean doPreProcess(Transaction transaction, Account senderAccount, int totalAmount);

        abstract void apply(Transaction transaction, Account senderAccount, Account recipientAccount);

        abstract void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                                   Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount);

        abstract long getRecipientDeltaBalance(Transaction transaction);

        abstract long getSenderDeltaBalance(Transaction transaction);

        public static abstract class Payment extends Type {

            @Override
            public final byte getType() {
                return TYPE_PAYMENT;
            }

            @Override
            protected final void loadAttachment(Transaction transaction, ByteBuffer buffer) {}

            @Override
            protected final void loadAttachment(Transaction transaction, JSONObject attachmentData) {}

            @Override
            final long getRecipientDeltaBalance(Transaction transaction) {
                return 0;
            }

            @Override
            final long getSenderDeltaBalance(Transaction transaction) {
                return 0;
            }

            public static final Type ORDINARY = new Payment() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_PAYMENT_ORDINARY_PAYMENT;
                }

                @Override
                boolean doValidateAttachment(Transaction transaction) {
                    return transaction.amount > 0 && transaction.amount < Nxt.MAX_BALANCE;
                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    recipientAccount.addToBalanceAndUnconfirmedBalance(transaction.amount * 100L);
                }

                @Override
                void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                                  Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {}

                @Override
                boolean doPreProcess(Transaction transaction, Account senderAccount, int totalAmount) {
                    return false;
                }

            };
        }

        public static abstract class Messaging extends Type {

            @Override
            public final byte getType() {
                return TYPE_MESSAGING;
            }

            @Override
            boolean doPreProcess(Transaction transaction, Account senderAccount, int totalAmount) {
                return false;
            }

            @Override
            void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                              Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {}

            @Override
            final long getRecipientDeltaBalance(Transaction transaction) {
                return 0;
            }

            @Override
            final long getSenderDeltaBalance(Transaction transaction) {
                return 0;
            }

            public final static Type ARBITRARY_MESSAGE = new Messaging() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_MESSAGING_ARBITRARY_MESSAGE;
                }

                @Override
                protected void loadAttachment(Transaction transaction, ByteBuffer buffer) {
                    int messageLength = buffer.getInt();
                    if (messageLength <= Nxt.MAX_ARBITRARY_MESSAGE_LENGTH) {
                        byte[] message = new byte[messageLength];
                        buffer.get(message);
                        transaction.attachment = new Attachment.MessagingArbitraryMessage(message);
                    }
                }

                @Override
                protected void loadAttachment(Transaction transaction, JSONObject attachmentData) {
                    String message = (String)attachmentData.get("message");
                    transaction.attachment = new Attachment.MessagingArbitraryMessage(Convert.convert(message));
                }

                @Override
                boolean doValidateAttachment(Transaction transaction) {
                    if (Blockchain.getLastBlock().height < Nxt.ARBITRARY_MESSAGES_BLOCK) {
                        return false;
                    }
                    try {
                        Attachment.MessagingArbitraryMessage attachment = (Attachment.MessagingArbitraryMessage)transaction.attachment;
                        return transaction.amount == 0 && attachment.message.length <= Nxt.MAX_ARBITRARY_MESSAGE_LENGTH;
                    } catch (RuntimeException e) {
                        Logger.logDebugMessage("Error validating arbitrary message", e);
                        return false;
                    }
                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {}

            };

            public static final Type ALIAS_ASSIGNMENT = new Messaging() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT;
                }

                @Override
                protected void loadAttachment(Transaction transaction, ByteBuffer buffer) {
                    int aliasLength = buffer.get();
                    byte[] alias = new byte[aliasLength];
                    buffer.get(alias);
                    int uriLength = buffer.getShort();
                    byte[] uri = new byte[uriLength];
                    buffer.get(uri);
                    try {
                        transaction.attachment = new Attachment.MessagingAliasAssignment(new String(alias, "UTF-8").intern(),
                                new String(uri, "UTF-8").intern());
                    } catch (RuntimeException|UnsupportedEncodingException e) {
                        Logger.logDebugMessage("Error parsing alias assignment", e);
                    }
                }

                @Override
                protected void loadAttachment(Transaction transaction, JSONObject attachmentData) {
                    String alias = (String)attachmentData.get("alias");
                    String uri = (String)attachmentData.get("uri");
                    transaction.attachment = new Attachment.MessagingAliasAssignment(alias.trim(), uri.trim());
                }

                @Override
                boolean doValidateAttachment(Transaction transaction) {
                    if (Blockchain.getLastBlock().height < Nxt.ALIAS_SYSTEM_BLOCK) {
                        return false;
                    }
                    try {
                        Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment)transaction.attachment;
                        if (transaction.recipient != Genesis.CREATOR_ID || transaction.amount != 0 || attachment.alias.length() == 0
                                || attachment.alias.length() > 100 || attachment.uri.length() > 1000) {
                            return false;
                        } else {
                            String normalizedAlias = attachment.alias.toLowerCase();
                            for (int i = 0; i < normalizedAlias.length(); i++) {
                                if (Convert.alphabet.indexOf(normalizedAlias.charAt(i)) < 0) {
                                    return false;
                                }
                            }
                            Alias alias = Nxt.aliases.get(normalizedAlias);
                            return alias == null || Arrays.equals(alias.account.publicKey.get(), transaction.senderPublicKey);
                        }
                    } catch (RuntimeException e) {
                        Logger.logDebugMessage("Error in alias assignment validation", e);
                        return false;
                    }
                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment)transaction.attachment;
                    String normalizedAlias = attachment.alias.toLowerCase();
                    Alias alias = Nxt.aliases.get(normalizedAlias);
                    Block block = Blockchain.getBlock(transaction.block);
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
            };
        }

        public static abstract class ColoredCoins extends Type {

            @Override
            public final byte getType() {
                return TYPE_COLORED_COINS;
            }

            public static final Type ASSET_ISSUANCE = new ColoredCoins() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_COLORED_COINS_ASSET_ISSUANCE;
                }

                @Override
                protected void loadAttachment(Transaction transaction, ByteBuffer buffer) {
                    int nameLength = buffer.get();
                    byte[] name = new byte[nameLength];
                    buffer.get(name);
                    int descriptionLength = buffer.getShort();
                    byte[] description = new byte[descriptionLength];
                    buffer.get(description);
                    int quantity = buffer.getInt();
                    try {
                        transaction.attachment = new Attachment.ColoredCoinsAssetIssuance(new String(name, "UTF-8").intern(),
                                new String(description, "UTF-8").intern(), quantity);
                    } catch (RuntimeException|UnsupportedEncodingException e) {
                        Logger.logDebugMessage("Error in asset issuance", e);
                    }
                }

                @Override
                protected void loadAttachment(Transaction transaction, JSONObject attachmentData) {
                    String name = (String)attachmentData.get("name");
                    String description = (String)attachmentData.get("description");
                    int quantity = ((Long)attachmentData.get("quantity")).intValue();
                    transaction.attachment = new Attachment.ColoredCoinsAssetIssuance(name.trim(), description.trim(), quantity);
                }

                @Override
                boolean doValidateAttachment(Transaction transaction) {
                    try {
                        Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.attachment;
                        if (transaction.recipient != Genesis.CREATOR_ID || transaction.amount != 0 || transaction.fee < ASSET_ISSUANCE_FEE
                                || attachment.name.length() < 3 || attachment.name.length() > 10 || attachment.description.length() > 1000
                                || attachment.quantity <= 0 || attachment.quantity > Nxt.MAX_ASSET_QUANTITY) {
                            return false;
                        } else {
                            String normalizedName = attachment.name.toLowerCase();
                            for (int i = 0; i < normalizedName.length(); i++) {
                                if (Convert.alphabet.indexOf(normalizedName.charAt(i)) < 0) {
                                    return false;
                                }
                            }
                            return Nxt.assetNameToIdMappings.get(normalizedName) == null;
                        }
                    } catch (RuntimeException e) {
                        Logger.logDebugMessage("Error validating colored coins asset issuance", e);
                        return false;
                    }
                }

                @Override
                boolean doPreProcess(Transaction transaction, Account senderAccount, int totalAmount) {
                    return false;
                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {

                    Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.attachment;
                    long assetId = transaction.getId();
                    Asset asset = new Asset(transaction.getSenderAccountId(), attachment.name, attachment.description, attachment.quantity);
                    Nxt.assets.put(assetId, asset);
                    Nxt.assetNameToIdMappings.put(attachment.name.toLowerCase(), assetId);
                    Blockchain.sortedAskOrders.put(assetId, new TreeSet<AskOrder>());
                    Blockchain.sortedBidOrders.put(assetId, new TreeSet<BidOrder>());
                    senderAccount.addToAssetAndUnconfirmedAssetBalance(assetId, attachment.quantity);

                }

                @Override
                void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                                 Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {}

                @Override
                final long getRecipientDeltaBalance(Transaction transaction) {
                    return 0;
                }

                @Override
                final long getSenderDeltaBalance(Transaction transaction) {
                    return 0;
                }
            };

            public static final Type ASSET_TRANSFER = new ColoredCoins() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_COLORED_COINS_ASSET_TRANSFER;
                }

                @Override
                protected void loadAttachment(Transaction transaction, ByteBuffer buffer) {
                    long asset = buffer.getLong();
                    int quantity = buffer.getInt();
                    transaction.attachment = new Attachment.ColoredCoinsAssetTransfer(asset, quantity);
                }

                @Override
                protected void loadAttachment(Transaction transaction, JSONObject attachmentData) {
                    long asset = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
                    int quantity = ((Long)attachmentData.get("quantity")).intValue();
                    transaction.attachment = new Attachment.ColoredCoinsAssetTransfer(asset, quantity);
                }

                @Override
                boolean doValidateAttachment(Transaction transaction) {
                    Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.attachment;
                    return transaction.amount == 0 && attachment.quantity > 0 && attachment.quantity <= Nxt.MAX_ASSET_QUANTITY;
                }

                @Override
                boolean doPreProcess(Transaction transaction, Account account, int totalAmount) {
                    Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.attachment;
                    Integer unconfirmedAssetBalance = account.getUnconfirmedAssetBalance(attachment.asset);
                    if (unconfirmedAssetBalance == null || unconfirmedAssetBalance < attachment.quantity) {
                        account.addToUnconfirmedBalance(totalAmount * 100L);
                        return true;
                    } else {
                        account.addToUnconfirmedAssetBalance(attachment.asset, -attachment.quantity);
                        return false;
                    }
                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.attachment;
                    senderAccount.addToAssetAndUnconfirmedAssetBalance(attachment.asset, -attachment.quantity);
                    recipientAccount.addToAssetAndUnconfirmedAssetBalance(attachment.asset, attachment.quantity);
                }

                @Override
                void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                                  Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {
                    Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.attachment;
                    Map<Long, Long> accountAccumulatedAssetQuantities = accumulatedAssetQuantities.get(transaction.getSenderAccountId());
                    if (accountAccumulatedAssetQuantities == null) {
                        accountAccumulatedAssetQuantities = new HashMap<>();
                        accumulatedAssetQuantities.put(transaction.getSenderAccountId(), accountAccumulatedAssetQuantities);
                    }
                    Long assetAccumulatedAssetQuantities = accountAccumulatedAssetQuantities.get(attachment.asset);
                    if (assetAccumulatedAssetQuantities == null) {
                        assetAccumulatedAssetQuantities = 0L;
                    }
                    accountAccumulatedAssetQuantities.put(attachment.asset, assetAccumulatedAssetQuantities + attachment.quantity);
                }

                @Override
                final long getRecipientDeltaBalance(Transaction transaction) {
                    return 0;
                }

                @Override
                final long getSenderDeltaBalance(Transaction transaction) {
                    return 0;
                }
            };

            abstract static class ColoredCoinsOrderPlacement extends ColoredCoins {

                protected abstract Attachment.ColoredCoinsOrderPlacement makeAttachment(long asset, int quantity, long price);

                @Override
                protected final void loadAttachment(Transaction transaction, ByteBuffer buffer) {
                    long asset = buffer.getLong();
                    int quantity = buffer.getInt();
                    long price = buffer.getLong();
                    transaction.attachment = makeAttachment(asset, quantity, price);
                }

                @Override
                protected final void loadAttachment(Transaction transaction, JSONObject attachmentData) {
                    long asset = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
                    int quantity = ((Long)attachmentData.get("quantity")).intValue();
                    long price = (Long)attachmentData.get("price");
                    transaction.attachment = makeAttachment(asset, quantity, price);
                }

                @Override
                final boolean doValidateAttachment(Transaction transaction) {
                    Attachment.ColoredCoinsOrderPlacement attachment = (Attachment.ColoredCoinsOrderPlacement)transaction.attachment;
                    return transaction.recipient == Genesis.CREATOR_ID && transaction.amount == 0
                            && attachment.quantity > 0 && attachment.quantity <= Nxt.MAX_ASSET_QUANTITY
                            && attachment.price > 0 && attachment.price <= Nxt.MAX_BALANCE * 100L;
                }

            }

            public static final Type ASK_ORDER_PLACEMENT = new ColoredCoinsOrderPlacement() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT;
                }

                protected final Attachment.ColoredCoinsOrderPlacement makeAttachment(long asset, int quantity, long price) {
                    return new Attachment.ColoredCoinsAskOrderPlacement(asset, quantity, price);
                }

                @Override
                boolean doPreProcess(Transaction transaction, Account account, int totalAmount) {
                    Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement)transaction.attachment;
                    Integer unconfirmedAssetBalance = account.getUnconfirmedAssetBalance(attachment.asset);
                    if (unconfirmedAssetBalance == null || unconfirmedAssetBalance < attachment.quantity) {
                        account.addToUnconfirmedBalance(totalAmount * 100L);
                        return true;
                    } else {
                        account.addToUnconfirmedAssetBalance(attachment.asset, -attachment.quantity);
                        return false;
                    }
                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement)transaction.attachment;
                    AskOrder order = new AskOrder(transaction.getId(), senderAccount, attachment.asset, attachment.quantity, attachment.price);
                    senderAccount.addToAssetAndUnconfirmedAssetBalance(attachment.asset, -attachment.quantity);
                    Blockchain.askOrders.put(order.id, order);
                    Blockchain.sortedAskOrders.get(attachment.asset).add(order);
                    Blockchain.matchOrders(attachment.asset);
                }

                @Override
                void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                                 Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {
                    Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement) transaction.attachment;
                    Map<Long, Long> accountAccumulatedAssetQuantities = accumulatedAssetQuantities.get(transaction.getSenderAccountId());
                    if (accountAccumulatedAssetQuantities == null) {
                        accountAccumulatedAssetQuantities = new HashMap<>();
                        accumulatedAssetQuantities.put(transaction.getSenderAccountId(), accountAccumulatedAssetQuantities);
                    }
                    Long assetAccumulatedAssetQuantities = accountAccumulatedAssetQuantities.get(attachment.asset);
                    if (assetAccumulatedAssetQuantities == null) {
                        assetAccumulatedAssetQuantities = 0L;
                    }
                    accountAccumulatedAssetQuantities.put(attachment.asset, assetAccumulatedAssetQuantities + attachment.quantity);
                }

                @Override
                final long getRecipientDeltaBalance(Transaction transaction) {
                    return 0;
                }

                @Override
                final long getSenderDeltaBalance(Transaction transaction) {
                    return 0;
                }
            };

            public final static Type BID_ORDER_PLACEMENT = new ColoredCoinsOrderPlacement() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT;
                }

                protected final Attachment.ColoredCoinsOrderPlacement makeAttachment(long asset, int quantity, long price) {
                    return new Attachment.ColoredCoinsBidOrderPlacement(asset, quantity, price);
                }

                @Override
                boolean doPreProcess(Transaction transaction, Account account, int totalAmount) {
                    Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.attachment;
                    if (account.getUnconfirmedBalance() < attachment.quantity * attachment.price) {
                        account.addToUnconfirmedBalance(totalAmount * 100L);
                        return true;
                    } else {
                        account.addToUnconfirmedBalance(-attachment.quantity * attachment.price);
                        return false;
                    }
                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement)transaction.attachment;
                    BidOrder order = new BidOrder(transaction.getId(), senderAccount, attachment.asset, attachment.quantity, attachment.price);
                    senderAccount.addToBalanceAndUnconfirmedBalance(- attachment.quantity * attachment.price);
                    Blockchain.bidOrders.put(order.id, order);
                    Blockchain.sortedBidOrders.get(attachment.asset).add(order);
                    Blockchain.matchOrders(attachment.asset);
                }

                @Override
                void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                                 Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {
                    Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.attachment;
                    accumulatedAmounts.put(transaction.getSenderAccountId(), accumulatedAmount + attachment.quantity * attachment.price);
                }

                @Override
                final long getRecipientDeltaBalance(Transaction transaction) {
                    return 0;
                }

                @Override
                final long getSenderDeltaBalance(Transaction transaction) {
                    Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement)transaction.attachment;
                    return -attachment.quantity * attachment.price;
                }
            };

            abstract static class ColoredCoinsOrderCancellation extends ColoredCoins {

                @Override
                final boolean doValidateAttachment(Transaction transaction) {
                    return transaction.recipient == Genesis.CREATOR_ID && transaction.amount == 0;
                }

                @Override
                final boolean doPreProcess(Transaction transaction, Account senderAccount, int totalAmount) {
                    return false;
                }

                @Override
                final void updateTotals(Transaction transaction, Map<Long, Long> accumulatedAmounts,
                                  Map<Long, Map<Long, Long>> accumulatedAssetQuantities, Long accumulatedAmount) {}

            }

            public static final Type ASK_ORDER_CANCELLATION = new ColoredCoinsOrderCancellation() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION;
                }

                @Override
                protected void loadAttachment(Transaction transaction, ByteBuffer buffer) {
                    transaction.attachment = new Attachment.ColoredCoinsAskOrderCancellation(buffer.getLong());
                }

                @Override
                protected void loadAttachment(Transaction transaction, JSONObject attachmentData) {
                    transaction.attachment = new Attachment.ColoredCoinsAskOrderCancellation(Convert.parseUnsignedLong((String) attachmentData.get("order")));
                }


                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    Attachment.ColoredCoinsAskOrderCancellation attachment = (Attachment.ColoredCoinsAskOrderCancellation)transaction.attachment;
                    AskOrder order = Blockchain.askOrders.remove(attachment.order);
                    Blockchain.sortedAskOrders.get(order.asset).remove(order);
                    senderAccount.addToAssetAndUnconfirmedAssetBalance(order.asset, order.quantity);
                }

                @Override
                final long getRecipientDeltaBalance(Transaction transaction) {
                    return 0;
                }

                @Override
                final long getSenderDeltaBalance(Transaction transaction) {
                    return 0;
                }
            };

            public static final Type BID_ORDER_CANCELLATION = new ColoredCoinsOrderCancellation() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION;
                }

                @Override
                protected void loadAttachment(Transaction transaction, ByteBuffer buffer) {
                    transaction.attachment = new Attachment.ColoredCoinsBidOrderCancellation(buffer.getLong());
                }

                @Override
                protected void loadAttachment(Transaction transaction, JSONObject attachmentData) {
                    transaction.attachment = new Attachment.ColoredCoinsBidOrderCancellation(Convert.parseUnsignedLong((String) attachmentData.get("order")));
                }

                @Override
                void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
                    Attachment.ColoredCoinsBidOrderCancellation attachment = (Attachment.ColoredCoinsBidOrderCancellation)transaction.attachment;
                    BidOrder order = Blockchain.bidOrders.remove(attachment.order);
                    Blockchain.sortedBidOrders.get(order.asset).remove(order);
                    senderAccount.addToBalanceAndUnconfirmedBalance(order.quantity * order.price);
                }

                @Override
                final long getRecipientDeltaBalance(Transaction transaction) {
                    return 0;
                }

                @Override
                final long getSenderDeltaBalance(Transaction transaction) {
                    Attachment.ColoredCoinsBidOrderCancellation attachment = (Attachment.ColoredCoinsBidOrderCancellation)transaction.attachment;
                    BidOrder bidOrder = Blockchain.bidOrders.get(attachment.order);
                    if (bidOrder == null) {
                        return 0;
                    }
                    return bidOrder.quantity * bidOrder.price;
                }
            };
        }
    }
}
