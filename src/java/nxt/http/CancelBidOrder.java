package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Blockchain;
import nxt.Genesis;
import nxt.Nxt;
import nxt.Order;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class CancelBidOrder extends HttpRequestHandler {

    static final CancelBidOrder instance = new CancelBidOrder();

    private CancelBidOrder() {}

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

                Long order = Convert.parseUnsignedLong(orderValue);

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

                        Long referencedTransaction = referencedTransactionValue == null ? null : Convert.parseUnsignedLong(referencedTransactionValue);

                        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
                        Long accountId = Account.getId(publicKey);

                        Order.Bid orderData = Order.Bid.getBidOrder(order);
                        if (orderData == null || !orderData.account.id.equals(accountId)) {

                            response.put("errorCode", 5);
                            response.put("errorDescription", "Unknown order");

                        } else {

                            Account account = Account.getAccount(accountId);
                            if (account == null) {

                                response.put("errorCode", 6);
                                response.put("errorDescription", "Not enough funds");

                            } else {

                                if (fee * 100L > account.getUnconfirmedBalance()) {

                                    response.put("errorCode", 6);
                                    response.put("errorDescription", "Not enough funds");

                                } else {

                                    int timestamp = Convert.getEpochTime();

                                    Attachment attachment = new Attachment.ColoredCoinsBidOrderCancellation(order);
                                    Transaction transaction = Transaction.newTransaction(timestamp, deadline, publicKey,
                                            Genesis.CREATOR_ID, 0, fee, referencedTransaction, attachment);
                                    transaction.sign(secretPhrase);

                                    Blockchain.broadcast(transaction);

                                    response.put("transaction", transaction.getStringId());


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
