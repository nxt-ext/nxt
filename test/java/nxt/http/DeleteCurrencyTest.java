package nxt.http;

import nxt.Account;
import nxt.BlockchainTest;
import nxt.Constants;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class DeleteCurrencyTest extends BlockchainTest {

    @Test
    public void deleteByIssuer() {
        APICall apiCall = new TestCurrencyIssuance.Builder().naming("yjwcv", "YJWCV", "test1").build();
        TestCurrencyIssuance.issueCurrencyApi(apiCall);
        generateBlock();
        apiCall = new APICall.Builder("getCurrency").param("code", "YJWCV").build();
        JSONObject response = apiCall.invoke();
        String currencyId = (String)response.get("currency");
        String code = (String)response.get("code");

        // Delete the currency
        apiCall = new APICall.Builder("deleteCurrency").
                secretPhrase(secretPhrase1).feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                build();
        response = apiCall.invoke();
        Logger.logDebugMessage("deleteCurrencyResponse:" + response);
        generateBlock();
        apiCall = new APICall.Builder("getCurrency").param("code", code).build();
        response = apiCall.invoke();
        Assert.assertEquals((long)5, response.get("errorCode"));
        Assert.assertEquals("Unknown currency", response.get("errorDescription"));

        // Issue the same currency code again
        apiCall = new TestCurrencyIssuance.Builder().naming("yjwcv", "YJWCV", "test1").build();
        TestCurrencyIssuance.issueCurrencyApi(apiCall);
        generateBlock();
        apiCall = new APICall.Builder("getCurrency").param("code", "YJWCV").build();
        response = apiCall.invoke();
        String newCurrencyId = (String)response.get("currency");
        String newCode = (String)response.get("code");
        Assert.assertNotEquals(currencyId, newCurrencyId); // this check may fail once in 2^64 tests
        Assert.assertEquals(code, newCode);
    }

    @Test
    public void deleteByNonOwnerNotAllowed() {
        APICall apiCall = new TestCurrencyIssuance.Builder().naming("yjwcv", "YJWCV", "test1").build();
        TestCurrencyIssuance.issueCurrencyApi(apiCall);
        generateBlock();
        apiCall = new APICall.Builder("getAllCurrencies").build();
        JSONObject response = apiCall.invoke();
        JSONArray currencies = (JSONArray) response.get("currencies");
        String currencyId = (String)((JSONObject)currencies.get(0)).get("currency");
        String code = (String)((JSONObject)currencies.get(0)).get("code");

        // Delete the currency
        apiCall = new APICall.Builder("deleteCurrency").
                secretPhrase(secretPhrase2).feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                build();
        response = apiCall.invoke();
        Logger.logDebugMessage("deleteCurrencyResponse:" + response);
        Assert.assertEquals((long)8, response.get("errorCode"));

        // Verify that currency still exists
        apiCall = new APICall.Builder("getCurrency").param("code", code).build();
        response = apiCall.invoke();
        Assert.assertEquals(currencyId, response.get("currency"));
    }

    @Test
    public void deleteByOwnerNonIssuer() {
        APICall apiCall = new TestCurrencyIssuance.Builder().naming("yjwcv", "YJWCV", "test1").build();
        TestCurrencyIssuance.issueCurrencyApi(apiCall);
        generateBlock();

        apiCall = new APICall.Builder("getCurrency").param("code", "YJWCV").build();
        JSONObject response = apiCall.invoke();
        String currencyId = (String)response.get("currency");
        String code = (String)response.get("code");

        // Transfer all units
        apiCall = new APICall.Builder("transferCurrency").
                secretPhrase(secretPhrase1).feeNQT(Constants.ONE_NXT).
                param("recipient", Convert.toUnsignedLong(Account.getAccount(Crypto.getPublicKey(secretPhrase2)).getId())).
                param("currency", currencyId).
                param("code", code).
                param("units", (String)response.get("maxSupply")).
                build();
        response = apiCall.invoke();
        Logger.logDebugMessage("transferCurrencyResponse:" + response);
        generateBlock();

        // Delete the currency
        apiCall = new APICall.Builder("deleteCurrency").
                secretPhrase(secretPhrase2).feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                build();
        response = apiCall.invoke();
        Logger.logDebugMessage("deleteCurrencyResponse:" + response);
        generateBlock();
        apiCall = new APICall.Builder("getCurrency").param("code", code).build();
        response = apiCall.invoke();
        Assert.assertEquals((long)5, response.get("errorCode"));
        Assert.assertEquals("Unknown currency", response.get("errorDescription"));

        // Issue the same currency code again by the original issuer
        apiCall = new TestCurrencyIssuance.Builder().naming("yjwcv", "YJWCV", "test1").build();
        TestCurrencyIssuance.issueCurrencyApi(apiCall);
        generateBlock();
        apiCall = new APICall.Builder("getCurrency").param("code", "YJWCV").build();
        response = apiCall.invoke();
        String newCurrencyId = (String)response.get("currency");
        String newCode = (String)response.get("code");
        Assert.assertNotEquals(currencyId, newCurrencyId); // this check may fail once in 2^64 tests
        Assert.assertEquals(code, newCode);
    }

}
