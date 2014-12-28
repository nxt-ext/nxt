package nxt.http;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.CurrencyType;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestCurrencyIssuance extends BlockchainTest {

    @Test
    public void issueCurrency() {
        APICall apiCall = new Builder().build();
        issueCurrencyApi(apiCall);
    }

    @Test
    public void issueMultipleCurrencies() {
        APICall apiCall = new Builder().naming("aaa", "AAA", "Currency A").build();
        issueCurrencyApi(apiCall);
        apiCall = new Builder().naming("bbbb", "BBBB", "Currency B").feeNQT(1000 * Constants.ONE_NXT).build();
        issueCurrencyApi(apiCall);
        apiCall = new Builder().naming("ccccc", "CCCCC", "Currency C").feeNQT(40 * Constants.ONE_NXT).build();
        issueCurrencyApi(apiCall);
        apiCall = new APICall.Builder("getAllCurrencies").build();
        JSONObject response = apiCall.invoke();
        Logger.logDebugMessage(response.toJSONString());
        JSONArray currencies = (JSONArray)response.get("currencies");
        Assert.assertEquals(3, currencies.size());
    }

    static String issueCurrencyApi(APICall apiCall) {
        JSONObject issueCurrencyResponse = apiCall.invoke();
        String currencyId = (String) issueCurrencyResponse.get("transaction");
        Logger.logMessage("issueCurrencyResponse: " + issueCurrencyResponse.toJSONString());
        generateBlock();

        apiCall = new APICall.Builder("getCurrency").param("currency", currencyId).build();
        JSONObject getCurrencyResponse = apiCall.invoke();
        Logger.logMessage("getCurrencyResponse:" + getCurrencyResponse.toJSONString());
        Assert.assertEquals(currencyId, getCurrencyResponse.get("currency"));
        return currencyId;
    }

    public static class Builder extends APICall.Builder {

        public Builder() {
            super("issueCurrency");
            secretPhrase(secretPhrase1);
            feeNQT(0l);
            //feeNQT(25000 * Constants.ONE_NXT);
            param("name", "Test1");
            param("code", "TSXXX");
            param("description", "Test Currency 1");
            param("type", CurrencyType.EXCHANGEABLE.getCode());
            param("maxSupply", 100000);
            param("initialSupply", 100000);
            param("issuanceHeight", 0);
            param("minReservePerUnitNQT", 1);
            param("minDifficulty", (byte) 0);
            param("maxDifficulty", (byte) 0);
            param("algorithm", (byte)0);

        }

        public Builder naming(String name, String code, String description) {
            param("name", name);
            param("code", code).
            param("description", description);
            return this;
        }

        public Builder type(int type) {
            param("type", type);
            return this;
        }

        public Builder maxSupply(long maxSupply) {
            param("maxSupply", maxSupply);
            return this;
        }

        public Builder reserveSupply(long reserveSupply) {
            param("reserveSupply", reserveSupply);
            return this;
        }

        public Builder initialSupply(long initialSupply) {
            param("initialSupply", initialSupply);
            return this;
        }

        public Builder issuanceHeight(int issuanceHeight) {
            param("issuanceHeight", issuanceHeight);
            return this;
        }

        public Builder minReservePerUnitNQT(long minReservePerUnitNQT) {
            param("minReservePerUnitNQT", minReservePerUnitNQT);
            return this;
        }

        public Builder minting(byte minDifficulty, byte maxDifficulty, byte algorithm) {
            param("minDifficulty", minDifficulty);
            param("maxDifficulty", maxDifficulty);
            param("algorithm", algorithm);
            return this;
        }

    }
}
