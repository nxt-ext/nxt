package nxt;

import nxt.crypto.Crypto;
import nxt.util.Logger;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

class Account {

    final long id;
    private long balance;
    final int height;

    final AtomicReference<byte[]> publicKey = new AtomicReference<>();

    private final Map<Long, Integer> assetBalances = new HashMap<>();

    private long unconfirmedBalance;
    private final Map<Long, Integer> unconfirmedAssetBalances = new HashMap<>();

    Account(long id) {

        this.id = id;
        this.height = Nxt.lastBlock.get().height;

    }

    static Account addAccount(long id) {

        Account account = new Account(id);
        Nxt.accounts.put(id, account);

        return account;

    }

    // returns true iff:
    // this.publicKey is set to null (in which case this.publicKey also gets set to key)
    // or
    // this.publicKey is already set to an array equal to key
    boolean setOrVerify(byte[] key) {

        return this.publicKey.compareAndSet(null, key) || Arrays.equals(key, this.publicKey.get());

    }

    void generateBlock(String secretPhrase) {

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

        Block block;
        Block previousBlock = Nxt.lastBlock.get();
        if (previousBlock.height < Nxt.TRANSPARENT_FORGING_BLOCK) {

            block = new Block(1, Nxt.getEpochTime(System.currentTimeMillis()), previousBlock.getId(), newTransactions.size(), 0, 0, 0, null, Crypto.getPublicKey(secretPhrase), null, new byte[64]);

        } else {

            byte[] previousBlockHash = Crypto.getMessageDigest("SHA-256").digest(previousBlock.getBytes());
            block = new Block(2, Nxt.getEpochTime(System.currentTimeMillis()), previousBlock.getId(), newTransactions.size(), 0, 0, 0, null, Crypto.getPublicKey(secretPhrase), null, new byte[64], previousBlockHash);

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
        MessageDigest digest = Crypto.getMessageDigest("SHA-256");
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
            block.generationSignature = digest.digest(Crypto.getPublicKey(secretPhrase));

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

    int getEffectiveBalance() {

        Block lastBlock = Nxt.lastBlock.get();
        if (height < Nxt.TRANSPARENT_FORGING_BLOCK_2) {

            if (height == 0) {

                return (int)(getBalance() / 100);

            }

            if (lastBlock.height - height < 1440) {

                return 0;

            }

            int amount = 0;
            for (Transaction transaction : lastBlock.blockTransactions) {

                if (transaction.recipient == id) {

                    amount += transaction.amount;

                }

            }

            return (int)(getBalance() / 100) - amount;

        } else {

            return (int)(getGuaranteedBalance(1440) / 100);

        }

    }

    static long getId(byte[] publicKey) {

        byte[] publicKeyHash = Crypto.getMessageDigest("SHA-256").digest(publicKey);
        BigInteger bigInteger = new BigInteger(1, new byte[] {publicKeyHash[7], publicKeyHash[6], publicKeyHash[5], publicKeyHash[4], publicKeyHash[3], publicKeyHash[2], publicKeyHash[1], publicKeyHash[0]});
        return bigInteger.longValue();

    }

    synchronized Integer getAssetBalance(Long assetId) {
        return assetBalances.get(assetId);
    }

    synchronized Integer getUnconfirmedAssetBalance(Long assetId) {
        return unconfirmedAssetBalances.get(assetId);
    }

    synchronized void addToAssetBalance(Long assetId, int quantity) {

        Integer assetBalance = assetBalances.get(assetId);
        if (assetBalance == null) {
            assetBalances.put(assetId, quantity);
        } else {
            assetBalances.put(assetId, assetBalance + quantity);
        }

    }

    synchronized void addToUnconfirmedAssetBalance(Long assetId, int quantity) {

        Integer unconfirmedAssetBalance = unconfirmedAssetBalances.get(assetId);
        if (unconfirmedAssetBalance == null) {
            unconfirmedAssetBalances.put(assetId, quantity);
        } else {
            unconfirmedAssetBalances.put(assetId, unconfirmedAssetBalance + quantity);
        }

    }

    synchronized void addToAssetAndUnconfirmedAssetBalance(Long assetId, int quantity) {

        Integer assetBalance = assetBalances.get(assetId);
        if (assetBalance == null) {
            assetBalances.put(assetId, quantity);
            unconfirmedAssetBalances.put(assetId, quantity);
        } else {
            assetBalances.put(assetId, assetBalance + quantity);
            unconfirmedAssetBalances.put(assetId, unconfirmedAssetBalances.get(assetId) + quantity);
        }

    }

    synchronized long getBalance() {
        return balance;
    }

    long getGuaranteedBalance(int numberOfConfirmations) {

        long guaranteedBalance = getBalance();
        ArrayList<Block> lastBlocks = Blockchain.getLastBlocks(numberOfConfirmations - 1);
        byte[] accountPublicKey = publicKey.get();
        for (Block block : lastBlocks) {

            if (Arrays.equals(block.generatorPublicKey, accountPublicKey)) {

                if ((guaranteedBalance -= block.totalFee * 100L) <= 0) {

                    return 0;

                }

            }

            for (int i = block.blockTransactions.length; i-- > 0; ) {

                Transaction transaction = block.blockTransactions[i];
                if (Arrays.equals(transaction.senderPublicKey, accountPublicKey)) {

                    long deltaBalance = transaction.getSenderDeltaBalance();
                    if (deltaBalance > 0 && (guaranteedBalance -= deltaBalance) <= 0) {

                        return 0;

                    } else if (deltaBalance < 0 && (guaranteedBalance += deltaBalance) <= 0) {

                        return 0;

                    }

                }
                if (transaction.recipient == id) {

                    long deltaBalance = transaction.getRecipientDeltaBalance();
                    if (deltaBalance > 0 && (guaranteedBalance -= deltaBalance) <= 0) {

                        return 0;

                    } else if (deltaBalance < 0 && (guaranteedBalance += deltaBalance) <= 0) {

                        return 0;

                    }

                }

            }

        }

        return guaranteedBalance;

    }

    synchronized long getUnconfirmedBalance() {
        return unconfirmedBalance;
    }

    void addToBalance(long amount) {

        synchronized (this) {

            this.balance += amount;

        }

        updatePeerWeights();

    }

    void addToUnconfirmedBalance(long amount) {

        synchronized (this) {

            this.unconfirmedBalance += amount;

        }

        updateUserUnconfirmedBalance();

    }

    void addToBalanceAndUnconfirmedBalance(long amount) {

        synchronized (this) {

            this.balance += amount;
            this.unconfirmedBalance += amount;

        }

        updatePeerWeights();
        updateUserUnconfirmedBalance();

    }

    private void updatePeerWeights() {

        for (Peer peer : Nxt.peers.values()) {

            if (peer.accountId == id && peer.adjustedWeight > 0) {

                peer.updateWeight();

            }

        }

    }

    private void updateUserUnconfirmedBalance() {

        JSONObject response = new JSONObject();
        response.put("response", "setBalance");
        response.put("balance", getUnconfirmedBalance());
        byte[] accountPublicKey = publicKey.get();
        for (User user : Nxt.users.values()) {

            if (user.secretPhrase != null && Arrays.equals(user.publicKey, accountPublicKey)) {

                user.send(response);

            }

        }

    }

}
