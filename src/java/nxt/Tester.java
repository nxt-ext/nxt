package nxt;

import nxt.crypto.Crypto;
import nxt.db.DbIterator;
import nxt.util.Convert;

import java.util.HashMap;
import java.util.Map;

public class Tester {
    private final String secretPhrase;
    private final byte[] privateKey;
    private final byte[] publicKey;
    private final String publicKeyStr;
    private final Account account;
    private final long id;
    private final String strId;
    private final String rsAccount;
    private final long initialBalance;
    private final long initialUnconfirmedBalance;
    private final Map<Long, Long> initialAssetQuantity = new HashMap<>();
    private final Map<Long, Long> initialUnconfirmedAssetQuantity = new HashMap<>();
    private final Map<Long, Long> initialCurrencyUnits = new HashMap<>();
    private final Map<Long, Long> initialUnconfirmedCurrencyUnits = new HashMap<>();

    Tester(String secretPhrase) {
        this.secretPhrase = secretPhrase;
        this.privateKey = Crypto.getPrivateKey(secretPhrase);
        this.publicKey = Crypto.getPublicKey(secretPhrase);
        this.publicKeyStr = Convert.toHexString(publicKey);
        this.id = Account.getId(publicKey);
        this.strId = Convert.toUnsignedLong(id);
        this.rsAccount = Convert.rsAccount(id);
        this.account = Account.getAccount(publicKey);
        if (account != null) {
            this.initialBalance = account.getBalanceNQT();
            this.initialUnconfirmedBalance = account.getUnconfirmedBalanceNQT();
            DbIterator<Account.AccountAsset> assets = account.getAssets(0, -1);
            for (Account.AccountAsset accountAsset : assets) {
                initialAssetQuantity.put(accountAsset.getAssetId(), accountAsset.getQuantityQNT());
                initialUnconfirmedAssetQuantity.put(accountAsset.getAssetId(), accountAsset.getUnconfirmedQuantityQNT());
            }
            DbIterator<Account.AccountCurrency> currencies = account.getCurrencies(0, -1);
            for (Account.AccountCurrency accountCurrency : currencies) {
                initialCurrencyUnits.put(accountCurrency.getCurrencyId(), accountCurrency.getUnits());
                initialUnconfirmedCurrencyUnits.put(accountCurrency.getCurrencyId(), accountCurrency.getUnconfirmedUnits());
            }
        } else {
            initialBalance = 0;
            initialUnconfirmedBalance = 0;
        }
    }

    public String getSecretPhrase() {
        return secretPhrase;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public String getPublicKeyStr() {
        return publicKeyStr;
    }

    public Account getAccount() {
        return account;
    }

    public long getId() {
        return id;
    }

    public String getStrId() {
        return strId;
    }

    public String getRsAccount() {
        return rsAccount;
    }

    public long getInitialBalance() {
        return initialBalance;
    }

    public long getInitialUnconfirmedBalance() {
        return initialUnconfirmedBalance;
    }

    public long getInitialAssetQuantity(long assetId) {
        return initialAssetQuantity.get(assetId);
    }

    public long getInitialUnconfirmedAssetQuantity(long assetId) {
        return initialUnconfirmedAssetQuantity.get(assetId);
    }

    public long getInitialCurrencyUnits(long assetId) {
        return initialCurrencyUnits.get(assetId);
    }

    public long getInitialUnconfirmedCurrencyUnits(long assetId) {
        return initialUnconfirmedCurrencyUnits.get(assetId);
    }
}