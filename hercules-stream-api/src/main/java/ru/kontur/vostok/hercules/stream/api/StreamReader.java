package ru.kontur.vostok.hercules.stream.api;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import ru.kontur.vostok.hercules.kafka.util.VoidDeserializer;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.protocol.EventStreamContent;
import ru.kontur.vostok.hercules.protocol.ShardReadState;
import ru.kontur.vostok.hercules.protocol.StreamReadState;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class StreamReader {

//    private final KafkaConsumer<Void, byte[]> consumer;
    private final Partitioner partitioner;
    private final StreamRepository streamRepository;

    public StreamReader(Properties properties, Partitioner partitioner, StreamRepository streamRepository) {
  //      this.consumer = new KafkaConsumer<Void, byte[]>(properties);
        this.partitioner = partitioner;
        this.streamRepository = streamRepository;
    }


    public EventStreamContent getStreamContent(String streamName, StreamReadState readState, int k, int n, int take) {

        // FIXME How does it works with multi threading?

        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "consumer-tutorial");
        props.put("enable.auto.commit", "false");
        props.put("max.poll.records", take);
        props.put("key.deserializer", VoidDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());

        try (KafkaConsumer<Void, String> consumer = new KafkaConsumer<Void, String>(props)) {

            Stream stream = streamRepository.read(streamName).get();

            Collection<TopicPartition> partitions = Arrays.stream(stream.partitionsForLogicalSharding(k, n))
                    .mapToObj(partition -> new TopicPartition(stream.getName(), partition))
                    .collect(Collectors.toList());

            consumer.assign(partitions);

            Map<Integer, Long> offsets = partitions.stream().collect(Collectors.toMap(TopicPartition::partition, p -> 0L));
            offsets.putAll(stateToMap(readState));

            for (TopicPartition partition : partitions) {
                consumer.seek(partition, offsets.get(partition.partition()));
            }

            ConsumerRecords<Void, String> poll = consumer.poll(1000); //FIXME: to config

            List<String> result = new ArrayList<>(poll.count());
            for (ConsumerRecord<Void, String> record : poll) {
                result.add(record.value());
                offsets.put(record.partition(), record.offset() + 1);

            }
            return new EventStreamContent(
                    stateFromMap(offsets),
                    result.toArray(new String[]{})
            );
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void stop(long timeout, TimeUnit timeUnit) {
        //consumer.close(timeout, timeUnit);
    }

    private static Map<Integer, Long> stateToMap(StreamReadState state) {
        return Arrays.stream(state.getShardStates())
                .collect(Collectors.toMap(ShardReadState::getPartition, ShardReadState::getOffset));
    }

    private static StreamReadState stateFromMap(Map<Integer, Long> map) {
        return new StreamReadState(
                map.entrySet().stream()
                    .map(e -> new ShardReadState(e.getKey(), e.getValue()))
                    .collect(Collectors.toList())
                    .toArray(new ShardReadState[]{})
        );
    }
}
