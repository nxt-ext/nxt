package nxt;

import nxt.db.BasicDb;
import nxt.db.TransactionalDb;

public final class Db {

    public static final TransactionalDb db = new TransactionalDb(new BasicDb.DbProperties()
            .maxCacheSize(Nxt.getIntProperty("nxt.dbCacheKB"))
            .dbUrl(Constants.isTestnet ? Nxt.getStringProperty("nxt.testDbUrl") : Nxt.getStringProperty("nxt.dbUrl"))
            .maxConnections(Nxt.getIntProperty("nxt.maxDbConnections"))
            .loginTimeout(Nxt.getIntProperty("nxt.dbLoginTimeout"))
            .defaultLockTimeout(Nxt.getIntProperty("nxt.dbDefaultLockTimeout") * 1000)
    );

    /*
    public static final BasicDb userDb = new BasicDb(new BasicDb.DbProperties()
            .maxCacheSize(Nxt.getIntProperty("nxt.userDbCacheKB"))
            .dbUrl(Constants.isTestnet ? Nxt.getStringProperty("nxt.testUserDbUrl") : Nxt.getStringProperty("nxt.userDbUrl"))
            .maxConnections(Nxt.getIntProperty("nxt.maxUserDbConnections"))
            .loginTimeout(Nxt.getIntProperty("nxt.userDbLoginTimeout"))
            .defaultLockTimeout(Nxt.getIntProperty("nxt.userDbDefaultLockTimeout") * 1000)
    );
    */

    static void init() {
        db.init("sa", "sa", new NxtDbVersion());
        //userDb.init("sa", "databaseencryptionpassword sa", new UserDbVersion());
    }

    static void shutdown() {
        //userDb.shutdown();
        db.shutdown();
    }

    private Db() {} // never

}
