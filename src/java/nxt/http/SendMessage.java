package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Blockchain;
import nxt.Nxt;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class SendMessage extends HttpRequestHandler {

    static final SendMessage instance = new SendMessage();

    private SendMessage() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String secretPhrase = req.getParameter("secretPhrase");
        String recipientValue = req.getParameter("recipient");
        String messageValue = req.getParameter("message");
        String feeValue = req.getParameter("fee");
        String deadlineValue = req.getParameter("deadline");
        String referencedTransactionValue = req.getParameter("referencedTransaction");
        if (secretPhrase == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"secretPhrase\" not specified");

        } else if (recipientValue == null || "0".equals(recipientValue)) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"recipient\" not specified");

        } else if (messageValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"message\" not specified");

        } else if (feeValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"fee\" not specified");

        } else if (deadlineValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"deadline\" not specified");

        } else {

            try {

                Long recipient = Convert.parseUnsignedLong(recipientValue);

                try {

                    byte[] message = Convert.convert(messageValue);
                    if (message.length > Nxt.MAX_ARBITRARY_MESSAGE_LENGTH) {

                        response.put("errorCode", 4);
                        response.put("errorDescription", "Incorrect \"message\" (length must be not longer than " + Nxt.MAX_ARBITRARY_MESSAGE_LENGTH + " bytes)");

                    } else {

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

                                Account account = Account.getAccount(publicKey);
                                if (account == null || fee * 100L > account.getUnconfirmedBalance()) {

                                    response.put("errorCode", 6);
                                    response.put("errorDescription", "Not enough funds");

                                } else {

                                    int timestamp = Convert.getEpochTime();

                                    Attachment attachment = new Attachment.MessagingArbitraryMessage(message);
                                    Transaction transaction = Transaction.newTransaction(timestamp, deadline, publicKey,
                                            recipient, 0, fee, referencedTransaction, attachment);
                                    transaction.sign(secretPhrase);

                                    JSONObject peerRequest = new JSONObject();
                                    peerRequest.put("requestType", "processTransactions");
                                    JSONArray transactionsData = new JSONArray();
                                    transactionsData.add(transaction.getJSONObject());
                                    peerRequest.put("transactions", transactionsData);

                                    Peer.sendToSomePeers(peerRequest);

                                    response.put("transaction", transaction.getStringId());
                                    response.put("bytes", Convert.convert(transaction.getBytes()));

                                    Blockchain.broadcast(transaction);

                                }

                            } catch (Exception e) {

                                response.put("errorCode", 4);
                                response.put("errorDescription", "Incorrect \"deadline\"");

                            }

                        } catch (Exception e) {

                            response.put("errorCode", 4);
                            response.put("errorDescription", "Incorrect \"fee\"");

                        }

                    }

                } catch (Exception e) {

                    response.put("errorCode", 4);
                    response.put("errorDescription", "Incorrect \"message\"");

                }

            } catch (Exception e) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"recipient\"");

            }

        }

        return response;
    }

}
