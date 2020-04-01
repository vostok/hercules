package ru.kontur.vostok.hercules.util.time;

/**
 * @author Gregory Koshelev
 * @deprecated Use {@link Timer} instead
 */
@Deprecated
public class StopwatchUtil {
    public static long elapsedTime(long startedAtMs) {
        return System.currentTimeMillis() - startedAtMs;
    }

    public static long remainingTimeOrZero(long timeoutMs, long elapsedTimeMs) {
        return Math.max(timeoutMs - elapsedTimeMs, 0L);
    }
}
