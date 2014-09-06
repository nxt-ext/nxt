package nxt;

import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.EntityDbTable;
import nxt.db.VersionedEntityDbTable;
import nxt.util.Convert;
import nxt.util.Listener;
import org.eclipse.jetty.util.ConcurrentHashSet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("UnusedDeclaration")
public final class Currency {

    public static final class NonIssuedCurrency {

        private final Currency currency;
        private final Map<Long, Long> founders;

        NonIssuedCurrency(Currency currency) {
            this.currency = currency;
            this.founders = new HashMap<>();
        }

        public Currency getCurrency() {
            return currency;
        }

        public Map<Long, Long> getFounders() {
            return founders;
        }

        public void addFounder(Long accountId, Long amount) {
            Long initialAmount = founders.get(accountId);
            if (initialAmount == null) {
                founders.put(accountId, amount);
            } else {
                founders.put(accountId, initialAmount + amount);
            }
        }

    }

    private static final DbKey.LongKeyFactory<Currency> currencyDbKeyFactory = new DbKey.LongKeyFactory<Currency>("id") {

        @Override
        public DbKey newKey(Currency currency) {
            return currency.dbKey;
        }

    };

    private static final EntityDbTable<Currency> currencyTable = new VersionedEntityDbTable<Currency>(currencyDbKeyFactory) {

        @Override
        protected String table() {
            return "asset";
        }

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

    public static Currency getCurrency(Long id) {
        return currencyTable.get(currencyDbKeyFactory.newKey(id));
    }

    public static DbIterator<Currency> getCurrencyIssuedBy(Long accountId, int from, int to) {
        return currencyTable.getManyBy("account_id", accountId, from, to);
    }

    static void addCurrency(Transaction transaction, Attachment.MonetarySystemCurrencyIssuance attachment) {
        currencyTable.insert(new Currency(transaction, attachment));
    }

    private static final ConcurrentMap<Long, Currency> currencies = new ConcurrentHashMap<>();
    private static final Collection<Currency> allCurrencies = Collections.unmodifiableCollection(currencies.values());
    private static final Set<String> currencyNames = new ConcurrentHashSet<>();
    private static final Set<String> currencyCodes = new ConcurrentHashSet<>();

    private static final ConcurrentMap<Long, NonIssuedCurrency> nonIssuedCurrencies = new ConcurrentHashMap<>();

    static {
        addNXTCurrency();

        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                for (Map.Entry<Long, NonIssuedCurrency> nonIssuedCurrencyEntry : nonIssuedCurrencies.entrySet()) {
                    NonIssuedCurrency nonIssuedCurrency = nonIssuedCurrencyEntry.getValue();
                    Currency currency = nonIssuedCurrency.getCurrency();
                    if (currency.getIssuanceHeight() <= block.getHeight()) {
                        if (currency.getCurrentReservePerUnitNQT() < currency.getMinReservePerUnitNQT()) {
                            for (Map.Entry<Long, Long> founderEntry : nonIssuedCurrency.getFounders().entrySet()) {
                                Account.getAccount(founderEntry.getKey()).addToBalanceAndUnconfirmedBalanceNQT(founderEntry.getValue());
                            }

                            currencies.remove(nonIssuedCurrencyEntry.getKey());
                            currencyNames.remove(currency.getName());
                            currencyCodes.remove(currency.getCode());
                        }

                        nonIssuedCurrencies.remove(nonIssuedCurrencyEntry.getKey());
                    }
                }
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);

    }

    public static Collection<Currency> getAllCurrencies() {
        return allCurrencies;
    }

    static void addNXTCurrency() {
        addCurrency(0L, Genesis.GENESIS_BLOCK_ID, "Nxt", "NXT", "", (byte)0, Constants.MAX_BALANCE_NQT, 0, 1, (byte)0, (byte)0, (byte)0, Constants.MAX_BALANCE_NQT, 0);
    }

    static void addCurrency(Long currencyId, Long accountId, String name, String code, String description, byte type, long totalSupply, int issuanceHeight, long minReservePerUnitNQT, byte minDifficulty, byte maxDifficulty, byte ruleset, long currentSupply, long currentReservePerUnitNQT) {
        Currency currency = new Currency(currencyId, (long)-1, name, code, description, type, totalSupply, issuanceHeight, minReservePerUnitNQT, minDifficulty, maxDifficulty, ruleset, currentSupply, currentReservePerUnitNQT);
        if (Currency.currencies.putIfAbsent(currencyId, currency) != null) {
            throw new IllegalStateException("Currency with id " + Convert.toUnsignedLong(currencyId) + " already exists");
        }
        currencyNames.add(name.toLowerCase());
        currencyCodes.add(code);

        if (currency.getIssuanceHeight() > 0) {
            nonIssuedCurrencies.put(currencyId, new NonIssuedCurrency(currency));
        }
    }

    static void clear() {
        currencies.clear();
        currencyNames.clear();
        currencyCodes.clear();
        addNXTCurrency();
    }

    static boolean isNameSquatted(String name) {
        return currencyNames.contains(name);
    }

    static boolean isCodeSquatted(String code) {
        return currencyCodes.contains(code);
    }

    private final Long currencyId;

    private final DbKey dbKey;
    private final Long accountId;
    private final String name;
    private final String code;
    private final String description;
    private final byte type;
    private long totalSupply;
    private final int issuanceHeight;
    private final long minReservePerUnitNQT;
    private final byte minDifficulty;
    private final byte maxDifficulty;
    private final byte ruleset;
    private long currentSupply;

