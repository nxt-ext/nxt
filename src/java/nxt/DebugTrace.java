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

final class DebugTrace {

    static void init() {
        List<String> accountIds = Nxt.getStringListProperty("nxt.debugTraceAccounts");
        String logName = Nxt.getStringProperty("nxt.debugTraceLog");
        if (accountIds.isEmpty() || logName == null) {
            return;
        }
        PrintWriter log;
        try {
            log = new PrintWriter((new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logName)))), true);
        } catch (IOException e) {
            Logger.logDebugMessage("Debug tracing to " + logName + " not possible", e);
            throw new RuntimeException(e);
        }
        addDebugTrace(accountIds, log);
        Logger.logDebugMessage("Debug tracing of " + accountIds.size() + " balances enabled");
    }

    private static void addDebugTrace(List<String> accountIds, PrintWriter log) {
        final DebugTrace debugTrace = new DebugTrace(accountIds, log);
        final Map<String, String> headers = new HashMap<>();
        for (String entry : entries) {
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
                debugTrace.log(headers);
            }
        }, BlockchainProcessor.Event.RESCAN_BEGIN);
    }

    private static final String[] entries = {"account", "balance", "unconfirmedBalance",
            "asset", "quantity", "price", "totalPrice", "timestamp"};

    private final Set<Long> accountIds;
    private final PrintWriter log;

    private DebugTrace(List<String> accountIds, PrintWriter log) {
        this.accountIds = new HashSet<>();
        for (String accountId : accountIds) {
            this.accountIds.add(Convert.parseUnsignedLong(accountId));
        }
        this.log = log;
    }

    private void trace(Trade trade) {
        Account askAccount = Order.Ask.getAskOrder(trade.getAskOrderId()).getAccount();
        Account bidAccount = Order.Bid.getBidOrder(trade.getBidOrderId()).getAccount();
        if (accountIds.contains(askAccount.getId())) {
            log(askAccount, trade);
        } else if (accountIds.contains(bidAccount.getId())) {
            log(bidAccount, trade);
        }
    }

    private void trace(Account account) {
        if (accountIds.contains(account.getId())) {
            log(account);
        }
    }

    private void log(Account account) {
        Map<String,String> map = new HashMap<>();
        map.put("account", Convert.toUnsignedLong(account.getId()));
        map.put("balance", String.valueOf(account.getBalance()));
        map.put("unconfirmedBalance", String.valueOf(account.getUnconfirmedBalance()));
        map.put("timestamp", String.valueOf(Nxt.getBlockchain().getLastBlock().getTimestamp()));
        log(map);
    }

    private void log(Account account, Trade trade) {
        Map<String,String> map = new HashMap<>();
        map.put("account", Convert.toUnsignedLong(account.getId()));
        map.put("balance", String.valueOf(account.getBalance()));
        map.put("unconfirmedBalance", String.valueOf(account.getUnconfirmedBalance()));
        map.put("asset", Convert.toUnsignedLong(trade.getAssetId()));
        map.put("quantity", String.valueOf(trade.getQuantity()));
        map.put("price", String.valueOf(trade.getPrice()));
        map.put("totalPrice", String.valueOf(trade.getQuantity() * trade.getPrice()));
        map.put("timestamp", String.valueOf(trade.getTimestamp()));
        log(map);
    }

    private void log(Map<String,String> map) {
        StringBuilder buf = new StringBuilder();
        for (String entry : entries) {
            String value = map.get(entry);
            if (value != null) {
                buf.append(value);
            }
            buf.append('\t');
        }
        log.println(buf.toString());
    }

}
