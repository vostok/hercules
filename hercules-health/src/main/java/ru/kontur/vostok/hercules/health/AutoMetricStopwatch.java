package ru.kontur.vostok.hercules.health;

import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class AutoMetricStopwatch implements AutoCloseable {
    private final Timer timer;
    private final TimeUnit unit;
    private final long start = System.nanoTime();

    public AutoMetricStopwatch(Timer timer, TimeUnit unit) {
        this.timer = timer;
        this.unit = unit;
    }


    @Override
    public void close() {
        timer.update(unit.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS), unit);
    }
}
