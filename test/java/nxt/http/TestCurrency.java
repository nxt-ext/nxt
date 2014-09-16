package nxt.http;

import nxt.*;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.*;

import java.util.HashMap;
import java.util.Map;

public class TestCurrency {

    static int baseHeight;

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
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("requestType", "issueCurrency");
        String secretPhrase = "hope peace happen touch easy pretend worthless talk them indeed wheel state";
        reqParams.put("secretPhrase", secretPhrase);
        reqParams.put("deadline", "1440");
        reqParams.put("feeNQT", "100000000000");
        reqParams.put("name", "Test1");
        reqParams.put("code", "TSX");
        reqParams.put("description", "Test Currency 1");
        reqParams.put("type", "1");
        reqParams.put("totalSupply", "100000");

        JSONObject issueCurrencyResponse = (JSONObject) APIHelper.processRequest(reqParams);
        String currencyId = (String) issueCurrencyResponse.get("transaction");
        System.out.println("issueCurrencyResponse: " + issueCurrencyResponse.toJSONString());
        Helper.generateBlock(secretPhrase);
        reqParams.clear();
        reqParams.put("requestType", "getCurrency");
        reqParams.put("currency", currencyId);
        JSONObject getCurrencyResponse = (JSONObject) APIHelper.processRequest(reqParams);
        System.out.println("getCurrencyResponse:" + getCurrencyResponse.toJSONString());
        Assert.assertEquals(currencyId, getCurrencyResponse.get("currency"));
        Assert.assertEquals("TSX", getCurrencyResponse.get("code"));
        return currencyId;
    }

    @Test
    public void exchange() {
        String currencyId = issueCurrencyImpl();

        String secretPhrase = "hope peace happen touch easy pretend worthless talk them indeed wheel state";
        Account account = Account.getAccount(Crypto.getPublicKey(secretPhrase));
        long unconfirmedBalanceNQT = account.getUnconfirmedBalanceNQT();
        Assert.assertEquals(409700000000L, unconfirmedBalanceNQT);
        long unconfirmedCurrencyBalanceQNT = account.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId));
        Assert.assertEquals(100000, unconfirmedCurrencyBalanceQNT);

        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("requestType", "publishExchangeOffer");
        reqParams.put("secretPhrase", secretPhrase);
        reqParams.put("deadline", "1440");
        reqParams.put("feeNQT", "" + Constants.ONE_NXT);
        reqParams.put("currency", currencyId);
        reqParams.put("buyRateNQT", "" + 10 * Constants.ONE_NXT);
        reqParams.put("sellRateNQT", "" + 20 * Constants.ONE_NXT);
        reqParams.put("totalBuyLimit", "30");
        reqParams.put("totalSellLimit", "40");
        reqParams.put("initialBuySupply", "50");
        reqParams.put("initialSellSupply", "60");
        reqParams.put("expirationHeight", "130000");

        JSONObject publishExchangeOfferResponse = (JSONObject) APIHelper.processRequest(reqParams);
        System.out.println("publishExchangeOfferResponse: " + publishExchangeOfferResponse.toJSONString());
        Helper.generateBlock(secretPhrase);
        Helper.executeQuery("select * from buy_offer");
        Helper.executeQuery("select * from sell_offer");
        reqParams.clear();
        reqParams.put("requestType", "getAllOffers");
        JSONObject getAllOffersResponse = (JSONObject) APIHelper.processRequest(reqParams);
        System.out.println("getAllOffersResponse:" + getAllOffersResponse.toJSONString());
        JSONArray offer = (JSONArray)getAllOffersResponse.get("openOffers");
        Assert.assertEquals(publishExchangeOfferResponse.get("transaction"), ((JSONObject) offer.get(0)).get("offer"));
        unconfirmedBalanceNQT = account.getUnconfirmedBalanceNQT();
        Assert.assertEquals(409700000000L, unconfirmedBalanceNQT); // not sure if this is correct
        unconfirmedCurrencyBalanceQNT = account.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId));
        Assert.assertEquals(99940, unconfirmedCurrencyBalanceQNT); // not sure if this is correct

        reqParams.clear();
        reqParams.put("requestType", "currencyExchange");
        reqParams.put("secretPhrase", secretPhrase);
        reqParams.put("deadline", "1440");
        reqParams.put("feeNQT", "" + Constants.ONE_NXT);
        reqParams.put("currency", currencyId);
        reqParams.put("rateNQT", "" + 10 * Constants.ONE_NXT);
        reqParams.put("units", "20");
        JSONObject currencyExchangeResponse = (JSONObject) APIHelper.processRequest(reqParams);
        System.out.println(currencyExchangeResponse);
        Helper.generateBlock(secretPhrase);
        unconfirmedBalanceNQT = account.getUnconfirmedBalanceNQT();
        Assert.assertEquals(409700000000L, unconfirmedBalanceNQT); // not sure if this is correct
        unconfirmedCurrencyBalanceQNT = account.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId));
        Assert.assertEquals(99940, unconfirmedCurrencyBalanceQNT); // not sure if this is correct
    }

    @After
    public void destroy() {
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("requestType", "popOff");
        reqParams.put("height", "" + baseHeight);
        JSONObject popOffResponse = (JSONObject) APIHelper.processRequest(reqParams);
        System.out.println("popOffResponse:" + popOffResponse.toJSONString());
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
