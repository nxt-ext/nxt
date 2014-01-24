package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

public final class Transaction implements Comparable<Transaction>, Serializable {

    static final long serialVersionUID = 0;

    public static final byte TYPE_PAYMENT = 0;
    public static final byte TYPE_MESSAGING = 1;
    public static final byte TYPE_COLORED_COINS = 2;

    public static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;

    public static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
    public static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;

    public static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
    public static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
    public static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
    public static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
    public static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
    public static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;

    public static final int ASSET_ISSUANCE_FEE = 1000;

    public int timestamp;
    public final short deadline;
    public final byte[] senderPublicKey;
    public final long recipient;
    public final int amount;
    public final int fee;
    final long referencedTransaction;
    byte[] signature;
    public Attachment attachment;
    private transient Type type;

    public int index;
    public long block;
    public int height;

    private Transaction(int timestamp, short deadline, byte[] senderPublicKey, long recipient,
                       int amount, int fee, long referencedTransaction, byte[] signature, Type type) {

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

        return amount * 100L + (attachment == null ? 0 : attachment.getRecipientDeltaBalance());

    }

    final long getSenderDeltaBalance() {

        return -(amount + fee) * 100L + (attachment == null ? 0 : attachment.getSenderDeltaBalance());

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

        Transaction transaction = newTransaction(type, subtype, timestamp, deadline, senderPublicKey, recipient, amount,
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

        Transaction transaction = newTransaction(type, subtype, timestamp, deadline, senderPublicKey, recipient, amount, fee, referencedTransaction, signature);

        JSONObject attachmentData = (JSONObject)transactionData.get("attachment");
        transaction.loadAttachment(attachmentData);

        return transaction;
    }

    static void loadTransactions(String fileName) throws FileNotFoundException {

        try (FileInputStream fileInputStream = new FileInputStream(fileName);
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
            Nxt.transactionCounter.set(objectInputStream.readInt());
            Nxt.transactions.clear();
            Nxt.transactions.putAll((HashMap<Long, Transaction>) objectInputStream.readObject());
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException |ClassNotFoundException e) {
            Logger.logMessage("Error loading transactions from " + fileName, e);
            System.exit(1);
        }

    }

    static void saveTransactions(String fileName) {

        try (FileOutputStream fileOutputStream = new FileOutputStream(fileName);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)
        ) {
            objectOutputStream.writeInt(Nxt.transactionCounter.get());
            objectOutputStream.writeObject(new HashMap(Nxt.transactions));
            objectOutputStream.close();
        } catch (IOException e) {
            Logger.logMessage("Error saving transactions to " + fileName, e);
            throw new RuntimeException(e);
        }

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

    public static Transaction newTransaction(byte type, byte subtype, int timestamp, short deadline, byte[] senderPublicKey, long recipient,
                                                   int amount, int fee, long referencedTransaction) {
        return newTransaction(type, subtype, timestamp, deadline, senderPublicKey, recipient, amount, fee, referencedTransaction, null);
    }

    public static Transaction newTransaction(byte type, byte subtype, int timestamp, short deadline, byte[] senderPublicKey, long recipient,
                                             int amount, int fee, long referencedTransaction, byte[] signature) {
        return new Transaction(timestamp, deadline, senderPublicKey, recipient, amount, fee, referencedTransaction, signature, findTransactionType(type, subtype));
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

        boolean validateAttachment(Transaction transaction) {
            //TODO: this check may no longer be needed here now
            return transaction.fee <= Nxt.MAX_BALANCE;
        }

        public static abstract class Payment extends Type {

            @Override
            public final byte getType() {
                return TYPE_PAYMENT;
            }

            @Override
            protected final void loadAttachment(Transaction transaction, ByteBuffer buffer) {}

            @Override
            protected final void loadAttachment(Transaction transaction, JSONObject attachmentData) {}

            public static final Type ORDINARY = new Payment() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_PAYMENT_ORDINARY_PAYMENT;
                }

                @Override
                boolean validateAttachment(Transaction transaction) {
                    return super.validateAttachment(transaction) &&  transaction.amount > 0 && transaction.amount < Nxt.MAX_BALANCE;
                }
            };
        }

        public static abstract class Messaging extends Type {

