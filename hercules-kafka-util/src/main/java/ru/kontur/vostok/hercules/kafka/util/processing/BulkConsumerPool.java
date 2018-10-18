package ru.kontur.vostok.hercules.kafka.util.processing;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.PatternMatcher;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * BulkConsumerPool
 *
 * @author Kirill Sulim
 */
public class BulkConsumerPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkConsumerPool.class);

    private static final String CONSUMER_POOL_SCOPE = "consumerPool";

    private static final String POOL_SIZE_PARAM = "size";
    private static final int POOL_SIZE_DEFAULT_VALUE = 2;

    private static final String SHUTDOWN_TIMEOUT_MS_PARAM = "shutdownTimeoutMs";
    private static final int SHUTDOWN_TIMEOUT_MS_DEFAULT_VALUE = 5_000;

    private static final String PATTERN_PARAM = "pattern";

    private static final String ID_TEMPLATE = "hercules.sink.%s.%s";

    private final AtomicLong id = new AtomicLong(0);

    private final int poolSize;
    private final int shutdownTimeoutMs;

    private final ExecutorService pool;
    private final Supplier<BulkConsumer> bulkConsumerSupplier;
    private final List<BulkConsumer> consumers;

    public BulkConsumerPool(
            String destinationName,
            Properties consumerProperties,
            Properties sinkProperties,
            CommonBulkSinkStatusFsm status,
            MetricsCollector metricsCollector,
            Supplier<BulkSender<Event>> senderSupplier
    ) {
        final Properties consumerPoolProperties = PropertiesUtil.ofScope(sinkProperties, CONSUMER_POOL_SCOPE);

        this.poolSize = PropertiesExtractor.getAs(consumerPoolProperties, POOL_SIZE_PARAM, Integer.class)
                .orElse(POOL_SIZE_DEFAULT_VALUE);

        this.shutdownTimeoutMs = PropertiesExtractor.getAs(consumerPoolProperties, SHUTDOWN_TIMEOUT_MS_PARAM, Integer.class)
                .orElse(SHUTDOWN_TIMEOUT_MS_DEFAULT_VALUE);

        final String streamPatternString = PropertiesExtractor.getRequiredProperty(consumerPoolProperties, PATTERN_PARAM, String.class);

        final PatternMatcher streamPattern = new PatternMatcher(streamPatternString);

        final String groupId = String.format(ID_TEMPLATE, destinationName, streamPattern.toString())
                .replaceAll("\\s+", "-");


        final Meter receivedEventsMeter = metricsCollector.meter("receivedEvents");
        final Meter receivedEventsSizeMeter = metricsCollector.meter("receivedEventsSize");
        final Meter processedEventsMeter = metricsCollector.meter("processedEvents");
        final Meter droppedEventsMeter = metricsCollector.meter("droppedEvents");
        final Timer processTimeTimer = metricsCollector.timer("processTime");

        this.pool = Executors.newFixedThreadPool(poolSize, new NamedThreadFactory("consumer-pool"));
        this.consumers = new ArrayList<>(poolSize);
        this.bulkConsumerSupplier = () -> new BulkConsumer(
                String.valueOf(id.getAndIncrement()),
                consumerProperties,
                sinkProperties,
                streamPattern,
                groupId,
                status,
                senderSupplier,
                receivedEventsMeter,
                receivedEventsSizeMeter,
                processedEventsMeter,
                droppedEventsMeter,
                processTimeTimer
        );
    }

    public void start() {
        for (int i = 0; i < poolSize; ++i) {
            BulkConsumer consumer = bulkConsumerSupplier.get();
            consumers.add(consumer);
            pool.execute(consumer);
        }
    }

    public void stop() throws InterruptedException {
        consumers.forEach(BulkConsumer::wakeup);
        pool.shutdown();
        if (!pool.awaitTermination(shutdownTimeoutMs, TimeUnit.MILLISECONDS)) {
            LOGGER.warn("Consumer pool was terminated by force");
        }
    }
}
