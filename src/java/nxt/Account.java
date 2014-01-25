package nxt;

import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.user.User;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Account {

    public final long id;
    private long balance;
    final int height;

    public final AtomicReference<byte[]> publicKey = new AtomicReference<>();

    private final Map<Long, Integer> assetBalances = new HashMap<>();

    private long unconfirmedBalance;
    private final Map<Long, Integer> unconfirmedAssetBalances = new HashMap<>();

    Account(long id) {

        this.id = id;
        this.height = Blockchain.getLastBlock().height;

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

    public int getEffectiveBalance() {

        Block lastBlock = Blockchain.getLastBlock();
        if (lastBlock.height < Nxt.TRANSPARENT_FORGING_BLOCK_3 && this.height < Nxt.TRANSPARENT_FORGING_BLOCK_2) {

            if (this.height == 0) {
                return (int)(getBalance() / 100);
            }
            if (lastBlock.height - this.height < 1440) {
                return 0;
            }
            int receivedInlastBlock = 0;
            for (Transaction transaction : lastBlock.blockTransactions) {
                if (transaction.recipient == id) {
                    receivedInlastBlock += transaction.amount;
                }
            }
            return (int)(getBalance() / 100) - receivedInlastBlock;

        } else {
            return (int)(getGuaranteedBalance(1440) / 100);
        }

    }

    public static long getId(byte[] publicKey) {

        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        BigInteger bigInteger = new BigInteger(1, new byte[] {publicKeyHash[7], publicKeyHash[6], publicKeyHash[5], publicKeyHash[4], publicKeyHash[3], publicKeyHash[2], publicKeyHash[1], publicKeyHash[0]});
        return bigInteger.longValue();

    }

    synchronized Integer getAssetBalance(Long assetId) {
        return assetBalances.get(assetId);
    }

    public synchronized Integer getUnconfirmedAssetBalance(Long assetId) {
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

    public synchronized long getBalance() {
        return balance;
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

    public synchronized long getUnconfirmedBalance() {
        return unconfirmedBalance;
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
