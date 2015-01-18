package nxt;

import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.DerivedDbTable;
import nxt.db.VersionedEntityDbTable;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings({"UnusedDeclaration", "SuspiciousNameCombination"})
public final class Account {

    public static enum Event {
        BALANCE, UNCONFIRMED_BALANCE, ASSET_BALANCE, UNCONFIRMED_ASSET_BALANCE, CURRENCY_BALANCE, UNCONFIRMED_CURRENCY_BALANCE,
        LEASE_SCHEDULED, LEASE_STARTED, LEASE_ENDED
    }

    public static class AccountAsset {

        private final long accountId;
        private final long assetId;
        private final DbKey dbKey;
        private long quantityQNT;
        private long unconfirmedQuantityQNT;

        private AccountAsset(long accountId, long assetId, long quantityQNT, long unconfirmedQuantityQNT) {
            this.accountId = accountId;
            this.assetId = assetId;
            this.dbKey = accountAssetDbKeyFactory.newKey(this.accountId, this.assetId);
            this.quantityQNT = quantityQNT;
            this.unconfirmedQuantityQNT = unconfirmedQuantityQNT;
        }

        private AccountAsset(ResultSet rs) throws SQLException {
            this.accountId = rs.getLong("account_id");
            this.assetId = rs.getLong("asset_id");
            this.dbKey = accountAssetDbKeyFactory.newKey(this.accountId, this.assetId);
            this.quantityQNT = rs.getLong("quantity");
            this.unconfirmedQuantityQNT = rs.getLong("unconfirmed_quantity");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_asset "
                    + "(account_id, asset_id, quantity, unconfirmed_quantity, height, latest) "
                    + "KEY (account_id, asset_id, height) VALUES (?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.accountId);
                pstmt.setLong(++i, this.assetId);
                pstmt.setLong(++i, this.quantityQNT);
                pstmt.setLong(++i, this.unconfirmedQuantityQNT);
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public long getAccountId() {
            return accountId;
        }

        public long getAssetId() {
            return assetId;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public long getUnconfirmedQuantityQNT() {
            return unconfirmedQuantityQNT;
        }

        private void save() {
            checkBalance(this.accountId, this.quantityQNT, this.unconfirmedQuantityQNT);
            if (this.quantityQNT > 0 || this.unconfirmedQuantityQNT > 0) {
                accountAssetTable.insert(this);
            } else {
                accountAssetTable.delete(this);
            }
        }

        @Override
        public String toString() {
            return "AccountAsset account_id: " + Convert.toUnsignedLong(accountId) + " asset_id: " + Convert.toUnsignedLong(assetId)
                    + " quantity: " + quantityQNT + " unconfirmedQuantity: " + unconfirmedQuantityQNT;
        }

    }

    @SuppressWarnings("UnusedDeclaration")
    public static class AccountCurrency {

        private final long accountId;
        private final long currencyId;
        private final DbKey dbKey;
        private long units;
        private long unconfirmedUnits;

        private AccountCurrency(long accountId, long currencyId, long quantityQNT, long unconfirmedQuantityQNT) {
            this.accountId = accountId;
            this.currencyId = currencyId;
            this.dbKey = accountCurrencyDbKeyFactory.newKey(this.accountId, this.currencyId);
            this.units = quantityQNT;
            this.unconfirmedUnits = unconfirmedQuantityQNT;
        }

        private AccountCurrency(ResultSet rs) throws SQLException {
            this.accountId = rs.getLong("account_id");
            this.currencyId = rs.getLong("currency_id");
            this.dbKey = accountAssetDbKeyFactory.newKey(this.accountId, this.currencyId);
            this.units = rs.getLong("units");
            this.unconfirmedUnits = rs.getLong("unconfirmed_units");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_currency "
                    + "(account_id, currency_id, units, unconfirmed_units, height, latest) "
                    + "KEY (account_id, currency_id, height) VALUES (?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.accountId);
                pstmt.setLong(++i, this.currencyId);
                pstmt.setLong(++i, this.units);
                pstmt.setLong(++i, this.unconfirmedUnits);
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public long getAccountId() {
            return accountId;
        }

        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }

        public long getUnconfirmedUnits() {
            return unconfirmedUnits;
        }

        private void save() {
            checkBalance(this.accountId, this.units, this.unconfirmedUnits);
            if (this.units > 0 || this.unconfirmedUnits > 0) {
                accountCurrencyTable.insert(this);
            } else if (this.units == 0 && this.unconfirmedUnits == 0) {
                accountCurrencyTable.delete(this);
            }
        }

        @Override
        public String toString() {
            return "AccountCurrency account_id: " + Convert.toUnsignedLong(accountId) + " currency_id: " + Convert.toUnsignedLong(currencyId)
                    + " quantity: " + units + " unconfirmedQuantity: " + unconfirmedUnits;
        }

    }

    public static class AccountLease {

        public final long lessorId;
        public final long lesseeId;
        public final int fromHeight;
        public final int toHeight;

        private AccountLease(long lessorId, long lesseeId, int fromHeight, int toHeight) {
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
                if (height < Constants.TRANSPARENT_FORGING_BLOCK_6) {
                    return;
                }
                List<Account> leaseChangingAccounts = new ArrayList<>();
                try (DbIterator<Account> accounts = getLeaseChangingAccounts(height)) {
                    while (accounts.hasNext()) {
                        leaseChangingAccounts.add(accounts.next());
                    }
                }
                for (Account account : leaseChangingAccounts) {
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
                            account.currentLesseeId = 0;
                            accountTable.insert(account);
                        } else {
                            account.currentLeasingHeightFrom = account.nextLeasingHeightFrom;
                            account.currentLeasingHeightTo = account.nextLeasingHeightTo;
                            account.currentLesseeId = account.nextLesseeId;
                            account.nextLeasingHeightFrom = Integer.MAX_VALUE;
                            account.nextLesseeId = 0;
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

    }

    private static final DbKey.LongKeyFactory<Account> accountDbKeyFactory = new DbKey.LongKeyFactory<Account>("id") {

        @Override
        public DbKey newKey(Account account) {
            return account.dbKey;
        }

    };

    private static final VersionedEntityDbTable<Account> accountTable = new VersionedEntityDbTable<Account>("account", accountDbKeyFactory) {

        @Override
        protected Account load(Connection con, ResultSet rs) throws SQLException {
            return new Account(rs);
        }

        @Override
        protected void save(Connection con, Account account) throws SQLException {
            account.save(con);
        }

    };

    private static final DbKey.LinkKeyFactory<AccountAsset> accountAssetDbKeyFactory = new DbKey.LinkKeyFactory<AccountAsset>("account_id", "asset_id") {

        @Override
        public DbKey newKey(AccountAsset accountAsset) {
            return accountAsset.dbKey;
        }

    };

    private static final VersionedEntityDbTable<AccountAsset> accountAssetTable = new VersionedEntityDbTable<AccountAsset>("account_asset", accountAssetDbKeyFactory) {

        @Override
        protected AccountAsset load(Connection con, ResultSet rs) throws SQLException {
            return new AccountAsset(rs);
        }

        @Override
        protected void save(Connection con, AccountAsset accountAsset) throws SQLException {
            accountAsset.save(con);
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY quantity DESC, account_id, asset_id ";
        }

    };

    private static final DbKey.LinkKeyFactory<AccountCurrency> accountCurrencyDbKeyFactory = new DbKey.LinkKeyFactory<AccountCurrency>("account_id", "currency_id") {

        @Override
        public DbKey newKey(AccountCurrency accountCurrency) {
            return accountCurrency.dbKey;
        }

    };

    private static final VersionedEntityDbTable<AccountCurrency> accountCurrencyTable = new VersionedEntityDbTable<AccountCurrency>("account_currency", accountCurrencyDbKeyFactory) {

        @Override
        protected AccountCurrency load(Connection con, ResultSet rs) throws SQLException {
            return new AccountCurrency(rs);
        }

        @Override
        protected void save(Connection con, AccountCurrency accountCurrency) throws SQLException {
            accountCurrency.save(con);
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY units DESC, account_id, currency_id ";
        }

    };

    private static final DerivedDbTable accountGuaranteedBalanceTable = new DerivedDbTable("account_guaranteed_balance") {

        @Override
        public void trim(int height) {
            try (Connection con = Db.db.getConnection();
                 PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM account_guaranteed_balance "
                         + "WHERE height < ?")) {
                pstmtDelete.setInt(1, height - 1440);
                pstmtDelete.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

    };

    private static final Listeners<Account,Event> listeners = new Listeners<>();

    private static final Listeners<AccountAsset,Event> assetListeners = new Listeners<>();

    private static final Listeners<AccountCurrency,Event> currencyListeners = new Listeners<>();

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

    public static boolean addCurrencyListener(Listener<AccountCurrency> listener, Event eventType) {
        return currencyListeners.addListener(listener, eventType);
    }

    public static boolean removeCurrencyListener(Listener<AccountCurrency> listener, Event eventType) {
        return currencyListeners.removeListener(listener, eventType);
    }

    public static boolean addLeaseListener(Listener<AccountLease> listener, Event eventType) {
        return leaseListeners.addListener(listener, eventType);
    }

    public static boolean removeLeaseListener(Listener<AccountLease> listener, Event eventType) {
        return leaseListeners.removeListener(listener, eventType);
    }

    public static DbIterator<Account> getAllAccounts(int from, int to) {
        return accountTable.getAll(from, to);
    }

    public static int getCount() {
        return accountTable.getCount();
    }

    public static int getAssetAccountCount(long assetId) {
        return accountAssetTable.getCount(new DbClause.LongClause("asset_id", assetId));
    }

    public static int getAssetAccountCount(long assetId, int height) {
        if (height < 0) {
            return getAssetAccountCount(assetId);
        }
        return accountAssetTable.getCount(new DbClause.LongClause("asset_id", assetId), height);
    }

    public static int getAccountAssetCount(long accountId) {
        return accountAssetTable.getCount(new DbClause.LongClause("account_id", accountId));
    }

    public static int getAccountAssetCount(long accountId, int height) {
        if (height < 0) {
            return getAccountAssetCount(accountId);
        }
        return accountAssetTable.getCount(new DbClause.LongClause("account_id", accountId), height);
    }

    public static int getCurrencyAccountCount(long currencyId) {
        return accountCurrencyTable.getCount(new DbClause.LongClause("currency_id", currencyId));
    }

    public static int getCurrencyAccountCount(long currencyId, int height) {
        if (height < 0) {
            return getCurrencyAccountCount(currencyId);
        }
        return accountCurrencyTable.getCount(new DbClause.LongClause("currency_id", currencyId), height);
    }

    public static int getAccountCurrencyCount(long accountId) {
        return accountCurrencyTable.getCount(new DbClause.LongClause("account_id", accountId));
    }

    public static int getAccountCurrencyCount(long accountId, int height) {
        if (height < 0) {
            return getAccountCurrencyCount(accountId);
        }
        return accountCurrencyTable.getCount(new DbClause.LongClause("account_id", accountId), height);
    }

    public static Account getAccount(long id) {
        return id == 0 ? null : accountTable.get(accountDbKeyFactory.newKey(id));
    }

    public static Account getAccount(long id, int height) {
        return id == 0 ? null : accountTable.get(accountDbKeyFactory.newKey(id), height);
    }

    public static Account getAccount(byte[] publicKey) {
        Account account = accountTable.get(accountDbKeyFactory.newKey(getId(publicKey)));
        if (account == null) {
            return null;
        }
        if (account.getPublicKey() == null || Arrays.equals(account.getPublicKey(), publicKey)) {
            return account;
        }
        throw new RuntimeException("DUPLICATE KEY for account " + Convert.toUnsignedLong(account.getId())
                + " existing key " + Convert.toHexString(account.getPublicKey()) + " new key " + Convert.toHexString(publicKey));
    }

    public static long getId(byte[] publicKey) {
        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        return Convert.fullHashToId(publicKeyHash);
    }

    static Account addOrGetAccount(long id) {
        if (id == 0) {
            throw new IllegalArgumentException("Invalid accountId 0");
        }
        Account account = accountTable.get(accountDbKeyFactory.newKey(id));
        if (account == null) {
            account = new Account(id);
            accountTable.insert(account);
        }
        return account;
    }

    private static final class LeaseChangingAccountsClause extends DbClause {

        private final int height;

        private LeaseChangingAccountsClause(final int height) {
            super(" current_lessee_id >= ? AND (current_leasing_height_from = ? OR current_leasing_height_to = ?) ");
            this.height = height;
        }

        @Override
        public int set(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setLong(index++, Long.MIN_VALUE);
            pstmt.setInt(index++, height);
            pstmt.setInt(index++, height);
            return index;
        }

    }

    private static DbIterator<Account> getLeaseChangingAccounts(final int height) {
        return accountTable.getManyBy(new LeaseChangingAccountsClause(height), 0, -1, " ORDER BY current_lessee_id, id ");
    }

    public static DbIterator<AccountAsset> getAssetAccounts(long assetId, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to, " ORDER BY quantity DESC, account_id ");
    }

    public static DbIterator<AccountAsset> getAssetAccounts(long assetId, int height, int from, int to) {
        if (height < 0) {
            return getAssetAccounts(assetId, from, to);
        }
        return accountAssetTable.getManyBy(new DbClause.LongClause("asset_id", assetId), height, from, to, " ORDER BY quantity DESC, account_id ");
    }

    public static DbIterator<AccountCurrency> getCurrencyAccounts(long currencyId, int from, int to) {
        return accountCurrencyTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to);
    }

    public static DbIterator<AccountCurrency> getCurrencyAccounts(long currencyId, int height, int from, int to) {
        if (height < 0) {
            return getCurrencyAccounts(currencyId, from, to);
        }
        return accountCurrencyTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), height, from, to);
    }

