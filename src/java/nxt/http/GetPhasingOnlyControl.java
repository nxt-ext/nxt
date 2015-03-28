package nxt.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import nxt.Account;
import nxt.Account.ControlType;
import nxt.AccountControlTxBlocking.PhasingOnly;
import nxt.VoteWeighting.VotingModel;
import nxt.util.Convert;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

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
public class GetPhasingOnlyControl extends APIServlet.APIRequestHandler {

    static final GetPhasingOnlyControl instance = new GetPhasingOnlyControl();
    
    private GetPhasingOnlyControl() {
        super(new APITag[] {APITag.ACCOUNT_CONTROL}, "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        Account account = ParameterParser.getAccount(req);
        JSONObject response = new JSONObject();
        if (account.getControls().contains(ControlType.PHASING_ONLY)) {
            final long accountId = account.getId();
            
            PhasingOnly phasingOnly = PhasingOnly.get(accountId);
            phasingOnly.getPhasingParams().putMyJSON(response);
        } else {
            response.put("phasingVotingModel", VotingModel.NONE.getCode());
        }
        return response;
    }

}
