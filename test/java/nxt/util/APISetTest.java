package nxt.util;

import nxt.http.APIEnum;
import org.junit.Assert;
import org.junit.Test;

import java.util.EnumSet;

public class APISetTest {
    @Test
    public void testBase64() {
        Logger.logMessage("empty enum: " + APIEnum.enumSetToBase64String(EnumSet.noneOf(APIEnum.class)));

        EnumSet<APIEnum> set = EnumSet.of(APIEnum.SET_API_PROXY_PEER, APIEnum.GET_ASSET_DIVIDENDS);
        String base64String = APIEnum.enumSetToBase64String(set);
        Logger.logMessage("base64String: " + base64String);

        set = APIEnum.base64StringToEnumSet(base64String);
        Assert.assertTrue(set.contains(APIEnum.SET_API_PROXY_PEER));
        for (APIEnum api : APIEnum.values()) {
            if (api != APIEnum.SET_API_PROXY_PEER && api != APIEnum.GET_ASSET_DIVIDENDS) {
                Assert.assertFalse(set.contains(api));
            }
        }
    }

}
