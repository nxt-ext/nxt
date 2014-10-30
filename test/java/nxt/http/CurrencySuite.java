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
        TestShufflingRegistration.class,
        MessageEncryptionTest.class
})

public class CurrencySuite {

    private static String NO_TRANSACTIONS = "0 rows";

    @BeforeClass
    public static void init() {
        Nxt.init();
        Nxt.getBlockchainProcessor().addListener(new Helper.BlockListener(), BlockchainProcessor.Event.BLOCK_GENERATED);
        String output = Helper.executeQuery("select * from unconfirmed_transaction");
        Assert.assertTrue(output.contains(NO_TRANSACTIONS));
        output = Helper.executeQuery("select * from currency");
        Assert.assertTrue(output.contains(NO_TRANSACTIONS));
    }

    @AfterClass
    public static void shutdown() {
        String output = Helper.executeQuery("select * from currency");
        Assert.assertTrue(output.contains(NO_TRANSACTIONS));
        output = Helper.executeQuery("select * from unconfirmed_transaction");
        Assert.assertTrue(output.contains(NO_TRANSACTIONS));
        Nxt.shutdown();
    }

}
