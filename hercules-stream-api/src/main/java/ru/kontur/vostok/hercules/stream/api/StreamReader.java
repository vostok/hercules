package ru.kontur.vostok.hercules.stream.api;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import ru.kontur.vostok.hercules.kafka.util.processing.RecordStorage;
import ru.kontur.vostok.hercules.kafka.util.serialization.VoidDeserializer;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.partitioner.LogicalPartitioner;
import ru.kontur.vostok.hercules.protocol.ByteStreamContent;
import ru.kontur.vostok.hercules.protocol.StreamReadState;
import ru.kontur.vostok.hercules.protocol.StreamShardReadState;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.throwable.ThrowableUtil;
import ru.kontur.vostok.hercules.util.time.Timer;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class StreamReader {

    private static class Props {
        static final PropertyDescription<String> SERVERS = PropertyDescriptions
                .stringProperty("bootstrap.servers")
                .build();

        static final PropertyDescription<Integer> POLL_TIMEOUT_MS = PropertyDescriptions
                .integerProperty("poll.timeout")
                .withDefaultValue(1_000)
                .withValidator(Validators.greaterOrEquals(0))
                .build();
    }

    private static final Object DUMMY = new Object();

    private final StreamRepository streamRepository;
    private final ConcurrentMap<KafkaConsumer, Object> activeConsumers = new ConcurrentHashMap<>();

    private final String servers;
    private final long pollTimeout;

    public StreamReader(Properties properties, StreamRepository streamRepository) {
        this.streamRepository = streamRepository;
        this.servers = Props.SERVERS.extract(properties);
        this.pollTimeout = Props.POLL_TIMEOUT_MS.extract(properties);
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
                Map<TopicPartition, Long> beginningOffsets = consumer.beginningOffsets(partitions);
                Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);
                Map<TopicPartition, Long> offsetsToRequest = new HashMap<>();
                Map<TopicPartition, Long> overflowedOffsets = new HashMap<>();
                for (Map.Entry<TopicPartition, Long> entry : stateToMap(streamName, readState).entrySet()) {
                    TopicPartition partition = entry.getKey();

                    Long requestOffset = entry.getValue();
                    Long beginningOffset = beginningOffsets.get(partition);
                    Long endOffset = endOffsets.get(partition);

                    // requestOffset < beginningOffset
                    if (requestOffset < beginningOffset) {
                        offsetsToRequest.put(partition, beginningOffset);
                    }
                    // beginningOffset <= requestOffset && requestOffset < endOffset
                    else if (requestOffset < endOffset) {
                        offsetsToRequest.put(partition, requestOffset);
                    }
                    // endOffset <= requestOffset
                    else {
                        // These offsets will not be polled, but returning them marks these offsets as overflowed
                        overflowedOffsets.put(partition, requestOffset);
                    }
                }

                RecordStorage<Void, byte[]> poll;

                Set<TopicPartition> partitionsToRequest = offsetsToRequest.keySet();
                if (!partitionsToRequest.isEmpty()) {
                    consumer.assign(partitionsToRequest);
                    for (TopicPartition partition : partitionsToRequest) {
                        consumer.seek(partition, offsetsToRequest.get(partition));
                    }

                    poll = pollRecords(consumer, take);

                    Map<TopicPartition, OffsetAndMetadata> polledOffsets = poll.getOffsets(null);
                    polledOffsets.forEach((topicPartition, offsetAndMetadata) -> {
                        offsetsToRequest.put(topicPartition, offsetAndMetadata.offset() + 1);
                    });
                }
                else {
                    poll = new RecordStorage<>(0);
                }

                offsetsToRequest.putAll(overflowedOffsets);

                return new ByteStreamContent(
                        stateFromMap(streamName, offsetsToRequest),
                        poll.getRecords().toArray(new byte[][]{})
                );
            } finally {
                consumer.close();
                activeConsumers.remove(consumer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        activeConsumers.forEach((consumer, v) -> {
            consumer.wakeup();
        });
    }

    private RecordStorage<Void, byte[]> pollRecords(KafkaConsumer<Void, byte[]> consumer, int maxCount) {
        RecordStorage<Void, byte[]> result = new RecordStorage<>(maxCount);

        TimeUnit unit = TimeUnit.MICROSECONDS;
        Timer timer = new Timer(unit, pollTimeout);
        timer.reset().start();
        long timeLeft = pollTimeout;

        while (result.available() &&  0 <= timeLeft) {
            ConsumerRecords<Void, byte[]> poll = consumer.poll(timeLeft);
            for (ConsumerRecord<Void, byte[]> record : poll) {
                if (result.available()) {
                    result.add(record);
                } else {
                    return result;
                }
            }
            timeLeft = timer.timeLeft();
        }

        return result;
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
