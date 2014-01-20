package nxt;

class Asset {

    final long accountId;
    final String name;
    final String description;
    final int quantity;

    Asset(long accountId, String name, String description, int quantity) {

        this.accountId = accountId;
        this.name = name;
        this.description = description;
        this.quantity = quantity;

    }

}
