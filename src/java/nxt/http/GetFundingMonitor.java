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
import nxt.HoldingType;
import nxt.crypto.Crypto;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static nxt.http.JSONResponses.MONITOR_NOT_STARTED;

/**
 * Get a funding monitor
 * <p>
 * A single monitor will be returned when the secret phrase is specified.
 * The monitor is identified by the secret phrase, holding and account property.
 * The administrator password is not required and will be ignored.
 * <p>
 * When the administrator password is specified, a single monitor can be
 * returned by specifying the funding account, holding and account property.
 * If no account is specified, all monitors will be returned.
 * <p>
 * The monitor holding type and account property name must be specified when the secret
 * phrase or account is specified. Holding type codes are listed in getConstants.
 * In addition, the holding identifier must be specified when the holding type is ASSET or CURRENCY.
 */
public class GetFundingMonitor extends APIServlet.APIRequestHandler {

    static final GetFundingMonitor instance = new GetFundingMonitor();

    private GetFundingMonitor() {
        super(new APITag[] {APITag.ACCOUNTS}, "holdingType", "holding", "property", "secretPhrase",
                "includeMonitoredAccounts", "account", "adminPassword");
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
        String secretPhrase = ParameterParser.getSecretPhrase(req, false);
        long accountId = ParameterParser.getAccountId(req, false);
        boolean includeMonitoredAccounts = "true".equalsIgnoreCase(req.getParameter("includeMonitoredAccounts"));
        if (secretPhrase == null) {
            API.verifyPassword(req);
        }
        if (secretPhrase != null || accountId != 0) {
            if (secretPhrase != null) {
                if (accountId != 0) {
                    if (Account.getId(Crypto.getPublicKey(secretPhrase)) != accountId) {
                        return JSONResponses.INCORRECT_ACCOUNT;
                    }
                } else {
                    accountId = Account.getId(Crypto.getPublicKey(secretPhrase));
                }
            }
            HoldingType holdingType = ParameterParser.getHoldingType(req);
            long holdingId = ParameterParser.getHoldingId(req, holdingType);
            String property = ParameterParser.getAccountProperty(req);
            FundingMonitor monitor = FundingMonitor.getMonitor(holdingType, holdingId, property, accountId);
            if (monitor == null) {
                return MONITOR_NOT_STARTED;
            }
            JSONObject response = JSONData.accountMonitor(monitor);
            if (includeMonitoredAccounts) {
                JSONArray jsonAccounts = new JSONArray();
                List<FundingMonitor.MonitoredAccount> accountList = FundingMonitor.getMonitoredAccounts(monitor);
                accountList.forEach(account -> jsonAccounts.add(JSONData.monitoredAccount(account)));
                response.put("monitoredAccounts", jsonAccounts);
            }
            return response;
        } else {
            List<FundingMonitor> monitors = FundingMonitor.getAllMonitors();
            JSONObject response = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            monitors.forEach(monitor -> {
                JSONObject monitorJSON = JSONData.accountMonitor(monitor);
                if (includeMonitoredAccounts) {
                    JSONArray jsonAccounts = new JSONArray();
                    List<FundingMonitor.MonitoredAccount> accountList = FundingMonitor.getMonitoredAccounts(monitor);
                    accountList.forEach(account -> jsonAccounts.add(JSONData.monitoredAccount(account)));
                    monitorJSON.put("monitoredAccounts", jsonAccounts);
                }
                jsonArray.add(monitorJSON);
            });
            response.put("monitors", jsonArray);
            return response;
        }
    }

    @Override
    boolean allowRequiredBlockParameters() {
        return false;
    }
}
