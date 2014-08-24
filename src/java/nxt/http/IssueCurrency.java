package nxt.http;

import nxt.*;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.*;

public final class IssueCurrency extends CreateTransaction {

    static final IssueCurrency instance = new IssueCurrency();

    private IssueCurrency() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION},
                "name", "code", "description", "type", "totalSupply", "issuanceHeight", "minReservePerUnitNQT",
                "minDifficulty", "maxDifficulty", "ruleset");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        String name = Convert.nullToEmpty(req.getParameter("name"));
        name = name.trim();
        if (name.length() < Constants.MIN_CURRENCY_NAME_LENGTH || name.length() > Constants.MAX_CURRENCY_NAME_LENGTH) {
            return INCORRECT_CURRENCY_NAME_LENGTH;
        }
        String normalizedName = name.toLowerCase();
        for (int i = 0; i < normalizedName.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                return INCORRECT_CURRENCY_NAME;
            }
        }
        String code = Convert.nullToEmpty(req.getParameter("code"));
        if (code.length() != Constants.CURRENCY_CODE_LENGTH) {
            return INCORRECT_CURRENCY_CODE_LENGTH;
        }
        String description = Convert.nullToEmpty(req.getParameter("description"));
        if (description.length() > Constants.MAX_CURRENCY_DESCRIPTION_LENGTH) {
            return INCORRECT_CURRENCY_DESCRIPTION;
        }

        byte type = ParameterParser.getByte(req, "type", (byte)0, (byte)(CurrencyType.getSize() - 1));
        long totalSupply = ParameterParser.getLong(req, "totalSupply", 1, Constants.MAX_CURRENCY_TOTAL_SUPPLY);
        int issuanceHeight = ParameterParser.getInt(req, "issuanceHeight", 0, Integer.MAX_VALUE);
        long minReservePerUnit = ParameterParser.getLong(req, "minReservePerUnitNQT", 0, Constants.MAX_BALANCE_NQT);
        byte minDifficulty = ParameterParser.getByte(req, "minDifficulty", (byte)0, Byte.MAX_VALUE);
        byte maxDifficulty = ParameterParser.getByte(req, "maxDifficulty", (byte)0, Byte.MAX_VALUE);
        byte ruleset = ParameterParser.getByte(req, "ruleset", (byte)0, Byte.MAX_VALUE);
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.MonetarySystemCurrencyIssuance(name, code, description, type, totalSupply,
                issuanceHeight, minReservePerUnit, minDifficulty, maxDifficulty, ruleset);
        return createTransaction(req, account, attachment);

    }
}
