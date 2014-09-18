package nxt.http;

import nxt.*;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.*;

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
        Helper.generateBlock(secretPhrase);

        apiCall = new APICall.Builder("getCurrency").
                param("currency", currencyId).
                build();

        JSONObject getCurrencyResponse = apiCall.invoke();
        System.out.println("getCurrencyResponse:" + getCurrencyResponse.toJSONString());
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
        Account buyerAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase2));
        long issuerStartBalanceNQT = issuerAccount.getBalanceNQT();
        long issuerStartCurrencyBalanceQNT = issuerAccount.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId));
        long buyerStartBalanceNQT = buyerAccount.getUnconfirmedBalanceNQT();
        Assert.assertEquals(100000, issuerStartCurrencyBalanceQNT);

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
        System.out.println("publishExchangeOfferResponse: " + publishExchangeOfferResponse.toJSONString());
        Helper.generateBlock(secretPhrase1);
        Helper.executeQuery("select * from buy_offer");
        Helper.executeQuery("select * from sell_offer");
        apiCall = new APICall.Builder("getAllOffers").build();
        JSONObject getAllOffersResponse = apiCall.invoke();
        System.out.println("getAllOffersResponse:" + getAllOffersResponse.toJSONString());
        JSONArray offer = (JSONArray)getAllOffersResponse.get("openOffers");
        Assert.assertEquals(publishExchangeOfferResponse.get("transaction"), ((JSONObject)offer.get(0)).get("offer"));
        Assert.assertEquals(issuerStartBalanceNQT, issuerAccount.getBalanceNQT()); // balance not reduced yet (why ? bug ?)
        Assert.assertEquals(issuerStartCurrencyBalanceQNT - 1000, issuerAccount.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId))); // currency balance reduced by initial supply

        apiCall = new APICall.Builder("currencyExchange").
        secretPhrase(secretPhrase2).feeNQT("" + Constants.ONE_NXT).
                param("currency", currencyId).
                param("rateNQT", "" + 96).
                param("units", "200").
                build();
        JSONObject currencyExchangeResponse = apiCall.invoke();
        System.out.println(currencyExchangeResponse);
        Helper.generateBlock(secretPhrase1);
        Assert.assertEquals(issuerStartBalanceNQT, issuerAccount.getBalanceNQT()); // not sure if this is correct
        Assert.assertEquals(buyerStartBalanceNQT, buyerAccount.getUnconfirmedBalanceNQT()); // not sure if this is correct
        Assert.assertEquals(issuerStartCurrencyBalanceQNT - 1000, issuerAccount.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId))); // not sure if this is correct
        Assert.assertEquals(202, buyerAccount.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId))); // not sure if this is correct
    }

    @After
    public void destroy() {
        APICall apiCall = new APICall.Builder("popOff").param("height", "" + baseHeight).build();
        JSONObject popOffResponse = apiCall.invoke();
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
