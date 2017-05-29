package nxt.addons;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.http.APICall;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class JPLSnapshotTest extends BlockchainTest {

    public static final String INPUT_JSON_STR =
            "{\n" +
            "    \"balances\": {\n" +
            "        \"NXT-NZKH-MZRE-2CTT-98NPZ\": 30000000000000000,\n" +
            "        \"NXT-X5JH-TJKJ-DVGC-5T2V8\": 30000000000000000,\n" +
            "        \"NXT-LTR8-GMHB-YG56-4NWSE\": 30000000000000000\n" +
            "    },\n" +
            "    \"publicKeys\": [\n" +
            "        \"bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37\",\n" +
            "        \"39dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d37960152\",\n" +
            "        \"011889a0988ccbed7f488878c62c020587de23ebbbae9ba56dd67fd9f432f808\"\n" +
            "    ]\n" +
            "}\n";

    @Test
    public void testSnapshotWithoutInput() {
        long aliceCurrentBalance = ALICE.getBalance();
        String aliceId = Long.toUnsignedString(ALICE.getAccount().getId());
        JSONObject response = new APICall.Builder("downloadJPLSnapshot").
                param("height", getHeight()).
                build().invoke();
        JSONObject balances = (JSONObject)response.get("balances");
        long total = 0;
        long aliceSnapshotBalance = 0;
        for (Map.Entry<String, Long> entry : ((Map<String, Long>)balances).entrySet()) {
            total += entry.getValue();
            if (entry.getKey().equals(aliceId)) {
                aliceSnapshotBalance = entry.getValue();
            }
        }
        Assert.assertEquals(aliceCurrentBalance, aliceSnapshotBalance);
        Assert.assertEquals(Constants.MAX_BALANCE_NQT, total);
    }

    @Test
    public void testSnapshotWithInput() {
        long aliceCurrentBalance = ALICE.getBalance();
        String aliceId = Long.toUnsignedString(ALICE.getAccount().getId());
        JSONObject response = new APICall.Builder("downloadJPLSnapshot").
            param("height", getHeight()).
            parts("newGenesisAccounts", INPUT_JSON_STR).
            build().invoke();
        JSONObject balances = (JSONObject)response.get("balances");
        long total = 0;
        long aliceSnapshotBalance = 0;
        for (Map.Entry<String, Long> entry : ((Map<String, Long>)balances).entrySet()) {
            total += entry.getValue();
            if (entry.getKey().equals(aliceId)) {
                aliceSnapshotBalance = entry.getValue();
            }
        }
        Assert.assertEquals(aliceCurrentBalance, aliceSnapshotBalance);
        Assert.assertEquals(Constants.MAX_BALANCE_NQT, total);
    }
}
