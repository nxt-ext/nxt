package nxt.http;

import nxt.*;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static nxt.http.JSONResponses.*;

public final class CreatePoll extends CreateTransaction {

    static final CreatePoll instance = new CreatePoll();

    private CreatePoll() {
        super("name", "description", "minNumberOfOptions", "maxNumberOfOptions", "optionsAreBinary",
                "option1", "option2", "option3"); // hardcoded to 3 options for testing
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String nameValue = req.getParameter("name");
        String descriptionValue = req.getParameter("description");

        String finishHeightValue = Convert.emptyToNull(req.getParameter("finishHeight"));

        String minNumberOfOptionsValue = req.getParameter("minNumberOfOptions");
        String maxNumberOfOptionsValue = req.getParameter("maxNumberOfOptions");

        String optionModelValue = Convert.emptyToNull(req.getParameter("optionModel"));
        String votingModelValue = Convert.emptyToNull(req.getParameter("votingModel"));
        String parameter1Value = Convert.emptyToNull(req.getParameter("parameter1"));

        if (nameValue == null) {
            return MISSING_NAME;
        } else if (descriptionValue == null) {
            return MISSING_DESCRIPTION;
        } else if (minNumberOfOptionsValue == null) {
            return MISSING_MINNUMBEROFOPTIONS;
        } else if (maxNumberOfOptionsValue == null) {
            return MISSING_MAXNUMBEROFOPTIONS;
        } else if (optionModelValue == null) {
            return MISSING_OPTIONMODEL;
        } else if (votingModelValue == null) {
            return MISSING_VOTINGMODEL;
        } else if (finishHeightValue == null) {
            return MISSING_FINISHHEIGHT;
        }

        if (nameValue.length() > Constants.MAX_POLL_NAME_LENGTH) {
            return INCORRECT_POLL_NAME_LENGTH;
        }

        if (descriptionValue.length() > Constants.MAX_POLL_DESCRIPTION_LENGTH) {
            return INCORRECT_POLL_DESCRIPTION_LENGTH;
        }

        List<String> options = new ArrayList<>();
        while (options.size() < 100) {
            String optionValue = req.getParameter("option" + options.size());
            if (optionValue == null) {
                break;
            }
            if (optionValue.length() > Constants.MAX_POLL_OPTION_LENGTH) {
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

        int finishHeight;
        try {
            finishHeight = Integer.parseInt(finishHeightValue);
        } catch (NumberFormatException e) {
            return INCORRECT_FINISHHEIGHT;
        }

        byte optionModel;
        try {
            optionModel = Byte.parseByte(optionModelValue);
        } catch (NumberFormatException e) {
            return INCORRECT_OPTIONMODEL;
        }

        byte votingModel;
        try {
            votingModel = Byte.parseByte(votingModelValue);
        } catch (NumberFormatException e) {
            return INCORRECT_VOTINGMODEL;
        }

        long parameter1 = 0;
        if(parameter1Value != null){
            try {
                parameter1 = Long.parseLong(parameter1Value);
            } catch (NumberFormatException e) {
                return INCORRECT_PARAMETER1;
            }
        }

        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new Attachment.MessagingPollCreation(nameValue.trim(), descriptionValue.trim(),
                finishHeight,
                options.toArray(new String[options.size()]), minNumberOfOptions, maxNumberOfOptions,
                optionModel, votingModel, Poll.COUNTING_AT_THE_END, parameter1);
        return createTransaction(req, account, attachment);
    }
}
