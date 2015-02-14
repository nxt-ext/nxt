package nxt.http;

import nxt.Account;
import nxt.Alias;
import nxt.Asset;
import nxt.Constants;
import nxt.Currency;
import nxt.CurrencyBuyOffer;
import nxt.CurrencySellOffer;
import nxt.DigitalGoodsStore;
import nxt.Nxt;
import nxt.NxtException;
import nxt.PhasingPoll;
import nxt.Poll;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static nxt.http.JSONResponses.HEIGHT_NOT_AVAILABLE;
import static nxt.http.JSONResponses.INCORRECT_ACCOUNT;
import static nxt.http.JSONResponses.INCORRECT_ALIAS;
import static nxt.http.JSONResponses.INCORRECT_DGS_ENCRYPTED_GOODS;
import static nxt.http.JSONResponses.INCORRECT_ENCRYPTED_MESSAGE;
import static nxt.http.JSONResponses.INCORRECT_HEIGHT;
import static nxt.http.JSONResponses.INCORRECT_PENDING_TRANSACTION;
import static nxt.http.JSONResponses.INCORRECT_PLAIN_MESSAGE;
import static nxt.http.JSONResponses.INCORRECT_PUBLIC_KEY;
import static nxt.http.JSONResponses.INCORRECT_PURCHASE;
import static nxt.http.JSONResponses.INCORRECT_RECIPIENT;
import static nxt.http.JSONResponses.MISSING_ACCOUNT;
import static nxt.http.JSONResponses.MISSING_ALIAS_OR_ALIAS_NAME;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE_OR_PUBLIC_KEY;
import static nxt.http.JSONResponses.MISSING_TRANSACTION_BYTES_OR_JSON;
import static nxt.http.JSONResponses.UNKNOWN_ACCOUNT;
import static nxt.http.JSONResponses.UNKNOWN_ALIAS;
import static nxt.http.JSONResponses.UNKNOWN_ASSET;
import static nxt.http.JSONResponses.UNKNOWN_CURRENCY;
import static nxt.http.JSONResponses.UNKNOWN_GOODS;
import static nxt.http.JSONResponses.UNKNOWN_OFFER;
import static nxt.http.JSONResponses.UNKNOWN_POLL;
import static nxt.http.JSONResponses.incorrect;
import static nxt.http.JSONResponses.missing;

final class ParameterParser {

    static byte getByte(HttpServletRequest req, String name, byte min, byte max, boolean isMandatory) throws ParameterException {
        String paramValue = Convert.emptyToNull(req.getParameter(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new ParameterException(missing(name));
            }
            return 0;
        }
        try {
            byte value = Byte.parseByte(paramValue);
            if (value < min || value > max) {
                throw new ParameterException(incorrect(name));
            }
            return value;
        } catch (RuntimeException e) {
            throw new ParameterException(incorrect(name));
        }
    }

    static int getInt(HttpServletRequest req, String name, int min, int max, boolean isMandatory) throws ParameterException {
        String paramValue = Convert.emptyToNull(req.getParameter(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new ParameterException(missing(name));
            }
            return 0;
        }
        try {
            int value = Integer.parseInt(paramValue);
            if (value < min || value > max) {
                throw new ParameterException(incorrect(name));
            }
            return value;
        } catch (RuntimeException e) {
            throw new ParameterException(incorrect(name));
        }
    }

    static long getLong(HttpServletRequest req, String name, long min, long max,
                        boolean isMandatory) throws ParameterException {
        String paramValue = Convert.emptyToNull(req.getParameter(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new ParameterException(missing(name));
            }
            return 0;
        }
        try {
            long value = Long.parseLong(paramValue);
            if (value < min || value > max) {
                throw new ParameterException(incorrect(name));
            }
            return value;
        } catch (RuntimeException e) {
            throw new ParameterException(incorrect(name));
        }
    }

    static long getUnsignedLong(HttpServletRequest req, String name, boolean isMandatory) throws ParameterException {
        String paramValue = Convert.emptyToNull(req.getParameter(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new ParameterException(missing(name));
            }
            return 0;
        }
        try {
            long value = Convert.parseUnsignedLong(paramValue);
            if (value == 0) { // 0 is not allowed as an id
                throw new ParameterException(incorrect(name));
            }
            return value;
        } catch (RuntimeException e) {
            throw new ParameterException(incorrect(name));
        }
    }

