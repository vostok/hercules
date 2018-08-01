package ru.kontur.vostok.hercules.stream.api;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.VoidDeserializer;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.partitioner.LogicalPartitioner;
import ru.kontur.vostok.hercules.protocol.*;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.throwable.ThrowableUtil;

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

    // TODO: Probably we can use output streams to reduce memory consumption
    public ByteStreamContent getStreamContent(String streamName, StreamReadState readState, int k, int n, int take) {

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, generateUniqueName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, take);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, VoidDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        try {
            KafkaConsumer<Void, byte[]> consumer = new KafkaConsumer<>(props);
            activeConsumers.putIfAbsent(consumer, DUMMY);

            try {
                Stream stream = streamRepository.read(streamName)
                        .orElseThrow(() -> new IllegalArgumentException(String.format("Stream '%s' not found", streamName)));

                Collection<TopicPartition> partitions = Arrays.stream(LogicalPartitioner.getPartitionsForLogicalSharding(stream, k, n))
                        .mapToObj(partition -> new TopicPartition(stream.getName(), partition))
                        .collect(Collectors.toList());

                // TODO: Add partition exist checking
                Map<TopicPartition, Long> offsets = consumer.beginningOffsets(partitions);
                for (Map.Entry<TopicPartition, Long> entry : stateToMap(streamName, readState).entrySet()) {
                    Long requestOffset = entry.getValue();
                    Long beginningOffset = offsets.get(entry.getKey());
                    if (beginningOffset < requestOffset) {
                        offsets.put(entry.getKey(), entry.getValue());
                    }
                }

                consumer.assign(partitions);

                for (TopicPartition partition : partitions) {
                    consumer.seek(partition, offsets.get(partition));
                }

                ConsumerRecords<Void, byte[]> poll = consumer.poll(pollTimeout);

                Map<Integer, Long> offsetsByPartition = new HashMap<>();
                List<byte[]> result = new ArrayList<>(poll.count());
                for (ConsumerRecord<Void, byte[]> record : poll) {
                    result.add(record.value());
                    offsetsByPartition.put(record.partition(), record.offset() + 1);
                }
                offsetsByPartition.forEach((partition, offset) -> offsets.put(new TopicPartition(streamName, partition), offset));

                return new ByteStreamContent(
                        stateFromMap(streamName, offsets),
                        result.toArray(new byte[][]{})
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

    private static Map<TopicPartition, Long> stateToMap(String streamName, StreamReadState state) {
        return Arrays.stream(state.getShardStates())
                .collect(Collectors.toMap(
                        shardState -> new TopicPartition(streamName, shardState.getPartition()),
                        StreamShardReadState::getOffset
                ));
    }

    private static StreamReadState stateFromMap(String streamName, Map<TopicPartition, Long> map) {
        return new StreamReadState(
                map.entrySet().stream()
                        .filter(e -> e.getKey().topic().equals(streamName))
                        .map(e -> new StreamShardReadState(e.getKey().partition(), e.getValue()))
                        .collect(Collectors.toList())
                        .toArray(new StreamShardReadState[]{})
        );
    }

    private static String generateUniqueName() {
        return ThrowableUtil.toUnchecked(() -> InetAddress.getLocalHost().getHostName() + ":" +
                Thread.currentThread().getName() + ":" + System.currentTimeMillis()
        );
    }
}
