package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.VersionedEntityDbTable;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public final class Currency {

    public static enum Event {
        BEFORE_DISTRIBUTE_CROWDFUNDING, BEFORE_UNDO_CROWDFUNDING, BEFORE_DELETE
    }

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

        @Override
        public String defaultSort() {
            return " ORDER BY creation_height DESC ";
        }

    };

    private static class CurrencySupply {

        private final DbKey dbKey;
        private long currencyId;
        private long currentSupply;
        private long currentReservePerUnitNQT;

        private CurrencySupply(Currency currency) {
            this.currencyId = currency.currencyId;
            this.dbKey = currencySupplyDbKeyFactory.newKey(this.currencyId);
        }

        private CurrencySupply(ResultSet rs) throws SQLException {
            this.currencyId = rs.getLong("id");
            this.dbKey = currencySupplyDbKeyFactory.newKey(this.currencyId);
            this.currentSupply = rs.getLong("current_supply");
            this.currentReservePerUnitNQT = rs.getLong("current_reserve_per_unit_nqt");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO currency_supply (id, current_supply, "
                    + "current_reserve_per_unit_nqt, height, latest) "
                    + "KEY (id, height) VALUES (?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.currencyId);
                pstmt.setLong(++i, this.currentSupply);
                pstmt.setLong(++i, this.currentReservePerUnitNQT);
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }
    }

    private static final DbKey.LongKeyFactory<CurrencySupply> currencySupplyDbKeyFactory = new DbKey.LongKeyFactory<CurrencySupply>("id") {

        @Override
        public DbKey newKey(CurrencySupply currencySupply) {
            return currencySupply.dbKey;
        }

    };

    private static final VersionedEntityDbTable<CurrencySupply> currencySupplyTable = new VersionedEntityDbTable<CurrencySupply>("currency_supply", currencySupplyDbKeyFactory) {

        @Override
        protected CurrencySupply load(Connection con, ResultSet rs) throws SQLException {
            return new CurrencySupply(rs);
        }

        @Override
        protected void save(Connection con, CurrencySupply currencySupply) throws SQLException {
            currencySupply.save(con);
        }

    };

    private static final Listeners<Currency,Event> listeners = new Listeners<>();

    public static boolean addListener(Listener<Currency> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Currency> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

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

    public static DbIterator<Currency> searchCurrencies(String query, int from, int to) {
        return currencyTable.search(query, DbClause.EMPTY_CLAUSE, from, to, " ORDER BY ft.score DESC, currency.creation_height DESC ");
    }

    static void addCurrency(Transaction transaction, Attachment.MonetarySystemCurrencyIssuance attachment) {
        Currency oldCurrency;
        if ((oldCurrency = Currency.getCurrencyByCode(attachment.getCode())) != null) {
            oldCurrency.delete(transaction.getSenderId());
        }
        if ((oldCurrency = Currency.getCurrencyByCode(attachment.getName().toUpperCase())) != null) {
            oldCurrency.delete(transaction.getSenderId());
        }
        if ((oldCurrency = Currency.getCurrencyByName(attachment.getName())) != null) {
            oldCurrency.delete(transaction.getSenderId());
        }
        if ((oldCurrency = Currency.getCurrencyByName(attachment.getCode())) != null) {
            oldCurrency.delete(transaction.getSenderId());
        }
        Currency currency = new Currency(transaction, attachment);
        currencyTable.insert(currency);
        if (currency.is(CurrencyType.MINTABLE) || currency.is(CurrencyType.RESERVABLE)) {
            CurrencySupply currencySupply = currency.getSupplyData();
            currencySupply.currentSupply = attachment.getInitialSupply();
            currencySupplyTable.insert(currencySupply);
        }

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
    private final long maxSupply;
    private final long reserveSupply;
    private final int creationHeight;
    private final int issuanceHeight;
    private final long minReservePerUnitNQT;
    private final int minDifficulty;
    private final int maxDifficulty;
    private final byte ruleset;
    private final byte algorithm;
    private final byte decimals;
    private final long initialSupply;
    private CurrencySupply currencySupply;

    private Currency(Transaction transaction, Attachment.MonetarySystemCurrencyIssuance attachment) {
        this.currencyId = transaction.getId();
        this.dbKey = currencyDbKeyFactory.newKey(this.currencyId);
        this.accountId = transaction.getSenderId();
        this.name = attachment.getName();
        this.code = attachment.getCode();
        this.description = attachment.getDescription();
        this.type = attachment.getType();
        this.initialSupply = attachment.getInitialSupply();
        this.reserveSupply = attachment.getReserveSupply();
        this.maxSupply = attachment.getMaxSupply();
        this.creationHeight = transaction.getHeight();
        this.issuanceHeight = attachment.getIssuanceHeight();
        this.minReservePerUnitNQT = attachment.getMinReservePerUnitNQT();
        this.minDifficulty = attachment.getMinDifficulty();
        this.maxDifficulty = attachment.getMaxDifficulty();
        this.ruleset = attachment.getRuleset();
        this.algorithm = attachment.getAlgorithm();
        this.decimals = attachment.getDecimals();
    }

    private Currency(ResultSet rs) throws SQLException {
        this.currencyId = rs.getLong("id");
        this.dbKey = currencyDbKeyFactory.newKey(this.currencyId);
        this.accountId = rs.getLong("account_id");
        this.name = rs.getString("name");
        this.code = rs.getString("code");
        this.description = rs.getString("description");
        this.type = rs.getInt("type");
        this.initialSupply = rs.getLong("initial_supply");
        this.reserveSupply = rs.getLong("reserve_supply");
        this.maxSupply = rs.getLong("max_supply");
        this.creationHeight = rs.getInt("creation_height");
        this.issuanceHeight = rs.getInt("issuance_height");
        this.minReservePerUnitNQT = rs.getLong("min_reserve_per_unit_nqt");
        this.minDifficulty = rs.getByte("min_difficulty") & 0xFF;
        this.maxDifficulty = rs.getByte("max_difficulty") & 0xFF;
        this.ruleset = rs.getByte("ruleset");
        this.algorithm = rs.getByte("algorithm");
        this.decimals = rs.getByte("decimals");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO currency (id, account_id, name, code, "
                + "description, type, initial_supply, reserve_supply, max_supply, creation_height, issuance_height, min_reserve_per_unit_nqt, "
                + "min_difficulty, max_difficulty, ruleset, algorithm, decimals, height, latest) "
                + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setString(++i, this.getName());
            pstmt.setString(++i, this.getCode());
            pstmt.setString(++i, this.getDescription());
            pstmt.setInt(++i, this.getType());
            pstmt.setLong(++i, this.getInitialSupply());
            pstmt.setLong(++i, this.getReserveSupply());
            pstmt.setLong(++i, this.getMaxSupply());
            pstmt.setInt(++i, this.getCreationHeight());
            pstmt.setInt(++i, this.getIssuanceHeight());
            pstmt.setLong(++i, this.getMinReservePerUnitNQT());
            pstmt.setByte(++i, (byte)this.getMinDifficulty());
            pstmt.setByte(++i, (byte)this.getMaxDifficulty());
            pstmt.setByte(++i, this.getRuleset());
            pstmt.setByte(++i, this.getAlgorithm());
            pstmt.setByte(++i, this.getDecimals());
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

    public long getInitialSupply() {
        return initialSupply;
    }

    public long getCurrentSupply() {
        if (!is(CurrencyType.RESERVABLE) && !is(CurrencyType.MINTABLE)) {
            return initialSupply;
        }
        if (getSupplyData() == null) {
            return 0;
        }
        return currencySupply.currentSupply;
    }

    public long getReserveSupply() {
        return reserveSupply;
    }

    public long getMaxSupply() {
        return maxSupply;
    }

    public int getCreationHeight() {
        return creationHeight;
    }

    public int getIssuanceHeight() {
        return issuanceHeight;
    }

    public long getMinReservePerUnitNQT() {
        return minReservePerUnitNQT;
    }

    public int getMinDifficulty() {
        return minDifficulty;
    }

    public int getMaxDifficulty() {
        return maxDifficulty;
    }

    public byte getRuleset() {
        return ruleset;
    }

    public byte getAlgorithm() {
        return algorithm;
    }

    public byte getDecimals() {
        return decimals;
    }

    public long getCurrentReservePerUnitNQT() {
        if (!is(CurrencyType.RESERVABLE) || getSupplyData() == null) {
            return 0;
        }
        return currencySupply.currentReservePerUnitNQT;
    }

    public boolean isActive() {
        return issuanceHeight <= BlockchainImpl.getInstance().getHeight();
    }

    private CurrencySupply getSupplyData() {
        if (!is(CurrencyType.RESERVABLE) && !is(CurrencyType.MINTABLE)) {
            return null;
        }
        if (currencySupply == null) {
            currencySupply = currencySupplyTable.get(currencySupplyDbKeyFactory.newKey(currencyId));
            if (currencySupply == null) {
                currencySupply = new CurrencySupply(this);
            }
        }
        return currencySupply;
    }

    static void increaseReserve(Account account, long currencyId, long amountPerUnitNQT) {
        Currency currency = Currency.getCurrency(currencyId);
        account.addToBalanceNQT(-Convert.safeMultiply(currency.getReserveSupply(), amountPerUnitNQT));
        CurrencySupply currencySupply = currency.getSupplyData();
        currencySupply.currentReservePerUnitNQT += amountPerUnitNQT;
        currencySupplyTable.insert(currencySupply);
        CurrencyFounder.addOrUpdateFounder(currencyId, account.getId(), amountPerUnitNQT);
    }

    static void claimReserve(Account account, long currencyId, long units) {
        account.addToCurrencyUnits(currencyId, -units);
        Currency currency = Currency.getCurrency(currencyId);
        currency.increaseSupply(- units);
        account.addToBalanceAndUnconfirmedBalanceNQT(Convert.safeMultiply(units, currency.getCurrentReservePerUnitNQT()));
    }

    static void transferCurrency(Account senderAccount, Account recipientAccount, long currencyId, long units) {
        senderAccount.addToCurrencyUnits(currencyId, -units);
        recipientAccount.addToCurrencyAndUnconfirmedCurrencyUnits(currencyId, units);
    }

    void increaseSupply(long units) {
        getSupplyData();
        currencySupply.currentSupply += units;
        if (currencySupply.currentSupply > maxSupply || currencySupply.currentSupply < 0) {
            currencySupply.currentSupply -= units;
            throw new IllegalArgumentException("Cannot add " + units + " to current supply of " + currencySupply.currentSupply);
        }
        currencySupplyTable.insert(currencySupply);
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

    public boolean is(CurrencyType type) {
        return (this.type & type.getCode()) != 0;
    }

    public boolean canBeDeletedBy(long ownerAccountId) {
        if (!isActive()) {
            return ownerAccountId == accountId;
        }
        if (is(CurrencyType.MINTABLE) && getCurrentSupply() < maxSupply && ownerAccountId != accountId) {
            return false;
        }
        try (DbIterator<Account.AccountCurrency> accountCurrencies = Account.getCurrencyAccounts(this.currencyId, 0, -1)) {
            return ! accountCurrencies.hasNext() || accountCurrencies.next().getAccountId() == ownerAccountId && ! accountCurrencies.hasNext();
        }
    }

    void delete(long ownerAccountId) {
        if (!canBeDeletedBy(ownerAccountId)) {
            // shouldn't happen as ownership has already been checked in validate, but as a safety check
            throw new IllegalStateException("Currency " + Convert.toUnsignedLong(currencyId) + " not entirely owned by " + Convert.toUnsignedLong(ownerAccountId));
        }
        listeners.notify(this, Event.BEFORE_DELETE);
        Account ownerAccount = Account.getAccount(ownerAccountId);
        if (is(CurrencyType.RESERVABLE)) {
            if (is(CurrencyType.CLAIMABLE) && isActive()) {
                ownerAccount.addToUnconfirmedCurrencyUnits(currencyId, -ownerAccount.getCurrencyUnits(currencyId));
                Currency.claimReserve(ownerAccount, currencyId, ownerAccount.getCurrencyUnits(currencyId));
            }
            if (!isActive()) {
                try (DbIterator<CurrencyFounder> founders = CurrencyFounder.getCurrencyFounders(currencyId, 0, Integer.MAX_VALUE)) {
                    for (CurrencyFounder founder : founders) {
                        Account.getAccount(founder.getAccountId()).addToBalanceAndUnconfirmedBalanceNQT(Convert.safeMultiply(reserveSupply, founder.getAmountPerUnitNQT()));
                    }
                }
            }
            CurrencyFounder.remove(currencyId);
        }
        if (is(CurrencyType.EXCHANGEABLE)) {
            List<CurrencyBuyOffer> buyOffers = new ArrayList<>();
            try (DbIterator<CurrencyBuyOffer> offers = CurrencyBuyOffer.getOffers(this, 0, -1)) {
                while (offers.hasNext()) {
                    buyOffers.add(offers.next());
                }
            }
            for (CurrencyBuyOffer offer : buyOffers) {
                CurrencyExchangeOffer.removeOffer(offer);
            }
        }
        if (is(CurrencyType.MINTABLE)) {
            CurrencyMint.deleteCurrency(this);
        }
        ownerAccount.addToUnconfirmedCurrencyUnits(currencyId, -ownerAccount.getUnconfirmedCurrencyUnits(currencyId));
        ownerAccount.addToCurrencyUnits(currencyId, -ownerAccount.getCurrencyUnits(currencyId));
        currencyTable.delete(this);
    }

    private static final class CrowdFundingListener implements Listener<Block> {

        @Override
        public void notify(Block block) {
            if (block.getHeight() <= Constants.MONETARY_SYSTEM_BLOCK) {
                return;
            }
            try (DbIterator<Currency> issuedCurrencies = currencyTable.getManyBy(new DbClause.IntClause("issuance_height", block.getHeight()), 0, -1)) {
                for (Currency currency : issuedCurrencies) {
                    if (currency.getCurrentReservePerUnitNQT() < currency.getMinReservePerUnitNQT()) {
                        listeners.notify(currency, Event.BEFORE_UNDO_CROWDFUNDING);
                        undoCrowdFunding(currency);
                    } else {
                        listeners.notify(currency, Event.BEFORE_DISTRIBUTE_CROWDFUNDING);
                        distributeCurrency(currency);
                    }
                }
            }
        }

        private void undoCrowdFunding(Currency currency) {
            try (DbIterator<CurrencyFounder> founders = CurrencyFounder.getCurrencyFounders(currency.getId(), 0, Integer.MAX_VALUE)) {
                for (CurrencyFounder founder : founders) {
                    Account.getAccount(founder.getAccountId()).addToBalanceAndUnconfirmedBalanceNQT(Convert.safeMultiply(currency.getReserveSupply(), founder.getAmountPerUnitNQT()));
                }
            }
            Account.getAccount(currency.getAccountId()).addToCurrencyAndUnconfirmedCurrencyUnits(currency.getId(), - currency.getInitialSupply());
            currencyTable.delete(currency);
            CurrencyFounder.remove(currency.getId());
        }

        private void distributeCurrency(Currency currency) {
            long totalAmountPerUnit = 0;
            final long remainingSupply = currency.getReserveSupply() - currency.getInitialSupply();
            List<CurrencyFounder> currencyFounders = new ArrayList<>();
            try (DbIterator<CurrencyFounder> founders = CurrencyFounder.getCurrencyFounders(currency.getId(), 0, Integer.MAX_VALUE)) {
                for (CurrencyFounder founder : founders) {
                    totalAmountPerUnit += founder.getAmountPerUnitNQT();
                    currencyFounders.add(founder);
                }
            }
            CurrencySupply currencySupply = currency.getSupplyData();
            for (CurrencyFounder founder : currencyFounders) {
                long units = Convert.safeDivide(Convert.safeMultiply(remainingSupply, founder.getAmountPerUnitNQT()), totalAmountPerUnit);
                currencySupply.currentSupply += units;
                Account.getAccount(founder.getAccountId()).addToCurrencyAndUnconfirmedCurrencyUnits(currency.getId(), units);
            }
            Account issuerAccount = Account.getAccount(currency.getAccountId());
            issuerAccount.addToCurrencyAndUnconfirmedCurrencyUnits(currency.getId(), currency.getReserveSupply() - currency.getCurrentSupply());
            if (!currency.is(CurrencyType.CLAIMABLE)) {
                issuerAccount.addToBalanceAndUnconfirmedBalanceNQT(Convert.safeMultiply(totalAmountPerUnit, currency.getReserveSupply()));
            }
            currencySupply.currentSupply = currency.getReserveSupply();
            currencySupplyTable.insert(currencySupply);
        }
    }
}
