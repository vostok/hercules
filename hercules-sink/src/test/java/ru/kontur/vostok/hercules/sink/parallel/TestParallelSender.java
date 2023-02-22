package ru.kontur.vostok.hercules.sink.parallel;

import ru.kontur.vostok.hercules.health.NoOpMetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.parallel.sender.ParallelSender;

import java.util.List;
import java.util.Properties;

public class TestParallelSender extends ParallelSender<TestPreparedData> {
    public int lastPreparedData = Integer.MAX_VALUE;

    public TestParallelSender() {
        super(new Properties(), new NoOpMetricsCollector());
    }

    @Override
    public TestPreparedData prepare(List<Event> events) {
        return new TestPreparedData(events.size());
    }

    @Override
    public int send(TestPreparedData preparedData) {
        lastPreparedData = preparedData.getEventsCount();
        return preparedData.getEventsCount();
    }
}
