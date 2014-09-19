package nxt.http;

import nxt.*;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.*;

public class TestCurrency {

    static int baseHeight;
    static String forgerSecretPhrase = "aSykrgKGZNlSVOMDxkZZgbTvQqJPGtsBggb";

    @BeforeClass
    public static void init() {
        Nxt.init();
        Nxt.getBlockchainProcessor().addListener(new Helper.BlockListener(), BlockchainProcessor.Event.BLOCK_GENERATED);
        Nxt.setIsUnitTest(true);
        baseHeight = Nxt.getBlockchain().getHeight();
        Helper.executeQuery("select * from currency");
        Helper.executeQuery("select * from unconfirmed_transaction");
    }

    @Test
    public void issueCurrency() {
        issueCurrencyImpl();
    }

    public String issueCurrencyImpl() {
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

    @Test
    public void exchange() {
        String currencyId = issueCurrencyImpl();

        String secretPhrase1 = "hope peace happen touch easy pretend worthless talk them indeed wheel state";
        Account issuerAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase1));
        String secretPhrase2 = "rshw9abtpsa2";
        long issuerStartBalanceNQT = issuerAccount.getBalanceNQT();
        long issuerStartCurrencyBalanceQNT = issuerAccount.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId));

        Account buyerAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase2));
        long buyerStartBalanceNQT = buyerAccount.getBalanceNQT();
        Logger.logDebugMessage("buyerStartBalanceNQT = " + buyerStartBalanceNQT);
        Assert.assertEquals(100000, issuerStartCurrencyBalanceQNT);

        long forgerStartBalance = Account.getAccount(Crypto.getPublicKey(forgerSecretPhrase)).getBalanceNQT();

        APICall apiCall = new APICall.Builder("publishExchangeOffer").
                secretPhrase(secretPhrase1).feeNQT("" + Constants.ONE_NXT).
                param("requestType", "publishExchangeOffer").
                param("secretPhrase", secretPhrase1).
                param("deadline", "1440").
                param("feeNQT", "" + Constants.ONE_NXT).
                param("currency", currencyId).
                param("buyRateNQT", "" + 105).
                param("sellRateNQT", "" + 95).
                param("totalBuyLimit", "10000").
                param("totalSellLimit", "10000").
                param("initialBuySupply", "1000").
                param("initialSellSupply", "1000").
                param("expirationHeight", "" + Integer.MAX_VALUE).
                build();

        JSONObject publishExchangeOfferResponse = apiCall.invoke();
        Logger.logDebugMessage("publishExchangeOfferResponse: " + publishExchangeOfferResponse.toJSONString());
        Helper.generateBlock(forgerSecretPhrase);
        apiCall = new APICall.Builder("getAllOffers").build();
        JSONObject getAllOffersResponse = apiCall.invoke();
        Logger.logDebugMessage("getAllOffersResponse:" + getAllOffersResponse.toJSONString());
        JSONArray offer = (JSONArray)getAllOffersResponse.get("openOffers");
        Assert.assertEquals(publishExchangeOfferResponse.get("transaction"), ((JSONObject)offer.get(0)).get("offer"));
        issuerAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase1));
        Assert.assertEquals(issuerStartBalanceNQT - 1000 * 105 - Constants.ONE_NXT, issuerAccount.getUnconfirmedBalanceNQT());
        Assert.assertEquals(issuerStartBalanceNQT - Constants.ONE_NXT, issuerAccount.getBalanceNQT());
        Assert.assertEquals(issuerStartCurrencyBalanceQNT - 1000, issuerAccount.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId))); // currency balance reduced by initial supply

        apiCall = new APICall.Builder("currencyExchange").
        secretPhrase(secretPhrase2).feeNQT("" + Constants.ONE_NXT).
                param("currency", currencyId).
                param("rateNQT", "" + 96).
                param("units", "200").
                build();
        JSONObject currencyExchangeResponse = apiCall.invoke();
        Logger.logDebugMessage("currencyExchangeResponse:" + currencyExchangeResponse);
        Helper.generateBlock(forgerSecretPhrase);

        issuerAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase1));
        Assert.assertEquals(issuerStartBalanceNQT - 1000 * 105 - Constants.ONE_NXT, issuerAccount.getUnconfirmedBalanceNQT());
        Assert.assertEquals(issuerStartBalanceNQT + 202 * 95 - Constants.ONE_NXT, issuerAccount.getBalanceNQT());
        Assert.assertEquals(100000 - 1000, issuerAccount.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId)));
        Assert.assertEquals(100000 - 202, issuerAccount.getCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId)));

        buyerAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase2));
        Assert.assertEquals(buyerStartBalanceNQT - 202 * 95 - Constants.ONE_NXT, buyerAccount.getUnconfirmedBalanceNQT());
        Assert.assertEquals(buyerStartBalanceNQT - 202 * 95 - Constants.ONE_NXT, buyerAccount.getBalanceNQT());
        Assert.assertEquals(202, buyerAccount.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId))); // not sure if this is correct
        Assert.assertEquals(202, buyerAccount.getCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId))); // not sure if this is correct

        Account forgerAccount = Account.getAccount(Crypto.getPublicKey(forgerSecretPhrase));
        Assert.assertEquals(forgerStartBalance + 2 * Constants.ONE_NXT, forgerAccount.getUnconfirmedBalanceNQT());
        Assert.assertEquals(forgerStartBalance + 2 * Constants.ONE_NXT, forgerAccount.getBalanceNQT());
    }

    @After
    public void destroy() {
        APICall apiCall = new APICall.Builder("popOff").param("height", "" + baseHeight).build();
        JSONObject popOffResponse = apiCall.invoke();
        Logger.logDebugMessage("popOffResponse:" + popOffResponse.toJSONString());
        Helper.executeQuery("select * from currency");
        Helper.executeQuery("select * from unconfirmed_transaction");
        Nxt.getTransactionProcessor().shutdown();
    }

    @AfterClass
    public static void shutdown() {
        Helper.executeQuery("select * from currency");
        Helper.executeQuery("select * from unconfirmed_transaction");
        Nxt.shutdown();
    }

}
