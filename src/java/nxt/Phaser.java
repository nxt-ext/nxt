package nxt;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Phaser {

    private static final Set<Long> phasedTransactionIds = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

    public static void processTransaction(TransactionImpl transaction) {
        transaction.apply();
    }

}
