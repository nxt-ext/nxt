package nxt.http;

import nxt.Block;
import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetBlock extends HttpRequestHandler {

    static final GetBlock instance = new GetBlock();

    private GetBlock() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String block = req.getParameter("block");
        if (block == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"block\" not specified");

        } else {

            try {

                Block blockData = Nxt.blocks.get(Convert.parseUnsignedLong(block));
                if (blockData == null) {

                    response.put("errorCode", 5);
                    response.put("errorDescription", "Unknown block");

                } else {

                    response.put("height", blockData.height);
                    response.put("generator", Convert.convert(blockData.getGeneratorAccountId()));
                    response.put("timestamp", blockData.timestamp);
                    response.put("numberOfTransactions", blockData.transactions.length);
                    response.put("totalAmount", blockData.totalAmount);
                    response.put("totalFee", blockData.totalFee);
                    response.put("payloadLength", blockData.payloadLength);
                    response.put("version", blockData.version);
                    response.put("baseTarget", Convert.convert(blockData.baseTarget));
                    if (blockData.previousBlock != 0) {

                        response.put("previousBlock", Convert.convert(blockData.previousBlock));

                    }
                    if (blockData.nextBlock != 0) {

                        response.put("nextBlock", Convert.convert(blockData.nextBlock));

                    }
                    response.put("payloadHash", Convert.convert(blockData.payloadHash));
                    response.put("generationSignature", Convert.convert(blockData.generationSignature));
                    if (blockData.version > 1) {

                        response.put("previousBlockHash", Convert.convert(blockData.previousBlockHash));

                    }
                    response.put("blockSignature", Convert.convert(blockData.blockSignature));
                    JSONArray transactions = new JSONArray();
                    for (long transactionId : blockData.transactions) {

                        transactions.add(Convert.convert(transactionId));

                    }
                    response.put("transactions", transactions);

                }

            } catch (Exception e) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"block\"");

            }

        }
        return response;
    }

}
