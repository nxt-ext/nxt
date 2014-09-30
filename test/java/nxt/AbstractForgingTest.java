package nxt;

import org.junit.Assert;

import java.util.Properties;

public abstract class AbstractForgingTest extends AbstractBlockchainTest {

    protected static final int minStartHeight = Constants.LAST_KNOWN_BLOCK;
    protected static int startHeight;
    protected static String testForgingSecretPhrase;

    protected static Properties newTestProperties() {
        Properties properties = AbstractBlockchainTest.newTestProperties();
        properties.setProperty("nxt.isTestnet", "true");
        properties.setProperty("nxt.isOffline", "true");
        return properties;
    }

    protected static void init(Properties properties) {
        AbstractBlockchainTest.init(properties);
        startHeight = blockchain.getHeight();
        Assert.assertTrue(startHeight >= minStartHeight);
        testForgingSecretPhrase = Nxt.getStringProperty("nxt.testForgingSecretPhrase");
        Assert.assertTrue("nxt.testForgingSecretPhrase must be defined in your nxt.properties file", testForgingSecretPhrase != null);
    }

    protected static void shutdown() {
        blockchainProcessor.popOffTo(startHeight);
        AbstractBlockchainTest.shutdown();
    }

}
