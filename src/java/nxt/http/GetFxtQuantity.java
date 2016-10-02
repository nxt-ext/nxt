/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016 Jelurida IP B.V.
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

import nxt.FxtDistribution;
import nxt.NxtException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetFxtQuantity extends APIServlet.APIRequestHandler {

    static final GetFxtQuantity instance = new GetFxtQuantity();

    private GetFxtQuantity() {
        super(new APITag[] {APITag.ACCOUNTS}, "account");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        long accountId = ParameterParser.getAccountId(req, true);
        JSONObject json = new JSONObject();
        long confirmedQuantity = FxtDistribution.getConfirmedFxtQuantity(accountId);
        long remainingQuantity = FxtDistribution.getRemainingFxtQuantity(accountId);
        long total = confirmedQuantity + remainingQuantity;
        json.put("quantityQNT", String.valueOf(confirmedQuantity));
        json.put("remainingQuantityQNT", String.valueOf(remainingQuantity));
        json.put("totalExpectedQuantityQNT", String.valueOf(total));
        json.put("distributionStart", FxtDistribution.DISTRIBUTION_START);
        json.put("distributionEnd", FxtDistribution.DISTRIBUTION_END);
        json.put("distributionFrequency", FxtDistribution.DISTRIBUTION_FREQUENCY);
        json.put("distributionStep", FxtDistribution.DISTRIBUTION_STEP);
        json.put("fxtAsset", Long.toUnsignedString(FxtDistribution.FXT_ASSET_ID));
        return json;
    }

}
