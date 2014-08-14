package nxt;

import nxt.db.DbTable;
import nxt.util.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class PollResults<K, V> {
    public static final byte BINARY_RESULTS = 1;
    public static final byte CHOICE_RESULTS = 2;

    protected long pollId;
    protected Map<K, V> results;

    PollResults(long pollId, Map<K,V> results) {
        this.pollId = pollId;
        this.results = results;
    }

    PollResults(long pollId, String resultsAsJson) throws NxtException.ValidationException {
        this.pollId = pollId;
        this.results = decodeResultsFromJson(resultsAsJson);
    }

    private static final DbTable<PollResults> pollResultsTable = new DbTable<PollResults>() {
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

        @Override
        protected void delete(Connection con, PollResults pr) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("DELETE FROM "+TABLE_NAME+" WHERE id = ?")) {
                pstmt.setLong(1, pr.getPollId());
                pstmt.executeUpdate();
            }
        }
    };

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
        return pollResultsTable.get(pollId);
    }

    public static Collection<PollResults> getAllPollResults() {
        return pollResultsTable.getAll();
    }

    public static void save(PollResults pr){
        pollResultsTable.insert(pr);
    }

    static void clear(){
        pollResultsTable.truncate();
    }

    protected String encodeResultsAsJson() throws NxtException.ValidationException {
        return JSONObject.toJSONString(results);
    }

    protected Map<K,V> decodeResultsFromJson(String json) throws NxtException.ValidationException{
        try {
            JSONParser parser = new JSONParser();
            return (Map<K, V>)(parser.parse(json));
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
