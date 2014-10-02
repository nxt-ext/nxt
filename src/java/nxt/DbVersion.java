package nxt;

import nxt.db.Db;
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
                stmt.executeUpdate("UPDATE version SET next_update = next_update + 1");
                Db.commitTransaction();
            } catch (Exception e) {
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
                            "('54.84.4.195'), ('139.30.62.180'), ('bitsy05.vps.nxtcrypto.org'), ('128.199.151.235'), ('199.233.245.105'), " +
                            "('54.201.228.27'), ('209.126.70.159'), ('77.123.25.170'), ('wallet.nxtty.com'), ('198.27.64.207'), " +
                            "('188.226.245.226'), ('79.215.198.253'), ('88.12.55.125'), ('nxt.sx'), ('24.149.8.238'), ('abctc.vps.nxtcrypto.org'), " +
                            "('162.243.198.24'), ('178.32.221.221'), ('nxt3.grit.tk:37874'), ('54.187.153.45'), ('24.161.110.115'), " +
                            "('nxtnode.hopto.org'), ('89.12.92.15'), ('191.238.101.73'), ('nxt.homer.ru'), ('nxtcoint119a.no-ip.org'), " +
                            "('84.46.53.162'), ('178.15.99.67'), ('107.17.66.127'), ('77.179.112.19'), ('195.154.136.165'), ('210.188.36.5'), " +
                            "('212.18.225.173'), ('109.254.63.44'), ('90.146.62.91'), ('nxt01.now.im'), ('node10.mynxt.info'), ('198.98.119.85'), " +
                            "('zobue.com'), ('89.250.240.56'), ('bitsy08.vps.nxtcrypto.org'), ('beor.homeip.net'), ('vps12.nxtcrypto.org'), " +
                            "('188.35.156.10'), ('54.88.54.58'), ('77.249.237.229'), ('mycrypto.no-ip.biz'), ('213.46.57.77'), ('nxtpi.zapto.org'), " +
                            "('nxt4.grit.tk'), ('80.82.209.82'), ('ns1.anameserver.de'), ('46.28.111.249'), ('178.194.110.193'), ('106.187.95.232'), " +
                            "('nebula-gw.hopto.org'), ('vps3.nxtcrypto.org'), ('198.57.198.33'), ('168.63.232.16'), ('77.179.97.27'), " +
                            "('178.33.203.157'), ('79.24.191.97'), ('bitsy04.vps.nxtcrypto.org'), ('humanoide.thican.net'), " +
                            "('bitsy09.vps.nxtcrypto.org'), ('bitsy07.vps.nxtcrypto.org'), ('173.17.82.130'), ('vps2.nxtcrypto.org'), " +
                            "('80.86.92.139'), ('180.157.101.215'), ('54.214.250.209'), ('87.230.14.1'), ('zdani.szn.dk'), ('nxt5.webice.ru'), " +
                            "('188.226.206.41'), ('nxt7.webice.ru'), ('nxt1.grit.tk:17874'), ('node13.mynxt.info'), ('90.153.116.122'), " +
                            "('66.30.204.105'), ('nxt1.webice.ru'), ('63.165.243.112'), ('oldnbold.vps.nxtcrypto.org'), ('sluni.szn.dk'), " +
                            "('105.229.177.106'), ('88.163.78.131'), ('80.153.101.190'), ('188.138.88.154'), ('85.25.198.120'), ('89.250.243.200'), " +
                            "('nxtx.ru'), ('87.139.122.48'), ('188.226.179.119'), ('vps5.nxtcrypto.org'), ('dreschel2.dyndns.org'), " +
                            "('67.212.71.173'), ('88.153.5.179'), ('rigel1.ddns.net'), ('77.58.253.73'), ('miasik.no-ip.org'), ('217.26.24.27'), " +
                            "('cyborg.thican.net'), ('cryptkeeper.vps.nxtcrypto.org'), ('cryonet.de'), ('nxt2.webice.ru'), ('43.252.230.48'), " +
                            "('91.202.253.240'), ('nxtforgersr.ddns.net'), ('silvanoip.dhcp.biz'), ('85.214.222.82'), ('87.139.122.157'), " +
                            "('54.164.174.175'), ('24.230.136.187'), ('128.199.189.226'), ('101.164.96.109'), ('allbits.vps.nxtcrypto.org'), " +
                            "('211.149.213.86'), ('node7.mynxt.info'), ('107.170.189.27'), ('89.12.202.115'), ('49.245.169.94'), ('91.121.41.192'), " +
                            "('xeqtorcreed.vps.nxtcrypto.org'), ('knattereule.sytes.net'), ('jefdiesel.vps.nxtcrypto.org'), ('node1.mynxt.info'), " +
                            "('111.199.191.99'), ('nxt1107.no-ip.biz'), ('95.68.48.56'), ('87.138.143.21'), ('mycoinmine.org'), ('46.173.9.98'), " +
                            "('ct.flipflop.mooo.com'), ('83.172.25.92'), ('xyzzyx.vps.nxtcrypto.org'), ('31.15.211.201'), ('84.128.162.88'), " +
                            "('80.86.92.70'), ('108.61.57.76'), ('samson.vps.nxtcrypto.org'), ('screenname.vps.nxtcrypto.org'), ('nxt.hofhom.nl'), " +
                            "('111.194.210.159'), ('58.95.145.117'), ('95.68.86.118'), ('54.245.255.250'), ('bitsy06.vps.nxtcrypto.org'), " +
                            "('vps4.nxtcrypto.org'), ('54.88.120.117'), ('54.164.98.250'), ('46.165.208.108'), ('xeqtorcreed2.vps.nxtcrypto.org'), " +
                            "('vps1.nxtcrypto.org'), ('nxt8.webice.ru'), ('162.201.61.133'), ('54.191.138.175'), ('54.85.132.143'), " +
                            "('80.86.92.66'), ('84.242.91.139'), ('82.211.30.183'), ('bitsy03.vps.nxtcrypto.org'), ('vh44.ddns.net:7873'), " +
                            "('node0.forgenxt.com'), ('185.4.72.115'), ('108.170.9.170'), ('54.200.164.31'), ('bitsy01.vps.nxtcrypto.org'), " +
                            "('178.150.207.53'), ('71.90.207.16'), ('107.170.3.62'), ('54.86.132.52'), ('85.214.199.215'), ('cubie-solar.mjke.de'), " +
                            "('nxt2.grit.tk:27874'), ('108.231.13.250'), ('104.131.254.22'), ('cerub.ddns.net'), ('216.36.3.42'), " +
                            "('37.120.168.131'), ('54.77.63.53'), ('93.103.20.35'), ('54.213.222.141'), ('162.243.213.190'), ('89.250.240.60'), " +
                            "('67.255.7.120'), ('87.254.182.122'), ('95.143.216.60'), ('79.164.108.1'), ('67.212.71.171'), " +
                            "('node15.mynxt.info'), ('192.51.0.12'), ('76.164.201.91'), ('88.198.74.99'), ('bitsy10.vps.nxtcrypto.org'), " +
                            "('node2.mynxt.info'), ('vps9.nxtcrypto.org'), ('46.165.209.144'), ('46.4.212.230'), ('185.12.44.108'), " +
                            "('162.243.87.10'), ('76.250.84.156'), ('91.69.121.229'), ('178.24.158.31'), ('209.126.70.170'), ('93.129.172.14'), " +
                            "('93.219.140.96'), ('serras.homenet.org'), ('89.72.57.246'), ('37.187.21.28'), ('lyynx.vps.nxtcrypto.org'), " +
                            "('105.224.254.145'), ('195.154.174.124'), ('184.57.30.220'), ('54.83.4.11'), ('nxt.phukhew.com'), ('nxt5.grit.tk'), " +
                            "('nxt4.webice.ru'), ('162.243.145.83'), ('89.133.34.109'), ('73.36.141.199'), ('90.153.30.215'), " +
                            "('209.126.70.156'), ('bitsy02.vps.nxtcrypto.org'), ('pakisnxt.no-ip.org'), ('188.167.90.118'), ('107.170.164.129'), " +
                            "('23.102.0.45'), ('46.194.8.79')");
                } else {
                    apply("INSERT INTO peer (address) VALUES " +
                            "('178.150.207.53'), ('192.241.223.132'), ('node9.mynxtcoin.org'), ('node10.mynxtcoin.org'), " +
                            "('node3.mynxtcoin.org'), ('109.87.169.253'), ('nxtnet.fr'), ('50.112.241.97'), " +
                            "('2.84.142.149'), ('bug.airdns.org'), ('83.212.103.14'), ('62.210.131.30'), ('104.131.254.22'), " +
                            "('46.28.111.249'), ('94.79.54.205')");
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
                apply(null);
            case 48:
                apply("ALTER TABLE transaction DROP COLUMN attachment");
            case 49:
                apply(null);
            case 50:
                apply("ALTER TABLE transaction DROP COLUMN referenced_transaction_id");
            case 51:
                apply("ALTER TABLE transaction DROP COLUMN hash");
            case 52:
                apply(null);
            case 53:
                apply("DROP INDEX transaction_recipient_id_idx");
            case 54:
                apply("ALTER TABLE transaction ALTER COLUMN recipient_id SET NULL");
            case 55:
                BlockDb.deleteAll();
                apply(null);
            case 56:
                apply("CREATE INDEX IF NOT EXISTS transaction_recipient_id_idx ON transaction (recipient_id)");
            case 57:
                apply(null);
            case 58:
                apply(null);
            case 59:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS version TINYINT");
            case 60:
                apply("UPDATE transaction SET version = 0");
            case 61:
                apply("ALTER TABLE transaction ALTER COLUMN version SET NOT NULL");
            case 62:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS has_message BOOLEAN NOT NULL DEFAULT FALSE");
            case 63:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS has_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE");
            case 64:
                apply("UPDATE transaction SET has_message = TRUE WHERE type = 1 AND subtype = 0");
            case 65:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS has_public_key_announcement BOOLEAN NOT NULL DEFAULT FALSE");
            case 66:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS ec_block_height INT DEFAULT NULL");
            case 67:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS ec_block_id BIGINT DEFAULT NULL");
            case 68:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS has_encrypttoself_message BOOLEAN NOT NULL DEFAULT FALSE");
            case 69:
                apply("CREATE INDEX IF NOT EXISTS transaction_block_timestamp_idx ON transaction (block_timestamp DESC)");
            case 70:
                apply("DROP INDEX transaction_timestamp_idx");
            case 71:
                apply("CREATE TABLE IF NOT EXISTS alias (db_id INT IDENTITY, id BIGINT NOT NULL, "
                        + "account_id BIGINT NOT NULL, alias_name VARCHAR NOT NULL, "
                        + "alias_name_lower VARCHAR AS LOWER (alias_name) NOT NULL, "
                        + "alias_uri VARCHAR NOT NULL, timestamp INT NOT NULL, "
                        + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 72:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS alias_id_height_idx ON alias (id, height DESC)");
            case 73:
                apply("CREATE INDEX IF NOT EXISTS alias_account_id_idx ON alias (account_id, height DESC)");
            case 74:
                apply("CREATE INDEX IF NOT EXISTS alias_name_lower_idx ON alias (alias_name_lower)");
            case 75:
                apply("CREATE TABLE IF NOT EXISTS alias_offer (db_id INT IDENTITY, id BIGINT NOT NULL, "
                        + "price BIGINT NOT NULL, buyer_id BIGINT, "
                        + "height INT NOT NULL, latest BOOLEAN DEFAULT TRUE NOT NULL)");
            case 76:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS alias_offer_id_height_idx ON alias_offer (id, height DESC)");
            case 77:
                apply("CREATE TABLE IF NOT EXISTS asset (db_id INT IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "name VARCHAR NOT NULL, description VARCHAR, quantity BIGINT NOT NULL, decimals TINYINT NOT NULL, "
                        + "height INT NOT NULL)");
            case 78:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS asset_id_idx ON asset (id)");
            case 79:
                apply("CREATE INDEX IF NOT EXISTS asset_account_id_idx ON asset (account_id)");
            case 80:
                apply("CREATE TABLE IF NOT EXISTS trade (db_id INT IDENTITY, asset_id BIGINT NOT NULL, block_id BIGINT NOT NULL, "
                        + "ask_order_id BIGINT NOT NULL, bid_order_id BIGINT NOT NULL, ask_order_height INT NOT NULL, "
                        + "bid_order_height INT NOT NULL, seller_id BIGINT NOT NULL, buyer_id BIGINT NOT NULL, "
                        + "quantity BIGINT NOT NULL, price BIGINT NOT NULL, timestamp INT NOT NULL, height INT NOT NULL)");
            case 81:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS trade_ask_bid_idx ON trade (ask_order_id, bid_order_id)");
            case 82:
                apply("CREATE INDEX IF NOT EXISTS trade_asset_id_idx ON trade (asset_id, height DESC)");
            case 83:
                apply("CREATE INDEX IF NOT EXISTS trade_seller_id_idx ON trade (seller_id, height DESC)");
            case 84:
                apply("CREATE INDEX IF NOT EXISTS trade_buyer_id_idx ON trade (buyer_id, height DESC)");
            case 85:
                apply("CREATE TABLE IF NOT EXISTS ask_order (db_id INT IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "asset_id BIGINT NOT NULL, price BIGINT NOT NULL, "
                        + "quantity BIGINT NOT NULL, creation_height INT NOT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 86:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS ask_order_id_height_idx ON ask_order (id, height DESC)");
            case 87:
                apply("CREATE INDEX IF NOT EXISTS ask_order_account_id_idx ON ask_order (account_id, height DESC)");
            case 88:
                apply("CREATE INDEX IF NOT EXISTS ask_order_asset_id_price_idx ON ask_order (asset_id, price)");
            case 89:
                apply("CREATE TABLE IF NOT EXISTS bid_order (db_id INT IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "asset_id BIGINT NOT NULL, price BIGINT NOT NULL, "
                        + "quantity BIGINT NOT NULL, creation_height INT NOT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 90:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS bid_order_id_height_idx ON bid_order (id, height DESC)");
            case 91:
                apply("CREATE INDEX IF NOT EXISTS bid_order_account_id_idx ON bid_order (account_id, height DESC)");
            case 92:
                apply("CREATE INDEX IF NOT EXISTS bid_order_asset_id_price_idx ON bid_order (asset_id, price DESC)");
            case 93:
                apply("CREATE TABLE IF NOT EXISTS goods (db_id INT IDENTITY, id BIGINT NOT NULL, seller_id BIGINT NOT NULL, "
                        + "name VARCHAR NOT NULL, description VARCHAR, "
                        + "tags VARCHAR, timestamp INT NOT NULL, quantity INT NOT NULL, price BIGINT NOT NULL, "
                        + "delisted BOOLEAN NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 94:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS goods_id_height_idx ON goods (id, height DESC)");
            case 95:
                apply("CREATE INDEX IF NOT EXISTS goods_seller_id_name_idx ON goods (seller_id, name)");
            case 96:
                apply("CREATE INDEX IF NOT EXISTS goods_timestamp_idx ON goods (timestamp DESC, height DESC)");
            case 97:
                apply("CREATE TABLE IF NOT EXISTS purchase (db_id INT IDENTITY, id BIGINT NOT NULL, buyer_id BIGINT NOT NULL, "
                        + "goods_id BIGINT NOT NULL, "
                        + "seller_id BIGINT NOT NULL, quantity INT NOT NULL, "
                        + "price BIGINT NOT NULL, deadline INT NOT NULL, note VARBINARY, nonce BINARY(32), "
                        + "timestamp INT NOT NULL, pending BOOLEAN NOT NULL, goods VARBINARY, goods_nonce BINARY(32), "
                        + "refund_note VARBINARY, refund_nonce BINARY(32), has_feedback_notes BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "has_public_feedbacks BOOLEAN NOT NULL DEFAULT FALSE, discount BIGINT NOT NULL, refund BIGINT NOT NULL, "
                        + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 98:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS purchase_id_height_idx ON purchase (id, height DESC)");
            case 99:
                apply("CREATE INDEX IF NOT EXISTS purchase_buyer_id_height_idx ON purchase (buyer_id, height DESC)");
            case 100:
                apply("CREATE INDEX IF NOT EXISTS purchase_seller_id_height_idx ON purchase (seller_id, height DESC)");
            case 101:
                apply("CREATE INDEX IF NOT EXISTS purchase_deadline_idx ON purchase (deadline DESC, height DESC)");
            case 102:
                apply("CREATE TABLE IF NOT EXISTS account (db_id INT IDENTITY, id BIGINT NOT NULL, creation_height INT NOT NULL, "
                        + "public_key BINARY(32), key_height INT, balance BIGINT NOT NULL, unconfirmed_balance BIGINT NOT NULL, "
                        + "forged_balance BIGINT NOT NULL, name VARCHAR, description VARCHAR, current_leasing_height_from INT, "
                        + "current_leasing_height_to INT, current_lessee_id BIGINT NULL, next_leasing_height_from INT, "
                        + "next_leasing_height_to INT, next_lessee_id BIGINT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 103:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_id_height_idx ON account (id, height DESC)");
            case 104:
                apply("CREATE INDEX IF NOT EXISTS account_current_lessee_id_leasing_height_idx ON account (current_lessee_id, "
                        + "current_leasing_height_to DESC)");
            case 105:
                apply("CREATE TABLE IF NOT EXISTS account_asset (db_id INT IDENTITY, account_id BIGINT NOT NULL, "
                        + "asset_id BIGINT NOT NULL, quantity BIGINT NOT NULL, unconfirmed_quantity BIGINT NOT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 106:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_asset_id_height_idx ON account_asset (account_id, asset_id, height DESC)");
            case 107:
                apply("CREATE TABLE IF NOT EXISTS account_guaranteed_balance (db_id INT IDENTITY, account_id BIGINT NOT NULL, "
                        + "additions BIGINT NOT NULL, height INT NOT NULL)");
            case 108:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_guaranteed_balance_id_height_idx ON account_guaranteed_balance "
                        + "(account_id, height DESC)");
            case 109:
                apply("CREATE TABLE IF NOT EXISTS purchase_feedback (db_id INT IDENTITY, id BIGINT NOT NULL, feedback_data VARBINARY NOT NULL, "
                        + "feedback_nonce BINARY(32) NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 110:
                apply("CREATE INDEX IF NOT EXISTS purchase_feedback_id_height_idx ON purchase_feedback (id, height DESC)");
            case 111:
                apply("CREATE TABLE IF NOT EXISTS purchase_public_feedback (db_id INT IDENTITY, id BIGINT NOT NULL, public_feedback "
                        + "VARCHAR NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 112:
                apply("CREATE INDEX IF NOT EXISTS purchase_public_feedback_id_height_idx ON purchase_public_feedback (id, height DESC)");
            case 113:
                apply("CREATE TABLE IF NOT EXISTS unconfirmed_transaction (db_id INT IDENTITY, id BIGINT NOT NULL, expiration INT NOT NULL, "
                        + "transaction_height INT NOT NULL, fee_per_byte BIGINT NOT NULL, timestamp INT NOT NULL, "
                        + "transaction_bytes VARBINARY NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 114:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS unconfirmed_transaction_id_height_idx ON unconfirmed_transaction (id, height DESC)");
            case 115:
                apply("CREATE INDEX IF NOT EXISTS unconfirmed_transaction_height_fee_timestamp_idx ON unconfirmed_transaction "
                        + "(transaction_height ASC, fee_per_byte DESC, timestamp ASC)");
            case 116:
                apply(null);
            case 117:
                apply("CREATE TABLE IF NOT EXISTS transfer (db_id INT IDENTITY, id BIGINT NOT NULL, asset_id BIGINT NOT NULL, "
                        + "sender_id BIGINT NOT NULL, recipient_id BIGINT NOT NULL, quantity BIGINT NOT NULL, height INT NOT NULL)");
            case 118:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS transfer_id_idx ON transfer (id)");
            case 119:
                apply("CREATE INDEX IF NOT EXISTS transfer_asset_id_idx ON transfer (asset_id, height DESC)");
            case 120:
                apply("CREATE INDEX IF NOT EXISTS transfer_sender_id_idx ON transfer (sender_id, height DESC)");
            case 121:
                apply("CREATE INDEX IF NOT EXISTS transfer_recipient_id_idx ON transfer (recipient_id, height DESC)");
            case 122:
                BlockchainProcessorImpl.getInstance().forceScanAtStart();
                apply(null);
            case 123:
                apply("CREATE TABLE IF NOT EXISTS currency (db_id INT IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "name VARCHAR NOT NULL, code VARCHAR NOT NULL, description VARCHAR, type TINYINT NOT NULL, total_supply BIGINT NOT NULL, "
                        + "issuance_height INT NOT NULL, min_reserve_per_unit_nqt BIGINT NOT NULL, min_difficulty TINYINT NOT NULL, "
                        + "max_difficulty TINYINT NOT NULL, ruleset TINYINT NOT NULL, algorithm TINYINT NOT NULL, current_supply BIGINT NOT NULL, "
                        + "current_reserve_per_unit_nqt BIGINT NOT NULL, "
                        + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 124:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS currency_id_height_idx ON currency (id, height DESC)");
            case 125:
                apply("CREATE INDEX IF NOT EXISTS currency_account_id_idx ON currency (account_id)");
            case 126:
                apply("CREATE TABLE IF NOT EXISTS account_currency (db_id INT IDENTITY, account_id BIGINT NOT NULL, "
                        + "currency_id BIGINT NOT NULL, units BIGINT NOT NULL, unconfirmed_units BIGINT NOT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 127:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_currency_id_height_idx ON account_currency (account_id, currency_id, height DESC)");
            case 128:
                apply("CREATE TABLE IF NOT EXISTS currency_founder (db_id INT IDENTITY, currency_id BIGINT NOT NULL, "
                        + "account_id BIGINT NOT NULL, value BIGINT NOT NULL, "
                        + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 129:
                apply("CREATE INDEX IF NOT EXISTS currency_founder_currency_id_idx ON account_currency (currency_id)");
            case 130:
                apply("CREATE TABLE IF NOT EXISTS currency_mint (db_id INT IDENTITY, currency_id BIGINT NOT NULL, "
                        + "account_id BIGINT NOT NULL, counter BIGINT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 131:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS currency_mint_currency_id_account_id_height_idx ON currency_mint (currency_id, account_id, height)");
            case 132:
                apply("CREATE TABLE IF NOT EXISTS buy_offer (db_id INT IDENTITY, id BIGINT NOT NULL, currency_id BIGINT NOT NULL, account_id BIGINT NOT NULL,"
                        + "rate BIGINT NOT NULL, unit_limit BIGINT NOT NULL, supply BIGINT NOT NULL, expiration_height INT NOT NULL,"
                        + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 133:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS buy_offer_id_idx ON buy_offer (id)");
            case 134:
                apply("CREATE INDEX IF NOT EXISTS buy_offer_currency_id_idx ON buy_offer (currency_id)");
            case 135:
                apply("CREATE TABLE IF NOT EXISTS sell_offer (db_id INT IDENTITY, id BIGINT NOT NULL, currency_id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "rate BIGINT NOT NULL, unit_limit BIGINT NOT NULL, supply BIGINT NOT NULL, expiration_height INT NOT NULL, "
                        + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 136:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS sell_offer_id_idx ON sell_offer (id)");
            case 137:
                apply("CREATE INDEX IF NOT EXISTS sell_offer_currency_id_idx ON sell_offer (currency_id)");
            case 138:
                BlockchainProcessorImpl.getInstance().forceScanAtStart();
                apply(null);
            case 139:
                apply("CREATE TABLE IF NOT EXISTS exchange (db_id INT IDENTITY, currency_id BIGINT NOT NULL, block_id BIGINT NOT NULL, "
                        + "offer_id BIGINT NOT NULL, seller_id BIGINT NOT NULL, "
                        + "buyer_id BIGINT NOT NULL, units BIGINT NOT NULL, "
                        + "rate BIGINT NOT NULL, timestamp INT NOT NULL, height INT NOT NULL)");
            case 140:
                apply("CREATE INDEX IF NOT EXISTS exchange_offer_idx ON exchange (offer_id)");
            case 141:
                apply("CREATE INDEX IF NOT EXISTS exchange_currency_id_idx ON exchange (currency_id, height DESC)");
            case 142:
                apply("CREATE INDEX IF NOT EXISTS exchange_seller_id_idx ON exchange (seller_id, height DESC)");
            case 143:
                apply("CREATE INDEX IF NOT EXISTS exchange_buyer_id_idx ON exchange (buyer_id, height DESC)");
            case 144:
                BlockchainProcessorImpl.getInstance().forceScanAtStart();
                apply(null);
            case 145:
                return;
            default:
                throw new RuntimeException("Database inconsistent with code, probably trying to run older code on newer database");
        }
    }

    private DbVersion() {} //never
}
