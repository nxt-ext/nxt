package nxt;

public final class Alias {

    final Account account;
    final long id;
    final String alias;
    volatile String uri;
    volatile int timestamp;

    Alias(Account account, long id, String alias, String uri, int timestamp) {

        this.account = account;
        this.id = id;
        this.alias = alias;
        this.uri = uri;
        this.timestamp = timestamp;

    }

}
