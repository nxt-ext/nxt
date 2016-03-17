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
import nxt.FundingMonitor;
import nxt.Asset;
import nxt.Currency;
import nxt.HoldingType;
import nxt.NxtException;
import nxt.crypto.Crypto;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.incorrect;
import static nxt.http.JSONResponses.MONITOR_ALREADY_STARTED;
import static nxt.http.JSONResponses.UNKNOWN_ACCOUNT;

/**
 * Start a funding monitor
 * <p>
 * A funding monitor will transfer NXT, ASSET or CURRENCY from the funding account
 * to a recipient account when the amount held by the recipient account drops below
 * the threshold.  The transfer will not be done until the current block
 * height is greater than equal to the block height of the last transfer plus the
 * interval. Holding type codes are listed in getConstants. The asset or currency is
 * specified by the holding identifier.
 * <p>
 * The funding account is identified by the secret phrase.  The secret phrase must
 * be specified since the funding monitor needs to sign the transactions that it submits.
 * <p>
 * The recipient accounts are identified by the specified account property.  Each account
 * that has this property set by the funding account will be monitored for changes.
 * The property value can be omitted or it can consist of a JSON string containing one or more
 * values in the format: {"amount":unsigned-long,"threshold":unsigned-long,"interval":integer}
 * <p>
 * The values can be specified as numeric values or as unsigned-long strings.  Values greater than the maximum
 * signed long value must be specified as unsigned-long strings.
 * <p>
 * For example, {"amount":25,"threshold":10,"interval":1440}.  The specified values will
 * override the default values specified when the account monitor is started.
 * <p>
 * NXT amounts are specified with 8 decimal places.  Asset and Currency decimal places
 * are determined by the asset or currency definition.
 */
public final class StartFundingMonitor extends APIServlet.APIRequestHandler {

    static final StartFundingMonitor instance = new StartFundingMonitor();

    private StartFundingMonitor() {
        super(new APITag[] {APITag.ACCOUNTS}, "holdingType", "holding", "property", "amount", "threshold",
                "interval", "secretPhrase");
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
        long amount = ParameterParser.getUnsignedLong(req, "amount", true);
        if (amount < FundingMonitor.MIN_FUND_AMOUNT) {
            throw new ParameterException(incorrect("amount", "Minimum funding amount is " + FundingMonitor.MIN_FUND_AMOUNT));
        }
        long threshold = ParameterParser.getUnsignedLong(req, "threshold", true);
        if (threshold < FundingMonitor.MIN_FUND_THRESHOLD) {
            throw new ParameterException(incorrect("threshold", "Minimum funding threshold is " + FundingMonitor.MIN_FUND_THRESHOLD));
        }
        int interval = ParameterParser.getInt(req, "interval", FundingMonitor.MIN_FUND_INTERVAL, Integer.MAX_VALUE, true);
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
        if (FundingMonitor.startMonitor(holdingType, holdingId, property, amount, threshold, interval, secretPhrase)) {
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
