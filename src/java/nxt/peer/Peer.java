package nxt.peer;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public interface Peer extends Comparable<Peer> {

    enum State {
        NON_CONNECTED, CONNECTED, DISCONNECTED
    }

    String getHost();

    int getPort();

    String getAnnouncedAddress();

    State getState();

    String getVersion();

    String getApplication();

    String getPlatform();

    String getSoftware();

    Hallmark getHallmark();

    int getWeight();

    boolean shareAddress();

    boolean isBlacklisted();

    void blacklist(Exception cause);

    void blacklist(String cause);

    void unBlacklist();

    void deactivate();

    void remove();

    long getDownloadedVolume();

    long getUploadedVolume();

    int getLastUpdated();

    String getBlacklistingCause();

    JSONObject send(JSONStreamAware request);

    JSONObject send(JSONStreamAware request, int maxResponseSize);

}
