package ru.kontur.vostok.hercules.protocol;


public class ByteStreamContent {

    private final StreamReadState state;
    private final int eventCount;
    private final byte[][] events;

    public ByteStreamContent(StreamReadState state, byte[][] events) {
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

    public byte[][] getEvents() {
        return events;
    }

    public static ByteStreamContent empty() {
        return new ByteStreamContent(
                new StreamReadState(new StreamShardReadState[]{}),
                new byte[][]{}
        );
    }
}
