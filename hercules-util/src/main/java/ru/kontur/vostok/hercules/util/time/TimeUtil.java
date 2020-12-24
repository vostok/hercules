package ru.kontur.vostok.hercules.util.time;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Gregory Koshelev
 */
public class TimeUtil {
    /**
     * 100ns ticks in 1 microsecond
     */
    private static final long TICKS_IN_MICROSECOND = 10;
    /**
     * 100ns ticks in 1 millisecond
     */
    private static final long TICKS_IN_MS = 10_000L;
    /**
     * 100ns ticks in 1 second
     */
    private static final long TICKS_IN_SEC = 10_000_000L;
    /**
     * 100ns ticks in 1 minute. Equals {@code 600_000_000L}
     */
    private static final long TICKS_IN_MINUTE = TICKS_IN_SEC * 60;
    /**
     * 100ns ticks in 1 hour. Equals {@code 36_000_000_000L}
     */
    private static final long TICKS_IN_HOUR = TICKS_IN_SEC * 60 * 60;
    /**
     * 100ns ticks in 1 day. Equals {@code 864_000_000_000L}
     */
    private static final long TICKS_IN_DAY = TICKS_IN_SEC * 60 * 60 * 24;
    /**
     * Nanoseconds in 1 100ns tick
     */
    private static final long NANOS_IN_TICK = 100L;

    /**
     * GREGORIAN_EPOCH is offset from 1970-01-01T00:00:00.000Z to 1582-01-01T00:00:00.000Z in 100ns ticks.
     * Epoch determines time-point to start Time Traps
     */
    public static final long GREGORIAN_EPOCH = makeGregorianEpoch();// -122192928000000000L

    /**
     * UNIX_EPOCH starts from 1970-01-01T00:00:00.000Z.
     */
    public static final long UNIX_EPOCH = 0;

    private static long makeGregorianEpoch() {
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
     *
     * @param ticks is 100ns ticks from Unix Epoch
     * @return UUID compatible timestamp
     */
    public static long unixToGregorianTicks(long ticks) {
        return ticks - GREGORIAN_EPOCH;
    }

    /**
     * Convert Gregorian ticks (UUID compatible timestamp) to Unix ticks (Unix timestamp in 100ns ticks)
     *
     * @param ticks is 100ns ticks from Gregorian Epoch
     * @return Unix ticks
     */
    public static long gregorianToUnixTicks(long ticks) {
        return (ticks + GREGORIAN_EPOCH);
    }

    /**
     * Convert 100ns ticks to millis
     *
     * @param ticks is 100ns ticks
     * @return millis
     */
    public static long ticksToMillis(long ticks) {
        return ticks / TICKS_IN_MS;
    }

    /**
     * Convert 100ns ticks to seconds
     *
     * @param ticks is 100ns ticks
     * @return seconds
     */
    public static long ticksToSeconds(long ticks) {
        return ticks / TICKS_IN_SEC;
    }

    public static long ticksToNanos(long ticks) {
        return ticks * NANOS_IN_TICK;
    }

    /**
     * Convert millis to 100ns ticks
     *
     * @param millis is millis
     * @return 100ns ticks
     */
    public static long millisToTicks(long millis) {
        return millis * TICKS_IN_MS;
    }

    /**
     * Convert seconds to 100ns ticks
     *
     * @param seconds is seconds
     * @return 100ns ticks
     */
    public static long secondsToTicks(long seconds) {
        return seconds * TICKS_IN_SEC;
    }

    /**
     * Convert Unix ticks to Unix Time or POSIX time (Unix timestamp in seconds)
     *
     * @param ticks is 100ns ticks from Unix Epoch
     * @return Unix timestamp in seconds
     */
    public static long unixTicksToUnixTime(long ticks) {
        return ticksToSeconds(ticks);
    }

    /**
     * Convert Unix Time or POSIX time (Unix timestamp in seconds) to Unix ticks
     *
     * @param timestamp is Unix timestamp in seconds
     * @return Unix ticks
     */
    public static long unixTimeToUnixTicks(long timestamp) {
        return secondsToTicks(timestamp);
    }

    /**
     * Convert Gregorian ticks (UUID compatible timestamp) to Unix timestamp in millis
     *
     * @param ticks is 100ns ticks from Gregorian Epoch
     * @return Unix timestamp in millis
     */
    public static long gregorianTicksToUnixMillis(long ticks) {
        return ticksToMillis(gregorianToUnixTicks(ticks));
    }

    /**
     * Convert Unix timestamp in millis to Gregorian ticks (UUID compatible timestamp)
     *
     * @param timestamp is Unix timestamp in millis
     * @return UUID compatible timestamp
     */
    public static long unixMillisToGregorianTicks(long timestamp) {
        return unixToGregorianTicks(millisToTicks(timestamp));
    }


    /**
     * Convert Unix ticks (Unix timestamp in 100ns ticks) to Instant
     *
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
     *
     * @param dateTime is ZonedDateTime
     * @return Unix ticks
     */
    public static long dateTimeToUnixTicks(ZonedDateTime dateTime) {
        return dateTime.toEpochSecond() * TICKS_IN_SEC + dateTime.getNano() / NANOS_IN_TICK;
    }

    /**
     * Convert Unix ticks (Unix timestamp in 100ns ticks) to ZonedDateTime with UTC time zone
     *
     * @param ticks Unix ticks
     * @return ZonedDateTime
     */
    public static ZonedDateTime unixTicksToDateTime(long ticks) {
        return ZonedDateTime.ofInstant(unixTicksToInstant(ticks), ZoneOffset.UTC);
    }

    private static final ThreadLocal<DecimalFormat> FORMAT =
            ThreadLocal.withInitial(() -> new DecimalFormat("#.###", DecimalFormatSymbols.getInstance(Locale.ROOT)));

    /**
     * Format 100ns ticks to a string with the appropriate measurement unit
     * (days, hours, minutes, seconds, milliseconds or microseconds).
     * <p>
     * The unit can be printed in two forms: the short one (like {@code d} for {@code days}) or the full.
     *
     * @param ticks            is 100ns ticks
     * @param useShortUnitName use short unit name
     * @return formatted string
     */
    public static String ticksToPrettyString(long ticks, boolean useShortUnitName) {
        if (ticks >= TICKS_IN_DAY) {
            return FORMAT.get().format(((double) ticks) / TICKS_IN_DAY) + (useShortUnitName ? "d" : " days");
        }

        if (ticks >= TICKS_IN_HOUR) {
            return FORMAT.get().format(((double) ticks) / TICKS_IN_HOUR) + (useShortUnitName ? "h" : " hours");
        }

        if (ticks >= TICKS_IN_MINUTE) {
            return FORMAT.get().format(((double) ticks) / TICKS_IN_MINUTE) + (useShortUnitName ? "m" : " minutes");
        }

        if (ticks >= TICKS_IN_SEC) {
            return FORMAT.get().format(((double) ticks) / TICKS_IN_SEC) + (useShortUnitName ? "s" : " seconds");
        }

        if (ticks >= TICKS_IN_MS) {
            return FORMAT.get().format(((double) ticks) / TICKS_IN_MS) + (useShortUnitName ? "ms" : " milliseconds");
        }

        return FORMAT.get().format(((double) ticks) / TICKS_IN_MICROSECOND) + (useShortUnitName ? "us" : " microseconds");
    }
}
