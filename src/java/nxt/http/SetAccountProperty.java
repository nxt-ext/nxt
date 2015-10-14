/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ACCOUNT_PROPERTY_NAME_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_ACCOUNT_PROPERTY_VALUE_LENGTH;

public final class SetAccountProperty extends CreateTransaction {

    static final SetAccountProperty instance = new SetAccountProperty();

    private SetAccountProperty() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "property", "value", "recipient");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long recipient = ParameterParser.getAccountId(req, "recipient", false);
        String property = Convert.nullToEmpty(req.getParameter("property")).trim();
        String value = Convert.nullToEmpty(req.getParameter("value")).trim();

        if (property.length() > Constants.MAX_ACCOUNT_PROPERTY_NAME_LENGTH || property.length() == 0) {
            return INCORRECT_ACCOUNT_PROPERTY_NAME_LENGTH;
        }

        if (value.length() > Constants.MAX_ACCOUNT_PROPERTY_VALUE_LENGTH) {
            return INCORRECT_ACCOUNT_PROPERTY_VALUE_LENGTH;
        }

        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.MessagingAccountProperty(property, value);
        return createTransaction(req, account, recipient, 0, attachment);

    }

}
