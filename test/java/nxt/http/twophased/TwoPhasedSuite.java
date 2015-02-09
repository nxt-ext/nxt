package nxt.http.twophased;

import nxt.http.HttpApiSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestCreateTwoPhased.class,
        TestGetVoterPendingTransactions.class,
        TestApprovePendingTransaction.class,
        TestGetPendingTransactionVotes.class
})

public class TwoPhasedSuite extends HttpApiSuite { }

