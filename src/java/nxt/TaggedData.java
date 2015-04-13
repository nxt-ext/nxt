package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.PrunableDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TaggedData {

    public static final DbKey.LongKeyFactory<TaggedData> taggedDataKeyFactory = new DbKey.LongKeyFactory<TaggedData>("id") {

        @Override
        public DbKey newKey(TaggedData taggedData) {
            return taggedData.dbKey;
        }

    };

    public static final PrunableDbTable<TaggedData> taggedDataTable = new PrunableDbTable<TaggedData>(
            "tagged_data", taggedDataKeyFactory, "name,description,tags") {

        @Override
        protected TaggedData load(Connection con, ResultSet rs) throws SQLException {
            return new TaggedData(rs);
        }

        @Override
        protected void save(Connection con, TaggedData taggedData) throws SQLException {
            taggedData.save(con);
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY block_timestamp DESC, db_id DESC ";
        }

    };

    public static int getCount() {
        return taggedDataTable.getCount();
    }

    public static DbIterator<TaggedData> getAll(int from, int to) {
        return taggedDataTable.getAll(from, to);
    }

    public static TaggedData getData(long transactionId) {
        return taggedDataTable.get(taggedDataKeyFactory.newKey(transactionId));
    }

    public static DbIterator<TaggedData> getData(long accountId, int from, int to) {
        return taggedDataTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    public static DbIterator<TaggedData> searchAccountData(String query, long accountId, int from, int to) {
        return taggedDataTable.search(query, new DbClause.LongClause("account_id", accountId), from, to,
                " ORDER BY ft.score DESC, tagged_data.block_timestamp DESC, tagged_data.db_id DESC ");
    }

    public static DbIterator<TaggedData> searchData(String query, int from, int to) {
        return taggedDataTable.search(query, DbClause.EMPTY_CLAUSE, from, to,
                " ORDER BY ft.score DESC, tagged_data.block_timestamp DESC, tagged_data.db_id DESC ");
    }


    static void init() {}

    private final long id;
    private final DbKey dbKey;
    private final long accountId;
    private final String name;
    private final String description;
    private final String tags;
    private final byte[] data;
    private final String type;
    private final boolean isText;
    private final String filename;
    private final int transactionTimestamp;
    private final int blockTimestamp;

    private TaggedData(Transaction transaction, Attachment.TaggedDataUpload attachment) {
        this.id = transaction.getId();
        this.dbKey = taggedDataKeyFactory.newKey(this.id);
        this.accountId = transaction.getSenderId();
        this.name = attachment.getName();
        this.description = attachment.getDescription();
        this.tags = attachment.getTags();
        this.data = attachment.getData();
        this.type = attachment.getType();
        this.isText = attachment.isText();
        this.filename = attachment.getFilename();
        this.blockTimestamp = Nxt.getBlockchain().getLastBlockTimestamp();
        this.transactionTimestamp = transaction.getTimestamp();
    }

    private TaggedData(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = taggedDataKeyFactory.newKey(this.id);
        this.accountId = rs.getLong("account_id");
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        this.tags = rs.getString("tags");
        this.data = rs.getBytes("data");
        this.type = rs.getString("type");
        this.isText = rs.getBoolean("is_text");
        this.filename = rs.getString("filename");
        this.blockTimestamp = rs.getInt("block_timestamp");
        this.transactionTimestamp = rs.getInt("transaction_timestamp");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO tagged_data (id, account_id, name, description, tags, "
                + "type, data, is_text, filename, block_timestamp, transaction_timestamp, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.accountId);
            pstmt.setString(++i, this.name);
            pstmt.setString(++i, this.description);
            pstmt.setString(++i, this.tags);
            pstmt.setString(++i, this.type);
            pstmt.setBytes(++i, this.data);
            pstmt.setBoolean(++i, this.isText);
            pstmt.setString(++i, this.filename);
            pstmt.setInt(++i, this.blockTimestamp);
            pstmt.setInt(++i, this.transactionTimestamp);
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getTags() {
        return tags;
    }

    public byte[] getData() {
        return data;
    }

    public String getType() {
        return type;
    }

    public boolean isText() {
        return isText;
    }

    public String getFilename() {
        return filename;
    }

    public int getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public int getBlockTimestamp() {
        return blockTimestamp;
    }

    static void add(Transaction transaction, Attachment.TaggedDataUpload attachment) {
        if (Nxt.getEpochTime() - transaction.getTimestamp() < Constants.MAX_PRUNABLE_LIFETIME
                && taggedDataTable.get(taggedDataKeyFactory.newKey(transaction.getId())) == null) {
            TaggedData taggedData = new TaggedData(transaction, attachment);
            taggedDataTable.insert(taggedData);
        }
    }

}
