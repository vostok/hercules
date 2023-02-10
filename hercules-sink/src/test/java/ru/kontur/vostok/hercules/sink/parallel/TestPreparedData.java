package ru.kontur.vostok.hercules.sink.parallel;

import ru.kontur.vostok.hercules.sink.parallel.sender.PreparedData;

/**
 * @author Innokentiy Krivonosov
 */
public class TestPreparedData implements PreparedData {
    private final int eventsCount;

    public TestPreparedData(int eventsCount) {
        this.eventsCount = eventsCount;
    }

    @Override
    public int getEventsCount() {
        return eventsCount;
    }
}
