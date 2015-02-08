package nxt.http;


import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.Nxt;
import nxt.NxtException;
import nxt.PhasingPoll;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_PENDING_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_PENDING_TRANSACTION;

public class ApproveTransaction extends CreateTransaction {
    static final ApproveTransaction instance = new ApproveTransaction();

    private ApproveTransaction() {
        super(new APITag[]{APITag.CREATE_TRANSACTION,
                APITag.PHASING}, "transaction");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        String[] pendingTransactionValues = req.getParameterValues("transaction");

        if (pendingTransactionValues.length == 0) {
            return MISSING_PENDING_TRANSACTION;
        }

        if (pendingTransactionValues.length > Constants.MAX_VOTES_PER_VOTING_TRANSACTION) {
            return INCORRECT_PENDING_TRANSACTION;
        }

        long[] pendingTransactionIds = new long[pendingTransactionValues.length];
        for (int i = 0; i < pendingTransactionValues.length; i++) {
            pendingTransactionIds[i] = Convert.parseUnsignedLong(pendingTransactionValues[i]);
            PhasingPoll phasingPoll = PhasingPoll.getPoll(pendingTransactionIds[i]);
            if (phasingPoll == null) {
                return INCORRECT_PENDING_TRANSACTION;
            }
            if (phasingPoll.getFinishHeight() < Nxt.getBlockchain().getHeight()) {
                return INCORRECT_PENDING_TRANSACTION;
            }
        }

        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.MessagingPhasingVoteCasting(pendingTransactionIds);
        return createTransaction(req, account, attachment);
    }
}
