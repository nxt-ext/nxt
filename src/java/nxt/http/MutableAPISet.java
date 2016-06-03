package nxt.http;

import java.util.BitSet;

public class MutableAPISet extends APISet {

    public MutableAPISet() {
        super(new BitSet());
    }

    public MutableAPISet(APISet cloned) {
        super((BitSet) cloned.internal.clone());
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
}
