package ru.kontur.vostok.hercules.sink;

import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.Timer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Sink pool.
 *
 * @author Gregory Koshelev
 */
public class SinkPool implements Lifecycle {
    private final int poolSize;
    private final List<Sink> sinks;

    /**
     *
     * @param poolSize size of pool
     * @param sinkSupplier supplier must provide new Sink instance on each invoke of get()
     */
    public SinkPool(int poolSize, Supplier<Sink> sinkSupplier) {
        this.poolSize = poolSize;

        this.sinks = createSinks(poolSize, sinkSupplier);
    }

    @Override
    public void start() {
        sinks.forEach(Sink::start);
    }

    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        Timer timer = TimeSource.SYSTEM.timer(unit.toMillis(timeout));//FIXME: better use TimeSource is passed via constructor

        boolean stopped = true;
        for (Sink sink : sinks) {
            stopped = sink.stop(timer.remainingTimeMs(), TimeUnit.MILLISECONDS) && stopped;
        }
        return stopped;
    }

    private static List<Sink> createSinks(int poolSize, Supplier<Sink> sinkSupplier) {
        List<Sink> sinks = new ArrayList<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            sinks.add(sinkSupplier.get());
        }

        return sinks;
    }
}
