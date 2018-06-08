package ru.kontur.vostok.hercules.stream.api;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import ru.kontur.vostok.hercules.kafka.util.VoidDeserializer;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.protocol.EventStreamContent;
import ru.kontur.vostok.hercules.protocol.ShardReadState;
import ru.kontur.vostok.hercules.protocol.StreamReadState;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.throwables.ThrowableUtil;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;


public class StreamReader {

    private static final Object DUMMY = new Object();

    private final StreamRepository streamRepository;
    private final ConcurrentMap<KafkaConsumer, Object> activeConsumers = new ConcurrentHashMap<>();

    private final String servers;
    private final long pollTimeout;

    public StreamReader(Properties properties, StreamRepository streamRepository) {
        this.streamRepository = streamRepository;
        this.servers = properties.getProperty("bootstrap.servers");
        this.pollTimeout = PropertiesUtil.get(properties, "poll.timeout", 1000);
    }

    public EventStreamContent getStreamContent(String streamName, StreamReadState readState, int k, int n, int take) {

        Properties props = new Properties();
        props.put("bootstrap.servers", servers);
        props.put("group.id", generateUniqueName());
        props.put("enable.auto.commit", "false");
        props.put("max.poll.records", take);
        props.put("key.deserializer", VoidDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());

        try {
            KafkaConsumer<Void, String> consumer = new KafkaConsumer<Void, String>(props);
            activeConsumers.putIfAbsent(consumer, DUMMY);

            try {
                Stream stream = streamRepository.read(streamName)
                        .orElseThrow(() -> new IllegalArgumentException(String.format("Stream '%s' not found", streamName)));

                Collection<TopicPartition> partitions = Arrays.stream(stream.partitionsForLogicalSharding(k, n))
                        .mapToObj(partition -> new TopicPartition(stream.getName(), partition))
                        .collect(Collectors.toList());

                consumer.assign(partitions);

                Map<Integer, Long> offsets = partitions.stream().collect(Collectors.toMap(TopicPartition::partition, p -> 0L));
                offsets.putAll(stateToMap(readState));

                for (TopicPartition partition : partitions) {
                    consumer.seek(partition, offsets.get(partition.partition()));
                }

                ConsumerRecords<Void, String> poll = consumer.poll(pollTimeout);

                List<String> result = new ArrayList<>(poll.count());
                for (ConsumerRecord<Void, String> record : poll) {
                    result.add(record.value());
                    offsets.put(record.partition(), record.offset() + 1);
                }

                return new EventStreamContent(
                        stateFromMap(offsets),
                        result.toArray(new String[]{})
                );
            } finally {
                consumer.close();
                activeConsumers.remove(consumer);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        activeConsumers.forEach((consumer, v) -> {
            consumer.wakeup();
        });
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

    private static String generateUniqueName() {
        return ThrowableUtil.wrapException(() -> InetAddress.getLocalHost().getHostName() + ":" +
                Thread.currentThread().getName() + ":" + System.currentTimeMillis()
        );
    }
}
