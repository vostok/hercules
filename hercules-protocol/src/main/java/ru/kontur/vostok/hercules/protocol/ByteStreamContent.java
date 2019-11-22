package ru.kontur.vostok.hercules.protocol;


import ru.kontur.vostok.hercules.util.bytes.ByteUtil;

public class ByteStreamContent {
    private static final int SIZE_OF_EVENT_COUNT = Sizes.SIZE_OF_INTEGER;

    private final StreamReadState state;
    private final int eventCount;
    private final byte[][] events;
    private final int size;

    public ByteStreamContent(StreamReadState state, byte[][] events) {
        this.state = state;
        this.eventCount = events.length;
        this.events = events;

        this.size = state.sizeOf() + SIZE_OF_EVENT_COUNT + ByteUtil.overallLength(events);
    }

    public StreamReadState getState() {
        return state;
    }

    public int getEventCount() {
        return eventCount;
    }

    public byte[][] getEvents() {
        return events;
    }

    public int sizeOf() {
        return size;
    }

    public static ByteStreamContent empty() {
        return new ByteStreamContent(
                new StreamReadState(new StreamShardReadState[]{}),
                new byte[][]{}
        );
    }
}
