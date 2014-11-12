package nxt.http;

import nxt.Nxt;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        SendMoneyTest.class,
        SendMessageTest.class,
        LeaseTest.class
})

public class PaymentAndMessagesSuite {

    @BeforeClass
    public static void init() {
        Nxt.init();
    }

    @AfterClass
    public static void shutdown() {
        Nxt.shutdown();
    }

}
