package nxt.peer;

import nxt.Block;
import nxt.Constants;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

final class JSONData {

    static JSONObject block(Block block) {
        JSONObject json = new JSONObject();
        json.put("version", block.getVersion());
        json.put("timestamp", block.getTimestamp());
        json.put("previousBlock", Convert.toUnsignedLong(block.getPreviousBlockId()));
        if (block.getVersion() < 3) {
            json.put("totalAmount", block.getTotalAmountNQT() / Constants.ONE_NXT);
            json.put("totalFee", block.getTotalFeeNQT() / Constants.ONE_NXT);
        }
        json.put("totalAmountNQT", block.getTotalAmountNQT());
        json.put("totalFeeNQT", block.getTotalFeeNQT());
        json.put("payloadLength", block.getPayloadLength());
        json.put("payloadHash", Convert.toHexString(block.getPayloadHash()));
        json.put("generatorPublicKey", Convert.toHexString(block.getGeneratorPublicKey()));
        json.put("generationSignature", Convert.toHexString(block.getGenerationSignature()));
        if (block.getVersion() > 1) {
            json.put("previousBlockHash", Convert.toHexString(block.getPreviousBlockHash()));
        }
        json.put("blockSignature", Convert.toHexString(block.getBlockSignature()));
        JSONArray transactionsData = new JSONArray();
        for (Transaction transaction : block.getTransactions()) {
            transactionsData.add(transaction(transaction));
        }
        json.put("transactions", transactionsData);
        return json;
    }

    static JSONObject transaction(Transaction transaction) {
        JSONObject json = new JSONObject();
        json.put("type", transaction.getType().getType());
        json.put("subtype", transaction.getType().getSubtype());
        json.put("timestamp", transaction.getTimestamp());
        json.put("deadline", transaction.getDeadline());
        json.put("senderPublicKey", Convert.toHexString(transaction.getSenderPublicKey()));
        json.put("recipient", Convert.toUnsignedLong(transaction.getRecipientId()));
        json.put("amount", transaction.getAmountNQT() / Constants.ONE_NXT);
        json.put("fee", transaction.getFeeNQT() / Constants.ONE_NXT);
        json.put("amountNQT", transaction.getAmountNQT());
        json.put("feeNQT", transaction.getFeeNQT());
        json.put("referencedTransaction", Convert.toUnsignedLong(transaction.getReferencedTransactionId()));
        json.put("signature", Convert.toHexString(transaction.getSignature()));
        if (transaction.getAttachment() != null) {
            json.put("attachment", transaction.getAttachment().getJSONObject());
        }
        return json;
    }

    private JSONData() {} // never

}
