package ru.kontur.vostok.hercules.kafka.util.processing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * CommonBulkEventSink - common class for bulk processing of kafka streams content
 */
public class CommonBulkEventSink {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonBulkEventSink.class);

    private static final String PING_RATE_PARAM = "ping.rate";
    private static final int PING_RATE_DEFAULT_VALUE = 1000;

    private final BulkConsumerPool consumerPool;
    private final CommonBulkSinkStatusFsm status = new CommonBulkSinkStatusFsm();

    private final BulkSender<Event> pinger;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final int pingRate;

    /**
     * @param destinationName data flow destination name, where data must be copied
     * @param streamsProperties kafka streams properties
     * @param sinkProperties sink properties
     * @param senderFactory sender creator
     * @param metricsCollector metrics collector
     */
    public CommonBulkEventSink(
            String destinationName,
            Properties streamsProperties,
            Properties sinkProperties,
            Supplier<BulkSender<Event>> senderFactory,
            MetricsCollector metricsCollector
    ) {

        this.pingRate = PropertiesExtractor.getAs(sinkProperties, PING_RATE_PARAM, Integer.class)
                .orElse(PING_RATE_DEFAULT_VALUE);

        this.consumerPool = new BulkConsumerPool(
                destinationName,
                streamsProperties,
                sinkProperties,
                status,
                metricsCollector,
                senderFactory
        );

        metricsCollector.status("status", status::getState);
        this.pinger = senderFactory.get();
    }

    /**
     * Start sink
     */
    public void start() {
        this.executor.scheduleAtFixedRate(this::ping, 0, pingRate, TimeUnit.MILLISECONDS);
        consumerPool.start();
        status.markInitCompleted();
    }

    /**
     * Stop sink
     * @param timeout
     * @param timeUnit
     */
    public void stop(int timeout, TimeUnit timeUnit) throws InterruptedException {
        status.stop();
        consumerPool.stop();
        executor.shutdown();
    }

    private void ping() {
        try {
            if (pinger.ping()) {
                status.markBackendAlive();
            }
            else {
                status.markBackendFailed();
            }
        }
        catch (Throwable e) {
            LOGGER.error("Ping error should never happen, stopping service", e);
            System.exit(1);
            throw e;
        }
    }
}
