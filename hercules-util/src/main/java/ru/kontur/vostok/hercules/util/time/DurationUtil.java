package ru.kontur.vostok.hercules.util.time;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public final class DurationUtil {
    public static Duration of(long duration, TimeUnit unit) {
        return Duration.ofNanos(unit.toNanos(duration));
    }

    public static Duration from(Timer timer) {
        return Duration.ofMillis(timer.remainingTimeMs());
    }

    private DurationUtil() {
        /* static class */
    }
}
