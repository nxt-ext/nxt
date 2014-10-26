package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.NxtException;
import nxt.Shuffling;
import nxt.ShufflingParticipant;
import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Collections;

import static nxt.http.JSONResponses.*;

public final class ShufflingProcess extends CreateTransaction {

    static final ShufflingProcess instance = new ShufflingProcess();

    private ShufflingProcess() {
        super(new APITag[] {APITag.SHUFFLE, APITag.CREATE_TRANSACTION},
                "shuffling", "secretPhrase", "recipient", "data");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Shuffling shuffling = ParameterParser.getShuffling(req);
        String secretPhrase = req.getParameter("secretPhrase");
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        }
        long recipientId = ParameterParser.getRecipientId(req);
        byte[] data = Convert.parseHexString(Convert.nullToEmpty(req.getParameter("data")));

        Account senderAccount = ParameterParser.getSenderAccount(req);
        if (shuffling.getAssigneeAccountId() != senderAccount.getId()) {
            return INCORRECT_ACCOUNT;
        }

        ShufflingParticipant participant = ShufflingParticipant.getParticipant(shuffling.getId(), senderAccount.getId());
        ArrayList<byte[]> publicKeys = new ArrayList<>();
        while (participant.getNextAccountId() != 0) {
            Account account = Account.getAccount(participant.getNextAccountId());
            publicKeys.add(account.getPublicKey());
            participant = ShufflingParticipant.getParticipant(shuffling.getId(), account.getId());
        }

//        Attachment attachment = new Attachment.MonetarySystemShufflingDistribution(shuffling.getId());
//
//        Account senderAccount = ParameterParser.getSenderAccount(req);
//        return createTransaction(req, senderAccount, attachment);
        return null;
    }
}
