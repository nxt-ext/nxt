package nxt.http;

import nxt.*;
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
        String currencyId = TestCurrencyIssuance.issueCurrencyImpl(CurrencyType.CROWD_FUNDING, baseHeight + 5, 0);
        reserveIncreaseImpl(currencyId);
    }

    @Test
    public void cancelCrowdFunding() {
        String currencyId = TestCurrencyIssuance.issueCurrencyImpl(CurrencyType.CROWD_FUNDING, baseHeight + 4, 11);
        long balanceNQT1 = Account.getAccount(Crypto.getPublicKey(secretPhrase1)).getBalanceNQT();
        long balanceNQT2 = Account.getAccount(Crypto.getPublicKey(secretPhrase2)).getBalanceNQT();
        reserveIncreaseImpl(currencyId);
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
        String currencyId = TestCurrencyIssuance.issueCurrencyImpl(CurrencyType.CROWD_FUNDING, baseHeight + 4, 10);
        long balanceNQT1 = Account.getAccount(Crypto.getPublicKey(secretPhrase1)).getBalanceNQT();
        long balanceNQT2 = Account.getAccount(Crypto.getPublicKey(secretPhrase2)).getBalanceNQT();
        reserveIncreaseImpl(currencyId);
        generateBlock(); // distribution of currency to founders
        Assert.assertEquals(20000, Account.getAccount(Crypto.getPublicKey(secretPhrase1)).getCurrencyBalanceQNT(Convert.parseAccountId(currencyId)));
        Assert.assertEquals(80000, Account.getAccount(Crypto.getPublicKey(secretPhrase2)).getCurrencyBalanceQNT(Convert.parseAccountId(currencyId)));
        Assert.assertEquals(balanceNQT1 - Constants.ONE_NXT - 200000, Account.getAccount(Crypto.getPublicKey(secretPhrase1)).getBalanceNQT());
        Assert.assertEquals(balanceNQT2 - 2*Constants.ONE_NXT - 800000, Account.getAccount(Crypto.getPublicKey(secretPhrase2)).getBalanceNQT());
    }

    public void reserveIncreaseImpl(String currencyId) {
        APICall apiCall = new APICall.Builder("currencyReserveIncrease").
                secretPhrase(secretPhrase1).
                feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                param("amountNQT", "" + 2).
                build();
        JSONObject reserveIncreaseResponse = apiCall.invoke();
        Logger.logMessage("reserveIncreaseResponse: " + reserveIncreaseResponse);
        generateBlock();

        // Two increase reserve transactions in the same block
        apiCall = new APICall.Builder("currencyReserveIncrease").
                secretPhrase(secretPhrase2).
                feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                param("amountNQT", "" + 3).
                build();
        reserveIncreaseResponse = apiCall.invoke();
        Logger.logMessage("reserveIncreaseResponse: " + reserveIncreaseResponse);

        apiCall = new APICall.Builder("currencyReserveIncrease").
                secretPhrase(secretPhrase2).
                feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                param("amountNQT", "" + 5).
                build();
        reserveIncreaseResponse = apiCall.invoke();
        Logger.logMessage("reserveIncreaseResponse: " + reserveIncreaseResponse);

        generateBlock();

        apiCall = new APICall.Builder("getCurrencyFounders").
                secretPhrase(secretPhrase2).
                feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                build();
        JSONObject getFoundersResponse = apiCall.invoke();
        Logger.logMessage("getFoundersResponse: " + getFoundersResponse);

        JSONArray founders = (JSONArray)getFoundersResponse.get("founders");
        JSONObject founder1 = (JSONObject)founders.get(0);
        Assert.assertEquals(Convert.toUnsignedLong(Account.getId(Crypto.getPublicKey(secretPhrase2))), founder1.get("account"));
        Assert.assertEquals(300000L + 500000L, founder1.get("value"));

        JSONObject founder2 = (JSONObject)founders.get(1);
        Assert.assertEquals(Convert.toUnsignedLong(Account.getAccount(Crypto.getPublicKey(secretPhrase1)).getId()), founder2.get("account"));
        Assert.assertEquals(200000L, founder2.get("value"));
    }

}
