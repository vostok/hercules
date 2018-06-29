package ru.kontur.vostok.hercules.protocol;

import java.util.Map;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class Event {
    private final byte[] bytes;
    private final int version;
    private final UUID id;
    private final Map<String, Variant> tags;

    public Event(byte[] bytes, int version, UUID id, Map<String, Variant> tags) {
        this.bytes = bytes;
        this.version = version;
        this.id = id;
        this.tags = tags;
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

    public Map<String, Variant> getTags() {
        return tags;
    }
}
