package nxt.http;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        SendMoneyTest.class,
        SendMessageTest.class,
        LeaseTest.class
})

public class PaymentAndMessagesSuite extends AbstractHttpApiSuite {}
