package nxt.http;


import nxt.Account;
import nxt.Appendix;
import nxt.Attachment;
import nxt.Constants;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_PENDING_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_PENDING_TRANSACTION;

public class ApprovePendingTransaction extends CreateTransaction {
    static final ApprovePendingTransaction instance = new ApprovePendingTransaction();

    private ApprovePendingTransaction() { super(new APITag[]{APITag.CREATE_TRANSACTION,
                                                    APITag.PENDING_TRANSACTIONS},"pendingTransaction"); }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        String[] pendingTransactionValues = req.getParameterValues("pendingTransaction");

        if(pendingTransactionValues.length == 0){
            return MISSING_PENDING_TRANSACTION;
        }

        if(pendingTransactionValues.length > Constants.MAX_VOTES_PER_VOTING_TRANSACTION){
            return INCORRECT_PENDING_TRANSACTION;
        }

        long[] pendingTransactionIds = new long[pendingTransactionValues.length];
        for(int i=0; i < pendingTransactionValues.length; i++){
            pendingTransactionIds[i] = Convert.parseUnsignedLong(pendingTransactionValues[i]);
            Transaction transaction = Nxt.getBlockchain().getTransaction(pendingTransactionIds[i]);
            if(transaction==null){
                return INCORRECT_PENDING_TRANSACTION;
            }
            Appendix.TwoPhased twoPhased = transaction.getTwoPhased();
            if(twoPhased==null){
                return INCORRECT_PENDING_TRANSACTION;
            }
            if(twoPhased.getMaxHeight() < Nxt.getBlockchain().getHeight()){
                return INCORRECT_PENDING_TRANSACTION;
            }
        }

        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.PendingPaymentVoteCasting(pendingTransactionIds);
        return createTransaction(req, account, attachment);
    }
}
