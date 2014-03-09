package nxt.http;


import nxt.Account;
import nxt.Alias;
import nxt.Attachment;
import nxt.Genesis;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ALIAS;
import static nxt.http.JSONResponses.INCORRECT_ALIAS_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_DEADLINE;
import static nxt.http.JSONResponses.INCORRECT_FEE;
import static nxt.http.JSONResponses.INCORRECT_URI_LENGTH;
import static nxt.http.JSONResponses.MISSING_ALIAS;
import static nxt.http.JSONResponses.MISSING_DEADLINE;
import static nxt.http.JSONResponses.MISSING_FEE;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.MISSING_URI;
import static nxt.http.JSONResponses.NOT_ENOUGH_FUNDS;

public final class AssignAlias extends CreateTransaction {

    static final AssignAlias instance = new AssignAlias();

    private AssignAlias() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {
        String alias = req.getParameter("alias");
        String uri = req.getParameter("uri");

        if (alias == null) {
            return MISSING_ALIAS;
        } else if (uri == null) {
            return MISSING_URI;
        }

        alias = alias.trim();
        if (alias.length() == 0 || alias.length() > Nxt.MAX_ALIAS_LENGTH) {
            return INCORRECT_ALIAS_LENGTH;
        }

        String normalizedAlias = alias.toLowerCase();
        for (int i = 0; i < normalizedAlias.length(); i++) {
            if (Nxt.ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
                return INCORRECT_ALIAS;
            }
        }

        uri = uri.trim();
        if (uri.length() > Nxt.MAX_ALIAS_URI_LENGTH) {
            return INCORRECT_URI_LENGTH;
        }

        Account account = getAccount(req);
        if (account == null) {
            return NOT_ENOUGH_FUNDS;
        }

        Alias aliasData = Alias.getAlias(normalizedAlias);
        if (aliasData != null && ! aliasData.getAccount().getId().equals(account.getId())) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 8);
            response.put("errorDescription", "\"" + alias + "\" is already used");
            return response;
        } else {
            Attachment attachment = new Attachment.MessagingAliasAssignment(alias, uri);
            return createTransaction(req, account, attachment);
        }

    }

}
