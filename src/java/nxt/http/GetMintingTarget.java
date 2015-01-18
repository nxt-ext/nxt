package nxt.http;

import nxt.Currency;
import nxt.CurrencyMinting;
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
 * <li>account - miner account id
 * <li>units - number of currency units the miner is trying to mint
 * </ul>
 */
public final class GetMintingTarget extends APIServlet.APIRequestHandler {

    static final GetMintingTarget instance = new GetMintingTarget();

    private GetMintingTarget() {
        super(new APITag[] {APITag.MS}, "currency", "account", "units");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Currency currency = ParameterParser.getCurrency(req);
        JSONObject json = new JSONObject();
        json.put("currency", Convert.toUnsignedLong(currency.getId()));
        long units = ParameterParser.getLong(req, "units", 1, currency.getMaxSupply() - currency.getReserveSupply(), true);
        BigInteger numericTarget = CurrencyMinting.getNumericTarget(currency, units);
        json.put("difficulty", String.valueOf(BigInteger.ZERO.equals(numericTarget) ? -1 : BigInteger.valueOf(2).pow(256).subtract(BigInteger.ONE).divide(numericTarget)));
        json.put("targetBytes", Convert.toHexString(CurrencyMinting.getTarget(numericTarget)));
        json.put("counter", nxt.CurrencyMint.getCounter(currency.getId(), ParameterParser.getAccount(req).getId()));
        return json;
    }

}
