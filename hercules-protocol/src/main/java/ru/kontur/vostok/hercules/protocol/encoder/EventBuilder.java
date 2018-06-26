package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.LinkedHashMap;
import java.util.Map;

public class EventBuilder {

    private static final VariantWriter variantWriter = new VariantWriter();

    private long timestamp;
    private int version;
    private Map<String, Variant> tags = new LinkedHashMap<>();

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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
        encoder.writeLong(timestamp);
        encoder.writeShort((short) tags.size());

        for (Map.Entry<String, Variant> e : tags.entrySet()) {
            encoder.writeString(e.getKey());
            variantWriter.write(encoder, e.getValue());
        }

        return new Event(encoder.getBytes(), version, timestamp, tags);
    }
}
