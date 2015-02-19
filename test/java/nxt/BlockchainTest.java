package nxt;

import nxt.util.Logger;
import nxt.util.Time;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class BlockchainTest extends AbstractBlockchainTest {

    protected static int baseHeight;

    protected static final String unitTestsBaseSecretPhrase = "ReservedForUnitTestsSykrgKGZNlSVOMDxkZZgbTvQqJPGtsBggb";

    protected List<Tester> testers = new ArrayList<>();
    protected static String forgerSecretPhrase;
    protected static String secretPhrase1;
    protected static String secretPhrase2;
    protected static String secretPhrase3;
    protected static String secretPhrase4;

    protected static long id1;

    @Before
    public void init() {
        for (int i=0; i<10; i++) {
            Tester tester = new Tester(unitTestsBaseSecretPhrase + i);
            Logger.logDebugMessage("tester %d RSAccount %s public key %s", i, tester.getRsAccount(), tester.getPublicKeyStr());
            testers.add(tester);
        }

        forgerSecretPhrase = testers.get(0).getSecretPhrase();
        secretPhrase1 = testers.get(1).getSecretPhrase();
        secretPhrase2 = testers.get(2).getSecretPhrase();
        secretPhrase3 = testers.get(3).getSecretPhrase();
        secretPhrase4 = testers.get(4).getSecretPhrase();

        id1 = testers.get(1).getId();

        Properties properties = ManualForgingTest.newTestProperties();
        properties.setProperty("nxt.isTestnet", "true");
        properties.setProperty("nxt.isOffline", "true");
        properties.setProperty("nxt.enableFakeForging", "true");
        properties.setProperty("nxt.timeMultiplier", "1");
        AbstractForgingTest.init(properties);
        Nxt.setTime(new Time.CounterTime(Nxt.getEpochTime()));
        baseHeight = blockchain.getHeight();
        Logger.logMessage("baseHeight: " + baseHeight);
    }

    @After
    public void destroy() {
        AbstractForgingTest.shutdown();
    }

    public static void generateBlock() {
        try {
            blockchainProcessor.generateBlock(forgerSecretPhrase, Nxt.getEpochTime());
        } catch (BlockchainProcessor.BlockNotAcceptedException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    public static void generateBlocks(int howMany) {
        for (int i = 0; i < howMany; i++) {
            generateBlock();
        }
    }

    protected long balanceById(long id) {
        return Account.getAccount(id).getBalanceNQT();
    }


}
