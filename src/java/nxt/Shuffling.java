package nxt;

import nxt.crypto.EncryptedData;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.VersionedEntityDbTable;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class Shuffling {

    public static enum Event {
        SHUFFLING_CREATED, SHUFFLING_CANCELLED, SHUFFLING_DONE
    }

    public static enum Stage {
        REGISTRATION((byte)1),
        PROCESSING((byte)2),
        VERIFICATION((byte)3),
        CANCELLED((byte)4) {
            @Override
            public boolean isCancellationAllowed() {
                return false;
            }
        },
        DONE((byte)5) {
            @Override
            public boolean isCancellationAllowed() {
                return false;
            }
        };

        private final byte code;

        Stage(byte code) {
            this.code = code;
        }

        private static Stage get(byte code) {
            for (Stage stage : Stage.values()) {
                if (stage.code == code) {
                    return stage;
                }
            }
            throw new IllegalArgumentException("No matching stage for " + code);
        }

        public byte getCode() {
            return code;
        }

        public boolean isCancellationAllowed() {
            return true;
        }
    }

    static {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                Connection con = null;
                try {
                    con = Db.db.getConnection();
                    PreparedStatement pstmt = con.prepareStatement("SELECT * FROM shuffling WHERE stage <> ? and stage <> ? AND latest = TRUE");
                    int i = 0;
                    pstmt.setByte(++i, Stage.CANCELLED.getCode());
                    pstmt.setByte(++i, Stage.DONE.getCode());
                    DbIterator<Shuffling> shufflings = shufflingTable.getManyBy(con, pstmt, false);
                    for (Shuffling shuffling : shufflings) {
                        // Cancel the shuffling in case the blockchain reached its cancellation height
                        if (block.getHeight() > shuffling.getCancellationHeight() && shuffling.getStage().isCancellationAllowed()) {
                            shuffling.cancel();
                        }
                    }
                } catch (SQLException e) {
                    DbUtils.close(con);
                    throw new RuntimeException(e.toString(), e);
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

    private static final VersionedEntityDbTable<Shuffling> shufflingTable = new VersionedEntityDbTable<Shuffling>("shuffling", shufflingDbKeyFactory) {

        @Override
        protected Shuffling load(Connection con, ResultSet rs) throws SQLException {
            return new Shuffling(rs);
        }

        @Override
        protected void save(Connection con, Shuffling shuffling) throws SQLException {
            shuffling.save(con);
        }

    };

    public static DbIterator<Shuffling> getAll(int from, int to) {
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

    public static Shuffling getShuffling(long shufflingId) {
        return shufflingTable.get(shufflingDbKeyFactory.newKey(shufflingId));
    }

    static void addShuffling(Transaction transaction, Attachment.MonetarySystemShufflingCreation attachment) {
        Shuffling shuffling = new Shuffling(transaction, attachment);
        shufflingTable.insert(shuffling);
        ShufflingParticipant.addParticipant(shuffling.getId(), transaction.getSenderId());
        listeners.notify(shuffling, Event.SHUFFLING_CREATED);
    }

    static void addParticipant(Transaction transaction, Attachment.MonetarySystemShufflingRegistration attachment) {
        long shufflingId = attachment.getShufflingId();
        long participantId = transaction.getSenderId();
        ShufflingParticipant.addParticipant(shufflingId, participantId);

        // Update the shuffling assignee to point to the new participant and update the next pointer of the existing participant
        // to the new participant
        Shuffling shuffling = Shuffling.getShuffling(shufflingId);
        ShufflingParticipant participant = ShufflingParticipant.getParticipant(shufflingId, shuffling.getAssigneeAccountId());
        participant.setNextAccountId(participantId);
        shuffling.setAssigneeAccountId(participantId);

        // Check if participant registration is complete and if so update the shuffling
        if (ShufflingParticipant.getCount(shufflingId) == shuffling.participantCount) {
            shuffling.assigneeAccountId = shuffling.issuerId;
            shuffling.setStage(Stage.PROCESSING); // update the db
        }
    }

    public static void updateParticipantData(Transaction transaction, Attachment.MonetarySystemShufflingProcessing attachment) {
        long shufflingId = attachment.getShufflingId();
        long participantId = transaction.getSenderId();
        byte[] data = attachment.getData();
        ShufflingParticipant senderParticipant = ShufflingParticipant.getParticipant(shufflingId, participantId);
        Shuffling shuffling = Shuffling.getShuffling(shufflingId);
        long nextParticipantId = senderParticipant.getNextAccountId();
        if (nextParticipantId != 0) {
            shuffling.setAssigneeAccountId(nextParticipantId);
            ShufflingParticipant.updateData(shufflingId, nextParticipantId, data);
            senderParticipant.setProcessingComplete();
        } else {
            // participant processing is complete update the shuffling stage
            shuffling.setStage(Stage.VERIFICATION);
            List<EncryptedData> unmarshaledDataList = EncryptedData.getUnmarshaledDataList(data);
            Deque<EncryptedData> stack = new ArrayDeque<>(unmarshaledDataList);
            for (ShufflingParticipant participant : ShufflingParticipant.getParticipants(shufflingId)) {
                long recipientId = Convert.parseUnsignedLong(Convert.toString(stack.pop().getData()));
                participant.setRecipientId(recipientId);
            }
        }
    }

    static void init() {}

    private final long id;
    private final DbKey dbKey;
    private final long currencyId;
    private final long issuerId;
    private final long amount;
    private final byte participantCount;
    private final int cancellationHeight;

    private Stage stage;
    private long assigneeAccountId;

    private Shuffling(Transaction transaction, Attachment.MonetarySystemShufflingCreation attachment) {
        this.id = transaction.getId();
        this.dbKey = shufflingDbKeyFactory.newKey(this.id);
        this.currencyId = attachment.getCurrencyId();
        this.issuerId = transaction.getSenderId();
        this.amount = attachment.getAmount();
        this.participantCount = attachment.getParticipantCount();
        this.cancellationHeight = attachment.getCancellationHeight();
        this.stage = Stage.REGISTRATION;
        this.assigneeAccountId = issuerId;
    }

    private Shuffling(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = shufflingDbKeyFactory.newKey(this.id);
        this.currencyId = rs.getLong("currency_id");
        this.issuerId = rs.getLong("issuer_id");
        this.amount = rs.getLong("amount");
        this.participantCount = rs.getByte("participant_count");
        this.cancellationHeight = rs.getInt("cancellation_height");
        this.stage = Stage.get(rs.getByte("stage"));
        this.assigneeAccountId = rs.getLong("assignee_account_Id");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO shuffling (id, currency_id, "
                + "issuer_id, amount, participant_count, cancellation_height, stage, assignee_account_Id,"
                + "height, latest) "
                + "KEY (id, height)"
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            DbUtils.setLongZeroToNull(pstmt, ++i, this.getCurrencyId());
            pstmt.setLong(++i, this.getIssuerId());
            pstmt.setLong(++i, this.getAmount());
            pstmt.setLong(++i, this.getParticipantCount());
            pstmt.setInt(++i, this.getCancellationHeight());
            pstmt.setByte(++i, this.getStage().getCode());
            pstmt.setLong(++i, this.getAssigneeAccountId());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public boolean isCurrency() {
        return currencyId != 0;
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

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        shufflingTable.insert(this);
    }

    public long getAssigneeAccountId() {
        return assigneeAccountId;
    }

    public void setAssigneeAccountId(long assigneeAccountId) {
        this.assigneeAccountId = assigneeAccountId;
        shufflingTable.insert(this);
    }

    void verify(long accountId) {
        ShufflingParticipant.getParticipant(id, accountId).verify();
    }

    void distribute() {
        for (ShufflingParticipant participant : ShufflingParticipant.getParticipants(id)) {
            updateBalance(participant.getRecipientId(), amount);
            updateUnconfirmedBalance(participant.getRecipientId(), amount);
            updateBalance(participant.getAccountId(), -amount);
        }
        setStage(Stage.DONE);
        listeners.notify(this, Event.SHUFFLING_DONE);
    }

    void cancel() {
        for (ShufflingParticipant participant : ShufflingParticipant.getParticipants(id)) {
            updateUnconfirmedBalance(participant.getAccountId(), amount);
        }
        setStage(Stage.CANCELLED);
        listeners.notify(this, Event.SHUFFLING_CANCELLED);
    }

    // TODO check balances in unit test
    private void updateBalance(long accountId, long amount) {
        if (isCurrency()) {
            Account.getAccount(accountId).addToCurrencyUnits(currencyId, amount);
        } else {
            Account.getAccount(accountId).addToBalanceNQT(amount);
        }
    }

    private void updateUnconfirmedBalance(long accountId, long amount) {
        if (isCurrency()) {
            Account.getAccount(accountId).addToUnconfirmedCurrencyUnits(currencyId, amount);
        } else {
            Account.getAccount(accountId).addToUnconfirmedBalanceNQT(amount);
        }
    }

    boolean isRegistrationAllowed() {
        return stage == Stage.REGISTRATION;
    }

    public boolean isProcessingAllowed() {
        return stage == Stage.PROCESSING;
    }

    boolean isVerificationAllowed() {
        return stage == Stage.VERIFICATION;
    }

    /**
     * Distribution is allowed only if the shuffling is in verification stage and all participants verified their recipient account
     * @return is distribution allowed
     */
    boolean isDistributionAllowed() {
        if (!isVerificationAllowed()) {
            return false;
        }
        for (ShufflingParticipant participant : ShufflingParticipant.getParticipants(id)) {
            if (!participant.isVerified()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Shuffling issuer can cancel the shuffling at any time but participants can only cancel during the verification stage
     *
     * @param senderId the sender of the transaction
     * @return is cancellation allowed
     */
    boolean isCancellationAllowed(long senderId) {
        if (senderId == issuerId) {
            return stage.isCancellationAllowed();
        } else {
            return stage == Stage.VERIFICATION;
        }
    }

    boolean isParticipant(long senderId) {
        return ShufflingParticipant.getParticipant(id, senderId) != null;
    }

    boolean isParticipantVerified(long senderId) {
        return ShufflingParticipant.getParticipant(id, senderId).isVerified();
    }

    public boolean isParticipantProcessingComplete(long senderId) {
        return ShufflingParticipant.getParticipant(id, senderId).isProcessingComplete();
    }

    public static void cancelShuffling(long currencyId) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM shuffling WHERE currency_id = ? AND latest = TRUE");
            int i = 0;
            pstmt.setLong(++i, currencyId);
            DbIterator<Shuffling> shufflings = shufflingTable.getManyBy(con, pstmt, false);
            for (Shuffling shuffling : shufflings) {
                if (shuffling.getStage().isCancellationAllowed()) {
                    shuffling.cancel();
                }
            }
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
}
