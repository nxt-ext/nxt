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

    public void intersect(APISet other) {
        internal.and(other.internal);
    }

    public boolean containsName(String name) {
        APIEnum api = APIEnum.fromName(name);
        return api != null && internal.get(api.ordinal());
    }
/*
    public APISet not() {
        BitSet result = (BitSet) internal.clone();
        result.flip(0, APIEnum.values().length);
        return new APISet(result);
    }

    public APISet and(APISet other) {
        BitSet result = (BitSet) internal.clone();
        result.and(other.internal);
        return new APISet(result);
    }

    public APISet andNot(APISet other) {
        BitSet result = (BitSet) internal.clone();
        result.andNot(other.internal);
        return new APISet(result);
    }
*/
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