    static long getAccountId(HttpServletRequest req, String name, boolean isMandatory) throws ParameterException {
        String paramValue = Convert.emptyToNull(req.getParameter(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new ParameterException(missing(name));
            }
            return 0;
        }
        try {
            long value = Convert.parseAccountId(paramValue);
            if (value == 0) {
                throw new ParameterException(incorrect(name));
            }
            return value;
        } catch (RuntimeException e) {
            throw new ParameterException(incorrect(name));
        }
    }

    static boolean getBoolean(HttpServletRequest req, String name, boolean isMandatory) throws ParameterException {
        String paramValue = Convert.emptyToNull(req.getParameter(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new ParameterException(missing(name));
            }
            return false;
        }
        try {
            return Boolean.parseBoolean(paramValue);
        } catch (RuntimeException e) {
            throw new ParameterException(incorrect(name));
        }
    }

    static PhasingPoll getPhasingPoll(HttpServletRequest req) throws ParameterException {
        PhasingPoll phasingPoll = PhasingPoll.getPoll(getUnsignedLong(req, "transaction", true));
        if (phasingPoll ==null) {
            throw new ParameterException(INCORRECT_PENDING_TRANSACTION);
        }
        return phasingPoll;
    }

    static Alias getAlias(HttpServletRequest req) throws ParameterException {
        long aliasId;
        try {
            aliasId = Convert.parseUnsignedLong(Convert.emptyToNull(req.getParameter("alias")));
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_ALIAS);
        }
        String aliasName = Convert.emptyToNull(req.getParameter("aliasName"));
        Alias alias;
        if (aliasId != 0) {
            alias = Alias.getAlias(aliasId);
        } else if (aliasName != null) {
            alias = Alias.getAlias(aliasName);
        } else {
            throw new ParameterException(MISSING_ALIAS_OR_ALIAS_NAME);
        }
        if (alias == null) {
            throw new ParameterException(UNKNOWN_ALIAS);
        }
        return alias;
    }

    static long getAmountNQT(HttpServletRequest req) throws ParameterException {
        return getLong(req, "amountNQT", 1L, Constants.MAX_BALANCE_NQT, true);
    }

    static long getFeeNQT(HttpServletRequest req) throws ParameterException {
        return getLong(req, "feeNQT", 0L, Constants.MAX_BALANCE_NQT, true);
    }

    static long getPriceNQT(HttpServletRequest req) throws ParameterException {
        return getLong(req, "priceNQT", 1L, Constants.MAX_BALANCE_NQT, true);
    }

    static Poll getPoll(HttpServletRequest req) throws ParameterException {
        Poll poll = Poll.getPoll(getUnsignedLong(req, "poll", true));
        if (poll == null) {
            throw new ParameterException(UNKNOWN_POLL);
        }
        return poll;
    }

    static Asset getAsset(HttpServletRequest req) throws ParameterException {
        Asset asset = Asset.getAsset(getUnsignedLong(req, "asset", true));
        if (asset == null) {
            throw new ParameterException(UNKNOWN_ASSET);
        }
        return asset;
    }

    static Currency getCurrency(HttpServletRequest req) throws ParameterException {
        Currency currency = Currency.getCurrency(getUnsignedLong(req, "currency", true));
        if (currency == null) {
            throw new ParameterException(UNKNOWN_CURRENCY);
        }
        return currency;
    }

    static CurrencyBuyOffer getBuyOffer(HttpServletRequest req) throws ParameterException {
        CurrencyBuyOffer offer = CurrencyBuyOffer.getOffer(getUnsignedLong(req, "offer", true));
        if (offer == null) {
            throw new ParameterException(UNKNOWN_OFFER);
        }
        return offer;
    }

    static CurrencySellOffer getSellOffer(HttpServletRequest req) throws ParameterException {
        CurrencySellOffer offer = CurrencySellOffer.getOffer(getUnsignedLong(req, "offer", true));
        if (offer == null) {
            throw new ParameterException(UNKNOWN_OFFER);
        }
        return offer;
    }

    static long getQuantityQNT(HttpServletRequest req) throws ParameterException {
        return getLong(req, "quantityQNT", 1L, Constants.MAX_ASSET_QUANTITY_QNT, true);
    }

    static long getAmountNQTPerQNT(HttpServletRequest req) throws ParameterException {
        return getLong(req, "amountNQTPerQNT", 1L, Constants.MAX_BALANCE_NQT, true);
    }

    static DigitalGoodsStore.Goods getGoods(HttpServletRequest req) throws ParameterException {
        DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(getUnsignedLong(req, "goods", true));
        if (goods == null) {
            throw new ParameterException(UNKNOWN_GOODS);
        }
        return goods;
    }

    static int getGoodsQuantity(HttpServletRequest req) throws ParameterException {
        return getInt(req, "quantity", 0, Constants.MAX_DGS_LISTING_QUANTITY, true);
    }

    static EncryptedData getEncryptedMessage(HttpServletRequest req, Account recipientAccount) throws ParameterException {
        String data = Convert.emptyToNull(req.getParameter("encryptedMessageData"));
        String nonce = Convert.emptyToNull(req.getParameter("encryptedMessageNonce"));
        if (data != null && nonce != null) {
            try {
                return new EncryptedData(Convert.parseHexString(data), Convert.parseHexString(nonce));
            } catch (RuntimeException e) {
                throw new ParameterException(INCORRECT_ENCRYPTED_MESSAGE);
            }
        }
        String plainMessage = Convert.emptyToNull(req.getParameter("messageToEncrypt"));
        if (plainMessage == null) {
            return null;
        }
        if (recipientAccount == null) {
            throw new ParameterException(INCORRECT_RECIPIENT);
        }
        String secretPhrase = getSecretPhrase(req);
        boolean isText = !"false".equalsIgnoreCase(req.getParameter("messageToEncryptIsText"));
        try {
            byte[] plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
            return recipientAccount.encryptTo(plainMessageBytes, secretPhrase);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_PLAIN_MESSAGE);
        }
    }

    static EncryptedData getEncryptToSelfMessage(HttpServletRequest req) throws ParameterException {
        String data = Convert.emptyToNull(req.getParameter("encryptToSelfMessageData"));
        String nonce = Convert.emptyToNull(req.getParameter("encryptToSelfMessageNonce"));
        if (data != null && nonce != null) {
            try {
                return new EncryptedData(Convert.parseHexString(data), Convert.parseHexString(nonce));
            } catch (RuntimeException e) {
                throw new ParameterException(INCORRECT_ENCRYPTED_MESSAGE);
            }
        }
        String plainMessage = Convert.emptyToNull(req.getParameter("messageToEncryptToSelf"));
        if (plainMessage == null) {
            return null;
        }
        String secretPhrase = getSecretPhrase(req);
        Account senderAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase));
        boolean isText = !"false".equalsIgnoreCase(req.getParameter("messageToEncryptToSelfIsText"));
        try {
            byte[] plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
            return senderAccount.encryptTo(plainMessageBytes, secretPhrase);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_PLAIN_MESSAGE);
        }
    }

    static EncryptedData getEncryptedGoods(HttpServletRequest req) throws ParameterException {
        String data = Convert.emptyToNull(req.getParameter("goodsData"));
        String nonce = Convert.emptyToNull(req.getParameter("goodsNonce"));
        if (data != null && nonce != null) {
            try {
                return new EncryptedData(Convert.parseHexString(data), Convert.parseHexString(nonce));
            } catch (RuntimeException e) {
                throw new ParameterException(INCORRECT_DGS_ENCRYPTED_GOODS);
            }
        }
        return null;
    }

    static DigitalGoodsStore.Purchase getPurchase(HttpServletRequest req) throws ParameterException {
        DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.Purchase.getPurchase(getUnsignedLong(req, "purchase", true));
        if (purchase == null) {
            throw new ParameterException(INCORRECT_PURCHASE);
        }
        return purchase;
    }

    static String getSecretPhrase(HttpServletRequest req) throws ParameterException {
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        if (secretPhrase == null) {
            throw new ParameterException(MISSING_SECRET_PHRASE);
        }
        return secretPhrase;
    }

    static Account getSenderAccount(HttpServletRequest req) throws ParameterException {
        Account account;
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        String publicKeyString = Convert.emptyToNull(req.getParameter("publicKey"));
        if (secretPhrase != null) {
            account = Account.getAccount(Crypto.getPublicKey(secretPhrase));
        } else if (publicKeyString != null) {
            try {
                account = Account.getAccount(Convert.parseHexString(publicKeyString));
            } catch (RuntimeException e) {
                throw new ParameterException(INCORRECT_PUBLIC_KEY);
            }
        } else {
            throw new ParameterException(MISSING_SECRET_PHRASE_OR_PUBLIC_KEY);
        }
        if (account == null) {
            throw new ParameterException(UNKNOWN_ACCOUNT);
        }
        return account;
    }

    static Account getAccount(HttpServletRequest req) throws ParameterException {
        Account account = Account.getAccount(getAccountId(req, "account", true));
        if (account == null) {
            throw new ParameterException(UNKNOWN_ACCOUNT);
        }
        return account;
    }

    static List<Account> getAccounts(HttpServletRequest req) throws ParameterException {
        String[] accountValues = req.getParameterValues("account");
        if (accountValues == null || accountValues.length == 0) {
            throw new ParameterException(MISSING_ACCOUNT);
        }
        List<Account> result = new ArrayList<>();
        for (String accountValue : accountValues) {
            if (accountValue == null || accountValue.equals("")) {
                continue;
            }
            try {
                Account account = Account.getAccount(Convert.parseAccountId(accountValue));
                if (account == null) {
                    throw new ParameterException(UNKNOWN_ACCOUNT);
                }
                result.add(account);
            } catch (RuntimeException e) {
                throw new ParameterException(INCORRECT_ACCOUNT);
            }
        }
        return result;
    }

    static int getTimestamp(HttpServletRequest req) throws ParameterException {
        return getInt(req, "timestamp", 0, Integer.MAX_VALUE, false);
    }

    static int getFirstIndex(HttpServletRequest req) {
        try {
            int firstIndex = Integer.parseInt(req.getParameter("firstIndex"));
            if (firstIndex < 0) {
                return 0;
            }
            return firstIndex;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static int getLastIndex(HttpServletRequest req) {
        try {
            int lastIndex = Integer.parseInt(req.getParameter("lastIndex"));
            if (lastIndex < 0) {
                return Integer.MAX_VALUE;
            }
            return lastIndex;
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    static int getNumberOfConfirmations(HttpServletRequest req) throws ParameterException {
        return getInt(req, "numberOfConfirmations", 0, Nxt.getBlockchain().getHeight(), false);
    }

    static int getHeight(HttpServletRequest req) throws ParameterException {
        String heightValue = Convert.emptyToNull(req.getParameter("height"));
        if (heightValue != null) {
            try {
                int height = Integer.parseInt(heightValue);
                if (height < 0 || height > Nxt.getBlockchain().getHeight()) {
                    throw new ParameterException(INCORRECT_HEIGHT);
                }
                if (height < Nxt.getBlockchainProcessor().getMinRollbackHeight()) {
                    throw new ParameterException(HEIGHT_NOT_AVAILABLE);
                }
                return height;
            } catch (NumberFormatException e) {
                throw new ParameterException(INCORRECT_HEIGHT);
            }
        }
        return -1;
    }

    static Transaction parseTransaction(String transactionBytes, String transactionJSON) throws ParameterException {
        if (transactionBytes == null && transactionJSON == null) {
            throw new ParameterException(MISSING_TRANSACTION_BYTES_OR_JSON);
        }
        if (transactionBytes != null) {
            try {
                byte[] bytes = Convert.parseHexString(transactionBytes);
                return Nxt.getTransactionProcessor().parseTransaction(bytes);
            } catch (NxtException.ValidationException|RuntimeException e) {
                Logger.logDebugMessage(e.getMessage(), e);
                JSONObject response = new JSONObject();
                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect transactionBytes: " + e.toString());
                throw new ParameterException(response);
            }
        } else {
            try {
                JSONObject json = (JSONObject) JSONValue.parseWithException(transactionJSON);
                return Nxt.getTransactionProcessor().parseTransaction(json);
            } catch (NxtException.ValidationException | RuntimeException | ParseException e) {
                Logger.logDebugMessage(e.getMessage(), e);
                JSONObject response = new JSONObject();
                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect transactionJSON: " + e.toString());
                throw new ParameterException(response);
            }
        }
    }


    private ParameterParser() {} // never

}
