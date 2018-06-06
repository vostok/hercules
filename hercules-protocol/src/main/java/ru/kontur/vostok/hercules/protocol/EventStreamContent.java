package ru.kontur.vostok.hercules.protocol;

public class EventStreamContent {

    private final StreamReadState state;
    private final int eventCount;
    private final String[] events; // !!!! FIXME: для более просто й    разработки временно используем строки

    public EventStreamContent(StreamReadState state, String[] events) {
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

    public String[] getEvents() {
        return events;
    }
}
