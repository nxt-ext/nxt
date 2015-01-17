package nxt.http;

import nxt.Account;
import nxt.Appendix;
import nxt.Attachment;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.Constants;
import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import static nxt.http.JSONResponses.*;

abstract class CreateTransaction extends APIServlet.APIRequestHandler {

    private static final String[] commonParameters = new String[]{"secretPhrase", "publicKey", "feeNQT",
            "deadline", "referencedTransactionFullHash", "broadcast",
            "message", "messageIsText",
            "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce",
            "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce",
            "isPending", "pendingMaxHeight", "pendingVotingModel", "pendingQuorum", "pendingMinBalance", "pendingAsset",
            "pendingWhitelisted", "pendingWhitelisted", "pendingWhitelisted",
            "pendingBlacklisted", "pendingBlacklisted", "pendingBlacklisted",
            "recipientPublicKey"};

    private static String[] addCommonParameters(String[] parameters) {
        String[] result = Arrays.copyOf(parameters, parameters.length + commonParameters.length);
        System.arraycopy(commonParameters, 0, result, parameters.length, commonParameters.length);
        return result;
    }

    CreateTransaction(APITag[] apiTags, String... parameters) {
        super(apiTags, addCommonParameters(parameters));
    }

    final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Attachment attachment)
            throws NxtException {
        return createTransaction(req, senderAccount, 0, 0, attachment);
    }

    final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, long recipientId, long amountNQT)
            throws NxtException {
        return createTransaction(req, senderAccount, recipientId, amountNQT, Attachment.ORDINARY_PAYMENT);
    }

    private Appendix.TwoPhased parseTwoPhased(HttpServletRequest req) throws ParameterException {
        byte votingModel = ParameterParser.getByte(req, "pendingVotingModel", Constants.VOTING_MODEL_ACCOUNT, Constants.VOTING_MODEL_MS_COIN);

        int maxHeight = ParameterParser.getInt(req, "pendingMaxHeight",
                Nxt.getBlockchain().getHeight() + Constants.VOTING_MIN_VOTE_DURATION,
                Nxt.getBlockchain().getHeight() + Constants.PENDING_TRANSACTIONS_MAX_PERIOD,
                true);

        long quorum = ParameterParser.getLong(req, "pendingQuorum", 0, Long.MAX_VALUE, true);
        long minBalance = ParameterParser.getLong(req, "pendingMinBalance", 0, Long.MAX_VALUE, true);

        long assetId = ParameterParser.getLong(req, "pendingAsset", Long.MIN_VALUE, Long.MAX_VALUE, false);
        if (votingModel == Constants.VOTING_MODEL_ASSET && assetId == 0) {
            throw new ParameterException(MISSING_PENDING_ASSET_ID);
        }

        long[] whitelist = new long[0];
        String[] whitelistValues = req.getParameterValues("pendingWhitelisted");
        if (whitelistValues != null && whitelistValues.length > 0) {
            whitelist = new long[whitelistValues.length];
            for (int i = 0; i < whitelist.length; i++) {
                whitelist[i] = Convert.parseAccountId(whitelistValues[i]);
            }
        }
        if (votingModel == Constants.VOTING_MODEL_ACCOUNT && whitelist.length == 0) {
            throw new ParameterException(INCORRECT_PENDING_WHITELIST);
        }

        long[] blacklist = new long[0];
        String[] blacklistValues = req.getParameterValues("pendingBlacklisted");
        if (blacklistValues != null && blacklistValues.length > 0) {
            blacklist = new long[blacklistValues.length];
            for (int i = 0; i < blacklist.length; i++) {
                blacklist[i] = Convert.parseAccountId(blacklistValues[i]);
            }
        }
        if (votingModel == Constants.VOTING_MODEL_ACCOUNT && blacklist.length != 0) {
            throw new ParameterException(INCORRECT_PENDING_BLACKLISTED);
        }
        return new Appendix.TwoPhased(maxHeight, votingModel, assetId, quorum, minBalance, whitelist, blacklist);
    }

    final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, long recipientId,
                                            long amountNQT, Attachment attachment)
            throws NxtException {
        String deadlineValue = req.getParameter("deadline");
        String referencedTransactionFullHash = Convert.emptyToNull(req.getParameter("referencedTransactionFullHash"));
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        String publicKeyValue = Convert.emptyToNull(req.getParameter("publicKey"));
        boolean broadcast = !"false".equalsIgnoreCase(req.getParameter("broadcast"));
        Appendix.EncryptedMessage encryptedMessage = null;
        if (attachment.getTransactionType().canHaveRecipient()) {
            EncryptedData encryptedData = ParameterParser.getEncryptedMessage(req, Account.getAccount(recipientId));
            if (encryptedData != null) {
                encryptedMessage = new Appendix.EncryptedMessage(encryptedData, !"false".equalsIgnoreCase(req.getParameter("messageToEncryptIsText")));
            }
        }
        Appendix.EncryptToSelfMessage encryptToSelfMessage = null;
        EncryptedData encryptedToSelfData = ParameterParser.getEncryptToSelfMessage(req);
        if (encryptedToSelfData != null) {
            encryptToSelfMessage = new Appendix.EncryptToSelfMessage(encryptedToSelfData, !"false".equalsIgnoreCase(req.getParameter("messageToEncryptToSelfIsText")));
        }
        Appendix.Message message = null;
        String messageValue = Convert.emptyToNull(req.getParameter("message"));
        if (messageValue != null) {
            boolean messageIsText = !"false".equalsIgnoreCase(req.getParameter("messageIsText"));
            try {
                message = messageIsText ? new Appendix.Message(messageValue) : new Appendix.Message(Convert.parseHexString(messageValue));
            } catch (RuntimeException e) {
                throw new ParameterException(INCORRECT_ARBITRARY_MESSAGE);
            }
        }
        Appendix.PublicKeyAnnouncement publicKeyAnnouncement = null;
        String recipientPublicKey = Convert.emptyToNull(req.getParameter("recipientPublicKey"));
        if (recipientPublicKey != null) {
            publicKeyAnnouncement = new Appendix.PublicKeyAnnouncement(Convert.parseHexString(recipientPublicKey));
        }

        Appendix.TwoPhased twoPhased = null;
        String isPending = Convert.emptyToNull(req.getParameter("isPending"));
        if ("true".equalsIgnoreCase(isPending)) {
            twoPhased = parseTwoPhased(req);
        }

        if (secretPhrase == null && publicKeyValue == null) {
            return MISSING_SECRET_PHRASE;
        } else if (deadlineValue == null) {
            return MISSING_DEADLINE;
        }

        short deadline;
        try {
            deadline = Short.parseShort(deadlineValue);
            if (deadline < 1 || deadline > 1440) {
                return INCORRECT_DEADLINE;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_DEADLINE;
        }

        long feeNQT = ParameterParser.getFeeNQT(req);

        JSONObject response = new JSONObject();

        // shouldn't try to get publicKey from senderAccount as it may have not been set yet
        byte[] publicKey = secretPhrase != null ? Crypto.getPublicKey(secretPhrase) : Convert.parseHexString(publicKeyValue);

        try {
            Transaction.Builder builder = Nxt.newTransactionBuilder(publicKey, amountNQT, feeNQT,
                    deadline, attachment).referencedTransactionFullHash(referencedTransactionFullHash);
            if (attachment.getTransactionType().canHaveRecipient()) {
                builder.recipientId(recipientId);
            }
            if (encryptedMessage != null) {
                builder.encryptedMessage(encryptedMessage);
            }
            if (message != null) {
                builder.message(message);
            }
            if (publicKeyAnnouncement != null) {
                builder.publicKeyAnnouncement(publicKeyAnnouncement);
            }
            if (encryptToSelfMessage != null) {
                builder.encryptToSelfMessage(encryptToSelfMessage);
            }
            if (twoPhased != null) {
                builder.twoPhased(twoPhased);
            }
            Transaction transaction = builder.build();
            try {
                if (Convert.safeAdd(amountNQT, transaction.getFeeNQT()) > senderAccount.getUnconfirmedBalanceNQT()) {
                    return NOT_ENOUGH_FUNDS;
                }
            } catch (ArithmeticException e) {
                return NOT_ENOUGH_FUNDS;
            }
            if (secretPhrase != null) {
                transaction.sign(secretPhrase);
                response.put("transaction", transaction.getStringId());
                response.put("fullHash", transaction.getFullHash());
                response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
                response.put("signatureHash", Convert.toHexString(Crypto.sha256().digest(transaction.getSignature())));
                if (broadcast) {
                    Nxt.getTransactionProcessor().broadcast(transaction);
                    response.put("broadcasted", true);
                } else {
                    transaction.validate();
                    response.put("broadcasted", false);
                }
            } else {
                transaction.validate();
                response.put("broadcasted", false);
            }
            response.put("unsignedTransactionBytes", Convert.toHexString(transaction.getUnsignedBytes()));
            response.put("transactionJSON", JSONData.unconfirmedTransaction(transaction));

        } catch (NxtException.NotYetEnabledException e) {
            return FEATURE_NOT_AVAILABLE;
        } catch (NxtException.ValidationException e) {
            response.put("error", e.getMessage());
        }
        return response;

    }

    @Override
    final boolean requirePost() {
        return true;
    }

}
