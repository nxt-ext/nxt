/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
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
import nxt.AccountMonitor;
import nxt.Asset;
import nxt.Constants;
import nxt.Currency;
import nxt.HoldingType;
import nxt.NxtException;
import nxt.crypto.Crypto;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.MONITOR_ALREADY_STARTED;
import static nxt.http.JSONResponses.UNKNOWN_ACCOUNT;

/**
 * <p>Start an account monitor</p>
 *
 * <p>An account monitor will transfer NXT, ASSET or CURRENCY from the funding account
 * to a recipient account when the amount held by the recipient account drops below
 * the threshold.  The transfer will not be done until the current block
 * height is greater than equal to the block height of the last transfer plus the
 * interval. Holding type codes are listed in getConstants. The asset or currency is
 * specified by the holding identifier.</p>
 *
 * <p>The funding account is identified by the secret phrase.  The secret phrase must
 * be specified since the account monitor needs to sign the transactions that it submits.</p>
 *
 * <p>The recipient accounts are identified by the specified account property.  Each account
 * that has this property set by the funding account  will be monitored for changes.
 * The property value can be omitted or it can consist of a string containing one or more
 * comma-separated values in the format 'name=value' when name can be 'amount', 'threshold' or 'interval.
 * For example, 'amount=25,threshold=10,interval=1440'.  The specified values will
 * override the default values specified when the account monitor is started.</p>
 *
 * <p>NXT amounts are specified with 8 decimal places.  Asset and Currency decimal places
 * are determined by the asset or currency definition.</p>
 */
public final class StartAccountMonitor extends APIServlet.APIRequestHandler {

    static final StartAccountMonitor instance = new StartAccountMonitor();

    private StartAccountMonitor() {
        super(new APITag[] {APITag.ACCOUNTS}, "holdingType", "holding", "property", "amount", "threshold", "interval", "secretPhrase");
    }

    /**
     * Process the request
     *
     * @param   req                 Client request
     * @return                      Client response
     * @throws  NxtException        Unable to process request
     */
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        HoldingType holdingType = ParameterParser.getHoldingType(req);
        long holdingId = ParameterParser.getHoldingId(req, holdingType);
        String property = ParameterParser.getAccountProperty(req);
        long amount = ParameterParser.getLong(req, "amount", AccountMonitor.MIN_FUND_AMOUNT, Constants.MAX_BALANCE_NQT, true);
        long threshold = ParameterParser.getLong(req, "threshold", AccountMonitor.MIN_FUND_THRESHOLD, Constants.MAX_BALANCE_NQT, true);
        int interval = ParameterParser.getInt(req, "interval", AccountMonitor.MIN_FUND_INTERVAL, Integer.MAX_VALUE, true);
        String secretPhrase = ParameterParser.getSecretPhrase(req, true);
        switch (holdingType) {
            case ASSET:
                Asset asset = Asset.getAsset(holdingId);
                if (asset == null) {
                    throw new ParameterException(JSONResponses.UNKNOWN_ASSET);
                }
                break;
            case CURRENCY:
                Currency currency = Currency.getCurrency(holdingId);
                if (currency == null) {
                    throw new ParameterException(JSONResponses.UNKNOWN_CURRENCY);
                }
                break;
        }
        Account account = Account.getAccount(Crypto.getPublicKey(secretPhrase));
        if (account == null) {
            throw new ParameterException(UNKNOWN_ACCOUNT);
        }
        if (AccountMonitor.startMonitor(holdingType, holdingId, property, amount, threshold, interval, secretPhrase)) {
            JSONObject response = new JSONObject();
            response.put("started", true);
            return response;
        } else {
            return MONITOR_ALREADY_STARTED;
        }
    }

    @Override
    boolean requirePost() {
        return true;
    }

    @Override
    boolean allowRequiredBlockParameters() {
        return false;
    }
}
