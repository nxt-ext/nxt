/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
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

/**
 * Represents a single shuffling participant
 */
package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.VersionedEntityDbTable;
import nxt.util.Listener;
import nxt.util.Listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class ShufflingParticipant {

    public enum State {
        REGISTERED((byte)0),
        PROCESSED((byte)1),
        VERIFIED((byte)2);

        private final byte code;

        State(byte code) {
            this.code = code;
        }

        static State get(byte code) {
            for (State state : State.values()) {
                if (state.code == code) {
                    return state;
                }
            }
            throw new IllegalArgumentException("No matching state for " + code);
        }

        public byte getCode() {
            return code;
        }
    }

    public enum Event {
        PARTICIPANT_ADDED, RECIPIENT_ADDED
    }

    private static final Listeners<ShufflingParticipant, Event> listeners = new Listeners<>();

    private static final DbKey.LinkKeyFactory<ShufflingParticipant> shufflingParticipantDbKeyFactory =
            new DbKey.LinkKeyFactory<ShufflingParticipant>("shuffling_id", "account_id") {

        @Override
        public DbKey newKey(ShufflingParticipant transfer) {
            return transfer.dbKey;
        }

    };

    //TODO: trim participants 1440 blocks after shuffle completion?
    private static final VersionedEntityDbTable<ShufflingParticipant> shufflingParticipantTable = new VersionedEntityDbTable<ShufflingParticipant>("shuffling_participant", shufflingParticipantDbKeyFactory) {

        @Override
        protected ShufflingParticipant load(Connection con, ResultSet rs) throws SQLException {
            return new ShufflingParticipant(rs);
        }

        @Override
        protected void save(Connection con, ShufflingParticipant participant) throws SQLException {
            participant.save(con);
        }

    };

    public static int getCount(long shufflingId) {
        return shufflingParticipantTable.getCount(new DbClause.LongClause("shuffling_id", shufflingId));
    }

    public static boolean addListener(Listener<ShufflingParticipant> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<ShufflingParticipant> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static DbIterator<ShufflingParticipant> getParticipants(long shufflingId) {
        return shufflingParticipantTable.getManyBy(new DbClause.LongClause("shuffling_id", shufflingId), 0, -1);
    }

    public static ShufflingParticipant getParticipant(long shufflingId, long accountId) {
        return shufflingParticipantTable.get(shufflingParticipantDbKeyFactory.newKey(shufflingId, accountId));
    }

    static void addParticipant(long shufflingId, long accountId) {
        ShufflingParticipant participant = new ShufflingParticipant(shufflingId, accountId);
        shufflingParticipantTable.insert(participant);
        listeners.notify(participant, Event.PARTICIPANT_ADDED);
    }

    static void updateData(long shufflingId, long accountId, byte[] data) {
        ShufflingParticipant participant = ShufflingParticipant.getParticipant(shufflingId, accountId);
        participant.setData(data);
    }

    static void init() {}

    private final long shufflingId;
    private final long accountId; // sender account
    private final DbKey dbKey;

    private long nextAccountId; // pointer to the next shuffling participant updated during registration
    private byte[] recipientPublicKey; // decrypted recipient public key (shuffled) updated after the shuffling process is complete
    private State state; // tracks the state of the participant in the process
    private byte[] data; // encrypted data saved as intermediate result in the shuffling process

    public ShufflingParticipant(long shufflingId, long accountId) {
        this.shufflingId = shufflingId;
        this.accountId = accountId;
        this.dbKey = shufflingParticipantDbKeyFactory.newKey(shufflingId, accountId);
        this.state = State.REGISTERED;
        this.data = new byte[] {};
    }

    private ShufflingParticipant(ResultSet rs) throws SQLException {
        this.shufflingId = rs.getLong("shuffling_id");
        this.accountId = rs.getLong("account_id");
        this.dbKey = shufflingParticipantDbKeyFactory.newKey(shufflingId, accountId);
        this.nextAccountId = rs.getLong("next_account_id");
        this.recipientPublicKey = rs.getBytes("recipient_public_key");
        this.state = State.get(rs.getByte("state"));
        this.data = rs.getBytes("data");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO shuffling_participant (shuffling_id, "
                + "account_id, next_account_id, recipient_public_key, state, data, height, latest) "
                + "KEY (shuffling_id, account_id, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.shufflingId);
            pstmt.setLong(++i, this.accountId);
            DbUtils.setLongZeroToNull(pstmt, ++i, this.nextAccountId);
            pstmt.setBytes(++i, this.recipientPublicKey);
            pstmt.setByte(++i, this.getState().getCode());
            pstmt.setBytes(++i, this.data);
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getShufflingId() {
        return shufflingId;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getNextAccountId() {
        return nextAccountId;
    }

    void setNextAccountId(long nextAccountId) {
        this.nextAccountId = nextAccountId;
        shufflingParticipantTable.insert(this);
    }

    public byte[] getRecipientPublicKey() {
        return recipientPublicKey;
    }

    void setRecipientPublicKey(byte[] recipientPublicKey) {
        this.recipientPublicKey = recipientPublicKey;
        shufflingParticipantTable.insert(this);
        listeners.notify(this, Event.RECIPIENT_ADDED);
    }

    public State getState() {
        return state;
    }

    void setState(State state) {
        this.state = state;
        shufflingParticipantTable.insert(this);
    }

    public byte[] getData() {
        return data;
    }

    void setData(byte[] data) {
        this.data = data;
        shufflingParticipantTable.insert(this);
    }

    public boolean isVerified() {
        return state == State.VERIFIED;
    }

    void verify() {
        state = State.VERIFIED;
        shufflingParticipantTable.insert(this);
    }

    public boolean isProcessingComplete() {
        return state == State.PROCESSED;
    }

    void setProcessingComplete() {
        state = State.PROCESSED;
    }

}
