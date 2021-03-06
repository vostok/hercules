package ru.kontur.vostok.hercules.stream.api;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.protocol.ByteStreamContent;
import ru.kontur.vostok.hercules.protocol.StreamReadState;
import ru.kontur.vostok.hercules.util.Maps;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.Timer;

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
    private final ConsumerPool<Void, byte[]> consumerPool;
    private final TimeSource time;
    private final StreamReaderMetrics metrics;

    public StreamReader(Properties properties,
                        ConsumerPool<Void, byte[]> consumerPool,
                        MetricsCollector metricsCollector) {
        this(properties, consumerPool, metricsCollector, TimeSource.SYSTEM);
    }

    StreamReader(Properties properties,
                 ConsumerPool<Void, byte[]> consumerPool,
                 MetricsCollector metricsCollector,
                 TimeSource time) {
        this.consumerPool = consumerPool;
        this.time = time;
        this.metrics = new StreamReaderMetrics(metricsCollector);
    }

    public ByteStreamContent read(Stream stream, StreamReadState state, int shardIndex, int shardCount, int take, int timeoutMs) {
        List<TopicPartition> partitions = StreamUtil.getTopicPartitions(stream, shardIndex, shardCount);

        if (partitions.isEmpty()) {
            return ByteStreamContent.empty();
        }

        Timer timer = time.timer(timeoutMs);

        Consumer<Void, byte[]> consumer = null;
        try {
            consumer = consumerPool.acquire(timer.remainingTimeMs(), TimeUnit.MILLISECONDS);
            metrics.updateWaitConsumer(timer.elapsedTimeMs());

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
            seekToNextOffsets(consumer, partitionsToRead, nextOffsets);

            List<byte[]> events = pollAndUpdateNextOffsets(consumer, nextOffsets, take, timer);

            metrics.update(stream.getName(), events);

            return new ByteStreamContent(
                    StreamReadStateUtil.stateFromMap(stream.getName(), nextOffsets),
                    events.toArray(new byte[0][])
            );
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

    private static <K, V> List<V> pollAndUpdateNextOffsets(Consumer<K, V> consumer,
                                                           Map<TopicPartition, Long> nextOffsets,
                                                           int take,
                                                           Timer timer) {
        List<V> events = new ArrayList<>(take);
        int count = 0;

        do {
            Duration timeout = timer.toDuration();

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
        }
        while ((count < take) && !timer.isExpired());

        return events;
    }
}
