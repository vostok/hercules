package ru.kontur.vostok.hercules.gate;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.KafkaConfigs;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerializer;
import ru.kontur.vostok.hercules.partitioner.BatchedPerThreadPartitioner;
import ru.kontur.vostok.hercules.partitioner.Partitioner;
import ru.kontur.vostok.hercules.partitioner.RandomPartitioner;
import ru.kontur.vostok.hercules.partitioner.RoundRobinPartitioner;
import ru.kontur.vostok.hercules.partitioner.ShardingKey;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.lifecycle.Stoppable;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
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
    private final Partitioner defaultPartitioner;

    private final EventSenderMetrics metrics;

    public EventSender(Properties properties, Partitioner partitioner, MetricsCollector metricsCollector) {
        Properties producerProperties = PropertiesUtil.ofScope(properties, Scopes.PRODUCER);
        producerProperties.put(KafkaConfigs.METRICS_COLLECTOR_INSTANCE_CONFIG, metricsCollector);
        this.producer = new KafkaProducer<>(producerProperties, new UuidSerializer(), new ByteArraySerializer());

        this.partitioner = partitioner;
        this.defaultPartitioner = getDefaultPartitionerFromProperties(PropertiesUtil.ofScope(properties, Props.defaultPartitionerScope()));

        this.metrics = new EventSenderMetrics(metricsCollector);
    }

    public void send(Event event, UUID eventId, String topic, int partitions, ShardingKey shardingKey, Callback callback, Callback errorCallback) {
        Integer partition = partition(event, shardingKey, partitions);

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

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        producer.close(timeout, timeUnit);
        return true;
    }

    /**
     * Get partition for an event.
     *
     * @param event event
     * @param shardingKey sharding key
     * @param partitions partitions
     * @return partition number or {@code null}
     */
    @Nullable
    private Integer partition(Event event, ShardingKey shardingKey, int partitions) {
        if (!shardingKey.isEmpty()) {
            return partitioner.partition(event, shardingKey, partitions);
        }
        if (defaultPartitioner != null) {
            return defaultPartitioner.partition(event, shardingKey, partitions);
        }
        return null;
    }

    @Nullable
    private Partitioner getDefaultPartitionerFromProperties(Properties properties) {
        PartitionerType partitionerType = PropertiesUtil.get(Props.DEFAULT_PARTITIONER_TYPE, properties).get();
        switch (partitionerType) {
            case KAFKA_DEFAULT:
                return null;
            case BATCHED:
                return new BatchedPerThreadPartitioner(PropertiesUtil.get(Props.DEFAULT_PARTITIONER_BATCH_SIZE, properties).get());
            case RANDOM:
                return new RandomPartitioner();
            case ROUND_ROBIN:
                return new RoundRobinPartitioner();
            default:
                throw new IllegalArgumentException("Unsupported partitioner type " + partitionerType);
        }
    }

    private static class Props {
        static final Parameter<PartitionerType> DEFAULT_PARTITIONER_TYPE =
                Parameter.enumParameter("type", PartitionerType.class).
                        withDefault(PartitionerType.KAFKA_DEFAULT).
                        build();
        static final Parameter<Integer> DEFAULT_PARTITIONER_BATCH_SIZE =
                Parameter.integerParameter("batch.size").
                        withDefault(16).
                        build();

        static String defaultPartitionerScope() {
            return "default.partitioner";
        }
    }
}
