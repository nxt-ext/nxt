package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.VersionedEntityDbTable;
import nxt.util.Convert;
import nxt.util.Listener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@SuppressWarnings("UnusedDeclaration")
public final class Currency {

    private static final DbKey.LongKeyFactory<Currency> currencyDbKeyFactory = new DbKey.LongKeyFactory<Currency>("id") {

        @Override
        public DbKey newKey(Currency currency) {
            return currency.dbKey;
        }

    };

    private static final VersionedEntityDbTable<Currency> currencyTable = new VersionedEntityDbTable<Currency>("currency", currencyDbKeyFactory) {

        @Override
        protected Currency load(Connection con, ResultSet rs) throws SQLException {
            return new Currency(rs);
        }

        @Override
        protected void save(Connection con, Currency currency) throws SQLException {
            currency.save(con);
        }

    };

    public static DbIterator<Currency> getAllCurrencies(int from, int to) {
        return currencyTable.getAll(from, to);
    }

    public static int getCount() {
        return currencyTable.getCount();
    }

    public static Currency getCurrency(long id) {
        return currencyTable.get(currencyDbKeyFactory.newKey(id));
    }

    public static Currency getCurrencyByName(String name) {
        return currencyTable.getBy(new DbClause.StringClause("name_lower", name.toLowerCase()));
    }

    public static Currency getCurrencyByCode(String code) {
        return currencyTable.getBy(new DbClause.StringClause("code", code));
    }

