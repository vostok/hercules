package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class EventBuilder {

    private static final VariantWriter variantWriter = new VariantWriter();

    private UUID eventId;
    private int version;
    private Map<String, Variant> tags = new LinkedHashMap<>();

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setTag(String key, Variant value) {
        tags.put(key, value);
    }

    public Event build() {
        Encoder encoder = new Encoder();

        encoder.writeUnsignedByte(version);
        encoder.writeUuid(eventId);
        encoder.writeShort((short) tags.size());

        for (Map.Entry<String, Variant> e : tags.entrySet()) {
            encoder.writeString(e.getKey());
            variantWriter.write(encoder, e.getValue());
        }

        return new Event(encoder.getBytes(), version, eventId, tags);
    }
}
