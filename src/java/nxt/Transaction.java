package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
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
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

class Transaction implements Comparable<Transaction>, Serializable {

    static final long serialVersionUID = 0;

    static final byte TYPE_PAYMENT = 0;
    static final byte TYPE_MESSAGING = 1;
    static final byte TYPE_COLORED_COINS = 2;

    static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;

    static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
    static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;

    static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
    static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
    static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
    static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
    static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
    static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;

    static final int ASSET_ISSUANCE_FEE = 1000;

    final byte type, subtype;
    int timestamp;
    final short deadline;
    final byte[] senderPublicKey;
    final long recipient;
    final int amount, fee;
    final long referencedTransaction;
    byte[] signature;
    Attachment attachment;

    int index;
    long block;
    int height;

    Transaction(byte type, byte subtype, int timestamp, short deadline, byte[] senderPublicKey, long recipient, int amount, int fee, long referencedTransaction, byte[] signature) {

        this.type = type;
        this.subtype = subtype;
        this.timestamp = timestamp;
        this.deadline = deadline;
        this.senderPublicKey = senderPublicKey;
        this.recipient = recipient;
        this.amount = amount;
        this.fee = fee;
        this.referencedTransaction = referencedTransaction;
        this.signature = signature;

        height = Integer.MAX_VALUE;

    }

