package nxt.http;


import nxt.BlockchainProcessor;
import nxt.Helper;
import nxt.Nxt;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

public abstract class AbstractHttpApiSuite {
    @BeforeClass
    public static void init() {
        Nxt.init();
        Nxt.getTransactionProcessor().clearUnconfirmedTransactions();
        Nxt.getBlockchainProcessor().addListener(new Helper.BlockListener(), BlockchainProcessor.Event.BLOCK_GENERATED);
        Assert.assertEquals(0, Helper.getCount("unconfirmed_transaction"));
    }

    @AfterClass
    public static void shutdown() {
        Assert.assertEquals(0, Helper.getCount("unconfirmed_transaction"));
        Nxt.shutdown();
    }
}
