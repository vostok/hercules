package ru.kontur.vostok.hercules.sink.parallel;

import org.apache.kafka.common.TopicPartition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kontur.vostok.hercules.health.NoOpMetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.sink.metrics.SinkMetrics;
import ru.kontur.vostok.hercules.sink.parallel.sender.ParallelSender;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.kontur.vostok.hercules.sink.parallel.TestUtils.TOPIC;

/**
 * @author Innokentiy Krivonosov
 */
@ExtendWith(MockitoExtension.class)
public class PrepareExecutorImplTest {
    @Mock
    private ParallelSender<TestPreparedData> parallelSender;

    @Test
    public void processDone() {
        TopicPartition topicPartition = new TopicPartition(TOPIC, 0);
        when(parallelSender.prepare(any())).thenReturn(new TestPreparedData(1));

        PrepareExecutor<TestPreparedData> prepareExecutor = getExecutor();
        EventsBatch<TestPreparedData> eventsBatch = TestUtils.getEventsBatch(topicPartition);
        prepareExecutor.processPrepare(eventsBatch);
        assertEquals(1, eventsBatch.events.get(topicPartition).size());
        assertTrue(eventsBatch.isReadyToSend());
        assertFalse(eventsBatch.isProcessFinished());
    }

    @Test
    public void processManyPartitions() {
        EventDeserializer eventDeserializer = EventDeserializer.parseAllTags();
        TopicPartition topicPartition0 = new TopicPartition(TOPIC, 0);
        TopicPartition topicPartition1 = new TopicPartition(TOPIC, 1);
        when(parallelSender.prepare(any())).thenReturn(new TestPreparedData(2));

        PrepareExecutor<TestPreparedData> prepareExecutor = getExecutor();
        EventsBatch<TestPreparedData> eventsBatch = TestUtils.getEventsBatch(topicPartition0, topicPartition1);
        prepareExecutor.processPrepare(eventsBatch);
        assertEquals(1, eventsBatch.events.get(topicPartition0).size());
        assertEquals(1, eventsBatch.events.get(topicPartition1).size());
        assertEquals(eventDeserializer.deserialize("", eventsBatch.rawEvents.get(topicPartition0).get(0)).getUuid(),
                eventsBatch.events.get(topicPartition0).get(0).getUuid());
        assertEquals(eventDeserializer.deserialize("", eventsBatch.rawEvents.get(topicPartition1).get(0)).getUuid(),
                eventsBatch.events.get(topicPartition1).get(0).getUuid());
        assertTrue(eventsBatch.isReadyToSend());
        assertFalse(eventsBatch.isProcessFinished());
    }

    @Test
    public void processError() {
        TopicPartition topicPartition = new TopicPartition(TOPIC, 0);
        when(parallelSender.prepare(any())).thenThrow(new RuntimeException("test error"));

        PrepareExecutor<TestPreparedData> prepareExecutor = getExecutor();
        EventsBatch<TestPreparedData> eventsBatch = TestUtils.getEventsBatch(topicPartition);
        prepareExecutor.processPrepare(eventsBatch);
        assertEquals(1, eventsBatch.events.get(topicPartition).size());

        assertFalse(eventsBatch.isReadyToSend());
        assertTrue(eventsBatch.isProcessFinished());
        assertFalse(eventsBatch.getProcessorResult().isSuccess());
    }

    @NotNull
    private PrepareExecutorImpl<TestPreparedData> getExecutor() {
        return new PrepareExecutorImpl<>(parallelSender, Collections.emptyList(), new SinkMetrics(new NoOpMetricsCollector()));
    }
}