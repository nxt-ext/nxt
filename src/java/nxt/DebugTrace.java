package nxt;

import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Logger;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
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
        Logger.logDebugMessage("Debug tracing of " + (accountIds.contains("*") ? "ALL"
                : String.valueOf(accountIds.size())) + " balances enabled");
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
        Account.addListener(new Listener<Account>() {
            @Override
            public void notify(Account account) {
                debugTrace.traceAssets(account);
            }
        }, Account.Event.ASSET_BALANCE);
        Account.addListener(new Listener<Account>() {
            @Override
            public void notify(Account account) {
                debugTrace.traceAssets(account);
            }
        }, Account.Event.UNCONFIRMED_ASSET_BALANCE);
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

    private static final String[] columns = {"account", "asset", "balance", "unconfirmed balance",
            "asset balance", "unconfirmed asset balance",
            "transaction amount", "transaction fee", "generation fee",
            "order", "order price", "order quantity", "order cost",
            "trade price", "trade quantity", "trade cost",
            "asset quantity", "event",
            "transaction", "timestamp"};

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

    private void traceAssets(Account account) {
        if (!include(account.getId())) {
            return;
        }
        for (Map.Entry<Long,Integer> mapEntry : account.getAssetBalances().entrySet()) {
            log(getAssetValues(account.getId(), mapEntry.getKey(), mapEntry.getValue(), false));
        }
        for (Map.Entry<Long,Integer> mapEntry : account.getUnconfirmedAssetBalances().entrySet()) {
            log(getAssetValues(account.getId(), mapEntry.getKey(), mapEntry.getValue(), true));
        }
    }

    private void trace(Block block, boolean isUndo) {
        Long generatorId = block.getGeneratorId();
        if (include(generatorId)) {
            log(getValues(generatorId, block, isUndo));
        }
        for (Transaction transaction : block.getTransactions()) {
            Long senderId = transaction.getSenderId();
            if (include(senderId)) {
                log(getValues(senderId, transaction, false, isUndo));
                log(getValues(senderId, transaction, transaction.getAttachment(), false, isUndo));
            }
            Long recipientId = transaction.getRecipientId();
            if (include(recipientId)) {
                log(getValues(recipientId, transaction, true, isUndo));
                log(getValues(recipientId, transaction, transaction.getAttachment(), true, isUndo));
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
        map.put("event", "balance");
        return map;
    }

    private Map<String,String> getValues(Long accountId, Trade trade, boolean isAsk) {
        Map<String,String> map = getValues(accountId);
        map.put("asset", Convert.toUnsignedLong(trade.getAssetId()));
        map.put("trade quantity", String.valueOf(isAsk ? - trade.getQuantity() : trade.getQuantity()));
        map.put("trade price", String.valueOf(trade.getPrice()));
        map.put("trade cost", String.valueOf((isAsk ? trade.getQuantity() : -trade.getQuantity()) * trade.getPrice()));
        map.put("event", "trade");
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
        map.put("event", "transaction" + (isUndo ? " undo" : ""));
        return map;
    }

    private Map<String,String> getValues(Long accountId, Block block, boolean isUndo) {
        Map<String,String> map = getValues(accountId);
        long fee = block.getTotalFee();
        if (isUndo) {
            fee = - fee;
        }
        map.put("generation fee", String.valueOf(fee * 100L));
        map.put("event", "block" + (isUndo ? " undo" : ""));
        return map;
    }

    private Map<String,String> getAssetValues(Long accountId, Long assetId, Integer quantity, boolean unconfirmed) {
        Map<String,String> map = new HashMap<>();
        map.put("account", Convert.toUnsignedLong(accountId));
        map.put("asset", Convert.toUnsignedLong(assetId));
        map.put(unconfirmed ? "unconfirmed asset balance" : "asset balance", String.valueOf(quantity == null ? 0 : quantity));
        map.put("timestamp", String.valueOf(Nxt.getBlockchain().getLastBlock().getTimestamp()));
        map.put("event", "asset balance");
        return map;
    }

    private Map<String,String> getValues(Long accountId, Transaction transaction, Attachment attachment, boolean isRecipient, boolean isUndo) {
        Map<String,String> map = getValues(accountId);
        if (attachment instanceof Attachment.ColoredCoinsOrderPlacement) {
            if (isRecipient) {
                return Collections.emptyMap();
            }
            Attachment.ColoredCoinsOrderPlacement orderPlacement = (Attachment.ColoredCoinsOrderPlacement)attachment;
            boolean isAsk = orderPlacement instanceof Attachment.ColoredCoinsAskOrderPlacement;
            map.put("asset", Convert.toUnsignedLong(orderPlacement.getAssetId()));
            map.put("order", transaction.getStringId());
            map.put("order price", String.valueOf(orderPlacement.getPrice()));
            long quantity = orderPlacement.getQuantity();
            if (isAsk) {
                if (! isUndo) {
                    quantity = - quantity;
                }
            } else {
                if (isUndo) {
                    quantity = - quantity;
                }
            }
            map.put("order quantity", String.valueOf(quantity));
            long orderCost = orderPlacement.getPrice() * orderPlacement.getQuantity();
            if (isAsk) {
                if (isUndo) {
                    orderCost = - orderCost;
                }
            } else {
                if (! isUndo) {
                    orderCost = - orderCost;
                }
            }
            map.put("order cost", String.valueOf(orderCost));
            String event = (isAsk ? "ask" : "bid") + " order" + (isUndo ? " undo" : "");
            map.put("event", event);
        } else if (attachment instanceof Attachment.ColoredCoinsAssetIssuance) {
            if (isRecipient) {
                return Collections.emptyMap();
            }
            Attachment.ColoredCoinsAssetIssuance assetIssuance = (Attachment.ColoredCoinsAssetIssuance)attachment;
            map.put("asset", transaction.getStringId());
            map.put("asset quantity", String.valueOf(isUndo ? -assetIssuance.getQuantity() : assetIssuance.getQuantity()));
            map.put("event", "asset issuance" + (isUndo ? " undo" : ""));
        } else if (attachment instanceof Attachment.ColoredCoinsAssetTransfer) {
            Attachment.ColoredCoinsAssetTransfer assetTransfer = (Attachment.ColoredCoinsAssetTransfer)attachment;
            map.put("asset", Convert.toUnsignedLong(assetTransfer.getAssetId()));
            int quantity = assetTransfer.getQuantity();
            if (isRecipient) {
                if (isUndo) {
                    quantity = - quantity;
                }
            } else {
                if (! isUndo) {
                    quantity = - quantity;
                }
            }
            map.put("asset quantity", String.valueOf(quantity));
            map.put("event", "asset transfer" + (isUndo ? " undo" : ""));
        } else if (attachment instanceof Attachment.ColoredCoinsOrderCancellation) {
            Attachment.ColoredCoinsOrderCancellation orderCancellation = (Attachment.ColoredCoinsOrderCancellation)attachment;
            map.put("order", Convert.toUnsignedLong(orderCancellation.getOrderId()));
            map.put("event", "order cancel");
        }
        return map;
    }

    private void log(Map<String,String> map) {
        if (map.isEmpty()) {
            return;
        }
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
