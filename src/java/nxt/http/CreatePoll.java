package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Genesis;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static nxt.http.JSONResponses.INCORRECT_DEADLINE;
import static nxt.http.JSONResponses.INCORRECT_FEE;
import static nxt.http.JSONResponses.INCORRECT_MAXNUMBEROFOPTIONS;
import static nxt.http.JSONResponses.INCORRECT_MINNUMBEROFOPTIONS;
import static nxt.http.JSONResponses.INCORRECT_OPTIONSAREBINARY;
import static nxt.http.JSONResponses.INCORRECT_POLL_DESCRIPTION_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_POLL_NAME_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_POLL_OPTION_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_DEADLINE;
import static nxt.http.JSONResponses.MISSING_DESCRIPTION;
import static nxt.http.JSONResponses.MISSING_FEE;
import static nxt.http.JSONResponses.MISSING_MAXNUMBEROFOPTIONS;
import static nxt.http.JSONResponses.MISSING_MINNUMBEROFOPTIONS;
import static nxt.http.JSONResponses.MISSING_NAME;
import static nxt.http.JSONResponses.MISSING_OPTIONSAREBINARY;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.NOT_ENOUGH_FUNDS;

public final class CreatePoll extends APIServlet.APIRequestHandler {

    static final CreatePoll instance = new CreatePoll();

    private CreatePoll() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException, IOException {

        String secretPhrase = req.getParameter("secretPhrase");
        String nameValue = req.getParameter("name");
        String descriptionValue = req.getParameter("description");
        String minNumberOfOptionsValue = req.getParameter("minNumberOfOptions");
        String maxNumberOfOptionsValue = req.getParameter("maxNumberOfOptions");
        String optionsAreBinaryValue = req.getParameter("optionsAreBinary");
        String feeValue = req.getParameter("fee");
        String deadlineValue = req.getParameter("deadline");
        String referencedTransactionValue = req.getParameter("referencedTransaction");
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        } else if (nameValue == null) {
            return MISSING_NAME;
        } else if (descriptionValue == null) {
            return MISSING_DESCRIPTION;
        } else if (minNumberOfOptionsValue == null) {
            return MISSING_MINNUMBEROFOPTIONS;
        } else if (maxNumberOfOptionsValue == null) {
            return MISSING_MAXNUMBEROFOPTIONS;
        } else if (optionsAreBinaryValue == null) {
            return MISSING_OPTIONSAREBINARY;
        } else if (feeValue == null) {
            return MISSING_FEE;
        } else if (deadlineValue == null) {
            return MISSING_DEADLINE;
        }

        if (nameValue.length() > Nxt.MAX_POLL_NAME_LENGTH) {
            return INCORRECT_POLL_NAME_LENGTH;
        }

        if (descriptionValue.length() > Nxt.MAX_POLL_DESCRIPTION_LENGTH) {
            return INCORRECT_POLL_DESCRIPTION_LENGTH;
        }

        List<String> options = new ArrayList<>();
        while (options.size() < 100) {
            String optionValue = req.getParameter("option" + options.size());
            if (optionValue == null) {
                break;
            }
            if (optionValue.length() > Nxt.MAX_POLL_OPTION_LENGTH) {
                return INCORRECT_POLL_OPTION_LENGTH;
            }
            options.add(optionValue.trim());
        }

        byte minNumberOfOptions;
        try {
            minNumberOfOptions = Byte.parseByte(minNumberOfOptionsValue);
        } catch (NumberFormatException e) {
            return INCORRECT_MINNUMBEROFOPTIONS;
        }

        byte maxNumberOfOptions;
        try {
            maxNumberOfOptions = Byte.parseByte(maxNumberOfOptionsValue);
        } catch (NumberFormatException e) {
            return INCORRECT_MAXNUMBEROFOPTIONS;
        }

        boolean optionsAreBinary;
        try {
            optionsAreBinary = Boolean.parseBoolean(optionsAreBinaryValue);
        } catch (NumberFormatException e) {
            return INCORRECT_OPTIONSAREBINARY;
        }

        int fee;
        try {
            fee = Integer.parseInt(feeValue);
            if (fee <= 0 || fee >= Nxt.MAX_BALANCE) {
                return INCORRECT_FEE;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_FEE;
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

        Long referencedTransaction;
        try {
            referencedTransaction = referencedTransactionValue == null ? null : Convert.parseUnsignedLong(referencedTransactionValue);
        } catch (RuntimeException e) {
            return INCORRECT_REFERENCED_TRANSACTION;
        }

        byte[] publicKey = Crypto.getPublicKey(secretPhrase);

        Account account = Account.getAccount(publicKey);
        if (account == null || fee * 100L > account.getUnconfirmedBalance()) {
            return NOT_ENOUGH_FUNDS;
        }
        int timestamp = Convert.getEpochTime();

        Attachment attachment = new Attachment.MessagingPollCreation(nameValue.trim(), descriptionValue.trim(), options.toArray(new String[options.size()]), minNumberOfOptions, maxNumberOfOptions, optionsAreBinary);
        Transaction transaction = Nxt.getTransactionProcessor().newTransaction(timestamp, deadline, publicKey, Genesis.CREATOR_ID, 0, fee, referencedTransaction, attachment);
        transaction.sign(secretPhrase);

        Nxt.getTransactionProcessor().broadcast(transaction);

        JSONObject response = new JSONObject();
        response.put("transaction", transaction.getStringId());
        response.put("bytes", Convert.toHexString(transaction.getBytes()));

        return response;
    }

    @Override
    boolean requirePost() {
        return true;
    }

}
