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
                            "('node11.nxtbase.com'), ('46.165.208.108'), ('node17.mynxt.info'), ('node2.mynxt.info'), " +
                            "('node56.nxtbase.com'), ('node15.mynxt.info'), ('node19.mynxt.info'), ('node9.nxtbase.com'), " +
                            "('node53.nxtbase.com'), ('node52.nxtbase.com'), ('node6.mynxt.info'), ('node4.mynxt.info'), " +
                            "('node12.mynxt.info'), ('node68.nxtbase.com'), ('node38.nxtbase.com'), ('node48.nxtbase.com'), " +
                            "('node18.mynxt.info'), ('109.194.162.16'), ('nrs02.nxtsolaris.info'), ('162.243.145.83'), " +
                            "('185.12.44.108'), ('80.86.92.139'), ('node8.mynxt.info'), ('node72.nxtbase.com'), " +
                            "('188.226.221.215'), ('69.196.157.180'), ('83.212.110.150'), ('node1.mynxt.info'), " +
                            "('178.79.133.99'), ('87.230.14.1'), ('node6.nxtbase.com'), ('174.140.167.239'), " +
                            "('62.57.125.237'), ('84.133.73.87'), ('109.195.50.132'), ('80.86.92.70'), ('62.4.23.171'), " +
                            "('node13.mynxt.info'), ('217.81.153.75'), ('24.161.110.115'), ('nxt.sx'), ('node7.mynxt.info'), " +
                            "('nxtbase.de'), ('85.25.199.208'), ('67.212.71.171'), ('woll-e.net'), ('nxtcoin.fr'), " +
                            "('node80.nxtbase.com'), ('node69.nxtbase.com'), ('wallet.nxtty.com'), ('node61.nxtbase.com'), " +
                            "('37.59.47.155'), ('node71.nxtbase.com'), ('109.87.169.253'), ('node66.nxtbase.com'), " +
                            "('node01.nxtcrypto.de'), ('node65.nxtbase.com'), ('162.243.213.190'), ('115.28.2.240'), " +
                            "('85.13.123.16'), ('85.10.201.15'), ('77.56.141.240'), ('node14.mynxt.info'), ('79.220.80.245'), " +
                            "('nxtcoin.ru'), ('95.85.8.113'), ('89.79.241.48'), ('188.226.219.233'), ('miasik.no-ip.org'), " +
                            "('89.133.34.109'), ('146.185.168.129'), ('84.148.107.117'), ('ingalls.io'), ('node11.mynxt.info'), " +
                            "('82.225.188.204'), ('95.24.12.91'), ('90.188.4.177'), ('node5.mynxt.info'), ('101.128.209.197'), " +
                            "('67.212.71.172'), ('67.212.71.173'), ('84.246.228.249'), ('107.170.20.50'), ('146.52.46.108'), " +
                            "('106.186.28.72'), ('31.40.112.4'), ('node1.nxtdb.info'), ('112.124.34.241'), ('178.162.198.45'), " +
                            "('pakisnxt.no-ip.org'), ('server.heuvelrugict.nl:7874'), ('nxtx.ru'), ('77.58.253.73'), " +
                            "('185.14.185.19'), ('87.198.219.221'), ('122.148.248.178'), ('114.215.171.11'), ('146.52.229.93'), " +
                            "('79.127.207.58'), ('90.52.50.32'), ('nxtnode.eu'), ('212.146.138.100'), ('85.214.222.82'), " +
                            "('185.4.72.115'), ('136.169.57.54'), ('nrs01.nxtsolaris.info'), ('boxy.ingalls.io'), ('198.57.198.33'), " +
                            "('84.168.112.18'), ('144.76.97.106'), ('188.138.88.154'), ('217.73.196.11'), ('85.185.142.244'), " +
                            "('188.86.119.93'), ('151.70.2.149'), ('node9.mynxt.info'), ('31.22.237.5'), ('23.94.5.32'), " +
                            "('85.214.147.237'), ('node3.mynxt.info'), ('213.21.225.23'), ('91.69.121.229'), ('91.155.101.22'), " +
                            "('162.220.167.190'), ('85.214.250.44'), ('217.81.140.252'), ('84.173.100.172'), ('37.187.70.29'), " +
                            "('95.188.226.246'), ('95.85.60.102'), ('84.82.190.244'), ('2.225.88.10'), ('65.111.181.1'), " +
                            "('95.85.42.178'), ('221.223.234.7'), ('nxt.abuminogs.com'), ('89.250.243.200'), ('54.186.204.166'), " +
                            "('nxtnet.fr'), ('nxt01.now.im'), ('88.198.104.84'), ('nxt.homer.ru'), ('nxtalk.ru'), ('88.198.142.92'), " +
                            "('63.237.225.138'), ('217.17.88.5'), ('83.212.102.193'), ('54.72.7.96'), ('node0.nxtdb.info'), " +
                            "('162.243.213.236'), ('77.93.202.227'), ('107.150.11.19'), ('151.236.29.228'), " +
                            "('nxt.ravensbloodrealms.com'), ('110.143.228.78'), ('188.226.171.81'), ('84.241.44.180'), " +
                            "('198.27.80.197'), ('91.153.23.165'), ('81.82.248.205'), ('84.133.68.170'), ('162.219.3.67'), " +
                            "('37.139.10.249'), ('54.201.107.122'), ('84.144.206.125'), ('90.52.165.169'), ('54.72.136.132'), " +
                            "('188.226.183.138'), ('162.242.18.45'), ('95.248.244.30'), ('96.227.216.203'), ('node10.mynxt.info'), " +
                            "('195.3.205.202'), ('59.188.1.133'), ('nxt.pucchiwerk.eu'), ('78.235.55.95'), ('162.218.89.56'), " +
                            "('162.243.6.75'), ('nxt.olxp.in'), ('crypto.homeip.net'), ('46.173.9.98'), ('85.10.202.109'), " +
                            "('107.170.3.62'), ('188.226.169.81'), ('nxtio.org'), ('211.140.186.62'), ('111.192.251.197'), " +
                            "('87.98.163.78'), ('cryptohub.com'), ('46.149.84.141'), ('198.27.64.207'), ('212.85.38.25'), " +
                            "('5.45.98.105'), ('217.252.212.50'), ('95.24.93.5'), ('node75.nxtbase.com'), ('94.26.187.66'), " +
                            "('144.76.17.83'), ('82.241.71.230'), ('46.16.113.70'), ('79.175.53.134'), ('88.79.173.189'), " +
                            "('78.202.156.78'), ('178.162.39.12'), ('217.70.39.132'), ('antoined.zapto.org:8874'), " +
                            "('54.220.169.74.mynxt.info'), ('98.109.164.39'), ('tomeczech.cz'), ('192.34.61.121'), " +
                            "('208.88.124.30'), ('212.85.37.150'), ('188.120.255.97'), ('130.204.2.143'), ('105.224.254.167'), " +
                            "('178.210.216.146'), ('81.66.138.167'), ('217.26.24.27'), ('80.86.83.79'), ('node1.nxtbase.com'), " +
                            "('91.183.55.237'), ('node77.nxtbase.com'), ('node2.nxtbase.com'), ('node64.nxtbase.com'), " +
                            "('node62.nxtbase.com'), ('74.84.70.66'), ('95.85.46.177'), ('180.129.41.212'), ('node10.nxtbase.com'), " +
                            "('84.242.91.139'), ('217.81.138.154'), ('node74.nxtbase.com'), ('217.252.219.147'), ('37.28.187.117'), " +
                            "('93.215.129.243'), ('node73.nxtbase.com'), ('208.85.4.102'), ('95.55.135.7'), ('93.215.140.167'), " +
                            "('94.223.109.134'), ('node5.nxtbase.com'), ('gayka.no-ip.info'), ('37.193.89.221'), " +
                            "('node79.nxtbase.com'), ('212.85.38.103'), ('105.229.167.58'), ('217.81.147.164'), ('72.184.226.255'), " +
                            "('94.69.251.50'), ('24.23.120.252'), ('24.20.21.143'), ('node3.nxtbase.com'), ('162.217.204.46'), " +
                            "('80.171.107.16'), ('217.81.152.63'), ('217.81.165.176'), ('54.206.92.0'), ('222.128.136.235'), " +
                            "('93.213.105.254'), ('46.28.111.249'), ('80.248.53.214'), ('222.128.140.87'), ('217.252.215.63'), " +
                            "('84.168.113.32'), ('24.132.70.212'), ('195.182.1.192'), ('202.89.150.189'), ('85.25.198.120'), " +
                            "('142.244.213.113'), ('217.81.146.144'), ('186.90.251.60'), ('88.189.156.162'), ('95.158.16.228'), " +
                            "('125.183.74.159'), ('31.150.232.47'), ('vtc.quickenjoy.com'), ('222.128.130.219'), ('93.213.114.245'), " +
                            "('84.133.68.15'), ('95.24.67.205'), ('54.199.191.81'), ('178.122.228.95'), ('80.171.109.254'), " +
                            "('84.173.109.23'), ('178.27.60.2'), ('87.142.196.19'), ('93.193.65.156'), ('83.240.14.35'), " +
                            "('95.188.254.224'), ('141.219.237.205'), ('79.219.5.73'), ('95.188.252.198'), ('95.55.20.75'), " +
                            "('54.246.93.148'), ('141.0.170.101'), ('131.151.102.242'), ('87.142.207.251'), ('31.19.188.145'), " +
                            "('78.54.148.76'), ('84.144.221.176'), ('93.213.107.32'), ('105.229.187.239'), ('84.144.203.91'), " +
                            "('87.205.93.58'), ('217.252.223.87'), ('95.46.84.117'), ('62.109.34.205'), ('82.173.178.169'), " +
                            "('95.188.231.65'), ('178.158.19.243'), ('217.84.218.168'), ('128.8.195.13'), ('92.129.236.202'), " +
                            "('76.164.201.88'), ('91.122.106.91'), ('79.220.92.236'), ('216.169.138.156'), ('171.104.183.76'), " +
                            "('95.24.95.71'), ('nxt.coinmine.pl'), ('105.224.254.232'), ('178.158.18.164'), ('91.122.106.84'), " +
                            "('91.122.106.85'), ('105.229.172.182'), ('84.168.116.217'), ('84.133.65.223'), ('78.54.143.167'), " +
                            "('120.82.79.203'), ('171.121.241.9'), ('178.158.21.57'), ('88.204.147.38'), ('93.215.134.86'), " +
                            "('178.122.168.235'), ('217.81.151.203'), ('46.59.215.230'), ('219.111.71.234'), ('162.217.202.162'), " +
                            "('105.224.255.253'), ('fxzonxt.zapto.org'), ('77.106.70.104'), ('42.60.31.119'), ('93.215.132.160'), " +
                            "('217.81.146.82'), ('188.62.174.36'), ('221.223.236.200'), ('158.195.217.79'), ('84.144.203.18'), " +
                            "('85.177.17.225'), ('10.186.10.36'), ('61.131.37.210'), ('node7.nxtbase.com'), ('84.133.71.126'), " +
                            "('87.142.199.55'), ('95.188.251.172'), ('node70.nxtbase.com'), ('node67.nxtbase.com'), ('92.129.121.33'), " +
                            "('95.249.246.230'), ('95.188.236.195'), ('192.241.245.96'), ('75.127.77.161'), ('93.215.152.156'), " +
                            "('23.88.104.217'), ('217.71.41.70'), ('112.199.218.55'), ('node16.mynxt.info'), ('84.144.196.64'), " +
                            "('node78.nxtbase.com'), ('node4.nxtbase.com'), ('128.70.62.178'), ('93.205.234.91'), ('162.243.93.105'), " +
                            "('node76.nxtbase.com'), ('93.213.126.14'), ('84.168.109.224'), ('159.205.190.0'), ('70.74.182.14'), " +
                            "('77.72.149.109'), ('169.230.180.51'), ('95.188.244.235'), ('216.131.75.219'), ('95.188.250.97'), " +
                            "('217.117.208.17'), ('vps1.nxtcrypto.org'), ('84.144.212.157'), ('xyzzyx.vps.nxtcrypto.org'), " +
                            "('vps2.nxtcrypto.org'), ('vps5.nxtcrypto.org'), ('vps10.nxtcrypto.org'), ('vps4.nxtcrypto.org'), " +
                            "('vps11.nxtcrypto.org'), ('bitsy02.vps.nxtcrypto.org'), ('vps6.nxtcrypto.org'), ('vps7.nxtcrypto.org'), " +
                            "('vps3.nxtcrypto.org'), ('lyynx.vps.nxtcrypto.org'), ('bitsy01.vps.nxtcrypto.org'), " +
                            "('allbits.vps.nxtcrypto.org'), ('xeqtorcreed2.vps.nxtcrypto.org'), ('vps8.nxtcrypto.org'), " +
                            "('bitsy03.vps.nxtcrypto.org'), ('bitsy05.vps.nxtcrypto.org'), ('bitsy04.vps.nxtcrypto.org'), " +
                            "('173.246.41.38')");
                } else {
                    apply("INSERT INTO peer (address) VALUES " +
                            "('node10.mynxtcoin.org'), ('79.129.177.7'), ('5.248.109.176'), ('83.212.110.150'), " +
                            "('209.126.73.158'), ('162.243.212.91'), ('node2.mynxtcoin.org'), ('209.126.73.156'), " +
                            "('tn01.nxtsolaris.info'), ('209.126.71.170'), ('192.241.223.132'), ('109.87.169.253'), " +
                            "('83.212.102.193'), ('nxtnet.fr'), ('46.28.111.249'), ('node9.mynxtcoin.org'), " +
                            "('83.212.103.14'), ('node3.mynxtcoin.org'), ('node4.mynxtcoin.org'), ('203.107.212.7'), " +
                            "('83.212.103.18'), ('bug.airdns.org')");
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
                return;
            default:
                throw new RuntimeException("Database inconsistent with code, probably trying to run older code on newer database");
        }
    }

    private DbVersion() {} //never
}
