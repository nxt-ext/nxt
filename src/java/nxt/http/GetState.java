package nxt.http;

import nxt.Account;
import nxt.Alias;
import nxt.Asset;
import nxt.AssetTransfer;
import nxt.Block;
import nxt.Currency;
import nxt.CurrencyBuyOffer;
import nxt.CurrencyTransfer;
import nxt.DigitalGoodsStore;
import nxt.Exchange;
import nxt.Generator;
import nxt.Nxt;
import nxt.Order;
import nxt.Trade;
import nxt.peer.Peer;
import nxt.peer.Peers;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetState extends APIServlet.APIRequestHandler {

    static final GetState instance = new GetState();

    private GetState() {
        super(new APITag[] {APITag.INFO}, "includeCounts");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        response.put("application", Nxt.APPLICATION);
        response.put("version", Nxt.VERSION);
        response.put("time", Nxt.getEpochTime());
        Block lastBlock = Nxt.getBlockchain().getLastBlock();
        response.put("lastBlock", lastBlock.getStringId());
        response.put("cumulativeDifficulty", lastBlock.getCumulativeDifficulty().toString());
        response.put("numberOfBlocks", lastBlock.getHeight() + 1);

        /*
        long totalEffectiveBalance = 0;
        try (DbIterator<Account> accounts = Account.getAllAccounts(0, -1)) {
            for (Account account : accounts) {
                long effectiveBalanceNXT = account.getEffectiveBalanceNXT();
                if (effectiveBalanceNXT > 0) {
                    totalEffectiveBalance += effectiveBalanceNXT;
                }
            }
        }
        response.put("totalEffectiveBalanceNXT", totalEffectiveBalance);
        */

        if (!"false".equalsIgnoreCase(req.getParameter("includeCounts"))) {
            response.put("numberOfTransactions", Nxt.getBlockchain().getTransactionCount());
            response.put("numberOfAccounts", Account.getCount());
            response.put("numberOfAssets", Asset.getCount());
            int askCount = Order.Ask.getCount();
            int bidCount = Order.Bid.getCount();
            response.put("numberOfOrders", askCount + bidCount);
            response.put("numberOfAskOrders", askCount);
            response.put("numberOfBidOrders", bidCount);
            response.put("numberOfTrades", Trade.getCount());
            response.put("numberOfTransfers", AssetTransfer.getCount());
	        response.put("numberOfCurrencies", Currency.getCount());
    	    response.put("numberOfOffers", CurrencyBuyOffer.getCount());
        	response.put("numberOfExchanges", Exchange.getCount());
        	response.put("numberOfCurrencyTransfers", CurrencyTransfer.getCount());
            response.put("numberOfAliases", Alias.getCount());
            response.put("numberOfGoods", DigitalGoodsStore.Goods.getCount());
            response.put("numberOfPurchases", DigitalGoodsStore.Purchase.getCount());
            response.put("numberOfTags", DigitalGoodsStore.Tag.getCount());
            //response.put("numberOfPolls", Poll.getCount());
            //response.put("numberOfVotes", Vote.getCount());
        }
        response.put("numberOfPeers", Peers.getAllPeers().size());
        response.put("numberOfUnlockedAccounts", Generator.getAllGenerators().size());
        Peer lastBlockchainFeeder = Nxt.getBlockchainProcessor().getLastBlockchainFeeder();
        response.put("lastBlockchainFeeder", lastBlockchainFeeder == null ? null : lastBlockchainFeeder.getAnnouncedAddress());
        response.put("lastBlockchainFeederHeight", Nxt.getBlockchainProcessor().getLastBlockchainFeederHeight());
        response.put("isScanning", Nxt.getBlockchainProcessor().isScanning());
        response.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        response.put("maxMemory", Runtime.getRuntime().maxMemory());
        response.put("totalMemory", Runtime.getRuntime().totalMemory());
        response.put("freeMemory", Runtime.getRuntime().freeMemory());

        return response;
    }

}
