package nxt.http.twophased;

import nxt.http.HttpApiSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestCreateTwoPhased.class,
        TestGetAccountPendingTransactionToApproveIds.class,
        TestApprovePendingTransaction.class
})

public class TwoPhasedSuite extends HttpApiSuite { }

