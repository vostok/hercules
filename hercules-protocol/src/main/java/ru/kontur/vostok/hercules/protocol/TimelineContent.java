package ru.kontur.vostok.hercules.protocol;

public class TimelineContent {
    private static final int SIZE_OF_EVENT_COUNT = Type.INTEGER.size;

    private final TimelineState readState;
    private final Event[] events;

    public TimelineContent(TimelineState readState, Event[] events) {
        this.readState = readState;
        this.events = events;
    }

    public TimelineState getReadState() {
        return readState;
    }

    public Event[] getEvents() {
        return events;
    }

    public int sizeOf() {
        int size = readState.sizeOf() + SIZE_OF_EVENT_COUNT;
        for (Event event : events) {
            size += event.getBytes().length;
        }
        return size;
    }
}
