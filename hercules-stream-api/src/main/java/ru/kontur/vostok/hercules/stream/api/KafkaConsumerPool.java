package ru.kontur.vostok.hercules.stream.api;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.time.DurationUtil;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Gregory Koshelev
 */
public class KafkaConsumerPool<K, V> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumerPool.class);

    private final Properties properties;

    private final Deserializer<K> keyDeserializer;
    private final Deserializer<V> valueDeserializer;

    private final String bootstrapServers;
    private final int maxPollRecords;
    private final int poolSize;

    private ArrayBlockingQueue<KafkaConsumer<K, V>> consumers;

    public KafkaConsumerPool(Properties properties, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer) {
        this.properties = properties;

        this.keyDeserializer = keyDeserializer;
        this.valueDeserializer = valueDeserializer;

        bootstrapServers = Props.BOOTSTRAP_SERVERS.extract(properties);
        maxPollRecords = Props.MAX_POLL_RECORDS.extract(properties);
        poolSize = Props.POOL_SIZE.extract(properties);

        consumers = new ArrayBlockingQueue<>(poolSize);
    }

    public void start() {
        for (int i = 0; i < poolSize; i++) {
            consumers.offer(create());
        }
    }

    public KafkaConsumer<K, V> tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return consumers.poll(timeout, unit);
    }

    public KafkaConsumer<K, V> acquire(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        KafkaConsumer<K, V> consumer = tryAcquire(timeout, unit);
        if (consumer != null) {
            return consumer;
        }
        throw new TimeoutException();
    }

    public void release(KafkaConsumer<K, V> consumer) {
        consumers.offer(consumer);
    }

    public void stop(long timeout, TimeUnit unit) {
        List<KafkaConsumer<K,V>> list = new ArrayList<>(poolSize);
        int count = consumers.drainTo(list, poolSize);
        LOGGER.info("Closing " + count + " of " + poolSize + " consumers");

        Duration duration = DurationUtil.of(timeout, unit);//TODO: Should use elapsed time on each iteration
        for (KafkaConsumer<K,V> consumer : list) {
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

    private KafkaConsumer<K, V> create() {
        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "stub");
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProperties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);

        return new KafkaConsumer<K, V>(consumerProperties, keyDeserializer, valueDeserializer);
    }

    static final class Props {
        static final PropertyDescription<String> BOOTSTRAP_SERVERS =
                PropertyDescriptions.stringProperty("bootstrap.servers").build();

        static final PropertyDescription<Integer> MAX_POLL_RECORDS =
                PropertyDescriptions.integerProperty("max.poll.records").
                        withDefaultValue(1_000).
                        withValidator(Validators.interval(1, 100_000)).
                        build();

        static final PropertyDescription<Integer> POOL_SIZE =
                PropertyDescriptions.integerProperty("poolSize").
                        withDefaultValue(4).
                        withValidator(Validators.greaterThan(0)).
                        build();
    }
}
