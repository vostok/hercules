package ru.kontur.vostok.hercules.protocol;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class Event implements Iterable<Map.Entry<String, Variant>> {

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

    public Variant getTag(String tagName) {
        return payload.get(tagName);
    }

    public int getTagCount() {
        return payload.size();
    }

    @Override
    public Iterator<Map.Entry<String, Variant>> iterator() {
        return payload.iterator();
    }
}
