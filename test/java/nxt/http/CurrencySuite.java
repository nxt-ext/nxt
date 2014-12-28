package nxt.http;

import nxt.BlockchainProcessor;
import nxt.Helper;
import nxt.Nxt;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestCurrencyIssuance.class,
        TestCurrencyExchange.class,
        TestCurrencyReserveAndClaim.class,
        TestCurrencyMint.class,
        nxt.TestMintCalculations.class,
        DeleteCurrencyTest.class,
})

public class CurrencySuite {

    @BeforeClass
    public static void init() {
        Nxt.init();
        Nxt.getBlockchainProcessor().addListener(new Helper.BlockListener(), BlockchainProcessor.Event.BLOCK_GENERATED);
        Assert.assertEquals(0, Helper.getCount("unconfirmed_transaction"));
    }

    @AfterClass
    public static void shutdown() {
        Assert.assertEquals(0, Helper.getCount("unconfirmed_transaction"));
        Nxt.shutdown();
    }

}
