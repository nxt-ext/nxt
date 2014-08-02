package nxt;

import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.db.BasicDbTable;
import nxt.db.Db;
import nxt.db.DbUtils;
import nxt.db.VersioningDbTable;
import nxt.db.VersioningLinkDbTable;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class Account {

    public static enum Event {
        BALANCE, UNCONFIRMED_BALANCE, ASSET_BALANCE, UNCONFIRMED_ASSET_BALANCE,
        LEASE_SCHEDULED, LEASE_STARTED, LEASE_ENDED
    }

    public static class AccountAsset {

        public final Long accountId;
        public final Long assetId;
        public final Long quantityQNT;
        public final Long unconfirmedQuantityQNT;

        private AccountAsset(Long accountId, Long assetId, Long quantityQNT, Long unconfirmedQuantityQNT) {
            this.accountId = accountId;
            this.assetId = assetId;
            this.quantityQNT = quantityQNT;
            this.unconfirmedQuantityQNT = unconfirmedQuantityQNT;
        }

        private AccountAsset(ResultSet rs) throws SQLException {
            this.accountId = rs.getLong("account_id");
            this.assetId = rs.getLong("asset_id");
            this.quantityQNT = rs.getLong("quantity");
            this.unconfirmedQuantityQNT = rs.getLong("unconfirmed_quantity");
        }

        private void save() {
            if (this.quantityQNT > 0 || this.unconfirmedQuantityQNT > 0) {
                accountAssetTable.insert(this.accountId, this.assetId, this);
            } else if (this.quantityQNT == 0 && this.unconfirmedQuantityQNT == 0) {
                accountAssetTable.delete(this.accountId, this.assetId);
            } else if (this.quantityQNT < 0 || this.unconfirmedQuantityQNT < 0) {
                throw new DoubleSpendingException("Negative asset balance for account " + Convert.toUnsignedLong(this.accountId));
            }
        }

        @Override
        public String toString() {
            return "AccountAsset account_id: " + Convert.toUnsignedLong(accountId) + " asset_id: " + Convert.toUnsignedLong(assetId)
                    + " quantity: " + quantityQNT + " unconfirmedQuantity: " + unconfirmedQuantityQNT;
        }
    }

    public static class AccountLease {

        public final Long lessorId;
        public final Long lesseeId;
        public final int fromHeight;
        public final int toHeight;

        private AccountLease(Long lessorId, Long lesseeId, int fromHeight, int toHeight) {
            this.lessorId = lessorId;
            this.lesseeId = lesseeId;
            this.fromHeight = fromHeight;
            this.toHeight = toHeight;
        }

    }

    static class DoubleSpendingException extends RuntimeException {

        DoubleSpendingException(String message) {
            super(message);
        }

    }

    static {

        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                int height = block.getHeight();
                for (Account account : getLeasingAccounts()) {
                    if (height == account.currentLeasingHeightFrom) {
                        leaseListeners.notify(
                                new AccountLease(account.getId(), account.currentLesseeId, height, account.currentLeasingHeightTo),
                                Event.LEASE_STARTED);
                    } else if (height == account.currentLeasingHeightTo) {
                        leaseListeners.notify(
                                new AccountLease(account.getId(), account.currentLesseeId, account.currentLeasingHeightFrom, height),
                                Event.LEASE_ENDED);
                        if (account.nextLeasingHeightFrom == Integer.MAX_VALUE) {
                            account.currentLeasingHeightFrom = Integer.MAX_VALUE;
                            account.currentLesseeId = null;
                            accountTable.insert(account);
                        } else {
                            account.currentLeasingHeightFrom = account.nextLeasingHeightFrom;
                            account.currentLeasingHeightTo = account.nextLeasingHeightTo;
                            account.currentLesseeId = account.nextLesseeId;
                            account.nextLeasingHeightFrom = Integer.MAX_VALUE;
                            account.nextLesseeId = null;
                            accountTable.insert(account);
                            if (height == account.currentLeasingHeightFrom) {
                                leaseListeners.notify(
                                        new AccountLease(account.getId(), account.currentLesseeId, height, account.currentLeasingHeightTo),
                                        Event.LEASE_STARTED);
                            }
                        }
                    }
                }
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);

        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                int height = block.getHeight();
                for (Account account : getLeasingAccounts()) {
                    if (height == account.currentLeasingHeightFrom || height == account.currentLeasingHeightTo) {
                        accountTable.rollbackTo(account.getId(), height - 1);
                    }
                }
            }
        }, BlockchainProcessor.Event.BLOCK_POPPED);

        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                if (block.getHeight() % 1440 != 0) {
                    return;
                }
                try (Connection con = Db.getConnection();
                     PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM account_guaranteed_balance "
                             + "WHERE height < ?")) {
                    pstmtDelete.setInt(1, block.getHeight() - maxTrackedBalanceConfirmations);
                    pstmtDelete.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    private static final int maxTrackedBalanceConfirmations = 2881;

    private static VersioningDbTable<Account> accountTable = new VersioningDbTable<Account>() {

        @Override
        protected Long getId(Account account) {
            return account.getId();
        }

        @Override
        protected String table() {
            return "account";
        }

        @Override
        protected Account load(Connection con, ResultSet rs) throws SQLException {
            return new Account(rs);
        }

        @Override
        protected void save(Connection con, Account account) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account (id, creation_height, public_key, "
                    + "key_height, balance, unconfirmed_balance, forged_balance, name, description, "
                    + "current_leasing_height_from, current_leasing_height_to, current_lessee_id, "
                    + "next_leasing_height_from, next_leasing_height_to, next_lessee_id, "
                    + "height, latest) "
                    + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, account.getId());
                pstmt.setInt(++i, account.getCreationHeight());
                DbUtils.setBytes(pstmt, ++i, account.getPublicKey());
                pstmt.setInt(++i, account.getKeyHeight());
                pstmt.setLong(++i, account.getBalanceNQT());
                pstmt.setLong(++i, account.getUnconfirmedBalanceNQT());
                pstmt.setLong(++i, account.getForgedBalanceNQT());
                DbUtils.setString(pstmt, ++i, account.getName());
                DbUtils.setString(pstmt, ++i, account.getDescription());
                DbUtils.setInt(pstmt, ++i, Convert.zeroToNull(account.getCurrentLeasingHeightFrom()));
                DbUtils.setInt(pstmt, ++i, Convert.zeroToNull(account.getCurrentLeasingHeightTo()));
                DbUtils.setLong(pstmt, ++i, account.getCurrentLesseeId());
                DbUtils.setInt(pstmt, ++i, Convert.zeroToNull(account.getNextLeasingHeightFrom()));
                DbUtils.setInt(pstmt, ++i, Convert.zeroToNull(account.getNextLeasingHeightTo()));
                DbUtils.setLong(pstmt, ++i, account.getNextLesseeId());
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

    };

    private static final VersioningLinkDbTable<AccountAsset> accountAssetTable = new VersioningLinkDbTable<AccountAsset>() {

        @Override
        protected String table() {
            return "account_asset";
        }

        @Override
        protected AccountAsset load(Connection con, ResultSet rs) throws SQLException {
            return new AccountAsset(rs);
        }

        @Override
        protected void save(Connection con, Long idA, Long idB, AccountAsset accountAsset) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_asset "
                    + "(account_id, asset_id, quantity, unconfirmed_quantity, height, latest) "
                    + "KEY (account_id, asset_id, height) VALUES (?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, idA);
                pstmt.setLong(++i, idB);
                pstmt.setLong(++i, accountAsset.quantityQNT);
                pstmt.setLong(++i, accountAsset.unconfirmedQuantityQNT);
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        @Override
        protected String idColumnA() {
            return "account_id";
        }

        @Override
        protected String idColumnB() {
            return "asset_id";
        }

    };

    private static final BasicDbTable accountGuaranteedBalanceTable = new BasicDbTable() {
        @Override
        protected String table() {
            return "account_guaranteed_balance";
        }
    };

    private static final Listeners<Account,Event> listeners = new Listeners<>();

    private static final Listeners<AccountAsset,Event> assetListeners = new Listeners<>();

    private static final Listeners<AccountLease,Event> leaseListeners = new Listeners<>();

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

    public static boolean addLeaseListener(Listener<AccountLease> listener, Event eventType) {
        return leaseListeners.addListener(listener, eventType);
    }

    public static boolean removeLeaseListener(Listener<AccountLease> listener, Event eventType) {
        return leaseListeners.removeListener(listener, eventType);
    }

    public static Collection<Account> getAllAccounts() {
        return accountTable.getAll();
    }

    public static int getCount() {
        return accountTable.getCount();
    }

    public static Account getAccount(Long id) {
        return id == null ? null : accountTable.get(id);
    }

    public static Account getAccount(byte[] publicKey) {
        return accountTable.get(getId(publicKey));
    }

    public static Long getId(byte[] publicKey) {
        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        return Convert.fullHashToId(publicKeyHash);
    }

    static Account addOrGetAccount(Long id) {
        Account account = accountTable.get(id);
        if (account == null) {
            account = new Account(id);
            accountTable.insert(account);
        }
        return account;
    }

    public static List<Account> getLeasingAccounts() {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM account WHERE current_lessee_id >= ? AND latest = TRUE "
                     + "ORDER BY id ASC")) {
            pstmt.setLong(1, Long.MIN_VALUE); // this forces H2 to use the index, unlike WHERE IS NOT NULL which does a table scan
            return accountTable.getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static void clear() {
        accountTable.truncate();
        accountAssetTable.truncate();
        accountGuaranteedBalanceTable.truncate();
    }

    private final Long id;
    private final int creationHeight;
    private byte[] publicKey;
    private int keyHeight;
    private long balanceNQT;
    private long unconfirmedBalanceNQT;
    private long forgedBalanceNQT;

    private int currentLeasingHeightFrom;
    private int currentLeasingHeightTo;
    private Long currentLesseeId;
    private int nextLeasingHeightFrom;
    private int nextLeasingHeightTo;
    private Long nextLesseeId;
    private String name;
    private String description;

    private Account(Long id) {
        if (! id.equals(Crypto.rsDecode(Crypto.rsEncode(id)))) {
            Logger.logMessage("CRITICAL ERROR: Reed-Solomon encoding fails for " + id);
        }
        this.id = id;
        this.creationHeight = Nxt.getBlockchain().getLastBlock().getHeight();
        currentLeasingHeightFrom = Integer.MAX_VALUE;
    }

    private Account(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.creationHeight = rs.getInt("creation_height");
        this.publicKey = rs.getBytes("public_key");
        this.keyHeight = rs.getInt("key_height");
        this.balanceNQT = rs.getLong("balance");
        this.unconfirmedBalanceNQT = rs.getLong("unconfirmed_balance");
        this.forgedBalanceNQT = rs.getLong("forged_balance");
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        this.currentLeasingHeightFrom = rs.getInt("current_leasing_height_from");
        this.currentLeasingHeightTo = rs.getInt("current_leasing_height_to");
        this.currentLesseeId = DbUtils.getLong(rs, "current_lessee_id");
        this.nextLeasingHeightFrom = rs.getInt("next_leasing_height_from");
        this.nextLeasingHeightTo = rs.getInt("next_leasing_height_to");
        this.nextLesseeId = DbUtils.getLong(rs, "next_lessee_id");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    void setAccountInfo(String name, String description) {
        this.name = Convert.emptyToNull(name.trim());
        this.description = Convert.emptyToNull(description.trim());
        accountTable.insert(this);
    }

    public byte[] getPublicKey() {
        if (this.keyHeight == -1) {
            return null;
        }
        return publicKey;
    }

    private int getCreationHeight() {
        return creationHeight;
    }

    private int getKeyHeight() {
        return keyHeight;
    }

    public EncryptedData encryptTo(byte[] data, String senderSecretPhrase) {
        if (getPublicKey() == null) {
            throw new IllegalArgumentException("Recipient account doesn't have a public key set");
        }
        return EncryptedData.encrypt(data, Crypto.getPrivateKey(senderSecretPhrase), publicKey);
    }

    public byte[] decryptFrom(EncryptedData encryptedData, String recipientSecretPhrase) {
        if (getPublicKey() == null) {
            throw new IllegalArgumentException("Sender account doesn't have a public key set");
        }
        return encryptedData.decrypt(Crypto.getPrivateKey(recipientSecretPhrase), publicKey);
    }

    public long getBalanceNQT() {
        return balanceNQT;
    }

    public long getUnconfirmedBalanceNQT() {
        return unconfirmedBalanceNQT;
    }

    public long getForgedBalanceNQT() {
        return forgedBalanceNQT;
    }

    public long getEffectiveBalanceNXT() {
        Block lastBlock = Nxt.getBlockchain().getLastBlock();
        if (lastBlock.getHeight() >= Constants.TRANSPARENT_FORGING_BLOCK_6
                && (getPublicKey() == null || lastBlock.getHeight() - keyHeight <= 1440)) {
            return 0; // cfb: Accounts with the public key revealed less than 1440 blocks ago are not allowed to generate blocks
        }
        if (lastBlock.getHeight() < Constants.TRANSPARENT_FORGING_BLOCK_3
                && this.creationHeight < Constants.TRANSPARENT_FORGING_BLOCK_2) {
            if (this.creationHeight == 0) {
                return getBalanceNQT() / Constants.ONE_NXT;
            }
            if (lastBlock.getHeight() - this.creationHeight < 1440) {
                return 0;
            }
            long receivedInlastBlock = 0;
            for (Transaction transaction : lastBlock.getTransactions()) {
                if (id.equals(transaction.getRecipientId())) {
                    receivedInlastBlock += transaction.getAmountNQT();
                }
            }
            return (getBalanceNQT() - receivedInlastBlock) / Constants.ONE_NXT;
        }
        if (lastBlock.getHeight() < currentLeasingHeightFrom) {
            return (getGuaranteedBalanceNQT(1440) + getLessorsGuaranteedBalanceNQT()) / Constants.ONE_NXT;
        }
        return getLessorsGuaranteedBalanceNQT() / Constants.ONE_NXT;
    }

    private long getLessorsGuaranteedBalanceNQT() {
        long lessorsGuaranteedBalanceNQT = 0;
        for (Account lessor : getLessors()) {
            lessorsGuaranteedBalanceNQT += lessor.getGuaranteedBalanceNQT(1440);
        }
        return lessorsGuaranteedBalanceNQT;
    }

    public List<Account> getLessors() {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM account WHERE current_lessee_id = ? "
                     + "AND current_leasing_height_from <= ? AND current_leasing_height_to > ? AND latest = TRUE ")) {
            pstmt.setLong(1, this.id);
            int height = Nxt.getBlockchain().getHeight();
            pstmt.setInt(2, height);
            pstmt.setInt(3, height);
            return accountTable.getManyBy(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public long getGuaranteedBalanceNQT(final int numberOfConfirmations) {
        if (numberOfConfirmations >= Nxt.getBlockchain().getLastBlock().getHeight()) {
            return 0;
        }
        if (numberOfConfirmations > maxTrackedBalanceConfirmations || numberOfConfirmations < 0) {
            throw new IllegalArgumentException("Number of required confirmations must be between 0 and " + maxTrackedBalanceConfirmations);
        }
        int height = Nxt.getBlockchain().getHeight() - numberOfConfirmations;
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT SUM (additions) AS additions "
                     + "FROM account_guaranteed_balance WHERE account_id = ? AND height > ?")) {
            pstmt.setLong(1, this.id);
            pstmt.setInt(2, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return balanceNQT;
                }
                return Math.max(Convert.safeSubtract(balanceNQT, rs.getLong("additions")), 0);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public List<AccountAsset> getAccountAssets() {
        return accountAssetTable.getManyByA(this.id);
    }

    public Long getUnconfirmedAssetBalanceQNT(Long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(this.id, assetId);
        return accountAsset == null ? 0 : accountAsset.unconfirmedQuantityQNT;
    }

    public Long getCurrentLesseeId() {
        return currentLesseeId;
    }

    public Long getNextLesseeId() {
        return nextLesseeId;
    }

    public int getCurrentLeasingHeightFrom() {
        return currentLeasingHeightFrom;
    }

    public int getCurrentLeasingHeightTo() {
        return currentLeasingHeightTo;
    }

    public int getNextLeasingHeightFrom() {
        return nextLeasingHeightFrom;
    }

    public int getNextLeasingHeightTo() {
        return nextLeasingHeightTo;
    }

    void leaseEffectiveBalance(Long lesseeId, short period) {
        Account lessee = Account.getAccount(lesseeId);
        if (lessee != null && lessee.getPublicKey() != null) {
            Block lastBlock = Nxt.getBlockchain().getLastBlock();
            if (currentLeasingHeightFrom == Integer.MAX_VALUE) {

                currentLeasingHeightFrom = lastBlock.getHeight() + 1440;
                currentLeasingHeightTo = currentLeasingHeightFrom + period;
                currentLesseeId = lesseeId;
                nextLeasingHeightFrom = Integer.MAX_VALUE;
                accountTable.insert(this);
                leaseListeners.notify(
                        new AccountLease(this.getId(), lesseeId, currentLeasingHeightFrom, currentLeasingHeightTo),
                        Event.LEASE_SCHEDULED);

            } else {

                nextLeasingHeightFrom = lastBlock.getHeight() + 1440;
                if (nextLeasingHeightFrom < currentLeasingHeightTo) {
                    nextLeasingHeightFrom = currentLeasingHeightTo;
                }
                nextLeasingHeightTo = nextLeasingHeightFrom + period;
                nextLesseeId = lesseeId;
                accountTable.insert(this);
                leaseListeners.notify(
                        new AccountLease(this.getId(), lesseeId, nextLeasingHeightFrom, nextLeasingHeightTo),
                        Event.LEASE_SCHEDULED);

            }
        }
    }

    // returns true iff:
    // this.publicKey is set to null (in which case this.publicKey also gets set to key)
    // or
    // this.publicKey is already set to an array equal to key
    boolean setOrVerify(byte[] key, int height) {
        if (this.publicKey == null) {
            this.publicKey = key;
            this.keyHeight = -1;
            accountTable.insert(this);
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
            accountTable.insert(this);
            return true;
        }
        Logger.logMessage("DUPLICATE KEY!!!");
        Logger.logMessage("Invalid key for account " + Convert.toUnsignedLong(id) + " at height " + height
                + ", was already set to a different one at height " + keyHeight);
        return false;
    }

    void apply(byte[] key, int height) {
        if (! setOrVerify(key, this.creationHeight)) {
            throw new IllegalStateException("Generator public key mismatch");
        }
        if (this.publicKey == null) {
            throw new IllegalStateException("Public key has not been set for account " + Convert.toUnsignedLong(id)
                    +" at height " + height + ", key height is " + keyHeight);
        }
        if (this.keyHeight == -1 || this.keyHeight > height) {
            this.keyHeight = height;
            accountTable.insert(this);
        }
    }

    //TODO
    void undo(int height) {
        if (this.keyHeight >= height) {
            Logger.logDebugMessage("Unsetting key for account " + Convert.toUnsignedLong(id) + " at height " + height
                    + ", was previously set at height " + keyHeight);
            this.publicKey = null;
            this.keyHeight = -1;
            accountTable.insert(this);
        }
        if (this.creationHeight == height) {
            Logger.logDebugMessage("Removing account " + Convert.toUnsignedLong(id) + " which was created in the popped off block");
            accountTable.delete(this);
        }
    }

    long getAssetBalanceQNT(Long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(this.id, assetId);
        return accountAsset == null ? 0 : accountAsset.quantityQNT;
    }

    void addToAssetBalanceQNT(Long assetId, long quantityQNT) {
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(this.id, assetId);
        long assetBalance = accountAsset == null ? 0 : accountAsset.quantityQNT;
        assetBalance = Convert.safeAdd(assetBalance, quantityQNT);
        accountAsset = new AccountAsset(this.id, assetId, assetBalance, accountAsset == null ? 0 : accountAsset.unconfirmedQuantityQNT);
        accountAsset.save();
        listeners.notify(this, Event.ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.ASSET_BALANCE);
    }

    void addToUnconfirmedAssetBalanceQNT(Long assetId, long quantityQNT) {
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(this.id, assetId);
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.unconfirmedQuantityQNT;
        unconfirmedAssetBalance = Convert.safeAdd(unconfirmedAssetBalance, quantityQNT);
        accountAsset = new AccountAsset(this.id, assetId, accountAsset == null ? 0 : accountAsset.quantityQNT, unconfirmedAssetBalance);
        accountAsset.save();
        listeners.notify(this, Event.UNCONFIRMED_ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.UNCONFIRMED_ASSET_BALANCE);
    }

    void addToAssetAndUnconfirmedAssetBalanceQNT(Long assetId, long quantityQNT) {
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(this.id, assetId);
        long assetBalance = accountAsset == null ? 0 : accountAsset.quantityQNT;
        assetBalance = Convert.safeAdd(assetBalance, quantityQNT);
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.unconfirmedQuantityQNT;
        unconfirmedAssetBalance = Convert.safeAdd(unconfirmedAssetBalance, quantityQNT);
        accountAsset = new AccountAsset(this.id, assetId, assetBalance, unconfirmedAssetBalance);
        accountAsset.save();
        listeners.notify(this, Event.ASSET_BALANCE);
        listeners.notify(this, Event.UNCONFIRMED_ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.UNCONFIRMED_ASSET_BALANCE);
    }

    void addToBalanceNQT(long amountNQT) {
        this.balanceNQT = Convert.safeAdd(this.balanceNQT, amountNQT);
        addToGuaranteedBalanceNQT(amountNQT);
        checkBalance();
        accountTable.insert(this);
        if (amountNQT != 0) {
            listeners.notify(this, Event.BALANCE);
        }
    }

    void addToUnconfirmedBalanceNQT(long amountNQT) {
        if (amountNQT == 0) {
            return;
        }
        this.unconfirmedBalanceNQT = Convert.safeAdd(this.unconfirmedBalanceNQT, amountNQT);
        checkBalance();
        accountTable.insert(this);
        listeners.notify(this, Event.UNCONFIRMED_BALANCE);
    }

    void addToBalanceAndUnconfirmedBalanceNQT(long amountNQT) {
        this.balanceNQT = Convert.safeAdd(this.balanceNQT, amountNQT);
        this.unconfirmedBalanceNQT = Convert.safeAdd(this.unconfirmedBalanceNQT, amountNQT);
        addToGuaranteedBalanceNQT(amountNQT);
        checkBalance();
        accountTable.insert(this);
        if (amountNQT != 0) {
            listeners.notify(this, Event.BALANCE);
            listeners.notify(this, Event.UNCONFIRMED_BALANCE);
        }
    }

    void addToForgedBalanceNQT(long amountNQT) {
        this.forgedBalanceNQT = Convert.safeAdd(this.forgedBalanceNQT, amountNQT);
        accountTable.insert(this);
    }

    private void checkBalance() {
        if (id.equals(Genesis.CREATOR_ID)) {
            return;
        }
        if (balanceNQT < 0) {
            throw new DoubleSpendingException("Negative balance for account " + Convert.toUnsignedLong(id));
        }
        if (unconfirmedBalanceNQT < 0) {
            throw new DoubleSpendingException("Negative unconfirmed balance for account " + Convert.toUnsignedLong(id));
        }
        if (unconfirmedBalanceNQT > balanceNQT) {
            throw new DoubleSpendingException("Unconfirmed balance exceeds balance for account " + Convert.toUnsignedLong(id));
        }
    }

    //TODO: undo
    private void addToGuaranteedBalanceNQT(long amountNQT) {
        if (amountNQT <= 0) {
            return;
        }
        int blockchainHeight = Nxt.getBlockchain().getHeight();
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT additions FROM account_guaranteed_balance "
                     + "WHERE account_id = ? and height = ?");
             PreparedStatement pstmtUpdate = con.prepareStatement("MERGE INTO account_guaranteed_balance (account_id, "
                     + " additions, height) KEY (account_id, height) VALUES(?, ?, ?)")) {
            pstmtSelect.setLong(1, this.id);
            pstmtSelect.setInt(2, blockchainHeight);
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                long additions = amountNQT;
                if (rs.next()) {
                    additions = Convert.safeAdd(additions, rs.getLong("additions"));
                }
                pstmtUpdate.setLong(1, this.id);
                pstmtUpdate.setLong(2, additions);
                pstmtUpdate.setInt(3, blockchainHeight);
                pstmtUpdate.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
