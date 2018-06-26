package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.*;

public class EventReader2 implements Reader<Event> {

    private static final VariantReader variantReader = new VariantReader();

    private final Set<String> tags;

    private EventReader2(Set<String> tags) {
        this.tags = tags;
    }

    @Override
    public Event read(Decoder decoder) {
        int from = decoder.position();

        int version = decoder.readUnsignedByte();
        long timestamp = decoder.readLong();
        short tagsCount = decoder.readShort();

        Map<String, Variant> tagValues = tags != null ? new HashMap<>(tags.size()) : new HashMap<>();
        for (int i = 0; i < tagsCount; i++) {
            String tagKey = decoder.readString();
            if (tags == null || tags.contains(tagKey)) {
                Variant tagValue = variantReader.read(decoder);
                tagValues.put(tagKey, tagValue);
            } else {
                variantReader.skip(decoder);
            }
        }

        int to = decoder.position();
        byte[] bytes = decoder.subarray(from, to);

        return new Event(bytes, version, timestamp, tagValues);
    }

    public static EventReader2 readNoTags() {
        return new EventReader2(Collections.emptySet());
    }

    public static EventReader2 readAllTags() {
        return new EventReader2(null);
    }

    public static EventReader2 readTags(Set<String> tags) {
        return new EventReader2(tags);
    }
}
