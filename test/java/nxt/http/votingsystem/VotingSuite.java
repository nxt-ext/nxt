package nxt.http.votingsystem;

import nxt.http.AbstractHttpApiSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestCreatePoll.class,
        TestCastVote.class,
        TestGetPolls.class
})

public class VotingSuite extends AbstractHttpApiSuite { }
