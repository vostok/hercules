package ru.kontur.vostok.hercules.gate;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.KafkaConfigs;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerializer;
import ru.kontur.vostok.hercules.partitioner.Partitioner;
import ru.kontur.vostok.hercules.partitioner.ShardingKey;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.lifecycle.Stoppable;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class EventSender implements Stoppable {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventSender.class);

    private final KafkaProducer<UUID, byte[]> producer;
    private final Partitioner partitioner;

    private final EventSenderMetrics metrics;

    public EventSender(Properties properties, Partitioner partitioner, MetricsCollector metricsCollector) {
        Properties producerProperties = PropertiesUtil.ofScope(properties, Scopes.PRODUCER);
        producerProperties.put(KafkaConfigs.METRICS_COLLECTOR_INSTANCE_CONFIG, metricsCollector);
        this.producer = new KafkaProducer<>(producerProperties, new UuidSerializer(), new ByteArraySerializer());

        this.partitioner = partitioner;

        this.metrics = new EventSenderMetrics(metricsCollector);
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
        metrics.updateSent(event);
        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                metrics.markDelivered();
                if (callback != null) {
                    callback.call();
                }
            } else {
                metrics.markFailed();
                if (errorCallback != null) {
                    errorCallback.call();
                }

                LOGGER.error("Error on event send", exception);
                //TODO: process exception
            }
        });
    }

    public boolean stop(long timeout, TimeUnit timeUnit) {
        producer.close(timeout, timeUnit);
        return true;
    }
}
