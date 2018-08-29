package ru.kontur.vostok.hercules.protocol;

import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class Event {

    private final byte[] bytes;
    private final int version;
    private final UUID id;
    private final Container payload;

    public Event(byte[] bytes, int version, UUID id, Container payload) {
        this.bytes = bytes;
        this.version = version;
        this.id = id;
        this.payload = payload;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getVersion() {
        return version;
    }

    public UUID getId() {
        return id;
    }

    public Container getPayload() {
        return payload;
    }
}
