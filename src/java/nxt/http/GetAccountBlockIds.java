package nxt.http;

import nxt.Account;
import nxt.Block;
import nxt.Blockchain;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.PriorityQueue;

import static nxt.http.JSONResponses.INCORRECT_ACCOUNT;
import static nxt.http.JSONResponses.INCORRECT_TIMESTAMP;
import static nxt.http.JSONResponses.MISSING_ACCOUNT;
import static nxt.http.JSONResponses.MISSING_TIMESTAMP;
import static nxt.http.JSONResponses.UNKNOWN_ACCOUNT;

public final class GetAccountBlockIds extends HttpRequestHandler {

    static final GetAccountBlockIds instance = new GetAccountBlockIds();

    private GetAccountBlockIds() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String account = req.getParameter("account");
        String timestampValue = req.getParameter("timestamp");
        if (account == null) {
            return MISSING_ACCOUNT;
        } else if (timestampValue == null) {
            return MISSING_TIMESTAMP;
        }

        Account accountData;
        try {
            accountData = Account.getAccount(Convert.parseUnsignedLong(account));
            if (accountData == null) {
                return UNKNOWN_ACCOUNT;
            }
        } catch (RuntimeException e) {
            return INCORRECT_ACCOUNT;
        }

        int timestamp;
        try {
            timestamp = Integer.parseInt(timestampValue);
            if (timestamp < 0) {
                return INCORRECT_TIMESTAMP;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_TIMESTAMP;
        }

        PriorityQueue<Block> sortedBlocks = new PriorityQueue<>(11, Block.heightComparator);
        byte[] accountPublicKey = accountData.getPublicKey();
        for (Block block : Blockchain.getAllBlocks()) {
            if (block.getTimestamp() >= timestamp && Arrays.equals(block.getGeneratorPublicKey(), accountPublicKey)) {
                sortedBlocks.offer(block);
            }
        }

        JSONArray blockIds = new JSONArray();
        while (! sortedBlocks.isEmpty()) {
            blockIds.add(sortedBlocks.poll().getStringId());
        }

        JSONObject response = new JSONObject();
        response.put("blockIds", blockIds);

        return response;
    }

}
