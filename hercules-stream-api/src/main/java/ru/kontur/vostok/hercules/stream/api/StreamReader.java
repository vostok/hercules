package ru.kontur.vostok.hercules.stream.api;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.protocol.ByteStreamContent;
import ru.kontur.vostok.hercules.protocol.StreamReadState;
import ru.kontur.vostok.hercules.util.Maps;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.LongValidators;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Gregory Koshelev
 */
public class StreamReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamReader.class);

    private final Properties properties;

    private final ConsumerPool<Void, byte[]> consumerPool;

    private final long readTimeoutMs;

    public StreamReader(Properties properties, ConsumerPool<Void, byte[]> consumerPool) {
        this.properties = properties;
        this.consumerPool = consumerPool;

        readTimeoutMs = Props.READ_TIMEOUT_MS.extract(properties);
    }

    public ByteStreamContent read(Stream stream, StreamReadState state, int shardIndex, int shardCount, int take) {
        List<TopicPartition> partitions = StreamUtil.getTopicPartitions(stream, shardIndex, shardCount);

        long elapsedTimeMs = 0L;
        long remainingTimeMs = readTimeoutMs;
        final long readStartedMs = System.currentTimeMillis();

        Consumer<Void, byte[]> consumer = null;
        try {
            consumer = consumerPool.acquire(remainingTimeMs, TimeUnit.MILLISECONDS);

            Map<TopicPartition, Long> beginningOffsets = consumer.beginningOffsets(partitions);

            Map<TopicPartition, Long> requestedOffsets = StreamReadStateUtil.stateToMap(stream.getName(), state);

            // New offsets for new StreamReadState
            Map<TopicPartition, Long> nextOffsets = new HashMap<>(Maps.effectiveHashMapCapacity(partitions.size()));
            List<TopicPartition> partitionsToRead = new ArrayList<>(partitions.size());

            for (TopicPartition partition : partitions) {
                Long beginningOffset = beginningOffsets.get(partition);
                Long requestedOffset = requestedOffsets.getOrDefault(partition, 0L);

                // If no events are available, then nothing to read. But should preserve requested offset in new StreamReadState.
                if (beginningOffset == null) {
                    nextOffsets.put(partition, requestedOffset);
                    continue;
                }

                // If events are unavailable anymore (due to retention), then read from the beginning (available once).
                if (requestedOffset < beginningOffset) {
                    requestedOffset = beginningOffset;
                }

                partitionsToRead.add(partition);
                nextOffsets.put(partition, requestedOffset);
            }

            consumer.assign(partitionsToRead);
            seekToNextOffsets(consumer, partitions, nextOffsets);

            elapsedTimeMs = readStartedMs - System.currentTimeMillis();
            remainingTimeMs = remainingTimeOrZero(readTimeoutMs, elapsedTimeMs);

            List<byte[]> events = pollAndUpdateNextOffsets(consumer, nextOffsets, take, remainingTimeMs);

            return new ByteStreamContent(
                    StreamReadStateUtil.stateFromMap(stream.getName(), nextOffsets),
                    events.toArray(new byte[0][]));
        } catch (InterruptedException | TimeoutException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (consumer != null) {
                consumerPool.release(consumer);
            }
        }
    }

    private static <K, V> void seekToNextOffsets(Consumer<K, V> consumer, List<TopicPartition> partitions, Map<TopicPartition, Long> nextOffsets) {
        for (TopicPartition partition : partitions) {
            consumer.seek(partition, nextOffsets.get(partition));
        }
    }

    private static <K, V> List<V> pollAndUpdateNextOffsets(Consumer<K, V> consumer, Map<TopicPartition, Long> nextOffsets, int take, long timeoutMs) {
        List<V> events = new ArrayList<>(take);
        int count = 0;

        long elapsedTimeMs = 0L;
        long remainingTimeMs = timeoutMs;
        final long pollStartedAt = System.currentTimeMillis();
        do {
            Duration timeout = Duration.ofMillis(remainingTimeMs);

            ConsumerRecords<K, V> records = consumer.poll(timeout);
            for (TopicPartition partition : records.partitions()) {
                long nextOffset = nextOffsets.get(partition);
                for (ConsumerRecord<K, V> record : records.records(partition)) {
                    if (++count <= take) {
                        events.add(record.value());
                        nextOffset = record.offset() + 1;
                    } else {
                        break;
                    }
                }
                nextOffsets.put(partition, nextOffset);
                if (count == take) {
                    break;
                }
            }

            elapsedTimeMs = pollStartedAt - System.currentTimeMillis();
            remainingTimeMs = remainingTimeOrZero(timeoutMs, elapsedTimeMs);
        }
        while ((count < take) && (remainingTimeMs > 0));

        return events;
    }

    private static long remainingTimeOrZero(long timeoutMs, long elapsedTimeMs) {
        return Math.max(timeoutMs - elapsedTimeMs, 0L );
    }

    static class Props {
        static final PropertyDescription<Long> READ_TIMEOUT_MS =
                PropertyDescriptions.longProperty("readTimeoutMs").
                        withDefaultValue(1_000L).
                        withValidator(LongValidators.positive()).
                        build();
    }
}
