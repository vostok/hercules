package ru.kontur.vostok.hercules.sink.parallel.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.IMetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.ProcessorResult;
import ru.kontur.vostok.hercules.sink.Sender;

import java.util.List;
import java.util.Properties;

/**
 * Base class for parallel sender.
 * Adds abstract methods to processing in two steps: prepare and send {@link PreparedData}.
 *
 * @author Innokentiy Krivonosov
 * @see ParallelSender
 * @see NoPrepareParallelSender
 */
public abstract class AbstractParallelSender<T extends PreparedData> extends Sender {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractParallelSender.class);

    /**
     * Sender.
     *
     * @param properties       sender's properties.
     * @param metricsCollector metrics collector
     */
    AbstractParallelSender(Properties properties, IMetricsCollector metricsCollector) {
        super(properties, metricsCollector);
    }

    /**
     * Prepare batch of events
     *
     * @param events events to be sent
     * @return prepared batch of events
     */
    public abstract T prepare(List<Event> events);

    /**
     * Send prepared batch of events to some backend (storage or processing unit).
     *
     * @param preparedData prepared batch of events
     * @return count of events were successfully processed by backend
     * @throws BackendServiceFailedException if backend failed
     */
    public abstract int send(T preparedData) throws BackendServiceFailedException;

    /**
     * Process send batch of events in parallel sink.
     *
     * @param events events to be processed
     * @return result of processing
     * @see #process(List) base sink analog
     */
    public final ProcessorResult processSend(T events) {
        try {
            int processedEvents = send(events);
            return ProcessorResult.ok(processedEvents, events.getEventsCount() - processedEvents);
        } catch (BackendServiceFailedException ex) {
            LOGGER.error("Backend failed with exception", ex);
            disable();
            return ProcessorResult.fail();
        }
    }
}
