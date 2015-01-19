package nxt.http;

import nxt.Db;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.SQLException;

public final class LuceneReindex extends APIServlet.APIRequestHandler {

    static final LuceneReindex instance = new LuceneReindex();

    private LuceneReindex() {
        super(new APITag[] {APITag.DEBUG});
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        try (Connection con = Db.db.getConnection()) {
            org.h2.fulltext.FullTextLucene.reindex(con);
            response.put("done", true);
        } catch (SQLException e) {
            response.put("error", e.toString());
        }
        return response;
    }

    @Override
    final boolean requirePost() {
        return true;
    }

    @Override
    boolean requirePassword() {
        return true;
    }

}
