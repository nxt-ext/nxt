package nxt.http;

import nxt.AccountRestrictions.PhasingOnly;
import nxt.util.JSON;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * Returns the phasing control certain account. The result contains the following entries similar to the control* parameters of {@link SetPhasingOnlyControl}
 * 
 * <ul>
 * <li>phasingVotingModel - See {@link SetPhasingOnlyControl} for possible values. NONE(-1) means not control is set</li>
 * <li>phasingQuorum</li>
 * <li>phasingMinBalance</li>
 * <li>phasingMinBalanceModel - See {@link SetPhasingOnlyControl} for possible values</li>
 * <li>phasingHolding</li>
 * <li>phasingWhitelisted - array of whitelisted voter account IDs</li>
 * </ul>
 * 
 * </p>
 * Parameters
 * <ul>
 * <li>account - the account for which the phasing control is queried</li>
 * </ul>
 * </p>
 * 
 * @see SetPhasingOnlyControl
 * 
 */
public final class GetPhasingOnlyControl extends APIServlet.APIRequestHandler {

    static final GetPhasingOnlyControl instance = new GetPhasingOnlyControl();
    
    private GetPhasingOnlyControl() {
        super(new APITag[] {APITag.ACCOUNT_CONTROL}, "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        long accountId = ParameterParser.getAccountId(req, true);
        PhasingOnly phasingOnly = PhasingOnly.get(accountId);
        return phasingOnly == null ? JSON.emptyJSON : JSONData.phasingOnly(phasingOnly);
    }

}
