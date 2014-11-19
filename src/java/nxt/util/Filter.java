package nxt.util;

public interface Filter<T> {

    boolean ok(T t);

}
