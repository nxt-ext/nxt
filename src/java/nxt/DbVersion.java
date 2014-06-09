package nxt;

import nxt.util.Logger;

import java.sql.Connection;
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
                stmt.executeUpdate("UPDATE version SET next_update = (SELECT next_update + 1 FROM version)");
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
                            "('77.179.106.9'), ('110.143.228.78'), ('54.72.7.96'), ('54.86.139.231'), " +
                            "('abctc.vps.nxtcrypto.org'), ('nxt.pucchiwerk.eu'), ('185.4.72.115'), ('vps4.nxtcrypto.org'), " +
                            "('89.250.240.63'), ('162.243.145.83'), ('85.10.199.79'), ('89.70.254.145'), " +
                            "('103.224.81.143'), ('85.181.230.69'), ('198.199.85.20'), ('217.17.88.5'), " +
                            "('109.87.169.253'), ('87.172.190.182'), ('67.149.193.205'), ('31.15.211.201'), " +
                            "('178.24.158.31'), ('46.4.77.180'), ('188.226.242.50'), ('108.170.40.2'), ('217.117.208.17'), " +
                            "('89.250.243.150'), ('178.26.207.190'), ('wallet.nxtty.com'), ('nxtnet.fr'), " +
                            "('188.194.241.72'), ('nxt.alkeron.com'), ('xyzzyx.vps.nxtcrypto.org'), ('69.207.170.32'), " +
                            "('bitsy02.vps.nxtcrypto.org'), ('83.240.14.35'), ('212.85.38.25'), ('212.85.38.103'), " +
                            "('vps9.nxtcrypto.org'), ('84.241.44.180'), ('158.195.217.79'), ('89.133.34.109'), " +
                            "('stakexplorer.com'), ('198.27.64.207'), ('vps5.nxtcrypto.org'), ('185.12.44.108'), " +
                            "('nxtcoin.ru'), ('vps6.nxtcrypto.org'), ('92.129.239.166'), ('91.69.121.229'), " +
                            "('nxtpi.zapto.org'), ('92.228.252.60'), ('216.8.180.222'), ('bitsy05.vps.nxtcrypto.org'), " +
                            "('88.198.142.92'), ('31.19.188.145'), ('54.186.135.231'), ('ankhy.no-ip.biz'), " +
                            "('94.26.187.66'), ('bitsy01.vps.nxtcrypto.org'), ('bitsy04.vps.nxtcrypto.org'), " +
                            "('90.188.4.177'), ('88.184.64.208'), ('raspnxt.hopto.org'), ('151.236.29.228'), " +
                            "('188.138.88.154'), ('nxt.homer.ru'), ('107.170.208.249'), ('31.150.173.6'), " +
                            "('vps10.nxtcrypto.org'), ('bitsy03.vps.nxtcrypto.org'), ('xeqtorcreed2.vps.nxtcrypto.org'), " +
                            "('lyynx.vps.nxtcrypto.org'), ('146.185.145.192'), ('37.138.105.143'), ('162.220.167.190'), " +
                            "('94.74.170.10'), ('vps7.nxtcrypto.org'), ('nxt01.now.im'), ('2.225.88.10'), " +
                            "('85.214.222.82'), ('vps8.nxtcrypto.org'), ('178.122.5.83'), ('88.160.247.181'), " +
                            "('85.229.150.2'), ('158.195.19.226'), ('nxt.ravensbloodrealms.com'), ('105.229.251.144'), " +
                            "('nxt.olxp.in'), ('37.59.47.155'), ('nxtportal.org'), ('87.198.219.221'), ('87.172.180.199'), " +
                            "('36.74.56.184'), ('nxtx.ru'), ('58.95.145.117'), ('109.230.224.65'), ('www.pagezo.de'), " +
                            "('allbits.vps.nxtcrypto.org'), ('107.170.3.62'), ('192.157.244.160'), ('vps12.nxtcrypto.org'), " +
                            "('nacho.damnserver.com'), ('67.212.71.173'), ('vps11.nxtcrypto.org'), ('miasik.no-ip.org'), " +
                            "('212.85.37.150'), ('217.26.24.27'), ('24.161.110.115'), ('89.70.164.196'), ('46.28.111.249'), " +
                            "('vps1.nxtcrypto.org'), ('199.195.148.27'), ('176.226.191.152'), " +
                            "('37.44.107.50'), ('95.143.216.60'), ('62.57.125.237'), ('xeqtorcreed.vps.nxtcrypto.org')");
                } else {
                    apply("INSERT INTO peer (address) VALUES " +
                            "('109.87.169.253'), ('nxtnet.fr'), ('node10.mynxtcoin.org'), ('50.112.241.97'), " +
                            "('node9.mynxtcoin.org'), ('2.84.142.149'), ('192.241.223.132'), ('node3.mynxtcoin.org'), " +
                            "('bug.airdns.org')");
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
                apply("CREATE TABLE IF NOT EXISTS alias (id BIGINT NOT NULL, account_id BIGINT NOT NULL, alias_name VARCHAR NOT NULL, "
                        + "alias_name_lower VARCHAR AS LOWER (alias_name) NOT NULL, "
                        + "alias_uri VARCHAR NOT NULL, timestamp INT NOT NULL, "
                        + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 53:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS alias_id_height_idx ON alias (id, height DESC)");
            case 54:
                apply("CREATE INDEX IF NOT EXISTS alias_account_id_idx ON alias (account_id, height DESC)");
            case 55:
                apply("CREATE INDEX IF NOT EXISTS alias_name_lower_idx ON alias (alias_name_lower)");
            case 56:
                apply("CREATE TABLE IF NOT EXISTS alias_offer (id BIGINT NOT NULL, price BIGINT NOT NULL, buyer_id BIGINT NOT NULL, "
                        + "height INT NOT NULL, latest BOOLEAN DEFAULT TRUE NOT NULL)");
            case 57:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS alias_offer_id_height_idx ON alias_offer (id, height DESC)");
            case 58:
                apply("CREATE TABLE IF NOT EXISTS asset (db_id INT IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "name VARCHAR NOT NULL, description VARCHAR, quantity BIGINT NOT NULL, decimals TINYINT NOT NULL)");
            case 59:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS asset_id_idx ON asset (id)");
            case 60:
                apply("CREATE INDEX IF NOT EXISTS asset_account_id_idx ON asset (account_id)");
            case 61:
                apply("CREATE TABLE IF NOT EXISTS trade (db_id INT IDENTITY, asset_id BIGINT NOT NULL, block_id BIGINT NOT NULL, "
                        + "ask_order_id BIGINT NOT NULL, bid_order_id BIGINT NOT NULL, quantity BIGINT NOT NULL, "
                        + "price BIGINT NOT NULL, timestamp INT NOT NULL, height INT NOT NULL)");
            case 62:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS trade_ask_bid_idx ON trade (ask_order_id, bid_order_id)");
            case 63:
                apply("CREATE INDEX IF NOT EXISTS trade_asset_id_idx ON trade (asset_id, height)");
            case 64:
                apply("CREATE TABLE IF NOT EXISTS ask_order (db_id INT IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "asset_id BIGINT NOT NULL, price BIGINT NOT NULL, quantity BIGINT NOT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 65:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS ask_order_id_height_idx ON ask_order (id, height DESC)");
            case 66:
                apply("CREATE INDEX IF NOT EXISTS ask_order_account_id_idx ON ask_order (account_id, height DESC)");
            case 67:
                apply("CREATE INDEX IF NOT EXISTS ask_order_asset_id_price_idx ON ask_order (asset_id, price)");
            case 68:
                apply("CREATE TABLE IF NOT EXISTS bid_order (db_id INT IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "asset_id BIGINT NOT NULL, price BIGINT NOT NULL, quantity BIGINT NOT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 69:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS bid_order_id_height_idx ON bid_order (id, height DESC)");
            case 70:
                apply("CREATE INDEX IF NOT EXISTS bid_order_account_id_idx ON bid_order (account_id, height DESC)");
            case 71:
                apply("CREATE INDEX IF NOT EXISTS bid_order_asset_id_price_idx ON bid_order (asset_id, price DESC)");
            case 72:
                return;
            default:
                throw new RuntimeException("Database inconsistent with code, probably trying to run older code on newer database");
        }
    }

    private DbVersion() {} //never
}
