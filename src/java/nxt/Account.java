package nxt;

import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.user.User;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public final class Account {

    private static final ConcurrentMap<Long, Account> accounts = new ConcurrentHashMap<>();

    public static final Collection<Account> allAccounts = Collections.unmodifiableCollection(accounts.values());

    public static Account getAccount(Long id) {
        return accounts.get(id);
    }

    public static Account getAccount(byte[] publicKey) {
        return accounts.get(getId(publicKey));
    }

    public static Long getId(byte[] publicKey) {
        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        BigInteger bigInteger = new BigInteger(1, new byte[] {publicKeyHash[7], publicKeyHash[6], publicKeyHash[5],
                publicKeyHash[4], publicKeyHash[3], publicKeyHash[2], publicKeyHash[1], publicKeyHash[0]});
        return bigInteger.longValue();
    }

    static Account addOrGetAccount(Long id) {
        Account account = new Account(id);
        Account oldAccount = accounts.putIfAbsent(id, account);
        return oldAccount != null ? oldAccount : account;
    }

    static void clear() {
        accounts.clear();
    }

    public final Long id;

    private final int height;
    private final AtomicReference<byte[]> publicKey = new AtomicReference<>();
    private long balance;
    private long unconfirmedBalance;

    private final Map<Long, Integer> assetBalances = new HashMap<>();
    private final Map<Long, Integer> unconfirmedAssetBalances = new HashMap<>();

    private Account(Long id) {
        this.id = id;
        this.height = Blockchain.getLastBlock().getHeight();
    }

    public byte[] getPublicKey() {
        return publicKey.get();
    }

    public synchronized long getBalance() {
        return balance;
    }

    public synchronized long getUnconfirmedBalance() {
        return unconfirmedBalance;
    }

    public long getGuaranteedBalance(int numberOfConfirmations) {

        long guaranteedBalance = getBalance();
        ArrayList<Block> lastBlocks = Blockchain.getLastBlocks(numberOfConfirmations - 1);
        byte[] accountPublicKey = publicKey.get();
        for (Block block : lastBlocks) {
            if (Arrays.equals(block.generatorPublicKey, accountPublicKey)) {
                if ((guaranteedBalance -= block.totalFee * 100L) <= 0) {
                    return 0;
                }
            }

            Transaction[] blockTransactions = block.getBlockTransactions();
            for (int i = blockTransactions.length; i-- > 0; ) {
                Transaction transaction = blockTransactions[i];
                if (Arrays.equals(transaction.senderPublicKey, accountPublicKey)) {
                    long deltaBalance = transaction.getSenderDeltaBalance();
                    if (deltaBalance > 0 && (guaranteedBalance -= deltaBalance) <= 0) {
                        return 0;
                    } else if (deltaBalance < 0 && (guaranteedBalance += deltaBalance) <= 0) {
                        return 0;
                    }
                }
                if (transaction.recipient.equals(id)) {
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

    public int getEffectiveBalance() {

        Block lastBlock = Blockchain.getLastBlock();
        if (lastBlock.getHeight() < Nxt.TRANSPARENT_FORGING_BLOCK_3 && this.height < Nxt.TRANSPARENT_FORGING_BLOCK_2) {

            if (this.height == 0) {
                return (int)(getBalance() / 100);
            }
            if (lastBlock.getHeight() - this.height < 1440) {
                return 0;
            }
            int receivedInlastBlock = 0;
            for (Transaction transaction : lastBlock.getBlockTransactions()) {
                if (transaction.recipient.equals(id)) {
                    receivedInlastBlock += transaction.amount;
                }
            }
            return (int)(getBalance() / 100) - receivedInlastBlock;

        } else {
            return (int)(getGuaranteedBalance(1440) / 100);
        }

    }

    public synchronized Integer getUnconfirmedAssetBalance(Long assetId) {
        return unconfirmedAssetBalances.get(assetId);
    }

    // returns true iff:
    // this.publicKey is set to null (in which case this.publicKey also gets set to key)
    // or
    // this.publicKey is already set to an array equal to key
    boolean setOrVerify(byte[] key) {
        return this.publicKey.compareAndSet(null, key) || Arrays.equals(key, this.publicKey.get());
    }

    synchronized Integer getAssetBalance(Long assetId) {
        return assetBalances.get(assetId);
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

    void addToBalance(long amount) {
        synchronized (this) {
            this.balance += amount;
        }
        Peer.updatePeerWeights(this);
    }

    void addToUnconfirmedBalance(long amount) {
        synchronized (this) {
            this.unconfirmedBalance += amount;
        }
        User.updateUserUnconfirmedBalance(this);
    }

    void addToBalanceAndUnconfirmedBalance(long amount) {
        synchronized (this) {
            this.balance += amount;
            this.unconfirmedBalance += amount;
        }
        Peer.updatePeerWeights(this);
        User.updateUserUnconfirmedBalance(this);
    }

}
