package nxt;

import nxt.db.DbIterator;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Logger;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DebugTrace {

    static final String QUOTE = Nxt.getStringProperty("nxt.debugTraceQuote", "");
    static final String SEPARATOR = Nxt.getStringProperty("nxt.debugTraceSeparator", "\t");
    static final boolean LOG_UNCONFIRMED = Nxt.getBooleanProperty("nxt.debugLogUnconfirmed");

    static void init() {
        List<String> accountIdStrings = Nxt.getStringListProperty("nxt.debugTraceAccounts");
        String logName = Nxt.getStringProperty("nxt.debugTraceLog");
        if (accountIdStrings.isEmpty() || logName == null) {
            return;
        }
        Set<Long> accountIds = new HashSet<>();
        for (String accountId : accountIdStrings) {
            if ("*".equals(accountId)) {
                accountIds.clear();
                break;
            }
            accountIds.add(Convert.parseUnsignedLong(accountId));
        }
        final DebugTrace debugTrace = addDebugTrace(accountIds, logName);
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                debugTrace.resetLog();
            }
        }, BlockchainProcessor.Event.RESCAN_BEGIN);
        Logger.logDebugMessage("Debug tracing of " + (accountIdStrings.contains("*") ? "ALL"
                : String.valueOf(accountIds.size())) + " accounts enabled");
    }

    public static DebugTrace addDebugTrace(Set<Long> accountIds, String logName) {
        final DebugTrace debugTrace = new DebugTrace(accountIds, logName);
        Trade.addListener(new Listener<Trade>() {
            @Override
            public void notify(Trade trade) {
                debugTrace.trace(trade);
            }
        }, Trade.Event.TRADE);
        Exchange.addListener(new Listener<Exchange>() {
            @Override
            public void notify(Exchange exchange) {
                debugTrace.trace(exchange);
            }
        }, Exchange.Event.EXCHANGE);
        Currency.addListener(new Listener<Currency>() {
            @Override
            public void notify(Currency currency) {
                debugTrace.crowdfunding(currency);
            }
        }, Currency.Event.BEFORE_DISTRIBUTE_CROWDFUNDING);
        Currency.addListener(new Listener<Currency>() {
            @Override
            public void notify(Currency currency) {
                debugTrace.undoCrowdfunding(currency);
            }
        }, Currency.Event.BEFORE_UNDO_CROWDFUNDING);
        Currency.addListener(new Listener<Currency>() {
            @Override
            public void notify(Currency currency) {
                debugTrace.delete(currency);
            }
        }, Currency.Event.BEFORE_DELETE);
        CurrencyMint.addListener(new Listener<CurrencyMint.Mint>() {
            @Override
            public void notify(CurrencyMint.Mint mint) {
                debugTrace.currencyMint(mint);
            }
        }, CurrencyMint.Event.CURRENCY_MINT);
        Account.addListener(new Listener<Account>() {
            @Override
            public void notify(Account account) {
                debugTrace.trace(account, false);
            }
        }, Account.Event.BALANCE);
        if (LOG_UNCONFIRMED) {
            Account.addListener(new Listener<Account>() {
                @Override
                public void notify(Account account) {
                    debugTrace.trace(account, true);
                }
            }, Account.Event.UNCONFIRMED_BALANCE);
        }
        Account.addAssetListener(new Listener<Account.AccountAsset>() {
            @Override
            public void notify(Account.AccountAsset accountAsset) {
                debugTrace.trace(accountAsset, false);
            }
        }, Account.Event.ASSET_BALANCE);
        if (LOG_UNCONFIRMED) {
            Account.addAssetListener(new Listener<Account.AccountAsset>() {
                @Override
                public void notify(Account.AccountAsset accountAsset) {
                    debugTrace.trace(accountAsset, true);
                }
            }, Account.Event.UNCONFIRMED_ASSET_BALANCE);
        }
        Account.addCurrencyListener(new Listener<Account.AccountCurrency>() {
            @Override
            public void notify(Account.AccountCurrency accountCurrency) {
                debugTrace.trace(accountCurrency, false);
            }
        }, Account.Event.CURRENCY_BALANCE);
        if (LOG_UNCONFIRMED) {
            Account.addCurrencyListener(new Listener<Account.AccountCurrency>() {
                @Override
                public void notify(Account.AccountCurrency accountCurrency) {
                    debugTrace.trace(accountCurrency, true);
                }
            }, Account.Event.UNCONFIRMED_CURRENCY_BALANCE);
        }
        Account.addLeaseListener(new Listener<Account.AccountLease>() {
            @Override
            public void notify(Account.AccountLease accountLease) {
                debugTrace.trace(accountLease, true);
            }
        }, Account.Event.LEASE_STARTED);
        Account.addLeaseListener(new Listener<Account.AccountLease>() {
            @Override
            public void notify(Account.AccountLease accountLease) {
                debugTrace.trace(accountLease, false);
            }
        }, Account.Event.LEASE_ENDED);
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                debugTrace.traceBeforeAccept(block);
            }
        }, BlockchainProcessor.Event.BEFORE_BLOCK_ACCEPT);
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                debugTrace.trace(block);
            }
        }, BlockchainProcessor.Event.BEFORE_BLOCK_APPLY);
        return debugTrace;
    }

    //NOTE: first and last columns should not have a blank entry in any row, otherwise VerifyTrace fails to parse the line
    private static final String[] columns = {"height", "event", "account", "asset", "currency", "balance", "unconfirmed balance",
            "asset balance", "unconfirmed asset balance", "currency balance", "unconfirmed currency balance",
            "transaction amount", "transaction fee", "generation fee", "effective balance", "dividend",
            "order", "order price", "order quantity", "order cost",
            "offer", "buy rate", "sell rate", "buy units", "sell units", "buy cost", "sell cost",
            "trade price", "trade quantity", "trade cost",
            "exchange rate", "exchange quantity", "exchange cost", "currency cost",
            "crowdfunding", "claim", "mint",
            "asset quantity", "currency units", "transaction", "lessee", "lessor guaranteed balance",
            "purchase", "purchase price", "purchase quantity", "purchase cost", "discount", "refund",
            "sender", "recipient", "key height", "block", "timestamp"};

    private static final Map<String,String> headers = new HashMap<>();
    static {
        for (String entry : columns) {
            headers.put(entry, entry);
        }
    }

    private final Set<Long> accountIds;
    private final String logName;
    private PrintWriter log;

    private DebugTrace(Set<Long> accountIds, String logName) {
        this.accountIds = accountIds;
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
        this.log(headers);
    }

    private boolean include(long accountId) {
        return accountId != 0 && (accountIds.isEmpty() || accountIds.contains(accountId));
    }

    // Note: Trade events occur before the change in account balances
    private void trace(Trade trade) {
        long askAccountId = Order.Ask.getAskOrder(trade.getAskOrderId()).getAccountId();
        long bidAccountId = Order.Bid.getBidOrder(trade.getBidOrderId()).getAccountId();
        if (include(askAccountId)) {
            log(getValues(askAccountId, trade, true));
        }
        if (include(bidAccountId)) {
            log(getValues(bidAccountId, trade, false));
        }
    }

    private void trace(Exchange exchange) {
        long sellerAccountId = exchange.getSellerId();
        long buyerAccountId = exchange.getBuyerId();
        if (include(sellerAccountId)) {
            log(getValues(sellerAccountId, exchange, true));
        }
        if (include(buyerAccountId)) {
            log(getValues(buyerAccountId, exchange, false));
        }
    }

    private void trace(Account account, boolean unconfirmed) {
        if (include(account.getId())) {
            log(getValues(account.getId(), unconfirmed));
        }
    }

    private void trace(Account.AccountAsset accountAsset, boolean unconfirmed) {
        if (! include(accountAsset.getAccountId())) {
            return;
        }
        log(getValues(accountAsset.getAccountId(), accountAsset, unconfirmed));
    }

    private void trace(Account.AccountCurrency accountCurrency, boolean unconfirmed) {
        if (! include(accountCurrency.getAccountId())) {
            return;
        }
        log(getValues(accountCurrency.getAccountId(), accountCurrency, unconfirmed));
    }

    private void trace(Account.AccountLease accountLease, boolean start) {
        if (! include(accountLease.lesseeId) && ! include(accountLease.lessorId)) {
            return;
        }
        log(getValues(accountLease.lessorId, accountLease, start));
    }

    private void traceBeforeAccept(Block block) {
        long generatorId = block.getGeneratorId();
        if (include(generatorId)) {
            log(getValues(generatorId, block));
        }
        for (long accountId : accountIds) {
            Account account = Account.getAccount(accountId);
            if (account != null) {
                try (DbIterator<Account> lessors = account.getLessors()) {
                    while (lessors.hasNext()) {
                        log(lessorGuaranteedBalance(lessors.next(), accountId));
                    }
                }
            }
        }
    }

    private void trace(Block block) {
        for (Transaction transaction : block.getTransactions()) {
            long senderId = transaction.getSenderId();
            if (include(senderId)) {
                log(getValues(senderId, transaction, false));
                log(getValues(senderId, transaction, transaction.getAttachment(), false));
            }
            long recipientId = transaction.getRecipientId();
            if (include(recipientId)) {
                log(getValues(recipientId, transaction, true));
                log(getValues(recipientId, transaction, transaction.getAttachment(), true));
            }
        }
    }

    private Map<String,String> lessorGuaranteedBalance(Account account, long lesseeId) {
        Map<String,String> map = new HashMap<>();
        map.put("account", Convert.toUnsignedLong(account.getId()));
        map.put("lessor guaranteed balance", String.valueOf(account.getGuaranteedBalanceNQT(1440)));
        map.put("lessee", Convert.toUnsignedLong(lesseeId));
        map.put("timestamp", String.valueOf(Nxt.getBlockchain().getLastBlock().getTimestamp()));
        map.put("height", String.valueOf(Nxt.getBlockchain().getHeight()));
        map.put("event", "lessor guaranteed balance");
        return map;
    }

    private void crowdfunding(Currency currency) {
        long totalAmountPerUnit = 0;
        long foundersTotal = 0;
        final long remainingSupply = currency.getReserveSupply() - currency.getInitialSupply();
        List<CurrencyFounder> currencyFounders = new ArrayList<>();
        try (DbIterator<CurrencyFounder> founders = CurrencyFounder.getCurrencyFounders(currency.getId(), 0, Integer.MAX_VALUE)) {
            for (CurrencyFounder founder : founders) {
                totalAmountPerUnit += founder.getAmountPerUnitNQT();
                currencyFounders.add(founder);
            }
        }
        for (CurrencyFounder founder : currencyFounders) {
            long units = Convert.safeDivide(Convert.safeMultiply(remainingSupply, founder.getAmountPerUnitNQT()), totalAmountPerUnit);
            Map<String,String> founderMap = getValues(founder.getAccountId(), false);
            founderMap.put("currency", Convert.toUnsignedLong(currency.getId()));
            founderMap.put("currency units", String.valueOf(units));
            founderMap.put("event", "distribution");
            log(founderMap);
            foundersTotal += units;
        }
        Map<String,String> map = getValues(currency.getAccountId(), false);
        map.put("currency", Convert.toUnsignedLong(currency.getId()));
        map.put("crowdfunding", String.valueOf(currency.getReserveSupply()));
        map.put("currency units", String.valueOf(remainingSupply - foundersTotal));
        if (!currency.is(CurrencyType.CLAIMABLE)) {
            map.put("currency cost", String.valueOf(Convert.safeMultiply(currency.getReserveSupply(), currency.getCurrentReservePerUnitNQT())));
        }
        map.put("event", "crowdfunding");
        log(map);
    }

    private void undoCrowdfunding(Currency currency) {
        try (DbIterator<CurrencyFounder> founders = CurrencyFounder.getCurrencyFounders(currency.getId(), 0, Integer.MAX_VALUE)) {
            for (CurrencyFounder founder : founders) {
                Map<String,String> founderMap = getValues(founder.getAccountId(), false);
                founderMap.put("currency", Convert.toUnsignedLong(currency.getId()));
                founderMap.put("currency cost", String.valueOf(Convert.safeMultiply(currency.getReserveSupply(), founder.getAmountPerUnitNQT())));
                founderMap.put("event", "undo distribution");
                log(founderMap);
            }
        }
        Map<String,String> map = getValues(currency.getAccountId(), false);
        map.put("currency", Convert.toUnsignedLong(currency.getId()));
        map.put("currency units", String.valueOf(-currency.getInitialSupply()));
        map.put("event", "undo crowdfunding");
        log(map);
    }

    private void delete(Currency currency) {
        long accountId = 0;
        long units = 0;
        if (!currency.isActive()) {
            accountId = currency.getAccountId();
            units = currency.getCurrentSupply();
        } else {
            try (DbIterator<Account.AccountCurrency> accountCurrencies = Account.getCurrencyAccounts(currency.getId(), 0, -1)) {
                if (accountCurrencies.hasNext()) {
                    Account.AccountCurrency accountCurrency = accountCurrencies.next();
                    accountId = accountCurrency.getAccountId();
                    units = accountCurrency.getUnits();
                }
            }
        }
        if (accountId == 0 || units == 0) {
            return;
        }
        Map<String,String> map = getValues(accountId, false);
        map.put("currency", Convert.toUnsignedLong(currency.getId()));
        if (currency.is(CurrencyType.RESERVABLE)) {
            if (currency.is(CurrencyType.CLAIMABLE) && currency.isActive()) {
                map.put("currency cost", String.valueOf(Convert.safeMultiply(units, currency.getCurrentReservePerUnitNQT())));
            }
            if (!currency.isActive()) {
                try (DbIterator<CurrencyFounder> founders = CurrencyFounder.getCurrencyFounders(currency.getId(), 0, Integer.MAX_VALUE)) {
                    for (CurrencyFounder founder : founders) {
                        Map<String,String> founderMap = getValues(founder.getAccountId(), false);
                        founderMap.put("currency", Convert.toUnsignedLong(currency.getId()));
                        founderMap.put("currency cost", String.valueOf(Convert.safeMultiply(currency.getReserveSupply(), founder.getAmountPerUnitNQT())));
                        founderMap.put("event", "undo distribution");
                        log(founderMap);
                    }
                }
            }
        }
        map.put("currency units", String.valueOf(-units));
        map.put("event", "currency delete");
        log(map);
    }

    private void currencyMint(CurrencyMint.Mint mint) {
        if (!include(mint.accountId)) {
            return;
        }
        Map<String, String> map = getValues(mint.accountId, false);
        map.put("currency", Convert.toUnsignedLong(mint.currencyId));
        map.put("currency units", String.valueOf(mint.units));
        map.put("event", "currency mint");
        log(map);
    }

    private Map<String,String> getValues(long accountId, boolean unconfirmed) {
        Map<String,String> map = new HashMap<>();
        map.put("account", Convert.toUnsignedLong(accountId));
        Account account = Account.getAccount(accountId);
        map.put("balance", String.valueOf(account != null ? account.getBalanceNQT() : 0));
        map.put("unconfirmed balance", String.valueOf(account != null ? account.getUnconfirmedBalanceNQT() : 0));
        map.put("timestamp", String.valueOf(Nxt.getBlockchain().getLastBlock().getTimestamp()));
        map.put("height", String.valueOf(Nxt.getBlockchain().getHeight()));
        map.put("event", unconfirmed ? "unconfirmed balance" : "balance");
        map.put("key height", String.valueOf(account != null ? account.getKeyHeight() : 0));
        map.put("effective balance", String.valueOf(account != null ? account.getEffectiveBalanceNXT() : 0));
        return map;
    }

    private Map<String,String> getValues(long accountId, Trade trade, boolean isAsk) {
        Map<String,String> map = getValues(accountId, false);
        map.put("asset", Convert.toUnsignedLong(trade.getAssetId()));
        map.put("trade quantity", String.valueOf(isAsk ? - trade.getQuantityQNT() : trade.getQuantityQNT()));
        map.put("trade price", String.valueOf(trade.getPriceNQT()));
        long tradeCost = Convert.safeMultiply(trade.getQuantityQNT(), trade.getPriceNQT());
        map.put("trade cost", String.valueOf((isAsk ? tradeCost : - tradeCost)));
        map.put("event", "trade");
        return map;
    }

    private Map<String,String> getValues(long accountId, Exchange exchange, boolean isSell) {
        Map<String,String> map = getValues(accountId, false);
        map.put("currency", Convert.toUnsignedLong(exchange.getCurrencyId()));
        map.put("exchange quantity", String.valueOf(isSell ? -exchange.getUnits() : exchange.getUnits()));
        map.put("exchange rate", String.valueOf(exchange.getRate()));
        long exchangeCost = Convert.safeMultiply(exchange.getUnits(), exchange.getRate());
        map.put("exchange cost", String.valueOf((isSell ? exchangeCost : - exchangeCost)));
        map.put("event", "exchange");
        return map;
    }

    private Map<String,String> getValues(long accountId, Transaction transaction, boolean isRecipient) {
        long amount = transaction.getAmountNQT();
        long fee = transaction.getFeeNQT();
        if (isRecipient) {
            fee = 0; // fee doesn't affect recipient account
        } else {
            // for sender the amounts are subtracted
            amount = - amount;
            fee = - fee;
        }
        if (fee == 0 && amount == 0) {
            return Collections.emptyMap();
        }
        Map<String,String> map = getValues(accountId, false);
        map.put("transaction amount", String.valueOf(amount));
        map.put("transaction fee", String.valueOf(fee));
        map.put("transaction", transaction.getStringId());
        if (isRecipient) {
            map.put("sender", Convert.toUnsignedLong(transaction.getSenderId()));
        } else {
            map.put("recipient", Convert.toUnsignedLong(transaction.getRecipientId()));
        }
        map.put("event", "transaction");
        return map;
    }

    private Map<String,String> getValues(long accountId, Block block) {
        long fee = block.getTotalFeeNQT();
        if (fee == 0) {
            return Collections.emptyMap();
        }
        Map<String,String> map = getValues(accountId, false);
        map.put("generation fee", String.valueOf(fee));
        map.put("block", block.getStringId());
        map.put("event", "block");
        map.put("timestamp", String.valueOf(block.getTimestamp()));
        map.put("height", String.valueOf(block.getHeight()));
        return map;
    }

    private Map<String,String> getValues(long accountId, Account.AccountAsset accountAsset, boolean unconfirmed) {
        Map<String,String> map = new HashMap<>();
        map.put("account", Convert.toUnsignedLong(accountId));
        map.put("asset", Convert.toUnsignedLong(accountAsset.getAssetId()));
        if (unconfirmed) {
            map.put("unconfirmed asset balance", String.valueOf(accountAsset.getUnconfirmedQuantityQNT()));
        } else {
            map.put("asset balance", String.valueOf(accountAsset.getQuantityQNT()));
        }
        map.put("timestamp", String.valueOf(Nxt.getBlockchain().getLastBlock().getTimestamp()));
        map.put("height", String.valueOf(Nxt.getBlockchain().getHeight()));
        map.put("event", "asset balance");
        return map;
    }

    private Map<String,String> getValues(long accountId, Account.AccountCurrency accountCurrency, boolean unconfirmed) {
        Map<String,String> map = new HashMap<>();
        map.put("account", Convert.toUnsignedLong(accountId));
        map.put("currency", Convert.toUnsignedLong(accountCurrency.getCurrencyId()));
        if (unconfirmed) {
            map.put("unconfirmed currency balance", String.valueOf(accountCurrency.getUnconfirmedUnits()));
        } else {
            map.put("currency balance", String.valueOf(accountCurrency.getUnits()));
        }
        map.put("timestamp", String.valueOf(Nxt.getBlockchain().getLastBlock().getTimestamp()));
        map.put("height", String.valueOf(Nxt.getBlockchain().getHeight()));
        map.put("event", "currency balance");
        return map;
    }

    private Map<String,String> getValues(long accountId, Account.AccountLease accountLease, boolean start) {
        Map<String,String> map = new HashMap<>();
        map.put("account", Convert.toUnsignedLong(accountId));
        map.put("event", start ? "lease begin" : "lease end");
        map.put("timestamp", String.valueOf(Nxt.getBlockchain().getLastBlock().getTimestamp()));
        map.put("height", String.valueOf(Nxt.getBlockchain().getHeight()));
        map.put("lessee", Convert.toUnsignedLong(accountLease.lesseeId));
        return map;
    }

    private Map<String,String> getValues(long accountId, Transaction transaction, Attachment attachment, boolean isRecipient) {
        Map<String,String> map = getValues(accountId, false);
        if (attachment instanceof Attachment.ColoredCoinsOrderPlacement) {
            if (isRecipient) {
                return Collections.emptyMap();
            }
            Attachment.ColoredCoinsOrderPlacement orderPlacement = (Attachment.ColoredCoinsOrderPlacement)attachment;
            boolean isAsk = orderPlacement instanceof Attachment.ColoredCoinsAskOrderPlacement;
            map.put("asset", Convert.toUnsignedLong(orderPlacement.getAssetId()));
            map.put("order", transaction.getStringId());
            map.put("order price", String.valueOf(orderPlacement.getPriceNQT()));
            long quantity = orderPlacement.getQuantityQNT();
            if (isAsk) {
                quantity = - quantity;
            }
            map.put("order quantity", String.valueOf(quantity));
            BigInteger orderCost = BigInteger.valueOf(orderPlacement.getPriceNQT()).multiply(BigInteger.valueOf(orderPlacement.getQuantityQNT()));
            if (! isAsk) {
                orderCost = orderCost.negate();
            }
            map.put("order cost", orderCost.toString());
            String event = (isAsk ? "ask" : "bid") + " order";
            map.put("event", event);
        } else if (attachment instanceof Attachment.ColoredCoinsAssetIssuance) {
            if (isRecipient) {
                return Collections.emptyMap();
            }
            Attachment.ColoredCoinsAssetIssuance assetIssuance = (Attachment.ColoredCoinsAssetIssuance)attachment;
            map.put("asset", transaction.getStringId());
            map.put("asset quantity", String.valueOf(assetIssuance.getQuantityQNT()));
            map.put("event", "asset issuance");
        } else if (attachment instanceof Attachment.ColoredCoinsAssetTransfer) {
            Attachment.ColoredCoinsAssetTransfer assetTransfer = (Attachment.ColoredCoinsAssetTransfer)attachment;
            map.put("asset", Convert.toUnsignedLong(assetTransfer.getAssetId()));
            long quantity = assetTransfer.getQuantityQNT();
            if (! isRecipient) {
                quantity = - quantity;
            }
            map.put("asset quantity", String.valueOf(quantity));
            map.put("event", "asset transfer");
        } else if (attachment instanceof Attachment.ColoredCoinsOrderCancellation) {
            Attachment.ColoredCoinsOrderCancellation orderCancellation = (Attachment.ColoredCoinsOrderCancellation)attachment;
            map.put("order", Convert.toUnsignedLong(orderCancellation.getOrderId()));
            map.put("event", "order cancel");
        } else if (attachment instanceof Attachment.DigitalGoodsPurchase) {
            Attachment.DigitalGoodsPurchase purchase = (Attachment.DigitalGoodsPurchase)transaction.getAttachment();
            if (isRecipient) {
                map = getValues(DigitalGoodsStore.Goods.getGoods(purchase.getGoodsId()).getSellerId(), false);
            }
            map.put("event", "purchase");
            map.put("purchase", transaction.getStringId());
        } else if (attachment instanceof Attachment.DigitalGoodsDelivery) {
            Attachment.DigitalGoodsDelivery delivery = (Attachment.DigitalGoodsDelivery)transaction.getAttachment();
            DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.Purchase.getPurchase(delivery.getPurchaseId());
            if (isRecipient) {
                map = getValues(purchase.getBuyerId(), false);
            }
            map.put("event", "delivery");
            map.put("purchase", Convert.toUnsignedLong(delivery.getPurchaseId()));
            long discount = delivery.getDiscountNQT();
            map.put("purchase price", String.valueOf(purchase.getPriceNQT()));
            map.put("purchase quantity", String.valueOf(purchase.getQuantity()));
            long cost = Convert.safeMultiply(purchase.getPriceNQT(), purchase.getQuantity());
            if (isRecipient) {
                cost = - cost;
            }
            map.put("purchase cost", String.valueOf(cost));
            if (! isRecipient) {
                discount = - discount;
            }
            map.put("discount", String.valueOf(discount));
        } else if (attachment instanceof Attachment.DigitalGoodsRefund) {
            Attachment.DigitalGoodsRefund refund = (Attachment.DigitalGoodsRefund)transaction.getAttachment();
            if (isRecipient) {
                map = getValues(DigitalGoodsStore.Purchase.getPurchase(refund.getPurchaseId()).getBuyerId(), false);
            }
            map.put("event", "refund");
            map.put("purchase", Convert.toUnsignedLong(refund.getPurchaseId()));
            long refundNQT = refund.getRefundNQT();
            if (! isRecipient) {
                refundNQT = - refundNQT;
            }
            map.put("refund", String.valueOf(refundNQT));
        } else if (attachment == Attachment.ARBITRARY_MESSAGE) {
            map = new HashMap<>();
            map.put("account", Convert.toUnsignedLong(accountId));
            map.put("timestamp", String.valueOf(Nxt.getBlockchain().getLastBlock().getTimestamp()));
            map.put("height", String.valueOf(Nxt.getBlockchain().getHeight()));
            map.put("event", attachment == Attachment.ARBITRARY_MESSAGE ? "message" : "encrypted message");
            if (isRecipient) {
                map.put("sender", Convert.toUnsignedLong(transaction.getSenderId()));
            } else {
                map.put("recipient", Convert.toUnsignedLong(transaction.getRecipientId()));
            }
        } else if (attachment instanceof Attachment.MonetarySystemPublishExchangeOffer) {
            Attachment.MonetarySystemPublishExchangeOffer publishOffer = (Attachment.MonetarySystemPublishExchangeOffer)attachment;
            map.put("currency", Convert.toUnsignedLong(publishOffer.getCurrencyId()));
            map.put("offer", transaction.getStringId());
            map.put("buy rate", String.valueOf(publishOffer.getBuyRateNQT()));
            map.put("sell rate", String.valueOf(publishOffer.getSellRateNQT()));
            long buyUnits = publishOffer.getInitialBuySupply();
            map.put("buy units", String.valueOf(buyUnits));
            long sellUnits = publishOffer.getInitialSellSupply();
            map.put("sell units", String.valueOf(sellUnits));
            BigInteger buyCost = BigInteger.valueOf(publishOffer.getBuyRateNQT()).multiply(BigInteger.valueOf(buyUnits));
            map.put("buy cost", buyCost.toString());
            BigInteger sellCost = BigInteger.valueOf(publishOffer.getSellRateNQT()).multiply(BigInteger.valueOf(sellUnits));
            map.put("sell cost", sellCost.toString());
            map.put("event", "offer");
        } else if (attachment instanceof Attachment.MonetarySystemCurrencyIssuance) {
            Attachment.MonetarySystemCurrencyIssuance currencyIssuance = (Attachment.MonetarySystemCurrencyIssuance) attachment;
            map.put("currency", transaction.getStringId());
            map.put("currency units", String.valueOf(currencyIssuance.getInitialSupply()));
            map.put("event", "currency issuance");
        } else if (attachment instanceof Attachment.MonetarySystemCurrencyTransfer) {
            Attachment.MonetarySystemCurrencyTransfer currencyTransfer = (Attachment.MonetarySystemCurrencyTransfer) attachment;
            map.put("currency", Convert.toUnsignedLong(currencyTransfer.getCurrencyId()));
            long units = currencyTransfer.getUnits();
            if (!isRecipient) {
                units = -units;
            }
            map.put("currency units", String.valueOf(units));
            map.put("event", "currency transfer");
        } else if (attachment instanceof Attachment.MonetarySystemReserveClaim) {
            Attachment.MonetarySystemReserveClaim claim = (Attachment.MonetarySystemReserveClaim) attachment;
            map.put("currency", Convert.toUnsignedLong(claim.getCurrencyId()));
            Currency currency = Currency.getCurrency(claim.getCurrencyId());
            map.put("currency units", String.valueOf(-claim.getUnits()));
            map.put("currency cost", String.valueOf(Convert.safeMultiply(claim.getUnits(), currency.getCurrentReservePerUnitNQT())));
            map.put("event", "currency claim");
        } else if (attachment instanceof Attachment.MonetarySystemReserveIncrease) {
            Attachment.MonetarySystemReserveIncrease reserveIncrease = (Attachment.MonetarySystemReserveIncrease) attachment;
            map.put("currency", Convert.toUnsignedLong(reserveIncrease.getCurrencyId()));
            Currency currency = Currency.getCurrency(reserveIncrease.getCurrencyId());
            map.put("currency cost", String.valueOf(-Convert.safeMultiply(reserveIncrease.getAmountPerUnitNQT(), currency.getReserveSupply())));
            map.put("event", "currency reserve");
        } else if (attachment instanceof Attachment.ColoredCoinsDividendPayment) {
            Attachment.ColoredCoinsDividendPayment dividendPayment = (Attachment.ColoredCoinsDividendPayment)attachment;
            long totalDividend = 0;
            String assetId = Convert.toUnsignedLong(dividendPayment.getAssetId());
            try (DbIterator<Account.AccountAsset> iterator = Account.getAssetAccounts(dividendPayment.getAssetId(), dividendPayment.getHeight(), 0, -1)) {
                while (iterator.hasNext()) {
                    Account.AccountAsset accountAsset = iterator.next();
                    if (accountAsset.getAccountId() != accountId && accountAsset.getAccountId() != Genesis.CREATOR_ID && accountAsset.getQuantityQNT() != 0) {
                        long dividend = Convert.safeMultiply(accountAsset.getQuantityQNT(), dividendPayment.getAmountNQTPerQNT());
                        Map recipient = getValues(accountAsset.getAccountId(), false);
                        recipient.put("dividend", String.valueOf(dividend));
                        recipient.put("asset", assetId);
                        recipient.put("event", "dividend");
                        totalDividend += dividend;
                        log(recipient);
                    }
                }
            }
            map.put("dividend", String.valueOf(-totalDividend));
            map.put("asset", assetId);
            map.put("event", "dividend");
        } else {
            return Collections.emptyMap();
        }
        return map;
    }

    private void log(Map<String,String> map) {
        if (map.isEmpty()) {
            return;
        }
        StringBuilder buf = new StringBuilder();
        for (String column : columns) {
            if (!LOG_UNCONFIRMED && column.startsWith("unconfirmed")) {
                continue;
            }
            String value = map.get(column);
            if (value != null) {
                buf.append(QUOTE).append(value).append(QUOTE);
            }
            buf.append(SEPARATOR);
        }
        log.println(buf.toString());
    }

}
