package ru.kontur.vostok.hercules.protocol;

public class TimelineContent {

    private final TimelineReadState readState;
    private final Event[] events;

    public TimelineContent(TimelineReadState readState, Event[] events) {
        this.readState = readState;
        this.events = events;
    }

    public TimelineReadState getReadState() {
        return readState;
    }

    public Event[] getEvents() {
        return events;
    }
}
