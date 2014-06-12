package nxt.util;

import java.util.HashMap;
import java.util.Map;

/**
 * SuperComplexNumber is a number represented in a form similar to standard complex numbers (http://en.wikipedia.org/wiki/Complex_number).
 * The main contract of this class is to provide a convenient way to detect attempts to spend more money/assets/etc.
 * than is available on an account balance.
 * Pay attention that SuperComplexNumber does not necessarily inherit properties of conventional complex numbers.
 */

public final class SuperComplexNumber {

    private final Map<Long, Long> values = new HashMap<>();

    public Map<Long, Long> getValues() {
        return values;
    }

    public long get(Long measure) {
        Long value = values.get(measure);
        return value == null ? 0 : value;
    }

    public void add(Long measure, long value) {
        values.put(measure, Convert.safeAdd(get(measure), value));
    }

    public void add(SuperComplexNumber otherSuperComplexNumber) {
        for (Map.Entry<Long, Long> entry : otherSuperComplexNumber.values.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }

    public void subtract(Long measure, long value) {
        values.put(measure, Convert.safeSubtract(get(measure), value));
    }

    public void subtract(SuperComplexNumber otherSuperComplexNumber) {
        for (Map.Entry<Long, Long> entry : otherSuperComplexNumber.values.entrySet()) {
            subtract(entry.getKey(), entry.getValue());
        }
    }

    public boolean isCovered(SuperComplexNumber otherSuperComplexNumber) {
        for (Map.Entry<Long, Long> valueEntry : values.entrySet()) {
            if (otherSuperComplexNumber.get(valueEntry.getKey()) < valueEntry.getValue()) {
                return false;
            }
        }
        return true;
    }

}
