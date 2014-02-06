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
        response.put("timestamp", blockData.getTimestamp());
        response.put("numberOfTransactions", blockData.getTransactionIds().length);
        response.put("totalAmount", blockData.getTotalAmount());
        response.put("totalFee", blockData.getTotalFee());
        response.put("payloadLength", blockData.getPayloadLength());
        response.put("version", blockData.getVersion());
        response.put("baseTarget", Convert.convert(blockData.getBaseTarget()));
        if (blockData.getPreviousBlockId() != null) {
            response.put("previousBlock", Convert.convert(blockData.getPreviousBlockId()));
        }
        if (blockData.getNextBlockId() != null) {
            response.put("nextBlock", Convert.convert(blockData.getNextBlockId()));
        }
        response.put("payloadHash", Convert.convert(blockData.getPayloadHash()));
        response.put("generationSignature", Convert.convert(blockData.getGenerationSignature()));
        if (blockData.getVersion() > 1) {
            response.put("previousBlockHash", Convert.convert(blockData.getPreviousBlockHash()));
        }
        response.put("blockSignature", Convert.convert(blockData.getBlockSignature()));
        JSONArray transactions = new JSONArray();
        for (Long transactionId : blockData.getTransactionIds()) {
            transactions.add(Convert.convert(transactionId));
        }
        response.put("transactions", transactions);

        return response;
    }

}