            @Override
            public final byte getType() {
                return TYPE_MESSAGING;
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
                boolean validateAttachment(Transaction transaction) {
                    if (!super.validateAttachment(transaction)) {
                        return false;
                    }
                    if (Nxt.lastBlock.get().height < Nxt.ARBITRARY_MESSAGES_BLOCK) {
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
                boolean validateAttachment(Transaction transaction) {
                    if (!super.validateAttachment(transaction)) {
                        return false;
                    }
                    if (Nxt.lastBlock.get().height < Nxt.ALIAS_SYSTEM_BLOCK) {
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
                boolean validateAttachment(Transaction transaction) {
                    if (!super.validateAttachment(transaction)) {
                        return false;
                    }
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
                boolean validateAttachment(Transaction transaction) {
                    if (!super.validateAttachment(transaction)) {
                        return false;
                    }
                    Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.attachment;
                    return transaction.amount == 0 && attachment.quantity > 0 && attachment.quantity <= Nxt.MAX_ASSET_QUANTITY;
                }
            };

            public static final Type ASK_ORDER_PLACEMENT = new ColoredCoins() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT;
                }

                @Override
                protected void loadAttachment(Transaction transaction, ByteBuffer buffer) {
                    long asset = buffer.getLong();
                    int quantity = buffer.getInt();
                    long price = buffer.getLong();
                    transaction.attachment = new Attachment.ColoredCoinsAskOrderPlacement(asset, quantity, price);
                }

                @Override
                protected void loadAttachment(Transaction transaction, JSONObject attachmentData) {
                    long asset = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
                    int quantity = ((Long)attachmentData.get("quantity")).intValue();
                    long price = (Long)attachmentData.get("price");
                    transaction.attachment = new Attachment.ColoredCoinsAskOrderPlacement(asset, quantity, price);
                }

                @Override
                boolean validateAttachment(Transaction transaction) {
                    if (!super.validateAttachment(transaction)) {
                        return false;
                    }
                    Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement)transaction.attachment;
                    return transaction.recipient == Genesis.CREATOR_ID && transaction.amount == 0
                            && attachment.quantity > 0 && attachment.quantity <= Nxt.MAX_ASSET_QUANTITY
                            && attachment.price > 0 && attachment.price <= Nxt.MAX_BALANCE * 100L;
                }
            };

            public final static Type BID_ORDER_PLACEMENT = new ColoredCoins() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT;
                }

                @Override
                protected void loadAttachment(Transaction transaction, ByteBuffer buffer) {
                    long asset = buffer.getLong();
                    int quantity = buffer.getInt();
                    long price = buffer.getLong();
                    transaction.attachment = new Attachment.ColoredCoinsBidOrderPlacement(asset, quantity, price);
                }

                @Override
                protected void loadAttachment(Transaction transaction, JSONObject attachmentData) {
                    long asset = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
                    int quantity = ((Long)attachmentData.get("quantity")).intValue();
                    long price = (Long)attachmentData.get("price");
                    transaction.attachment = new Attachment.ColoredCoinsBidOrderPlacement(asset, quantity, price);
                }

                @Override
                boolean validateAttachment(Transaction transaction) {
                    if (!super.validateAttachment(transaction)) {
                        return false;
                    }
                    Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement)transaction.attachment;
                    return transaction.recipient == Genesis.CREATOR_ID && transaction.amount == 0
                            && attachment.quantity > 0 && attachment.quantity <= Nxt.MAX_ASSET_QUANTITY
                            && attachment.price > 0 && attachment.price <= Nxt.MAX_BALANCE * 100L;
                }
            };

            public static final Type ASK_ORDER_CANCELLATION = new ColoredCoins() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION;
                }

                @Override
                protected void loadAttachment(Transaction transaction, ByteBuffer buffer) {
                    long order = buffer.getLong();
                    transaction.attachment = new Attachment.ColoredCoinsAskOrderCancellation(order);
                }

                @Override
                protected void loadAttachment(Transaction transaction, JSONObject attachmentData) {
                    transaction.attachment = new Attachment.ColoredCoinsAskOrderCancellation(Convert.parseUnsignedLong((String) attachmentData.get("order")));
                }

                @Override
                boolean validateAttachment(Transaction transaction) {
                    return super.validateAttachment(transaction) && transaction.recipient == Genesis.CREATOR_ID && transaction.amount == 0;
                }
            };

            public static final Type BID_ORDER_CANCELLATION = new ColoredCoins() {

                @Override
                public final byte getSubtype() {
                    return SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION;
                }

                @Override
                protected void loadAttachment(Transaction transaction, ByteBuffer buffer) {
                    long order = buffer.getLong();
                    transaction.attachment = new Attachment.ColoredCoinsBidOrderCancellation(order);
                }

                @Override
                protected void loadAttachment(Transaction transaction, JSONObject attachmentData) {
                    transaction.attachment = new Attachment.ColoredCoinsBidOrderCancellation(Convert.parseUnsignedLong((String) attachmentData.get("order")));
                }

                @Override
                boolean validateAttachment(Transaction transaction) {
                    return super.validateAttachment(transaction) && transaction.recipient == Genesis.CREATOR_ID && transaction.amount == 0;
                }
            };
        }
    }
}
