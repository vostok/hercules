package ru.kontur.vostok.hercules.protocol;

public class EventStreamContent {
    private static final int SIZE_OF_EVENT_COUNT = Type.INTEGER.size;

    private final StreamReadState state;
    private final int eventCount;
    private final Event[] events;

    public EventStreamContent(StreamReadState state, Event[] events) {
        this.state = state;
        this.eventCount = events.length;
        this.events = events;
    }

    public StreamReadState getState() {
        return state;
    }

    public int getEventCount() {
        return eventCount;
    }

    public Event[] getEvents() {
        return events;
    }

    public int sizeOf() {
        int size = state.sizeOf() + SIZE_OF_EVENT_COUNT;
        for (Event event : events) {
            size += event.getBytes().length;
        }
        return size;
    }
}
