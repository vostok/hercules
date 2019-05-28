package ru.kontur.vostok.hercules.util.time;

import ru.kontur.vostok.hercules.util.Maps;

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
    private static final Map<TimeUnit, String> unitToBriefString;

    static {
        unitToString = Arrays.stream(TimeUnit.values()).
                collect(Collectors.toMap(
                        Function.identity(),
                        unit -> unit.name().toLowerCase()));

        Map<TimeUnit, String> map = new HashMap<>(Maps.effectiveHashMapCapacity(TimeUnit.values().length));
        map.putAll(unitToString);
        map.put(TimeUnit.NANOSECONDS, "ns");
        map.put(TimeUnit.MICROSECONDS, "us");
        map.put(TimeUnit.MILLISECONDS, "ms");
        map.put(TimeUnit.SECONDS, "s");
        map.put(TimeUnit.MINUTES, "min");
        map.put(TimeUnit.HOURS, "hrs");
        map.put(TimeUnit.DAYS, "days");
        unitToBriefString = map;
    }

    public static String toString(TimeUnit unit) {
        return unitToString.get(unit);
    }

    public static String toBriefString(TimeUnit unit) {
        return unitToBriefString.get(unit);
    }

    private TimeUnitUtil() {
        /* static class */
    }
}
