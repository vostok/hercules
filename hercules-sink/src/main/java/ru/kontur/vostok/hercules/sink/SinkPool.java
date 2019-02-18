package ru.kontur.vostok.hercules.sink;

import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Sink pool.
 *
 * @author Gregory Koshelev
 */
public class SinkPool {
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

    public void start() {
        sinks.forEach(Sink::start);
    }

    public void stop() {
        sinks.forEach(Sink::stop);
    }

    private static List<Sink> createSinks(int poolSize, Supplier<Sink> sinkSupplier) {
        List<Sink> sinks = new ArrayList<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            sinks.add(sinkSupplier.get());
        }

        return sinks;
    }
}
