package ru.kontur.vostok.hercules.util.time;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Gregory Koshelev
 */
public final class TimeUnitUtil {
    private static final Map<TimeUnit, String> unitToString;

    static {
        unitToString = Arrays.stream(TimeUnit.values()).
                collect(Collectors.toMap(
                        Function.identity(),
                        unit -> unit.name().toLowerCase()));
    }

    public static String toString(TimeUnit unit) {
        return unitToString.get(unit);
    }

    private TimeUnitUtil() {
        /* static class */
    }
}
