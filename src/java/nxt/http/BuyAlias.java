package nxt.http;

import nxt.Account;
import nxt.Alias;
import nxt.Attachment;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ALIAS;
import static nxt.http.JSONResponses.INCORRECT_ALIAS_NOTFORSALE;
import static nxt.http.JSONResponses.MISSING_ALIAS;

public class BuyAlias extends CreateTransaction {
    static final BuyAlias instance = new BuyAlias();

    private BuyAlias() {
        super("alias", "price");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Account account = ParameterParser.getSenderAccount(req);
        String aliasName = req.getParameter("alias").toLowerCase();
        Long recipient = ParameterParser.getRecipientId(req);

        if (aliasName == null) {
            return MISSING_ALIAS;
        }

        if (!Alias.aliasExists(aliasName)) {
            return INCORRECT_ALIAS;
        }

        if (Alias.getPrice(Alias.getAlias(aliasName)) == null) {
            return INCORRECT_ALIAS_NOTFORSALE;
        }

        Attachment attachment = new Attachment.MessagingAliasBuy(aliasName);
        return createTransaction(req, account, recipient, 0, attachment);
    }
}
