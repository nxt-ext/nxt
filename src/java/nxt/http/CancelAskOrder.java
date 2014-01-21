package nxt.http;

import nxt.Account;
import nxt.AskOrder;
import nxt.Attachment;
import nxt.Blockchain;
import nxt.Genesis;
import nxt.Nxt;
import nxt.Peer;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class CancelAskOrder extends HttpRequestHandler {

    static final CancelAskOrder instance = new CancelAskOrder();

    private CancelAskOrder() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String secretPhrase = req.getParameter("secretPhrase");
        String orderValue = req.getParameter("order");
        String feeValue = req.getParameter("fee");
        String deadlineValue = req.getParameter("deadline");
        String referencedTransactionValue = req.getParameter("referencedTransaction");
        if (secretPhrase == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"secretPhrase\" not specified");

        } else if (orderValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"order\" not specified");

        } else if (feeValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"fee\" not specified");

        } else if (deadlineValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"deadline\" not specified");

        } else {

            try {

                long order = Convert.parseUnsignedLong(orderValue);

                try {

                    int fee = Integer.parseInt(feeValue);
                    if (fee <= 0 || fee >= Nxt.MAX_BALANCE) {

                        throw new Exception();

                    }

                    try {

                        short deadline = Short.parseShort(deadlineValue);
                        if (deadline < 1) {

                            throw new Exception();

                        }

                        long referencedTransaction = referencedTransactionValue == null ? 0 : Convert.parseUnsignedLong(referencedTransactionValue);

                        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
                        long accountId = Account.getId(publicKey);

                        AskOrder orderData = Blockchain.askOrders.get(order);
                        if (orderData == null || orderData.account.id != accountId) {

                            response.put("errorCode", 5);
                            response.put("errorDescription", "Unknown order");

                        } else {

                            Account account = Nxt.accounts.get(accountId);
                            if (account == null) {

                                response.put("errorCode", 6);
                                response.put("errorDescription", "Not enough funds");

                            } else {

                                if (fee * 100L > account.getUnconfirmedBalance()) {

                                    response.put("errorCode", 6);
                                    response.put("errorDescription", "Not enough funds");

                                } else {

                                    int timestamp = Convert.getEpochTime();

                                    Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS,
                                            Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION, timestamp, deadline,
                                            publicKey, Genesis.CREATOR_ID, 0, fee, referencedTransaction);
                                    transaction.attachment = new Attachment.ColoredCoinsAskOrderCancellation(order);
                                    transaction.sign(secretPhrase);

                                    JSONObject peerRequest = new JSONObject();
                                    peerRequest.put("requestType", "processTransactions");
                                    JSONArray transactionsData = new JSONArray();
                                    transactionsData.add(transaction.getJSONObject());
                                    peerRequest.put("transactions", transactionsData);

                                    Peer.sendToSomePeers(peerRequest);

                                    response.put("transaction", transaction.getStringId());

                                    Nxt.nonBroadcastedTransactions.put(transaction.id, transaction);

                                }

                            }

                        }

                    } catch (Exception e) {

                        response.put("errorCode", 4);
                        response.put("errorDescription", "Incorrect \"deadline\"");

                    }

                } catch (Exception e) {

                    response.put("errorCode", 4);
                    response.put("errorDescription", "Incorrect \"fee\"");

                }

            } catch (Exception e) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"order\"");

            }

        }
        return response;
    }

}
