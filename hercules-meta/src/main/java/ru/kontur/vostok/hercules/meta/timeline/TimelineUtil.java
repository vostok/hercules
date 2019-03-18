package ru.kontur.vostok.hercules.meta.timeline;

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
     * @param timestamp timestamp
     * @param timetrapSize timetrap size
     * @return timetrap offset
     */
    public static long calculateTimetrapOffset(long timestamp, long timetrapSize) {
        return timestamp - (timestamp % timetrapSize);
    }

    /**
     * Calculate next timetrap offset for timestamp
     *
     * Example: if timestamp = 42 and timetrap size = 100, timestamp belong to the timetrap offset 0,
     * but we are looking for the next timetrap, so result will be 100
     *
     * @param timestamp timestamp
     * @param timetrapSize timetrap size
     * @return next timetrap offset
     */
    public static long calculateNextTimetrapOffset(long timestamp, long timetrapSize) {
        return calculateTimetrapOffset(timestamp, timetrapSize) + timetrapSize;
    }

    private TimelineUtil() {
    }
}
