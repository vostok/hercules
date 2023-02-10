package ru.kontur.vostok.hercules.sink.parallel.sender;

import ru.kontur.vostok.hercules.health.IMetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.List;
import java.util.Properties;

/**
 * Sender with prepare and send steps
 *
 * @author Innokentiy Krivonosov
 */
public abstract class ParallelSender<T extends PreparedData> extends AbstractParallelSender<T> {

    /**
     * Sender.
     *
     * @param properties       sender's properties.
     * @param metricsCollector metrics collector
     */
    public ParallelSender(Properties properties, IMetricsCollector metricsCollector) {
        super(properties, metricsCollector);
    }

    /**
     * Method is not used in parallel processing
     *
     * @param events events to be sent
     * @return count of events were successfully processed by backend
     * @throws BackendServiceFailedException if backend failed
     */
    @Override
    protected final int send(List<Event> events) throws BackendServiceFailedException {
        return send(prepare(events));
    }
}
