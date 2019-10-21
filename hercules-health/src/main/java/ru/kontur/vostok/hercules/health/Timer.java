package ru.kontur.vostok.hercules.health;

import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public interface Timer extends Metric {
    void update(long duration, TimeUnit unit);
    default void update(long durationMs) {
        update(durationMs, TimeUnit.MILLISECONDS);
    }
}
