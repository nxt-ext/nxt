package nxt.http;

import java.util.Base64;
import java.util.BitSet;

public class APISet {
    protected BitSet internal;

    public final static APISet EMPTY = new APISet(new BitSet());

    protected APISet(BitSet bitSet) {
        internal = bitSet;
    }

    public static APISet fromBase64String(String base64Str) {
        byte[] decoded = Base64.getDecoder().decode(base64Str);
        return new APISet(BitSet.valueOf(decoded));
    }

    public String toBase64String() {
        return Base64.getEncoder().encodeToString(internal.toByteArray());
    }


    public boolean containsName(String name) {
        APIEnum api = APIEnum.fromName(name);
        return api != null && internal.get(api.ordinal());
    }

    public boolean isEmpty() {
        return internal.isEmpty();
    }

    public boolean containsAll(APISet other) {
        long[] containerArr = internal.toLongArray();
        long[] containedArr = other.internal.toLongArray();
        if (containedArr.length > containerArr.length) {
            return false;
        }

        for (int i = 0; i < containedArr.length; i++) {
            if ((containerArr[i] & containedArr[i]) != containedArr[i]) {
                return false;
            }
        }
        return true;
    }
}
