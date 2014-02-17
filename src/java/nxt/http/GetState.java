package nxt.http;

import nxt.*;
import nxt.peer.Peer;
import nxt.user.User;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GetState extends HttpRequestDispatcher.HttpRequestHandler {

    static final GetState instance = new GetState();

    private GetState() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        response.put("version", Nxt.VERSION);
        response.put("time", Convert.getEpochTime());
        response.put("lastBlock", Blockchain.getLastBlock().getStringId());
        response.put("cumulativeDifficulty", Blockchain.getLastBlock().getCumulativeDifficulty().toString());

        long totalEffectiveBalance = 0;
        for (Account account : Account.getAllAccounts()) {
            long effectiveBalance = account.getEffectiveBalance();
            if (effectiveBalance > 0) {
                totalEffectiveBalance += effectiveBalance;
            }
        }
        response.put("totalEffectiveBalance", totalEffectiveBalance * 100L);

        response.put("numberOfBlocks", Blockchain.getBlockCount());
        response.put("numberOfTransactions", Blockchain.getTransactionCount());
        response.put("numberOfAccounts", Account.getAllAccounts().size());
        response.put("numberOfAssets", Asset.getAllAssets().size());
        response.put("numberOfOrders", Order.Ask.getAllAskOrders().size() + Order.Bid.getAllBidOrders().size());
        int numberOfTrades = 0;
        for (CopyOnWriteArrayList<Trade> assetTrades : Trade.getTrades().values()) {
            numberOfTrades += assetTrades.size();
        }
        response.put("numberOfTrades", numberOfTrades);
        response.put("numberOfAliases", Alias.getAllAliases().size());
        response.put("numberOfPolls", Poll.getPolls().size());
        response.put("numberOfVotes", Vote.getVotes().size());
        response.put("numberOfPeers", Peer.getAllPeers().size());
        response.put("numberOfUsers", User.getAllUsers().size());
        response.put("numberOfUnlockedAccounts", Generator.getAllGenerators().size());
        Peer lastBlockchainFeeder = Blockchain.getLastBlockchainFeeder();
        response.put("lastBlockchainFeeder", lastBlockchainFeeder == null ? null : lastBlockchainFeeder.getAnnouncedAddress());
        response.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        response.put("maxMemory", Runtime.getRuntime().maxMemory());
        response.put("totalMemory", Runtime.getRuntime().totalMemory());
        response.put("freeMemory", Runtime.getRuntime().freeMemory());

        return response;
    }

}
