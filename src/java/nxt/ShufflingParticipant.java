package nxt;

import nxt.db.Db;
import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.VersionedEntityDbTable;
import nxt.util.Listener;
import nxt.util.Listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class ShufflingParticipant {

    public static enum State {
        REGISTERED((byte)0),
        VERIFIED((byte)1);

        private final byte code;

        State(byte code) {
            this.code = code;
        }

        public static State get(byte code) {
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

    public static enum Event {
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
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM Shuffling_participant WHERE shuffling_id = ? AND latest = TRUE")) {
            pstmt.setLong(1, shufflingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
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

    static ShufflingParticipant addParticipant(long shufflingId, long accountId) {
        ShufflingParticipant participant = new ShufflingParticipant(shufflingId, accountId);
        shufflingParticipantTable.insert(participant);
        return participant;
    }

    static void init() {}

    private final long shufflingId;

    private final DbKey dbKey;
    private final long accountId;

    private long nextAccountId;
    private long recipientId;
    private State state;
    private byte[] data;

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
        this.recipientId = rs.getLong("recipient_id");
        this.state = State.get(rs.getByte("state"));
        this.data = rs.getBytes("data");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO shuffling_participant (shuffling_id, "
                + "account_id, next_account_id, recipient_id, state, data, height, latest) "
                + "KEY (shuffling_id, account_id, height)"
                + "VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.getShufflingId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setLong(++i, this.getNextAccountId());
            pstmt.setLong(++i, this.getRecipientId());
            pstmt.setByte(++i, this.getState().getCode());
            pstmt.setBytes(++i, this.getData());
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

    public long getRecipientId() {
        return recipientId;
    }

    public State getState() {
        return state;
    }

    public byte[] getData() {
        return data;
    }

    public void setNextAccountId(long nextAccountId) {
        this.nextAccountId = nextAccountId;
        shufflingParticipantTable.insert(this);
    }

    public void verify() {
        state = State.VERIFIED;
        shufflingParticipantTable.insert(this);
    }

}
