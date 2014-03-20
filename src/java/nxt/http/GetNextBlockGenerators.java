package nxt.http;

import nxt.Account;
import nxt.Block;
import nxt.Constants;
import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
                long thisAccountId = this.account.getId().longValue(), thatAccountId = entry.account.getId().longValue();
                if (thisAccountId < thatAccountId) {
                    return -1;
                } else if (thisAccountId > thatAccountId) {
                    return 1;
                } else {
                    return 0;
                }
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

        List<Entry> entries = new LinkedList<>();
        for (Account account : Account.getAllAccounts()) {
            long effectiveBalance = account.getEffectiveBalance();
            if (effectiveBalance > 0) {
                byte[] publicKey = account.getPublicKey();
                if (publicKey != null) {
                    entries.add(new Entry(account, account.getHitTime(account.getHit(null, curBlock), curBlock)));
                }
            }
        }
        Entry[] sortedEntries = entries.toArray(new Entry[0]);
        Arrays.sort(sortedEntries);

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
        try {
            for (int i = 0; i < limit; i++) {
                JSONObject generator = new JSONObject();
                Entry entry = sortedEntries[i];
                generator.put("account", Convert.toUnsignedLong(entry.account.getId()));
                generator.put("time", entry.time);
                generators.add(generator);
            }
        } catch (ArrayIndexOutOfBoundsException e) {}

        response.put("generators", generators);
        return response;

    }

}
