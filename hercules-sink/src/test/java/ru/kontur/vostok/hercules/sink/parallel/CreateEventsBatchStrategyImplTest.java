package ru.kontur.vostok.hercules.sink.parallel;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static ru.kontur.vostok.hercules.sink.parallel.TestUtils.TOPIC;
import static ru.kontur.vostok.hercules.sink.parallel.TestUtils.createEvent;
import static ru.kontur.vostok.hercules.sink.parallel.TestUtils.record;

/**
 * @author Innokentiy Krivonosov
 */
public class CreateEventsBatchStrategyImplTest {

    @Test
    public void notFullPartition() {
        TopicPartitionQueues topicPartitionQueues = new TopicPartitionQueuesImpl();
        TopicPartition topicPartition0 = new TopicPartition(TOPIC, 0);
        TopicPartition topicPartition1 = new TopicPartition(TOPIC, 1);
        topicPartitionQueues.addAll(topicPartition0, List.of(record(topicPartition0, createEvent())));
        topicPartitionQueues.addAll(topicPartition1, List.of(record(topicPartition1, createEvent())));

        CreateEventsBatchStrategy<TestPreparedData> createEventsBatchStrategy = getCreateEventsBatchStrategy(topicPartitionQueues);
        List<EventsBatch<TestPreparedData>> eventsBatches = createEventsBatchStrategy.create(it -> true);
        assertEquals(1, eventsBatches.size());
        assertEquals(2, eventsBatches.get(0).rawEvents.size());
        assertEquals(1, eventsBatches.get(0).rawEvents.get(topicPartition0).size());
        assertEquals(1, eventsBatches.get(0).rawEvents.get(topicPartition1).size());
    }

    @Test
    public void useOnlyIsAllowedPartition() {
        TopicPartitionQueues topicPartitionQueues = new TopicPartitionQueuesImpl();
        TopicPartition topicPartition0 = new TopicPartition(TOPIC, 0);
        TopicPartition topicPartition1 = new TopicPartition(TOPIC, 1);
        topicPartitionQueues.addAll(topicPartition0, List.of(record(topicPartition0, createEvent())));
        topicPartitionQueues.addAll(topicPartition1, List.of(record(topicPartition1, createEvent())));

        CreateEventsBatchStrategy<TestPreparedData> createEventsBatchStrategy = getCreateEventsBatchStrategy(topicPartitionQueues);
        List<EventsBatch<TestPreparedData>> eventsBatches = createEventsBatchStrategy.create(it -> it.equals(topicPartition0));
        assertEquals(1, eventsBatches.size());
        assertEquals(1, eventsBatches.get(0).rawEvents.size());
        assertEquals(1, eventsBatches.get(0).rawEvents.get(topicPartition0).size());
        assertFalse(eventsBatches.get(0).rawEvents.containsKey(topicPartition1));
    }

    @Test
    public void fullPartition() {
        TopicPartitionQueues topicPartitionQueues = new TopicPartitionQueuesImpl();
        TopicPartition topicPartition0 = new TopicPartition(TOPIC, 0);
        List<ConsumerRecord<byte[], byte[]>> consumerRecords = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            consumerRecords.add(record(topicPartition0, createEvent()));
        }
        topicPartitionQueues.addAll(topicPartition0, consumerRecords);

        CreateEventsBatchStrategy<TestPreparedData> createEventsBatchStrategy = getCreateEventsBatchStrategy(topicPartitionQueues);
        List<EventsBatch<TestPreparedData>> eventsBatches = createEventsBatchStrategy.create(it -> true);
        assertEquals(1, eventsBatches.size());
        assertEquals(10, eventsBatches.get(0).rawEvents.get(topicPartition0).size());
    }

    private CreateEventsBatchStrategyImpl<TestPreparedData> getCreateEventsBatchStrategy(TopicPartitionQueues topicPartitionQueues) {
        Properties props = new Properties();
        props.setProperty(CreateEventsBatchStrategyImpl.Props.CREATE_BATCH_TIMEOUT_MS.name(), "0");
        return new CreateEventsBatchStrategyImpl<>(props, topicPartitionQueues, 10, 10_000_000L);
    }
}