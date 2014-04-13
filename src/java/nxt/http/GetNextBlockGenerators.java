package nxt.http;

import nxt.Account;
import nxt.Block;
import nxt.Constants;
import nxt.Generator;
import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.PriorityQueue;

import static nxt.http.JSONResponses.INCORRECT_BLOCK;
import static nxt.http.JSONResponses.UNKNOWN_BLOCK;

public final class GetNextBlockGenerators extends APIServlet.APIRequestHandler {

    static final GetNextBlockGenerators instance = new GetNextBlockGenerators();

    private GetNextBlockGenerators() {}

    static final class Entry implements Comparable<Entry> {

        final Account account;
        final long time;

        Entry(Account account, long time) {
            this.account = account;
            this.time = time;
        }

        @Override
        public int compareTo(Entry entry) {
            if (this.time < entry.time) {
                return -1;
            } else if (this.time > entry.time) {
                return 1;
            } else {
                return this.account.getId().compareTo(entry.account.getId());
            }
        }

    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        Block curBlock;

        String block = req.getParameter("block");
        if (block == null) {
            curBlock = Nxt.getBlockchain().getLastBlock();
        } else {
            try {
                curBlock = Nxt.getBlockchain().getBlock(Convert.parseUnsignedLong(block));
                if (curBlock == null) {
                    return UNKNOWN_BLOCK;
                }
            } catch (RuntimeException e) {
                return INCORRECT_BLOCK;
            }
        }

        if (curBlock.getHeight() < Constants.TRANSPARENT_FORGING_BLOCK) {
            return JSONResponses.FEATURE_NOT_AVAILABLE;
        }

        //TODO: rewrite to use only hub terminal announced accounts
        PriorityQueue<Entry> entries = new PriorityQueue<>();
        for (Account account : Account.getAllAccounts()) {
            if (account.getEffectiveBalanceNXT() > 0 && account.getPublicKey() != null) {
                entries.add(new Entry(account, Generator.getHitTime(account, curBlock)));
            }
        }

        JSONObject response = new JSONObject();
        response.put("time", Convert.getEpochTime());
        response.put("lastBlock", Convert.toUnsignedLong(curBlock.getId()));
        JSONArray generators = new JSONArray();

        int limit;
        try {
            limit = Integer.parseInt(req.getParameter("limit"));
        } catch (RuntimeException e) {
            limit = Integer.MAX_VALUE;
        }
        Entry entry;
        while ((entry = entries.poll()) != null && generators.size() < limit) {
            JSONObject generator = new JSONObject();
            generator.put("account", Convert.toUnsignedLong(entry.account.getId()));
            generator.put("time", entry.time);
            generators.add(generator);
        }
        response.put("generators", generators);
        return response;
    }

}
