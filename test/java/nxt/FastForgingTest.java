package nxt;

import org.junit.Test;

import java.util.Properties;

public class FastForgingTest extends AbstractForgingTest {

    @Test
    public void fastForgingTest() {
        Properties properties = FastForgingTest.newTestProperties();
        properties.setProperty("nxt.disableGenerateBlocksThread", "false");
        properties.setProperty("nxt.enableFakeForging", "false");
        properties.setProperty("nxt.timeMultiplier", "1000");
        AbstractForgingTest.init(properties);
        forgeTo(startHeight + 10, testForgingSecretPhrase);
        AbstractForgingTest.shutdown();
    }

}
