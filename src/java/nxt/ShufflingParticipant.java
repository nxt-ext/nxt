package nxt;

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

    private static final VersionedEntityDbTable<ShufflingParticipant> shufflingParticipantTable = new VersionedEntityDbTable<ShufflingParticipant>("Shuffling_participant", shufflingParticipantDbKeyFactory) {

        @Override
        protected ShufflingParticipant load(Connection con, ResultSet rs) throws SQLException {
            return new ShufflingParticipant(rs);
        }

        @Override
        protected void save(Connection con, ShufflingParticipant transfer) throws SQLException {
            transfer.save(con);
        }

    };

    public static int getCount() {
        return shufflingParticipantTable.getCount();
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

    static ShufflingParticipant addParticipant(Transaction transaction, Attachment.MonetarySystemShufflingRegistration attachment) {
        ShufflingParticipant participant = new ShufflingParticipant(transaction, attachment);
        shufflingParticipantTable.insert(participant);
        listeners.notify(participant, Event.PARTICIPANT_ADDED);
        return participant;
    }

    static ShufflingParticipant addParticipant(long shufflingId, long accountId, long recipientId) {
        ShufflingParticipant participant = new ShufflingParticipant(shufflingId, accountId, recipientId);
        participant.recipientId = recipientId;
        shufflingParticipantTable.insert(participant);
        listeners.notify(participant, Event.PARTICIPANT_ADDED);
        return participant;
    }

    static void init() {}

    private final long shufflingId;

    private final DbKey dbKey;
    private final long accountId;
    private long recipientId;

    public ShufflingParticipant(long shufflingId, long accountId, long recipientId) {
        this.shufflingId = shufflingId;
        this.accountId = accountId;
        this.dbKey = shufflingParticipantDbKeyFactory.newKey(shufflingId, accountId);
        this.recipientId = recipientId;
    }

    private ShufflingParticipant(Transaction transaction, Attachment.MonetarySystemShufflingRegistration attachment) {
        this.shufflingId = attachment.getShufflingId();
        this.accountId = transaction.getSenderId();
        this.dbKey = shufflingParticipantDbKeyFactory.newKey(shufflingId, accountId);
    }

    private ShufflingParticipant(ResultSet rs) throws SQLException {
        this.shufflingId = rs.getLong("shuffling_id");
        this.accountId = rs.getLong("account_id");
        this.dbKey = shufflingParticipantDbKeyFactory.newKey(shufflingId, accountId);
        this.recipientId = rs.getLong("recipient_id");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO shuffle (shuffle_id, "
                + "account_id, recipient_id) "
                + "VALUES (?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getShufflingId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setLong(++i, this.getRecipientId());
            pstmt.executeUpdate();
        }
    }

    public long getShufflingId() {
        return shufflingId;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getRecipientId() {
        return recipientId;
    }
}
