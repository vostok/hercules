package ru.kontur.vostok.hercules.sink.parallel;

import org.apache.kafka.common.TopicPartition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kontur.vostok.hercules.health.NoOpMetricsCollector;
import ru.kontur.vostok.hercules.sink.ProcessorResult;
import ru.kontur.vostok.hercules.sink.metrics.SinkMetrics;
import ru.kontur.vostok.hercules.sink.parallel.sender.ParallelSender;

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
public class SendExecutorImplTest {
    @Mock
    private ParallelSender<TestPreparedData> parallelSender;

    @Test
    public void sendAll() {
        TopicPartition topicPartition = new TopicPartition(TOPIC, 0);
        when(parallelSender.processSend(any())).thenReturn(ProcessorResult.ok(1, 0));

        SendExecutor<TestPreparedData> sendExecutor = getExecutor();

        EventsBatch<TestPreparedData> eventsBatch = TestUtils.getEventsBatch(topicPartition);

        sendExecutor.processSend(eventsBatch);
        assertTrue(eventsBatch.isProcessFinished());
        assertTrue(eventsBatch.getProcessorResult().isSuccess());
        assertEquals(1, eventsBatch.getProcessorResult().getProcessedEvents());
    }

    @Test
    public void sendNone() {
        TopicPartition topicPartition = new TopicPartition(TOPIC, 0);
        when(parallelSender.processSend(any())).thenReturn(ProcessorResult.ok(0, 1));

        SendExecutor<TestPreparedData> sendExecutor = getExecutor();

        EventsBatch<TestPreparedData> eventsBatch = TestUtils.getEventsBatch(topicPartition);

        sendExecutor.processSend(eventsBatch);
        assertTrue(eventsBatch.isProcessFinished());
        assertTrue(eventsBatch.getProcessorResult().isSuccess());
        assertEquals(0, eventsBatch.getProcessorResult().getProcessedEvents());
    }

    @Test
    public void noRequestDataError() {
        TopicPartition topicPartition = new TopicPartition(TOPIC, 0);
        when(parallelSender.processSend(any())).thenThrow(new RuntimeException("test error"));

        SendExecutor<TestPreparedData> sendExecutor = getExecutor();

        EventsBatch<TestPreparedData> eventsBatch = TestUtils.getEventsBatch(topicPartition);

        sendExecutor.processSend(eventsBatch);
        assertTrue(eventsBatch.isProcessFinished());
        assertFalse(eventsBatch.getProcessorResult().isSuccess());
    }

    @NotNull
    private SendExecutorImpl<TestPreparedData> getExecutor() {
        return new SendExecutorImpl<>(parallelSender, new SinkMetrics(new NoOpMetricsCollector()));
    }
}