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
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class DeleteAccountProperty extends CreateTransaction {

    static final DeleteAccountProperty instance = new DeleteAccountProperty();

    private DeleteAccountProperty() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "recipient", "property", "setter");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account senderAccount = ParameterParser.getSenderAccount(req);
        long recipientId = ParameterParser.getAccountId(req, "recipient", false);
        if (recipientId == 0) {
            recipientId = senderAccount.getId();
        }
        long setterId = ParameterParser.getAccountId(req, "setter", false);
        if (setterId == 0) {
            setterId = senderAccount.getId();
        }
        String property = Convert.nullToEmpty(req.getParameter("property")).trim();
        if (property.isEmpty()) {
            return JSONResponses.MISSING_PROPERTY;
        }
        Account.AccountProperty accountProperty = Account.getProperty(recipientId, property, setterId);
        if (accountProperty == null) {
            return JSONResponses.UNKNOWN_PROPERTY;
        }
        if (accountProperty.getRecipientId() != senderAccount.getId() && accountProperty.getSetterId() != senderAccount.getId()) {
            return JSONResponses.INCORRECT_PROPERTY;
        }
        Attachment attachment = new Attachment.MessagingAccountPropertyDelete(accountProperty.getId());
        return createTransaction(req, senderAccount, recipientId, 0, attachment);

    }

}
