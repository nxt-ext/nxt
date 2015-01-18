package nxt.http;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.CurrencyMinting;
import nxt.CurrencyType;
import nxt.crypto.HashFunction;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestCurrencyMint extends BlockchainTest {

    @Test
    public void mint() {
        APICall apiCall = new TestCurrencyIssuance.Builder().
                type(CurrencyType.MINTABLE.getCode() | CurrencyType.EXCHANGEABLE.getCode()).
                maxSupply((long)10000000).
                initialSupply((long)0).
                issuanceHeight(0).
                minting((byte)2, (byte)8, HashFunction.SHA256.getId()).
                build();

        String currencyId = TestCurrencyIssuance.issueCurrencyApi(apiCall);
        mintCurrency(currencyId);
    }

    public void mintCurrency(String currencyId) {
        // Failed attempt to mint
        APICall apiCall = new APICall.Builder("currencyMint").
                secretPhrase(secretPhrase1).
                feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                param("nonce", 123456).
                param("units", 1000).
                param("counter", 1).
                build();
        JSONObject mintResponse = apiCall.invoke();
        Logger.logDebugMessage("mintResponse: " + mintResponse);
        generateBlock();
        apiCall = new APICall.Builder("getCurrency").
                secretPhrase(secretPhrase1).
                feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                build();
        JSONObject getCurrencyResponse = apiCall.invoke();
        Logger.logDebugMessage("getCurrencyResponse: " + getCurrencyResponse);
        Assert.assertEquals("0", getCurrencyResponse.get("currentSupply"));

        // Successful attempt
        long units = 10;
        long algorithm = (Long)getCurrencyResponse.get("algorithm");
        long nonce;
        for (nonce=0; nonce < Long.MAX_VALUE; nonce++) {
            if (CurrencyMinting.meetsTarget(CurrencyMinting.getHash((byte) algorithm, nonce, Convert.parseUnsignedLong(currencyId), units, 1, id1),
                    CurrencyMinting.getTarget(2, 8, units, 0, 100000))) {
                break;
            }
        }
        Logger.logDebugMessage("nonce: " + nonce);
        apiCall = new APICall.Builder("currencyMint").
                secretPhrase(secretPhrase1).
                feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                param("nonce", nonce).
                param("units", units).
                param("counter", 1).
                build();
        mintResponse = apiCall.invoke();
        Logger.logDebugMessage("mintResponse: " + mintResponse);
        generateBlock();
        apiCall = new APICall.Builder("getCurrency").
                secretPhrase(secretPhrase1).
                feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                build();
        getCurrencyResponse = apiCall.invoke();
        Logger.logDebugMessage("getCurrencyResponse: " + getCurrencyResponse);
        Assert.assertEquals("" + units, getCurrencyResponse.get("currentSupply"));

        apiCall = new APICall.Builder("getMintingTarget").
                param("currency", currencyId).
                param("units", "1000").
                build();
        JSONObject getMintingTargetResponse = apiCall.invoke();
        Logger.logDebugMessage("getMintingTargetResponse: " + getMintingTargetResponse);
        Assert.assertEquals((long)4000, getMintingTargetResponse.get("difficulty"));
        Assert.assertEquals("a9f1d24d62105839b4c876be9f1a2fdd24068195438b6ce7fba9f1d24d621000", getMintingTargetResponse.get("targetBytes"));
    }
}
