package ru.kontur.vostok.hercules.stream.api;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.KafkaConfigs;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.DurationUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Gregory Koshelev
 */
public class ConsumerPool<K, V> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerPool.class);

    private final Properties properties;

    private final Deserializer<K> keyDeserializer;
    private final Deserializer<V> valueDeserializer;

    private final MetricsCollector metricsCollector;

    private final String bootstrapServers;
    private final int maxPollRecords;
    private final int poolSize;
    private final String metricReporterClasses;

    private ArrayBlockingQueue<Consumer<K, V>> consumers;

    public ConsumerPool(Properties properties, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer,
                        MetricsCollector metricsCollector) {
        this.properties = properties;

        this.keyDeserializer = keyDeserializer;
        this.valueDeserializer = valueDeserializer;

        this.metricsCollector = metricsCollector;

        bootstrapServers = PropertiesUtil.get(Props.BOOTSTRAP_SERVERS, properties).get();
        maxPollRecords = PropertiesUtil.get(Props.MAX_POLL_RECORDS, properties).get();
        poolSize = PropertiesUtil.get(Props.POOL_SIZE, properties).get();
        metricReporterClasses = PropertiesUtil.get(Props.METRIC_REPORTERS, properties).get();

        consumers = new ArrayBlockingQueue<>(poolSize);
    }

    public void start() {
        for (int i = 0; i < poolSize; i++) {
            consumers.offer(create());
        }
    }

    public Consumer<K, V> tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return consumers.poll(timeout, unit);
    }

    public Consumer<K, V> acquire(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        Consumer<K, V> consumer = tryAcquire(timeout, unit);
        if (consumer != null) {
            return consumer;
        }
        throw new TimeoutException();
    }

    public void release(Consumer<K, V> consumer) {
        consumer.assign(Collections.emptyList());
        consumers.offer(consumer);
    }

    public void stop(long timeout, TimeUnit unit) {
        List<Consumer<K,V>> list = new ArrayList<>(poolSize);
        int count = consumers.drainTo(list, poolSize);
        LOGGER.info("Closing " + count + " of " + poolSize + " consumers");

        Duration duration = DurationUtil.of(timeout, unit);//TODO: Should use (timeout - elapsed time) on each iteration
        for (Consumer<K,V> consumer : list) {
            try {
                consumer.wakeup();
            } catch (Exception ex) {
                LOGGER.warn("Exception on wakeup", ex);
            }

            try {
                consumer.close(duration);
            } catch (Exception ex) {
                LOGGER.warn("Exception on close", ex);
            }
        }
    }

    private Consumer<K, V> create() {
        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "stub");
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProperties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        consumerProperties.put(ConsumerConfig.METRIC_REPORTER_CLASSES_CONFIG, metricReporterClasses);
        consumerProperties.put(KafkaConfigs.METRICS_COLLECTOR_INSTANCE_CONFIG, metricsCollector);

        return new KafkaConsumer<K, V>(consumerProperties, keyDeserializer, valueDeserializer);
    }

    static final class Props {
        static final Parameter<String> BOOTSTRAP_SERVERS =
                Parameter.stringParameter("bootstrap.servers").build();

        static final Parameter<Integer> MAX_POLL_RECORDS =
                Parameter.integerParameter("max.poll.records").
                        withDefault(10_000).
                        withValidator(IntegerValidators.positive()).
                        build();

        static final Parameter<Integer> POOL_SIZE =
                Parameter.integerParameter("poolSize").
                        withDefault(4).
                        withValidator(IntegerValidators.positive()).
                        build();

        static final Parameter<String> METRIC_REPORTERS =
                Parameter.stringParameter("metric.reporters").build();
    }
}
