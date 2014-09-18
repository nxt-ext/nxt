package nxt.http;

import nxt.*;
import nxt.util.Convert;
import nxt.Attachment.MessagingPollCreation.PollBuilder;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static nxt.http.JSONResponses.*;

public final class CreatePoll extends CreateTransaction {

    static final CreatePoll instance = new CreatePoll();

    private CreatePoll() {
        super(new APITag[] {APITag.VS, APITag.CREATE_TRANSACTION},
                "name", "description", "finishHeight", "optionModel", "votingModel",
                "option1", "option2", "option3"); // hardcoded to 3 options for testing todo: fix ?
        //todo: minNumberOfOptions, maxNumberOfOptions, minBalance, asstId
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String nameValue = req.getParameter("name");
        String descriptionValue = req.getParameter("description");

        String finishHeightValue = Convert.emptyToNull(req.getParameter("finishHeight"));

        String optionModelValue = Convert.emptyToNull(req.getParameter("optionModel"));
        String votingModelValue = Convert.emptyToNull(req.getParameter("votingModel"));

        if (nameValue == null) {
            return MISSING_NAME;
        } else if (descriptionValue == null) {
            return MISSING_DESCRIPTION;
        } else if (finishHeightValue == null) {
            return MISSING_FINISHHEIGHT;
        } else if (optionModelValue == null) {
            return MISSING_OPTIONMODEL;
        } else if (votingModelValue == null) {
            return MISSING_VOTINGMODEL;
        }

        if (nameValue.length() > Constants.MAX_POLL_NAME_LENGTH) {
            return INCORRECT_POLL_NAME_LENGTH;
        }

        if (descriptionValue.length() > Constants.MAX_POLL_DESCRIPTION_LENGTH) {
            return INCORRECT_POLL_DESCRIPTION_LENGTH;
        }

        List<String> options = new ArrayList<>(Constants.MAX_POLL_OPTION_COUNT / 20);
        while (options.size() < Constants.MAX_POLL_OPTION_COUNT) {
            String optionValue = req.getParameter("option" + options.size());
            if (optionValue == null) {
                break;
            }
            if (optionValue.length() > Constants.MAX_POLL_OPTION_LENGTH) {
                return INCORRECT_POLL_OPTION_LENGTH;
            }
            options.add(optionValue.trim());
        }

        int finishHeight;
        try {
            finishHeight = Integer.parseInt(finishHeightValue);
            if (finishHeight <= Nxt.getBlockchain().getHeight()) {
                return INCORRECT_FINISHHEIGHT;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_FINISHHEIGHT;
        }

        byte optionModel;
        try {
            optionModel = Byte.parseByte(optionModelValue);
            if (optionModel != Poll.OPTION_MODEL_BINARY && optionModel != Poll.OPTION_MODEL_CHOICE) {
                return INCORRECT_OPTIONMODEL;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_OPTIONMODEL;
        }

        byte votingModel;
        try {
            votingModel = Byte.parseByte(votingModelValue);
            if (votingModel != Poll.VOTING_MODEL_ACCOUNT && votingModel != Poll.VOTING_MODEL_ASSET &&
                    votingModel != Poll.VOTING_MODEL_BALANCE) {
                return INCORRECT_VOTINGMODEL;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_VOTINGMODEL;
        }

        String minBalanceValue = Convert.emptyToNull(req.getParameter("minBalance"));
        long minBalance = Poll.DEFAULT_MIN_BALANCE;
        if (minBalanceValue != null) {
            try {
                minBalance = Long.parseLong(minBalanceValue);
                if (minBalance < 0) {
                    return INCORRECT_MINBALANCE;
                }
            } catch (NumberFormatException e) {
                return INCORRECT_MINBALANCE;
            }
        }

        PollBuilder builder = new PollBuilder(nameValue.trim(), descriptionValue.trim(),
                options.toArray(new String[options.size()]),
                finishHeight, optionModel, votingModel);
        builder.minBalance(minBalance);


        if (optionModel == Poll.OPTION_MODEL_CHOICE) {
            String minNumberOfOptionsValue = req.getParameter("minNumberOfOptions");
            String maxNumberOfOptionsValue = req.getParameter("maxNumberOfOptions");

            if (minNumberOfOptionsValue == null) {
                return MISSING_MINNUMBEROFOPTIONS;
            } else if (maxNumberOfOptionsValue == null) {
                return MISSING_MAXNUMBEROFOPTIONS;
            }

            byte minNumberOfOptions;
            try {
                minNumberOfOptions = Byte.parseByte(minNumberOfOptionsValue);
                if (minNumberOfOptions < 1 || minNumberOfOptions > options.size()) {
                    return INCORRECT_MINNUMBEROFOPTIONS;
                }
            } catch (NumberFormatException e) {
                return INCORRECT_MINNUMBEROFOPTIONS;
            }

            byte maxNumberOfOptions;
            try {
                maxNumberOfOptions = Byte.parseByte(maxNumberOfOptionsValue);
                if (maxNumberOfOptions < 1 || maxNumberOfOptions > options.size()) {
                    return INCORRECT_MAXNUMBEROFOPTIONS;
                }
            } catch (NumberFormatException e) {
                return INCORRECT_MAXNUMBEROFOPTIONS;
            }

            builder.optionsNumRange(minNumberOfOptions, maxNumberOfOptions);
        }

        if (votingModel == Poll.VOTING_MODEL_ASSET) {
            String assetIdValue = Convert.emptyToNull(req.getParameter("assetId"));

            try {
                long assetId = Convert.parseUnsignedLong(assetIdValue);
                if (Asset.getAsset(assetId) == null) {
                    return INCORRECT_ASSET;
                }
                builder.assetId(assetId);
            } catch (NumberFormatException e) {
                return INCORRECT_ASSET;
            }
        }

        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new Attachment.MessagingPollCreation(builder);
        return createTransaction(req, account, attachment);
    }
}
