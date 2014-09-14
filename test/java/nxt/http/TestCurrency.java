package nxt.http;

import nxt.Block;
import nxt.BlockchainProcessor;
import nxt.Generator;
import nxt.Nxt;
import nxt.util.Listener;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestCurrency {

    @Test
    public void issueSimpleCurrency() {
        Nxt.init();
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
    }

    @After
    public void destroy() {
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("requestType", "popOff");
        reqParams.put("numBlocks", "1");
        JSONObject popOffResponse = processRequest(reqParams);
        System.out.println("popOffResponse:" + popOffResponse.toJSONString());
    }

    private JSONObject processRequest(Map<String, String> reqParams) {
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

    @Before
    public void init() {

        Nxt.getBlockchainProcessor().addListener(new BlockListener(), BlockchainProcessor.Event.BLOCK_GENERATED);
    }

    private static class BlockListener implements Listener<Block> {
        @Override
        public void notify(Block block) {
            System.out.printf("Block Generated at height %d with %d transactions\n", block.getHeight(), block.getTransactionIds().size());
        }
    }
}
