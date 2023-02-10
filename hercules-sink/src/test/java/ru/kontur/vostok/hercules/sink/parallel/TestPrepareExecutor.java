package ru.kontur.vostok.hercules.sink.parallel;

import org.apache.kafka.common.TopicPartition;

import java.util.List;

/**
 * @author Innokentiy Krivonosov
 */
public class TestPrepareExecutor implements PrepareExecutor<TestPreparedData> {
    @Override
    public void processPrepare(EventsBatch<TestPreparedData> eventsBatch) {
        eventsBatch.events.put(new TopicPartition("test", 0), List.of(TestUtils.createEvent()));
        eventsBatch.setPreparedData(new TestPreparedData(eventsBatch.getAllEvents().size()));
    }
}