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
    private final Container tags;

    public Event(byte[] bytes, int version, UUID id, Container tags) {
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

    public Variant getTag(String tagName) {
        return tags.get(tagName);
    }

    public int getTagCount() {
        return tags.size();
    }

    @Override
    public Iterator<Map.Entry<String, Variant>> iterator() {
        return tags.iterator();
    }
}
