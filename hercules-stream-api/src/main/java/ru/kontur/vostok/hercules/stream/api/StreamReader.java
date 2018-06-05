package ru.kontur.vostok.hercules.stream.api;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Partitioner;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class StreamReader {

    private final KafkaConsumer<Void, byte[]> consumer;
    private final Partitioner partitioner;

    public StreamReader(Properties properties, Partitioner partitioner) {
        this.consumer = new KafkaConsumer<Void, byte[]>(properties);
        this.partitioner = partitioner;
    }



    public void stop(long timeout, TimeUnit timeUnit) {
        consumer.close(timeout, timeUnit);
    }
}
