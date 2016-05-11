/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt;

import nxt.db.DerivedDbTable;
import nxt.util.Listener;
import nxt.util.Logger;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

final class DistributionListener implements Listener<Block> {

    private static final int DISTRIBUTION_START = 640000;
    private static final int DISTRIBUTION_END = DISTRIBUTION_START + 90 * 1440; // run for 90 days
    private static final int DISTRIBUTION_FREQUENCY = 720; // run processing every 720 blocks
    private static final int DISTRIBUTION_STEP = 60; // take snapshots every 60 blocks
    private static final long FXT_ASSET_ID = Long.parseUnsignedLong("111111111111111111");
    private static final long FXT_ISSUER_ID = Long.parseUnsignedLong("22222222222222222");
    private static final BigInteger BALANCE_DIVIDER = BigInteger.valueOf(10000L * (DISTRIBUTION_END - DISTRIBUTION_START) / DISTRIBUTION_STEP);

    private static final DerivedDbTable accountFXTTable = new DerivedDbTable("account_fxt") {
        @Override
        public void trim(int height) {
            try (Connection con = db.getConnection()) {
                if (height > DISTRIBUTION_END) {
                    try (Statement stmt = con.createStatement()) {
                        stmt.executeUpdate("TRUNCATE TABLE account_fxt");
                    }
                } else {
                    try (PreparedStatement pstmt = con.prepareStatement("DELETE FROM account_fxt WHERE (id, height) NOT IN "
                            + "(SELECT (id, MAX(height)) FROM account_fxt WHERE height < ? GROUP BY id) AND height < ? AND height >= 0")) {
                        pstmt.setInt(1, height);
                        pstmt.setInt(2, height);
                        pstmt.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    };

    static void init() {}

    static {
        Nxt.getBlockchainProcessor().addListener(new DistributionListener(), BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
    }

    @Override
    public void notify(Block block) {

        final int currentHeight = block.getHeight();
        if (currentHeight <= DISTRIBUTION_START || currentHeight > DISTRIBUTION_END || (currentHeight - DISTRIBUTION_START) % DISTRIBUTION_FREQUENCY != 0) {
            return;
        }
        Logger.logDebugMessage("Running FXT balance update at height " + currentHeight);
        Map<Long, BigInteger> accountBalanceTotals = new HashMap<>();
        for (int height = currentHeight - DISTRIBUTION_FREQUENCY + DISTRIBUTION_STEP; height <= currentHeight; height += DISTRIBUTION_STEP) {
            Logger.logDebugMessage("Calculating balances at height " + height);
            try (Connection con = Db.db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT id, balance FROM account WHERE (id, height) IN "
                         + "(SELECT (id, MAX(height)) FROM account WHERE height <= ? GROUP BY id) AND balance > 0")) {
                pstmt.setInt(1, height);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Long accountId = rs.getLong("id");
                        long balance = rs.getLong("balance");
                        BigInteger accountBalanceTotal = accountBalanceTotals.get(accountId);
                        accountBalanceTotals.put(accountId, accountBalanceTotal == null ?
                                BigInteger.valueOf(balance) : accountBalanceTotal.add(BigInteger.valueOf(balance)));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
        Logger.logDebugMessage("Updating balances for " + accountBalanceTotals.size() + " accounts");
        boolean wasInTransaction = Db.db.isInTransaction();
        if (!wasInTransaction) {
            Db.db.beginTransaction();
        }
        Db.db.clearCache();
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT balance FROM account_fxt WHERE id = ? ORDER BY height DESC LIMIT 1");
             PreparedStatement pstmtInsert = con.prepareStatement("INSERT INTO account_fxt (id, balance, height) values (?, ?, ?)")) {
            for (Map.Entry<Long, BigInteger> entry : accountBalanceTotals.entrySet()) {
                long accountId = entry.getKey();
                BigInteger balanceTotal = entry.getValue();
                pstmtSelect.setLong(1, accountId);
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    if (rs.next()) {
                        balanceTotal = balanceTotal.add(new BigInteger(rs.getBytes("balance")));
                    }
                }
                pstmtInsert.setLong(1, accountId);
                pstmtInsert.setBytes(2, balanceTotal.toByteArray());
                pstmtInsert.setInt(3, currentHeight);
                pstmtInsert.executeUpdate();
            }
            Db.db.commitTransaction();
            if (currentHeight == DISTRIBUTION_END) {
                Logger.logDebugMessage("Running FXT distribution at height " + currentHeight);
                long totalDistributed = 0;
                int count = 0;
                try (Statement stmt = con.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id, balance FROM account_fxt WHERE (id, height) IN "
                             + "(SELECT (id, MAX(height)) FROM account_fxt WHERE height <= " + currentHeight + " GROUP BY id)")) {
                    while (rs.next()) {
                        long accountId = rs.getLong("id");
                        // 1 NXT held for the full period should give 1 asset unit, i.e. 10000 QNT assuming 4 decimals
                        long quantity = new BigInteger(rs.getBytes("balance")).divide(BALANCE_DIVIDER).longValueExact();
                        Account.getAccount(accountId).addToAssetAndUnconfirmedAssetBalanceQNT(AccountLedger.LedgerEvent.FXT_DISTRIBUTION, block.getId(),
                                FXT_ASSET_ID, quantity);
                        totalDistributed += quantity;
                        count += 1;
                    }
                /*
                Account.getAccount(FXT_ISSUER_ID).addToAssetAndUnconfirmedAssetBalanceQNT(AccountLedger.LedgerEvent.FXT_DISTRIBUTION, block.getId(),
                    FXT_ASSET_ID, -totalDistributed);
                */
                }
                Logger.logDebugMessage("Distributed " + totalDistributed + " QNT to " + count + " accounts");
                Db.db.commitTransaction();
            }
        } catch (Exception e) {
            Db.db.rollbackTransaction();
            throw new RuntimeException(e.toString(), e);
        } finally {
            if (!wasInTransaction) {
                Db.db.endTransaction();
            }
        }
    }
}
