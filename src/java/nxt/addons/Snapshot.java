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

import nxt.Block;
import nxt.BlockchainProcessor;
import nxt.Constants;
import nxt.Db;
import nxt.FxtDistribution;
import nxt.Nxt;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Logger;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
            @Override
            public void notify(Block block) {
                if (block.getHeight() == snapshotHeight) {
                    {
                        SortedMap<String, Long> snapshotMap = new TreeMap<>();
                        SortedMap<String, Long> btcSnapshotMap = new TreeMap<>();
                        SortedMap<String, Long> usdSnapshotMap = new TreeMap<>();
                        try (Connection con = Db.db.getConnection();
                             PreparedStatement pstmt = con.prepareStatement("SELECT id, balance FROM account where LATEST=true")) {
                            try (ResultSet rs = pstmt.executeQuery()) {
                                while (rs.next()) {
                                    long accountId = rs.getLong("id");
                                    long balance = rs.getLong("balance");
                                    String account = Long.toUnsignedString(accountId);
                                    snapshotMap.put(account, balance);
                                    btcSnapshotMap.put(account, (balance / Constants.ONE_NXT) * 700);
                                    usdSnapshotMap.put(account, ((balance / 2) / Constants.ONE_NXT));
                                }
                            }
                        } catch (SQLException e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                        saveBalances(snapshotMap, "IGNIS.json");
                        saveBalances(btcSnapshotMap, "BTC.json");
                        saveBalances(usdSnapshotMap, "USD.json");
                    }
                    {
                        SortedMap<String, Long> snapshotMap = new TreeMap<>();
                        try (Connection con = Db.db.getConnection();
                             PreparedStatement pstmt = con.prepareStatement("SELECT account_id, quantity FROM account_asset where asset_id = ? AND LATEST=true")) {
                            pstmt.setLong(1, FxtDistribution.FXT_ASSET_ID);
                            try (ResultSet rs = pstmt.executeQuery()) {
                                while (rs.next()) {
                                    long accountId = rs.getLong("account_id");
                                    long balance = rs.getLong("quantity");
                                    snapshotMap.put(Long.toUnsignedString(accountId), balance);
                                }
                            }
                        } catch (SQLException e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                        saveBalances(snapshotMap, "ARDOR.json");
                    }
                }
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
    }

    private void saveBalances(Map<String,Long> snapshotMap, String file) {
        Logger.logInfoMessage("Will save " + snapshotMap.size() + " balances to " + file);
        try (PrintWriter writer = new PrintWriter((new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))), true)) {
            StringBuilder sb = new StringBuilder(1024);
            JSON.encodeObject(snapshotMap, sb);
            writer.write(sb.toString());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Logger.logInfoMessage("Done");
    }
}
