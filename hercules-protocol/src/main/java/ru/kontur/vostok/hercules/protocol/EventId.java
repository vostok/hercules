package ru.kontur.vostok.hercules.protocol;

public class EventId {

    // TODO: Give a normal names to parts of EventId
    private final long p1;
    private final long p2;

    public EventId(long p1, long p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    public long getP1() {
        return p1;
    }

    public long getP2() {
        return p2;
    }
}
