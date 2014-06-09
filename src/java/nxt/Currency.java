package nxt;

import nxt.util.Convert;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Currency {

    private static final ConcurrentMap<Long, Currency> currencies = new ConcurrentHashMap<>();
    private static final Collection<Currency> allCurrencies = Collections.unmodifiableCollection(currencies.values());

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
        addCurrency(0L, "Nxt", "NXT", "", (byte)0, Constants.MAX_BALANCE_NQT, 0, 1, (byte)0, (byte)0);
    }

    static void addCurrency(Long currencyId, String name, String code, String description, byte type, long totalSupply, int issuanceHeight, long minReservePerUnitNQT, byte minDifficulty, byte maxDifficulty) {
        Currency currency = new Currency(currencyId, name, code, description, type, totalSupply, issuanceHeight, minReservePerUnitNQT, minDifficulty, maxDifficulty);
        if (Currency.currencies.putIfAbsent(currencyId, currency) != null) {
            throw new IllegalStateException("Currency with id " + Convert.toUnsignedLong(currencyId) + " already exists");
        }
    }

    static void clear() {
        currencies.clear();
        addNXTCurrency();
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

    private Currency(Long currencyId, String name, String code, String description, byte type, long totalSupply, int issuanceHeight, long minReservePerUnitNQT, byte minDifficulty, byte maxDifficulty) {
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

}
