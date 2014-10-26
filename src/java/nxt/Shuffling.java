package nxt;

import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.VersionedEntityDbTable;
import nxt.util.Listener;
import nxt.util.Listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class Shuffling {

    public static enum Event {
        SHUFFLING_CREATED, SHUFFLING_CANCELLED
    }

    private static enum State {
        REGISTRATION((byte)1),
        SHUFFLING((byte)2),
        VERIFICATION((byte)3),
        DISTRIBUTION((byte)4),
        CANCELLED((byte)5),
        DONE((byte)6);

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

    static {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                for (Shuffling shuffling : Shuffling.getAllShufflings(0, -1)) {
                    // Cancel the shuffling in case the blockchain reached its cancellation height
                    if (block.getHeight() > shuffling.getCancellationHeight()) {
                        shuffling.cancel();
                    }
                }
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    private static final Listeners<Shuffling, Event> listeners = new Listeners<>();

    private static final DbKey.LongKeyFactory<Shuffling> shufflingDbKeyFactory = new DbKey.LongKeyFactory<Shuffling>("id") {

        @Override
        public DbKey newKey(Shuffling transfer) {
            return transfer.dbKey;
        }

    };

    private static final VersionedEntityDbTable<Shuffling> shufflingTable = new VersionedEntityDbTable<Shuffling>("Shuffling", shufflingDbKeyFactory) {

        @Override
        protected Shuffling load(Connection con, ResultSet rs) throws SQLException {
            return new Shuffling(rs);
        }

        @Override
        protected void save(Connection con, Shuffling transfer) throws SQLException {
            transfer.save(con);
        }

    };

    public static DbIterator<Shuffling> getAllShufflings(int from, int to) {
        return shufflingTable.getAll(from, to);
    }

    public static int getCount() {
        return shufflingTable.getCount();
    }

    public static boolean addListener(Listener<Shuffling> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Shuffling> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

//    public static DbIterator<Shuffling> getShuffling(long currencyId, int from, int to) {
//        return shufflingTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to);
//    }
//
    public static Shuffling getShuffling(long shufflingId) {
        return shufflingTable.get(shufflingDbKeyFactory.newKey(shufflingId));
    }

    static Shuffling addShuffling(Transaction transaction, Attachment.MonetarySystemShufflingCreation attachment) {
        Shuffling shuffling = new Shuffling(transaction, attachment);
        shufflingTable.insert(shuffling);
        ShufflingParticipant.addParticipant(shuffling.getId(), transaction.getSenderId());
        ShufflingParticipant.addListener(new ParticipantListener(shuffling), ShufflingParticipant.Event.PARTICIPANT_ADDED);
        listeners.notify(shuffling, Event.SHUFFLING_CREATED);
        return shuffling;
    }

    public static void addParticipant(Transaction transaction, Attachment.MonetarySystemShufflingRegistration attachment) {
        long shufflingId = attachment.getShufflingId();
        long participantId = transaction.getSenderId();
        ShufflingParticipant.addParticipant(shufflingId, participantId);

        // Update the shuffling assignee to the new participant and update the next pointer of the existing participant
        // to the new participant
        Shuffling shuffling = Shuffling.getShuffling(shufflingId);
        ShufflingParticipant participant = ShufflingParticipant.getParticipant(shufflingId, shuffling.getAssigneeAccountId());
        participant.setNextAccountId(participantId);
        shuffling.setAssigneeAccountId(participantId);
    }



    static void init() {}

    private final long id;
    private final DbKey dbKey;
    private final boolean isCurrency;
    private final long currencyId;
    private final long issuerId;
    private final long amount;
    private final byte participantCount;
    private final int cancellationHeight;

    private State state;
    private long assigneeAccountId;

    private Shuffling(Transaction transaction, Attachment.MonetarySystemShufflingCreation attachment) {
        this.id = transaction.getId();
        this.dbKey = shufflingDbKeyFactory.newKey(this.id);
        this.isCurrency = attachment.isCurrency();
        this.currencyId = attachment.getCurrencyId();
        this.issuerId = transaction.getSenderId();
        this.amount = attachment.getAmount();
        this.participantCount = attachment.getParticipantCount();
        this.cancellationHeight = attachment.getCancellationHeight();
        this.state = State.REGISTRATION;
        this.assigneeAccountId = issuerId;
    }

    private Shuffling(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = shufflingDbKeyFactory.newKey(this.id);
        this.isCurrency = rs.getBoolean("is_currency");
        this.currencyId = rs.getLong("currency_id");
        this.issuerId = rs.getLong("issuer_id");
        this.amount = rs.getLong("amount");
        this.participantCount = rs.getByte("participant_count");
        this.cancellationHeight = rs.getInt("cancellation_height");
        this.state = State.get(rs.getByte("state"));
        this.assigneeAccountId = rs.getInt("assignee_account_Id");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO shuffling (id, is_currency, currency_id, "
                + "issuer_id, amount_id, participant_count, cancellation_height, state, assignee_account_Id,"
                + "height, latest) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setBoolean(++i, this.isCurrency());
            pstmt.setLong(++i, this.getCurrencyId());
            pstmt.setLong(++i, this.getIssuerId());
            pstmt.setLong(++i, this.getAmount());
            pstmt.setLong(++i, this.getParticipantCount());
            pstmt.setInt(++i, this.getCancellationHeight());
            pstmt.setByte(++i, this.getState().getCode());
            pstmt.setLong(++i, this.getAssigneeAccountId());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public boolean isCurrency() {
        return isCurrency;
    }

    public long getCurrencyId() { return currencyId; }

    public long getIssuerId() {
        return issuerId;
    }

    public long getAmount() {
        return amount;
    }

    public byte getParticipantCount() {
        return participantCount;
    }

    public int getCancellationHeight() {
        return cancellationHeight;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
        shufflingTable.insert(this);
    }

    public long getAssigneeAccountId() {
        return assigneeAccountId;
    }

    public void setAssigneeAccountId(long assigneeAccountId) {
        this.assigneeAccountId = assigneeAccountId;
        shufflingTable.insert(this);
    }

    public void verify(long accountId) {
        ShufflingParticipant.getParticipant(id, accountId).verify();
    }

    public void distribute() {
        for (ShufflingParticipant participant : ShufflingParticipant.getParticipants(id)) {
            updateUnconfirmedBalance(participant.getRecipientId(), amount);
            updateBalance(participant.getRecipientId(), amount);
            updateBalance(participant.getAccountId(), -amount);
        }
    }

    public void cancel() {
        for (ShufflingParticipant participant : ShufflingParticipant.getParticipants(id)) {
            updateUnconfirmedBalance(participant.getAccountId(), amount);
        }
        setState(State.CANCELLED);
    }

    public void updateBalance(long accountId, long amount) {
        if (!isCurrency) {
            Account.getAccount(accountId).addToBalanceNQT(amount);
        } else {
            Account.getAccount(accountId).addToCurrencyUnits(currencyId, amount);
        }
        setState(State.DONE);
    }

    public void updateUnconfirmedBalance(long accountId, long amount) {
        if (!isCurrency) {
            Account.getAccount(accountId).addToUnconfirmedBalanceNQT(amount);
        } else {
            Account.getAccount(accountId).addToUnconfirmedCurrencyUnits(currencyId, amount);
        }
    }

    public boolean isRegistrationEnabled() {
        return state == State.REGISTRATION;
    }
    public boolean isVerificationEnabled() {
        return state == State.VERIFICATION;
    }

    public boolean isDistributionEnabled() {
        return state == State.DISTRIBUTION;
    }

    public boolean isCancelingEnabled() {
        return state != State.CANCELLED && state != State.DONE;
    }

    public boolean isParticipant(long senderId) {
        return ShufflingParticipant.getParticipant(id, senderId) != null;
    }

    static class ParticipantListener implements Listener<ShufflingParticipant> {

        private final Shuffling shuffling;

        public ParticipantListener(Shuffling shuffling) {
            this.shuffling = shuffling;
        }

        @Override
        public void notify(ShufflingParticipant shufflingParticipant) {
            long shufflingId = shufflingParticipant.getShufflingId();
            if (ShufflingParticipant.getCount(shufflingId) == shuffling.participantCount) {
                shuffling.assigneeAccountId = shuffling.issuerId;
                shuffling.setState(State.SHUFFLING); // update the db
            }
        }
    }

}
