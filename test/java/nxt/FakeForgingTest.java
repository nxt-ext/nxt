package nxt;

import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class FakeForgingTest extends AbstractForgingTest {

    @Test
    public void fakeForgingTest() {
        Properties properties = AbstractForgingTest.newTestProperties();
        properties.setProperty("nxt.disableGenerateBlocksThread", "false");
        properties.setProperty("nxt.enableFakeForging", "true");
        properties.setProperty("nxt.timeMultiplier", "1");
        AbstractForgingTest.init(properties);
        Assert.assertTrue("nxt.fakeForgingAccount must be defined in nxt.properties", Nxt.getStringProperty("nxt.fakeForgingAccount") != null);
        forgeTo(startHeight + 10, testForgingSecretPhrase);
        AbstractForgingTest.shutdown();
    }

}
