package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ACCOUNT_DESCRIPTION_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_ACCOUNT_NAME_LENGTH;

public final class SetAccountInfo extends CreateTransaction {

    static final SetAccountInfo instance = new SetAccountInfo();

    private SetAccountInfo() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "name", "description"/*, "messagePatternRegex", "messagePatternFlags"*/);
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String name = Convert.nullToEmpty(req.getParameter("name")).trim();
        String description = Convert.nullToEmpty(req.getParameter("description")).trim();

        if (name.length() > Constants.MAX_ACCOUNT_NAME_LENGTH) {
            return INCORRECT_ACCOUNT_NAME_LENGTH;
        }

        if (description.length() > Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH) {
            return INCORRECT_ACCOUNT_DESCRIPTION_LENGTH;
        }

        /*
        Pattern messagePattern = null;
        String regex = Convert.emptyToNull(req.getParameter("messagePatternRegex"));
        if (regex != null) {
            String flagsValue = Convert.emptyToNull(req.getParameter("messagePatternFlags"));
            try {
                int flags = flagsValue == null ? 0 : Integer.parseInt(flagsValue);
                messagePattern = Pattern.compile(regex, flags);
            } catch (NumberFormatException e) {
                return INCORRECT_MESSAGE_PATTERN_FLAGS;
            } catch (RuntimeException e) {
                return INCORRECT_MESSAGE_PATTERN_REGEX;
            }
        }
        */

        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.MessagingAccountInfo(name, description);
        return createTransaction(req, account, attachment);

    }

}
