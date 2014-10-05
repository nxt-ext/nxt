package nxt.http;

import nxt.*;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.*;

/**
 * Issue a currency on the NXT blockchain
 * <p/>
 * A currency is the basic block of the NXT Monetary System it can be exchanged with NXT, transferred between accounts,
 * minted using proof of work methods, reserved and claimed as a crowd funding tool.
 * <p/>
 * Pass the following parameters in order to issue a currency
 * <ul>
 * <li>name - unique identifier of the currency composed of between 3 to 10 latin alphabetic symbols and numbers
 * <li>code - unique 3 letter currency trading symbol composed of upper case latin letters
 * <li>description - free text description of the currency limited to 1000 characters
 * <li>type - a numeric value representing a bit vector modeling the currency capabilities (see below)
 * <li>ruleset - for future use, always set to 0
 * <li>totalSupply - the total number of currency units which can be created
 * <li>issuanceHeight - the blockchain height at which the currency would become active
 * <li>minReservePerUnitNQT - the minimum NXT value per unit to allow the currency to become active.
 * For {@link nxt.CurrencyType#RESERVABLE} currency
 * <li>initialSupply - the number of currency units created when the currency is issued.
 * For {@link nxt.CurrencyType#MINTABLE} currency
 * <li>minDifficulty - for mint-able currency, the exponent of the initial difficulty.
 * For {@link nxt.CurrencyType#MINTABLE} currency
 * <li>maxDifficulty - for mint-able currency, the exponent of the final difficulty.
 * For {@link nxt.CurrencyType#MINTABLE} currency
 * <li>algorithm - the hashing {@link nxt.crypto.HashFunction algorithm} used for minting.
 * For {@link nxt.CurrencyType#MINTABLE} currency
 * </ul>
 * <p/>
 * Constraints
 * <ul>
 * <li>A given currency must be either {@link nxt.CurrencyType#EXCHANGEABLE} or {@link nxt.CurrencyType#CLAIMABLE} but not both.<br>
 * Therefore you can either use the {@link PublishExchangeOffer} and {@link CurrencyBuy} / {@link CurrencySell} APIs or<br>
 * the {@link CurrencyReserveClaim} API but not both
 * <li>Currency becomes active once the blockchain height reaches the currency issuance height.<br>
 * At this time, in case the currency is {@link nxt.CurrencyType#RESERVABLE} and the minReservePerUnitNQT has not been reached the currency issuance is cancelled and
 * funds are returned to the founders.<br>
 * Otherwise the currency becomes active and remain active forever
 * <li>When issuing a {@link nxt.CurrencyType#MINTABLE} currency, the number of units per {@link nxt.http.CurrencyMint} cannot exceed 0.01% of the
 * total supply. Therefore make sure totalSupply > 10000 or otherwise the currency cannot be minted
 * <li>difficulty is calculated as follows<br>
 * difficulty of minting the first unit is based on 2^minDifficulty<br>
 * difficulty of minting the last unit is based on 2^maxDifficulty<br>
 * difficulty increases linearly from min to max based on the ratio between the current number of units and the total supply<br>
 * difficulty increases linearly with the number units minted per {@link nxt.http.CurrencyMint}<br>
 * </ul>
 *
 * @see nxt.CurrencyType
 * @see nxt.crypto.HashFunction
 */
public final class IssueCurrency extends CreateTransaction {

    static final IssueCurrency instance = new IssueCurrency();

    private IssueCurrency() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION},
                "name", "code", "description", "type", "totalSupply", "initialSupply", "issuanceHeight", "minReservePerUnitNQT",
                "minDifficulty", "maxDifficulty", "ruleset", "algorithm");
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

        byte type = ParameterParser.getByte(req, "type", (byte)0, Byte.MAX_VALUE);
        long totalSupply = ParameterParser.getLong(req, "totalSupply", 1, Constants.MAX_CURRENCY_TOTAL_SUPPLY, false);
        long initialSupply = ParameterParser.getLong(req, "initialSupply", 0, totalSupply, false);
        int issuanceHeight = ParameterParser.getInt(req, "issuanceHeight", 0, Integer.MAX_VALUE, false);
        long minReservePerUnit = ParameterParser.getLong(req, "minReservePerUnitNQT", 0, Constants.MAX_BALANCE_NQT, false);
        byte minDifficulty = ParameterParser.getByte(req, "minDifficulty", (byte)0, Byte.MAX_VALUE);
        byte maxDifficulty = ParameterParser.getByte(req, "maxDifficulty", (byte)0, Byte.MAX_VALUE);
        byte ruleset = ParameterParser.getByte(req, "ruleset", (byte)0, Byte.MAX_VALUE);
        byte algorithm = ParameterParser.getByte(req, "algorithm", (byte)0, Byte.MAX_VALUE);
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.MonetarySystemCurrencyIssuance(name, code, description, type, totalSupply,
                initialSupply, issuanceHeight, minReservePerUnit, minDifficulty, maxDifficulty, ruleset, algorithm);

        return createTransaction(req, account, attachment);

    }
}
