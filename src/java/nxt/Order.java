package nxt;

abstract class Order {
    public final long id;
    public final Account account;
    public final long asset;
    public final long price;
    public final long height;
    // writes protected by Blockchain lock
    public volatile int quantity;

    public Order(long id, Account account, long asset, int quantity, long price) {
        this.id = id;
        this.account = account;
        this.asset = asset;
        this.quantity = quantity;
        this.price = price;
        this.height = Blockchain.getLastBlock().height;
    }

    protected int compareTo(Order o) {

        if (height < o.height) {

            return -1;

        } else if (height > o.height) {

            return 1;

        } else {

            if (id < o.id) {

                return -1;

            } else if (id > o.id) {

                return 1;

            } else {

                return 0;

            }

        }

    }

}
