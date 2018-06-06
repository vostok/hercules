package ru.kontur.vostok.hercules.stream.api;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.KafkaStreams;
import ru.kontur.vostok.hercules.kafka.util.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.VoidDeserializer;
import ru.kontur.vostok.hercules.protocol.EventStreamContent;
import ru.kontur.vostok.hercules.protocol.ShardReadState;
import ru.kontur.vostok.hercules.protocol.StreamReadState;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class StreamReader {

//    private final KafkaConsumer<Void, byte[]> consumer;
    private final Partitioner partitioner;

    public StreamReader(Properties properties, Partitioner partitioner) {
  //      this.consumer = new KafkaConsumer<Void, byte[]>(properties);
        this.partitioner = partitioner;
    }


    public EventStreamContent getStreamContent(String streamName, int take) {

        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "consumer-tutorial");
        props.put("enable.auto.commit", "false");
        props.put("max.poll.records", take);
        props.put("key.deserializer", VoidDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());

        try (KafkaConsumer<Void, String> consumer = new KafkaConsumer<Void, String>(props)) {

            List<TopicPartition> partitions = consumer.partitionsFor(streamName).stream()
                    .map(partitionInfo -> new TopicPartition(partitionInfo.topic(), partitionInfo.partition()))
                    .sorted(Comparator.comparing(TopicPartition::partition))
                    .collect(Collectors.toList());

            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);

            ConsumerRecords<Void, String> poll = consumer.poll(1000);

            Map<Integer, Long> offsets = new HashMap<>();

            List<String> result = new ArrayList<>(poll.count());
            for (ConsumerRecord<Void, String> record : poll) {
                // FIXME
                result.add(record.value());
                offsets.put(record.partition(), record.offset());
            }
            return new EventStreamContent(
                    new StreamReadState(
                            offsets.size(),
                            offsets.entrySet().stream()
                                    .map(e -> new ShardReadState(e.getKey(), e.getValue()))
                                    .collect(Collectors.toList())
                                    .toArray(new ShardReadState[]{})
                    ),
                    result.toArray(new String[]{})
            );
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }





    public void stop(long timeout, TimeUnit timeUnit) {
        //consumer.close(timeout, timeUnit);
    }
}
