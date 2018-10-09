package ru.kontur.vostok.hercules.kafka.util.processing;

import com.codahale.metrics.Meter;
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

    private static final String CONSUMER_POOL_SCOPE = "consumerPool";

    private static final String POOL_SIZE_PARAM = "size";

    private static final int DEFAUL_POOL_SIZE = 1;

    private static final String ID_TEMPLATE = "hercules.sink.%s.%s";

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkConsumerPool.class);


    private final Meter receivedEventsMeter;
    private final Meter processedEventsMeter;
    private final Meter droppedEventsMeter;
    private final com.codahale.metrics.Timer processTimeTimer;




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
                .orElse(DEFAUL_POOL_SIZE);


        final String groupId = String.format(ID_TEMPLATE, destinationName, streamPattern.toString())
                .replaceAll("\\s+", "-");


        this.receivedEventsMeter = metricsCollector.meter("receivedEvents");
        this.processedEventsMeter = metricsCollector.meter("processedEvents");
        this.droppedEventsMeter = metricsCollector.meter("droppedEvents");
        this.processTimeTimer = metricsCollector.timer("processTime");

        this.threads = new Thread[poolSize];
        for (int i = 0; i < threads.length; ++i) {
            threads[i] = new Thread(() -> {
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
        }
    }

    public void start() {
        for (Thread thread : threads) {
            thread.start();
        }
    }

    public void stop() throws InterruptedException {
        for (Thread thread : threads) {
            thread.interrupt();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }
}
