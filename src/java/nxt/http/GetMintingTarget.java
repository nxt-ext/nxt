package nxt.http;

import nxt.Currency;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;

/**
 * Currency miners can use this API to obtain their target hash value for minting currency units
 * </p>
 * Parameters
 * <ul>
 * <li>currency - currency id
 * <li>units - number of currency units the miner is trying to mint
 * </ul>
 */
public final class GetMintingTarget extends APIServlet.APIRequestHandler {

    static final GetMintingTarget instance = new GetMintingTarget();

    private GetMintingTarget() {
        super(new APITag[] {APITag.MS}, "currency", "code", "units");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Currency currency = ParameterParser.getCurrency(req);
        JSONObject json = new JSONObject();
        json.put("currency", Convert.toUnsignedLong(currency.getId()));
        long units = ParameterParser.getLong(req, "units", 1, currency.getMaxSupply() - currency.getReserveSupply(), true);
        BigInteger target = nxt.CurrencyMint.getNumericTarget(currency.getMinDifficulty(), currency.getMaxDifficulty(), units,
                currency.getCurrentSupply() - currency.getReserveSupply(), currency.getMaxSupply() - currency.getReserveSupply());
        json.put("difficulty", BigInteger.valueOf(2).pow(256).divide(target));
        json.put("targetBytes", Convert.toHexString(nxt.CurrencyMint.getTarget(currency.getMinDifficulty(), currency.getMaxDifficulty(), units,
                currency.getCurrentSupply() - currency.getReserveSupply(), currency.getMaxSupply() - currency.getReserveSupply())));
        return json;
    }

}
