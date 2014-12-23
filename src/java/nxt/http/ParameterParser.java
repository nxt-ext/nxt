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

import static nxt.http.JSONResponses.*;

final class ParameterParser {

    static byte getByte(HttpServletRequest req, String name, byte min, byte max) throws ParameterException {
        String paramValue = Convert.emptyToNull(req.getParameter(name));
        if (paramValue == null) {
            return 0;
        }
        byte value;
        try {
            value = Byte.parseByte(paramValue);
        } catch (RuntimeException e) {
            throw new ParameterException(incorrect(name));
        }
        if (value < min || value > max) {
            throw new ParameterException(incorrect(name));
        }
        return value;
    }

    static int getInt(HttpServletRequest req, String name, int min, int max, boolean isMandatory) throws ParameterException {
        String paramValue = Convert.emptyToNull(req.getParameter(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new ParameterException(missing(name));
            }
            return 0;
        }
        int value;
        try {
            value = Integer.parseInt(paramValue);
        } catch (RuntimeException e) {
            throw new ParameterException(incorrect(name));
        }
        if (value < min || value > max) {
            throw new ParameterException(incorrect(name));
        }
        return value;
    }

    static long getLong(HttpServletRequest req, String name, long min, long max, boolean isMandatory) throws ParameterException {
        String paramValue = Convert.emptyToNull(req.getParameter(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new ParameterException(missing(name));
            }
            return 0;
        }
        long value;
        try {
            value = Long.parseLong(paramValue);
        } catch (RuntimeException e) {
            throw new ParameterException(incorrect(name));
        }
        if (value < min || value > max) {
            throw new ParameterException(incorrect(name));
        }
        return value;
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

    static Asset getAsset(HttpServletRequest req) throws ParameterException {
        String assetValue = Convert.emptyToNull(req.getParameter("asset"));
        if (assetValue == null) {
            throw new ParameterException(MISSING_ASSET);
        }
        Asset asset;
        try {
            long assetId = Convert.parseUnsignedLong(assetValue);
            asset = Asset.getAsset(assetId);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_ASSET);
        }
        if (asset == null) {
            throw new ParameterException(UNKNOWN_ASSET);
        }
        return asset;
    }

    static Currency getCurrency(HttpServletRequest req) throws ParameterException {
        String currencyValue = Convert.emptyToNull(req.getParameter("currency"));
        if (currencyValue == null) {
            throw new ParameterException(MISSING_CURRENCY);
        }
        Currency currency;
        try {
            long currencyId = Convert.parseUnsignedLong(currencyValue);
            currency = Currency.getCurrency(currencyId);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_CURRENCY);
        }
        if (currency == null) {
            throw new ParameterException(UNKNOWN_CURRENCY);
        }
        return currency;
    }

    static CurrencyBuyOffer getBuyOffer(HttpServletRequest req) throws ParameterException {
        String offerValue = Convert.emptyToNull(req.getParameter("offer"));
        if (offerValue == null) {
            throw new ParameterException(MISSING_OFFER);
        }
        CurrencyBuyOffer offer;
        try {
            long offerId = Convert.parseUnsignedLong(offerValue);
            offer = CurrencyBuyOffer.getOffer(offerId);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_OFFER);
        }
        if (offer == null) {
            throw new ParameterException(UNKNOWN_OFFER);
        }
        return offer;
    }

    static CurrencySellOffer getSellOffer(HttpServletRequest req) throws ParameterException {
        String offerValue = Convert.emptyToNull(req.getParameter("offer"));
        if (offerValue == null) {
            throw new ParameterException(MISSING_OFFER);
        }
        CurrencySellOffer offer;
        try {
            long offerId = Convert.parseUnsignedLong(offerValue);
            offer = CurrencySellOffer.getOffer(offerId);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_OFFER);
        }
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

    static long getOrderId(HttpServletRequest req) throws ParameterException {
        String orderValue = Convert.emptyToNull(req.getParameter("order"));
        if (orderValue == null) {
            throw new ParameterException(MISSING_ORDER);
        }
        try {
            return Convert.parseUnsignedLong(orderValue);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_ORDER);
        }
    }

    static DigitalGoodsStore.Goods getGoods(HttpServletRequest req) throws ParameterException {
        String goodsValue = Convert.emptyToNull(req.getParameter("goods"));
        if (goodsValue == null) {
            throw new ParameterException(MISSING_GOODS);
        }
        DigitalGoodsStore.Goods goods;
        try {
            long goodsId = Convert.parseUnsignedLong(goodsValue);
            goods = DigitalGoodsStore.Goods.getGoods(goodsId);
            if (goods == null) {
                throw new ParameterException(UNKNOWN_GOODS);
            }
            return goods;
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_GOODS);
        }
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
        String purchaseIdString = Convert.emptyToNull(req.getParameter("purchase"));
        if (purchaseIdString == null) {
            throw new ParameterException(MISSING_PURCHASE);
        }
        try {
            DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.Purchase.getPurchase(Convert.parseUnsignedLong(purchaseIdString));
            if (purchase == null) {
                throw new ParameterException(INCORRECT_PURCHASE);
            }
            return purchase;
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_PURCHASE);
        }
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
        String accountValue = Convert.emptyToNull(req.getParameter("account"));
        if (accountValue == null) {
            throw new ParameterException(MISSING_ACCOUNT);
        }
        try {
            Account account = Account.getAccount(Convert.parseAccountId(accountValue));
            if (account == null) {
                throw new ParameterException(UNKNOWN_ACCOUNT);
            }
            return account;
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_ACCOUNT);
        }
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

    static long getRecipientId(HttpServletRequest req) throws ParameterException {
        String recipientValue = Convert.emptyToNull(req.getParameter("recipient"));
        if (recipientValue == null || "0".equals(recipientValue)) {
            throw new ParameterException(MISSING_RECIPIENT);
        }
        long recipientId;
        try {
            recipientId = Convert.parseAccountId(recipientValue);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_RECIPIENT);
        }
        if (recipientId == 0) {
            throw new ParameterException(INCORRECT_RECIPIENT);
        }
        return recipientId;
    }

    static long getSellerId(HttpServletRequest req) throws ParameterException {
        String sellerIdValue = Convert.emptyToNull(req.getParameter("seller"));
        try {
            return Convert.parseAccountId(sellerIdValue);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_RECIPIENT);
        }
    }

    static long getBuyerId(HttpServletRequest req) throws ParameterException {
        String buyerIdValue = Convert.emptyToNull(req.getParameter("buyer"));
        try {
            return Convert.parseAccountId(buyerIdValue);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_RECIPIENT);
        }
    }

    static int getFirstIndex(HttpServletRequest req) {
        int firstIndex;
        try {
            firstIndex = Integer.parseInt(req.getParameter("firstIndex"));
            if (firstIndex < 0) {
                return 0;
            }
        } catch (NumberFormatException e) {
            return 0;
        }
        return firstIndex;
    }

    static int getLastIndex(HttpServletRequest req) {
        int lastIndex;
        try {
            lastIndex = Integer.parseInt(req.getParameter("lastIndex"));
            if (lastIndex < 0) {
                return Integer.MAX_VALUE;
            }
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
        return lastIndex;
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
