package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Currency;
import nxt.Db;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.TransactionType;
import nxt.db.DbIterator;
import nxt.db.DbUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class GetAccountExchangeRequests extends APIServlet.APIRequestHandler {

    static final GetAccountExchangeRequests instance = new GetAccountExchangeRequests();

    private GetAccountExchangeRequests() {
        super(new APITag[] {APITag.ACCOUNTS}, "account", "currency", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getAccount(req);
        Currency currency = ParameterParser.getCurrency(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        List<Transaction> transactions = new ArrayList<>();
        DbIterator<? extends Transaction> transactionIterator;
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction where sender_id = ? AND type = ? AND subtype >= ? AND subtype <= ? " +
                    " ORDER BY timestamp DESC" + DbUtils.limitsClause(firstIndex, lastIndex));
            int i = 0;
            pstmt.setLong(++i, account.getId());
            pstmt.setByte(++i, TransactionType.TYPE_MONETARY_SYSTEM);
            pstmt.setByte(++i, TransactionType.SUBTYPE_MONETARY_SYSTEM_EXCHANGE_BUY);
            pstmt.setByte(++i, TransactionType.SUBTYPE_MONETARY_SYSTEM_EXCHANGE_SELL);
            DbUtils.setLimits(++i, pstmt, firstIndex, lastIndex);
            transactionIterator = Nxt.getBlockchain().getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
        for (Transaction transaction : transactionIterator) {
            if (((Attachment.MonetarySystemAttachment)transaction.getAttachment()).getCurrencyId() == currency.getId()) {
                transactions.add(transaction);
            }
        }

        Collections.sort(transactions, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction t1, Transaction t2) {
                return t2.getTimestamp() - t1.getTimestamp();
            }
        });

        JSONArray exchangeRequests = new JSONArray();
        for (Transaction transaction : transactions) {
            exchangeRequests.add(JSONData.exchangeRequest(transaction, true));
        }
        JSONObject response = new JSONObject();
        response.put("exchangeRequests", exchangeRequests);
        return response;


    }

}
