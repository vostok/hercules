package ru.kontur.vostok.hercules.gateway;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.partitioner.Partitioner;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class EventSender {
    private final KafkaProducer<Void, byte[]> producer;
    private final Partitioner partitioner;

    public EventSender(Map<String, Object> config, Partitioner partitioner) {
        this.producer = new KafkaProducer<>(config, new VoidSerializer(), new ByteArraySerializer());
        this.partitioner = partitioner;
    }

    public EventSender(Properties properties, Partitioner partitioner) {
        this.producer = new KafkaProducer<>(properties, new VoidSerializer(), new ByteArraySerializer());
        this.partitioner = partitioner;
    }

    public void send(Event event, String topic, int partitions, String[] shardingKey, Callback callback, Callback errorCallback) {
        Integer partition = (shardingKey.length > 0) ? partitioner.partition(event, shardingKey, partitions) : null;

        ProducerRecord<Void, byte[]> record =
                new ProducerRecord<>(
                        topic,
                        partition,
                        event.getTimestamp(),
                        null,
                        event.getBytes()
                );
        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                if (callback != null) {
                    callback.call();
                }
                return;
            }
            if (errorCallback != null) {
                errorCallback.call();
            }
            //TODO: process exception
        });
    }

    public void stop(long timeout, TimeUnit timeUnit) {
        producer.close(timeout, timeUnit);
    }
}
