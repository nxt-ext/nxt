package nxt;

public class Asset {

    public final long accountId;
    public final String name;
    public final String description;
    public final int quantity;

    Asset(long accountId, String name, String description, int quantity) {

        this.accountId = accountId;
        this.name = name;
        this.description = description;
        this.quantity = quantity;

    }

}
