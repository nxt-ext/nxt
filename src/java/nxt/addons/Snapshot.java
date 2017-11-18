/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt.addons;

import nxt.Account;
import nxt.Asset;
import nxt.AssetTransfer;
import nxt.Block;
import nxt.BlockchainProcessor;
import nxt.Constants;
import nxt.Currency;
import nxt.Db;
import nxt.FxtDistribution;
import nxt.Genesis;
import nxt.Nxt;
import nxt.Transaction;
import nxt.TransactionType;
import nxt.crypto.Crypto;
import nxt.db.DbIterator;
import nxt.db.DbUtils;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Logger;
import org.json.simple.JSONArray;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class Snapshot implements AddOn {

    private static final int snapshotHeight = Nxt.getIntProperty("nxt.snapshotHeight", Integer.MAX_VALUE);
    private static final boolean snapshotForTestnet = Nxt.getBooleanProperty("nxt.snapshotForTestnet", true);

    @Override
    public void init() {

        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {

            List<byte[]> developerPublicKeys = new ArrayList<>();

            {
                if (snapshotForTestnet) {
                    InputStream is = ClassLoader.getSystemResourceAsStream("developerPasswords.txt");
                    if (is != null) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                developerPublicKeys.add(Crypto.getPublicKey(line));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                        developerPublicKeys.sort(Convert.byteArrayComparator);
                    } else {
                        Logger.logDebugMessage("No developerPasswords.txt file found");
                    }
                }
            }

            @Override
            public void notify(Block block) {
                if (block.getHeight() == snapshotHeight) {
                    exportPublicKeys();
                    Map ignisBalances = exportIgnisBalances();
                    exportBitswiftBalances(ignisBalances);
                    exportArdorBalances();
                    exportAssetBalances();
                    exportAliases();
                    exportCurrencies();
                    exportAccountInfo();
                    exportAccountProperties();
                    exportAccountControl();
                }
            }

            private void exportPublicKeys() {
                JSONArray publicKeys = new JSONArray();
                try (Connection con = Db.db.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("SELECT public_key FROM public_key WHERE public_key IS NOT NULL AND LATEST=true ORDER by account_id")) {
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            byte[] publicKey = rs.getBytes("public_key");
                            if (Collections.binarySearch(developerPublicKeys, publicKey, Convert.byteArrayComparator) >= 0) {
                                throw new RuntimeException("Developer account " + Account.getId(publicKey) + " already exists");
                            }
                            publicKeys.add(Convert.toHexString(rs.getBytes("public_key")));
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                developerPublicKeys.forEach(publicKey -> publicKeys.add(Convert.toHexString(publicKey)));
                Logger.logInfoMessage("Will save " + publicKeys.size() + " public keys");
                try (PrintWriter writer = new PrintWriter((new BufferedWriter( new OutputStreamWriter(new FileOutputStream(
                        snapshotForTestnet ? "PUBLIC_KEY-testnet.json" : "PUBLIC_KEY.json")))), true)) {
                    JSON.writeJSONString(publicKeys, writer);
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                Logger.logInfoMessage("Done");
            }

            private Map<String, Long> exportIgnisBalances() {
                SortedMap<String, Long> snapshotMap = new TreeMap<>();
                SortedMap<String, Long> btcSnapshotMap = new TreeMap<>();
                SortedMap<String, Long> usdSnapshotMap = new TreeMap<>();
                SortedMap<String, Long> eurSnapshotMap = new TreeMap<>();
                try (Connection con = Db.db.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("SELECT id, balance FROM account WHERE LATEST=true")) {
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            long accountId = rs.getLong("id");
                            if (accountId == FxtDistribution.FXT_ISSUER_ID) {
                                continue;
                            }
                            long balance = rs.getLong("balance");
                            if (balance <= 0) {
                                continue;
                            }
                            // on mainnet reduce balance by 2 because of JLRDA ICO
                            if (!Constants.isTestnet) {
                                balance = balance / 2;
                            }
                            // if for testnet, reduce by 2 again to reserve for developer accounts
                            if (snapshotForTestnet && !developerPublicKeys.isEmpty()) {
                                balance = balance / 2;
                            }
                            String account = Long.toUnsignedString(accountId);
                            snapshotMap.put(account, balance);
                            if (snapshotForTestnet) {
                                btcSnapshotMap.put(account, (balance / Constants.ONE_NXT) * 600);
                                usdSnapshotMap.put(account, ((balance * 300 / 5) / Constants.ONE_NXT));
                                eurSnapshotMap.put(account, ((balance * 300 / 5) / Constants.ONE_NXT));
                            }
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                try (DbIterator<? extends Transaction> transactions = Nxt.getBlockchain().getTransactions(
                        FxtDistribution.FXT_ISSUER_ID, 0, TransactionType.Payment.ORDINARY.getType(),
                        TransactionType.Payment.ORDINARY.getSubtype(), Nxt.getBlockchain().getBlockAtHeight(FxtDistribution.DISTRIBUTION_START).getTimestamp(),
                        false, false, false, 0, -1, false, true)) {
                    while (transactions.hasNext()) {
                        Transaction transaction = transactions.next();
                        if (transaction.getRecipientId() != FxtDistribution.FXT_ISSUER_ID) {
                            continue;
                        }
                        String senderId = Long.toUnsignedString(transaction.getSenderId());
                        long amount = transaction.getAmountNQT() / 2;
                        Logger.logDebugMessage("Will refund " + amount + " IGNIS to " + Convert.rsAccount(transaction.getSenderId()));
                        long balance = Convert.nullToZero(snapshotMap.get(senderId));
                        balance += amount;
                        snapshotMap.put(senderId, balance);
                    }
                }
                if (!Constants.isTestnet) {
                    try (Connection con = Db.db.getConnection();
                    PreparedStatement pstmt = con.prepareStatement("SELECT account_id, units FROM account_currency WHERE currency_id = ? AND LATEST=true")) {
                        pstmt.setLong(1, Currency.getCurrencyByCode("JLRDA").getId());
                        try (ResultSet rs = pstmt.executeQuery()) {
                            while (rs.next()) {
                                String accountId = Long.toUnsignedString(rs.getLong("account_id"));
                                long units = rs.getLong("units");
                                if (units <= 0) {
                                    continue;
                                }
                                if (snapshotForTestnet && !developerPublicKeys.isEmpty()) {
                                    units = units / 2;
                                }
                                long balance = Convert.nullToZero(snapshotMap.get(accountId));
                                balance += units * 10000;
                                snapshotMap.put(accountId, balance);
                            }
                        }

                    } catch (SQLException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
                if (snapshotForTestnet && !developerPublicKeys.isEmpty()) {
                    final long developerBalance = Constants.MAX_BALANCE_NQT / (2 * developerPublicKeys.size());
                    developerPublicKeys.forEach(publicKey -> {
                        String account = Long.toUnsignedString(Account.getId(publicKey));
                        snapshotMap.put(account, developerBalance);
                        btcSnapshotMap.put(account, (developerBalance / Constants.ONE_NXT) * 600);
                        usdSnapshotMap.put(account, ((developerBalance * 300 / 5) / Constants.ONE_NXT));
                        eurSnapshotMap.put(account, ((developerBalance * 300 / 5) / Constants.ONE_NXT));
                    });
                }
                saveMap(snapshotMap, snapshotForTestnet ? "IGNIS-testnet.json" : "IGNIS.json");
                if (snapshotForTestnet) {
                    saveMap(btcSnapshotMap, "BTC-testnet.json");
                    saveMap(usdSnapshotMap, "USD-testnet.json");
                    saveMap(eurSnapshotMap, "EUR-testnet.json");
                }
                return Collections.unmodifiableMap(snapshotMap);
            }

            private void exportArdorBalances() {
                SortedMap<String, Long> snapshotMap = new TreeMap<>();
                try (Connection con = Db.db.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("SELECT account_id, quantity FROM account_asset WHERE asset_id = ? AND LATEST=true")) {
                    pstmt.setLong(1, FxtDistribution.FXT_ASSET_ID);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            long accountId = rs.getLong("account_id");
                            long balance = rs.getLong("quantity");
                            if (balance <= 0) {
                                continue;
                            }
                            if (accountId == FxtDistribution.FXT_ISSUER_ID) {
                                continue;
                            }
                            if (snapshotForTestnet && !developerPublicKeys.isEmpty()) {
                                balance = balance / 2;
                            }
                            snapshotMap.put(Long.toUnsignedString(accountId), balance * 10000);
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                try (DbIterator<AssetTransfer> transfers = AssetTransfer.getAccountAssetTransfers(FxtDistribution.FXT_ISSUER_ID, FxtDistribution.FXT_ASSET_ID, 0, -1)) {
                    while (transfers.hasNext()) {
                        AssetTransfer transfer = transfers.next();
                        if (transfer.getRecipientId() != FxtDistribution.FXT_ISSUER_ID) {
                            continue;
                        }
                        String senderId = Long.toUnsignedString(transfer.getSenderId());
                        long quantityQNT = transfer.getQuantityQNT();
                        Logger.logDebugMessage("Will refund " + quantityQNT + " ARDR to " + Convert.rsAccount(transfer.getSenderId()));
                        long balance = Convert.nullToZero(snapshotMap.get(senderId));
                        balance += quantityQNT * 10000;
                        snapshotMap.put(senderId, balance);
                    }
                }
                if (snapshotForTestnet && !developerPublicKeys.isEmpty()) {
                    final long developerBalance = Constants.MAX_BALANCE_NQT / (2 * developerPublicKeys.size());
                    developerPublicKeys.forEach(publicKey -> {
                        String account = Long.toUnsignedString(Account.getId(publicKey));
                        snapshotMap.put(account, developerBalance);
                    });
                }
                saveMap(snapshotMap, snapshotForTestnet ? "ARDR-testnet.json" : "ARDR.json");
            }

            private void exportBitswiftBalances(Map<String,Long> ignisBalances) {
                Asset bitswiftAsset = Asset.getAsset(FxtDistribution.BITSWIFT_ASSET_ID);
                if (bitswiftAsset == null) {
                    return;
                }
                BigInteger totalQuantity = BigInteger.valueOf(bitswiftAsset.getQuantityQNT());
                BigInteger totalIgnisBalance = BigInteger.valueOf(ignisBalances.values().stream().mapToLong(Long::longValue).sum());
                SortedMap<String, Long> snapshotMap = new TreeMap<>();
                try (Connection con = Db.db.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("SELECT account_id, quantity FROM account_asset WHERE asset_id = ? AND LATEST=true")) {
                    pstmt.setLong(1, FxtDistribution.BITSWIFT_ASSET_ID);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            long quantity = rs.getLong("quantity");
                            if (quantity <= 0) {
                                continue;
                            }
                            String accountId = Long.toUnsignedString(rs.getLong("account_id"));
                            snapshotMap.put(accountId, quantity);
                        }
                    }
                    for (Map.Entry<String, Long> entry : ignisBalances.entrySet()) {
                        String accountId = entry.getKey();
                        long ignisBalance = Convert.nullToZero(entry.getValue());
                        long sharedropQuantity = totalQuantity.multiply(BigInteger.valueOf(ignisBalance)).divide(totalIgnisBalance).divide(BigInteger.TEN).longValueExact();
                        Long quantity = snapshotMap.get(accountId);
                        snapshotMap.put(accountId, quantity == null ? sharedropQuantity : quantity + sharedropQuantity);
                    }
                    String bitswiftSharedropAccount = Long.toUnsignedString(FxtDistribution.BITSWIFT_SHAREDROP_ACCOUNT);
                    long bitswiftIssuerBalance = snapshotMap.get(bitswiftSharedropAccount);
                    bitswiftIssuerBalance -= totalQuantity.divide(BigInteger.TEN).longValueExact();
                    if (bitswiftIssuerBalance < 0) {
                        throw new RuntimeException("Not enough Bitswift available for sharedrop");
                    }
                    snapshotMap.put(bitswiftSharedropAccount, bitswiftIssuerBalance);
                } catch (SQLException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                saveMap(snapshotMap, snapshotForTestnet ? "BITSWIFT-testnet.json" : "BITSWIFT.json");
            }

            private void exportAssetBalances() {
                SortedMap<String, Map<String, Object>> snapshotMap = new TreeMap<>();
                try (Connection con = Db.db.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("SELECT account_id, quantity FROM account_asset WHERE asset_id = ? AND LATEST=true")) {
                    for (long assetId : new long[] {FxtDistribution.JANUS_ASSET_ID, FxtDistribution.JANUSXT_ASSET_ID, FxtDistribution.COMJNSXT_ASSET_ID}) { //TODO: others?
                        Asset asset = Asset.getAsset(assetId);
                        String name = asset.getName();
                        String description = asset.getDescription();
                        String assetIssuer = Long.toUnsignedString(asset.getAccountId());
                        byte decimals = asset.getDecimals();
                        SortedMap<String, Long> assetBalances = new TreeMap<>();
                        pstmt.setLong(1, assetId);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            while (rs.next()) {
                                long accountId = rs.getLong("account_id");
                                long balance = rs.getLong("quantity");
                                if (balance <= 0) {
                                    continue;
                                }
                                assetBalances.put(Long.toUnsignedString(accountId), balance);
                            }
                        }
                        SortedMap<String, Object> assetMap = new TreeMap<>();
                        assetMap.put("name", name);
                        assetMap.put("description", description);
                        assetMap.put("issuer", assetIssuer);
                        assetMap.put("decimals", decimals);
                        assetMap.put("balances", assetBalances);
                        snapshotMap.put(Long.toUnsignedString(assetId), assetMap);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                saveMap(snapshotMap, snapshotForTestnet ? "ASSETS-testnet.json" : "ASSETS.json");
            }

            private void exportAliases() {
                SortedMap<String, Map<String, String>> snapshotMap = new TreeMap<>();
                try (Connection con = Db.db.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("SELECT account_id, alias_name, alias_uri FROM alias WHERE LATEST=true")) {
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            String aliasName = rs.getString("alias_name");
                            String aliasURI = Convert.nullToEmpty(rs.getString("alias_uri"));
                            long accountId = rs.getLong("account_id");
                            Map alias = new TreeMap();
                            alias.put("account", Long.toUnsignedString(accountId));
                            alias.put("uri", aliasURI);
                            snapshotMap.put(aliasName, alias);
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                saveMap(snapshotMap, snapshotForTestnet ? "IGNIS_ALIASES-testnet.json" : "IGNIS_ALIASES.json");
            }

            private void exportCurrencies() {
                SortedMap<String, Map<String, String>> snapshotMap = new TreeMap<>();
                try (Connection con = Db.db.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("SELECT account_id, name, code FROM currency WHERE LATEST=true")) {
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            String currencyName = rs.getString("name");
                            String currencyCode = rs.getString("code");
                            if (invalidCurrency(currencyCode, currencyName.toLowerCase())) {
                                Logger.logDebugMessage("Skipping currency " + currencyCode + " " + currencyName);
                                continue;
                            }
                            long accountId = rs.getLong("account_id");
                            Map currency = new TreeMap();
                            currency.put("account", Long.toUnsignedString(accountId));
                            currency.put("name", currencyName);
                            snapshotMap.put(currencyCode, currency);
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                saveMap(snapshotMap, snapshotForTestnet ? "IGNIS_CURRENCIES-testnet.json" : "IGNIS_CURRENCIES.json");
            }

            private boolean invalidCurrency(String code, String normalizedName) {
                if (code.equals("ARDOR") || code.contains("ARDR") || "ardor".equals(normalizedName) || "ardr".equals(normalizedName)) {
                    return true;
                }
                if (code.contains("NXT") || code.contains("NEXT") || "nxt".equals(normalizedName) || "next".equals(normalizedName)) {
                    return true;
                }
                if (code.equals("IGNIS") || "ignis".equals(normalizedName)) {
                    return true;
                }
                if ("bitswift".equals(normalizedName)) {
                    return true;
                }
                //TODO: add production child chain names, when known
                return false;
            }

            private void exportAccountInfo() {
                SortedMap<String, Map<String, String>> snapshotMap = new TreeMap<>();
                try (Connection con = Db.db.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("SELECT account_id, name, description FROM account_info WHERE LATEST=true")) {
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            String accountName = Convert.nullToEmpty(rs.getString("name"));
                            String accountDescription = Convert.nullToEmpty(rs.getString("description"));
                            long accountId = rs.getLong("account_id");
                            Map account = new TreeMap();
                            account.put("name", accountName);
                            account.put("description", accountDescription);
                            snapshotMap.put(Long.toUnsignedString(accountId), account);
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                saveMap(snapshotMap, snapshotForTestnet ? "ACCOUNT_INFO-testnet.json" : "ACCOUNT_INFO.json");
            }

            private void exportAccountProperties() {
                SortedMap<String, Map<String, Map<String, String>>> snapshotMap = new TreeMap<>();
                try (Connection con = Db.db.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("SELECT recipient_id, setter_id, property, value FROM account_property WHERE LATEST=true")) {
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            String property = rs.getString("property");
                            String value = Convert.nullToEmpty(rs.getString("value"));
                            String recipientId = Long.toUnsignedString(rs.getLong("recipient_id"));
                            String setterId = Long.toUnsignedString(rs.getLong("setter_id"));
                            Map<String, Map<String, String>> account = snapshotMap.computeIfAbsent(recipientId, k -> new TreeMap());
                            Map<String, String> properties = account.computeIfAbsent(setterId, k -> new TreeMap<>());
                            properties.put(property, value);
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                saveMap(snapshotMap, snapshotForTestnet ? "ACCOUNT_PROPERTIES-testnet.json" : "ACCOUNT_PROPERTIES.json");
            }

            private void exportAccountControl() {
                SortedMap<String, Map<String, Object>> snapshotMap = new TreeMap<>();
                try (Connection con = Db.db.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("SELECT account_id, whitelist, quorum, max_fees, min_duration, max_duration "
                             + "FROM account_control_phasing WHERE voting_model = 0 AND min_balance IS NULL AND whitelist IS NOT NULL AND LATEST=true")) {
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            String accountId = Long.toUnsignedString(rs.getLong("account_id"));
                            Map<String, Object> accountControl = new TreeMap();
                            Long[] whitelist = DbUtils.getArray(rs, "whitelist", Long[].class);
                            for (int i = 0; i < whitelist.length; i++) {
                                if (whitelist[i] == Genesis.CREATOR_ID) {
                                    whitelist[i] = 0L;
                                }
                            }
                            JSONArray whitelistJSON = new JSONArray();
                            whitelistJSON.addAll(Arrays.asList(whitelist));
                            accountControl.put("whitelist", whitelistJSON);
                            accountControl.put("quorum", rs.getInt("quorum"));
                            accountControl.put("maxFees", rs.getLong("max_fees"));
                            accountControl.put("minDuration", rs.getInt("min_duration"));
                            accountControl.put("maxDuration", rs.getInt("max_duration"));
                            snapshotMap.put(accountId, accountControl);
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                saveMap(snapshotMap, snapshotForTestnet ? "ACCOUNT_CONTROL-testnet.json" : "ACCOUNT_CONTROL.json");
            }

            private void saveMap(Map<String, ? extends Object> snapshotMap, String file) {
                Logger.logInfoMessage("Will save " + snapshotMap.size() + " entries to " + file);
                try (PrintWriter writer = new PrintWriter((new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))), true)) {
                    StringBuilder sb = new StringBuilder(1024);
                    JSON.encodeObject(snapshotMap, sb);
                    writer.write(sb.toString());
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                Logger.logInfoMessage("Done");
            }

        }, BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
    }

}
