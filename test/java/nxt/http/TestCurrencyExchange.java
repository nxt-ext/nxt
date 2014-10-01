package nxt.http;

import nxt.*;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestCurrencyExchange extends BlockchainTest {

    @Test
    public void exchangeNxtToCurrency() {
        String currencyId = TestCurrencyIssuance.issueCurrencyImpl();

        Account issuerAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase1));
        long issuerStartBalanceNQT = issuerAccount.getBalanceNQT();
        long issuerStartCurrencyBalanceQNT = issuerAccount.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId));

        Account buyerAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase2));
        long buyerStartBalanceNQT = buyerAccount.getBalanceNQT();
        Logger.logDebugMessage("buyerStartBalanceNQT = " + buyerStartBalanceNQT);
        Assert.assertEquals(100000, issuerStartCurrencyBalanceQNT);

        long forgerStartBalance = Account.getAccount(Crypto.getPublicKey(forgerSecretPhrase)).getBalanceNQT();

        JSONObject publishExchangeOfferResponse = publishExchangeOffer(currencyId, secretPhrase1);

        generateBlock();

        APICall apiCall = new APICall.Builder("getAllOffers").build();
        JSONObject getAllOffersResponse = apiCall.invoke();
        Logger.logDebugMessage("getAllOffersResponse:" + getAllOffersResponse.toJSONString());
        JSONArray offer = (JSONArray)getAllOffersResponse.get("openOffers");
        Assert.assertEquals(publishExchangeOfferResponse.get("transaction"), ((JSONObject)offer.get(0)).get("offer"));
        issuerAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase1));
        Assert.assertEquals(issuerStartBalanceNQT - 1000 * 105 - Constants.ONE_NXT, issuerAccount.getUnconfirmedBalanceNQT());
        Assert.assertEquals(issuerStartBalanceNQT - Constants.ONE_NXT, issuerAccount.getBalanceNQT());
        Assert.assertEquals(issuerStartCurrencyBalanceQNT - 1000, issuerAccount.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId))); // currency balance reduced by initial supply

        apiCall = new APICall.Builder("currencyExchange").
                secretPhrase(secretPhrase2).feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                param("rateNQT", "" + 96).
                param("units", "200").
                build();
        JSONObject currencyExchangeResponse = apiCall.invoke();
        Logger.logDebugMessage("currencyExchangeResponse:" + currencyExchangeResponse);
        generateBlock();

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

        apiCall = new APICall.Builder("getAllExchanges").build();
        JSONObject getAllExchangesResponse = apiCall.invoke();
        Logger.logDebugMessage("getAllExchangesResponse: " + getAllExchangesResponse);
        JSONArray exchanges = (JSONArray)getAllExchangesResponse.get("exchanges");
        JSONObject exchange = (JSONObject) exchanges.get(0);
        Assert.assertEquals("95", exchange.get("rateNQT"));
        Assert.assertEquals("202", exchange.get("units"));
        Assert.assertEquals(currencyId, exchange.get("currency"));
        Assert.assertEquals(issuerAccount.getId(), Convert.parseUnsignedLong((String)exchange.get("seller")));
        Assert.assertEquals(buyerAccount.getId(), Convert.parseUnsignedLong((String)exchange.get("buyer")));
    }

    @Test
    public void exchangeCurrencyToNxt() {
        String currencyId = TestCurrencyIssuance.issueCurrencyImpl();

        Account issuerAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase1));
        long issuerStartBalanceNQT = issuerAccount.getBalanceNQT();
        long issuerStartUnconfirmedBalanceNQT = issuerAccount.getUnconfirmedBalanceNQT();
        long issuerStartCurrencyBalanceQNT = issuerAccount.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId));

        Account buyerAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase2));
        long buyerStartBalanceNQT = buyerAccount.getBalanceNQT();
        Logger.logDebugMessage("buyerStartBalanceNQT = " + buyerStartBalanceNQT);
        Assert.assertEquals(100000, issuerStartCurrencyBalanceQNT);

        long forgerStartBalance = Account.getAccount(Crypto.getPublicKey(forgerSecretPhrase)).getBalanceNQT();

        JSONObject publishExchangeOfferResponse = publishExchangeOffer(currencyId, secretPhrase1);
        generateBlock();

        APICall apiCall = new APICall.Builder("getAllOffers").build();
        JSONObject getAllOffersResponse = apiCall.invoke();
        Logger.logDebugMessage("getAllOffersResponse:" + getAllOffersResponse.toJSONString());
        JSONArray offer = (JSONArray)getAllOffersResponse.get("openOffers");
        Assert.assertEquals(publishExchangeOfferResponse.get("transaction"), ((JSONObject)offer.get(0)).get("offer"));
        issuerAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase1));
        Assert.assertEquals(issuerStartUnconfirmedBalanceNQT - 1000 * 105 - Constants.ONE_NXT, issuerAccount.getUnconfirmedBalanceNQT());
        Assert.assertEquals(issuerStartBalanceNQT - Constants.ONE_NXT, issuerAccount.getBalanceNQT());
        Assert.assertEquals(issuerStartCurrencyBalanceQNT - 1000, issuerAccount.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId))); // currency balance reduced by initial supply

        apiCall = new APICall.Builder("transferCurrency").
                secretPhrase(secretPhrase1).feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                param("recipient", Convert.toUnsignedLong(buyerAccount.getId())).
                param("units", "2000").
                build();
        apiCall.invoke();
        generateBlock();
        issuerAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase1));
        Assert.assertEquals(100000 - 2000 - 1000, issuerAccount.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId)));
        Assert.assertEquals(100000 - 2000, issuerAccount.getCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId)));
        Assert.assertEquals(2000, buyerAccount.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId)));
        Assert.assertEquals(2000, buyerAccount.getCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId)));

        apiCall = new APICall.Builder("currencyExchange").
                secretPhrase(secretPhrase2).feeNQT(Constants.ONE_NXT).
                param("currency", currencyId).
                param("rateNQT", "" + 104).
                param("units", "-200").
                build();
        JSONObject currencyExchangeResponse = apiCall.invoke();
        Logger.logDebugMessage("currencyExchangeResponse:" + currencyExchangeResponse);
        generateBlock();

        issuerAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase1));
        Assert.assertEquals(-1000 * 105, issuerAccount.getUnconfirmedBalanceNQT() - issuerStartBalanceNQT +  2*Constants.ONE_NXT);
        Assert.assertEquals(-200 * 105, issuerAccount.getBalanceNQT() - issuerStartBalanceNQT +  2*Constants.ONE_NXT);
        Assert.assertEquals(100000 - 1000 - 2000, issuerAccount.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId)));
        Assert.assertEquals(100000 - 2000 + 200, issuerAccount.getCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId)));

        buyerAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase2));
        Assert.assertEquals(buyerStartBalanceNQT + 200 * 105 - Constants.ONE_NXT, buyerAccount.getUnconfirmedBalanceNQT());
        Assert.assertEquals(buyerStartBalanceNQT + 200 * 105 - Constants.ONE_NXT, buyerAccount.getBalanceNQT());
        Assert.assertEquals(1800, buyerAccount.getUnconfirmedCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId))); // not sure if this is correct
        Assert.assertEquals(1800, buyerAccount.getCurrencyBalanceQNT(Convert.parseUnsignedLong(currencyId))); // not sure if this is correct

        Account forgerAccount = Account.getAccount(Crypto.getPublicKey(forgerSecretPhrase));
        Assert.assertEquals(forgerStartBalance + 3 * Constants.ONE_NXT, forgerAccount.getUnconfirmedBalanceNQT());
        Assert.assertEquals(forgerStartBalance + 3 * Constants.ONE_NXT, forgerAccount.getBalanceNQT());

        apiCall = new APICall.Builder("getAllExchanges").build();
        JSONObject getAllExchangesResponse = apiCall.invoke();
        Logger.logDebugMessage("getAllExchangesResponse: " + getAllExchangesResponse);
        JSONArray exchanges = (JSONArray)getAllExchangesResponse.get("exchanges");
        JSONObject exchange = (JSONObject) exchanges.get(0);
        Assert.assertEquals("105", exchange.get("rateNQT"));
        Assert.assertEquals("200", exchange.get("units"));
        Assert.assertEquals(currencyId, exchange.get("currency"));
        Assert.assertEquals(buyerAccount.getId(), Convert.parseUnsignedLong((String)exchange.get("seller")));
        Assert.assertEquals(issuerAccount.getId(), Convert.parseUnsignedLong((String)exchange.get("buyer")));
    }

    private JSONObject publishExchangeOffer(String currencyId, String secretPhrase) {
        APICall apiCall = new APICall.Builder("publishExchangeOffer").
                secretPhrase(secretPhrase1).feeNQT(Constants.ONE_NXT).
                param("deadline", "1440").
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
        return publishExchangeOfferResponse;
    }


}
