package nxt.http;

import nxt.*;
import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static nxt.http.JSONResponses.*;


abstract class CreateTransaction extends APIServlet.APIRequestHandler {

    private static final String[] commonParameters = new String[] {"secretPhrase", "publicKey", "feeNQT",
            "deadline", "referencedTransactionFullHash", "broadcast",
            "message", "messageIsText",
            "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce",
            "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce",
            "isPending", "heightToRelease", "votingModelForPending", "quorum", "voteThreshold", "assetId",
            "whitelisted", "whitelisted", "whitelisted",
            "blacklisted", "blacklisted", "blacklisted",
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

    final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, long recipientId,
                                            long amountNQT, Attachment attachment)
            throws NxtException {
        String deadlineValue = req.getParameter("deadline");
        String referencedTransactionFullHash = Convert.emptyToNull(req.getParameter("referencedTransactionFullHash"));
        String referencedTransactionId = Convert.emptyToNull(req.getParameter("referencedTransaction"));
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        String publicKeyValue = Convert.emptyToNull(req.getParameter("publicKey"));
        boolean broadcast = !"false".equalsIgnoreCase(req.getParameter("broadcast"));
        Appendix.EncryptedMessage encryptedMessage = null;
        if (attachment.getTransactionType().hasRecipient()) {
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
        if (isPending != null && !isPending.equals("false")) {

            String votingModelValue = Convert.emptyToNull(req.getParameter("pendingTxVotingModel"));
            if (votingModelValue == null) {
                return MISSING_VOTINGMODEL;
            }

            byte votingModel;
            try {
                votingModel = Byte.parseByte(votingModelValue);
                if (votingModel != Constants.VOTING_MODEL_ACCOUNT
                        && votingModel != Constants.VOTING_MODEL_ASSET
                        && votingModel != Constants.VOTING_MODEL_BALANCE) {
                    return INCORRECT_VOTINGMODEL;
                }
            } catch (NumberFormatException e) {
                return INCORRECT_VOTINGMODEL;
            }

            String heightToReleaseValue = Convert.emptyToNull(req.getParameter("heightToRelease"));
            if (heightToReleaseValue == null) {
                return MISSING_HEIGHT_TO_RELEASE;
            }

            int heightToRelease;
            try {
                heightToRelease = Integer.parseInt(req.getParameter("heightToRelease"));
                if (heightToRelease <= Nxt.getBlockchain().getHeight() + Constants.VOTING_MIN_VOTE_DURATION) {
                    return INCORRECT_HEIGHT_TO_RELEASE;
                }
            } catch (NumberFormatException e) {
                return INCORRECT_HEIGHT_TO_RELEASE;
            }


            String quorumValue = Convert.emptyToNull(req.getParameter("quorum"));
            if (quorumValue == null) {
                return MISSING_QUORUM;
            }
            long quorum;
            try {
                quorum = Long.parseLong(quorumValue);
                if (quorum <= 0) {
                    return INCORRECT_QUORUM;
                }
            } catch (NumberFormatException e) {
                return INCORRECT_QUORUM;
            }

            String voteThresholdValue = Convert.emptyToNull(req.getParameter("voteThreshold"));
            long voteThreshold = 0;
            if (voteThresholdValue != null) {
                try {
                    voteThreshold = Long.parseLong(voteThresholdValue);
                    if (voteThreshold < 0) {
                        return INCORRECT_VOTE_THRESHOLD;
                    }
                } catch (NumberFormatException e) {
                    return INCORRECT_VOTE_THRESHOLD;
                }
            }

            String assetIdValue = Convert.emptyToNull(req.getParameter("pendingTxAssetId"));
            long assetId = 0;
            if (assetIdValue == null) {
                if (votingModel == Constants.VOTING_MODEL_ASSET) {
                    return MISSING_PENDING_TX_ASSET_ID;
                }
            } else {
                try {
                    assetId = Long.parseLong(voteThresholdValue);
                } catch (NumberFormatException e) {
                    return INCORRECT_VOTE_THRESHOLD;
                }
            }

            long[] whitelist = new long[0];

            String[] whitelistValues = Convert.emptyToNull(req.getParameterValues("whitelisted"));
            if (whitelistValues != null) {
                whitelist = new long[whitelistValues.length];
                for (int i = 0; i < whitelist.length; i++) {
                    whitelist[i] = Convert.parseAccountId(whitelistValues[i]);
                }
            }

            if (votingModel == Constants.VOTING_MODEL_ACCOUNT
                    && whitelist.length == 0) {
                return INCORRECT_WHITELIST;
            }


            long[] blacklist = new long[0];
            String[] blacklistValues = Convert.emptyToNull(req.getParameterValues("blacklisted"));

            if (blacklistValues != null) {
                blacklist = new long[blacklistValues.length];
                for (int i = 0; i < blacklist.length; i++) {
                    blacklist[i] = Convert.parseAccountId(blacklistValues[i]);
                }
            }

            if (votingModel == Constants.VOTING_MODEL_ACCOUNT
                    && blacklist.length != 0) {
                return INCORRECT_BLACKLIST;
            }

            twoPhased = new Appendix.TwoPhased(heightToRelease, votingModel, assetId, quorum, voteThreshold, whitelist, blacklist);
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
        if (referencedTransactionId != null) {
            return INCORRECT_REFERENCED_TRANSACTION;
        }

        JSONObject response = new JSONObject();

        // shouldn't try to get publicKey from senderAccount as it may have not been set yet
        byte[] publicKey = secretPhrase != null ? Crypto.getPublicKey(secretPhrase) : Convert.parseHexString(publicKeyValue);

        try {
            Transaction.Builder builder = Nxt.getTransactionProcessor().newTransactionBuilder(publicKey, amountNQT, feeNQT,
                    deadline, attachment).referencedTransactionFullHash(referencedTransactionFullHash);
            if (attachment.getTransactionType().hasRecipient()) {
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
            if(twoPhased!=null){
                builder.twoPhased(twoPhased);
            }
            Transaction transaction = builder.build();
            transaction.validate();
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
                    response.put("broadcasted", false);
                }
            } else {
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