    private long currentReservePerUnitNQT;

    public Currency(Transaction transaction, Attachment.MonetarySystemCurrencyIssuance attachment) {
        this(transaction.getId(), transaction.getSenderId(), attachment.getName(), attachment.getCode(), attachment.getDescription(), attachment.getType(),
                attachment.getTotalSupply(), attachment.getIssuanceHeight(), attachment.getMinReservePerUnitNQT(),
                attachment.getMinDifficulty(), attachment.getMaxDifficulty(), attachment.getRuleset(), attachment.getTotalSupply(), 0);
    }

    private Currency(Long currencyId, Long accountId, String name, String code, String description, byte type, long totalSupply, int issuanceHeight, long minReservePerUnitNQT, byte minDifficulty, byte maxDifficulty, byte ruleset, long currentSupply, long currentReservePerUnitNQT) {
        this.currencyId = currencyId;
        this.dbKey = currencyDbKeyFactory.newKey(this.currencyId);
        this.accountId = accountId;
        this.name = name;
        this.code = code;
        this.description = description;
        this.type = type;
        this.totalSupply = totalSupply;
        this.issuanceHeight = issuanceHeight;
        this.minReservePerUnitNQT = minReservePerUnitNQT;
        this.minDifficulty = minDifficulty;
        this.maxDifficulty = maxDifficulty;
        this.ruleset = ruleset;
        this.currentSupply = currentSupply;
        this.currentReservePerUnitNQT = currentReservePerUnitNQT;
    }

    private Currency(ResultSet rs) throws SQLException {
        this.currencyId = rs.getLong("id");
        this.dbKey = currencyDbKeyFactory.newKey(this.currencyId);
        this.accountId = rs.getLong("account_id");
        this.name = rs.getString("name");
        this.code = rs.getString("code");
        this.description = rs.getString("description");
        this.type = rs.getByte("type");
        this.totalSupply = rs.getLong("total_supply");
        this.issuanceHeight = rs.getInt("issuance_height");
        this.minReservePerUnitNQT = rs.getLong("min_reserve_per_unit_nqt");
        this.minDifficulty = rs.getByte("min_difficulty");
        this.maxDifficulty = rs.getByte("max_difficulty");
        this.ruleset = rs.getByte("ruleset");
        this.currentSupply = rs.getLong("current_supply");
        this.currentReservePerUnitNQT = rs.getLong("current_reserve_per_unit_nqt");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO currency (id, account_id, name, code, "
                + "description, type, total_supply, issuance_height, min_reserve_per_unit_nqt, "
                + "min_difficulty, max_difficulty, ruleset, current_supply, current_reserve_per_unit_nqt) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getCurrencyId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setString(++i, this.getName());
            pstmt.setString(++i, this.getCode());
            pstmt.setString(++i, this.getDescription());
            pstmt.setByte(++i, this.getType());
            pstmt.setLong(++i, this.getTotalSupply());
            pstmt.setInt(++i, this.getIssuanceHeight());
            pstmt.setLong(++i, this.getMinReservePerUnitNQT());
            pstmt.setByte(++i, this.getMinDifficulty());
            pstmt.setByte(++i, this.getMaxDifficulty());
            pstmt.setByte(++i, this.getRuleset());
            pstmt.setLong(++i, this.getCurrentSupply());
            pstmt.setLong(++i, this.getCurrentReservePerUnitNQT());
            pstmt.executeUpdate();
        }
    }

    public Long getCurrencyId() {
        return currencyId;
    }

    public Long getAccountId() {
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

    public byte getType() {
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

    public long getCurrentSupply() {
        return currentSupply;
    }

    public long getCurrentReservePerUnitNQT() {
        return currentReservePerUnitNQT;
    }

    public static boolean isIssued(Long currencyId) {
        Currency currency = currencies.get(currencyId);
        return currency != null && currency.getIssuanceHeight() <= BlockchainImpl.getInstance().getLastBlock().getHeight();
    }

    public static void increaseReserve(Account account, Long currencyId, long amountNQT) {
        Currency currency = Currency.getCurrency(currencyId);
        account.addToBalanceNQT(-Convert.safeMultiply(currency.getTotalSupply(), amountNQT));
        currency.currentReservePerUnitNQT += amountNQT;

        nonIssuedCurrencies.get(currencyId).addFounder(account.getId(), Convert.safeMultiply(currency.getTotalSupply(), amountNQT));
    }

    public static void claimReserve(Account account, Long currencyId, long units) {
        account.addToCurrencyBalanceQNT(currencyId, -units);
        Currency currency = Currency.getCurrency(currencyId);
        currency.totalSupply -= units;
        account.addToBalanceAndUnconfirmedBalanceNQT(Convert.safeMultiply(units, currency.currentReservePerUnitNQT));
    }

    public static void transferMoney(Account account, List<Attachment.MonetarySystemMoneyTransfer.Entry> entries) {
        for (Attachment.MonetarySystemMoneyTransfer.Entry entry : entries) {
            account.addToCurrencyBalanceQNT(entry.getCurrencyId(), -entry.getUnits());
            Account.addOrGetAccount(entry.getRecipientId()).addToCurrencyAndUnconfirmedCurrencyBalanceQNT(entry.getCurrencyId(), entry.getUnits());
        }
    }

    public void increaseSupply(int units) {
        currentSupply += units;
    }

}
