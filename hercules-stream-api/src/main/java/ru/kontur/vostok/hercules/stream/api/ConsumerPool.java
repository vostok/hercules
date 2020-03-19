package ru.kontur.vostok.hercules.stream.api;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.Scopes;
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

    private final Deserializer<K> keyDeserializer;
    private final Deserializer<V> valueDeserializer;

    private final int size;
    private final Properties consumerProperties;

    private ArrayBlockingQueue<Consumer<K, V>> consumers;

    public ConsumerPool(Properties properties, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer,
                        MetricsCollector metricsCollector) {
        this.keyDeserializer = keyDeserializer;
        this.valueDeserializer = valueDeserializer;

        size = PropertiesUtil.get(Props.SIZE, properties).get();

        consumerProperties = PropertiesUtil.ofScope(properties, Scopes.CONSUMER);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "stub");
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProperties.put(KafkaConfigs.METRICS_COLLECTOR_INSTANCE_CONFIG, metricsCollector);
        consumerProperties.putIfAbsent(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10_000);

        consumers = new ArrayBlockingQueue<>(size);
    }

    public void start() {
        for (int i = 0; i < size; i++) {
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
        List<Consumer<K, V>> list = new ArrayList<>(size);
        int count = consumers.drainTo(list, size);
        LOGGER.info("Closing " + count + " of " + size + " consumers");

        Duration duration = DurationUtil.of(timeout, unit);//TODO: Should use (timeout - elapsed time) on each iteration
        for (Consumer<K, V> consumer : list) {
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
        return new KafkaConsumer<>(consumerProperties, keyDeserializer, valueDeserializer);
    }

    static final class Props {
        static final Parameter<Integer> SIZE =
                Parameter.integerParameter("size").
                        withDefault(4).
                        withValidator(IntegerValidators.positive()).
                        build();
    }
}
