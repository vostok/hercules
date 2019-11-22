package ru.kontur.vostok.hercules.protocol;

public class TimelineContent {

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
        int size = readState.sizeOf() + Sizes.SIZE_OF_INTEGER;
        for (Event event : events) {
            size += event.getBytes().length;
        }
        return size;
    }
}
