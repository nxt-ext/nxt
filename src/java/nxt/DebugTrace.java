package nxt;

import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Logger;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DebugTrace {

    static void init() {
        List<String> accountIds = Nxt.getStringListProperty("nxt.debugTraceAccounts");
        String logName = Nxt.getStringProperty("nxt.debugTraceLog");
        if (accountIds.isEmpty() || logName == null) {
            return;
        }
        addDebugTrace(accountIds, logName);
        Logger.logDebugMessage("Debug tracing of " + accountIds.size() + " balances enabled");
    }

    public static void addDebugTrace(List<String> accountIds, String logName) {
        final DebugTrace debugTrace = new DebugTrace(accountIds, logName);
        final Map<String, String> headers = new HashMap<>();
        for (String entry : columns) {
            headers.put(entry, entry);
        }
        debugTrace.log(headers);
        Trade.addListener(new Listener<Trade>() {
            @Override
            public void notify(Trade trade) {
                debugTrace.trace(trade);
            }
        }, Trade.Event.TRADE);
        Account.addListener(new Listener<Account>() {
            @Override
            public void notify(Account account) {
                debugTrace.trace(account);
            }
        }, Account.Event.BALANCE);
        Account.addListener(new Listener<Account>() {
            @Override
            public void notify(Account account) {
                debugTrace.trace(account);
            }
        }, Account.Event.UNCONFIRMED_BALANCE);
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                debugTrace.resetLog();
                debugTrace.log(headers);
            }
        }, BlockchainProcessor.Event.RESCAN_BEGIN);
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                debugTrace.trace(block, false);
            }
        }, BlockchainProcessor.Event.BEFORE_BLOCK_APPLY);
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                debugTrace.trace(block, true);
            }
        }, BlockchainProcessor.Event.BEFORE_BLOCK_UNDO);
    }

    // account must be the first column
    private static final String[] columns = {"account", "balance", "unconfirmed balance",
            "transaction amount", "transaction fee", "generation fee",
            "asset", "quantity", "price", "asset cost", "transaction", "timestamp"};

    private final Set<Long> accountIds;
    private final String logName;
    private PrintWriter log;

    private DebugTrace(List<String> accountIds, String logName) {
        this.accountIds = new HashSet<>();
        for (String accountId : accountIds) {
            if ("*".equals(accountId)) {
                this.accountIds.clear();
                break;
            }
            this.accountIds.add(Convert.parseUnsignedLong(accountId));
        }
        this.logName = logName;
        resetLog();
    }

    void resetLog() {
        if (log != null) {
            log.close();
        }
        try {
            log = new PrintWriter((new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logName)))), true);
        } catch (IOException e) {
            Logger.logDebugMessage("Debug tracing to " + logName + " not possible", e);
            throw new RuntimeException(e);
        }
    }

    private boolean include(Long accountId) {
        return accountIds.isEmpty() || accountIds.contains(accountId);
    }

    // Note: Trade events occur before the change in account balances
    private void trace(Trade trade) {
        Long askAccountId = Order.Ask.getAskOrder(trade.getAskOrderId()).getAccount().getId();
        Long bidAccountId = Order.Bid.getBidOrder(trade.getBidOrderId()).getAccount().getId();
        if (include(askAccountId)) {
            log(getValues(askAccountId, trade, true));
        }
        if (include(bidAccountId)) {
            log(getValues(bidAccountId, trade, false));
        }
    }

    private void trace(Account account) {
        if (include(account.getId())) {
            log(getValues(account.getId()));
        }
    }

    private void trace(Block block, boolean isUndo) {
        Long generatorId = block.getGeneratorId();
        if (include(generatorId)) {
            log(getValues(generatorId, block, isUndo));
        }
        for (Transaction transaction : block.getTransactions()) {
            if (include(transaction.getSenderId())) {
                log(getValues(transaction.getSenderId(), transaction, false, isUndo));
            }
            if (include(transaction.getRecipientId())) {
                log(getValues(transaction.getRecipientId(), transaction, true, isUndo));
            }
        }
    }

    private Map<String,String> getValues(Long accountId) {
        Map<String,String> map = new HashMap<>();
        map.put("account", Convert.toUnsignedLong(accountId));
        Account account = Account.getAccount(accountId);
        map.put("balance", String.valueOf(account != null ? account.getBalance() : 0));
        map.put("unconfirmed balance", String.valueOf(account != null ? account.getUnconfirmedBalance() : 0));
        map.put("timestamp", String.valueOf(Nxt.getBlockchain().getLastBlock().getTimestamp()));
        return map;
    }

    private Map<String,String> getValues(Long accountId, Trade trade, boolean isRecipient) {
        Map<String,String> map = getValues(accountId);
        map.put("asset", Convert.toUnsignedLong(trade.getAssetId()));
        map.put("quantity", String.valueOf(trade.getQuantity()));
        map.put("price", String.valueOf(trade.getPrice()));
        map.put("asset cost", String.valueOf((isRecipient ? trade.getQuantity() : -trade.getQuantity()) * trade.getPrice()));
        return map;
    }

    private Map<String,String> getValues(Long accountId, Transaction transaction, boolean isRecipient, boolean isUndo) {
        Map<String,String> map = getValues(accountId);
        long amount = transaction.getAmount();
        long fee = transaction.getFee();
        if (isRecipient) {
            fee = 0; // fee doesn't affect recipient account
            if (isUndo) {
                amount = - amount;
            }
        } else {
            // for sender the amounts are subtracted
            if (! isUndo) {
                amount = - amount;
                fee = - fee;
            }
        }
        map.put("transaction amount", String.valueOf(amount * 100L));
        map.put("transaction fee", String.valueOf(fee * 100L));
        map.put("transaction", transaction.getStringId());
        return map;
    }

    private Map<String,String> getValues(Long accountId, Block block, boolean isUndo) {
        Map<String,String> map = getValues(accountId);
        long fee = block.getTotalFee();
        if (isUndo) {
            fee = - fee;
        }
        map.put("generation fee", String.valueOf(fee * 100L));
        return map;
    }

    private void log(Map<String,String> map) {
        StringBuilder buf = new StringBuilder();
        for (String column : columns) {
            String value = map.get(column);
            if (value != null) {
                buf.append(value);
            }
            buf.append('\t');
        }
        log.println(buf.toString());
    }

}
