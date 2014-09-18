package nxt;

import nxt.db.DbKey;
import nxt.db.EntityDbTable;
import nxt.util.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class PollResults<K, V> {
    public static final byte BINARY_RESULTS = 1;
    public static final byte CHOICE_RESULTS = 2;

    private static final DbKey.LongKeyFactory<PollResults> pollResultsDbKeyFactory = new DbKey.LongKeyFactory<PollResults>("id") {
        @Override
        public DbKey newKey(PollResults results) {
            return results.dbKey;
        }
    };

    private static final EntityDbTable<PollResults> resultsTable = new EntityDbTable<PollResults>(pollResultsDbKeyFactory) {
        public static final String TABLE_NAME = "poll_results";

        @Override
        protected String table() {
            return TABLE_NAME;
        }

        @Override
        protected PollResults load(Connection con, ResultSet rs) throws SQLException {
            return fromResultSet(rs);
        }

        @Override
        protected void save(Connection con, PollResults pr) throws SQLException {
            String query = "INSERT INTO "+TABLE_NAME+" (id, results_type, results_json) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = con.prepareStatement(query)) {
                int i = 0;
                pstmt.setLong(++i, pr.getPollId());

                byte resultsType;
                if(pr instanceof Binary){
                    resultsType = BINARY_RESULTS;
                } else if(pr instanceof Choice){
                    resultsType = CHOICE_RESULTS;
                }else{
                    throw new NxtException.NotValidException("Wrong kind of results");
                }

                pstmt.setByte(++i, resultsType);

                pstmt.setString(++i, pr.encodeResultsAsJson());
                pstmt.executeUpdate();
            }catch(NxtException.ValidationException ve){
                throw new SQLException(ve);
            }
        }
    };

    static void init() {}

    protected long pollId;
    private final DbKey dbKey;
    protected Map<K, V> results;

    PollResults(long pollId, Map<K,V> results) {
        this.pollId = pollId;
        this.dbKey = pollResultsDbKeyFactory.newKey(this.pollId);
        this.results = results;
    }

    PollResults(long pollId, String resultsAsJson) throws NxtException.ValidationException {
        this.pollId = pollId;
        this.dbKey = pollResultsDbKeyFactory.newKey(this.pollId);
        this.results = decodeResultsFromJson(resultsAsJson);
    }

    protected static PollResults fromResultSet(ResultSet rs) throws SQLException {
        Long pollId = rs.getLong("id");

        byte mode = rs.getByte("results_type");

        PollResults results;

        String resultsAsJson = rs.getString("results_json");

        try {
            if(mode==BINARY_RESULTS){
                 results = new Binary(pollId, resultsAsJson);
            }else if(mode==CHOICE_RESULTS){
                results = new Choice(pollId, resultsAsJson);
            }else{
                throw new NxtException.NotValidException("wrong kind of results");
            }
        } catch (NxtException.ValidationException e) {
            throw new SQLException(e);
        }
        return results;
    }

    public long getPollId() {
        return pollId;
    }

    public Map<K, V> getResults() {
        return Collections.unmodifiableMap(results);
    }

    public static PollResults get(long pollId) {
        return resultsTable.get(pollResultsDbKeyFactory.newKey(pollId));
    }

    public static void save(PollResults pr){
        resultsTable.insert(pr);
    }

    protected String encodeResultsAsJson() throws NxtException.ValidationException {
        return JSONObject.toJSONString(results);
    }

    protected Map<K,V> decodeResultsFromJson(String json) throws NxtException.ValidationException{
        try {
            return (Map<K, V>)(new JSONParser().parse(json));
        } catch (ParseException e) {
            throw new NxtException.NotValidException("Illegal contents of pollresults: "+json);
        }
    }

    public static class Choice extends PollResults<String, Long> {
        public Choice(long pollId, Map<String, Long> results) {
            super(pollId, results);
        }

        public Choice(long pollId, String resultsAsJson) throws NxtException.ValidationException {
            super(pollId, resultsAsJson);
        }
    }

    public static class Binary extends PollResults<String, Pair.YesNoCounts> {
        public Binary(long pollId, Map<String, Pair.YesNoCounts> results) {
            super(pollId, results);
        }

        public Binary(long pollId, String resultsAsJson) throws NxtException.ValidationException {
            super(pollId, resultsAsJson);
        }

        protected Map<String, Pair.YesNoCounts> decodeResultsFromJson(String json) throws NxtException.ValidationException{
            try {
                JSONParser parser = new JSONParser();
                Map<String, JSONArray> temp = (Map<String, JSONArray>)(parser.parse(json));

                Map<String, Pair.YesNoCounts> resulting = new HashMap<>();
                for(Map.Entry<String, JSONArray> entry:temp.entrySet()){
                    long yes = (long)entry.getValue().get(0);
                    long no = (long)entry.getValue().get(1);
                    resulting.put(entry.getKey(), new Pair.YesNoCounts(yes, no));
                }
                return resulting;
            } catch (ParseException e) {
                throw new NxtException.NotValidException("Illegal contents of pollresults: "+json);
            }
        }
    }
}
