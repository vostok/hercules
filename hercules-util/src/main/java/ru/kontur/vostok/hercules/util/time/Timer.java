package ru.kontur.vostok.hercules.util.time;

import com.google.common.base.Stopwatch;

import java.util.concurrent.TimeUnit;

/**
 * Timer
 */
public class Timer {

    private final Stopwatch stopwatch;
    private final TimeUnit timeUnit;
    private long duration;

    public Timer(TimeUnit timeUnit, long duration) {
        this.timeUnit = timeUnit;
        this.duration = duration;
        this.stopwatch = Stopwatch.createUnstarted();
    }

    public Timer start() {
        stopwatch.start();
        return this;
    }

    public Timer stop() {
        stopwatch.stop();
        return this;
    }

    public Timer reset() {
        stopwatch.reset();
        return this;
    }

    public boolean isOver() {
        return timeLeft() < 0;
    }

    public long elapsed() {
        return stopwatch.elapsed(timeUnit);
    }

    public long timeLeft() {
        return duration - elapsed();
    }
}
