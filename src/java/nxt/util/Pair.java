package nxt.util;

import org.json.simple.JSONArray;
import org.json.simple.JSONAware;

public class Pair<F, S> implements JSONAware {
    private final F first;
    private final S second;
    private transient final int hash; //TODO: why transient?

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
        hash = (first == null ? 0 : first.hashCode() * 31) + (second == null ? 0 : second.hashCode());
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object oth) {
        if (this == oth) {
            return true;
        }
        //TODO: why is the dynamic isInstance needed instead of instanceof?
        if (oth == null || !(getClass().isInstance(oth))) {
            return false;
        }
        Pair<F, S> other = getClass().cast(oth);
        return (first == null ? other.first == null : first.equals(other.first))
                && (second == null ? other.second == null : second.equals(other.second));
    }

    @Override
    public String toString() {
        return "("+first+":"+second+")";
    }

    @Override
    public String toJSONString() {
        JSONArray arr = new JSONArray();
        arr.add(first);
        arr.add(second);
        return arr.toString();
    }
}
