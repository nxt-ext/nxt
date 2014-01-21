package nxt.http;

import nxt.Account;
import nxt.Block;
import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.PriorityQueue;

final class GetAccountBlockIds extends HttpRequestHandler {

    static final GetAccountBlockIds instance = new GetAccountBlockIds();

    private GetAccountBlockIds() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String account = req.getParameter("account");
        String timestampValue = req.getParameter("timestamp");
        if (account == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"account\" not specified");

        } else if (timestampValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"timestamp\" not specified");

        } else {

            try {

                Account accountData = Nxt.accounts.get(Convert.parseUnsignedLong(account));
                if (accountData == null) {

                    response.put("errorCode", 5);
                    response.put("errorDescription", "Unknown account");

                } else {

                    try {

                        int timestamp = Integer.parseInt(timestampValue);
                        if (timestamp < 0) {

                            throw new Exception();

                        }

                        PriorityQueue<Block> sortedBlocks = new PriorityQueue<>(11, Block.heightComparator);
                        byte[] accountPublicKey = accountData.publicKey.get();
                        for (Block block : Nxt.blocks.values()) {

                            if (block.timestamp >= timestamp && Arrays.equals(block.generatorPublicKey, accountPublicKey)) {

                                sortedBlocks.offer(block);

                            }

                        }
                        JSONArray blockIds = new JSONArray();
                        while (! sortedBlocks.isEmpty()) {
                            blockIds.add(sortedBlocks.poll().getStringId());
                        }
                        response.put("blockIds", blockIds);

                    } catch (Exception e) {

                        response.put("errorCode", 4);
                        response.put("errorDescription", "Incorrect \"timestamp\"");

                    }

                }

            } catch (Exception e) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"account\"");

            }

        }
        return response;
    }

}
