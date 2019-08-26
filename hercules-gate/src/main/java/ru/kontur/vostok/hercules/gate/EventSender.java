package ru.kontur.vostok.hercules.gate;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.KafkaConfigs;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerializer;
import ru.kontur.vostok.hercules.partitioner.Partitioner;
import ru.kontur.vostok.hercules.partitioner.ShardingKey;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class EventSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventSender.class);

    private final KafkaProducer<UUID, byte[]> producer;
    private final Partitioner partitioner;

    public EventSender(Map<String, Object> config, Partitioner partitioner) {
        this.producer = new KafkaProducer<>(config, new UuidSerializer(), new ByteArraySerializer());
        this.partitioner = partitioner;
    }

    public EventSender(Properties properties, Partitioner partitioner, MetricsCollector metricsCollector) {
        properties.put(KafkaConfigs.METRICS_COLLECTOR_INSTANCE_CONFIG, metricsCollector);
        this.producer = new KafkaProducer<>(properties, new UuidSerializer(), new ByteArraySerializer());

        this.partitioner = partitioner;
    }

    public void send(Event event, UUID eventId, String topic, int partitions, ShardingKey shardingKey, Callback callback, Callback errorCallback) {
        Integer partition = (!shardingKey.isEmpty()) ? partitioner.partition(event, shardingKey, partitions) : null;

        ProducerRecord<UUID, byte[]> record =
                new ProducerRecord<>(
                        topic,
                        partition,
                        System.currentTimeMillis(),// Use current timestamp of the Gate
                        eventId,
                        event.getBytes()
                );
        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                if (callback != null) {
                    callback.call();
                }
            } else {
                if (errorCallback != null) {
                    errorCallback.call();
                }

                LOGGER.error("Error on event send", exception);
                //TODO: process exception
            }
        });
    }

    public void stop(long timeout, TimeUnit timeUnit) {
        producer.close(timeout, timeUnit);
    }
}
