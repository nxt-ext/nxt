package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Account {

    public static enum Event {
        BALANCE, UNCONFIRMED_BALANCE, ASSET_BALANCE, UNCONFIRMED_ASSET_BALANCE
    }

    public static class AccountAsset {

        public final Long accountId;
        public final Long assetId;
        public final Integer quantity;

        private AccountAsset(Long accountId, Long assetId, Integer quantity) {
            this.accountId = accountId;
            this.assetId = assetId;
            this.quantity = quantity;
        }

    }

    private static final int maxTrackedBalanceConfirmations = 2881;
    private static final ConcurrentMap<Long, Account> accounts = new ConcurrentHashMap<>();

    private static final Collection<Account> allAccounts = Collections.unmodifiableCollection(accounts.values());

    private static final Listeners<Account,Event> listeners = new Listeners<>();

    private static final Listeners<AccountAsset,Event> assetListeners = new Listeners<>();

    public static boolean addListener(Listener<Account> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Account> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static boolean addAssetListener(Listener<AccountAsset> listener, Event eventType) {
        return assetListeners.addListener(listener, eventType);
    }

    public static boolean removeAssetListener(Listener<AccountAsset> listener, Event eventType) {
        return assetListeners.removeListener(listener, eventType);
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
    private byte[] publicKey;
    private int keyHeight;
    private long balance, lockedBalance;
    private long unconfirmedBalance;
    private final List<GuaranteedBalance> guaranteedBalances = new ArrayList<>();

    private int curEffectiveBalanceLeasingHeightFrom, curEffectiveBalanceLeasingHeightTo;
    private Long curLesseeId;
    private int nextEffectiveBalanceLeasingHeightFrom, nextEffectiveBalanceLeasingHeightTo;
    private Long nextLesseeId;
    private List<Long> effectiveBalanceLeasers = new LinkedList<>();

    private final Map<Long, Integer> assetBalances = new HashMap<>();
    private final Map<Long, Integer> unconfirmedAssetBalances = new HashMap<>();

    private Account(Long id) {
        this.id = id;
        this.height = Nxt.getBlockchain().getLastBlock().getHeight();

        curEffectiveBalanceLeasingHeightFrom = Integer.MAX_VALUE;
    }

    public Long getId() {
        return id;
    }

    public synchronized byte[] getPublicKey() {
        return publicKey;
    }

    public synchronized long getBalance() {
        return balance;
    }

    public synchronized long getUnconfirmedBalance() {
        return unconfirmedBalance;
    }

    public int getEffectiveBalance() {

        Block lastBlock = Nxt.getBlockchain().getLastBlock();

        if (lastBlock.getHeight() >= Constants.TRANSPARENT_FORGING_BLOCK_6 && (publicKey == null || lastBlock.getHeight() - keyHeight <= 1440)) {
            return 0; // cfb: Accounts with the public key revealed less than 1440 blocks ago are not allowed to generate blocks
        }

        if (lastBlock.getHeight() < Constants.TRANSPARENT_FORGING_BLOCK_3 && this.height < Constants.TRANSPARENT_FORGING_BLOCK_2) {

            if (this.height == 0) {
                return (int)(getBalance() / 100);
            }
            if (lastBlock.getHeight() - this.height < 1440) {
                return 0;
            }
            int receivedInlastBlock = 0;
            for (Transaction transaction : lastBlock.getTransactions()) {
                if (transaction.getRecipientId().equals(id)) {
                    receivedInlastBlock += transaction.getAmount();
                }
            }
            return (int)(getBalance() / 100) - receivedInlastBlock;

        } else {
            if (lastBlock.getHeight() < curEffectiveBalanceLeasingHeightFrom) {
                return (int)(getGuaranteedBalance(1440) / 100) + getExtraEffectiveBalance();
            } else {
                return getExtraEffectiveBalance();
            }
        }

    }

    public int getExtraEffectiveBalance() {
        long extraEffectiveBalance = 0;
        for (Long accountId : effectiveBalanceLeasers) {
            extraEffectiveBalance += Account.getAccount(accountId).getGuaranteedBalance(1440);
        }
        return (int)(extraEffectiveBalance / 100);
    }

    public synchronized long getGuaranteedBalance(final int numberOfConfirmations) {
        if (numberOfConfirmations >= Nxt.getBlockchain().getLastBlock().getHeight()) {
            return 0;
        }
        if (numberOfConfirmations > maxTrackedBalanceConfirmations || numberOfConfirmations < 0) {
            throw new IllegalArgumentException("Number of required confirmations must be between 0 and " + maxTrackedBalanceConfirmations);
        }
        if (guaranteedBalances.isEmpty()) {
            return 0;
        }
        int i = Collections.binarySearch(guaranteedBalances, new GuaranteedBalance(Nxt.getBlockchain().getLastBlock().getHeight() - numberOfConfirmations, 0));
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

    public Map<Long, Integer> getUnconfirmedAssetBalances() {
        return Collections.unmodifiableMap(unconfirmedAssetBalances);
    }

    // returns true iff:
    // this.publicKey is set to null (in which case this.publicKey also gets set to key)
    // or
    // this.publicKey is already set to an array equal to key
    synchronized boolean setOrVerify(byte[] key, int height) {
        if (this.publicKey == null) {
            this.publicKey = key;
            this.keyHeight = -1;
            return true;
        } else if (Arrays.equals(this.publicKey, key)) {
            return true;
        } else if (this.keyHeight == -1) {
            Logger.logMessage("DUPLICATE KEY!!!");
            Logger.logMessage("Account key for " + Convert.toUnsignedLong(id) + " was already set to a different one at the same height "
                    + ", current height is " + height + ", rejecting new key");
            return false;
        } else if (this.keyHeight >= height) {
            Logger.logMessage("DUPLICATE KEY!!!");
            Logger.logMessage("Changing key for account " + Convert.toUnsignedLong(id) + " at height " + height
                    + ", was previously set to a different one at height " + keyHeight);
            this.publicKey = key;
            this.keyHeight = height;
            return true;
        }
        Logger.logMessage("DUPLICATE KEY!!!");
        Logger.logMessage("Invalid key for account " + Convert.toUnsignedLong(id) + " at height " + height
                + ", was already set to a different one at height " + keyHeight);
        return false;
    }

    synchronized void apply(byte[] key, int height) {
        if (! setOrVerify(key, this.height)) {
            throw new IllegalStateException("Generator public key mismatch");
        }
        if (this.publicKey == null) {
            throw new IllegalStateException("Public key has not been set for account " + Convert.toUnsignedLong(id)
                    +" at height " + height + ", key height is " + keyHeight);
        }
        if (this.keyHeight == -1 || this.keyHeight > height) {
            this.keyHeight = height;
        }
    }

    synchronized void undo(int height) {
        if (this.keyHeight >= height) {
            Logger.logDebugMessage("Unsetting key for account " + Convert.toUnsignedLong(id) + " at height " + height
                    + ", was previously set at height " + keyHeight);
            this.publicKey = null;
            this.keyHeight = -1;
        }
        if (this.height == height) {
            Logger.logDebugMessage("Removing account " + Convert.toUnsignedLong(id) + " which was created in the popped off block");
            accounts.remove(this.getId());
        }
    }

    synchronized int getAssetBalance(Long assetId) {
        return Convert.nullToZero(assetBalances.get(assetId));
    }

    void addToAssetBalance(Long assetId, int quantity) {
        synchronized (this) {
            Integer assetBalance = assetBalances.get(assetId);
            if (assetBalance == null) {
                assetBalances.put(assetId, quantity);
            } else {
                assetBalances.put(assetId, assetBalance + quantity);
            }
        }
        listeners.notify(this, Event.ASSET_BALANCE);
        assetListeners.notify(new AccountAsset(id, assetId, assetBalances.get(assetId)), Event.ASSET_BALANCE);
    }

    void addToUnconfirmedAssetBalance(Long assetId, int quantity) {
        synchronized (this) {
            Integer unconfirmedAssetBalance = unconfirmedAssetBalances.get(assetId);
            if (unconfirmedAssetBalance == null) {
                unconfirmedAssetBalances.put(assetId, quantity);
            } else {
                unconfirmedAssetBalances.put(assetId, unconfirmedAssetBalance + quantity);
            }
        }
        listeners.notify(this, Event.UNCONFIRMED_ASSET_BALANCE);
        assetListeners.notify(new AccountAsset(id, assetId, unconfirmedAssetBalances.get(assetId)), Event.UNCONFIRMED_ASSET_BALANCE);
    }

    void addToAssetAndUnconfirmedAssetBalance(Long assetId, int quantity) {
        synchronized (this) {
            Integer assetBalance = assetBalances.get(assetId);
            if (assetBalance == null) {
                assetBalances.put(assetId, quantity);
            } else {
                assetBalances.put(assetId, assetBalance + quantity);
            }
            Integer unconfirmedAssetBalance = unconfirmedAssetBalances.get(assetId);
            if (unconfirmedAssetBalance == null) {
                unconfirmedAssetBalances.put(assetId, quantity);
            } else {
                unconfirmedAssetBalances.put(assetId, unconfirmedAssetBalance + quantity);
            }
        }
        listeners.notify(this, Event.ASSET_BALANCE);
        listeners.notify(this, Event.UNCONFIRMED_ASSET_BALANCE);
        assetListeners.notify(new AccountAsset(id, assetId, assetBalances.get(assetId)), Event.ASSET_BALANCE);
        assetListeners.notify(new AccountAsset(id, assetId, unconfirmedAssetBalances.get(assetId)), Event.UNCONFIRMED_ASSET_BALANCE);
    }

    void addToBalance(long amount) {
        synchronized (this) {
            this.balance += amount;
            addToGuaranteedBalance(amount);
        }
        if (amount != 0) {
            listeners.notify(this, Event.BALANCE);
        }
    }

    void addToUnconfirmedBalance(long amount) {
        if (amount == 0) {
            return;
        }
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
        if (amount != 0) {
            listeners.notify(this, Event.BALANCE);
            listeners.notify(this, Event.UNCONFIRMED_BALANCE);
        }
    }

    private synchronized void addToGuaranteedBalance(long amount) {
        int blockchainHeight = Nxt.getBlockchain().getLastBlock().getHeight();
        GuaranteedBalance last = null;
        if (guaranteedBalances.size() > 0 && (last = guaranteedBalances.get(guaranteedBalances.size() - 1)).height > blockchainHeight) {
            // this only happens while last block is being popped off
            if (amount > 0) {
                // this is a reversal of a withdrawal or a fee, so previous gb records need to be corrected
                for (GuaranteedBalance gb : guaranteedBalances) {
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
                if (blockchainHeight >= Constants.TRANSPARENT_FORGING_BLOCK_4 && blockchainHeight < Constants.TRANSPARENT_FORGING_BLOCK_5) {
                    gb.balance += amount; // because of a bug which leads to a fork
                } else if (blockchainHeight >= Constants.TRANSPARENT_FORGING_BLOCK_5 && amount < 0) {
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

    public BigInteger getHit(String secretPhrase, Block block) {
        MessageDigest digest = Crypto.sha256();
        byte[] generationSignatureHash;
        if (block.getHeight() < Constants.TRANSPARENT_FORGING_BLOCK) {
            byte[] generationSignature = Crypto.sign(block.getGenerationSignature(), secretPhrase);
            generationSignatureHash = digest.digest(generationSignature);
        } else {
            digest.update(block.getGenerationSignature());
            generationSignatureHash = digest.digest(publicKey);
        }
        return new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});
    }

    public long getHitTime(BigInteger hit, Block block) {
        return block.getTimestamp() + hit.divide(BigInteger.valueOf(block.getBaseTarget()).multiply(BigInteger.valueOf(getEffectiveBalance()))).longValue();
    }

    public void leaseEffectiveBalance(Long lesseeId, short period) {
        Account lessee = Account.getAccount(lesseeId);
        if (lessee != null && lessee.getPublicKey() != null) {
            Block lastBlock = Nxt.getBlockchain().getLastBlock();
            if (curEffectiveBalanceLeasingHeightFrom == Integer.MAX_VALUE) {

                curEffectiveBalanceLeasingHeightFrom = lastBlock.getHeight() + 1440;
                curEffectiveBalanceLeasingHeightTo = curEffectiveBalanceLeasingHeightFrom + period;
                curLesseeId = lesseeId;
                nextEffectiveBalanceLeasingHeightFrom = Integer.MAX_VALUE;

            } else {

                nextEffectiveBalanceLeasingHeightFrom = lastBlock.getHeight() + 1440;
                if (nextEffectiveBalanceLeasingHeightFrom < curEffectiveBalanceLeasingHeightTo) {
                    nextEffectiveBalanceLeasingHeightFrom = curEffectiveBalanceLeasingHeightTo;
                }
                nextEffectiveBalanceLeasingHeightTo = nextEffectiveBalanceLeasingHeightFrom + period;
                nextLesseeId = lesseeId;

            }
        }
    }

    public static void reviewAllAccounts() {
        int height = Nxt.getBlockchain().getLastBlock().getHeight();
        for (Account account : getAllAccounts()) {
            if (account.curEffectiveBalanceLeasingHeightFrom != Integer.MAX_VALUE) {
                if (height == account.curEffectiveBalanceLeasingHeightFrom) {
                    Account.getAccount(account.curLesseeId).effectiveBalanceLeasers.add(account.getId());
                } else if (height == account.curEffectiveBalanceLeasingHeightTo) {
                    Account.getAccount(account.curLesseeId).effectiveBalanceLeasers.remove(account.getId());
                    if (account.nextEffectiveBalanceLeasingHeightFrom == Integer.MAX_VALUE) {
                        account.curEffectiveBalanceLeasingHeightFrom = Integer.MAX_VALUE;
                    } else {
                        account.curEffectiveBalanceLeasingHeightFrom = account.nextEffectiveBalanceLeasingHeightFrom;
                        account.curEffectiveBalanceLeasingHeightTo = account.nextEffectiveBalanceLeasingHeightTo;
                        account.curLesseeId = account.nextLesseeId;
                        account.nextEffectiveBalanceLeasingHeightFrom = Integer.MAX_VALUE;
                        if (height == account.curEffectiveBalanceLeasingHeightFrom) {
                            Account.getAccount(account.curLesseeId).effectiveBalanceLeasers.add(account.getId());
                        }
                    }
                }
            }
        }
    }

    public boolean addToLockedBalance(long amount) {
        synchronized (this) {
            if (amount > getBalance()) {
                return false;
            } else {
                addToBalanceAndUnconfirmedBalance(-amount);
                this.lockedBalance += amount;
                return true;
            }
        }
    }

    public long getLockedBalance() {
        return lockedBalance;
    }

    public boolean transferLockedBalance(long amount, Long recipientId, long discount) {
        synchronized (this) {
            if (amount > getLockedBalance()) {
                return false;
            } else {
                Account recipient = Account.getAccount(recipientId);
                if (recipient == null) {
                    return false;
                } else {
                    if (discount > 0) {
                        synchronized (recipient) {
                            if (discount > recipient.getBalance()) {
                                return false;
                            } else {
                                recipient.addToBalanceAndUnconfirmedBalance(-discount);
                                this.addToBalanceAndUnconfirmedBalance(discount);
                            }
                        }
                    }
                    this.lockedBalance -= amount;
                    recipient.addToBalanceAndUnconfirmedBalance(amount);
                    return true;
                }
            }
        }
    }

}
