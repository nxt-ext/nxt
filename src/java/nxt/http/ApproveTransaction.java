package nxt.http;


import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import nxt.PhasingPoll;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static nxt.http.JSONResponses.INCORRECT_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_TRANSACTION;
import static nxt.http.JSONResponses.UNKNOWN_TRANSACTION;

public class ApproveTransaction extends CreateTransaction {
    static final ApproveTransaction instance = new ApproveTransaction();

    private ApproveTransaction() {
        super(new APITag[]{APITag.CREATE_TRANSACTION,
                APITag.PHASING}, "transactionFullHash");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        String[] pendingTransactionValues = req.getParameterValues("transactionFullHash");

        if (pendingTransactionValues.length == 0) {
            return MISSING_TRANSACTION;
        }

        if (pendingTransactionValues.length > Constants.MAX_VOTES_PER_VOTING_TRANSACTION) {
            return INCORRECT_TRANSACTION;
        }

        List<byte[]> pendingTransactionFullHashes = new ArrayList<>(pendingTransactionValues.length);
        for (int i = 0; i < pendingTransactionValues.length; i++) {
            byte[] hash = Convert.parseHexString(pendingTransactionValues[i]);
            PhasingPoll phasingPoll = PhasingPoll.getPoll(Convert.fullHashToId(hash));
            if (phasingPoll == null) {
                return UNKNOWN_TRANSACTION;
            }
            if (phasingPoll.isFinished()) {
                return INCORRECT_TRANSACTION;
            }
            pendingTransactionFullHashes.add(hash);
        }

        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.MessagingPhasingVoteCasting(pendingTransactionFullHashes);
        return createTransaction(req, account, attachment);
    }
}
