package nxt.http;

import java.util.Base64;
import java.util.BitSet;

public class APISet {
    private BitSet internal;

    public APISet() {
        internal = new BitSet();
    }

    private APISet(BitSet bitSet) {
        internal = bitSet;
    }

    public static APISet fromBase64String(String base64Str) {
        byte[] decoded = Base64.getDecoder().decode(base64Str);
        return new APISet(BitSet.valueOf(decoded));
    }

    public String toBase64String() {
        return Base64.getEncoder().encodeToString(internal.toByteArray());
    }

    public boolean add(APIEnum apiEnum) {
        if (internal.get(apiEnum.ordinal())) {
            return false;
        }
        internal.set(apiEnum.ordinal());
        return true;
    }

    public boolean containsName(String name) {
        APIEnum api = APIEnum.fromName(name);
        return api != null && internal.get(api.ordinal());
    }
}
