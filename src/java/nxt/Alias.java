package nxt;

public final class Alias {

    public final Account account;
    public final long id;
    public final String alias;
    public volatile String uri;
    public volatile int timestamp;

    Alias(Account account, long id, String alias, String uri, int timestamp) {

        this.account = account;
        this.id = id;
        this.alias = alias;
        this.uri = uri;
        this.timestamp = timestamp;

    }

}
