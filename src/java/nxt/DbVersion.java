package nxt;

import nxt.db.Db;
import nxt.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class DbVersion {

    static void init() {
        try (Connection con = Db.beginTransaction(); Statement stmt = con.createStatement()) {
            int nextUpdate = 1;
            try {
                ResultSet rs = stmt.executeQuery("SELECT next_update FROM version");
                if (! rs.next()) {
                    throw new RuntimeException("Invalid version table");
                }
                nextUpdate = rs.getInt("next_update");
                if (! rs.isLast()) {
                    throw new RuntimeException("Invalid version table");
                }
                rs.close();
                Logger.logMessage("Database update may take a while if needed, current db version " + (nextUpdate - 1) + "...");
            } catch (SQLException e) {
                Logger.logMessage("Initializing an empty database");
                stmt.executeUpdate("CREATE TABLE version (next_update INT NOT NULL)");
                stmt.executeUpdate("INSERT INTO version VALUES (1)");
                Db.commitTransaction();
            }
            update(nextUpdate);
        } catch (SQLException e) {
            Db.rollbackTransaction();
            throw new RuntimeException(e.toString(), e);
        } finally {
            Db.endTransaction();
        }

    }

    private static void apply(String sql) {
        try (Connection con = Db.getConnection(); Statement stmt = con.createStatement()) {
            try {
                if (sql != null) {
                    Logger.logDebugMessage("Will apply sql:\n" + sql);
                    stmt.executeUpdate(sql);
                }
                stmt.executeUpdate("UPDATE version SET next_update = next_update + 1");
                Db.commitTransaction();
            } catch (SQLException e) {
                Db.rollbackTransaction();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error executing " + sql, e);
        }
    }

    private static void update(int nextUpdate) {
        switch (nextUpdate) {
            case 1:
                apply("CREATE TABLE IF NOT EXISTS block (db_id INT IDENTITY, id BIGINT NOT NULL, version INT NOT NULL, "
                        + "timestamp INT NOT NULL, previous_block_id BIGINT, "
                        + "FOREIGN KEY (previous_block_id) REFERENCES block (id) ON DELETE CASCADE, total_amount INT NOT NULL, "
                        + "total_fee INT NOT NULL, payload_length INT NOT NULL, generator_public_key BINARY(32) NOT NULL, "
                        + "previous_block_hash BINARY(32), cumulative_difficulty VARBINARY NOT NULL, base_target BIGINT NOT NULL, "
                        + "next_block_id BIGINT, FOREIGN KEY (next_block_id) REFERENCES block (id) ON DELETE SET NULL, "
                        + "index INT NOT NULL, height INT NOT NULL, generation_signature BINARY(64) NOT NULL, "
                        + "block_signature BINARY(64) NOT NULL, payload_hash BINARY(32) NOT NULL, generator_account_id BIGINT NOT NULL)");
            case 2:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_id_idx ON block (id)");
            case 3:
                apply("CREATE TABLE IF NOT EXISTS transaction (db_id INT IDENTITY, id BIGINT NOT NULL, "
                        + "deadline SMALLINT NOT NULL, sender_public_key BINARY(32) NOT NULL, recipient_id BIGINT NOT NULL, "
                        + "amount INT NOT NULL, fee INT NOT NULL, referenced_transaction_id BIGINT, index INT NOT NULL, "
                        + "height INT NOT NULL, block_id BIGINT NOT NULL, FOREIGN KEY (block_id) REFERENCES block (id) ON DELETE CASCADE, "
                        + "signature BINARY(64) NOT NULL, timestamp INT NOT NULL, type TINYINT NOT NULL, subtype TINYINT NOT NULL, "
                        + "sender_account_id BIGINT NOT NULL, attachment OTHER)");
            case 4:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS transaction_id_idx ON transaction (id)");
            case 5:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_height_idx ON block (height)");
            case 6:
                apply("CREATE INDEX IF NOT EXISTS transaction_timestamp_idx ON transaction (timestamp)");
            case 7:
                apply("CREATE INDEX IF NOT EXISTS block_generator_account_id_idx ON block (generator_account_id)");
            case 8:
                apply("CREATE INDEX IF NOT EXISTS transaction_sender_account_id_idx ON transaction (sender_account_id)");
            case 9:
                apply("CREATE INDEX IF NOT EXISTS transaction_recipient_id_idx ON transaction (recipient_id)");
            case 10:
                apply("ALTER TABLE block ALTER COLUMN generator_account_id RENAME TO generator_id");
            case 11:
                apply("ALTER TABLE transaction ALTER COLUMN sender_account_id RENAME TO sender_id");
            case 12:
                apply("ALTER INDEX block_generator_account_id_idx RENAME TO block_generator_id_idx");
            case 13:
                apply("ALTER INDEX transaction_sender_account_id_idx RENAME TO transaction_sender_id_idx");
            case 14:
                apply("ALTER TABLE block DROP COLUMN IF EXISTS index");
            case 15:
                apply("ALTER TABLE transaction DROP COLUMN IF EXISTS index");
            case 16:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS block_timestamp INT");
            case 17:
                apply(null);
            case 18:
                apply("ALTER TABLE transaction ALTER COLUMN block_timestamp SET NOT NULL");
            case 19:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS hash BINARY(32)");
            case 20:
                apply(null);
            case 21:
                apply(null);
            case 22:
                apply("CREATE INDEX IF NOT EXISTS transaction_hash_idx ON transaction (hash)");
            case 23:
                apply(null);
            case 24:
                apply("ALTER TABLE block ALTER COLUMN total_amount BIGINT");
            case 25:
                apply("ALTER TABLE block ALTER COLUMN total_fee BIGINT");
            case 26:
                apply("ALTER TABLE transaction ALTER COLUMN amount BIGINT");
            case 27:
                apply("ALTER TABLE transaction ALTER COLUMN fee BIGINT");
            case 28:
                apply(null);
            case 29:
                apply(null);
            case 30:
                apply(null);
            case 31:
                apply(null);
            case 32:
                apply(null);
            case 33:
                apply(null);
            case 34:
                apply(null);
            case 35:
                apply(null);
            case 36:
                apply("CREATE TABLE IF NOT EXISTS peer (address VARCHAR PRIMARY KEY)");
            case 37:
                if (!Constants.isTestnet) {
                    apply("INSERT INTO peer (address) VALUES " +
                            "('samson.vps.nxtcrypto.org'), ('210.70.137.216:80'), ('178.15.99.67'), ('114.215.130.79'), " +
                            "('85.25.198.120'), ('82.192.39.33'), ('84.242.91.139'), ('178.33.203.157'), ('bitsy08.vps.nxtcrypto.org'), " +
                            "('oldnbold.vps.nxtcrypto.org'), ('46.28.111.249'), ('vps8.nxtcrypto.org'), ('95.24.81.240'), " +
                            "('85.214.222.82'), ('188.226.179.119'), ('54.187.153.45'), ('89.176.190.43'), ('nxtpi.zapto.org'), " +
                            "('89.70.254.145'), ('wallet.nxtty.com'), ('95.85.24.151'), ('95.188.237.51'), ('nrs02.nxtsolaris.info'), " +
                            "('fsom.no-ip.org'), ('nrs01.nxtsolaris.info'), ('allbits.vps.nxtcrypto.org'), ('mycrypto.no-ip.biz'), " +
                            "('72.14.177.42'), ('bitsy03.vps.nxtcrypto.org'), ('199.195.148.27'), ('bitsy06.vps.nxtcrypto.org'), " +
                            "('188.226.139.71'), ('enricoip.no-ip.biz'), ('54.200.196.116'), ('24.161.110.115'), ('88.163.78.131'), " +
                            "('vps12.nxtcrypto.org'), ('vps10.nxtcrypto.org'), ('bitsy09.vps.nxtcrypto.org'), ('nxtnode.noip.me'), " +
                            "('49.245.183.103'), ('xyzzyx.vps.nxtcrypto.org'), ('nxt.ravensbloodrealms.com'), ('nxtio.org'), " +
                            "('67.212.71.173'), ('xeqtorcreed2.vps.nxtcrypto.org'), ('195.154.127.172'), ('vps11.nxtcrypto.org'), " +
                            "('184.57.30.220'), ('213.46.57.77'), ('162.243.159.190'), ('188.138.88.154'), ('178.150.207.53'), " +
                            "('54.76.203.25'), ('146.185.168.129'), ('107.23.118.157'), ('bitsy04.vps.nxtcrypto.org'), " +
                            "('nxt.alkeron.com'), ('23.88.229.194'), ('23.88.59.40'), ('77.179.121.91'), ('58.95.145.117'), " +
                            "('188.35.156.10'), ('166.111.77.95'), ('pakisnxt.no-ip.org'), ('81.4.107.191'), ('192.241.190.156'), " +
                            "('69.141.139.8'), ('nxs2.hanza.co.id'), ('bitsy01.vps.nxtcrypto.org'), ('209.126.73.158'), " +
                            "('nxt.phukhew.com'), ('89.250.240.63'), ('cryptkeeper.vps.nxtcrypto.org'), ('54.213.122.21'), " +
                            "('zobue.com'), ('91.69.121.229'), ('vps6.nxtcrypto.org'), ('54.187.49.62'), ('vps4.nxtcrypto.org'), " +
                            "('80.86.92.139'), ('109.254.63.44'), ('nxtportal.org'), ('89.250.243.200'), ('nxt.olxp.in'), " +
                            "('46.194.184.161'), ('178.63.69.203'), ('nxt.sx'), ('185.4.72.115'), ('178.198.145.191'), " +
                            "('panzetti.vps.nxtcrypto.org'), ('miasik.no-ip.org'), ('screenname.vps.nxtcrypto.org'), ('87.230.14.1'), " +
                            "('nacho.damnserver.com'), ('87.229.77.126'), ('bitsy05.vps.nxtcrypto.org'), ('lyynx.vps.nxtcrypto.org'), " +
                            "('209.126.73.156'), ('62.57.115.23'), ('66.30.204.105'), ('vps1.nxtcrypto.org'), " +
                            "('cubie-solar.mjke.de:7873'), ('192.99.212.121'), ('109.90.16.208')");
                } else {
                    apply("INSERT INTO peer (address) VALUES " +
                            "('178.150.207.53'), ('192.241.223.132'), ('node9.mynxtcoin.org'), ('node10.mynxtcoin.org'), " +
                            "('node3.mynxtcoin.org'), ('109.87.169.253'), ('nxtnet.fr'), ('50.112.241.97'), " +
                            "('2.84.142.149'), ('bug.airdns.org'), ('83.212.103.14'), ('62.210.131.30')");
                }
            case 38:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS full_hash BINARY(32)");
            case 39:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS referenced_transaction_full_hash BINARY(32)");
            case 40:
                apply(null);
            case 41:
                apply("ALTER TABLE transaction ALTER COLUMN full_hash SET NOT NULL");
            case 42:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS transaction_full_hash_idx ON transaction (full_hash)");
            case 43:
                apply(null);
            case 44:
                apply(null);
            case 45:
                apply(null);
            case 46:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS attachment_bytes VARBINARY");
            case 47:
                BlockDb.deleteAll();
                apply(null);
            case 48:
                apply("ALTER TABLE transaction DROP COLUMN attachment");
            case 49:
                apply("UPDATE transaction a SET a.referenced_transaction_full_hash = "
                        + "(SELECT full_hash FROM transaction b WHERE b.id = a.referenced_transaction_id) "
                        + "WHERE a.referenced_transaction_full_hash IS NULL");
            case 50:
                apply("ALTER TABLE transaction DROP COLUMN referenced_transaction_id");
            case 51:
                apply("ALTER TABLE transaction DROP COLUMN hash");
            case 52:
                BlockchainProcessorImpl.getInstance().validateAtNextScan();
                apply(null);
            case 53:
                apply("DROP INDEX transaction_recipient_id_idx");
            case 54:
                apply("ALTER TABLE transaction ALTER COLUMN recipient_id SET NULL");
            case 55:
                try (Connection con = Db.getConnection();
                     Statement stmt = con.createStatement();
                     PreparedStatement pstmt = con.prepareStatement("UPDATE transaction SET recipient_id = null WHERE type = ? AND subtype = ?")) {
                    try {
                        for (byte type = 0; type <= 4; type++) {
                            for (byte subtype = 0; subtype <= 8; subtype++) {
                                TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
                                if (transactionType == null) {
                                    continue;
                                }
                                if (!transactionType.hasRecipient()) {
                                    pstmt.setByte(1, type);
                                    pstmt.setByte(2, subtype);
                                    pstmt.executeUpdate();
                                }
                            }
                        }
                        stmt.executeUpdate("UPDATE version SET next_update = next_update + 1");
                        con.commit();
                    } catch (SQLException e) {
                        con.rollback();
                        throw e;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            case 56:
                apply("CREATE INDEX IF NOT EXISTS transaction_recipient_id_idx ON transaction (recipient_id)");
            case 57:
                apply("DROP INDEX transaction_timestamp_idx");
            case 58:
                apply("CREATE INDEX IF NOT EXISTS transaction_timestamp_idx ON transaction (timestamp DESC)");
            case 59:
                apply("ALTER TABLE transaction ADD COLUMN version TINYINT");
            case 60:
                apply("UPDATE transaction SET version = 0");
            case 61:
                apply("ALTER TABLE transaction ALTER COLUMN version SET NOT NULL");
            case 62:
                apply("ALTER TABLE transaction ADD COLUMN has_message BOOLEAN NOT NULL DEFAULT FALSE");
            case 63:
                apply("ALTER TABLE transaction ADD COLUMN has_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE");
            case 64:
                apply("UPDATE transaction SET has_message = TRUE WHERE type = 1 AND subtype = 0");
            case 65:
                apply("ALTER TABLE transaction ADD COLUMN has_public_key_announcement BOOLEAN NOT NULL DEFAULT FALSE");
            case 66:
                apply("CREATE TABLE IF NOT EXISTS alias (id BIGINT NOT NULL, FOREIGN KEY (id) REFERENCES transaction (id), "
                        + "account_id BIGINT NOT NULL, alias_name VARCHAR NOT NULL, "
                        + "alias_name_lower VARCHAR AS LOWER (alias_name) NOT NULL, "
                        + "alias_uri VARCHAR NOT NULL, timestamp INT NOT NULL, "
                        + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 67:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS alias_id_height_idx ON alias (id, height DESC)");
            case 68:
                apply("CREATE INDEX IF NOT EXISTS alias_account_id_idx ON alias (account_id, height DESC)");
            case 69:
                apply("CREATE INDEX IF NOT EXISTS alias_name_lower_idx ON alias (alias_name_lower)");
            case 70:
                apply("CREATE TABLE IF NOT EXISTS alias_offer (id BIGINT NOT NULL, "
                        + "price BIGINT NOT NULL, buyer_id BIGINT, "
                        + "height INT NOT NULL, latest BOOLEAN DEFAULT TRUE NOT NULL)");
            case 71:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS alias_offer_id_height_idx ON alias_offer (id, height DESC)");
            case 72:
                apply("CREATE TABLE IF NOT EXISTS asset (db_id INT IDENTITY, id BIGINT NOT NULL, FOREIGN KEY (id) REFERENCES "
                        + "transaction (id), account_id BIGINT NOT NULL, "
                        + "name VARCHAR NOT NULL, description VARCHAR, quantity BIGINT NOT NULL, decimals TINYINT NOT NULL)");
            case 73:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS asset_id_idx ON asset (id)");
            case 74:
                apply("CREATE INDEX IF NOT EXISTS asset_account_id_idx ON asset (account_id)");
            case 75:
                apply("CREATE TABLE IF NOT EXISTS trade (db_id INT IDENTITY, asset_id BIGINT NOT NULL, FOREIGN KEY (asset_id) "
                        + "REFERENCES asset (id), block_id BIGINT NOT NULL, FOREIGN KEY (block_id) REFERENCES block (id), "
                        + "ask_order_id BIGINT NOT NULL, bid_order_id BIGINT NOT NULL, quantity BIGINT NOT NULL, "
                        + "price BIGINT NOT NULL, timestamp INT NOT NULL, height INT NOT NULL)");
            case 76:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS trade_ask_bid_idx ON trade (ask_order_id, bid_order_id)");
            case 77:
                apply("CREATE INDEX IF NOT EXISTS trade_asset_id_idx ON trade (asset_id, height DESC)");
            case 78:
                apply("CREATE TABLE IF NOT EXISTS ask_order (db_id INT IDENTITY, id BIGINT NOT NULL, FOREIGN KEY (id) REFERENCES "
                        + "transaction (id), account_id BIGINT NOT NULL, "
                        + "asset_id BIGINT NOT NULL, FOREIGN KEY (asset_id) REFERENCES asset (id), price BIGINT NOT NULL, "
                        + "quantity BIGINT NOT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 79:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS ask_order_id_height_idx ON ask_order (id, height DESC)");
            case 80:
                apply("CREATE INDEX IF NOT EXISTS ask_order_account_id_idx ON ask_order (account_id, height DESC)");
            case 81:
                apply("CREATE INDEX IF NOT EXISTS ask_order_asset_id_price_idx ON ask_order (asset_id, price)");
            case 82:
                apply("CREATE TABLE IF NOT EXISTS bid_order (db_id INT IDENTITY, id BIGINT NOT NULL, FOREIGN KEY (id) REFERENCES "
                        + "transaction (id), account_id BIGINT NOT NULL, "
                        + "asset_id BIGINT NOT NULL, FOREIGN KEY (asset_id) REFERENCES asset (id), price BIGINT NOT NULL, "
                        + "quantity BIGINT NOT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 83:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS bid_order_id_height_idx ON bid_order (id, height DESC)");
            case 84:
                apply("CREATE INDEX IF NOT EXISTS bid_order_account_id_idx ON bid_order (account_id, height DESC)");
            case 85:
                apply("CREATE INDEX IF NOT EXISTS bid_order_asset_id_price_idx ON bid_order (asset_id, price DESC)");
            case 86:
                apply("CREATE TABLE IF NOT EXISTS vote (db_id INT IDENTITY, id BIGINT NOT NULL, FOREIGN KEY (id) REFERENCES "
                       + "transaction (id), poll_id BIGINT NOT NULL, "
                        + "voter_id BIGINT NOT NULL, vote_bytes VARBINARY NOT NULL)");
            case 87:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS vote_id_idx ON vote (id)");
            case 88:
                apply("CREATE INDEX IF NOT EXISTS vote_poll_id_idx ON vote (poll_id)");
            case 89:
                apply("CREATE TABLE IF NOT EXISTS poll (db_id INT IDENTITY, id BIGINT NOT NULL, FOREIGN KEY (id) REFERENCES "
                        + "transaction (id), name VARCHAR NOT NULL, "
                        + "description VARCHAR, options ARRAY NOT NULL, min_num_options TINYINT, max_num_options TINYINT, "
                        +" binary_options BOOLEAN NOT NULL)");
            case 90:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS poll_id_idx ON poll (id)");
            case 91:
                apply("ALTER TABLE vote ADD FOREIGN KEY (poll_id) REFERENCES poll (id)");
            case 92:
                apply("CREATE TABLE IF NOT EXISTS hub (db_id INT IDENTITY, account_id BIGINT NOT NULL, min_fee_per_byte "
                        + "BIGINT NOT NULL, uris ARRAY NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 93:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS hub_account_id_height_idx ON hub (account_id, height DESC)");
            case 94:
                apply("CREATE TABLE IF NOT EXISTS goods (db_id INT IDENTITY, id BIGINT NOT NULL, FOREIGN KEY (id) REFERENCES "
                        + "transaction (id), seller_id BIGINT NOT NULL, name VARCHAR NOT NULL, description VARCHAR, "
                        + "tags VARCHAR, timestamp INT NOT NULL, quantity INT NOT NULL, price BIGINT NOT NULL, "
                        + "delisted BOOLEAN NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 95:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS goods_id_height_idx ON goods (id, height DESC)");
            case 96:
                apply("CREATE INDEX IF NOT EXISTS goods_seller_id_name_idx ON goods (seller_id, name)");
            case 97:
                apply("CREATE INDEX IF NOT EXISTS goods_timestamp_idx ON goods (timestamp DESC, height DESC)");
            case 98:
                apply("CREATE TABLE IF NOT EXISTS purchase (db_id INT IDENTITY, id BIGINT NOT NULL, FOREIGN KEY (id) "
                        + "REFERENCES transaction (id), buyer_id BIGINT NOT NULL, goods_id BIGINT NOT NULL, "
                        + "seller_id BIGINT NOT NULL, quantity INT NOT NULL, "
                        + "price BIGINT NOT NULL, deadline INT NOT NULL, note VARBINARY, nonce BINARY(32), "
                        + "timestamp INT NOT NULL, pending BOOLEAN NOT NULL, goods VARBINARY, goods_nonce BINARY(32), "
                        + "refund_note VARBINARY, refund_nonce BINARY(32), has_feedback_notes BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "has_public_feedbacks BOOLEAN NOT NULL DEFAULT FALSE, discount BIGINT NOT NULL, refund BIGINT NOT NULL, "
                        + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 99:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS purchase_id_height_idx ON purchase (id, height DESC)");
            case 100:
                apply("CREATE INDEX IF NOT EXISTS purchase_buyer_id_height_idx ON purchase (buyer_id, height DESC)");
            case 101:
                apply("CREATE INDEX IF NOT EXISTS purchase_seller_id_height_idx ON purchase (seller_id, height DESC)");
            case 102:
                apply("CREATE INDEX IF NOT EXISTS purchase_deadline_idx ON purchase (deadline DESC, height DESC)");
            case 103:
                apply("CREATE TABLE IF NOT EXISTS account (db_id INT IDENTITY, id BIGINT NOT NULL, creation_height INT NOT NULL, "
                        + "public_key BINARY(32), key_height INT, balance BIGINT NOT NULL, unconfirmed_balance BIGINT NOT NULL, "
                        + "forged_balance BIGINT NOT NULL, name VARCHAR, description VARCHAR, current_leasing_height_from INT, "
                        + "current_leasing_height_to INT, current_lessee_id BIGINT NULL, next_leasing_height_from INT, "
                        + "next_leasing_height_to INT, next_lessee_id BIGINT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 104:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_id_height_idx ON account (id, height DESC)");
            case 105:
                apply("CREATE INDEX IF NOT EXISTS account_current_lessee_id_leasing_height_idx ON account (current_lessee_id, "
                        + "current_leasing_height_to DESC)");
            case 106:
                apply("CREATE TABLE IF NOT EXISTS account_asset (db_id INT IDENTITY, account_id BIGINT NOT NULL, "
                        + "asset_id BIGINT NOT NULL, FOREIGN KEY (asset_id) REFERENCES asset (id), quantity BIGINT NOT NULL, "
                        + "unconfirmed_quantity BIGINT NOT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 107:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_asset_id_height_idx ON account_asset (account_id, asset_id, height DESC)");
            case 108:
                apply("CREATE TABLE IF NOT EXISTS account_guaranteed_balance (db_id INT IDENTITY, account_id BIGINT NOT NULL, "
                        + "additions BIGINT NOT NULL, height INT NOT NULL)");
            case 109:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_guaranteed_balance_id_height_idx ON account_guaranteed_balance "
                        + "(account_id, height DESC)");
            case 110:
                return;
            default:
                throw new RuntimeException("Database inconsistent with code, probably trying to run older code on newer database");
        }
    }

    private DbVersion() {} //never
}
