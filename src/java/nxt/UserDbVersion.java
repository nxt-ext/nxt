package nxt;

import nxt.db.DbVersion;

class UserDbVersion extends DbVersion {

    protected void update(int nextUpdate) {
        switch (nextUpdate) {
            case 1:
                return;
            default:
                throw new RuntimeException("User database inconsistent with code, probably trying to run older code on newer database");
        }
    }

}
