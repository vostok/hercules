package ru.kontur.vostok.hercules.protocol;

public class TimelineByteContent {

    private final TimelineState readState;
    private final byte[][] rawEvents;

    public TimelineByteContent(TimelineState readState, byte[][] rawEvents) {
        this.readState = readState;
        this.rawEvents = rawEvents;
    }

    public TimelineState getReadState() {
        return readState;
    }

    public byte[][] getRawEvents() {
        return rawEvents;
    }
}
