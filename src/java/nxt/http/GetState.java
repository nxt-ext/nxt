package nxt.http;

import nxt.Account;
import nxt.Blockchain;
import nxt.Nxt;
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
        response.put("cumulativeDifficulty", Blockchain.getLastBlock().cumulativeDifficulty.toString());

        long totalEffectiveBalance = 0;
        for (Account account : Nxt.accounts.values()) {

            long effectiveBalance = account.getEffectiveBalance();
            if (effectiveBalance > 0) {

                totalEffectiveBalance += effectiveBalance;

            }

        }
        response.put("totalEffectiveBalance", totalEffectiveBalance * 100L);

        response.put("numberOfBlocks", Blockchain.allBlocks.size());
        response.put("numberOfTransactions", Blockchain.allTransactions.size());
        response.put("numberOfAccounts", Nxt.accounts.size());
        response.put("numberOfAssets", Nxt.assets.size());
        response.put("numberOfOrders", Blockchain.askOrders.size() + Blockchain.bidOrders.size());
        response.put("numberOfAliases", Nxt.aliases.size());
        response.put("numberOfPeers", Peer.peers.size());
        response.put("numberOfUsers", User.allUsers.size());
        response.put("lastBlockchainFeeder", Nxt.lastBlockchainFeeder == null ? null : Nxt.lastBlockchainFeeder.announcedAddress);
        response.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        response.put("maxMemory", Runtime.getRuntime().maxMemory());
        response.put("totalMemory", Runtime.getRuntime().totalMemory());
        response.put("freeMemory", Runtime.getRuntime().freeMemory());

        return response;
    }

}
