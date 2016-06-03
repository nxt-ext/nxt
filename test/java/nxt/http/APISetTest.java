package nxt.http;

import nxt.util.Logger;
import org.junit.Assert;
import org.junit.Test;

public class APISetTest {
    @Test
    public void testBase64() {
        MutableAPISet set = new MutableAPISet();
        set.add(APIEnum.SET_API_PROXY_PEER);
        String base64String = set.toBase64String();
        Logger.logMessage("base64String: " + base64String);

        set = new MutableAPISet(APISet.fromBase64String(base64String));
        Assert.assertTrue(set.containsName(APIEnum.SET_API_PROXY_PEER.getName()));
        for (int i = 0; i < APIEnum.values().length; i++) {
            if (i != APIEnum.SET_API_PROXY_PEER.ordinal()) {
                Assert.assertFalse(set.containsName(APIEnum.values()[i].getName()));
            }
        }
    }

    @Test
    public void testContainsAll() {
        MutableAPISet container = new MutableAPISet();
        MutableAPISet contained = new MutableAPISet();

        Assert.assertTrue(container.containsAll(contained));

        Assert.assertTrue(container.add(APIEnum.CURRENCY_RESERVE_INCREASE));
        Assert.assertTrue(container.add(APIEnum.GET_ALL_SHUFFLINGS));

        Assert.assertTrue(container.containsAll(contained));

        Assert.assertTrue(contained.add(APIEnum.CURRENCY_RESERVE_INCREASE));
        Assert.assertTrue(contained.add(APIEnum.GET_ALL_SHUFFLINGS));

        Assert.assertTrue(container.containsAll(contained));

        Assert.assertTrue(contained.add(APIEnum.GET_SHARED_KEY));

        Assert.assertFalse(container.containsAll(contained));

        Assert.assertTrue(container.add(APIEnum.DOWNLOAD_PRUNABLE_MESSAGE));
        Assert.assertFalse(container.containsAll(contained));
    }
}
