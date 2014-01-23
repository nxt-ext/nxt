package nxt;

public final class BidOrder extends Order implements Comparable<BidOrder> {

    BidOrder(long id, Account account, long asset, int quantity, long price) {
        super(id, account, asset, quantity, price);
    }

    @Override
    public int compareTo(BidOrder o) {

        if (price > o.price) {

            return -1;

        } else if (price < o.price) {

            return 1;

        } else {

            return super.compareTo(o);

        }

    }

}
