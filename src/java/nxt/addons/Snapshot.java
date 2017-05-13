/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016 Jelurida IP B.V.
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
 */

package nxt.addons;

import nxt.Account;
import nxt.AssetTransfer;
import nxt.Block;
import nxt.BlockchainProcessor;
import nxt.Constants;
import nxt.Db;
import nxt.FxtDistribution;
import nxt.Nxt;
import nxt.Transaction;
import nxt.TransactionType;
import nxt.crypto.Crypto;
import nxt.db.DbIterator;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class Snapshot implements AddOn {

    private static final int snapshotHeight = Nxt.getIntProperty("nxt.snapshotHeight");

    @Override
    public void init() {
        if (snapshotHeight == 0) {
            return;
        }

        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {

            List<byte[]> developerPublicKeys = new ArrayList<>();

            {
                if (Constants.isTestnet) {
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
                    exportIgnisBalances();
                    exportArdorBalances();
                    exportAliases();
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
                        Constants.isTestnet ? "PUBLIC_KEY-testnet.json" : "PUBLIC_KEY.json")))), true)) {
                    JSON.writeJSONString(publicKeys, writer);
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                Logger.logInfoMessage("Done");
            }

            private void exportIgnisBalances() {
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
                            if (Constants.isTestnet && !developerPublicKeys.isEmpty()) {
                                balance = balance / 2;
                            }
                            String account = Long.toUnsignedString(accountId);
                            snapshotMap.put(account, balance);
                            btcSnapshotMap.put(account, (balance / Constants.ONE_NXT) * 600);
                            usdSnapshotMap.put(account, ((balance * 300 / 5) / Constants.ONE_NXT));
                            eurSnapshotMap.put(account, ((balance * 300 / 5) / Constants.ONE_NXT));
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
                if (Constants.isTestnet && !developerPublicKeys.isEmpty()) {
                    final long developerBalance = Constants.MAX_BALANCE_NQT / (2 * developerPublicKeys.size());
                    developerPublicKeys.forEach(publicKey -> {
                        String account = Long.toUnsignedString(Account.getId(publicKey));
                        snapshotMap.put(account, developerBalance);
                        btcSnapshotMap.put(account, (developerBalance / Constants.ONE_NXT) * 600);
                        usdSnapshotMap.put(account, ((developerBalance * 300 / 5) / Constants.ONE_NXT));
                        eurSnapshotMap.put(account, ((developerBalance * 300 / 5) / Constants.ONE_NXT));
                    });
                }
                saveMap(snapshotMap, Constants.isTestnet ? "IGNIS-testnet.json" : "IGNIS.json");
                saveMap(btcSnapshotMap, Constants.isTestnet ? "BTC-testnet.json" : "BTC.json");
                saveMap(usdSnapshotMap, Constants.isTestnet ? "USD-testnet.json" : "USD.json");
                saveMap(eurSnapshotMap, Constants.isTestnet ? "EUR-testnet.json" : "EUR.json");
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
                            if (Constants.isTestnet && !developerPublicKeys.isEmpty()) {
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
                if (Constants.isTestnet && !developerPublicKeys.isEmpty()) {
                    final long developerBalance = Constants.MAX_BALANCE_NQT / (2 * developerPublicKeys.size());
                    developerPublicKeys.forEach(publicKey -> {
                        String account = Long.toUnsignedString(Account.getId(publicKey));
                        snapshotMap.put(account, developerBalance);
                    });
                }
                saveMap(snapshotMap, Constants.isTestnet ? "ARDR-testnet.json" : "ARDR.json");
            }

            private void exportAliases() {
                SortedMap<String, Map<String, String>> snapshotMap = new TreeMap<>();
                try (Connection con = Db.db.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("SELECT account_id, alias_name, alias_uri FROM alias WHERE LATEST=true")) {
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            String aliasName = rs.getString("alias_name");
                            String aliasURI = rs.getString("alias_uri");
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
                saveMap(snapshotMap, Constants.isTestnet ? "IGNIS_ALIASES-testnet.json" : "IGNIS_ALIASES.json");
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
