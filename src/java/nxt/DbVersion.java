package nxt;

import nxt.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class DbVersion {

    static void init() {
        try (Connection con = Db.getConnection(); Statement stmt = con.createStatement()) {
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
                con.commit();
            }
            update(nextUpdate);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
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
                con.commit();
            } catch (Exception e) {
                con.rollback();
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
                            "('178.194.110.193'), ('nrs01.nxtsolaris.info'), ('xeqtorcreed2.vps.nxtcrypto.org'), ('5.101.101.137'), " +
                            "('54.76.203.25'), ('ns1.anameserver.de'), ('cryptkeeper.vps.nxtcrypto.org'), ('vps11.nxtcrypto.org'), " +
                            "('80.137.236.53'), ('wallet.nxtty.com'), ('2.84.130.26'), ('91.121.223.107'), ('80.137.229.25'), " +
                            "('enricoip.no-ip.biz'), ('195.154.127.172'), ('69.64.35.62'), ('88.168.85.129:7874'), ('105.229.160.133'), " +
                            "('rigel1.ddns.net'), ('59.36.74.47'), ('n2.nxtportal.org'), ('samson.vps.nxtcrypto.org'), " +
                            "('nrs02.nxtsolaris.info'), ('miasik.no-ip.org'), ('vh44.ddns.net:7873'), ('212.18.225.173'), " +
                            "('91.121.41.192'), ('serras.homenet.org'), ('217.17.88.5'), ('77.179.100.57'), ('89.98.191.95'), " +
                            "('nxt1107.no-ip.biz'), ('mycrypto.no-ip.biz'), ('89.250.240.63'), ('vps4.nxtcrypto.org'), " +
                            "('89.72.57.246'), ('bitsy10.vps.nxtcrypto.org'), ('85.191.52.188'), ('gayka.no-ip.info'), " +
                            "('77.179.99.25'), ('106.186.127.189'), ('23.238.198.218'), ('www.mycoinmine.org'), ('162.201.61.133'), " +
                            "('54.191.200.44'), ('54.186.166.78'), ('212.129.12.103'), ('node0.forgenxt.com'), ('188.226.179.119'), " +
                            "('lyynx.vps.nxtcrypto.org'), ('nxt.phukhew.com'), ('162.242.16.147'), ('pakisnxt.no-ip.org'), " +
                            "('85.214.200.59'), ('101.164.96.109'), ('nxt.alkeron.com'), ('83.212.102.244'), ('23.88.229.194'), " +
                            "('162.243.213.190'), ('87.139.122.157'), ('nxt1.webice.ru'), ('37.59.41.216'), ('46.149.84.141'), " +
                            "('87.138.143.21'), ('151.236.29.228'), ('99.244.142.34'), ('nxt10.webice.ru'), ('cobaltskky.hopto.org'), " +
                            "('83.212.103.18'), ('nxt9.webice.ru'), ('89.70.254.145'), ('190.10.9.166'), ('95.85.46.177'), " +
                            "('dreschel2.dyndns.org'), ('113.77.223.63'), ('50.98.11.195'), ('209.126.70.159'), ('178.24.158.31'), " +
                            "('54.210.102.135'), ('83.212.102.193'), ('195.154.174.124'), ('162.243.243.32'), ('87.148.12.130'), " +
                            "('83.69.2.13'), ('cryonet.de'), ('79.24.191.97'), ('nxt.homer.ru'), ('nxtpi.zapto.org'), " +
                            "('nxs1.hanza.co.id'), ('23.102.0.45'), ('2.86.61.231'), ('87.230.14.1'), ('105.224.252.123'), " +
                            "('88.163.78.131'), ('50.43.35.122'), ('80.137.233.81'), ('24.149.8.238'), ('91.34.227.212'), " +
                            "('217.186.178.66'), ('178.198.145.191'), ('73.36.141.199'), ('192.3.157.232'), ('2.225.88.10'), " +
                            "('74.192.195.151'), ('108.61.57.76'), ('109.230.224.65'), ('94.26.187.66'), ('124.244.49.12'), " +
                            "('88.12.55.125'), ('180.129.0.77'), ('162.243.145.83'), ('93.171.209.103'), ('87.139.122.48'), " +
                            "('89.250.240.60'), ('83.212.102.234'), ('112.199.191.219'), ('vps10.nxtcrypto.org'), ('85.10.201.15'), " +
                            "('179.43.128.136'), ('85.25.134.59'), ('80.86.92.70'), ('178.162.39.12'), ('46.194.145.144'), " +
                            "('bitsy09.vps.nxtcrypto.org'), ('147.32.246.247'), ('74.91.124.3'), ('95.68.87.206'), " +
                            "('115.28.220.183'), ('91.34.239.93'), ('121.40.84.99'), ('168.63.232.16'), ('105.227.3.50'), " +
                            "('211.149.213.86'), ('nxtcoint119a.no-ip.org'), ('186.220.71.26'), ('bitsy05.vps.nxtcrypto.org'), " +
                            "('80.137.229.62'), ('162.243.198.24'), ('61.131.37.210'), ('n1.nxtportal.org'), ('nxtx.ru'), " +
                            "('201.209.45.121'), ('5.35.119.103'), ('105.229.173.29'), ('114.215.142.34:15011'), ('caelum.no-ip.org'), " +
                            "('46.109.166.244'), ('89.250.240.56'), ('77.179.96.66'), ('90.184.9.47'), ('188.226.206.41'), " +
                            "('nxtnode.noip.me'), ('bitsy07.vps.nxtcrypto.org'), ('abctc.vps.nxtcrypto.org'), " +
                            "('bitsy01.vps.nxtcrypto.org'), ('107.170.189.27'), ('109.74.203.187:7874'), ('188.35.156.10'), " +
                            "('cubie-solar.mjke.de:7873'), ('46.173.9.98'), ('xyzzyx.vps.nxtcrypto.org'), ('188.226.197.131'), " +
                            "('jefdiesel.vps.nxtcrypto.org'), ('89.250.243.166'), ('46.194.14.81'), ('109.254.63.44'), " +
                            "('80.86.92.139'), ('91.121.41.45'), ('nxt01.now.im'), ('54.179.177.81'), ('83.212.124.193'), " +
                            "('bitsy03.vps.nxtcrypto.org'), ('xeqtorcreed.vps.nxtcrypto.org'), ('bitsy08.vps.nxtcrypto.org'), " +
                            "('178.62.50.75'), ('212.83.145.17'), ('107.170.164.129'), ('67.212.71.172'), ('oldnbold.vps.nxtcrypto.org'), " +
                            "('54.72.17.26'), ('24.224.68.29'), ('107.170.35.110'), ('nxt7.webice.ru'), ('88.79.173.189'), " +
                            "('83.212.102.194'), ('113.10.136.142'), ('54.187.11.72'), ('139.228.37.156'), ('105.224.252.84'), " +
                            "('bitsy02.vps.nxtcrypto.org'), ('199.217.119.33'), ('silvanoip.dhcp.biz'), ('84.242.91.139'), " +
                            "('80.153.101.190'), ('198.199.81.29'), ('54.86.132.52'), ('77.58.253.73'), ('213.46.57.77'), " +
                            "('54.84.4.195'), ('105.229.177.132'), ('217.26.24.27'), ('raspnxt.hopto.org'), ('188.138.88.154'), " +
                            "('113.78.101.129'), ('nxt2.webice.ru'), ('vps5.nxtcrypto.org'), ('80.86.92.66'), ('107.170.3.62'), " +
                            "('85.214.222.82'), ('94.74.170.10'), ('24.230.136.187'), ('99.47.218.132'), ('nxt.hofhom.nl'), " +
                            "('nxt.sx'), ('188.167.90.118'), ('77.103.104.254'), ('allbits.vps.nxtcrypto.org'), ('24.161.110.115'), " +
                            "('90.146.62.91'), ('91.69.121.229'), ('131.151.103.114'), ('82.146.36.253'), ('162.243.80.209'), " +
                            "('89.250.243.200'), ('83.167.48.253'), ('54.88.54.58'), ('105.224.252.58'), ('nxt6.webice.ru'), " +
                            "('178.15.99.67'), ('54.85.132.143'), ('89.250.243.167'), ('85.214.199.215'), ('82.46.194.21'), " +
                            "('83.212.102.247'), ('bitsy06.vps.nxtcrypto.org'), ('nxs2.hanza.co.id'), ('23.238.198.144'), " +
                            "('screenname.vps.nxtcrypto.org'), ('67.212.71.171'), ('54.191.19.147'), ('24.91.143.15'), ('83.212.103.90'), " +
                            "('83.212.97.126'), ('77.249.237.229'), ('67.212.71.173'), ('37.120.168.131'), ('nxt4.webice.ru'), " +
                            "('184.57.30.220'), ('95.24.87.207'), ('162.243.38.34'), ('14.200.16.219'), ('80.137.243.161'), " +
                            "('113.78.102.157'), ('59.37.188.95'), ('nxt8.webice.ru'), ('nxtnode.hopto.org'), ('113.77.25.179'), " +
                            "('178.33.203.157'), ('91.120.22.146'), ('178.150.207.53'), ('77.179.117.226'), ('69.141.139.8'), " +
                            "('vh44.ddns.net'), ('83.212.103.212'), ('95.85.24.151'), ('5.39.76.123'), ('209.126.70.170'), " +
                            "('cubie-solar.mjke.de'), ('106.187.95.232'), ('185.12.44.108'), ('vps9.nxtcrypto.org'), ('nxt5.webice.ru'), " +
                            "('bitsy04.vps.nxtcrypto.org'), ('nxt3.webice.ru'), ('69.122.140.198'), ('54.210.102.134'), " +
                            "('46.109.165.4'), ('panzetti.vps.nxtcrypto.org'), ('80.137.230.115')");
                } else {
                    apply("INSERT INTO peer (address) VALUES " +
                            "('178.150.207.53'), ('192.241.223.132'), ('node9.mynxtcoin.org'), ('node10.mynxtcoin.org'), " +
                            "('node3.mynxtcoin.org'), ('109.87.169.253'), ('nxtnet.fr'), ('50.112.241.97'), " +
                            "('2.84.142.149'), ('bug.airdns.org'), ('83.212.103.14'), ('62.210.131.30'), ('104.131.254.22'), " +
                            "('46.28.111.249')");
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
                if (Constants.isTestnet) {
                    BlockchainProcessorImpl.getInstance().validateAtNextScan();
                }
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
                return;
            default:
                throw new RuntimeException("Database inconsistent with code, probably trying to run older code on newer database");
        }
    }

    private DbVersion() {} //never
}
