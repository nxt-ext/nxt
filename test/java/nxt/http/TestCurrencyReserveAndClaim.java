package nxt.http;

import nxt.Account;
import nxt.BlockchainTest;
import nxt.Constants;
import nxt.CurrencyType;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestCurrencyReserveAndClaim extends BlockchainTest {

    @Test
    public void reserveIncrease() {
        APICall apiCall = new TestCurrencyIssuance.Builder().
                type(CurrencyType.RESERVABLE.getCode() | CurrencyType.EXCHANGEABLE.getCode()).
                issuanceHeight(baseHeight + 5).
                minReservePerUnitNQT((long) 1).
                initialSupply((long)0).
                reserveSupply((long)100000).
                build();
        String currencyId = TestCurrencyIssuance.issueCurrencyApi(apiCall);
        reserveIncreaseImpl(currencyId, secretPhrase1, secretPhrase2);
    }

    @Test
    public void cancelCrowdFunding() {
        APICall apiCall1 = new TestCurrencyIssuance.Builder().
                type(CurrencyType.RESERVABLE.getCode() | CurrencyType.EXCHANGEABLE.getCode()).
                issuanceHeight(baseHeight + 4).
                minReservePerUnitNQT((long) 11).
                initialSupply((long)0).
                reserveSupply((long)100000).
                build();
        String currencyId = TestCurrencyIssuance.issueCurrencyApi(apiCall1);
        long balanceNQT1 = Account.getAccount(Crypto.getPublicKey(secretPhrase1)).getBalanceNQT();
        long balanceNQT2 = Account.getAccount(Crypto.getPublicKey(secretPhrase2)).getBalanceNQT();
        reserveIncreaseImpl(currencyId, secretPhrase1, secretPhrase2);
        generateBlock(); // cancellation of crowd funding because of insufficient funds
        APICall apiCall = new APICall.Builder("getCurrencyFounders").
                secretPhrase(secretPhrase2).
                feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                build();
        JSONObject getFoundersResponse = apiCall.invoke();
        Logger.logMessage("getFoundersResponse: " + getFoundersResponse);
        Assert.assertEquals(5L, getFoundersResponse.get("errorCode"));
        Assert.assertEquals("Unknown currency", getFoundersResponse.get("errorDescription"));
        Assert.assertEquals(balanceNQT1 - Constants.ONE_NXT, Account.getAccount(Crypto.getPublicKey(secretPhrase1)).getBalanceNQT());
        Assert.assertEquals(balanceNQT2 - 2*Constants.ONE_NXT, Account.getAccount(Crypto.getPublicKey(secretPhrase2)).getBalanceNQT());

    }

    @Test
    public void crowdFundingDistribution() {
        APICall apiCall = new TestCurrencyIssuance.Builder().
                type(CurrencyType.RESERVABLE.getCode() | CurrencyType.EXCHANGEABLE.getCode()).
                initialSupply((long) 0).
                reserveSupply((long) 100000).
                issuanceHeight(baseHeight + 4).
                minReservePerUnitNQT((long) 10).
                build();

        String currencyId = TestCurrencyIssuance.issueCurrencyApi(apiCall);
        long balanceNQT1 = Account.getAccount(Crypto.getPublicKey(secretPhrase1)).getBalanceNQT();
        long balanceNQT2 = Account.getAccount(Crypto.getPublicKey(secretPhrase2)).getBalanceNQT();
        reserveIncreaseImpl(currencyId, secretPhrase1, secretPhrase2);
        generateBlock(); // distribution of currency to founders
        Assert.assertEquals(20000, Account.getAccount(Crypto.getPublicKey(secretPhrase1)).getCurrencyUnits(Convert.parseAccountId(currencyId)));
        Assert.assertEquals(80000, Account.getAccount(Crypto.getPublicKey(secretPhrase2)).getCurrencyUnits(Convert.parseAccountId(currencyId)));
        Assert.assertEquals(balanceNQT1 - Constants.ONE_NXT - 200000 + (100000*10), Account.getAccount(Crypto.getPublicKey(secretPhrase1)).getBalanceNQT());
        Assert.assertEquals(balanceNQT2 - 2*Constants.ONE_NXT - 800000, Account.getAccount(Crypto.getPublicKey(secretPhrase2)).getBalanceNQT());
    }

    @Test
    public void crowdFundingDistributionRounding() {
        APICall apiCall = new TestCurrencyIssuance.Builder().
                type(CurrencyType.RESERVABLE.getCode() | CurrencyType.EXCHANGEABLE.getCode()).
                initialSupply((long)0).
                reserveSupply((long)24).
                maxSupply((long) 24).
                issuanceHeight(baseHeight + 4).
                minReservePerUnitNQT((long) 10).
                build();

        String currencyId = TestCurrencyIssuance.issueCurrencyApi(apiCall);
        long balanceNQT1 = Account.getAccount(Crypto.getPublicKey(secretPhrase1)).getBalanceNQT();
        long balanceNQT2 = Account.getAccount(Crypto.getPublicKey(secretPhrase2)).getBalanceNQT();
        long balanceNQT3 = Account.getAccount(Crypto.getPublicKey(secretPhrase3)).getBalanceNQT();
        reserveIncreaseImpl(currencyId, secretPhrase2, secretPhrase3);
        generateBlock(); // distribution of currency to founders

        // account 2 balance round(24 * 0.2) = round(4.8) = 4
        // account 3 balance round(24 * 0.8) = round(19.2) = 19
        // issuer receives the leftover of 1
        Assert.assertEquals(4, Account.getAccount(Crypto.getPublicKey(secretPhrase2)).getCurrencyUnits(Convert.parseAccountId(currencyId)));
        Assert.assertEquals(19, Account.getAccount(Crypto.getPublicKey(secretPhrase3)).getCurrencyUnits(Convert.parseAccountId(currencyId)));
        Assert.assertEquals(1, Account.getAccount(Crypto.getPublicKey(secretPhrase1)).getCurrencyUnits(Convert.parseAccountId(currencyId)));
        Assert.assertEquals(balanceNQT1 + 24 * 10, Account.getAccount(Crypto.getPublicKey(secretPhrase1)).getBalanceNQT());
        Assert.assertEquals(balanceNQT2 - Constants.ONE_NXT - 24 * 2, Account.getAccount(Crypto.getPublicKey(secretPhrase2)).getBalanceNQT());
        Assert.assertEquals(balanceNQT3 - 2 * Constants.ONE_NXT - 24 * 8, Account.getAccount(Crypto.getPublicKey(secretPhrase3)).getBalanceNQT());

        apiCall = new APICall.Builder("getCurrency").
                param("currency", currencyId).
                build();
        JSONObject response = apiCall.invoke();
        Assert.assertEquals("24", response.get("currentSupply"));
    }

    public void reserveIncreaseImpl(String currencyId, String secret1, String secret2) {
        APICall apiCall = new APICall.Builder("currencyReserveIncrease").
                secretPhrase(secret1).
                feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                param("amountPerUnitNQT", "" + 2).
                build();
        JSONObject reserveIncreaseResponse = apiCall.invoke();
        Logger.logMessage("reserveIncreaseResponse: " + reserveIncreaseResponse);
        generateBlock();

        // Two increase reserve transactions in the same block
        apiCall = new APICall.Builder("currencyReserveIncrease").
                secretPhrase(secret2).
                feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                param("amountPerUnitNQT", "" + 3).
                build();
        reserveIncreaseResponse = apiCall.invoke();
        Logger.logMessage("reserveIncreaseResponse: " + reserveIncreaseResponse);

        apiCall = new APICall.Builder("currencyReserveIncrease").
                secretPhrase(secret2).
                feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                param("amountPerUnitNQT", "" + 5).
                build();
        reserveIncreaseResponse = apiCall.invoke();
        Logger.logMessage("reserveIncreaseResponse: " + reserveIncreaseResponse);

        generateBlock();

        apiCall = new APICall.Builder("getCurrencyFounders").
                feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                build();
        JSONObject getFoundersResponse = apiCall.invoke();
        Logger.logMessage("getFoundersResponse: " + getFoundersResponse);

        JSONArray founders = (JSONArray)getFoundersResponse.get("founders");
        JSONObject founder1 = (JSONObject)founders.get(0);
        Assert.assertTrue(Convert.toUnsignedLong(Account.getId(Crypto.getPublicKey(secret1))).equals(founder1.get("account")) ||
                Convert.toUnsignedLong(Account.getId(Crypto.getPublicKey(secret2))).equals(founder1.get("account")));
        Assert.assertTrue(String.valueOf(3L + 5L).equals(founder1.get("amountPerUnitNQT")) || String.valueOf(2L).equals(founder1.get("amountPerUnitNQT")));

        JSONObject founder2 = (JSONObject)founders.get(1);
        Assert.assertTrue(Convert.toUnsignedLong(Account.getId(Crypto.getPublicKey(secret1))).equals(founder2.get("account")) ||
                Convert.toUnsignedLong(Account.getId(Crypto.getPublicKey(secret2))).equals(founder2.get("account")));
        Assert.assertTrue(String.valueOf(3L + 5L).equals(founder2.get("amountPerUnitNQT")) || String.valueOf(2L).equals(founder2.get("amountPerUnitNQT")));
    }

}
