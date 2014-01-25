package nxt.http;

import nxt.Account;
import nxt.Blockchain;
import nxt.Nxt;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.PriorityQueue;

final class GetAccountTransactionIds extends HttpRequestHandler {

    static final GetAccountTransactionIds instance = new GetAccountTransactionIds();

    private GetAccountTransactionIds() {}

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

                        int type, subtype;
                        try {

                            type = Integer.parseInt(req.getParameter("type"));

                        } catch (Exception e) {

                            type = -1;

                        }
                        try {

                            subtype = Integer.parseInt(req.getParameter("subtype"));

                        } catch (Exception e) {

                            subtype = -1;

                        }

                        PriorityQueue<Transaction> sortedTransactions = new PriorityQueue<>(11, Transaction.timestampComparator);
                        byte[] accountPublicKey = accountData.publicKey.get();
                        for (Transaction transaction : Blockchain.allTransactions) {
                            if ((transaction.recipient == accountData.id || Arrays.equals(transaction.senderPublicKey, accountPublicKey))
                                    && (type < 0 || transaction.getType().getType() == type) && (subtype < 0 || transaction.getType().getSubtype() == subtype)
                                    && Blockchain.getBlock(transaction.block).timestamp >= timestamp) {
                                sortedTransactions.offer(transaction);
                            }
                        }
                        JSONArray transactionIds = new JSONArray();
                        while (! sortedTransactions.isEmpty()) {
                            transactionIds.add(sortedTransactions.poll().getStringId());
                        }
                        response.put("transactionIds", transactionIds);

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
