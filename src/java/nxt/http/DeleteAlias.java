package nxt.http;

import nxt.Account;
import nxt.Alias;
import nxt.Attachment;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ALIAS_OWNER;


public final class DeleteAlias extends CreateTransaction {

    static final DeleteAlias instance = new DeleteAlias();

    private DeleteAlias() {
        super(new APITag[] {APITag.ALIASES, APITag.CREATE_TRANSACTION}, "alias", "aliasName");
    }

    @Override
    JSONStreamAware processRequest(final HttpServletRequest req) throws NxtException {
        final Alias alias = ParameterParser.getAlias(req);
        final Account owner = ParameterParser.getSenderAccount(req);

        if (alias.getAccountId() != owner.getId()) {
            return INCORRECT_ALIAS_OWNER;
        }

        final Attachment attachment = new Attachment.MessagingAliasDelete(alias.getAliasName());
        return createTransaction(req, owner, attachment);
    }
}
