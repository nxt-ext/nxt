package nxt.db;

import nxt.Block;
import nxt.BlockchainProcessor;
import nxt.Nxt;
import nxt.util.Listener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class BasicDbTable {

    public BasicDbTable() {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                BasicDbTable.this.rollback(block.getHeight());
            }
        }, BlockchainProcessor.Event.BLOCK_POPPED);
    }

    protected abstract String table();

    void rollback(int height) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table() + "WHERE height >= ?")) {
            pstmtDelete.setInt(1, height);
            pstmtDelete.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final void truncate() {
        try (Connection con = Db.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.executeUpdate("TRUNCATE TABLE " + table());
            stmt.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
