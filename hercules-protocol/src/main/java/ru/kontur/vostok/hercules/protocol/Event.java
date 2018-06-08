package ru.kontur.vostok.hercules.protocol;

import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public class Event {
    private final byte[] bytes;
    private final int version;
    private final long timestamp;
    private final Map<String, TagValue> tags;

    public Event(byte[] bytes, int version, long timestamp, Map<String, TagValue> tags) {
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

    public Map<String, TagValue> getTags() {
        return tags;
    }

}
