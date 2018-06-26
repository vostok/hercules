package ru.kontur.vostok.hercules.protocol;

public class TimelineByteContent {

    private final TimelineReadState readState;
    private final byte[][] rawEvents;

    public TimelineByteContent(TimelineReadState readState, byte[][] rawEvents) {
        this.readState = readState;
        this.rawEvents = rawEvents;
    }

    public TimelineReadState getReadState() {
        return readState;
    }

    public byte[][] getRawEvents() {
        return rawEvents;
    }
}
