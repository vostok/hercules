package ru.kontur.vostok.hercules.meta.timeline;

import ru.kontur.vostok.hercules.util.time.TimeUtil;

/**
 * @author Gregory Koshelev
 */
public final class TimeTrapUtil {

    /**
     * Determine Time Trap (i.e. it's offset) for specified timestamp
     *
     * @param timeTrapSize is size of Time Trap in millis
     * @param timestamp    is source timestamp in 100ns ticks from Gregorian Epoch (1582-10-15T00:00:00.000Z)
     * @return offset of Time Trap
     */
    public static long toTimeTrapOffset(long timeTrapSize, long timestamp) {
        return (TimeUtil.ticksToMillis(TimeUtil.gregorianToUnixTicks(timestamp)) / timeTrapSize) * timeTrapSize;
    }

    /**
     * Determine Time Trap (i.e. it's offset) where it's left bound is less than timestamp.
     *
     * @param timeTrapSize is size of Time Trap
     * @param timestamp    is source timestamp in 100ns ticks from Gregorian Epoch (1582-10-15T00:00:00.000Z)
     * @return offset of Time Trap
     */
    public static long toTimeTrapOffsetExclusive(long epoch, long timeTrapSize, long timestamp) {
        return ((TimeUtil.ticksToMillis(TimeUtil.gregorianToUnixTicks(timestamp)) - 1) / timeTrapSize) * timeTrapSize;
    }

    /**
     * Determine list of Time Trap (i.e. it's offset) covering time interval (parameter from inclusive,
     * parameter to exclusive)
     *
     * @param from left bound
     * @param to right bound
     * @param timetrapSize timetrap size
     * @return list of Time Trap
     */
    public static long[] getTimetrapOffsets(long from, long to, long timetrapSize) {
        long fromTimetrap = ru.kontur.vostok.hercules.meta.timeline.TimelineUtil.calculateTimetrapOffset(from, timetrapSize);
        long toTimetrapExclusive = TimelineUtil.calculateNextTimetrapOffset(to, timetrapSize);

        int size = (int)((toTimetrapExclusive - fromTimetrap) / timetrapSize);

        long[] result = new long[size];
        long currentTimetrap = fromTimetrap;
        for (int i = 0; i < size; ++i) {
            result[i] = currentTimetrap;
            currentTimetrap += timetrapSize;
        }
        return result;
    }

    private TimeTrapUtil() {
    }
}