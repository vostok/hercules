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

import java.util.Properties;
import java.util.UUID;

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

    private static final String ID_TEMPLATE = "hercules.sink.%s.%s";

    private final Thread[] threads;

    public BulkConsumerPool(
            String destinationName,
            PatternMatcher streamPattern,
            Properties consumerProperties,
            Properties sinkProperties,
            CommonBulkSinkStatusFsm status,
            MetricsCollector metricsCollector,
            BulkQueue<UUID, Event> queue
    ) {
        final Properties consumerPoolProperties = PropertiesUtil.ofScope(sinkProperties, CONSUMER_POOL_SCOPE);

        final int poolSize = PropertiesExtractor.getAs(consumerPoolProperties, POOL_SIZE_PARAM, Integer.class)
                .orElse(POOL_SIZE_DEFAULT_VALUE);

        final String groupId = String.format(ID_TEMPLATE, destinationName, streamPattern.toString())
                .replaceAll("\\s+", "-");


        final Meter receivedEventsMeter = metricsCollector.meter("receivedEvents");
        final Meter processedEventsMeter = metricsCollector.meter("processedEvents");
        final Meter droppedEventsMeter = metricsCollector.meter("droppedEvents");
        final Timer processTimeTimer = metricsCollector.timer("processTime");

        this.threads = new Thread[poolSize];
        for (int i = 0; i < threads.length; ++i) {
            Thread thread = new Thread(() -> {
                BulkConsumer bulkConsumer = new BulkConsumer(
                        consumerProperties,
                        sinkProperties,
                        streamPattern,
                        groupId,
                        status,
                        receivedEventsMeter,
                        processedEventsMeter,
                        droppedEventsMeter,
                        processTimeTimer,
                        queue
                );
                bulkConsumer.run();
            });
            thread.setName(String.format("consumer-pool-%d", i));
            thread.setUncaughtExceptionHandler(ExitOnThrowableHandler.INSTANCE);
            threads[i] = thread;
        }
    }

    public void start() {
        for (Thread thread : threads) {
            thread.start();
        }
    }

    public void stop() throws InterruptedException {
        for (Thread thread : threads) {
            thread.join();
        }
    }
}
