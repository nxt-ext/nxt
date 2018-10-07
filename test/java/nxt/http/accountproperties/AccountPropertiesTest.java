package nxt.http.accountproperties;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.http.APICall;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class AccountPropertiesTest extends BlockchainTest {

    public static final String VALUE1 =
            "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";

    public static final String KEY1 = "key1";

    @Test
    public void accountProperty1() {
        JSONObject response = new APICall.Builder("setAccountProperty").
                param("secretPhrase", ALICE.getSecretPhrase()).feeNQT(Constants.ONE_NXT * 20).
                param("recipient", BOB.getStrId()).
                param("property", KEY1).
                param("value", VALUE1).
                build().invoke();
        Assert.assertEquals(4L, response.get("errorCode"));
        Assert.assertTrue(((String)response.get("errorDescription")).contains("Invalid account property"));
        BlockchainTest.generateBlock();
    }

    @Test
    public void accountProperty2() {
        char[] fourBytesChar = Character.toChars(0x1F701);
        String specialChar = new String(fourBytesChar);
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < 44; i++) {
            sb.append(specialChar);
        }
        String value = sb.toString();
        JSONObject response = new APICall.Builder("setAccountProperty").
                param("secretPhrase", ALICE.getSecretPhrase()).feeNQT(Constants.ONE_NXT * 20).
                param("recipient", BOB.getStrId()).
                param("property", KEY1).
                param("value", value).
                build().invoke();
        Assert.assertEquals(4L, response.get("errorCode"));
        Assert.assertTrue(((String)response.get("errorDescription")).contains("Invalid account property"));
        BlockchainTest.generateBlock();
    }

    @Test
    public void accountProperty3() {
        char[] fourBytesChar = Character.toChars(0x1F701);
        String specialChar = new String(fourBytesChar);
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < 80; i++) {
            sb.append(specialChar);
        }
        String value = sb.toString();
        JSONObject response = new APICall.Builder("setAccountProperty").
                param("secretPhrase", ALICE.getSecretPhrase()).feeNQT(Constants.ONE_NXT * 20).
                param("recipient", BOB.getStrId()).
                param("property", KEY1).
                param("value", value).
                build().invoke();
        Assert.assertEquals(4L, response.get("errorCode"));
        Assert.assertTrue(((String)response.get("errorDescription")).contains("Invalid account property"));
        BlockchainTest.generateBlock();
    }

    @Test
    public void accountPropertyName() {
        String specialChar = "€";
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < 32; i++) {
            sb.append(specialChar);
        }
        String name = sb.toString();
        JSONObject response = new APICall.Builder("setAccountProperty").
                param("secretPhrase", ALICE.getSecretPhrase()).feeNQT(Constants.ONE_NXT * 20).
                param("recipient", BOB.getStrId()).
                param("property", name).
                param("value", "").
                build().invoke();

        Assert.assertNull(response.get("errorCode"));
        BlockchainTest.generateBlock();
    }

}
