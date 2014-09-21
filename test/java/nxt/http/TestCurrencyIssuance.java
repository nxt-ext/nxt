package nxt.http;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.Helper;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestCurrencyIssuance extends BlockchainTest {

    @Test
    public void issueCurrency() {
        issueCurrencyImpl();
    }

    public static String issueCurrencyImpl() {
        String secretPhrase = "hope peace happen touch easy pretend worthless talk them indeed wheel state";
        APICall apiCall = new APICall.Builder("issueCurrency").
                secretPhrase(secretPhrase).
                feeNQT("" + 1000 * Constants.ONE_NXT).
                param("name", "Test1").
                param("code", "TSX").
                param("code", "TSX").
                param("description", "Test Currency 1").
                param("type", "1").
                param("totalSupply", "100000").
                build();

        JSONObject issueCurrencyResponse = apiCall.invoke();
        String currencyId = (String) issueCurrencyResponse.get("transaction");
        System.out.println("issueCurrencyResponse: " + issueCurrencyResponse.toJSONString());
        Helper.generateBlock(forgerSecretPhrase);

        apiCall = new APICall.Builder("getCurrency").param("currency", currencyId).build();
        JSONObject getCurrencyResponse = apiCall.invoke();
        Logger.logDebugMessage("getCurrencyResponse:" + getCurrencyResponse.toJSONString());
        Assert.assertEquals(currencyId, getCurrencyResponse.get("currency"));
        Assert.assertEquals("TSX", getCurrencyResponse.get("code"));
        return currencyId;
    }
}
