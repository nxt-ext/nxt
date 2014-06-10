package nxt.util;

import java.util.HashMap;
import java.util.Map;

public final class Multiamount {

    private final Map<Long, Long> amounts = new HashMap<>();

    public long get(Long currencyId) {
        Long amount = amounts.get(currencyId);
        return amount == null ? 0 : amount;
    }

    public void add(Long currencyId, long amount) {
        amounts.put(currencyId, get(currencyId) + amount);
    }

    public boolean isCovered(Map<Long, Long> multibalance) {
        for (Map.Entry<Long, Long> amountEntry : amounts.entrySet()) {
            Long balance = multibalance.get(amountEntry.getKey());
            if (balance == null || balance < amountEntry.getValue()) {
                return false;
            }
        }
        return true;
    }

}
