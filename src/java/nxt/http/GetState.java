package nxt.http;

import nxt.Account;
import nxt.Alias;
import nxt.Asset;
import nxt.Blockchain;
import nxt.Nxt;
import nxt.Order;
import nxt.peer.Peer;
import nxt.user.User;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetState extends HttpRequestHandler {

    static final GetState instance = new GetState();

    private GetState() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        response.put("version", Nxt.VERSION);
        response.put("time", Convert.getEpochTime());
        response.put("lastBlock", Blockchain.getLastBlock().getStringId());
        response.put("cumulativeDifficulty", Blockchain.getLastBlock().getCumulativeDifficulty().toString());

        long totalEffectiveBalance = 0;
        for (Account account : Account.allAccounts) {

            long effectiveBalance = account.getEffectiveBalance();
            if (effectiveBalance > 0) {

                totalEffectiveBalance += effectiveBalance;

            }

        }
        response.put("totalEffectiveBalance", totalEffectiveBalance * 100L);

        response.put("numberOfBlocks", Blockchain.allBlocks.size());
        response.put("numberOfTransactions", Blockchain.allTransactions.size());
        response.put("numberOfAccounts", Account.allAccounts.size());
        response.put("numberOfAssets", Asset.allAssets.size());
        response.put("numberOfOrders", Order.Ask.allAskOrders.size() + Order.Bid.allBidOrders.size());
        response.put("numberOfAliases", Alias.allAliases.size());
        response.put("numberOfPeers", Peer.allPeers.size());
        response.put("numberOfUsers", User.allUsers.size());
        int unlockedAccounts = 0;
        for (User user : User.allUsers) {
            if (user.getSecretPhrase() != null) {
                unlockedAccounts += 1;
            }
        }
        response.put("numberOfUnlockedAccounts", unlockedAccounts);
        Peer lastBlockchainFeeder = Blockchain.getLastBlockchainFeeder();
        response.put("lastBlockchainFeeder", lastBlockchainFeeder == null ? null : lastBlockchainFeeder.getAnnouncedAddress());
        response.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        response.put("maxMemory", Runtime.getRuntime().maxMemory());
        response.put("totalMemory", Runtime.getRuntime().totalMemory());
        response.put("freeMemory", Runtime.getRuntime().freeMemory());

        return response;
    }

}
