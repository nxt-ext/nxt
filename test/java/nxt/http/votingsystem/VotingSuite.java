package nxt.http.votingsystem;

import nxt.http.HttpApiSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestCreatePoll.class,
        TestCastVote.class,
        TestGetAccountPolls.class
})

public class VotingSuite extends HttpApiSuite { }
