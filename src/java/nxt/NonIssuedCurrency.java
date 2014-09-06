package nxt;

import java.util.HashMap;
import java.util.Map;

/**
* Created by lyaf on 9/6/2014.
*/
public final class NonIssuedCurrency {

    private final Currency currency;
    private final Map<Long, Long> founders;

    NonIssuedCurrency(Currency currency) {
        this.currency = currency;
        this.founders = new HashMap<>();
    }

    public Currency getCurrency() {
        return currency;
    }

    public Map<Long, Long> getFounders() {
        return founders;
    }

    public void addFounder(Long accountId, Long amount) {
        Long initialAmount = founders.get(accountId);
        if (initialAmount == null) {
            founders.put(accountId, amount);
        } else {
            founders.put(accountId, initialAmount + amount);
        }
    }

}
