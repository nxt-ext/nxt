package nxt;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class Order {

    // called only from Blockchain.apply(Block) which is already synchronized on Blockchain.class
    private static void matchOrders(Long assetId) {

        SortedSet<Ask> sortedAssetAskOrders = Ask.getSortedOrders(assetId);
        SortedSet<Bid> sortedAssetBidOrders = Bid.getSortedOrders(assetId);

        while (!sortedAssetAskOrders.isEmpty() && !sortedAssetBidOrders.isEmpty()) {

            Ask askOrder = sortedAssetAskOrders.first();
            Bid bidOrder = sortedAssetBidOrders.first();

            if (askOrder.price > bidOrder.price) {

                break;

            }

            int quantity = ((Order)askOrder).quantity < ((Order)bidOrder).quantity ? ((Order)askOrder).quantity : ((Order)bidOrder).quantity;
            long price = askOrder.height < bidOrder.height || (askOrder.height == bidOrder.height && askOrder.id < bidOrder.id) ? askOrder.price : bidOrder.price;

            if ((((Order)askOrder).quantity -= quantity) == 0) {

                Ask.removeOrder(askOrder.id);

            }

            askOrder.account.addToBalanceAndUnconfirmedBalance(quantity * price);

            if ((((Order)bidOrder).quantity -= quantity) == 0) {

                Bid.removeOrder(bidOrder.id);

            }

            bidOrder.account.addToAssetAndUnconfirmedAssetBalance(assetId, quantity);

        }

    }

    public final Long id;
    public final Account account;
    public final Long asset;
    public final long price;

    final long height;

    private volatile int quantity;

    private Order(Long id, Account account, Long assetId, int quantity, long price) {
        this.id = id;
        this.account = account;
        this.asset = assetId;
        this.quantity = quantity;
        this.price = price;
        this.height = Blockchain.getLastBlock().getHeight();
    }

    public final int getQuantity() {
        return quantity;
    }

    private int compareTo(Order o) {

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

    public static final class Ask extends Order implements Comparable<Ask> {

        private static final ConcurrentMap<Long, Ask> askOrders = new ConcurrentHashMap<>();
        private static final ConcurrentMap<Long, SortedSet<Ask>> sortedAskOrders = new ConcurrentHashMap<>();

        public static final Collection<Ask> allAskOrders = Collections.unmodifiableCollection(askOrders.values());

        public static Ask getAskOrder(Long orderId) {
            return askOrders.get(orderId);
        }

        public static SortedSet<Ask> getSortedOrders(Long assetId) {
            return sortedAskOrders.get(assetId);
        }

        static void addOrder(Long transactionId, Account senderAccount, Long assetId, int quantity, long price) {
            Ask order = new Ask(transactionId, senderAccount, assetId, quantity, price);
            askOrders.put(order.id, order);
            SortedSet<Ask> sortedAssetAskOrders = sortedAskOrders.get(assetId);
            if (sortedAssetAskOrders == null) {
                sortedAssetAskOrders = new TreeSet<>();
                sortedAskOrders.put(assetId,sortedAssetAskOrders);
            }
            sortedAssetAskOrders.add(order);
            matchOrders(assetId);
        }

        static Ask removeOrder(Long orderId) {
            Ask askOrder = askOrders.remove(orderId);
            sortedAskOrders.get(askOrder.asset).remove(askOrder);
            return askOrder;
        }

        private Ask(Long orderId, Account account, Long assetId, int quantity, long price) {
            super(orderId, account, assetId, quantity, price);
        }

        @Override
        public int compareTo(Ask o) {

            if (price < o.price) {

                return -1;

            } else if (price > o.price) {

                return 1;

            } else {

                return super.compareTo(o);

            }

        }
    }

    public static final class Bid extends Order implements Comparable<Bid> {

        private static final ConcurrentMap<Long, Bid> bidOrders = new ConcurrentHashMap<>();
        private static final ConcurrentMap<Long, SortedSet<Bid>> sortedBidOrders = new ConcurrentHashMap<>();

        public static final Collection<Bid> allBidOrders = Collections.unmodifiableCollection(bidOrders.values());

        public static Bid getBidOrder(Long orderId) {
            return bidOrders.get(orderId);
        }

        public static SortedSet<Bid> getSortedOrders(Long assetId) {
            return sortedBidOrders.get(assetId);
        }

        static void addOrder(Long transactionId, Account senderAccount, Long assetId, int quantity, long price) {
            Bid order = new Bid(transactionId, senderAccount, assetId, quantity, price);
            senderAccount.addToBalanceAndUnconfirmedBalance(- quantity * price);
            bidOrders.put(order.id, order);
            SortedSet<Bid> sortedAssetBidOrders = sortedBidOrders.get(assetId);
            if (sortedAssetBidOrders == null) {
                sortedAssetBidOrders = new TreeSet<>();
                sortedBidOrders.put(assetId,sortedAssetBidOrders);
            }
            matchOrders(assetId);
        }

        static Bid removeOrder(Long orderId) {
            Bid bidOrder = bidOrders.remove(orderId);
            sortedBidOrders.get(bidOrder.asset).remove(bidOrder);
            return bidOrder;
        }

        private Bid(Long orderId, Account account, Long assetId, int quantity, long price) {
            super(orderId, account, assetId, quantity, price);
        }

        @Override
        public int compareTo(Bid o) {

            if (price > o.price) {

                return -1;

            } else if (price < o.price) {

                return 1;

            } else {

                return super.compareTo(o);

            }

        }

    }
}
