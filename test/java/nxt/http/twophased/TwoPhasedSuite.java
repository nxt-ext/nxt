package nxt.http.twophased;

import nxt.http.AbstractHttpApiSuite;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestCreateTwoPhased.class,
        TestGetVoterPhasedTransactions.class,
        TestApproveTransaction.class,
        TestGetPhasingPoll.class,
        TestGetAccountPhasedTransactions.class,
        TestGetAssetPhasedTransactions.class,
        TestGetCurrencyPhasedTransactions.class
})

public class TwoPhasedSuite extends AbstractHttpApiSuite {
    static boolean searchForTransactionId(JSONArray transactionsJson, String transactionId) {
        boolean found = false;
        for (Object transactionsJsonObj : transactionsJson) {
            JSONObject transactionObject = (JSONObject) transactionsJsonObj;
            String iteratedTransactionId = (String) transactionObject.get("transaction");
            if (iteratedTransactionId.equals(transactionId)) {
                found = true;
                break;
            }
        }
        return found;
    }
}

