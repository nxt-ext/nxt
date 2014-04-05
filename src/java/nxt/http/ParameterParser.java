package nxt.http;

import nxt.Account;
import nxt.Asset;
import nxt.Constants;
import nxt.crypto.Crypto;
import nxt.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.DUPLICATE_AMOUNT;
import static nxt.http.JSONResponses.DUPLICATE_FEE;
import static nxt.http.JSONResponses.DUPLICATE_PRICE;
import static nxt.http.JSONResponses.DUPLICATE_QUANTITY;
import static nxt.http.JSONResponses.INCORRECT_ACCOUNT;
import static nxt.http.JSONResponses.INCORRECT_AMOUNT;
import static nxt.http.JSONResponses.INCORRECT_ASSET;
import static nxt.http.JSONResponses.INCORRECT_FEE;
import static nxt.http.JSONResponses.INCORRECT_ORDER;
import static nxt.http.JSONResponses.INCORRECT_PRICE;
import static nxt.http.JSONResponses.INCORRECT_QUANTITY;
import static nxt.http.JSONResponses.INCORRECT_RECIPIENT;
import static nxt.http.JSONResponses.INCORRECT_TIMESTAMP;
import static nxt.http.JSONResponses.MISSING_ACCOUNT;
import static nxt.http.JSONResponses.MISSING_AMOUNT;
import static nxt.http.JSONResponses.MISSING_ASSET;
import static nxt.http.JSONResponses.MISSING_FEE;
import static nxt.http.JSONResponses.MISSING_ORDER;
import static nxt.http.JSONResponses.MISSING_PRICE;
import static nxt.http.JSONResponses.MISSING_QUANTITY;
import static nxt.http.JSONResponses.MISSING_RECIPIENT;
import static nxt.http.JSONResponses.MISSING_TIMESTAMP;
import static nxt.http.JSONResponses.UNKNOWN_ACCOUNT;
import static nxt.http.JSONResponses.UNKNOWN_ASSET;

final class ParameterParser {

    static long getAmountNQT(HttpServletRequest req) throws ParameterException {
        String amountValueNXT = Convert.emptyToNull(req.getParameter("amountNXT"));
        String amountValueNQT = Convert.emptyToNull(req.getParameter("amountNQT"));
        if (amountValueNXT == null && amountValueNQT == null) {
            throw new ParameterException(MISSING_AMOUNT);
        } else if (amountValueNXT != null && amountValueNQT != null) {
            throw new ParameterException(DUPLICATE_AMOUNT);
        }
        long amountNQT;
        try {
            amountNQT = amountValueNQT != null ? Long.parseLong(amountValueNQT) : Convert.parseNXT(amountValueNXT);
        } catch (NumberFormatException e) {
            throw new ParameterException(INCORRECT_AMOUNT);
        }
        if (amountNQT <= 0 || amountNQT >= Constants.MAX_BALANCE_NXT * Constants.ONE_NXT) {
            throw new ParameterException(INCORRECT_AMOUNT);
        }
        return amountNQT;
    }

    static long getFeeNQT(HttpServletRequest req) throws ParameterException {
        String feeValueNXT = Convert.emptyToNull(req.getParameter("feeNXT"));
        String feeValueNQT = Convert.emptyToNull(req.getParameter("feeNQT"));
        if (feeValueNXT == null && feeValueNQT == null) {
            throw new ParameterException(MISSING_FEE);
        } else if (feeValueNXT != null && feeValueNQT != null) {
            throw new ParameterException(DUPLICATE_FEE);
        }
        long feeNQT;
        try {
            feeNQT = feeValueNQT != null ? Long.parseLong(feeValueNQT) : Convert.parseNXT(feeValueNXT);
        } catch (NumberFormatException e) {
            throw new ParameterException(INCORRECT_FEE);
        }
        if (feeNQT <= 0 || feeNQT >= Constants.MAX_BALANCE_NXT * Constants.ONE_NXT) {
            throw new ParameterException(INCORRECT_FEE);
        }
        return feeNQT;
    }

