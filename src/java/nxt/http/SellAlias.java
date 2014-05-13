package nxt.http;

import nxt.*;
import org.json.simple.JSONStreamAware;
import javax.servlet.http.HttpServletRequest;
import static nxt.http.JSONResponses.*;


/**
 * Just simple alias transfer, with no any escrow support
 */
public class SellAlias extends CreateTransaction {
    static final SellAlias instance = new SellAlias();

    private SellAlias() {
        super("alias", "recipient", "priceNQT");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        String aliasName = req.getParameter("alias");
        long priceNQT = ParameterParser.getPriceNQT(req);

        Account owner = ParameterParser.getSenderAccount(req);
        Long recipient = ParameterParser.getRecipientId(req);

        if (aliasName == null) {
            return MISSING_ALIAS;
        }

        if (!Alias.aliasExists(aliasName)) {
            return INCORRECT_ALIAS;
        }

        if (!Alias.getAlias(aliasName).getAccount().equals(owner)) {
            return INCORRECT_ALIAS_OWNER;
        }

        Attachment attachment = new Attachment.MessagingAliasSell(aliasName, priceNQT);
        return createTransaction(req, owner, recipient, 0, attachment);
    }
}
