package nxt.user;

import nxt.Account;
import nxt.Constants;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static nxt.user.JSONResponses.NOTIFY_OF_ACCEPTED_TRANSACTION;

public final class SendMoney extends UserServlet.UserRequestHandler {

    static final SendMoney instance = new SendMoney();

    private SendMoney() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req, User user) throws NxtException.ValidationException, IOException {
        if (user.getSecretPhrase() == null) {
            return null;
        }

        String recipientValue = req.getParameter("recipient");
        String amountValue = req.getParameter("amount");
        String feeValue = req.getParameter("fee");
        String deadlineValue = req.getParameter("deadline");
        String secretPhrase = req.getParameter("secretPhrase");

        Long recipient;
        int amount = 0;
        int fee = 0;
        short deadline = 0;

        try {

            recipient = Convert.parseUnsignedLong(recipientValue);
            if (recipient == null) throw new IllegalArgumentException("invalid recipient");
            amount = Integer.parseInt(amountValue.trim());
            fee = Integer.parseInt(feeValue.trim());
            deadline = (short)(Double.parseDouble(deadlineValue) * 60);

        } catch (RuntimeException e) {

            JSONObject response = new JSONObject();
            response.put("response", "notifyOfIncorrectTransaction");
            response.put("message", "One of the fields is filled incorrectly!");
            response.put("recipient", recipientValue);
            response.put("amount", amountValue);
            response.put("fee", feeValue);
            response.put("deadline", deadlineValue);

            return response;

        }

        if (! user.getSecretPhrase().equals(secretPhrase)) {

            JSONObject response = new JSONObject();
            response.put("response", "notifyOfIncorrectTransaction");
            response.put("message", "Wrong secret phrase!");
            response.put("recipient", recipientValue);
            response.put("amount", amountValue);
            response.put("fee", feeValue);
            response.put("deadline", deadlineValue);

            return response;

        } else if (amount <= 0 || amount > Constants.MAX_BALANCE) {

            JSONObject response = new JSONObject();
            response.put("response", "notifyOfIncorrectTransaction");
            response.put("message", "\"Amount\" must be greater than 0!");
            response.put("recipient", recipientValue);
            response.put("amount", amountValue);
            response.put("fee", feeValue);
            response.put("deadline", deadlineValue);

            return response;

        } else if (fee <= 0 || fee > Constants.MAX_BALANCE) {

            JSONObject response = new JSONObject();
            response.put("response", "notifyOfIncorrectTransaction");
            response.put("message", "\"Fee\" must be greater than 0!");
            response.put("recipient", recipientValue);
            response.put("amount", amountValue);
            response.put("fee", feeValue);
            response.put("deadline", deadlineValue);

            return response;

        } else if (deadline < 1 || deadline > 1440) {

            JSONObject response = new JSONObject();
            response.put("response", "notifyOfIncorrectTransaction");
            response.put("message", "\"Deadline\" must be greater or equal to 1 minute and less than 24 hours!");
            response.put("recipient", recipientValue);
            response.put("amount", amountValue);
            response.put("fee", feeValue);
            response.put("deadline", deadlineValue);

            return response;

        }

        Account account = Account.getAccount(user.getPublicKey());
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

            final Transaction transaction = Nxt.getTransactionProcessor().newTransaction(deadline, user.getPublicKey(),
                    recipient, amount, fee, null);
            transaction.sign(user.getSecretPhrase());

            Nxt.getTransactionProcessor().broadcast(transaction);

            return NOTIFY_OF_ACCEPTED_TRANSACTION;

        }
    }

    @Override
    boolean requirePost() {
        return true;
    }

}
