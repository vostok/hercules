package ru.kontur.vostok.hercules.health;

import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class AutoMetricStopwatch implements AutoCloseable {
    private final Timer timer;
    private final TimeUnit unit;
    private final TimeSource time;

    private final long start;

    public AutoMetricStopwatch(Timer timer, TimeUnit unit) {
        this(timer, unit, TimeSource.SYSTEM);
    }

    public AutoMetricStopwatch(Timer timer, TimeUnit unit, TimeSource time) {
        this.timer = timer;
        this.unit = unit;
        this.time = time;

        this.start = time.nanoseconds();
    }


    @Override
    public void close() {
        timer.update(unit.convert(time.nanoseconds() - start, TimeUnit.NANOSECONDS), unit);
    }
}
