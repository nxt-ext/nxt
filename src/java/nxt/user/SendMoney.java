package nxt.user;

import nxt.Account;
import nxt.Blockchain;
import nxt.Nxt;
import nxt.Transaction;
import nxt.peer.Peer;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

final class SendMoney extends UserRequestHandler {

    static final SendMoney instance = new SendMoney();

    private SendMoney() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req, User user) throws IOException {
        if (user.secretPhrase != null) {

            String recipientValue = req.getParameter("recipient");
            String amountValue = req.getParameter("amount");
            String feeValue = req.getParameter("fee");
            String deadlineValue = req.getParameter("deadline");
            String secretPhrase = req.getParameter("secretPhrase");

            long recipient;
            int amount = 0, fee = 0;
            short deadline = 0;

            try {

                recipient = Convert.parseUnsignedLong(recipientValue);
                amount = Integer.parseInt(amountValue.trim());
                fee = Integer.parseInt(feeValue.trim());
                deadline = (short)(Double.parseDouble(deadlineValue) * 60);

            } catch (Exception e) {

                JSONObject response = new JSONObject();
                response.put("response", "notifyOfIncorrectTransaction");
                response.put("message", "One of the fields is filled incorrectly!");
                response.put("recipient", recipientValue);
                response.put("amount", amountValue);
                response.put("fee", feeValue);
                response.put("deadline", deadlineValue);

                return response;

            }

            if (! user.secretPhrase.equals(secretPhrase)) {

                JSONObject response = new JSONObject();
                response.put("response", "notifyOfIncorrectTransaction");
                response.put("message", "Wrong secret phrase!");
                response.put("recipient", recipientValue);
                response.put("amount", amountValue);
                response.put("fee", feeValue);
                response.put("deadline", deadlineValue);

                return response;

            } else if (amount <= 0 || amount > Nxt.MAX_BALANCE) {

                JSONObject response = new JSONObject();
                response.put("response", "notifyOfIncorrectTransaction");
                response.put("message", "\"Amount\" must be greater than 0!");
                response.put("recipient", recipientValue);
                response.put("amount", amountValue);
                response.put("fee", feeValue);
                response.put("deadline", deadlineValue);

                return response;

            } else if (fee <= 0 || fee > Nxt.MAX_BALANCE) {

                JSONObject response = new JSONObject();
                response.put("response", "notifyOfIncorrectTransaction");
                response.put("message", "\"Fee\" must be greater than 0!");
                response.put("recipient", recipientValue);
                response.put("amount", amountValue);
                response.put("fee", feeValue);
                response.put("deadline", deadlineValue);

                return response;

            } else if (deadline < 1) {

                JSONObject response = new JSONObject();
                response.put("response", "notifyOfIncorrectTransaction");
                response.put("message", "\"Deadline\" must be greater or equal to 1 minute!");
                response.put("recipient", recipientValue);
                response.put("amount", amountValue);
                response.put("fee", feeValue);
                response.put("deadline", deadlineValue);

                return response;

            } else {

                Account account = Nxt.accounts.get(Account.getId(user.publicKey));
                if (account == null || (amount + fee) * 100L > account.getUnconfirmedBalance()) {

                    JSONObject response = new JSONObject();
                    response.put("response", "notifyOfIncorrectTransaction");
                    response.put("message", "Not enough funds!");
                    response.put("recipient", recipientValue);
                    response.put("amount", amountValue);
                    response.put("fee", feeValue);
                    response.put("deadline", deadlineValue);

                    return response;

                } else {

                    final Transaction transaction = Transaction.newTransaction(Convert.getEpochTime(), deadline, user.publicKey, recipient, amount, fee, 0);
                    transaction.sign(user.secretPhrase);

                    JSONObject peerRequest = new JSONObject();
                    peerRequest.put("requestType", "processTransactions");
                    JSONArray transactionsData = new JSONArray();
                    transactionsData.add(transaction.getJSONObject());
                    peerRequest.put("transactions", transactionsData);

                    Peer.sendToSomePeers(peerRequest);

                    JSONObject response = new JSONObject();
                    response.put("response", "notifyOfAcceptedTransaction");

                    Blockchain.broadcast(transaction);
                    return response;

                }

            }

        } else {
            return null;
        }
    }
}
