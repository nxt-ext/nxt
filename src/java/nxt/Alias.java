package nxt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Alias {
    private static class OfferData {
        private long priceNQT;
        private long buyerId;

        OfferData(long priceNQT, long buyerId) {
            this.priceNQT = priceNQT;
            this.buyerId = buyerId;
        }

        private long getPriceNQT() {
            return priceNQT;
        }

        private long getBuyerId() {
            return buyerId;
        }
    }

    private static final ConcurrentMap<String, Alias> aliases = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, Alias> aliasIdToAliasMappings = new ConcurrentHashMap<>();
    private static final Collection<Alias> allAliases = Collections.unmodifiableCollection(aliases.values());
    private static final ConcurrentMap<Alias, OfferData> aliasesToSell = new ConcurrentHashMap<>();

    public static Collection<Alias> getAllAliases() {
        return allAliases;
    }

    public static void addSellAliasOrder(String alias, long priceNQT, long buyerId) {
        Alias als = aliases.get(alias);
        if (als != null) {
            aliasesToSell.put(als, new OfferData(priceNQT, buyerId));
        }
    }

    public static boolean matchOrders(Alias alias, long priceNQT, long buyerId) {
        OfferData od = aliasesToSell.get(alias);
        if (od == null || priceNQT < od.getPriceNQT() || (buyerId != Genesis.CREATOR_ID && buyerId < od.getBuyerId())) {
            return false;
        } else {
            aliasesToSell.remove(alias);
            return true;
        }
    }

    public static Long getPrice(Alias alias) {
        OfferData od = aliasesToSell.get(alias);
        if (od == null)
            return null;
        else
            return od.getPriceNQT();
    }

    public static Long getBuyerId(Alias alias) {
        OfferData od = aliasesToSell.get(alias);
        if (od == null)
            return null;
        else
            return od.getPriceNQT();
    }

    public static boolean aliasExists(String alias) {
        Set<String> keys = aliases.keySet();
        return keys.contains(alias);
    }

    public static Collection<Alias> getAliasesByOwner(Long accountId) {
        List<Alias> filtered = new ArrayList<>();
        for (Alias alias : Alias.getAllAliases()) {
            if (alias.getAccount().getId().equals(accountId)) {
                filtered.add(alias);
            }
        }
        return filtered;
    }

    public static Alias getAlias(String aliasName) {
        return aliases.get(aliasName);
    }

    public static Alias getAlias(Long id) {
        return aliasIdToAliasMappings.get(id);
    }

    static void addOrUpdateAlias(Account account, Long transactionId, String aliasName, String aliasURI, int timestamp) {
        String normalizedAlias = aliasName.toLowerCase();
        Alias newAlias = new Alias(account, transactionId, aliasName, aliasURI, timestamp);
        aliases.put(normalizedAlias, newAlias);
        aliasIdToAliasMappings.put(transactionId, newAlias);
    }

    static void changeOwner(Account newOwner, Long transactionId, String aliasName, int timestamp) {
        Alias oldAlias = aliases.get(aliasName);
        Alias newAlias = new Alias(newOwner, transactionId, aliasName, oldAlias.aliasURI, timestamp);

        aliases.put(aliasName, newAlias);
        aliasIdToAliasMappings.put(transactionId, newAlias);
    }

    static void clear() {
        aliases.clear();
        aliasIdToAliasMappings.clear();
    }

    private final Account account;
    private final Long id;
    private final String aliasName;
    private volatile String aliasURI;
    private volatile int timestamp;

    private Alias(Account account, Long id, String aliasName, String aliasURI, int timestamp) {
        this.account = account;
        this.id = id;
        this.aliasName = aliasName.intern();
        this.aliasURI = aliasURI.intern();
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public String getAliasName() {
        return aliasName;
    }

    public String getURI() {
        return aliasURI;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public Account getAccount() {
        return account;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alias)) return false;
        Alias alias = (Alias) o;
        return aliasName.equals(alias.aliasName);
    }

    @Override
    public int hashCode() {
        return aliasName.hashCode();
    }
}
