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

import nxt.AccountMonitor;
import nxt.HoldingType;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>Stop an account monitor</p>
 *
 * <p>A single account monitor will be stopped when the secret phrase is specified.
 * Otherwise, the administrator password must be specified and all account monitors
 * will be stopped.</p>
 *
 * <p>The account monitor holding type and account property name must be specified when the secret
 * phrase is specified. Holding type codes are listed in getConstants.
 *  In addition, the holding identifier must be specified when the holding type is ASSET or CURRENCY.</p>
 */
public class StopAccountMonitor extends APIServlet.APIRequestHandler {

    static final StopAccountMonitor instance = new StopAccountMonitor();

    private StopAccountMonitor() {
        super(new APITag[] {APITag.ACCOUNTS}, "holdingType", "holding", "property", "account", "adminPassword");
    }
    /**
     * Process the request
     *
     * @param   req                 Client request
     * @return                      Client response
     * @throws  ParameterException        Unable to process request
     */
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        long accountId = ParameterParser.getAccountId(req, false);
        JSONObject response = new JSONObject();
        if (accountId != 0) {
            HoldingType holdingType = ParameterParser.getHoldingType(req);
            long holdingId = ParameterParser.getHoldingId(req, holdingType);
            String property = ParameterParser.getAccountProperty(req);
            boolean stopped = AccountMonitor.stopMonitor(holdingType, holdingId, property, accountId);
            response.put("stopped", stopped ? 1 : 0);
        } else {
            API.verifyPassword(req);
            int count = AccountMonitor.stopAllMonitors();
            response.put("stopped", count);
        }
        return response;
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
