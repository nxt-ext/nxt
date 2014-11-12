package nxt;

import nxt.db.DbVersion;
import nxt.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class NxtDbVersion extends DbVersion {

    protected void update(int nextUpdate) throws SQLException {
        switch (nextUpdate) {
            case 1:
                apply("CREATE TABLE IF NOT EXISTS block (db_id IDENTITY, id BIGINT NOT NULL, version INT NOT NULL, "
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
                apply("CREATE TABLE IF NOT EXISTS transaction (db_id IDENTITY, id BIGINT NOT NULL, "
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
                            "('37.59.121.207'), ('nxt.phukhew.com'), ('humanoide.thican.net'), ('108.170.40.4'), ('54.77.63.53'), " +
                            "('89.250.243.200'), ('5.39.216.204'), ('198.211.127.34'), ('woll-e.net'), ('37.187.21.28'), ('54.191.19.147'), " +
                            "('209.222.2.110'), ('46.109.34.36'), ('188.226.245.226'), ('107.170.75.92'), ('212.47.228.0'), " +
                            "('nxt2.achnodes.com'), ('167.206.61.3'), ('81.64.77.101'), ('95.29.201.108'), ('178.162.198.109'), " +
                            "('178.89.42.148'), ('148.251.139.82'), ('85.239.129.252'), ('54.200.114.193'), ('silvanoip.dhcp.biz'), " +
                            "('37.120.168.131'), ('77.88.208.12'), ('58.95.145.117'), ('198.57.198.33'), ('host2.nuagelibre.org'), " +
                            "('dtodorov.asuscomm.com'), ('178.15.99.67'), ('5.150.195.208'), ('jnxt.org'), ('lan.wow64.net'), " +
                            "('198.27.64.207'), ('54.83.4.11'), ('88.163.78.131'), ('nxtpi.zapto.org'), ('188.226.197.131'), " +
                            "('node7.mynxtcoin.org'), ('80.86.92.139'), ('184.164.72.177'), ('nxt.hofhom.nl'), ('85.25.198.120'), " +
                            "('91.98.139.194'), ('46.109.48.18'), ('62.194.6.163'), ('96.240.128.221'), ('5.196.1.215'), ('87.139.122.48'), " +
                            "('210.188.36.5'), ('89.72.57.246'), ('198.199.95.15'), ('191.238.101.73'), ('128.199.160.141'), " +
                            "('85.214.222.82'), ('82.165.145.37'), ('85.84.67.234'), ('54.214.232.96'), ('91.121.150.75'), " +
                            "('54.208.32.151'), ('178.33.203.157'), ('176.94.115.161'), ('miasik.no-ip.org'), ('nxt1.thican.net'), " +
                            "('109.106.48.104'), ('23.89.192.151'), ('195.154.127.172'), ('ct.flipflop.mooo.com'), ('nxt1.achnodes.com'), " +
                            "('nxt01.now.im'), ('78.46.92.78'), ('nxs1.hanza.co.id'), ('91.120.22.146'), ('128.199.228.211'), " +
                            "('23.88.246.117'), ('162.243.122.251'), ('178.32.221.58'), ('217.26.24.27'), ('95.85.31.45'), " +
                            "('109.230.224.65'), ('89.212.19.49'), ('46.4.212.230'), ('92.222.168.75'), ('181.165.178.28'), " +
                            "('79.30.180.223'), ('178.62.50.75'), ('212.18.225.173'), ('37.59.115.204'), ('80.86.92.66'), " +
                            "('95.24.83.220'), ('gunka.szn.dk'), ('80.153.101.190'), ('nxt.cybermailing.com'), ('54.200.116.75'), " +
                            "('84.128.162.237'), ('81.23.22.150'), ('54.213.222.141'), ('185.21.192.9'), ('espo.no-ip.biz'), " +
                            "('174.140.167.239'), ('nacho.damnserver.com'), ('92.222.0.105'), ('nxtx.ru'), ('188.226.206.41'), " +
                            "('107.170.3.62'), ('211.149.213.86'), ('91.69.121.229'), ('176.9.0.19'), ('188.226.139.71'), " +
                            "('nxt.shscrypto.net'), ('46.246.44.104'), ('87.98.163.78'), ('155.133.19.33'), ('195.154.67.124'), " +
                            "('106.187.95.232'), ('2.225.88.10'), ('93.103.20.35'), ('168.63.232.16'), ('23.102.0.45'), ('46.28.111.249'), " +
                            "('46.173.9.98'), ('178.62.240.203'), ('54.194.212.248'), ('89.250.240.60'), ('phalanx149.ddns.net'), " +
                            "('37.139.6.166'), ('178.150.207.53'), ('scripterron.dyndns.biz'), ('nxtnode.hopto.org'), ('66.30.204.105'), " +
                            "('nxt.alkeron.com'), ('96.251.124.95'), ('5.45.98.105'), ('nxt.sx'), ('76.176.198.6'), ('192.3.158.120'), " +
                            "('83.217.195.132'), ('107.170.164.129'), ('54.68.87.225'), ('sluni.szn.dk'), ('212.47.237.7'), " +
                            "('nxt.scryptmh.eu'), ('pakisnxt.no-ip.org'), ('85.214.155.115'), ('174.140.166.124'), ('190.10.9.166'), " +
                            "('dorcsforge.cloudapp.net'), ('84.242.91.139'), ('54.187.153.45'), ('81.2.216.179'), ('84.133.75.209'), " +
                            "('81.169.150.141'), ('85.10.201.15'), ('cryonet.de'), ('185.12.44.108'), ('54.200.180.57'), ('213.46.57.77'), " +
                            "('46.237.8.30'), ('37.59.115.207'), ('67.212.71.173'), ('128.199.189.226'), ('node5.mynxtcoin.org'), " +
                            "('188.35.156.10'), ('node6.mynxtcoin.org'), ('67.212.71.171'), ('84.200.206.220'), ('node1.nxt.thican.net'), " +
                            "('oldminersnownodes.ddns.net'), ('90.153.106.133'), ('87.139.122.157'), ('beor.homeip.net'), " +
                            "('192.157.226.151'), ('node4.mynxtcoin.org'), ('node0.forgenxt.com'), ('63.165.243.112'), ('23.88.104.225')");
                } else {
                    apply("INSERT INTO peer (address) VALUES " +
                            "('nxt.scryptmh.eu'), ('54.186.98.117'), ('178.150.207.53'), ('192.241.223.132'), ('node9.mynxtcoin.org'), " +
                            "('node10.mynxtcoin.org'), ('node3.mynxtcoin.org'), ('109.87.169.253'), ('nxtnet.fr'), ('50.112.241.97'), " +
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
                apply("CREATE TABLE IF NOT EXISTS alias (db_id IDENTITY, id BIGINT NOT NULL, "
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
                apply("CREATE TABLE IF NOT EXISTS alias_offer (db_id IDENTITY, id BIGINT NOT NULL, "
                        + "price BIGINT NOT NULL, buyer_id BIGINT, "
                        + "height INT NOT NULL, latest BOOLEAN DEFAULT TRUE NOT NULL)");
            case 76:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS alias_offer_id_height_idx ON alias_offer (id, height DESC)");
            case 77:
                apply("CREATE TABLE IF NOT EXISTS asset (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "name VARCHAR NOT NULL, description VARCHAR, quantity BIGINT NOT NULL, decimals TINYINT NOT NULL, "
                        + "height INT NOT NULL)");
            case 78:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS asset_id_idx ON asset (id)");
            case 79:
                apply("CREATE INDEX IF NOT EXISTS asset_account_id_idx ON asset (account_id)");
            case 80:
                apply("CREATE TABLE IF NOT EXISTS trade (db_id IDENTITY, asset_id BIGINT NOT NULL, block_id BIGINT NOT NULL, "
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
                apply("CREATE TABLE IF NOT EXISTS ask_order (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
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
                apply("CREATE TABLE IF NOT EXISTS bid_order (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
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
                apply("CREATE TABLE IF NOT EXISTS goods (db_id IDENTITY, id BIGINT NOT NULL, seller_id BIGINT NOT NULL, "
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
                apply("CREATE TABLE IF NOT EXISTS purchase (db_id IDENTITY, id BIGINT NOT NULL, buyer_id BIGINT NOT NULL, "
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
                apply("CREATE TABLE IF NOT EXISTS account (db_id IDENTITY, id BIGINT NOT NULL, creation_height INT NOT NULL, "
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
                apply("CREATE TABLE IF NOT EXISTS account_asset (db_id IDENTITY, account_id BIGINT NOT NULL, "
                        + "asset_id BIGINT NOT NULL, quantity BIGINT NOT NULL, unconfirmed_quantity BIGINT NOT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 106:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_asset_id_height_idx ON account_asset (account_id, asset_id, height DESC)");
            case 107:
                apply("CREATE TABLE IF NOT EXISTS account_guaranteed_balance (db_id IDENTITY, account_id BIGINT NOT NULL, "
                        + "additions BIGINT NOT NULL, height INT NOT NULL)");
            case 108:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_guaranteed_balance_id_height_idx ON account_guaranteed_balance "
                        + "(account_id, height DESC)");
            case 109:
                apply("CREATE TABLE IF NOT EXISTS purchase_feedback (db_id IDENTITY, id BIGINT NOT NULL, feedback_data VARBINARY NOT NULL, "
                        + "feedback_nonce BINARY(32) NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 110:
                apply("CREATE INDEX IF NOT EXISTS purchase_feedback_id_height_idx ON purchase_feedback (id, height DESC)");
            case 111:
                apply("CREATE TABLE IF NOT EXISTS purchase_public_feedback (db_id IDENTITY, id BIGINT NOT NULL, public_feedback "
                        + "VARCHAR NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 112:
                apply("CREATE INDEX IF NOT EXISTS purchase_public_feedback_id_height_idx ON purchase_public_feedback (id, height DESC)");
            case 113:
                apply("CREATE TABLE IF NOT EXISTS unconfirmed_transaction (db_id IDENTITY, id BIGINT NOT NULL, expiration INT NOT NULL, "
                        + "transaction_height INT NOT NULL, fee_per_byte BIGINT NOT NULL, timestamp INT NOT NULL, "
                        + "transaction_bytes VARBINARY NOT NULL, height INT NOT NULL)");
            case 114:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS unconfirmed_transaction_id_idx ON unconfirmed_transaction (id)");
            case 115:
                apply("CREATE INDEX IF NOT EXISTS unconfirmed_transaction_height_fee_timestamp_idx ON unconfirmed_transaction "
                        + "(transaction_height ASC, fee_per_byte DESC, timestamp ASC)");
            case 116:
                apply("CREATE TABLE IF NOT EXISTS asset_transfer (db_id IDENTITY, id BIGINT NOT NULL, asset_id BIGINT NOT NULL, "
                        + "sender_id BIGINT NOT NULL, recipient_id BIGINT NOT NULL, quantity BIGINT NOT NULL, timestamp INT NOT NULL, "
                        + "height INT NOT NULL)");
            case 117:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS asset_transfer_id_idx ON asset_transfer (id)");
            case 118:
                apply("CREATE INDEX IF NOT EXISTS asset_transfer_asset_id_idx ON asset_transfer (asset_id, height DESC)");
            case 119:
                apply("CREATE INDEX IF NOT EXISTS asset_transfer_sender_id_idx ON asset_transfer (sender_id, height DESC)");
            case 120:
                apply("CREATE INDEX IF NOT EXISTS asset_transfer_recipient_id_idx ON asset_transfer (recipient_id, height DESC)");
            case 121:
                apply(null);
            case 122:
                apply("CREATE INDEX IF NOT EXISTS account_asset_quantity_idx ON account_asset (quantity DESC)");
            case 123:
                apply("CREATE INDEX IF NOT EXISTS purchase_timestamp_idx ON purchase (timestamp DESC, id)");
            case 124:
                apply("CREATE INDEX IF NOT EXISTS ask_order_creation_idx ON ask_order (creation_height DESC)");
            case 125:
                apply("CREATE INDEX IF NOT EXISTS bid_order_creation_idx ON bid_order (creation_height DESC)");
            case 126:
                apply(null);
            case 127:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_timestamp_idx ON block (timestamp DESC)");
            case 128:
                BlockchainProcessorImpl.getInstance().forceScanAtStart();
                apply(null);
            case 129:
                apply("CREATE TABLE IF NOT EXISTS currency (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "name VARCHAR NOT NULL, name_lower VARCHAR AS LOWER (name) NOT NULL, code VARCHAR NOT NULL, "
                        + "description VARCHAR, type INT NOT NULL, current_supply BIGINT NOT NULL, reserve_supply BIGINT NOT NULL, max_supply BIGINT NOT NULL, "
                        + "issuance_height INT NOT NULL, min_reserve_per_unit_nqt BIGINT NOT NULL, min_difficulty TINYINT NOT NULL, "
                        + "max_difficulty TINYINT NOT NULL, ruleset TINYINT NOT NULL, algorithm TINYINT NOT NULL, "
                        + "current_reserve_per_unit_nqt BIGINT NOT NULL, decimals TINYINT NOT NULL DEFAULT 0,"
                        + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 130:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS currency_id_height_idx ON currency (id, height DESC)");
            case 131:
                apply("CREATE INDEX IF NOT EXISTS currency_account_id_idx ON currency (account_id)");
            case 132:
                apply("CREATE TABLE IF NOT EXISTS account_currency (db_id IDENTITY, account_id BIGINT NOT NULL, "
                        + "currency_id BIGINT NOT NULL, units BIGINT NOT NULL, unconfirmed_units BIGINT NOT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 133:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_currency_id_height_idx ON account_currency (account_id, currency_id, height DESC)");
            case 134:
                apply("CREATE TABLE IF NOT EXISTS currency_founder (db_id IDENTITY, currency_id BIGINT NOT NULL, "
                        + "account_id BIGINT NOT NULL, amount BIGINT NOT NULL, "
                        + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 135:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS currency_founder_currency_id_idx ON currency_founder (currency_id, account_id, height DESC)");
            case 136:
                apply("CREATE TABLE IF NOT EXISTS currency_mint (db_id IDENTITY, currency_id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "counter BIGINT NOT NULL, submission_height INT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 137:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS currency_mint_currency_id_account_id_idx ON currency_mint (currency_id, account_id, height DESC)");
            case 138:
                apply("CREATE TABLE IF NOT EXISTS buy_offer (db_id INT IDENTITY, id BIGINT NOT NULL, currency_id BIGINT NOT NULL, account_id BIGINT NOT NULL,"
                        + "rate BIGINT NOT NULL, unit_limit BIGINT NOT NULL, supply BIGINT NOT NULL, expiration_height INT NOT NULL,"
                        + "creation_height INT NOT NULL, transaction_index SMALLINT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 139:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS buy_offer_id_idx ON buy_offer (id, height DESC)");
            case 140:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS buy_offer_currency_id_account_id_idx ON buy_offer (currency_id, account_id, height DESC)");
            case 141:
                apply("CREATE TABLE IF NOT EXISTS sell_offer (db_id INT IDENTITY, id BIGINT NOT NULL, currency_id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "rate BIGINT NOT NULL, unit_limit BIGINT NOT NULL, supply BIGINT NOT NULL, expiration_height INT NOT NULL, "
                        + "creation_height INT NOT NULL, transaction_index SMALLINT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 142:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS sell_offer_id_idx ON sell_offer (id, height DESC)");
            case 143:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS sell_offer_currency_id_account_id_idx ON sell_offer (currency_id, account_id, height DESC)");
            case 144:
                apply("CREATE TABLE IF NOT EXISTS exchange (db_id INT IDENTITY, transaction_id BIGINT NOT NULL, currency_id BIGINT NOT NULL, block_id BIGINT NOT NULL, "
                        + "offer_id BIGINT NOT NULL, seller_id BIGINT NOT NULL, "
                        + "buyer_id BIGINT NOT NULL, units BIGINT NOT NULL, "
                        + "rate BIGINT NOT NULL, timestamp INT NOT NULL, height INT NOT NULL)");
            case 145:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS exchange_offer_idx ON exchange (transaction_id, offer_id)");
            case 146:
                apply("CREATE INDEX IF NOT EXISTS exchange_currency_id_idx ON exchange (currency_id, height DESC)");
            case 147:
                apply("CREATE INDEX IF NOT EXISTS exchange_seller_id_idx ON exchange (seller_id, height DESC)");
            case 148:
                apply("CREATE INDEX IF NOT EXISTS exchange_buyer_id_idx ON exchange (buyer_id, height DESC)");
            case 149:
                apply("CREATE TABLE IF NOT EXISTS currency_transfer (db_id INT IDENTITY, id BIGINT NOT NULL, currency_id BIGINT NOT NULL, "
                        + "sender_id BIGINT NOT NULL, recipient_id BIGINT NOT NULL, units BIGINT NOT NULL, timestamp INT NOT NULL, "
                        + "height INT NOT NULL)");
            case 150:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS currency_transfer_id_idx ON currency_transfer (id)");
            case 151:
                apply("CREATE INDEX IF NOT EXISTS currency_transfer_currency_id_idx ON currency_transfer (currency_id, height DESC)");
            case 152:
                apply("CREATE INDEX IF NOT EXISTS currency_transfer_sender_id_idx ON currency_transfer (sender_id, height DESC)");
            case 153:
                apply("CREATE INDEX IF NOT EXISTS currency_transfer_recipient_id_idx ON currency_transfer (recipient_id, height DESC)");
            case 154:
                apply("CREATE INDEX IF NOT EXISTS account_currency_units_idx ON account_currency (units DESC)");
            case 155:
                apply("CREATE INDEX IF NOT EXISTS currency_name_idx ON currency (name_lower, height DESC)");
            case 156:
                apply("CREATE INDEX IF NOT EXISTS currency_code_idx ON currency (code, height DESC)");
            case 157:
                apply("CREATE INDEX IF NOT EXISTS buy_offer_rate_height_idx ON buy_offer (rate DESC, creation_height ASC)");
            case 158:
                apply("CREATE INDEX IF NOT EXISTS sell_offer_rate_height_idx ON sell_offer (rate ASC, creation_height ASC)");
            case 159:
                apply("ALTER TABLE account ADD COLUMN IF NOT EXISTS message_pattern_regex VARCHAR");
            case 160:
                apply("ALTER TABLE account ADD COLUMN IF NOT EXISTS message_pattern_flags INT");
            case 161:
                apply("DROP INDEX IF EXISTS unconfirmed_transaction_height_fee_timestamp_idx");
            case 162:
                apply("ALTER TABLE unconfirmed_transaction DROP COLUMN IF EXISTS timestamp");
            case 163:
                apply("ALTER TABLE unconfirmed_transaction ADD COLUMN IF NOT EXISTS arrival_timestamp BIGINT NOT NULL DEFAULT 0");
            case 164:
                apply("CREATE INDEX IF NOT EXISTS unconfirmed_transaction_height_fee_timestamp_idx ON unconfirmed_transaction "
                        + "(transaction_height ASC, fee_per_byte DESC, arrival_timestamp ASC)");
            case 165:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS transaction_index SMALLINT");
            case 166:
                Logger.logMessage("Will update transaction_index column...");
                try (Connection con = Db.db.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction ORDER BY height, id FOR UPDATE",
                             ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {
                    try (ResultSet rs = pstmt.executeQuery()) {
                        int height = 0;
                        short index = 0;
                        while (rs.next()) {
                            int nextHeight = rs.getInt("height");
                            if (nextHeight != height) {
                                index = 0;
                                if (height / 5000 != nextHeight / 5000) {
                                    Logger.logMessage("Processed " + (nextHeight / 5000) * 5000 + " blocks");
                                }
                                height = nextHeight;
                            }
                            rs.updateShort("transaction_index", index++);
                            rs.updateRow();
                        }
                    }
                }
                apply(null);
            case 167:
                apply("ALTER TABLE transaction ALTER COLUMN transaction_index SET NOT NULL");
            case 168:
                apply("ALTER TABLE ask_order ADD COLUMN IF NOT EXISTS transaction_index SMALLINT");
            case 169:
                apply("UPDATE ask_order SET transaction_index = (SELECT transaction_index FROM transaction WHERE transaction.id = ask_order.id)");
            case 170:
                apply("ALTER TABLE ask_order ALTER COLUMN transaction_index SET NOT NULL");
            case 171:
                apply("ALTER TABLE bid_order ADD COLUMN IF NOT EXISTS transaction_index SMALLINT");
            case 172:
                apply("UPDATE bid_order SET transaction_index = (SELECT transaction_index FROM transaction WHERE transaction.id = bid_order.id)");
            case 173:
                apply("ALTER TABLE bid_order ALTER COLUMN transaction_index SET NOT NULL");
            case 174:
                return;
            default:
                throw new RuntimeException("Blockchain database inconsistent with code, probably trying to run older code on newer database");
        }
    }

}
