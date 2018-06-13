package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * @author Gregory Koshelev
 */
public class EventReader {
    private final Decoder decoder;
    private final Set<String> tags;
    private int count;
    private int remaining;

    private EventReader(Decoder decoder, Set<String> tags) {
        this.decoder = decoder;
        this.tags = tags;
    }

    public boolean hasNext() {
        return remaining > 0;
    }

    public int count() {
        return count;
    }

    public Event read() {
        if (remaining <= 0) {
            throw new NoSuchElementException();
        }
        remaining--;

        int from = position();

        int version = readVersion();
        long timestamp = readTimestamp();
        short tagsCount = readTagsCount();

        Map<String, Variant> tagValues = tags != null ? new HashMap<>(tags.size()) : new HashMap<>();
        for (int i = 0; i < tagsCount; i++) {
            String tagKey = readTagKey();
            if (tags == null || tags.contains(tagKey)) {
                Variant tagValue = readTagValue();
                tagValues.put(tagKey, tagValue);
            } else {
                skipTagValue();
            }
        }

        int to = position();
        byte[] bytes = decoder.subarray(from, to);

        return new Event(bytes, version, timestamp, tagValues);
    }

    private int readEventsCount() {
        return decoder.readInteger();
    }
    private int readVersion() {
        return decoder.readUnsignedByte();
    }
    private long readTimestamp() {
        return decoder.readLong();
    }
    private short readTagsCount() {
        return decoder.readShort();
    }
    private String readTagKey() {
        return decoder.readString();
    }
    private int position() {
        return decoder.position();
    }
    private Variant readTagValue() {
        Type type = decoder.readType();
        Object value = decoder.read(type);

        return new Variant(type, value);
    }
    private void skipTagValue() {
        Type type = decoder.readType();
        decoder.skip(type);
    }

    public static EventReader batchReader(byte[] data, Set<String> tags) {
        EventReader reader = new EventReader(new Decoder(data), tags);
        reader.remaining = reader.count = reader.readEventsCount();
        return reader;
    }

    public static EventReader singleReader(byte[] data, Set<String> tags) {
        EventReader reader = new EventReader(new Decoder(data), tags);
        reader.remaining = reader.count = 1;
        return reader;
    }

    public static EventReader batchReader(byte[] data) {
        EventReader reader = new EventReader(new Decoder(data), null);
        reader.remaining = reader.count = reader.readEventsCount();
        return reader;
    }

    public static EventReader singleReader(byte[] data) {
        EventReader reader = new EventReader(new Decoder(data), null);
        reader.remaining = reader.count = 1;
        return reader;
    }

    public static EventReader batchReaderWithCount(Decoder decoder, int count) {
        EventReader eventReader = new EventReader(decoder, null);
        eventReader.remaining = eventReader.count = count;
        return eventReader;
    }
}
