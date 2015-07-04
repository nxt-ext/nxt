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
import nxt.Currency;
import nxt.MonetarySystem;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.util.Filter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetAccountExpectedExchangeRequests extends APIServlet.APIRequestHandler {

    static final GetAccountExpectedExchangeRequests instance = new GetAccountExpectedExchangeRequests();

    private GetAccountExpectedExchangeRequests() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.MS}, "account", "currency");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getAccount(req);
        Currency currency = ParameterParser.getCurrency(req);

        Filter<Transaction> filter = transaction -> {
            if (transaction.getType() != MonetarySystem.EXCHANGE_BUY && transaction.getType() != MonetarySystem.EXCHANGE_SELL) {
                return false;
            }
            if (transaction.getSenderId() != account.getId()) {
                return false;
            }
            Attachment.MonetarySystemExchange attachment = (Attachment.MonetarySystemExchange)transaction.getAttachment();
            return attachment.getCurrencyId() == currency.getId();
        };

        List<? extends Transaction> transactions = Nxt.getBlockchain().getExpectedTransactions(filter);

        JSONArray exchangeRequests = new JSONArray();
        transactions.forEach(transaction -> exchangeRequests.add(JSONData.expectedExchangeRequest(transaction)));
        JSONObject response = new JSONObject();
        response.put("exchangeRequests", exchangeRequests);
        return response;


    }

}
