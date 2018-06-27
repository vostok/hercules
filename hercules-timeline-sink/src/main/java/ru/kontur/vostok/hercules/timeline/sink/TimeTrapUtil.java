package ru.kontur.vostok.hercules.timeline.sink;

import ru.kontur.vostok.hercules.util.time.TimeUtil;

/**
 * @author Gregory Koshelev
 */
public class TimeTrapUtil {

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
}