package nxt.http;

import nxt.Account;
import nxt.Asset;
import nxt.Constants;
import nxt.DigitalGoodsStore;
import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

import static nxt.http.JSONResponses.*;

final class ParameterParser {

    static long getAmountNQT(HttpServletRequest req) throws ParameterException {
        String amountValueNQT = Convert.emptyToNull(req.getParameter("amountNQT"));
        if (amountValueNQT == null) {
            throw new ParameterException(MISSING_AMOUNT);
        }
        long amountNQT;
        try {
            amountNQT = Long.parseLong(amountValueNQT);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_AMOUNT);
        }
        if (amountNQT <= 0 || amountNQT >= Constants.MAX_BALANCE_NQT) {
            throw new ParameterException(INCORRECT_AMOUNT);
        }
        return amountNQT;
    }

    static long getFeeNQT(HttpServletRequest req) throws ParameterException {
        String feeValueNQT = Convert.emptyToNull(req.getParameter("feeNQT"));
        if (feeValueNQT == null) {
            throw new ParameterException(MISSING_FEE);
        }
        long feeNQT;
        try {
            feeNQT = Long.parseLong(feeValueNQT);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_FEE);
        }
        if (feeNQT <= 0 || feeNQT >= Constants.MAX_BALANCE_NQT) {
            throw new ParameterException(INCORRECT_FEE);
        }
        return feeNQT;
    }

    static long getPriceNQT(HttpServletRequest req) throws ParameterException {
        String priceValueNQT = Convert.emptyToNull(req.getParameter("priceNQT"));
        if (priceValueNQT == null) {
            throw new ParameterException(MISSING_PRICE);
        }
        long priceNQT;
        try {
            priceNQT = Long.parseLong(priceValueNQT);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_PRICE);
        }
        if (priceNQT <= 0 || priceNQT > Constants.MAX_BALANCE_NQT) {
            throw new ParameterException(INCORRECT_PRICE);
        }
        return priceNQT;
    }

    static Asset getAsset(HttpServletRequest req) throws ParameterException {
        String assetValue = Convert.emptyToNull(req.getParameter("asset"));
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

    static long getQuantityQNT(HttpServletRequest req) throws ParameterException {
        String quantityValueQNT = Convert.emptyToNull(req.getParameter("quantityQNT"));
        if (quantityValueQNT == null) {
            throw new ParameterException(MISSING_QUANTITY);
        }
        long quantityQNT;
        try {
            quantityQNT = Long.parseLong(quantityValueQNT);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_QUANTITY);
        }
        if (quantityQNT <= 0 || quantityQNT > Constants.MAX_ASSET_QUANTITY_QNT) {
            throw new ParameterException(INCORRECT_QUANTITY);
        }
        return quantityQNT;
    }

    static Long getOrderId(HttpServletRequest req) throws ParameterException {
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
            Long goodsId = Convert.parseUnsignedLong(goodsValue);
            goods = DigitalGoodsStore.getGoods(goodsId);
            if (goods == null) {
                throw new ParameterException(UNKNOWN_GOODS);
            }
            return goods;
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_GOODS);
        }
    }

    static int getGoodsQuantity(HttpServletRequest req) throws ParameterException {
        String quantityString = Convert.emptyToNull(req.getParameter("quantity"));
        try {
            int quantity = Integer.parseInt(quantityString);
            if (quantity < 0 || quantity > Constants.MAX_DGS_LISTING_QUANTITY) {
                throw new ParameterException(INCORRECT_QUANTITY);
            }
            return quantity;
        } catch (NumberFormatException e) {
            throw new ParameterException(INCORRECT_QUANTITY);
        }
    }

    static byte[] getNote(HttpServletRequest req) throws ParameterException {
        try {
            return Convert.nullToEmpty(req.getParameter("note")).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ParameterException(INCORRECT_DGS_NOTE);
        }
    }

    static EncryptedData getEncryptedNote(HttpServletRequest req, Account recipientAccount) throws ParameterException {
        String encryptedNote = Convert.emptyToNull(req.getParameter("encryptedNote"));
        String encryptedNoteNonce = Convert.emptyToNull(req.getParameter("encryptedNoteNonce"));
        if (encryptedNote != null && encryptedNoteNonce != null) {
            try {
                return new EncryptedData(Convert.parseHexString(encryptedNote), Convert.parseHexString(encryptedNoteNonce));
            } catch (RuntimeException e) {
                throw new ParameterException(INCORRECT_DGS_ENCRYPTED_NOTE);
            }
        }
        String secretPhrase = getSecretPhrase(req);
        return recipientAccount.encryptTo(getNote(req), secretPhrase);
    }

    static EncryptedData getEncryptedGoods(HttpServletRequest req) throws ParameterException {
        String encryptedGoodsData = Convert.emptyToNull(req.getParameter("encryptedGoodsData"));
        String encryptedGoodsNonce = Convert.emptyToNull(req.getParameter("encryptedGoodsNonce"));
        if (encryptedGoodsData != null && encryptedGoodsNonce != null) {
            try {
                return new EncryptedData(Convert.parseHexString(encryptedGoodsData), Convert.parseHexString(encryptedGoodsNonce));
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
            DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.getPurchase(Convert.parseUnsignedLong(purchaseIdString));
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
        Account account;
        try {
            account = Account.getAccount(Convert.parseAccountId(accountValue));
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_ACCOUNT);
        }
        if (account == null) {
            throw new ParameterException(UNKNOWN_ACCOUNT);
        }
        return account;
    }

    static int getTimestamp(HttpServletRequest req) throws ParameterException {
        String timestampValue = Convert.emptyToNull(req.getParameter("timestamp"));
        if (timestampValue == null) {
            return 0;
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
        String recipientValue = Convert.emptyToNull(req.getParameter("recipient"));
        if (recipientValue == null || "0".equals(recipientValue)) {
            throw new ParameterException(MISSING_RECIPIENT);
        }
        Long recipientId;
        try {
            recipientId = Convert.parseAccountId(recipientValue);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_RECIPIENT);
        }
        if (recipientId == null) {
            throw new ParameterException(INCORRECT_RECIPIENT);
        }
        return recipientId;
    }

    static Long getSellerId(HttpServletRequest req) throws ParameterException {
        String sellerIdValue = Convert.emptyToNull(req.getParameter("seller"));
        try {
            return Convert.parseUnsignedLong(sellerIdValue);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_RECIPIENT);
        }
    }

    static Long getBuyerId(HttpServletRequest req) throws ParameterException {
        String buyerIdValue = Convert.emptyToNull(req.getParameter("buyer"));
        try {
            return Convert.parseUnsignedLong(buyerIdValue);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_RECIPIENT);
        }
    }

    private ParameterParser() {} // never

}
