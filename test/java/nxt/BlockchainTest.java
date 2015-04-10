package nxt;

import nxt.util.Logger;
import nxt.util.Time;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class BlockchainTest extends AbstractBlockchainTest {

    protected static int baseHeight;

    protected static List<Tester> testers = new ArrayList<>();
    protected static String forgerSecretPhrase = "aSykrgKGZNlSVOMDxkZZgbTvQqJPGtsBggb";
    protected static final String forgerAccountId = "NXT-9KZM-KNYY-QBXZ-5TD8V";
    protected static String secretPhrase1 = "hope peace happen touch easy pretend worthless talk them indeed wheel state";
    protected static String secretPhrase2 = "rshw9abtpsa2";
    protected static String secretPhrase3 = "eOdBVLMgySFvyiTy8xMuRXDTr45oTzB7L5J";
    protected static String secretPhrase4 = "t9G2ymCmDsQij7VtYinqrbGCOAtDDA3WiNr";

    protected static long id1;
    protected static long id2;
    protected static long id3;
    protected static long id4;

    protected static boolean isNxtInitted = false;
    protected static boolean needShutdownAfterClass = false;

    public static void initNxt() {
        if (!isNxtInitted) {
            Properties properties = ManualForgingTest.newTestProperties();
            properties.setProperty("nxt.isTestnet", "true");
            properties.setProperty("nxt.isOffline", "true");
            properties.setProperty("nxt.enableFakeForging", "true");
            properties.setProperty("nxt.fakeForgingAccount", forgerAccountId);
            properties.setProperty("nxt.timeMultiplier", "1");
            AbstractForgingTest.init(properties);
            isNxtInitted = true;
        }
    }
    
    @BeforeClass
    public static void init() {
        needShutdownAfterClass = !isNxtInitted;
        initNxt();
        
        Nxt.setTime(new Time.CounterTime(Nxt.getEpochTime()));
        baseHeight = blockchain.getHeight();
        Logger.logMessage("baseHeight: " + baseHeight);
        testers.add(new Tester(forgerSecretPhrase));
        testers.add(new Tester(secretPhrase1));
        testers.add(new Tester(secretPhrase2));
        testers.add(new Tester(secretPhrase3));
        testers.add(new Tester(secretPhrase4));
        id1 = testers.get(1).getId();
        id2 = testers.get(2).getId();
        id3 = testers.get(3).getId();
        id4 = testers.get(4).getId();
    }

    @AfterClass
    public static void shutdown() {
        if (needShutdownAfterClass) {
            Nxt.shutdown();
        }
    }

    @After
    public void destroy() {
        blockchainProcessor.popOffTo(baseHeight);
        shutdown();
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
