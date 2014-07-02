package nxt;

import nxt.util.Convert;
import org.eclipse.jetty.util.ConcurrentHashSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Currency {

    private static final ConcurrentMap<Long, Currency> currencies = new ConcurrentHashMap<>();
    private static final Collection<Currency> allCurrencies = Collections.unmodifiableCollection(currencies.values());
    private static final Set<String> currencyNames = new ConcurrentHashSet<>();
    private static final Set<String> currencyCodes = new ConcurrentHashSet<>();

    static {
        addNXTCurrency();
    }

    public static Collection<Currency> getAllCurrencies() {
        return allCurrencies;
    }

    public static Currency getCurrency(Long id) {
        return currencies.get(id);
    }

    static void addNXTCurrency() {
        addCurrency(0L, "Nxt", "NXT", "", (byte)0, Constants.MAX_BALANCE_NQT, 0, 1, (byte)0, (byte)0, (byte)0);
    }

    static void addCurrency(Long currencyId, String name, String code, String description, byte type, long totalSupply, int issuanceHeight, long minReservePerUnitNQT, byte minDifficulty, byte maxDifficulty, byte ruleset) {
        Currency currency = new Currency(currencyId, name, code, description, type, totalSupply, issuanceHeight, minReservePerUnitNQT, minDifficulty, maxDifficulty, ruleset);
        if (Currency.currencies.putIfAbsent(currencyId, currency) != null) {
            throw new IllegalStateException("Currency with id " + Convert.toUnsignedLong(currencyId) + " already exists");
        }
        currencyNames.add(name.toLowerCase());
        currencyCodes.add(code);
    }

    static void clear() {
        currencies.clear();
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
    private final long totalSupply;
    private final int issuanceHeight;
    private final long minReservePerUnitNQT;
    private final byte minDifficulty;
    private final byte maxDifficulty;
    private final byte ruleset;

    private Currency(Long currencyId, String name, String code, String description, byte type, long totalSupply, int issuanceHeight, long minReservePerUnitNQT, byte minDifficulty, byte maxDifficulty, byte ruleset) {
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

    public static boolean isIssued(Long currencyId) {
        Currency currency = currencies.get(currencyId);
        return currency != null && currency.getIssuanceHeight() < BlockchainImpl.getInstance().getLastBlock().getHeight();
    }

}
