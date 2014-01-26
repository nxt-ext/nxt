package nxt.http;

import nxt.Block;
import nxt.Blockchain;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetTransactionBytes extends HttpRequestHandler {

    static final GetTransactionBytes instance = new GetTransactionBytes();

    private GetTransactionBytes() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String transaction = req.getParameter("transaction");
        if (transaction == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"transaction\" not specified");

        } else {

            try {

                Long transactionId = Convert.parseUnsignedLong(transaction);
                Transaction transactionData = Blockchain.getTransaction(transactionId);
                if (transactionData == null) {

                    transactionData = Blockchain.getUnconfirmedTransaction(transactionId);
                    if (transactionData == null) {

                        response.put("errorCode", 5);
                        response.put("errorDescription", "Unknown transaction");

                    } else {

                        response.put("bytes", Convert.convert(transactionData.getBytes()));

                    }

                } else {

                    response.put("bytes", Convert.convert(transactionData.getBytes()));
                    Block block = transactionData.getBlock();
                    response.put("confirmations", Blockchain.getLastBlock().getHeight() - block.getHeight() + 1);

                }

            } catch (Exception e) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"transaction\"");

            }

        }
        return response;
    }

}
