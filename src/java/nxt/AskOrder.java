package nxt;

class AskOrder implements Comparable<AskOrder> {

    final long id;
    final long height;
    final Account account;
    final long asset;
    // writes protected by blocksAndTransactions lock
    volatile int quantity;
    final long price;

    AskOrder(long id, Account account, long asset, int quantity, long price) {

        this.id = id;
        this.height = Nxt.lastBlock.get().height;
        this.account = account;
        this.asset = asset;
        this.quantity = quantity;
        this.price = price;

    }

    @Override
    public int compareTo(AskOrder o) {

        if (price < o.price) {

            return -1;

        } else if (price > o.price) {

            return 1;

        } else {

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

}
