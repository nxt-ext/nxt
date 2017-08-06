/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Currency;
import nxt.MonetarySystem;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.TransactionScheduler;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;

public final class ScheduleCurrencyBuy extends CreateTransaction {

    static final ScheduleCurrencyBuy instance = new ScheduleCurrencyBuy();

    private ScheduleCurrencyBuy() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "rateNQT", "units", "offerIssuer");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        boolean broadcast = !"false".equalsIgnoreCase(req.getParameter("broadcast"));
        if (broadcast) {
            return JSONResponses.error("Must use broadcast=false to schedule a future currency buy");
        }
        Currency currency = ParameterParser.getCurrency(req);
        long rateNQT = ParameterParser.getLong(req, "rateNQT", 0, Long.MAX_VALUE, true);
        long units = ParameterParser.getLong(req, "units", 0, Long.MAX_VALUE, true);
        Account account = ParameterParser.getSenderAccount(req);

        long offerIssuerId = ParameterParser.getAccountId(req, "offerIssuer", true);
        TransactionScheduler.SenderAndTypeFilter filter = new TransactionScheduler.SenderAndTypeFilter(offerIssuerId, MonetarySystem.PUBLISH_EXCHANGE_OFFER);

        Attachment attachment = new Attachment.MonetarySystemExchangeBuy(currency.getId(), rateNQT, units);
        try {
            JSONObject json = (JSONObject)JSONValue.parse(JSON.toString(createTransaction(req, account, attachment)));
            JSONObject transactionJSON = (JSONObject)json.get("transactionJSON");
            Transaction.Builder builder = Nxt.newTransactionBuilder(transactionJSON);
            Transaction transaction = builder.build();
            TransactionScheduler.schedule(filter, transaction);
            return json;
        } catch (NxtException.InsufficientBalanceException e) {
            return JSONResponses.NOT_ENOUGH_FUNDS;
        }
    }

}
