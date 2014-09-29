package nxt.http;

import nxt.Currency;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;

public final class GetMintingTarget extends APIServlet.APIRequestHandler {

    static final GetMintingTarget instance = new GetMintingTarget();

    private GetMintingTarget() {
        super(new APITag[] {APITag.MS}, "currency", "units");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Currency currency = ParameterParser.getCurrency(req);
        JSONObject json = new JSONObject();
        json.put("currency", Convert.toUnsignedLong(currency.getId()));
        long units = ParameterParser.getLong(req, "units", 1, currency.getTotalSupply(), true);
        BigInteger target = nxt.CurrencyMint.getNumericTarget(currency.getMinDifficulty(), currency.getMaxDifficulty(), units,
                currency.getCurrentSupply(), currency.getTotalSupply());
        json.put("difficulty", BigInteger.valueOf(2).pow(256).divide(target));
        json.put("targetBytes", Convert.toHexString(nxt.CurrencyMint.getTarget(currency.getMinDifficulty(), currency.getMaxDifficulty(), units,
                currency.getCurrentSupply(), currency.getTotalSupply())));
        return json;
    }

}
