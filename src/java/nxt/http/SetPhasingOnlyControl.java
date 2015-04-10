package nxt.http;

import javax.servlet.http.HttpServletRequest;

import nxt.Account;
import nxt.Attachment;
import nxt.NxtException;
import nxt.PhasingParams;

import org.json.simple.JSONStreamAware;
/**
 * Sets an account control that blocks transactions unless they are phased with certain parameters<br/>
 * 
 * </p>
 * Parameters
 * <ul>
 * <li>controlVotingModel - The expected voting model of the phasing. Possible values: 
 *  <ul>
 *  <li>NONE(-1) - the phasing control is removed</li>
 *  <li>ACCOUNT(0) - only whitelisted accounts voting is allowed</li>
 *  <li>NQT(1) - only balance voting is allowed</li>
 *  <li>ASSET(2) - only asset voting is allowed</li>
 *  <li>CURRENCY(3) - only currency voting is allowed</li>
 *  </ul>
 * </li>
 * <li>controlQuorum - The expected quorum.</li>
 * <li>controlMinBalance - The expected minimal balance</li>
 * <li>controlMinBalanceModel - The expected minimal balance model. Possible values:
 * <ul>
 *  <li>NONE(0) No minimal balance restriction. Expected with ACCOUNT voting</li>
 *  <li>NQT(1) Nxt balance threshold</li>
 *  <li>ASSET(2) Asset balance threshold</li>
 *  <li>CURRENCY(3) Currency balance threshold</li>
 * </ul>
 * </li>
 * <li>controlHolding - The expected holding ID - asset ID or currency ID.</li>
 * <li>controlWhitelisted - multiple values - the expected whitelisted accounts</li>
 * </ul>
 * </p>
 * 
 */
public class SetPhasingOnlyControl extends CreateTransaction {

    static final SetPhasingOnlyControl instance = new SetPhasingOnlyControl();

    private SetPhasingOnlyControl() {
        super(new APITag[] {APITag.ACCOUNT_CONTROL}, "controlVotingModel", "controlQuorum", "controlMinBalance", 
                "controlMinBalanceModel", "controlHolding", "controlWhitelisted", "controlWhitelisted", "controlWhitelisted");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest request)
            throws NxtException {
        
        Account account = ParameterParser.getSenderAccount(request);
        
        PhasingParams phasingParams = parsePhasingParams(request, "control");
        
        return createTransaction(request, account, new Attachment.SetPhasingOnly(phasingParams));
    }

}
