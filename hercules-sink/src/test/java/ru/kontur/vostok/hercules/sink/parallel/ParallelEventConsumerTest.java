package ru.kontur.vostok.hercules.sink.parallel;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.consumer.internals.NoOpConsumerRebalanceListener;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.NoOpMetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.consumer.Subscription;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.Processor;
import ru.kontur.vostok.hercules.sink.ProcessorResult;
import ru.kontur.vostok.hercules.sink.metrics.SinkMetrics;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.kontur.vostok.hercules.sink.parallel.TestUtils.TOPIC;
import static ru.kontur.vostok.hercules.sink.parallel.TestUtils.createEvent;
import static ru.kontur.vostok.hercules.sink.parallel.TestUtils.record;

/**
 * @author Innokentiy Krivonosov
 */
public class ParallelEventConsumerTest {
    private final EventDeserializer eventDeserializer = EventDeserializer.parseAllTags();

    private ConcurrentTopicPartitionQueues topicPartitionQueues;
    private BlockingQueue<Map<TopicPartition, OffsetAndMetadata>> offsetsToCommitQueue;
    MockConsumer<byte[], byte[]> consumer;

    @BeforeEach
    public void setUp() {
        topicPartitionQueues = new TopicPartitionQueuesImpl();
        offsetsToCommitQueue = new ArrayBlockingQueue<>(1000);
        consumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);

        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.DEBUG);
    }

    @Test
    public void base() {
        ParallelEventConsumer parallelEventConsumer = getKafkaEventSupplier();
        TopicPartition topicPartition = new TopicPartition(TOPIC, 0);

        Event event = createEvent();
        consumer.updateBeginningOffsets(TestUtils.getBeginningOffsets(1));
        consumer.schedulePollTask(() -> {
            consumer.rebalance(Collections.singletonList(topicPartition));
            consumer.addRecord(record(topicPartition, event));
        });
        consumer.schedulePollTask(() -> parallelEventConsumer.stop(5000, TimeUnit.MILLISECONDS));

        parallelEventConsumer.run();

        Event queueEvent = getEvent(topicPartition);
        assertEquals(event.getTimestamp(), queueEvent.getTimestamp());
        assertEquals(event.getUuid(), queueEvent.getUuid());
    }

    @Test
    public void onPartitionsRevoked() {
        ParallelEventConsumer parallelEventConsumer = getKafkaEventSupplier();
        TopicPartition topicPartition0 = new TopicPartition(TOPIC, 0);
        TopicPartition topicPartition1 = new TopicPartition(TOPIC, 1);

        consumer.updateBeginningOffsets(TestUtils.getBeginningOffsets(2));
        consumer.schedulePollTask(() -> {
            consumer.rebalance(List.of(topicPartition0, topicPartition1));
            consumer.addRecord(record(topicPartition1, createEvent()));
        });
        consumer.schedulePollTask(() -> parallelEventConsumer.stop(5000, TimeUnit.MILLISECONDS));

        parallelEventConsumer.run();
        parallelEventConsumer.onPartitionsRevoked(Collections.singletonList(topicPartition1));

        assertTrue(topicPartitionQueues.getPartitions().isEmpty());
    }

    @Test
    public void pollException() {
        ParallelEventConsumer parallelEventConsumer = getKafkaEventSupplier();
        TopicPartition topicPartition = new TopicPartition(TOPIC, 0);

        Event event = createEvent();
        consumer.updateBeginningOffsets(TestUtils.getBeginningOffsets(1));
        consumer.schedulePollTask(() -> consumer.setPollException(new KafkaException("poll exception")));
        consumer.schedulePollTask(() -> {
            consumer.rebalance(Collections.singletonList(topicPartition));
            consumer.addRecord(record(topicPartition, event));
        });
        consumer.schedulePollTask(() -> parallelEventConsumer.stop(5000, TimeUnit.MILLISECONDS));

        parallelEventConsumer.run();

        Event queueEvent = getEvent(topicPartition);
        assertEquals(event.getTimestamp(), queueEvent.getTimestamp());
        assertEquals(event.getUuid(), queueEvent.getUuid());
    }

    @Test
    public void pause() {
        ParallelEventConsumer parallelEventConsumer = getKafkaEventSupplier();
        TopicPartition topicPartition = new TopicPartition(TOPIC, 0);

        Event event = createEvent();
        consumer.updateBeginningOffsets(TestUtils.getBeginningOffsets(1));
        consumer.schedulePollTask(() -> {
            assertTrue(consumer.paused().isEmpty());
            consumer.rebalance(Collections.singletonList(topicPartition));
            consumer.addRecord(record(topicPartition, createEvent()));
            consumer.addRecord(record(topicPartition, createEvent()));
            consumer.addRecord(record(topicPartition, event));
        });
        consumer.schedulePollTask(() -> {
            assertTrue(consumer.paused().contains(topicPartition));
            topicPartitionQueues.poll(topicPartition);
            topicPartitionQueues.poll(topicPartition);
        });
        consumer.schedulePollTask(() -> {
            assertTrue(consumer.paused().isEmpty());
        });
        consumer.schedulePollTask(() -> parallelEventConsumer.stop(5000, TimeUnit.MILLISECONDS));

        parallelEventConsumer.run();

        Event queueEvent = getEvent(topicPartition);
        assertEquals(event.getTimestamp(), queueEvent.getTimestamp());
        assertEquals(event.getUuid(), queueEvent.getUuid());
    }

    @Test
    public void commit() {
        ParallelEventConsumer parallelEventConsumer = getKafkaEventSupplier();
        TopicPartition topicPartition = new TopicPartition(TOPIC, 0);

        int offset = 100;
        consumer.updateBeginningOffsets(TestUtils.getBeginningOffsets(1));
        consumer.schedulePollTask(() -> {
            consumer.rebalance(Collections.singletonList(topicPartition));
            consumer.addRecord(record(topicPartition, createEvent()));
            try {
                offsetsToCommitQueue.put(Map.of(topicPartition, new OffsetAndMetadata(offset)));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        consumer.schedulePollTask(() -> {
            OffsetAndMetadata committed = consumer.committed(Set.of(topicPartition)).get(topicPartition);
            assertEquals(offset, committed.offset());
        });

        consumer.schedulePollTask(() -> {
            parallelEventConsumer.onPartitionsRevoked(Collections.singletonList(topicPartition));
            try {
                offsetsToCommitQueue.put(Map.of(topicPartition, new OffsetAndMetadata(offset + 1)));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        consumer.schedulePollTask(() -> {
            OffsetAndMetadata committed = consumer.committed(Set.of(topicPartition)).get(topicPartition);
            assertEquals(offset, committed.offset());
        });
        consumer.schedulePollTask(() -> parallelEventConsumer.stop(5000, TimeUnit.MILLISECONDS));

        parallelEventConsumer.run();
    }

    private Event getEvent(TopicPartition topicPartition) {
        ConsumerRecord<byte[], byte[]> record = topicPartitionQueues.poll(topicPartition);
        return eventDeserializer.deserialize(TOPIC, record.value());
    }

    private ParallelEventConsumer getKafkaEventSupplier() {
        Subscription subscription = Subscription.builder().include(new String[]{TOPIC}).build();
        int batchSize = 2;

        Properties props = new Properties();
        props.setProperty(ParallelEventConsumer.Props.POLLING_DELAY_MS.name(), "10");
        SinkMetrics metrics = new SinkMetrics(new NoOpMetricsCollector());
        return new ParallelEventConsumer(
                props, subscription, consumer,
                topicPartitionQueues, offsetsToCommitQueue, new NoOpProcessor(TimeSource.SYSTEM), new NoOpConsumerRebalanceListener(),
                batchSize, 10_000_000L, metrics);
    }

    static class NoOpProcessor extends Processor {

        public NoOpProcessor(TimeSource time) {
            super(time);
        }

        @Override
        public ProcessorResult process(List<Event> events) {
            return ProcessorResult.ok(events.size(), 0);
        }
    }

}