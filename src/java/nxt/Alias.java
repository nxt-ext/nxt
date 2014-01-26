package nxt;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Alias {

    private static final ConcurrentMap<String, Alias> aliases = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, Alias> aliasIdToAliasMappings = new ConcurrentHashMap<>();

    public static final Collection<Alias> allAliases = Collections.unmodifiableCollection(aliases.values());

    public static final int MAX_URI_LENGTH = 1000;
    public static final int MAX_ALIAS_LENGTH = 100;

    public static Alias getAlias(String alias) {
        return aliases.get(alias);
    }

    public static Alias getAlias(Long id) {
        return aliasIdToAliasMappings.get(id);
    }

    static void addOrUpdateAlias(Account account, Long transactionId, String alias, String uri, int timestamp) {
        String normalizedAlias = alias.toLowerCase();
        Alias newAlias = new Alias(account, transactionId, alias, uri, timestamp);
        Alias oldAlias = aliases.putIfAbsent(normalizedAlias, newAlias);
        if (oldAlias == null) {
            aliasIdToAliasMappings.putIfAbsent(transactionId, newAlias);
        } else {
            oldAlias.uri = uri;
            oldAlias.timestamp = timestamp;
        }
    }

    static void clear() {
        aliases.clear();
        aliasIdToAliasMappings.clear();
    }

    public final Account account;
    public final Long id;
    public final String alias;
    public volatile String uri;
    public volatile int timestamp;

    private Alias(Account account, Long id, String alias, String uri, int timestamp) {

        this.account = account;
        this.id = id;
        this.alias = alias;
        this.uri = uri;
        this.timestamp = timestamp;

    }

}
