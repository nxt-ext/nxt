package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ARBITRARY_MESSAGE;
import static nxt.http.JSONResponses.INCORRECT_DEADLINE;
import static nxt.http.JSONResponses.INCORRECT_FEE;
import static nxt.http.JSONResponses.INCORRECT_RECIPIENT;
import static nxt.http.JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_DEADLINE;
import static nxt.http.JSONResponses.MISSING_FEE;
import static nxt.http.JSONResponses.MISSING_MESSAGE;
import static nxt.http.JSONResponses.MISSING_RECIPIENT;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.NOT_ENOUGH_FUNDS;

public final class SendMessage extends CreateTransaction {

    static final SendMessage instance = new SendMessage();

    private SendMessage() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {

        String recipientValue = req.getParameter("recipient");
        String messageValue = req.getParameter("message");

        if (recipientValue == null || "0".equals(recipientValue)) {
            return MISSING_RECIPIENT;
        } else if (messageValue == null) {
            return MISSING_MESSAGE;
        }

        Long recipient;
        try {
            recipient = Convert.parseUnsignedLong(recipientValue);
        } catch (RuntimeException e) {
            return INCORRECT_RECIPIENT;
        }

        byte[] message;
        try {
            message = Convert.parseHexString(messageValue);
        } catch (RuntimeException e) {
            return INCORRECT_ARBITRARY_MESSAGE;
        }
        if (message.length > Nxt.MAX_ARBITRARY_MESSAGE_LENGTH) {
            return INCORRECT_ARBITRARY_MESSAGE;
        }

        Account account = getAccount(req);
        if (account == null) {
            return NOT_ENOUGH_FUNDS;
        }

        Attachment attachment = new Attachment.MessagingArbitraryMessage(message);
        return createTransaction(req, account, recipient, 0, attachment);

    }

}
