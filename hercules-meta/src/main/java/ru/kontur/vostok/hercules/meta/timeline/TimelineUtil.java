package ru.kontur.vostok.hercules.meta.timeline;

import ru.kontur.vostok.hercules.util.time.TimeUtil;

/**
 * Utility class for timelines
 */
public final class TimelineUtil {

    /**
     * Calculate timetrap offset for timestamp
     *
     * Example: if timestamp = 42 and timetrap size = 100, timestamp belong to the timetrap offset 0
     *
     * Example: if timestamp = 142 and timetrap size = 100, timestamp belong to the timetrap offset 100
     *
     * Example: if timestamp = 1530023393 (26-Jun-18 14:29:53 UTC) and timetrap size = 3600,
     * timestamp belong to the timetrap offset 1530021600 (26-Jun-18 14:00:00 UTC)
     *
     * @param ticks timestamp in 100-ns ticks from Unix epoch
     * @param timetrapSize timetrap size in millis
     * @return timetrap offset
     */
    public static long calculateTimetrapOffset(long ticks, long timetrapSize) {
        long timestamp = TimeUtil.ticksToMillis(ticks);
        return timestamp - (timestamp % timetrapSize);
    }

    /**
     * Calculate next timetrap offset for timestamp
     *
     * Example: if timestamp = 42 and timetrap size = 100, timestamp belong to the timetrap offset 0,
     * but we are looking for the next timetrap, so result will be 100
     *
     * @param ticks timestamp in 100-ns ticks from Unix epoch
     * @param timetrapSize timetrap size in millis
     * @return next timetrap offset
     */
    public static long calculateNextTimetrapOffset(long ticks, long timetrapSize) {
        return calculateTimetrapOffset(ticks, timetrapSize) + timetrapSize;
    }

    private TimelineUtil() {
    }
}