    static long getPriceNQT(HttpServletRequest req, byte decimals) throws ParameterException {
        String priceValueNXT = Convert.emptyToNull(req.getParameter("priceNXT"));
        String priceValueNQT = Convert.emptyToNull(req.getParameter("priceNQT"));
        if (priceValueNXT == null && priceValueNQT == null) {
            throw new ParameterException(MISSING_PRICE);
        } else if (priceValueNXT != null && priceValueNQT != null) {
            throw new ParameterException(DUPLICATE_PRICE);
        }
        long priceNQT;
        try {
            priceNQT = priceValueNQT != null ? Long.parseLong(priceValueNQT) : Convert.parseNXT(priceValueNXT);
        } catch (NumberFormatException e) {
            throw new ParameterException(INCORRECT_PRICE);
        }
        String quantityINT = Convert.emptyToNull(req.getParameter("quantityINT"));
        if (quantityINT != null) { // quantity specified in whole units, recalculate price per QNT
            if (priceNQT % Convert.multiplier(decimals) != 0) {
                throw new ParameterException(INCORRECT_PRICE);
            }
            priceNQT = priceNQT / Convert.multiplier(decimals);
        }
        if (priceNQT <= 0 || priceNQT > Constants.MAX_BALANCE_NXT * Constants.ONE_NXT) {
            throw new ParameterException(INCORRECT_PRICE);
        }
        return priceNQT;
    }

    static Asset getAsset(HttpServletRequest req) throws ParameterException {
        String assetValue = req.getParameter("asset");
        if (assetValue == null) {
            throw new ParameterException(MISSING_ASSET);
        }
        Asset asset;
        try {
            Long assetId = Convert.parseUnsignedLong(assetValue);
            asset = Asset.getAsset(assetId);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_ASSET);
        }
        if (asset == null) {
            throw new ParameterException(UNKNOWN_ASSET);
        }
        return asset;
    }

    static long getQuantityQNT(HttpServletRequest req, byte decimals) throws ParameterException {
        String quantityValueQNT = Convert.emptyToNull(req.getParameter("quantityQNT"));
        String quantityValueINT = Convert.emptyToNull(req.getParameter("quantityINT"));
        if (quantityValueQNT == null && quantityValueINT == null) {
            throw new ParameterException(MISSING_QUANTITY);
        } else if (quantityValueQNT != null && quantityValueINT != null) {
            throw new ParameterException(DUPLICATE_QUANTITY);
        }
        long quantityQNT;
        try {
            quantityQNT = quantityValueQNT != null ? Long.parseLong(quantityValueQNT)
                    : Convert.parseQuantityINT(quantityValueINT, decimals);
        } catch (NumberFormatException e) {
            throw new ParameterException(INCORRECT_QUANTITY);
        }
        if (quantityQNT <= 0 || quantityQNT > Convert.safeMultiply(Constants.MAX_ASSET_QUANTITY, Convert.multiplier(decimals))) {
            throw new ParameterException(INCORRECT_QUANTITY);
        }
        return quantityQNT;
    }

    static Long getOrderId(HttpServletRequest req) throws ParameterException {
        String orderValue = req.getParameter("order");
        if (orderValue == null) {
            throw new ParameterException(MISSING_ORDER);
        }
        try {
            return Convert.parseUnsignedLong(orderValue);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_ORDER);
        }
    }

    static Account getSenderAccount(HttpServletRequest req) throws ParameterException {
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        if (secretPhrase != null) {
            return Account.getAccount(Crypto.getPublicKey(secretPhrase));
        }
        String publicKeyString = Convert.emptyToNull(req.getParameter("publicKey"));
        if (publicKeyString == null) {
            throw new ParameterException(UNKNOWN_ACCOUNT);
        }
        try {
            return Account.getAccount(Convert.parseHexString(publicKeyString));
        } catch (RuntimeException e) {
            throw new ParameterException(UNKNOWN_ACCOUNT);
        }
    }

    static Account getAccount(HttpServletRequest req) throws ParameterException {
        String accountValue = req.getParameter("account");
        if (accountValue == null) {
            throw new ParameterException(MISSING_ACCOUNT);
        }
        Account account;
        try {
            account = Account.getAccount(Convert.parseUnsignedLong(accountValue));
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_ACCOUNT);
        }
        if (account == null) {
            throw new ParameterException(UNKNOWN_ACCOUNT);
        }
        return account;
    }

    static int getTimestamp(HttpServletRequest req) throws ParameterException {
        String timestampValue = req.getParameter("timestamp");
        if (timestampValue == null) {
            throw new ParameterException(MISSING_TIMESTAMP);
        }
        int timestamp;
        try {
            timestamp = Integer.parseInt(timestampValue);
        } catch (NumberFormatException e) {
            throw new ParameterException(INCORRECT_TIMESTAMP);
        }
        if (timestamp < 0) {
            throw new ParameterException(INCORRECT_TIMESTAMP);
        }
        return timestamp;
    }

    static Long getRecipientId(HttpServletRequest req) throws ParameterException {
        String recipientValue = req.getParameter("recipient");
        if (recipientValue == null || "0".equals(recipientValue)) {
            throw new ParameterException(MISSING_RECIPIENT);
        }
        try {
            return Convert.parseUnsignedLong(recipientValue);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_RECIPIENT);
        }
    }

    private ParameterParser() {} // never

}
