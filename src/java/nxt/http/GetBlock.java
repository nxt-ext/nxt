package nxt.http;

import nxt.Block;
import nxt.Blockchain;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_BLOCK;
import static nxt.http.JSONResponses.MISSING_BLOCK;
import static nxt.http.JSONResponses.UNKNOWN_BLOCK;

final class GetBlock extends HttpRequestHandler {

    static final GetBlock instance = new GetBlock();

    private GetBlock() {}

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        String block = req.getParameter("block");
        if (block == null) {
            return MISSING_BLOCK;
        }

        Block blockData;
        try {
            blockData = Blockchain.getBlock(Convert.parseUnsignedLong(block));
            if (blockData == null) {
                return UNKNOWN_BLOCK;
            }
        } catch (RuntimeException e) {
            return INCORRECT_BLOCK;
        }

        JSONObject response = new JSONObject();
        response.put("height", blockData.getHeight());
        response.put("generator", Convert.convert(blockData.getGeneratorAccountId()));
        response.put("timestamp", blockData.timestamp);
        response.put("numberOfTransactions", blockData.transactions.length);
        response.put("totalAmount", blockData.totalAmount);
        response.put("totalFee", blockData.totalFee);
        response.put("payloadLength", blockData.payloadLength);
        response.put("version", blockData.version);
        response.put("baseTarget", Convert.convert(blockData.getBaseTarget()));
        if (blockData.previousBlock != null) {
            response.put("previousBlock", Convert.convert(blockData.previousBlock));
        }
        if (blockData.getNextBlock() != null) {
            response.put("nextBlock", Convert.convert(blockData.getNextBlock()));
        }
        response.put("payloadHash", Convert.convert(blockData.getPayloadHash()));
        response.put("generationSignature", Convert.convert(blockData.getGenerationSignature()));
        if (blockData.version > 1) {
            response.put("previousBlockHash", Convert.convert(blockData.previousBlockHash));
        }
        response.put("blockSignature", Convert.convert(blockData.getBlockSignature()));
        JSONArray transactions = new JSONArray();
        for (Long transactionId : blockData.transactions) {
            transactions.add(Convert.convert(transactionId));
        }
        response.put("transactions", transactions);

        return response;
    }

}