package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.DbIterator;
import nxt.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

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
                Logger.logMessage("Database is at level " + (nextUpdate - 1));
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
                stmt.executeUpdate("UPDATE version SET next_update = (SELECT next_update + 1 FROM version)");
                con.commit();
            } catch (SQLException e) {
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
                apply("UPDATE transaction SET block_timestamp = (SELECT timestamp FROM block WHERE block.id = transaction.block_id)");
            case 18:
                apply("ALTER TABLE transaction ALTER COLUMN block_timestamp SET NOT NULL");
            case 19:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS hash BINARY(32)");
            case 20:
                try (DbIterator<? extends Transaction> iterator = Nxt.getBlockchain().getAllTransactions();
                     Connection con = Db.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("UPDATE transaction SET hash = ? WHERE id = ?")) {
                    while (iterator.hasNext()) {
                        Transaction transaction = iterator.next();
                        pstmt.setBytes(1, Convert.parseHexString(transaction.getHash()));
                        pstmt.setLong(2, transaction.getId());
                        pstmt.executeUpdate();
                    }
                    con.commit();
                } catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
                apply(null);
            case 21:
                apply("ALTER TABLE transaction ALTER COLUMN hash SET NOT NULL");
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
                apply("UPDATE block SET total_amount = total_amount * " + Constants.ONE_NXT + " WHERE height <= " + Constants.NQT_BLOCK);
            case 29:
                apply("UPDATE block SET total_fee = total_fee * " + Constants.ONE_NXT + " WHERE height <= " + Constants.NQT_BLOCK);
            case 30:
                apply("UPDATE transaction SET amount = amount * " + Constants.ONE_NXT + " WHERE height <= " + Constants.NQT_BLOCK);
            case 31:
                apply("UPDATE transaction SET fee = fee * " + Constants.ONE_NXT + " WHERE height <= " + Constants.NQT_BLOCK);
            case 32:
                apply(null);
            case 33:
                apply(null);
            case 34:
                apply(null);
            case 35:
                BlockchainProcessorImpl.getInstance().validateAtNextScan();
                apply(null);
            case 36:
                apply("CREATE TABLE IF NOT EXISTS peer (address VARCHAR PRIMARY KEY)");
            case 37:
                if (!Constants.isTestnet) {
                    apply("INSERT INTO peer (address) VALUES " +
                            "('85.25.198.120'), ('185.4.72.115'), ('110.143.228.78'), ('54.72.7.96'), " +
                            "('195.134.66.245'), ('vps4.nxtcrypto.org'), ('80.86.92.139'), ('174.140.167.239'), " +
                            "('162.243.145.83'), ('node14.mynxt.info'), ('node20.mynxt.info'), ('nxtio.org'), " +
                            "('103.224.81.143'), ('54.197.243.235'), ('217.17.88.5'), ('node0.nxtdb.info'), " +
                            "('77.93.202.227'), ('109.87.169.253'), ('67.149.193.205'), ('178.24.158.31'), " +
                            "('95.33.239.194'), ('65.111.181.1'), ('88.168.85.129'), ('108.170.40.2'), " +
                            "('217.117.208.17'), ('85.214.250.44'), ('node15.mynxt.info'), ('wallet.nxtty.com'), " +
                            "('node13.mynxt.info'), ('nxtnet.fr'), ('69.196.157.180'), ('xyzzyx.vps.nxtcrypto.org'), " +
                            "('bitsy02.vps.nxtcrypto.org'), ('node3.mynxt.info'), ('vps9.nxtcrypto.org'), " +
                            "('84.241.44.180'), ('158.195.217.79'), ('89.133.34.109'), ('54.72.136.132'), " +
                            "('node10.mynxt.info'), ('107.170.95.105'), ('node01.nxtcrypto.de'), ('node19.mynxt.info'), " +
                            "('scripterron.noip.me'), ('90.42.133.144'), ('188.120.255.97'), ('node12.mynxt.info'), " +
                            "('198.27.64.207'), ('vps5.nxtcrypto.org'), ('185.12.44.108'), ('54.85.197.150'), " +
                            "('46.173.9.98'), ('nxtcoin.ru'), ('bitsy05.vps.nxtcrypto.org'), ('85.185.142.244'), " +
                            "('vps6.nxtcrypto.org'), ('62.4.23.171'), ('91.69.121.229'), ('88.198.142.92'), " +
                            "('77.56.141.240'), ('192.241.245.96'), ('85.10.201.15'), ('188.226.227.59'), " +
                            "('89.79.241.48'), ('94.26.187.66'), ('pakisnxt.no-ip.org'), ('bitsy01.vps.nxtcrypto.org'), " +
                            "('bitsy04.vps.nxtcrypto.org'), ('90.188.4.177'), ('88.184.64.208'), ('87.230.14.1'), " +
                            "('188.138.88.154'), ('node6.mynxt.info'), ('nrs02.nxtsolaris.info'), ('node17.mynxt.info'), " +
                            "('nxt.homer.ru'), ('vps10.nxtcrypto.org'), ('bitsy03.vps.nxtcrypto.org'), " +
                            "('xeqtorcreed2.vps.nxtcrypto.org'), ('lyynx.vps.nxtcrypto.org'), ('162.220.167.190'), " +
                            "('67.212.71.171'), ('vps7.nxtcrypto.org'), ('144.76.97.106'), ('nxt01.now.im'), " +
                            "('77.179.104.125'), ('2.225.88.10'), ('85.214.222.82'), ('nxtpi.zapto.org'), " +
                            "('vps8.nxtcrypto.org'), ('node8.mynxt.info'), ('nxt.ravensbloodrealms.com'), " +
                            "('node2.mynxt.info'), ('node9.mynxt.info'), ('91.155.101.22'), ('62.21.25.18'), " +
                            "('node1.mynxt.info'), ('node7.mynxt.info'), ('24.21.101.161'), ('nxtportal.org'), " +
                            "('83.212.110.150'), ('87.198.219.221'), ('nxt.sx'), ('87.148.4.4'), ('212.232.49.28'), " +
                            "('192.210.139.55'), ('nxtx.ru'), ('node5.mynxt.info'), ('node16.mynxt.info'), " +
                            "('24.23.120.252'), ('188.226.171.81'), ('allbits.vps.nxtcrypto.org'), ('67.212.71.173'), " +
                            "('107.170.3.62'), ('vps11.nxtcrypto.org'), ('woll-e.net'), ('188.226.219.233'), " +
                            "('37.187.70.29'), ('192.157.244.160'), ('node18.mynxt.info'), ('vps12.nxtcrypto.org'), " +
                            "('54.186.204.166'), ('162.243.167.235'), ('178.210.216.146'), ('cubie-solar.mjke.de'), " +
                            "('84.246.228.249'), ('24.161.110.115'), ('nrs01.nxtsolaris.info'), ('vps3.nxtcrypto.org'), " +
                            "('vps1.nxtcrypto.org'), ('67.212.71.172'), ('node1.nxtdb.info'), ('node4.mynxt.info'), " +
                            "('95.143.216.60'), ('54.201.108.14'), ('199.217.119.33'), ('54.245.255.250'), " +
                            "('93.205.226.113'), ('77.207.118.69'), ('178.122.51.129'), ('xeqtorcreed.vps.nxtcrypto.org'), " +
                            "('node11.mynxt.info'), ('ankhy.no-ip.biz'), ('95.85.42.178'), ('77.58.253.73')");
                } else {
                    apply("INSERT INTO peer (address) VALUES " +
                            "('2.86.61.152'), ('109.87.169.253'), ('83.212.103.14'), ('209.126.73.162'), ('nxtnet.fr'), " +
                            "('83.212.102.194'), ('node10.mynxtcoin.org'), ('83.212.102.193'), ('50.112.241.97'), " +
                            "('144.76.60.38'), ('209.126.73.164'), ('node9.mynxtcoin.org'), ('209.126.71.170'), " +
                            "('node2.mynxtcoin.org'), ('209.126.73.166'), ('192.241.223.132'), ('209.126.75.158'), " +
                            "('node3.mynxtcoin.org'), ('209.126.73.158'), ('83.212.102.244'), ('209.126.73.160'), " +
                            "('209.126.73.152'), ('46.28.111.249'), ('83.212.103.18'), ('209.126.73.168'), " +
                            "('209.126.73.156'), ('bug.airdns.org')");
                }
            case 38:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS full_hash BINARY(32)");
            case 39:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS referenced_transaction_full_hash BINARY(32)");
            case 40:
                try (DbIterator<? extends Transaction> iterator = Nxt.getBlockchain().getAllTransactions();
                     Connection con = Db.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("UPDATE transaction SET full_hash = ? WHERE id = ?")) {
                    while (iterator.hasNext()) {
                        Transaction transaction = iterator.next();
                        pstmt.setBytes(1, Crypto.sha256().digest(transaction.getBytes()));
                        pstmt.setLong(2, transaction.getId());
                        pstmt.executeUpdate();
                    }
                    con.commit();
                } catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
                apply(null);
            case 41:
                apply("ALTER TABLE transaction ALTER COLUMN full_hash SET NOT NULL");
            case 42:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS transaction_full_hash_idx ON transaction (full_hash)");
            case 43:
                apply("UPDATE transaction a SET a.referenced_transaction_full_hash = "
                + "(SELECT full_hash FROM transaction b WHERE b.id = a.referenced_transaction_id)");
            case 44:
                apply(null);
            case 45:
                BlockchainProcessorImpl.getInstance().validateAtNextScan();
                apply(null);
            case 46:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS attachment_bytes VARBINARY");
            case 47:
                try (Connection con = Db.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("UPDATE transaction SET attachment_bytes = ? where db_id = ?");
                     Statement stmt = con.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT * FROM transaction");
                    while (rs.next()) {
                        long dbId = rs.getLong("db_id");
                        Attachment attachment = (Attachment)rs.getObject("attachment");
                        if (attachment != null) {
                            pstmt.setBytes(1, attachment.getBytes());
                        } else {
                            pstmt.setNull(1, Types.VARBINARY);
                        }
                        pstmt.setLong(2, dbId);
                        pstmt.executeUpdate();
                    }
                    con.commit();
                } catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
                apply(null);
            case 48:
                apply("ALTER TABLE transaction DROP COLUMN attachment");
            case 49:
                return;
            default:
                throw new RuntimeException("Database inconsistent with code, probably trying to run older code on newer database");
        }
    }

    private DbVersion() {} //never
}
