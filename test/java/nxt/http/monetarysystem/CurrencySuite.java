package nxt.http.monetarysystem;

import nxt.BlockchainProcessor;
import nxt.Helper;
import nxt.Nxt;
import nxt.http.HttpApiSuite;
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

public class CurrencySuite extends HttpApiSuite { }
