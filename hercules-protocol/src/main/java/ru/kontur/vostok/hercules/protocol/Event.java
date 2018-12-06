package ru.kontur.vostok.hercules.protocol;

import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class Event {

    private final byte[] bytes;
    private final int version;
    private final long timestamp;
    private final UUID random;
    private final Container payload;

    public Event(byte[] bytes, int version, long timestamp, UUID random, Container payload) {
        this.bytes = bytes;
        this.version = version;
        this.timestamp = timestamp;
        this.random = random;
        this.payload = payload;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getVersion() {
        return version;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public UUID getRandom() {
        return random;
    }

    public Container getPayload() {
        return payload;
    }
}
