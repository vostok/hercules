package ru.kontur.vostok.hercules.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public abstract class Sender extends Processor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Sender.class);

    private final MetricsCollector metricsCollector;

    private final long pingPeriodMs;
    private final ScheduledExecutorService executor;

    /**
     * Sender.
     *
     * @param properties       sender's properties.
     * @param metricsCollector metrics collector
     */
    public Sender(Properties properties, MetricsCollector metricsCollector) {
        this(properties, metricsCollector, TimeSource.SYSTEM);
    }

    Sender(Properties properties, MetricsCollector metricsCollector, TimeSource time) {
        super(time);

        this.metricsCollector = metricsCollector;

        this.pingPeriodMs = PropertiesUtil.get(Props.PING_PERIOD_MS, properties).get();
        this.executor = Executors.newSingleThreadScheduledExecutor();//TODO: Should provide ThreadFactory
    }

    /**
     * Start sender. Activate periodical update availability status.
     */
    public void start() {
        executor.scheduleAtFixedRate(this::updateStatus, pingPeriodMs, pingPeriodMs, TimeUnit.MILLISECONDS);
        metricsCollector.gauge("status", () -> isAvailable() ? 0 : 1);
    }

    /**
     * Stop sender. Disable periodical update availability status.
     *
     * @param timeout maximum time to wait
     * @param unit    time unit of the timeout
     * @return {@code true} if sender stopped normally, {@code false} if the timeout elapsed or the current thread was interrupted
     */
    public boolean stop(long timeout, TimeUnit unit) {
        executor.shutdown();
        try {
            return executor.awaitTermination(timeout, unit);
        } catch (InterruptedException ex) {
            return false;
        }
    }

    /**
     * Process batch of events.
     *
     * @param events events to be processed
     * @return result of processing
     */
    @Override
    public final ProcessorResult process(List<Event> events) {
        try {
            int processedEvents = send(events);
            return ProcessorResult.ok(processedEvents, events.size() - processedEvents);
        } catch (BackendServiceFailedException ex) {
            LOGGER.error("Backend failed with exception", ex);
            disable();
            return ProcessorResult.fail();
        }
    }

    protected final void updateStatus() {
        status(ping());
    }

    /**
     * Ping backend to update availability status of the sender. Sender is available by default.
     */
    protected ProcessorStatus ping() {
        return ProcessorStatus.AVAILABLE;
    }

    /**
     * Send events to some backend (storage or processing unit).
     *
     * @param events events to be sent
     * @return count of events were successfully processed by backend
     * @throws BackendServiceFailedException if backend failed
     */
    protected abstract int send(List<Event> events) throws BackendServiceFailedException;

    private static class Props {
        static final Parameter<Long> PING_PERIOD_MS =
                Parameter.longParameter("pingPeriodMs").
                        withDefault(5_000L).
                        build();
    }
}
