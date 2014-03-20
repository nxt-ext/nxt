package nxt.http;

import nxt.Block;
import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_BLOCK;
import static nxt.http.JSONResponses.MISSING_BLOCK;
import static nxt.http.JSONResponses.UNKNOWN_BLOCK;

public final class GetBlock extends APIServlet.APIRequestHandler {

    static final GetBlock instance = new GetBlock();

    private GetBlock() {
        super("block");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String block = req.getParameter("block");
        if (block == null) {
            return MISSING_BLOCK;
        }

        Block blockData;
        try {
            blockData = Nxt.getBlockchain().getBlock(Convert.parseUnsignedLong(block));
            if (blockData == null) {
                return UNKNOWN_BLOCK;
            }
        } catch (RuntimeException e) {
            return INCORRECT_BLOCK;
        }

        JSONObject response = new JSONObject();
        response.put("height", blockData.getHeight());
        response.put("generator", Convert.toUnsignedLong(blockData.getGeneratorId()));
        response.put("timestamp", blockData.getTimestamp());
        response.put("numberOfTransactions", blockData.getTransactionIds().size());
        response.put("totalAmount", blockData.getTotalAmount());
        response.put("totalFee", blockData.getTotalFee());
        response.put("payloadLength", blockData.getPayloadLength());
        response.put("version", blockData.getVersion());
        response.put("baseTarget", Convert.toUnsignedLong(blockData.getBaseTarget()));
        if (blockData.getPreviousBlockId() != null) {
            response.put("previousBlock", Convert.toUnsignedLong(blockData.getPreviousBlockId()));
        }
        if (blockData.getNextBlockId() != null) {
            response.put("nextBlock", Convert.toUnsignedLong(blockData.getNextBlockId()));
        }
        response.put("payloadHash", Convert.toHexString(blockData.getPayloadHash()));
        response.put("generationSignature", Convert.toHexString(blockData.getGenerationSignature()));
        if (blockData.getVersion() > 1) {
            response.put("previousBlockHash", Convert.toHexString(blockData.getPreviousBlockHash()));
        }
        response.put("blockSignature", Convert.toHexString(blockData.getBlockSignature()));
        JSONArray transactions = new JSONArray();
        for (Long transactionId : blockData.getTransactionIds()) {
            transactions.add(Convert.toUnsignedLong(transactionId));
        }
        response.put("transactions", transactions);

        return response;
    }

}