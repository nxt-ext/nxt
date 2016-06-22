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
import nxt.util.Convert;
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

public final class FxtDistribution implements Listener<Block> {

    public static final int DISTRIBUTION_END = Constants.FXT_BLOCK;
    public static final int DISTRIBUTION_START = DISTRIBUTION_END - 90 * 1440; // run for 90 days
    public static final int DISTRIBUTION_FREQUENCY = 720; // run processing every 720 blocks
    public static final int DISTRIBUTION_STEP = 60; // take snapshots every 60 blocks
    public static final long FXT_ASSET_ID = Long.parseUnsignedLong(Constants.isTestnet ? "861080501219231688" : "12422608354438203866");
    public static final long FXT_ISSUER_ID = Convert.parseAccountId(Constants.isTestnet ? "NXT-F8FG-RDWZ-GRW7-4GSK9" : "NXT-FQ28-G9SQ-BG8M-6V6QH");
    private static final BigInteger BALANCE_DIVIDER = BigInteger.valueOf(10000L * (DISTRIBUTION_END - DISTRIBUTION_START) / DISTRIBUTION_STEP);
    private static final String logAccount = Nxt.getStringProperty("nxt.logFxtBalance");
    private static final long logAccountId = Convert.parseAccountId(logAccount);


    private static final DerivedDbTable accountFXTTable = new DerivedDbTable("account_fxt") {
        @Override
        public void trim(int height) {
            try (Connection con = db.getConnection()) {
                if (height > DISTRIBUTION_END) {
                    try (Statement stmt = con.createStatement()) {
                        stmt.executeUpdate("TRUNCATE TABLE account_fxt");
                    }
                } else {
                    try (PreparedStatement pstmtCreate = con.prepareStatement("CREATE TEMP TABLE account_fxt_tmp NOT PERSISTENT AS "
                            + "SELECT id, MAX(height) AS height FROM account_fxt WHERE height < ? GROUP BY id");
                         PreparedStatement pstmtDrop = con.prepareStatement("DROP TABLE account_fxt_tmp")) {
                        pstmtCreate.setInt(1, height);
                        pstmtCreate.executeUpdate();
                        try (PreparedStatement pstmt = con.prepareStatement("DELETE FROM account_fxt WHERE (id, height) NOT IN "
                                + "(SELECT (id, height) FROM account_fxt_tmp) AND height < ? AND height >= 0")) {
                            pstmt.setInt(1, height);
                            pstmt.executeUpdate();
                        } finally {
                            pstmtDrop.executeUpdate();
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    };

    static void init() {}

    static {
        Nxt.getBlockchainProcessor().addListener(new FxtDistribution(), BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
    }

    public static long getConfirmedFxtQuantity(long accountId) {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT balance FROM account_fxt WHERE id = ? ORDER BY height DESC LIMIT 1")) {
            pstmtSelect.setLong(1, accountId);
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                if (!rs.next()) {
                    return 0;
                }
                return new BigInteger(rs.getBytes("balance")).divide(BALANCE_DIVIDER).longValueExact();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static long getRemainingFxtQuantity(long accountId) {
        long balance = Account.getAccount(accountId).getBalanceNQT();
        int height = Nxt.getBlockchain().getHeight();
        if (height >= DISTRIBUTION_END) {
            return 0;
        }
        if (height < DISTRIBUTION_START) {
            return balance / 10000L;
        }
        int done = (height - DISTRIBUTION_START) / DISTRIBUTION_FREQUENCY;
        int total = (DISTRIBUTION_END - DISTRIBUTION_START) / DISTRIBUTION_FREQUENCY;
        return (balance * (total - done)) / (total * 10000L);
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
                 PreparedStatement pstmtCreate = con.prepareStatement("CREATE TEMP TABLE account_tmp NOT PERSISTENT AS SELECT id, MAX(height) as height FROM account "
                         + "WHERE height <= ? GROUP BY id")) {
                pstmtCreate.setInt(1, height);
                pstmtCreate.executeUpdate();
                try (PreparedStatement pstmtSelect = con.prepareStatement("SELECT account.id, account.balance FROM account, account_tmp WHERE account.id = account_tmp.id "
                        + "AND account.height = account_tmp.height AND account.balance > 0");
                     PreparedStatement pstmtDrop = con.prepareStatement("DROP TABLE account_tmp")) {
                    try (ResultSet rs = pstmtSelect.executeQuery()) {
                        while (rs.next()) {
                            Long accountId = rs.getLong("id");
                            long balance = rs.getLong("balance");
                            if (logAccountId != 0) {
                                if (accountId == logAccountId) {
                                    Logger.logMessage("NXT balance for " + logAccount + " at height " + height + ":\t" + balance);
                                }
                            }
                            BigInteger accountBalanceTotal = accountBalanceTotals.get(accountId);
                            accountBalanceTotals.put(accountId, accountBalanceTotal == null ?
                                    BigInteger.valueOf(balance) : accountBalanceTotal.add(BigInteger.valueOf(balance)));
                        }
                    } finally {
                        pstmtDrop.executeUpdate();
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
            int count = 0;
            for (Map.Entry<Long, BigInteger> entry : accountBalanceTotals.entrySet()) {
                long accountId = entry.getKey();
                BigInteger balanceTotal = entry.getValue();
                pstmtSelect.setLong(1, accountId);
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    if (rs.next()) {
                        balanceTotal = balanceTotal.add(new BigInteger(rs.getBytes("balance")));
                    }
                }
                if (logAccountId != 0) {
                    if (accountId == logAccountId) {
                        Logger.logMessage("Average NXT balance for " + logAccount + " as of height " + currentHeight + ":\t"
                                + balanceTotal.divide(BigInteger.valueOf((currentHeight - DISTRIBUTION_START) / DISTRIBUTION_STEP)).longValueExact());
                    }
                }
                pstmtInsert.setLong(1, accountId);
                pstmtInsert.setBytes(2, balanceTotal.toByteArray());
                pstmtInsert.setInt(3, currentHeight);
                pstmtInsert.executeUpdate();
                if (++count % 1000 == 0) {
                    Db.db.commitTransaction();
                }
            }
            accountBalanceTotals.clear();
            Db.db.commitTransaction();
            if (currentHeight == DISTRIBUTION_END) {
                Logger.logDebugMessage("Running FXT distribution at height " + currentHeight);
                long totalDistributed = 0;
                count = 0;
                try (PreparedStatement pstmtCreate = con.prepareStatement("CREATE TEMP TABLE account_fxt_tmp NOT PERSISTENT AS SELECT id, MAX(height) AS height FROM account_fxt "
                        + "WHERE height <= ? GROUP BY id");
                     PreparedStatement pstmtDrop = con.prepareStatement("DROP TABLE account_fxt_tmp")) {
                    pstmtCreate.setInt(1, currentHeight);
                    pstmtCreate.executeUpdate();
                    try (PreparedStatement pstmt = con.prepareStatement("SELECT account_fxt.id, account_fxt.balance FROM account_fxt, account_fxt_tmp "
                            + "WHERE account_fxt.id = account_fxt_tmp.id AND account_fxt.height = account_fxt_tmp.height");
                         ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            long accountId = rs.getLong("id");
                            // 1 NXT held for the full period should give 1 asset unit, i.e. 10000 QNT assuming 4 decimals
                            long quantity = new BigInteger(rs.getBytes("balance")).divide(BALANCE_DIVIDER).longValueExact();
                            if (logAccountId != 0) {
                                if (accountId == logAccountId) {
                                    Logger.logMessage("FXT quantity for " + logAccount + ":\t" + quantity);
                                }
                            }
                            Account.getAccount(accountId).addToAssetAndUnconfirmedAssetBalanceQNT(null, block.getId(),
                                    FXT_ASSET_ID, quantity);
                            totalDistributed += quantity;
                            if (++count % 1000 == 0) {
                                Db.db.commitTransaction();
                            }
                        }
                    } finally {
                        pstmtDrop.executeUpdate();
                    }
                }
                Account issuerAccount = Account.getAccount(FXT_ISSUER_ID);
                issuerAccount.addToAssetAndUnconfirmedAssetBalanceQNT(null, block.getId(),
                        FXT_ASSET_ID, -totalDistributed);
                long excessFxtQuantity = Asset.getAsset(FXT_ASSET_ID).getInitialQuantityQNT() - totalDistributed;
                issuerAccount.addToAssetAndUnconfirmedAssetBalanceQNT(null, block.getId(),
                        FXT_ASSET_ID, -excessFxtQuantity);
                Asset.deleteAsset(TransactionDb.findTransaction(FXT_ASSET_ID), FXT_ASSET_ID, excessFxtQuantity);
                Logger.logDebugMessage("Deleted " + excessFxtQuantity + " excess QNT");

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
