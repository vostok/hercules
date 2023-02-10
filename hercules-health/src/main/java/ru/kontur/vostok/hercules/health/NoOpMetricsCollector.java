package ru.kontur.vostok.hercules.health;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Implementation without any operations.
 * Useful for tests.
 *
 * @author Innokentiy Krivonosov
 */
public class NoOpMetricsCollector implements IMetricsCollector {
    @Override
    public Meter meter(String name) {
        return new MeterImpl();
    }

    @Override
    public Timer timer(String name) {
        return new TimerImpl();
    }

    @Override
    public Counter counter(String name) {
        return new CounterImpl();
    }

    @Override
    public Histogram histogram(String name) {
        return new HistogramImpl();
    }

    @Override
    public <T> void gauge(String name, Supplier<T> supplier) {

    }

    @Override
    public boolean remove(String name) {
        return false;
    }

    /**
     * @author Gregory Koshelev
     */
    public static class MeterImpl implements Meter {

        @Override
        public void mark(long n) {

        }

        @Override
        public void mark() {

        }

        @Override
        public String name() {
            return "name";
        }
    }

    /**
     * @author Gregory Koshelev
     */
    public static class TimerImpl implements Timer {
        @Override
        public void update(long duration, TimeUnit unit) {

        }
    }

    /**
     * @author Gregory Koshelev
     */
    public static class CounterImpl implements Counter {
        @Override
        public void increment(long value) {

        }

        @Override
        public void decrement(long value) {

        }
    }

    /**
     * @author Gregory Koshelev
     */
    public static class HistogramImpl implements Histogram {
        @Override
        public void update(int value) {

        }

        @Override
        public void update(long value) {

        }
    }
}
