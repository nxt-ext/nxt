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

import javax.servlet.http.HttpServletRequest;

import nxt.Account;
import nxt.AccountMonitor;
import nxt.crypto.Crypto;

import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;

import java.util.List;

import static nxt.http.JSONResponses.MONITOR_NOT_STARTED;

/**
 * Get an account monitor
 *
 * A single account monitor will be returned when the secret phrase is specified.
 * Otherwise, the administrator password must be specified and all account monitors
 * will be returned.
 *
 * The account monitor type and account property name must be specified when the secret
 * phrase is specified.  In addition, the holding identifier must be specified when
 * the monitor type is ASSET or CURRENCY.
 */
public class GetAccountMonitor extends APIServlet.APIRequestHandler {

    static final GetAccountMonitor instance = new GetAccountMonitor();

    private GetAccountMonitor() {
        super(new APITag[] {APITag.ACCOUNTS}, "type", "holding", "property", "secretPhrase", "adminPassword");
    }
    /**
     * Process the request
     *
     * @param   req                 Client request
     * @return                      Client response
     * @throws  NxtException        Unable to process request
     */
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        String secretPhrase = ParameterParser.getSecretPhrase(req, false);
        if (secretPhrase != null) {
            long accountId = Account.getId(Crypto.getPublicKey(secretPhrase));
            AccountMonitor.MonitorType monitorType = ParameterParser.getMonitorType(req);
            String property = ParameterParser.getAccountProperty(req);
            long holdingId = 0;
            switch (monitorType) {
                case ASSET:
                case CURRENCY:
                    holdingId = ParameterParser.getUnsignedLong(req, "holding", true);
                    break;
            }
            AccountMonitor monitor = AccountMonitor.getMonitor(monitorType, holdingId, property, accountId);
            if (monitor == null) {
                return MONITOR_NOT_STARTED;
            }
            return JSONData.accountMonitor(monitor);
        } else {
            API.verifyPassword(req);
            List<AccountMonitor> monitors = AccountMonitor.getAllMonitors();
            JSONArray response = new JSONArray();
            monitors.forEach(monitor -> response.add(JSONData.accountMonitor(monitor)));
            return response;
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
