package ru.kontur.vostok.hercules.sink.parallel;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.NoOpMetricsCollector;
import ru.kontur.vostok.hercules.sink.metrics.SinkMetrics;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static ru.kontur.vostok.hercules.sink.parallel.TestUtils.createEvent;
import static ru.kontur.vostok.hercules.sink.parallel.TestUtils.record;

/**
 * @author Innokentiy Krivonosov
 */
@ExtendWith(MockitoExtension.class)
public class ParallelSenderStrategyTest {
    private ConcurrentTopicPartitionQueues topicPartitionQueues;
    private BlockingQueue<Map<TopicPartition, OffsetAndMetadata>> offsetsToCommitQueue;
    private CreateEventsBatchStrategy<TestPreparedData> createEventsBatchStrategy;

    private final TopicPartition topicPartition = new TopicPartition("topic", 0);
    private final SinkMetrics metrics = new SinkMetrics(new NoOpMetricsCollector());

    @Mock
    private PrepareExecutor<TestPreparedData> prepareExecutor;

    @Mock
    private SendExecutor<TestPreparedData> sendExecutor;

    @Mock
    private EventsBatchListener<TestPreparedData> eventsBatchListener;

    @BeforeEach
    public void setUp() {
        topicPartitionQueues = new TopicPartitionQueuesImpl();
        createEventsBatchStrategy = getCreateEventsBatchStrategy(topicPartitionQueues);
        offsetsToCommitQueue = new ArrayBlockingQueue<>(1000);

        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.DEBUG);
    }

    @Test
    public void createBatchAndSendToPrepareExecutor() {
        var strategy = getStrategy(prepareExecutor, sendExecutor);

        addEventToQueues(topicPartition, record(topicPartition, createEvent()));

        strategy.createAndPrepareBatches();
        strategy.stop(5000, TimeUnit.MILLISECONDS);
        verify(eventsBatchListener).onFinishPrepare(any());
        verify(prepareExecutor).processPrepare(any());
    }

    @Test
    public void allCycle() {
        var strategy = getStrategy(new TestPrepareExecutor(), new TestOkResultSendExecutor());

        ConsumerRecord<byte[], byte[]> consumerRecord = record(topicPartition, createEvent());
        addEventToQueues(topicPartition, consumerRecord);

        strategy.createAndPrepareBatches();
        strategy.stopPrepare(5000, TimeUnit.MILLISECONDS);
        strategy.sendBatches();
        strategy.stopping();
        strategy.stop(5000, TimeUnit.MILLISECONDS);
        assertTrue(offsetsToCommitQueue.stream().anyMatch(offsets -> offsets.entrySet().stream().anyMatch(
                it -> it.getKey() == topicPartition && it.getValue().offset() == consumerRecord.offset() + 1
        )));
    }

    @Test
    public void onPartitionsRevoked() {
        var strategy = getStrategy(new TestPrepareExecutor(), new TestOkResultSendExecutor());

        ConsumerRecord<byte[], byte[]> consumerRecord = record(topicPartition, createEvent());
        addEventToQueues(topicPartition, consumerRecord);

        strategy.start();
        strategy.onPartitionsRevoked(List.of(new TopicPartition("notAddTopic", 0)));
        strategy.stop(5000, TimeUnit.MILLISECONDS);
        assertFalse(strategy.partitionsRevokedStarted);
    }

    private ParallelSenderStrategy<TestPreparedData> getStrategy(
            PrepareExecutor<TestPreparedData> prepareExecutor,
            SendExecutor<TestPreparedData> sendExecutor
    ) {
        return new ParallelSenderStrategy<>(
                new Properties(), createEventsBatchStrategy, prepareExecutor, sendExecutor, topicPartitionQueues,
                offsetsToCommitQueue, metrics, eventsBatchListener, 10_000_000L
        );
    }

    private void addEventToQueues(TopicPartition topicPartition, ConsumerRecord<byte[], byte[]> record) {
        topicPartitionQueues.addAll(topicPartition, Collections.singletonList(record));
    }

    private CreateEventsBatchStrategyImpl<TestPreparedData> getCreateEventsBatchStrategy(TopicPartitionQueues topicPartitionQueues) {
        Properties props = new Properties();
        props.setProperty(CreateEventsBatchStrategyImpl.Props.CREATE_BATCH_TIMEOUT_MS.name(), "0");
        return new CreateEventsBatchStrategyImpl<>(props, topicPartitionQueues, 1, 10_000_000L);
    }
}