    @Override
    public int compareTo(Transaction o) {

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

    public static final Comparator<Transaction> timestampComparator = new Comparator<Transaction>() {
        @Override
        public int compare(Transaction o1, Transaction o2) {
            return o1.timestamp < o2.timestamp ? -1 : (o1.timestamp > o2.timestamp ? 1 : 0);
        }
    };

    private static final int TRANSACTION_BYTES_LENGTH = 1 + 1 + 4 + 2 + 32 + 8 + 4 + 4 + 8 + 64;

    int getSize() {
        return TRANSACTION_BYTES_LENGTH + (attachment == null ? 0 : attachment.getSize());
    }

    byte[] getBytes() {

        ByteBuffer buffer = ByteBuffer.allocate(getSize());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(type);
        buffer.put(subtype);
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

    transient volatile long id;
    transient volatile String stringId = null;
    transient volatile long senderAccountId;

    long getId() {
        calculateIds();
        return id;
    }


    String getStringId() {
        calculateIds();
        return stringId;
    }

    long getSenderAccountId() {
        calculateIds();
        return senderAccountId;
    }

    private void calculateIds() {
        if (stringId != null) {
            return;
        }
        byte[] hash = Crypto.getMessageDigest("SHA-256").digest(getBytes());
        BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
        id = bigInteger.longValue();
        stringId = bigInteger.toString();
        senderAccountId = Account.getId(senderPublicKey);
    }


    JSONObject getJSONObject() {

        JSONObject transaction = new JSONObject();

        transaction.put("type", type);
        transaction.put("subtype", subtype);
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

    long getRecipientDeltaBalance() {

        return amount * 100L + (attachment == null ? 0 : attachment.getRecipientDeltaBalance());

    }

    long getSenderDeltaBalance() {

        return -(amount + fee) * 100L + (attachment == null ? 0 : attachment.getSenderDeltaBalance());

    }

    static Transaction getTransaction(ByteBuffer buffer) {

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

        Transaction transaction = new Transaction(type, subtype, timestamp, deadline, senderPublicKey, recipient, amount, fee, referencedTransaction, signature);

        switch (type) {

            case Transaction.TYPE_MESSAGING:
            {

                switch (subtype) {

                    case Transaction.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE:
                    {

                        int messageLength = buffer.getInt();
                        if (messageLength <= Nxt.MAX_ARBITRARY_MESSAGE_LENGTH) {

                            byte[] message = new byte[messageLength];
                            buffer.get(message);

                            transaction.attachment = new MessagingArbitraryMessageAttachment(message);

                        }

                    }
                    break;

                    case Transaction.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
                    {

                        int aliasLength = buffer.get();
                        byte[] alias = new byte[aliasLength];
                        buffer.get(alias);
                        int uriLength = buffer.getShort();
                        byte[] uri = new byte[uriLength];
                        buffer.get(uri);

                        try {

                            transaction.attachment = new MessagingAliasAssignmentAttachment(new String(alias, "UTF-8").intern(), new String(uri, "UTF-8").intern());

                        } catch (RuntimeException|UnsupportedEncodingException e) {
                            Logger.logDebugMessage("Error parsing alias assignment", e);
                        }

                    }
                    break;

                }

            }
            break;

            case Transaction.TYPE_COLORED_COINS:
            {

                switch (subtype) {

                    case Transaction.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE:
                    {

                        int nameLength = buffer.get();
                        byte[] name = new byte[nameLength];
                        buffer.get(name);
                        int descriptionLength = buffer.getShort();
                        byte[] description = new byte[descriptionLength];
                        buffer.get(description);
                        int quantity = buffer.getInt();

                        try {

                            transaction.attachment = new ColoredCoinsAssetIssuanceAttachment(new String(name, "UTF-8").intern(), new String(description, "UTF-8").intern(), quantity);

                        } catch (RuntimeException|UnsupportedEncodingException e) {
                            Logger.logDebugMessage("Error in asset issuance", e);
                        }

                    }
                    break;

                    case Transaction.SUBTYPE_COLORED_COINS_ASSET_TRANSFER:
                    {

                        long asset = buffer.getLong();
                        int quantity = buffer.getInt();

                        transaction.attachment = new ColoredCoinsAssetTransferAttachment(asset, quantity);

                    }
                    break;

                    case Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
                    {

                        long asset = buffer.getLong();
                        int quantity = buffer.getInt();
                        long price = buffer.getLong();

                        transaction.attachment = new ColoredCoinsAskOrderPlacementAttachment(asset, quantity, price);

                    }
                    break;

                    case Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT:
                    {

                        long asset = buffer.getLong();
                        int quantity = buffer.getInt();
                        long price = buffer.getLong();

                        transaction.attachment = new ColoredCoinsBidOrderPlacementAttachment(asset, quantity, price);

                    }
                    break;

                    case Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
                    {

                        long order = buffer.getLong();

                        transaction.attachment = new ColoredCoinsAskOrderCancellationAttachment(order);

                    }
                    break;

                    case Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
                    {

                        long order = buffer.getLong();

                        transaction.attachment = new ColoredCoinsBidOrderCancellationAttachment(order);

                    }
                    break;

                }

            }
            break;

        }

        return transaction;

    }

    static Transaction getTransaction(JSONObject transactionData) {

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

        Transaction transaction = new Transaction(type, subtype, timestamp, deadline, senderPublicKey, recipient, amount, fee, referencedTransaction, signature);

        JSONObject attachmentData = (JSONObject)transactionData.get("attachment");
        switch (type) {

            case TYPE_MESSAGING:
            {

                switch (subtype) {

                    case SUBTYPE_MESSAGING_ARBITRARY_MESSAGE:
                    {

                        String message = (String)attachmentData.get("message");
                        transaction.attachment = new MessagingArbitraryMessageAttachment(Convert.convert(message));

                    }
                    break;

                    case SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
                    {

                        String alias = (String)attachmentData.get("alias");
                        String uri = (String)attachmentData.get("uri");
                        transaction.attachment = new MessagingAliasAssignmentAttachment(alias.trim(), uri.trim());

                    }
                    break;

                }

            }
            break;

            case TYPE_COLORED_COINS:
            {

                switch (subtype) {

                    case SUBTYPE_COLORED_COINS_ASSET_ISSUANCE:
                    {

                        String name = (String)attachmentData.get("name");
                        String description = (String)attachmentData.get("description");
                        int quantity = ((Long)attachmentData.get("quantity")).intValue();
                        transaction.attachment = new ColoredCoinsAssetIssuanceAttachment(name.trim(), description.trim(), quantity);

                    }
                    break;

                    case SUBTYPE_COLORED_COINS_ASSET_TRANSFER:
                    {

                        long asset = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
                        int quantity = ((Long)attachmentData.get("quantity")).intValue();
                        transaction.attachment = new ColoredCoinsAssetTransferAttachment(asset, quantity);

                    }
                    break;

                    case SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
                    {

                        long asset = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
                        int quantity = ((Long)attachmentData.get("quantity")).intValue();
                        long price = (Long)attachmentData.get("price");
                        transaction.attachment = new ColoredCoinsAskOrderPlacementAttachment(asset, quantity, price);

                    }
                    break;

                    case SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT:
                    {

                        long asset = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
                        int quantity = ((Long)attachmentData.get("quantity")).intValue();
                        long price = (Long)attachmentData.get("price");
                        transaction.attachment = new ColoredCoinsBidOrderPlacementAttachment(asset, quantity, price);

                    }
                    break;

                    case SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
                    {

                        transaction.attachment = new ColoredCoinsAskOrderCancellationAttachment(Convert.parseUnsignedLong((String) attachmentData.get("order")));

                    }
                    break;

                    case SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
                    {

                        transaction.attachment = new ColoredCoinsBidOrderCancellationAttachment(Convert.parseUnsignedLong((String) attachmentData.get("order")));

                    }
                    break;

                }

            }
            break;

        }

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

    static void processTransactions(JSONObject request, String parameterName) {

        JSONArray transactionsData = (JSONArray)request.get(parameterName);
        JSONArray validTransactionsData = new JSONArray();

        for (Object transactionData : transactionsData) {

            Transaction transaction = Transaction.getTransaction((JSONObject) transactionData);

            try {

                int curTime = Nxt.getEpochTime(System.currentTimeMillis());
                if (transaction.timestamp > curTime + 15 || transaction.deadline < 1 || transaction.timestamp + transaction.deadline * 60 < curTime || transaction.fee <= 0 || !transaction.validateAttachment()) {

                    continue;

                }

                long senderId;
                boolean doubleSpendingTransaction;

                synchronized (Nxt.blocksAndTransactionsLock) {

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

                                        ColoredCoinsAssetTransferAttachment attachment = (ColoredCoinsAssetTransferAttachment)transaction.attachment;
                                        Integer unconfirmedAssetBalance = account.getUnconfirmedAssetBalance(attachment.asset);
                                        if (unconfirmedAssetBalance == null || unconfirmedAssetBalance < attachment.quantity) {

                                            doubleSpendingTransaction = true;

                                            account.addToUnconfirmedBalance(amount * 100L);

                                        } else {

                                            account.addToUnconfirmedAssetBalance(attachment.asset, -attachment.quantity);

                                        }

                                    } else if (transaction.subtype == Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT) {

                                        ColoredCoinsAskOrderPlacementAttachment attachment = (ColoredCoinsAskOrderPlacementAttachment)transaction.attachment;
                                        Integer unconfirmedAssetBalance = account.getUnconfirmedAssetBalance(attachment.asset);
                                        if (unconfirmedAssetBalance == null || unconfirmedAssetBalance < attachment.quantity) {

                                            doubleSpendingTransaction = true;

                                            account.addToUnconfirmedBalance(amount * 100L);

                                        } else {

                                            account.addToUnconfirmedAssetBalance(attachment.asset, -attachment.quantity);

                                        }

                                    } else if (transaction.subtype == Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT) {

                                        ColoredCoinsBidOrderPlacementAttachment attachment = (ColoredCoinsBidOrderPlacementAttachment)transaction.attachment;
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

    void sign(String secretPhrase) {

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

    boolean validateAttachment() {
        //TODO: this check may no longer be needed here now
        if (fee > Nxt.MAX_BALANCE) {

            return false;

        }
        //TODO: refactor switch statements
        switch (type) {

            case TYPE_PAYMENT:
            {

                switch (subtype) {

                    case SUBTYPE_PAYMENT_ORDINARY_PAYMENT:
                    {

                        return amount > 0 && amount < Nxt.MAX_BALANCE;

                    }

                    default:
                    {

                        return false;

                    }

                }

            }

            case TYPE_MESSAGING:
            {

                switch (subtype) {

                    case SUBTYPE_MESSAGING_ARBITRARY_MESSAGE:
                    {

                        if (Nxt.lastBlock.get().height < Nxt.ARBITRARY_MESSAGES_BLOCK) {

                            return false;

                        }

                        try {

                            MessagingArbitraryMessageAttachment attachment = (MessagingArbitraryMessageAttachment)this.attachment;
                            return amount == 0 && attachment.message.length <= Nxt.MAX_ARBITRARY_MESSAGE_LENGTH;

                        } catch (RuntimeException e) {

                            Logger.logDebugMessage("Error validating arbitrary message", e);
                            return false;

                        }

                    }

                    case SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
                    {

                        if (Nxt.lastBlock.get().height < Nxt.ALIAS_SYSTEM_BLOCK) {

                            return false;

                        }

                        try {

                            MessagingAliasAssignmentAttachment attachment = (MessagingAliasAssignmentAttachment)this.attachment;
                            if (recipient != Nxt.CREATOR_ID || amount != 0 || attachment.alias.length() == 0 || attachment.alias.length() > 100 || attachment.uri.length() > 1000) {

                                return false;

                            } else {

                                String normalizedAlias = attachment.alias.toLowerCase();
                                for (int i = 0; i < normalizedAlias.length(); i++) {

                                    if (Convert.alphabet.indexOf(normalizedAlias.charAt(i)) < 0) {

                                        return false;

                                    }

                                }

                                Alias alias = Nxt.aliases.get(normalizedAlias);

                                return alias == null || Arrays.equals(alias.account.publicKey.get(), senderPublicKey);

                            }

                        } catch (RuntimeException e) {

                            Logger.logDebugMessage("Error in alias assignment validation", e);
                            return false;

                        }

                    }

                    default:
                    {

                        return false;

                    }

                }

            }

            //TODO: uncomment, review and clean up the code, comment out again

        case TYPE_COLORED_COINS:
            {

                switch (subtype) {

                case SUBTYPE_COLORED_COINS_ASSET_ISSUANCE:
                    {

                        try {

                            ColoredCoinsAssetIssuanceAttachment attachment = (ColoredCoinsAssetIssuanceAttachment)this.attachment;
                            if (recipient != Nxt.CREATOR_ID || amount != 0 || fee < ASSET_ISSUANCE_FEE || attachment.name.length() < 3 || attachment.name.length() > 10 || attachment.description.length() > 1000 || attachment.quantity <= 0 || attachment.quantity > Nxt.MAX_ASSET_QUANTITY) {

                                return false;

                            } else {

                                String normalizedName = attachment.name.toLowerCase();
                                for (int i = 0; i < normalizedName.length(); i++) {

                                    if (Convert.alphabet.indexOf(normalizedName.charAt(i)) < 0) {

                                        return false;

                                    }

                                }
                                if (Nxt.assetNameToIdMappings.get(normalizedName) != null) {

                                    return false;

                                }

                                return true;

                            }

                        } catch (RuntimeException e) {

                            Logger.logDebugMessage("Error validating colored coins asset issuance", e);
                            return false;

                        }

                    }

                case SUBTYPE_COLORED_COINS_ASSET_TRANSFER:
                    {

                        ColoredCoinsAssetTransferAttachment attachment = (ColoredCoinsAssetTransferAttachment)this.attachment;
                        if (amount != 0 || attachment.quantity <= 0 || attachment.quantity > Nxt.MAX_ASSET_QUANTITY) {

                            return false;

                        } else {

                            return true;

                        }

                    }

                case SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
                    {

                        ColoredCoinsAskOrderPlacementAttachment attachment = (ColoredCoinsAskOrderPlacementAttachment)this.attachment;
                        if (recipient != Nxt.CREATOR_ID || amount != 0 || attachment.quantity <= 0 || attachment.quantity > Nxt.MAX_ASSET_QUANTITY || attachment.price <= 0 || attachment.price > Nxt.MAX_BALANCE * 100L) {

                            return false;

                        } else {

                            return true;

                        }

                    }

                case SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT:
                    {

                        ColoredCoinsBidOrderPlacementAttachment attachment = (ColoredCoinsBidOrderPlacementAttachment)this.attachment;
                        if (recipient != Nxt.CREATOR_ID || amount != 0 || attachment.quantity <= 0 || attachment.quantity > Nxt.MAX_ASSET_QUANTITY || attachment.price <= 0 || attachment.price > Nxt.MAX_BALANCE * 100L) {

                            return false;

                        } else {

                            return true;

                        }

                    }

                case SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
                    {

                        if (recipient != Nxt.CREATOR_ID || amount != 0) {

                            return false;

                        } else {

                            return true;

                        }

                    }

                case SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
                    {

                        if (recipient != Nxt.CREATOR_ID || amount != 0) {

                            return false;

                        } else {

                            return true;

                        }

                    }

                default:
                    {

                        return false;

                    }

                }

            }

            // TODO: comment ends here

            default:
            {

                return false;

            }

        }

    }

    boolean verify() {

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

    public static byte[] calculateTransactionsChecksum() {
        synchronized (Nxt.blocksAndTransactionsLock) {
            PriorityQueue<Transaction> sortedTransactions = new PriorityQueue<>(Nxt.transactions.size(), new Comparator<Transaction>() {
                @Override
                public int compare(Transaction o1, Transaction o2) {
                    long id1 = o1.getId();
                    long id2 = o2.getId();
                    return id1 < id2 ? -1 : (id1 > id2 ? 1 : (o1.timestamp < o2.timestamp ? -1 : (o1.timestamp > o2.timestamp ? 1 : 0)));
                }
            });
            sortedTransactions.addAll(Nxt.transactions.values());
            MessageDigest digest = Crypto.getMessageDigest("SHA-256");
            while (! sortedTransactions.isEmpty()) {
                digest.update(sortedTransactions.poll().getBytes());
            }
            return digest.digest();
        }
    }

    static interface Attachment {

        int getSize();
        byte[] getBytes();
        JSONObject getJSONObject();

        long getRecipientDeltaBalance();
        long getSenderDeltaBalance();

    }

    static class MessagingArbitraryMessageAttachment implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        final byte[] message;

        MessagingArbitraryMessageAttachment(byte[] message) {

            this.message = message;

        }

        @Override
        public int getSize() {
            return 4 + message.length;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(message.length);
            buffer.put(message);

            return buffer.array();

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("message", Convert.convert(message));

            return attachment;

        }

        @Override
        public long getRecipientDeltaBalance() {

            return 0;

        }

        @Override
        public long getSenderDeltaBalance() {

            return 0;

        }

    }

    static class MessagingAliasAssignmentAttachment implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        final String alias;
        final String uri;

        MessagingAliasAssignmentAttachment(String alias, String uri) {

            this.alias = alias;
            this.uri = uri;

        }

        @Override
        public int getSize() {
            try {
                return 1 + alias.getBytes("UTF-8").length + 2 + uri.getBytes("UTF-8").length;
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {

            try {

                byte[] alias = this.alias.getBytes("UTF-8");
                byte[] uri = this.uri.getBytes("UTF-8");

                ByteBuffer buffer = ByteBuffer.allocate(1 + alias.length + 2 + uri.length);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.put((byte)alias.length);
                buffer.put(alias);
                buffer.putShort((short)uri.length);
                buffer.put(uri);

                return buffer.array();

            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;

            }

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("alias", alias);
            attachment.put("uri", uri);

            return attachment;

        }

        @Override
        public long getRecipientDeltaBalance() {

            return 0;

        }

        @Override
        public long getSenderDeltaBalance() {

            return 0;

        }

    }

    static class ColoredCoinsAssetIssuanceAttachment implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        String name;
        String description;
        int quantity;

        ColoredCoinsAssetIssuanceAttachment(String name, String description, int quantity) {

            this.name = name;
            this.description = description == null ? "" : description;
            this.quantity = quantity;

        }

        @Override
        public int getSize() {
            try {
                return 1 + name.getBytes("UTF-8").length + 2 + description.getBytes("UTF-8").length + 4;
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {

            try {
                byte[] name = this.name.getBytes("UTF-8");
                byte[] description = this.description.getBytes("UTF-8");

                ByteBuffer buffer = ByteBuffer.allocate(1 + name.length + 2 + description.length + 4);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.put((byte)name.length);
                buffer.put(name);
                buffer.putShort((short)description.length);
                buffer.put(description);
                buffer.putInt(quantity);

                return buffer.array();
            } catch (RuntimeException|UnsupportedEncodingException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("name", name);
            attachment.put("description", description);
            attachment.put("quantity", quantity);

            return attachment;

        }

        @Override
        public long getRecipientDeltaBalance() {

            return 0;

        }

        @Override
        public long getSenderDeltaBalance() {

            return 0;

        }

    }

    static class ColoredCoinsAssetTransferAttachment implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        long asset;
        int quantity;

        ColoredCoinsAssetTransferAttachment(long asset, int quantity) {

            this.asset = asset;
            this.quantity = quantity;

        }

        @Override
        public int getSize() {
            return 8 + 4;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(asset);
            buffer.putInt(quantity);

            return buffer.array();

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("asset", Convert.convert(asset));
            attachment.put("quantity", quantity);

            return attachment;

        }

        @Override
        public long getRecipientDeltaBalance() {

            return 0;

        }

        @Override
        public long getSenderDeltaBalance() {

            return 0;

        }

    }

    static class ColoredCoinsAskOrderPlacementAttachment implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        long asset;
        int quantity;
        long price;

        ColoredCoinsAskOrderPlacementAttachment(long asset, int quantity, long price) {

            this.asset = asset;
            this.quantity = quantity;
            this.price = price;

        }

        @Override
        public int getSize() {
            return 8 + 4 + 8;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(asset);
            buffer.putInt(quantity);
            buffer.putLong(price);

            return buffer.array();

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("asset", Convert.convert(asset));
            attachment.put("quantity", quantity);
            attachment.put("price", price);

            return attachment;

        }

        @Override
        public long getRecipientDeltaBalance() {

            return 0;

        }

        @Override
        public long getSenderDeltaBalance() {

            return 0;

        }

    }

    static class ColoredCoinsBidOrderPlacementAttachment implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        long asset;
        int quantity;
        long price;

        ColoredCoinsBidOrderPlacementAttachment(long asset, int quantity, long price) {

            this.asset = asset;
            this.quantity = quantity;
            this.price = price;

        }

        @Override
        public int getSize() {
            return 8 + 4 + 8;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(asset);
            buffer.putInt(quantity);
            buffer.putLong(price);

            return buffer.array();

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("asset", Convert.convert(asset));
            attachment.put("quantity", quantity);
            attachment.put("price", price);

            return attachment;

        }

        @Override
        public long getRecipientDeltaBalance() {

            return 0;

        }

        @Override
        public long getSenderDeltaBalance() {

            return -quantity * price;

        }

    }

    static class ColoredCoinsAskOrderCancellationAttachment implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        long order;

        ColoredCoinsAskOrderCancellationAttachment(long order) {

            this.order = order;

        }

        @Override
        public int getSize() {
            return 8;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(order);

            return buffer.array();

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("order", Convert.convert(order));

            return attachment;

        }

        @Override
        public long getRecipientDeltaBalance() {

            return 0;

        }

        @Override
        public long getSenderDeltaBalance() {

            return 0;

        }

    }

    static class ColoredCoinsBidOrderCancellationAttachment implements Attachment, Serializable {

        static final long serialVersionUID = 0;

        long order;

        ColoredCoinsBidOrderCancellationAttachment(long order) {

            this.order = order;

        }

        @Override
        public int getSize() {
            return 8;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(order);

            return buffer.array();

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("order", Convert.convert(order));

            return attachment;

        }

        @Override
        public long getRecipientDeltaBalance() {

            return 0;

        }

        @Override
        public long getSenderDeltaBalance() {

            BidOrder bidOrder = Nxt.bidOrders.get(order);
            if (bidOrder == null) {

                return 0;

            }

            return bidOrder.quantity * bidOrder.price;

        }

    }

}