    public static long getAssetBalanceQNT(final long accountId, final long assetId, final int height) {
        final AccountAsset accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(accountId, assetId), height);
        return accountAsset == null ? 0 : accountAsset.quantityQNT;
    }

    static void init() {}


    private final long id;
    private final DbKey dbKey;
    private final int creationHeight;
    private byte[] publicKey;
    private int keyHeight;
    private long balanceNQT;
    private long unconfirmedBalanceNQT;
    private long forgedBalanceNQT;

    private int currentLeasingHeightFrom;
    private int currentLeasingHeightTo;
    private long currentLesseeId;
    private int nextLeasingHeightFrom;
    private int nextLeasingHeightTo;
    private long nextLesseeId;
    private String name;
    private String description;
    private Pattern messagePattern;

    private Account(long id) {
        if (id != Crypto.rsDecode(Crypto.rsEncode(id))) {
            Logger.logMessage("CRITICAL ERROR: Reed-Solomon encoding fails for " + id);
        }
        this.id = id;
        this.dbKey = accountDbKeyFactory.newKey(this.id);
        this.creationHeight = Nxt.getBlockchain().getHeight();
        currentLeasingHeightFrom = Integer.MAX_VALUE;
    }

    private Account(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = accountDbKeyFactory.newKey(this.id);
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
        this.currentLesseeId = rs.getLong("current_lessee_id");
        this.nextLeasingHeightFrom = rs.getInt("next_leasing_height_from");
        this.nextLeasingHeightTo = rs.getInt("next_leasing_height_to");
        this.nextLesseeId = rs.getLong("next_lessee_id");
        String regex = rs.getString("message_pattern_regex");
        if (regex != null) {
            int flags = rs.getInt("message_pattern_flags");
            this.messagePattern = Pattern.compile(regex, flags);
        }
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account (id, creation_height, public_key, "
                + "key_height, balance, unconfirmed_balance, forged_balance, name, description, "
                + "current_leasing_height_from, current_leasing_height_to, current_lessee_id, "
                + "next_leasing_height_from, next_leasing_height_to, next_lessee_id, message_pattern_regex, message_pattern_flags, "
                + "height, latest) "
                + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setInt(++i, this.getCreationHeight());
            DbUtils.setBytes(pstmt, ++i, this.getPublicKey());
            pstmt.setInt(++i, this.getKeyHeight());
            pstmt.setLong(++i, this.getBalanceNQT());
            pstmt.setLong(++i, this.getUnconfirmedBalanceNQT());
            pstmt.setLong(++i, this.getForgedBalanceNQT());
            DbUtils.setString(pstmt, ++i, this.getName());
            DbUtils.setString(pstmt, ++i, this.getDescription());
            DbUtils.setIntZeroToNull(pstmt, ++i, this.getCurrentLeasingHeightFrom());
            DbUtils.setIntZeroToNull(pstmt, ++i, this.getCurrentLeasingHeightTo());
            DbUtils.setLongZeroToNull(pstmt, ++i, this.getCurrentLesseeId());
            DbUtils.setIntZeroToNull(pstmt, ++i, this.getNextLeasingHeightFrom());
            DbUtils.setIntZeroToNull(pstmt, ++i, this.getNextLeasingHeightTo());
            DbUtils.setLongZeroToNull(pstmt, ++i, this.getNextLesseeId());
            if (messagePattern != null) {
                pstmt.setString(++i, messagePattern.pattern());
                pstmt.setInt(++i, messagePattern.flags());
            } else {
                pstmt.setNull(++i, Types.VARCHAR);
                pstmt.setNull(++i, Types.INTEGER);
            }
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Pattern getMessagePattern() {
        return messagePattern;
    }

    void setAccountInfo(String name, String description, Pattern messagePattern) {
        this.name = Convert.emptyToNull(name.trim());
        this.description = Convert.emptyToNull(description.trim());
        this.messagePattern = messagePattern;
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
                if (id == transaction.getRecipientId()) {
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
        try (DbIterator<Account> lessors = getLessors()) {
            while (lessors.hasNext()) {
                lessorsGuaranteedBalanceNQT += lessors.next().getGuaranteedBalanceNQT(1440);
            }
        }
        return lessorsGuaranteedBalanceNQT;
    }

    private DbClause getLessorsClause(final int height) {
        return new DbClause(" current_lessee_id = ? AND current_leasing_height_from <= ? AND current_leasing_height_to > ? ") {
            @Override
            public int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setLong(index++, getId());
                pstmt.setInt(index++, height);
                pstmt.setInt(index++, height);
                return index;
            }
        };
    }

    public DbIterator<Account> getLessors() {
        return accountTable.getManyBy(getLessorsClause(Nxt.getBlockchain().getHeight()), 0, -1);
    }

    public DbIterator<Account> getLessors(int height) {
        if (height < 0) {
            return getLessors();
        }
        return accountTable.getManyBy(getLessorsClause(height), height, 0, -1);
    }

    public long getGuaranteedBalanceNQT(final int numberOfConfirmations) {
        return getGuaranteedBalanceNQT(numberOfConfirmations, Nxt.getBlockchain().getHeight());
    }

    public long getGuaranteedBalanceNQT(final int numberOfConfirmations, final int currentHeight) {
        if (numberOfConfirmations >= currentHeight) {
            return 0;
        }
        if (numberOfConfirmations > 2880 || numberOfConfirmations < 0) {
            throw new IllegalArgumentException("Number of required confirmations must be between 0 and " + 2880);
        }
        int height = currentHeight - numberOfConfirmations;
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT SUM (additions) AS additions "
                     + "FROM account_guaranteed_balance WHERE account_id = ? AND height > ? AND height <= ?")) {
            pstmt.setLong(1, this.id);
            pstmt.setInt(2, height);
            pstmt.setInt(3, currentHeight);
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

    public DbIterator<AccountAsset> getAssets(int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("account_id", this.id), from, to);
    }

    public DbIterator<AccountAsset> getAssets(int height, int from, int to) {
        if (height < 0) {
            return getAssets(from, to);
        }
        return accountAssetTable.getManyBy(new DbClause.LongClause("account_id", this.id), height, from, to);
    }

    public DbIterator<Trade> getTrades(int from, int to) {
        return Trade.getAccountTrades(this.id, from, to);
    }

    public DbIterator<AssetTransfer> getAssetTransfers(int from, int to) {
        return AssetTransfer.getAccountAssetTransfers(this.id, from, to);
    }

    public DbIterator<CurrencyTransfer> getCurrencyTransfers(int from, int to) {
        return CurrencyTransfer.getAccountCurrencyTransfers(this.id, from, to);
    }

    public DbIterator<Exchange> getExchanges(int from, int to) {
        return Exchange.getAccountExchanges(this.id, from, to);
    }

    public AccountAsset getAsset(long assetId) {
        return accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
    }

    public AccountAsset getAsset(long assetId, int height) {
        if (height < 0) {
            return getAsset(assetId);
        }
        return accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId), height);
    }

    public long getAssetBalanceQNT(long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
        return accountAsset == null ? 0 : accountAsset.quantityQNT;
    }

    public long getAssetBalanceQNT(long assetId, int height) {
        AccountAsset accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId), height);
        return accountAsset == null ? 0 : accountAsset.quantityQNT;
    }

    public long getUnconfirmedAssetBalanceQNT(long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
        return accountAsset == null ? 0 : accountAsset.unconfirmedQuantityQNT;
    }

    public AccountCurrency getCurrency(long currencyId) {
        return accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
    }

    public AccountCurrency getCurrency(long currencyId, int height) {
        if (height < 0) {
            return getCurrency(currencyId);
        }
        return accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(this.id, currencyId), height);
    }

    public DbIterator<AccountCurrency> getCurrencies(int from, int to) {
        return accountCurrencyTable.getManyBy(new DbClause.LongClause("account_id", this.id), from, to);
    }

    public DbIterator<AccountCurrency> getCurrencies(int height, int from, int to) {
        if (height < 0) {
            return getCurrencies(from, to);
        }
        return accountCurrencyTable.getManyBy(new DbClause.LongClause("account_id", this.id), height, from, to);
    }

    public long getCurrencyUnits(long currencyId) {
        AccountCurrency accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
        return accountCurrency == null ? 0 : accountCurrency.units;
    }

    public long getUnconfirmedCurrencyUnits(long currencyId) {
        AccountCurrency accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
        return accountCurrency == null ? 0 : accountCurrency.unconfirmedUnits;
    }

    public long getCurrentLesseeId() {
        return currentLesseeId;
    }

    public long getNextLesseeId() {
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

    void leaseEffectiveBalance(long lesseeId, short period) {
        Account lessee = Account.getAccount(lesseeId);
        if (lessee != null && lessee.getPublicKey() != null) {
            int height = Nxt.getBlockchain().getHeight();
            if (currentLeasingHeightFrom == Integer.MAX_VALUE) {
                currentLeasingHeightFrom = height + 1440;
                currentLeasingHeightTo = currentLeasingHeightFrom + period;
                currentLesseeId = lesseeId;
                nextLeasingHeightFrom = Integer.MAX_VALUE;
                accountTable.insert(this);
                leaseListeners.notify(
                        new AccountLease(this.getId(), lesseeId, currentLeasingHeightFrom, currentLeasingHeightTo),
                        Event.LEASE_SCHEDULED);
            } else {
                nextLeasingHeightFrom = height + 1440;
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
            if (Db.db.isInTransaction()) {
                this.publicKey = key;
                this.keyHeight = -1;
                accountTable.insert(this);
            }
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
            if (Db.db.isInTransaction()) {
                Logger.logMessage("Changing key for account " + Convert.toUnsignedLong(id) + " at height " + height
                        + ", was previously set to a different one at height " + keyHeight);
                this.publicKey = key;
                this.keyHeight = height;
                accountTable.insert(this);
            }
            return true;
        }
        Logger.logMessage("DUPLICATE KEY!!!");
        Logger.logMessage("Invalid key for account " + Convert.toUnsignedLong(id) + " at height " + height
                + ", was already set to a different one at height " + keyHeight);
        return false;
    }

    void apply(byte[] key, int height) {
        if (! setOrVerify(key, this.creationHeight)) {
            throw new IllegalStateException("Public key mismatch");
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

    void addToAssetBalanceQNT(long assetId, long quantityQNT) {
        if (quantityQNT == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
        long assetBalance = accountAsset == null ? 0 : accountAsset.quantityQNT;
        assetBalance = Convert.safeAdd(assetBalance, quantityQNT);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(this.id, assetId, assetBalance, 0);
        } else {
            accountAsset.quantityQNT = assetBalance;
        }
        accountAsset.save();
        listeners.notify(this, Event.ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.ASSET_BALANCE);
    }

    void addToUnconfirmedAssetBalanceQNT(long assetId, long quantityQNT) {
        if (quantityQNT == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.unconfirmedQuantityQNT;
        unconfirmedAssetBalance = Convert.safeAdd(unconfirmedAssetBalance, quantityQNT);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(this.id, assetId, 0, unconfirmedAssetBalance);
        } else {
            accountAsset.unconfirmedQuantityQNT = unconfirmedAssetBalance;
        }
        accountAsset.save();
        listeners.notify(this, Event.UNCONFIRMED_ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.UNCONFIRMED_ASSET_BALANCE);
    }

    void addToAssetAndUnconfirmedAssetBalanceQNT(long assetId, long quantityQNT) {
        if (quantityQNT == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
        long assetBalance = accountAsset == null ? 0 : accountAsset.quantityQNT;
        assetBalance = Convert.safeAdd(assetBalance, quantityQNT);
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.unconfirmedQuantityQNT;
        unconfirmedAssetBalance = Convert.safeAdd(unconfirmedAssetBalance, quantityQNT);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(this.id, assetId, assetBalance, unconfirmedAssetBalance);
        } else {
            accountAsset.quantityQNT = assetBalance;
            accountAsset.unconfirmedQuantityQNT = unconfirmedAssetBalance;
        }
        accountAsset.save();
        listeners.notify(this, Event.ASSET_BALANCE);
        listeners.notify(this, Event.UNCONFIRMED_ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.UNCONFIRMED_ASSET_BALANCE);
    }

    void addToCurrencyUnits(long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency;
        accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
        long currencyUnits = accountCurrency == null ? 0 : accountCurrency.units;
        currencyUnits = Convert.safeAdd(currencyUnits, units);
        if (accountCurrency == null) {
            accountCurrency = new AccountCurrency(this.id, currencyId, currencyUnits, 0);
        } else {
            accountCurrency.units = currencyUnits;
        }
        accountCurrency.save();
        listeners.notify(this, Event.CURRENCY_BALANCE);
        currencyListeners.notify(accountCurrency, Event.CURRENCY_BALANCE);
    }

    void addToUnconfirmedCurrencyUnits(long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
        long unconfirmedCurrencyUnits = accountCurrency == null ? 0 : accountCurrency.unconfirmedUnits;
        unconfirmedCurrencyUnits = Convert.safeAdd(unconfirmedCurrencyUnits, units);
        if (accountCurrency == null) {
            accountCurrency = new AccountCurrency(this.id, currencyId, 0, unconfirmedCurrencyUnits);
        } else {
            accountCurrency.unconfirmedUnits = unconfirmedCurrencyUnits;
        }
        accountCurrency.save();
        listeners.notify(this, Event.UNCONFIRMED_CURRENCY_BALANCE);
        currencyListeners.notify(accountCurrency, Event.UNCONFIRMED_CURRENCY_BALANCE);
    }

    void addToCurrencyAndUnconfirmedCurrencyUnits(long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency;
        accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
        long currencyUnits = accountCurrency == null ? 0 : accountCurrency.units;
        currencyUnits = Convert.safeAdd(currencyUnits, units);
        long unconfirmedCurrencyUnits = accountCurrency == null ? 0 : accountCurrency.unconfirmedUnits;
        unconfirmedCurrencyUnits = Convert.safeAdd(unconfirmedCurrencyUnits, units);
        if (accountCurrency == null) {
            accountCurrency = new AccountCurrency(this.id, currencyId, currencyUnits, unconfirmedCurrencyUnits);
        } else {
            accountCurrency.units = currencyUnits;
            accountCurrency.unconfirmedUnits = unconfirmedCurrencyUnits;
        }
        accountCurrency.save();
        listeners.notify(this, Event.CURRENCY_BALANCE);
        listeners.notify(this, Event.UNCONFIRMED_CURRENCY_BALANCE);
        currencyListeners.notify(accountCurrency, Event.CURRENCY_BALANCE);
        currencyListeners.notify(accountCurrency, Event.UNCONFIRMED_CURRENCY_BALANCE);
    }

    void addToBalanceNQT(long amountNQT) {
        if (amountNQT == 0) {
            return;
        }
        this.balanceNQT = Convert.safeAdd(this.balanceNQT, amountNQT);
        addToGuaranteedBalanceNQT(amountNQT);
        checkBalance(this.id, this.balanceNQT, this.unconfirmedBalanceNQT);
        accountTable.insert(this);
        listeners.notify(this, Event.BALANCE);
    }

    void addToUnconfirmedBalanceNQT(long amountNQT) {
        if (amountNQT == 0) {
            return;
        }
        this.unconfirmedBalanceNQT = Convert.safeAdd(this.unconfirmedBalanceNQT, amountNQT);
        checkBalance(this.id, this.balanceNQT, this.unconfirmedBalanceNQT);
        accountTable.insert(this);
        listeners.notify(this, Event.UNCONFIRMED_BALANCE);
    }

    void addToBalanceAndUnconfirmedBalanceNQT(long amountNQT) {
        if (amountNQT == 0) {
            return;
        }
        this.balanceNQT = Convert.safeAdd(this.balanceNQT, amountNQT);
        this.unconfirmedBalanceNQT = Convert.safeAdd(this.unconfirmedBalanceNQT, amountNQT);
        addToGuaranteedBalanceNQT(amountNQT);
        checkBalance(this.id, this.balanceNQT, this.unconfirmedBalanceNQT);
        accountTable.insert(this);
        listeners.notify(this, Event.BALANCE);
        listeners.notify(this, Event.UNCONFIRMED_BALANCE);
    }

    void addToForgedBalanceNQT(long amountNQT) {
        if (amountNQT == 0) {
            return;
        }
        this.forgedBalanceNQT = Convert.safeAdd(this.forgedBalanceNQT, amountNQT);
        accountTable.insert(this);
    }

    private static void checkBalance(long accountId, long confirmed, long unconfirmed) {
        if (accountId == Genesis.CREATOR_ID) {
            return;
        }
        if (confirmed < 0) {
            throw new DoubleSpendingException("Negative balance or quantity for account " + Convert.toUnsignedLong(accountId));
        }
        if (unconfirmed < 0) {
            throw new DoubleSpendingException("Negative unconfirmed balance or quantity for account " + Convert.toUnsignedLong(accountId));
        }
        if (unconfirmed > confirmed) {
            throw new DoubleSpendingException("Unconfirmed exceeds confirmed balance or quantity for account " + Convert.toUnsignedLong(accountId));
        }
    }

    private void addToGuaranteedBalanceNQT(long amountNQT) {
        if (amountNQT <= 0) {
            return;
        }
        int blockchainHeight = Nxt.getBlockchain().getHeight();
        try (Connection con = Db.db.getConnection();
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

    void payDividends(final long assetId, final int height, final long amountNQTPerQNT) {
        long totalDividend = 0;
        List<AccountAsset> accountAssets = new ArrayList<>();
        try (DbIterator<AccountAsset> iterator = getAssetAccounts(assetId, height, 0, -1)) {
            while (iterator.hasNext()) {
                accountAssets.add(iterator.next());
            }
        }
        for (final AccountAsset accountAsset : accountAssets) {
            if (accountAsset.getAccountId() != this.id && accountAsset.getAccountId() != Genesis.CREATOR_ID) {
                long dividend = Convert.safeMultiply(accountAsset.getQuantityQNT(), amountNQTPerQNT);
                Account.getAccount(accountAsset.getAccountId()).addToBalanceAndUnconfirmedBalanceNQT(dividend);
                totalDividend += dividend;
            }
        }
        this.addToBalanceNQT(-totalDividend);
    }

}
