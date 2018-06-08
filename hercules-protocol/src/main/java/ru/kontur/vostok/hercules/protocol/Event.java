package ru.kontur.vostok.hercules.protocol;

import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public class Event {
    private final byte[] bytes;
    private final int version;
    private final long timestamp;
    private final Map<String, Variant> tags;

    public Event(byte[] bytes, int version, long timestamp, Map<String, Variant> tags) {
        this.bytes = bytes;
        this.version = version;
        this.timestamp = timestamp;
        this.tags = tags;
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

    public Map<String, Variant> getTags() {
        return tags;
    }
}
