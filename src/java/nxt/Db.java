package nxt;

import nxt.db.BasicDb;
import nxt.db.TransactionalDb;

public final class Db {

    public static final String PREFIX = Constants.isTestnet ? "nxt.testDb" : "nxt.db";
    public static final TransactionalDb db = new TransactionalDb(new BasicDb.DbProperties()
            .maxCacheSize(Nxt.getIntProperty("nxt.dbCacheKB"))
            .dbUrl(Nxt.getStringProperty(PREFIX + "Url"))
            .dbType(Nxt.getStringProperty(PREFIX + "Type"))
            .dbDir(Nxt.getStringProperty(PREFIX + "Dir"))
            .dbParams(Nxt.getStringProperty(PREFIX + "Params"))
            .dbUsername(Nxt.getStringProperty(PREFIX + "Username"))
            .dbPassword(Nxt.getStringProperty(PREFIX + "Password"))
            .maxConnections(Nxt.getIntProperty("nxt.maxDbConnections"))
            .loginTimeout(Nxt.getIntProperty("nxt.dbLoginTimeout"))
            .defaultLockTimeout(Nxt.getIntProperty("nxt.dbDefaultLockTimeout") * 1000)
    );

    static void init() {
        db.init(new NxtDbVersion());
    }

    static void shutdown() {
        db.shutdown();
    }

    private Db() {} // never

}
