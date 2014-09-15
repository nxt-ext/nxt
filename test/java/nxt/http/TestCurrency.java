package nxt.http;

import nxt.*;
import nxt.db.Db;
import nxt.util.Listener;
import org.h2.tools.Shell;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestCurrency {

    static int baseHeight;

    @BeforeClass
    public static void init() {
        Nxt.init();
        Nxt.getBlockchainProcessor().addListener(new BlockListener(), BlockchainProcessor.Event.BLOCK_GENERATED);
        baseHeight = Nxt.getBlockchain().getHeight();
        executeQuery("select * from currency");
        executeQuery("select * from unconfirmed_transaction");
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

        JSONObject issueCurrencyResponse = processRequest(reqParams);
        String currencyId = (String) issueCurrencyResponse.get("transaction");
        System.out.println("issueCurrencyResponse: " + issueCurrencyResponse.toJSONString());
        Generator.startForging(secretPhrase);
        Generator.stopForging(secretPhrase);
        reqParams.clear();
        reqParams.put("requestType", "getCurrency");
        reqParams.put("currency", currencyId);
        JSONObject getCurrencyResponse = processRequest(reqParams);
        System.out.println("getCurrencyResponse:" + getCurrencyResponse.toJSONString());
        Assert.assertEquals(currencyId, getCurrencyResponse.get("currency"));
        Assert.assertEquals("TSX", getCurrencyResponse.get("code"));
        return currencyId;
    }

    @Test
    public void exchange() {
        String currencyId = issueCurrencyImpl();

        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("requestType", "publishExchangeOffer");
        String secretPhrase = "hope peace happen touch easy pretend worthless talk them indeed wheel state";
        reqParams.put("secretPhrase", secretPhrase);
        reqParams.put("deadline", "1440");
        reqParams.put("feeNQT", "" + Constants.ONE_NXT);
        reqParams.put("currency", currencyId);
        reqParams.put("buyRateNQT", "" + Constants.ONE_NXT);
        reqParams.put("sellRateNQT", "" + 100 * Constants.ONE_NXT);
        reqParams.put("totalBuyLimit", "1000");
        reqParams.put("totalSellLimit", "1000");
        reqParams.put("initialBuySupply", "1");
        reqParams.put("initialSellSupply", "1");
        reqParams.put("expirationHeight", "130000");

        JSONObject publishExchangeOfferResponse = processRequest(reqParams);
        System.out.println("publishExchangeOfferResponse: " + publishExchangeOfferResponse.toJSONString());
        Generator.startForging(secretPhrase);
        Generator.stopForging(secretPhrase);
        reqParams.clear();
        reqParams.put("requestType", "getAllOffers");
        JSONObject getAllOffersResponse = processRequest(reqParams);
        System.out.println("getAllOffersResponse:" + getAllOffersResponse.toJSONString());
    }

    @After
    public void destroy() {
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("requestType", "popOff");
        reqParams.put("height", "" + baseHeight);
        JSONObject popOffResponse = processRequest(reqParams);
        System.out.println("popOffResponse:" + popOffResponse.toJSONString());
        executeQuery("select * from currency");
        executeQuery("select * from unconfirmed_transaction");
        Nxt.getTransactionProcessor().shutdown();
    }

    @AfterClass
    public static void shutdown() {
        executeQuery("select * from currency");
        executeQuery("select * from unconfirmed_transaction");
        Nxt.shutdown();
    }

    private static JSONObject processRequest(Map<String, String> reqParams) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getRemoteHost()).thenReturn("localhost");
        when(req.getMethod()).thenReturn("POST");
        for (String key : reqParams.keySet()) {
            when(req.getParameter(key)).thenReturn(reqParams.get(key));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        try {
            when(resp.getWriter()).thenReturn(writer);
            APIServlet apiServlet = new APIServlet();
            apiServlet.doPost(req, resp);
        } catch (ServletException | IOException e) {
            Assert.fail();
        }
        return (JSONObject) JSONValue.parse(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
    }

    private static void executeQuery(String line) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        out.println(line);
        try {
            Shell shell = new Shell();
            shell.setErr(out);
            shell.setOut(out);
            shell.runTool(Db.getConnection(), "-sql", line);
        } catch (SQLException e) {
            out.println(e.toString());
        }
        System.out.println(new String(baos.toByteArray()));
    }

    private static class BlockListener implements Listener<Block> {
        @Override
        public void notify(Block block) {
            System.out.printf("Block Generated at height %d with %d transactions\n", block.getHeight(), block.getTransactionIds().size());
        }
    }
}
