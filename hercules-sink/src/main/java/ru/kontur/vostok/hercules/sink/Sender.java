package ru.kontur.vostok.hercules.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public abstract class Sender {
    private static final Logger LOGGER = LoggerFactory.getLogger(Sender.class);

    private volatile SenderStatus status = SenderStatus.AVAILABLE;
    private final Object mutex = new Object();

    private final MetricsCollector metricsCollector;

    private final long pingPeriodMs;
    private final ScheduledExecutorService executor;

    /**
     * Sender.
     *
     * @param properties sender's properties.
     * @param metricsCollector metrics collector
     */
    public Sender(Properties properties, MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;

        this.pingPeriodMs = Props.PING_PERIOD_MS.extract(properties);
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
    public final SenderResult process(List<Event> events) {
        try {
            int processedEvents = send(events);
            return SenderResult.ok(processedEvents, events.size() - processedEvents);
        } catch (BackendServiceFailedException ex) {
            LOGGER.error("Backend failed with exception", ex);
            status = SenderStatus.UNAVAILABLE;
            return SenderResult.fail();
        }
    }

    /**
     * Return {@code true} if sender is available.
     *
     * @return {@code true} if sender is available, {@code false} otherwise
     */
    public final boolean isAvailable() {
        return status == SenderStatus.AVAILABLE;
    }

    /**
     * Wait for sender's availability
     *
     * @param timeoutMs maximum time (in millis) to wait
     * @return {@code true} if sender is available, {@code false} otherwise
     */
    public boolean awaitAvailability(long timeoutMs) {
        if (isAvailable()) {
            return true;
        }

        long nanoTime = System.nanoTime();
        long remainingTimeoutMs;
        synchronized (mutex) {
            if (isAvailable()) {
                return true;
            }
            while ((remainingTimeoutMs = timeoutMs - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime)) > 0
                    && !isAvailable()) {
                try {
                    mutex.wait(remainingTimeoutMs);
                } catch (InterruptedException e) {
                    /* Interruption during shutdown */
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return isAvailable();

    }

    /**
     * Check sender's availability status. Sender is available by default.
     */
    protected SenderStatus ping() {
        return SenderStatus.AVAILABLE;
    }

    /**
     * Send events to some backend (storage or processing unit).
     *
     * @param events events to be sent
     * @return count of events were successfully processed by backend
     * @throws BackendServiceFailedException if backend failed
     */
    protected abstract int send(List<Event> events) throws BackendServiceFailedException;

    private void updateStatus() {
        SenderStatus status = ping();
        synchronized (mutex) {
            this.status = status;
            mutex.notifyAll();
        }
    }

    private static class Props {
        static final PropertyDescription<Long> PING_PERIOD_MS =
                PropertyDescriptions.longProperty("pingPeriodMs").withDefaultValue(5_000L).build();
    }
}
