package ru.kontur.vostok.hercules.util.time;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * @author Gregory Koshelev
 */
public class TimeUtil {
    /**
     * 100ns ticks in 1 second
     */
    private static final long TICKS_IN_SEC = 10_000_000L;
    /**
     * 100ns ticks in 1 millisecond
     */
    private static final long TICKS_IN_MS = 10_000L;
    /**
     * Nanoseconds in 1 100ns tick
     */
    private static final long NANOS_IN_TICK = 100L;

    /**
     * GREGORIAN_EPOCH is offset from 1970-01-01T00:00:00.000Z to 1582-01-01T00:00:00.000Z in 100ns ticks. Epoch determines time-point to start Time Traps
     */
    public static final long GREGORIAN_EPOCH = makeEpoch();// -122192928000000000L

    /**
     * UNIX_EPOCH starts from 1970-01-01T00:00:00.000Z.
     */
    public static final long UNIX_EPOCH = 0;

    private static long makeEpoch() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT-0"));
        calendar.set(Calendar.YEAR, 1582);
        calendar.set(Calendar.MONTH, Calendar.OCTOBER);
        calendar.set(Calendar.DAY_OF_MONTH, 15);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis() * TICKS_IN_MS;
    }

    /**
     * Convert Unix ticks (Unix timestamp in 100ns ticks) to Gregorian ticks (UUID compatible timestamp)
     * @param ticks is 100ns ticks from Unix Epoch
     * @return UUID compatible timestamp
     */
    public static long unixToGregorianTicks(long ticks) {
        return ticks - GREGORIAN_EPOCH;
    }

    /**
     * Convert Gregorian ticks (UUID compatible timestamp) to Unix ticks (Unix timestamp in 100ns ticks)
     * @param ticks is 100ns ticks from Gregorian Epoch
     * @return Unix ticks
     */
    public static long gregorianToUnixTicks(long ticks) {
        return (ticks + GREGORIAN_EPOCH);
    }

    /**
     * Convert 100ns ticks to millis
     * @param ticks is 100ns ticks
     * @return millis
     */
    public static long ticksToMillis(long ticks) {
        return ticks / TICKS_IN_MS;
    }

    /**
     * Convert millis to 100ns ticks
     * @param millis is millis
     * @return 100ns ticks
     */
    public static long millisToTicks(long millis) {
        return millis * TICKS_IN_MS;
    }

    /**
     * Convert Unix ticks to Unix Time (Unix timestamp in millis)
     * @param ticks is 100ns ticks from Unix Epoch
     * @return Unix timestamp in millis
     */
    public static long unixTicksToTime(long ticks) {
        return ticksToMillis(ticks);
    }

    /**
     * Convert Unix Time to Unix ticks
     * @param timestamp is Unix Time
     * @return Unix ticks
     */
    public static long unixTimeToTicks(long timestamp) {
        return millisToTicks(timestamp);
    }

    /**
     * Convert Gregorian ticks (UUID compatible timestamp) to Unix Time (Unix timestamp in millis)
     * @param ticks is 100ns ticks from Gregorian Epoch
     * @return Unix timestamp in millis
     */
    public static long gregorianTicksToUnixTime(long ticks) {
        return ticksToMillis(gregorianToUnixTicks(ticks));
    }

    /**
     * Convert Unix Time (Unix timestamp in millis) to Gregorian ticks (UUID compatible timestamp)
     * @param timestamp is Unix Time
     * @return UUID compatible timestamp
     */
    public static long unixTimeToGregorianTicks(long timestamp) {
        return unixToGregorianTicks(millisToTicks(timestamp));
    }


    /**
     * Convert Unix ticks (Unix timestamp in 100ns ticks) to Instant
     * @param ticks is 100ns ticks from Unix Epoch
     * @return Instant
     */
    public static Instant unixTicksToInstant(long ticks) {
        return Instant.ofEpochSecond(ticks / TICKS_IN_SEC, (ticks % TICKS_IN_SEC) * NANOS_IN_TICK);
    }

    public static Instant gregorianTicksToInstant(long ticks) {
        return unixTicksToInstant(gregorianToUnixTicks(ticks));
    }

    /**
     * Convert ZonedDateTime to Unix ticks (Unix timestamp in 100ns ticks)
     * @param dateTime is ZonedDateTime
     * @return Unix ticks
     */
    public static long dateTimeToUnixTicks(ZonedDateTime dateTime) {
        return dateTime.toEpochSecond() * TICKS_IN_SEC + dateTime.getNano() / NANOS_IN_TICK;
    }
}
