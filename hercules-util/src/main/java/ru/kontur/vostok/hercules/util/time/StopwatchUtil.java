package ru.kontur.vostok.hercules.util.time;

/**
 * @author Gregory Koshelev
 */
public class StopwatchUtil {
    public static long elapsedTime(long startedAtMs) {
        return System.currentTimeMillis() - startedAtMs;
    }

    public static long remainingTimeOrZero(long timeoutMs, long elapsedTimeMs) {
        return Math.max(timeoutMs - elapsedTimeMs, 0L);
    }
}
