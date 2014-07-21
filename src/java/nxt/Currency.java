package nxt;

import nxt.util.Convert;
import nxt.util.Listener;
import org.eclipse.jetty.util.ConcurrentHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

    public static Currency getCurrency(Long id) {
        return currencies.get(id);
    }

    static void addNXTCurrency() {
        addCurrency(0L, "Nxt", "NXT", "", (byte)0, Constants.MAX_BALANCE_NQT, 0, 1, (byte)0, (byte)0, (byte)0, 0);
    }

    static void addCurrency(Long currencyId, String name, String code, String description, byte type, long totalSupply, int issuanceHeight, long minReservePerUnitNQT, byte minDifficulty, byte maxDifficulty, byte ruleset, long currentReservePerUnitNQT) {
        Currency currency = new Currency(currencyId, name, code, description, type, totalSupply, issuanceHeight, minReservePerUnitNQT, minDifficulty, maxDifficulty, ruleset, currentReservePerUnitNQT);
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

    private long currentReservePerUnitNQT;

    private Currency(Long currencyId, String name, String code, String description, byte type, long totalSupply, int issuanceHeight, long minReservePerUnitNQT, byte minDifficulty, byte maxDifficulty, byte ruleset, long currentReservePerUnitNQT) {
        this.currencyId = currencyId;
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

        this.currentReservePerUnitNQT = currentReservePerUnitNQT;
    }

    public Long getCurrencyId() {
        return currencyId;
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

}
