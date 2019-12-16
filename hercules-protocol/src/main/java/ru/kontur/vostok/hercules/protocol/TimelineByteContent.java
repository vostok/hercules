package ru.kontur.vostok.hercules.protocol;

import ru.kontur.vostok.hercules.util.bytes.ByteUtil;

public class TimelineByteContent {
    private static final int SIZE_OF_EVENT_COUNT = 4;

    private final TimelineState readState;
    private final byte[][] rawEvents;

    private final int size;

    public TimelineByteContent(TimelineState readState, byte[][] rawEvents) {
        this.readState = readState;
        this.rawEvents = rawEvents;

        this.size = readState.sizeOf() + SIZE_OF_EVENT_COUNT + ByteUtil.overallLength(rawEvents);
    }

    public TimelineState getReadState() {
        return readState;
    }

    public byte[][] getRawEvents() {
        return rawEvents;
    }

    public int sizeOf() {
        return size;
    }
}
