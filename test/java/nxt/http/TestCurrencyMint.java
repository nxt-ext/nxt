package nxt.http;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.CurrencyType;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.Test;

public class TestCurrencyMint extends BlockchainTest {

    @Test
    public void mint() {
        String currencyId = TestCurrencyIssuance.issueCurrencyImpl(CurrencyType.MINTABLE, baseHeight, 0,
                10000000, (byte)16, (byte)32);
        mintCurrency(currencyId);
    }

    public void mintCurrency(String currencyId) {
        APICall apiCall = new APICall.Builder("currencyMint").
                secretPhrase(secretPhrase1).
                feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                param("nonce", 123456).
                param("units", 10).
                param("counter", 1).
                build();
        JSONObject mintResponse = apiCall.invoke();
        Logger.logDebugMessage("mintResponse: " + mintResponse);
        generateBlock();
    }

}
