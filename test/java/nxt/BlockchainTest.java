package nxt;

import nxt.http.APICall;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public abstract class BlockchainTest {

    protected static int baseHeight;

    protected static final String forgerSecretPhrase = "aSykrgKGZNlSVOMDxkZZgbTvQqJPGtsBggb";
    protected static final String secretPhrase1 = "hope peace happen touch easy pretend worthless talk them indeed wheel state";
    protected static final String secretPhrase2 = "rshw9abtpsa2";

    @Before
    public void init() {
        baseHeight = Nxt.getBlockchain().getHeight();
        Logger.logDebugMessage("baseHeight: " + baseHeight);
    }

    @After
    public void destroy() {
        APICall apiCall = new APICall.Builder("popOff").param("height", "" + baseHeight).build();
        JSONObject popOffResponse = apiCall.invoke();
        Logger.logDebugMessage("popOffResponse:" + popOffResponse.toJSONString());
        Helper.executeQuery("select * from unconfirmed_transaction");
        Nxt.getTransactionProcessor().shutdown();
    }

    public static void generateBlock() {
        try {
            Nxt.getBlockchainProcessor().generateBlock(forgerSecretPhrase, Convert.getEpochTime());
        } catch (BlockchainProcessor.BlockNotAcceptedException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

}
