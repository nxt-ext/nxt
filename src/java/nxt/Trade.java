package nxt;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Trade {

    private static final ConcurrentMap<Long, CopyOnWriteArrayList<Trade>> trades = new ConcurrentHashMap<>(); // cfb: CopyOnWriteArrayList requires a lot of resources to grow but this happens only when a new block is pushed/applied, I can't decide if we should replace it with another class

    private final Long blockId;
    private final Long askOrderId, bidOrderId;
    private final int quantity;
    private final long price;

    private Trade(Long blockId, Long askOrderId, Long bidOrderId, int quantity, long price) {

        this.blockId = blockId;
        this.askOrderId = askOrderId;
        this.bidOrderId = bidOrderId;
        this.quantity = quantity;
        this.price = price;

    }

    public static void addTrade(Long assetId, Long blockId, Long askOrderId, Long bidOrderId, int quantity, long price) {
        CopyOnWriteArrayList<Trade> assetTrades = trades.get(assetId);
        if (assetTrades == null) {
            assetTrades = new CopyOnWriteArrayList<>();
            trades.put(assetId, assetTrades);
        }
        assetTrades.add(new Trade(blockId, askOrderId, bidOrderId, quantity, price));
    }

    public static Map<Long, CopyOnWriteArrayList<Trade>> getTrades() {
        return Collections.unmodifiableMap(trades);
    }

    public static void clear() {
        trades.clear();
        // cfb: JLP, should I clear every CopyOnWriteArrayList object as well? I think I shouldn't but am not sure this won't lead to memory leaks
    }

    public Long getBlockId() { return blockId; }

    public Long getAskOrderId() { return askOrderId; }

    public Long getBidOrderId() { return bidOrderId; }

    public int getQuantity() { return quantity; }

    public long getPrice() { return price; }

    public static CopyOnWriteArrayList<Trade> getTrades(Long assetId) {
        return trades.get(assetId);
    }

}
