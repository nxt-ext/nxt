package nxt.http;

import nxt.Attachment;
import org.junit.Test;

public class TestCurrency extends CreateTransaction {

    @Test
    public void issueSimpleCurrency() {
        Attachment attachment = new Attachment.MonetarySystemCurrencyIssuance("TestCur", "TST", "Test Currency", 1, 1000,
                0, 0, 0, 0, 0);

        return new CreateTransaction().createTransaction(req, account, attachment);

    }


}
