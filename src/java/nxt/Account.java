package nxt;

import nxt.crypto.Crypto;
import nxt.util.Listener;
import nxt.util.Listeners;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public final class Account {

    public static enum Event {
        BALANCE, UNCONFIRMED_BALANCE
    }

    private static final int maxTrackedBalanceConfirmations = 2881;
    private static final ConcurrentMap<Long, Account> accounts = new ConcurrentHashMap<>();

    private static final Collection<Account> allAccounts = Collections.unmodifiableCollection(accounts.values());

    private static final Listeners<Account,Event> listeners = new Listeners<>();

    public static boolean addListener(Listener<Account> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Account> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static Collection<Account> getAllAccounts() {
        return allAccounts;
    }

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

    private final Long id;
    private final int height;
    private final AtomicReference<byte[]> publicKey = new AtomicReference<>();
    private long balance;
    private long unconfirmedBalance;
    private final List<GuaranteedBalance> guaranteedBalances = new ArrayList<>();

    private final Map<Long, Integer> assetBalances = new HashMap<>();
    private final Map<Long, Integer> unconfirmedAssetBalances = new HashMap<>();

    private Account(Long id) {
        this.id = id;
        this.height = Blockchain.getLastBlock().getHeight();
    }

    public Long getId() {
        return id;
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
            for (Transaction transaction : lastBlock.blockTransactions) {
                if (transaction.getRecipientId().equals(id)) {
                    receivedInlastBlock += transaction.getAmount();
                }
            }
            return (int)(getBalance() / 100) - receivedInlastBlock;

        } else {
            return (int)(getGuaranteedBalance(1440) / 100);
        }

    }

    public synchronized long getGuaranteedBalance(final int numberOfConfirmations) {
        if (numberOfConfirmations > maxTrackedBalanceConfirmations || numberOfConfirmations >= Blockchain.getLastBlock().getHeight() || numberOfConfirmations < 0) {
            throw new IllegalArgumentException("Number of required confirmations must be between 0 and " + maxTrackedBalanceConfirmations);
        }
        if (guaranteedBalances.isEmpty()) {
            return 0;
        }
        int i = Collections.binarySearch(guaranteedBalances, new GuaranteedBalance(Blockchain.getLastBlock().getHeight() - numberOfConfirmations, 0));
        if (i == -1) {
            return 0;
        }
        if (i < -1) {
            i = -i - 2;
        }
        if (i > guaranteedBalances.size() - 1) {
            i = guaranteedBalances.size() - 1;
        }
        GuaranteedBalance result;
        while ((result = guaranteedBalances.get(i)).ignore && i > 0) {
            i--;
        }
        return result.ignore ? 0 : result.balance;

    }

    public synchronized Integer getUnconfirmedAssetBalance(Long assetId) {
        return unconfirmedAssetBalances.get(assetId);
    }

    public Map<Long, Integer> getAssetBalances() {
        return Collections.unmodifiableMap(assetBalances);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Account && this.getId().equals(((Account)o).getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
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
            addToGuaranteedBalance(amount);
        }
        listeners.notify(this, Event.BALANCE);
    }

    void addToUnconfirmedBalance(long amount) {
        synchronized (this) {
            this.unconfirmedBalance += amount;
        }
        listeners.notify(this, Event.UNCONFIRMED_BALANCE);
    }

    void addToBalanceAndUnconfirmedBalance(long amount) {
        synchronized (this) {
            this.balance += amount;
            this.unconfirmedBalance += amount;
            addToGuaranteedBalance(amount);
        }
        listeners.notify(this, Event.BALANCE);
        listeners.notify(this, Event.UNCONFIRMED_BALANCE);
    }

    private synchronized void addToGuaranteedBalance(long amount) {
        int blockchainHeight = Blockchain.getLastBlock().getHeight();
        GuaranteedBalance last = null;
        if (guaranteedBalances.size() > 0 && (last = guaranteedBalances.get(guaranteedBalances.size() - 1)).height > blockchainHeight) {
            // this only happens while last block is being popped off
            if (amount > 0) {
                // this is a reversal of a withdrawal or a fee, so previous gb records need to be corrected
                Iterator<GuaranteedBalance> iter = guaranteedBalances.iterator();
                while (iter.hasNext()) {
                    GuaranteedBalance gb = iter.next();
                    gb.balance += amount;
                }
            } // deposits don't need to be reversed as they have never been applied to old gb records to begin with
            last.ignore = true; // set dirty flag
            return; // block popped off, no further processing
        }
        int trimTo = 0;
        for (int i = 0; i < guaranteedBalances.size(); i++) {
            GuaranteedBalance gb = guaranteedBalances.get(i);
            if (gb.height < blockchainHeight - maxTrackedBalanceConfirmations
                    && i < guaranteedBalances.size() - 1
                    && guaranteedBalances.get(i + 1).height >= blockchainHeight - maxTrackedBalanceConfirmations) {
                trimTo = i; // trim old gb records but keep at least one at height lower than the supported maxTrackedBalanceConfirmations
                if (blockchainHeight >= Nxt.TRANSPARENT_FORGING_BLOCK_4 && blockchainHeight < Nxt.TRANSPARENT_FORGING_BLOCK_5) {
                    gb.balance += amount; // because of a bug which leads to a fork
                } else if (blockchainHeight >= Nxt.TRANSPARENT_FORGING_BLOCK_5 && amount < 0) {
                    gb.balance += amount;
                }
            } else if (amount < 0) {
                gb.balance += amount; // subtract current block withdrawals from all previous gb records
            }
            // ignore deposits when updating previous gb records
        }
        if (trimTo > 0) {
            Iterator<GuaranteedBalance> iter = guaranteedBalances.iterator();
            while (iter.hasNext() && trimTo > 0) {
                iter.next();
                iter.remove();
                trimTo--;
            }
        }
        if (guaranteedBalances.size() == 0 || last.height < blockchainHeight) {
            // this is the first transaction affecting this account in a newly added block
            guaranteedBalances.add(new GuaranteedBalance(blockchainHeight, balance));
        } else if (last.height == blockchainHeight) {
            // following transactions for same account in a newly added block
            // for the current block, guaranteedBalance (0 confirmations) must be same as balance
            last.balance = balance;
            last.ignore = false;
        } else {
            // should have been handled in the block popped off case
            throw new IllegalStateException("last guaranteed balance height exceeds blockchain height");
        }
    }

    private static class GuaranteedBalance implements Comparable<GuaranteedBalance> {

        final int height;
        long balance;
        boolean ignore;

        private GuaranteedBalance(int height, long balance) {
            this.height = height;
            this.balance = balance;
            this.ignore = false;
        }

        @Override
        public int compareTo(GuaranteedBalance o) {
            if (this.height < o.height) {
                return -1;
            } else if (this.height > o.height) {
                return 1;
            }
            return 0;
        }

        @Override
        public String toString() {
            return "height: " + height + ", guaranteed: " + balance;
        }
    }

}