    public static DbIterator<Currency> getCurrencyIssuedBy(long accountId, int from, int to) {
        return currencyTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    static void addCurrency(Transaction transaction, Attachment.MonetarySystemCurrencyIssuance attachment) {
        currencyTable.insert(new Currency(transaction, attachment));
    }

    static {
        Nxt.getBlockchainProcessor().addListener(new CrowdFundingListener(), BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    static void init() {}

    private final long currencyId;

    private final DbKey dbKey;
    private final long accountId;
    private final String name;
    private final String code;
    private final String description;
    private final int type;
    private long totalSupply;
    private final int issuanceHeight;
    private final long minReservePerUnitNQT;
    private final byte minDifficulty;
    private final byte maxDifficulty;
    private final byte ruleset;
    private final byte algorithm;
    private long currentSupply;

    private long currentReservePerUnitNQT;

    private Currency(Transaction transaction, Attachment.MonetarySystemCurrencyIssuance attachment) {
        this.currencyId = transaction.getId();
        this.dbKey = currencyDbKeyFactory.newKey(this.currencyId);
        this.accountId = transaction.getSenderId();
        this.name = attachment.getName();
        this.code = attachment.getCode();
        this.description = attachment.getDescription();
        this.type = attachment.getType();
        this.totalSupply = attachment.getTotalSupply();
        this.issuanceHeight = attachment.getIssuanceHeight();
        this.minReservePerUnitNQT = attachment.getMinReservePerUnitNQT();
        this.minDifficulty = attachment.getMinDifficulty();
        this.maxDifficulty = attachment.getMaxDifficulty();
        this.ruleset = attachment.getRuleset();
        this.algorithm = attachment.getAlgorithm();
        this.currentSupply = attachment.getCurrentSupply();
        this.currentReservePerUnitNQT = 0;
    }

    private Currency(ResultSet rs) throws SQLException {
        this.currencyId = rs.getLong("id");
        this.dbKey = currencyDbKeyFactory.newKey(this.currencyId);
        this.accountId = rs.getLong("account_id");
        this.name = rs.getString("name");
        this.code = rs.getString("code");
        this.description = rs.getString("description");
        this.type = rs.getInt("type");
        this.totalSupply = rs.getLong("total_supply");
        this.issuanceHeight = rs.getInt("issuance_height");
        this.minReservePerUnitNQT = rs.getLong("min_reserve_per_unit_nqt");
        this.minDifficulty = rs.getByte("min_difficulty");
        this.maxDifficulty = rs.getByte("max_difficulty");
        this.ruleset = rs.getByte("ruleset");
        this.algorithm = rs.getByte("algorithm");
        this.currentSupply = rs.getLong("current_supply");
        this.currentReservePerUnitNQT = rs.getLong("current_reserve_per_unit_nqt");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO currency (id, account_id, name, code, "
                + "description, type, total_supply, issuance_height, min_reserve_per_unit_nqt, "
                + "min_difficulty, max_difficulty, ruleset, algorithm, current_supply, current_reserve_per_unit_nqt, height, latest) "
                + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setString(++i, this.getName());
            pstmt.setString(++i, this.getCode());
            pstmt.setString(++i, this.getDescription());
            pstmt.setInt(++i, this.getType());
            pstmt.setLong(++i, this.getTotalSupply());
            pstmt.setInt(++i, this.getIssuanceHeight());
            pstmt.setLong(++i, this.getMinReservePerUnitNQT());
            pstmt.setByte(++i, this.getMinDifficulty());
            pstmt.setByte(++i, this.getMaxDifficulty());
            pstmt.setByte(++i, this.getRuleset());
            pstmt.setByte(++i, this.getAlgorithm());
            pstmt.setLong(++i, this.getCurrentSupply());
            pstmt.setLong(++i, this.getCurrentReservePerUnitNQT());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return currencyId;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getType() {
        return type;
    }

    public long getTotalSupply() {
        return totalSupply;
    }

    public int getIssuanceHeight() {
        return issuanceHeight;
    }

    public long getMinReservePerUnitNQT() {
        return minReservePerUnitNQT;
    }

    public byte getMinDifficulty() {
        return minDifficulty;
    }

    public byte getMaxDifficulty() {
        return maxDifficulty;
    }

    public byte getRuleset() {
        return ruleset;
    }

    public byte getAlgorithm() {
        return algorithm;
    }

    public long getCurrentSupply() {
        return currentSupply;
    }

    public long getCurrentReservePerUnitNQT() {
        return currentReservePerUnitNQT;
    }

    //TODO: each getCurrency call is a database query, those two methods should be made non-static,
    // and the Currency object retrieved only once and then passed around through the currency validation calls
    public static boolean isActive(long currencyId) {
        Currency currency = getCurrency(currencyId);
        return currency != null && currency.getIssuanceHeight() <= BlockchainImpl.getInstance().getHeight();
    }

    public static boolean isIssuer(long currencyId, long accountId) {
        Currency currency = getCurrency(currencyId);
        return currency != null && currency.getAccountId() == accountId;
    }

    //TODO: claimReserve decreases totalSupply, but increaseReserve doesn't increase totalSupply, is it intentional that totalSupply can go down but never go up?
    static void increaseReserve(Account account, long currencyId, long amountNQT) {
        Currency currency = Currency.getCurrency(currencyId);
        account.addToBalanceNQT(-Convert.safeMultiply(currency.getTotalSupply(), amountNQT));
        currency.currentReservePerUnitNQT += amountNQT;
        currencyTable.insert(currency);
        CurrencyFounder.addOrUpdateFounder(currencyId, account.getId(), Convert.safeMultiply(currency.getTotalSupply(), amountNQT));
    }

    static void claimReserve(Account account, long currencyId, long units) {
        account.addToCurrencyUnits(currencyId, -units);
        Currency currency = Currency.getCurrency(currencyId);
        currency.totalSupply -= units;
        currencyTable.insert(currency);
        account.addToBalanceAndUnconfirmedBalanceNQT(Convert.safeMultiply(units, currency.currentReservePerUnitNQT));
    }

    static void transferCurrency(Account account, long recipientId, long currencyId, long units) {
        account.addToCurrencyUnits(currencyId, -units);
        Account.addOrGetAccount(recipientId).addToCurrencyAndUnconfirmedCurrencyUnits(currencyId, units);
    }

    void increaseSupply(long units) {
        currentSupply += units;
        currencyTable.insert(this);
    }

    public DbIterator<Account.AccountCurrency> getAccounts(int from, int to) {
        return Account.getCurrencyAccounts(this.currencyId, from, to);
    }

    public DbIterator<Account.AccountCurrency> getAccounts(int height, int from, int to) {
        if (height < 0) {
            return getAccounts(from, to);
        }
        return Account.getCurrencyAccounts(this.currencyId, height, from, to);
    }

    public DbIterator<Exchange> getExchanges(int from, int to) {
        return Exchange.getCurrencyExchanges(this.currencyId, from, to);
    }

    public DbIterator<CurrencyTransfer> getTransfers(int from, int to) {
        return CurrencyTransfer.getCurrencyTransfers(this.currencyId, from, to);
    }

    private static final class CrowdFundingListener implements Listener<Block> {

        @Override
        public void notify(Block block) {
            //TODO: this will do a full table scan and load every single currency in memory
            //need to re-write to use an sql query that only returns the matching currencies
            try (DbIterator<Currency> allCurrencies = currencyTable.getAll(0, -1)) {
                for (Currency currency : allCurrencies) {
                    if (currency.getIssuanceHeight() == 0 || currency.getIssuanceHeight() > block.getHeight()) {
                        continue;
                    }
                    if (currency.getCurrentReservePerUnitNQT() < currency.getMinReservePerUnitNQT()) {
                        undoCrowdFunding(currency);
                    } else {
                        distributeCurrency(currency);
                    }
                }
            }
        }

        private void undoCrowdFunding(Currency currency) {
            for (CurrencyFounder founder : CurrencyFounder.getCurrencyFounders(currency.getId(), 0, Integer.MAX_VALUE)) {
                Account.getAccount(founder.getAccountId()).addToBalanceAndUnconfirmedBalanceNQT(founder.getValue());
            }
            currencyTable.delete(currency);
            CurrencyFounder.remove(currency.getId());
        }

        private void distributeCurrency(Currency currency) {
            long totalValue = 0;
            for (CurrencyFounder founder : CurrencyFounder.getCurrencyFounders(currency.getId(), 0, Integer.MAX_VALUE)) {
                totalValue += founder.getValue();
            }
            for (CurrencyFounder founder : CurrencyFounder.getCurrencyFounders(currency.getId(), 0, Integer.MAX_VALUE)) {
                long units = Convert.safeDivide(Convert.safeMultiply(currency.getTotalSupply(), founder.getValue()), totalValue);
                Account.getAccount(founder.getAccountId()).addToCurrencyAndUnconfirmedCurrencyUnits(currency.getId(), units);
            }
        }
    }
}
