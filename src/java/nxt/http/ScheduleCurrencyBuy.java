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
import nxt.db.DbIterator;
import nxt.util.Convert;
import nxt.util.Filter;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;

public final class ScheduleCurrencyBuy extends CreateTransaction {

    static final ScheduleCurrencyBuy instance = new ScheduleCurrencyBuy();

    private ScheduleCurrencyBuy() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "rateNQT", "units", "offerIssuer",
                "transactionJSON", "transactionBytes", "prunableAttachmentJSON");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        String transactionJSON = Convert.emptyToNull(req.getParameter("transactionJSON"));
        String transactionBytes = Convert.emptyToNull(req.getParameter("transactionBytes"));
        String prunableAttachmentJSON = Convert.emptyToNull(req.getParameter("prunableAttachmentJSON"));
        long offerIssuerId = ParameterParser.getAccountId(req, "offerIssuer", true);

        try {
            JSONObject response;
            Transaction transaction;
            if (transactionBytes == null && transactionJSON == null) {
                boolean broadcast = !"false".equalsIgnoreCase(req.getParameter("broadcast"));
                if (broadcast) {
                    return JSONResponses.error("Must use broadcast=false to schedule a future currency buy");
                }
                Currency currency = ParameterParser.getCurrency(req);
                long rateNQT = ParameterParser.getLong(req, "rateNQT", 0, Long.MAX_VALUE, true);
                long units = ParameterParser.getLong(req, "units", 0, Long.MAX_VALUE, true);
                Account account = ParameterParser.getSenderAccount(req);
                String secretPhrase = ParameterParser.getSecretPhrase(req, false);
                Attachment attachment = new Attachment.MonetarySystemExchangeBuy(currency.getId(), rateNQT, units);
                response = (JSONObject)JSONValue.parse(JSON.toString(createTransaction(req, account, attachment)));
                if (secretPhrase == null) {
                    response.put("scheduled", false);
                    return response;
                }
                transaction = Nxt.newTransactionBuilder((JSONObject) response.get("transactionJSON")).build();
            } else {
                response = new JSONObject();
                transaction = ParameterParser.parseTransaction(transactionJSON, transactionBytes, prunableAttachmentJSON).build();
                JSONObject json = JSONData.unconfirmedTransaction(transaction);
                response.put("transactionJSON", json);
                try {
                    response.put("unsignedTransactionBytes", Convert.toHexString(transaction.getUnsignedBytes()));
                } catch (NxtException.NotYetEncryptedException ignore) {}
                response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
                response.put("signatureHash", json.get("signatureHash"));
                response.put("transaction", transaction.getStringId());
                response.put("fullHash", transaction.getFullHash());
            }

            Attachment.MonetarySystemExchangeBuy attachment = (Attachment.MonetarySystemExchangeBuy)transaction.getAttachment();
            Filter<Transaction> filter = new ExchangeOfferFilter(offerIssuerId, attachment.getCurrencyId(), attachment.getRateNQT());

            Nxt.getBlockchain().readLock();
            try {
                transaction.validate();
                try (DbIterator<? extends Transaction> unconfirmedTransactions = Nxt.getTransactionProcessor().getAllUnconfirmedTransactions()) {
                    while (unconfirmedTransactions.hasNext()) {
                        if (filter.ok(unconfirmedTransactions.next())) {
                            Nxt.getTransactionProcessor().broadcast(transaction);
                            response.put("broadcasted", true);
                            return response;
                        }
                    }
                }
                TransactionScheduler.schedule(filter, transaction);
                response.put("scheduled", true);
            } finally {
                Nxt.getBlockchain().readUnlock();
            }
            return response;

        } catch (NxtException.InsufficientBalanceException e) {
            return JSONResponses.NOT_ENOUGH_FUNDS;
        }
    }

    @Override
    protected boolean requireFullClient() {
        return true;
    }

    @Override
    protected boolean requirePassword() {
        return true;
    }


    private static class ExchangeOfferFilter implements Filter<Transaction> {

        private final long senderId;
        private final long currencyId;
        private final long rateNQT;

        public ExchangeOfferFilter(long senderId, long currencyId, long rateNQT) {
            this.senderId = senderId;
            this.currencyId = currencyId;
            this.rateNQT = rateNQT;
        }

        @Override
        public boolean ok(Transaction transaction) {
            if (transaction.getSenderId() != senderId
                    || transaction.getType() != MonetarySystem.PUBLISH_EXCHANGE_OFFER
                    || transaction.getPhasing() != null) {
                return false;
            }
            Attachment.MonetarySystemPublishExchangeOffer attachment = (Attachment.MonetarySystemPublishExchangeOffer)transaction.getAttachment();
            if (attachment.getCurrencyId() != currencyId || attachment.getSellRateNQT() > rateNQT) {
                return false;
            }
            return true;
        }

    }


}
