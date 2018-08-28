package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.ContainerWriter;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Simple Event build. NOT thread-safe
 */
public class EventBuilder {

    private static final ContainerWriter containerWriter = new ContainerWriter();

    private UUID eventId;
    private int version;
    private Map<String, Variant> tags = new LinkedHashMap<>();

    private boolean wasBuild = false;

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setTag(String key, Variant value) {
        tags.put(key, value);
    }

    public void setTag(FieldDescription tag, Variant value) {
        if (tag.getType() != value.getType()) {
            throw new IllegalArgumentException(
                    String.format("Value type mismatch, expected: %s, actual: %s", tag.getType(), value.getType())
            );
        }
        setTag(tag.getName(), value);
    }

    public Event build() {
        if (wasBuild) {
            throw new IllegalStateException("Builder already used");
        } else {
            wasBuild = true;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);

        encoder.writeUnsignedByte(version);
        encoder.writeUuid(eventId);

        Container container = new Container(tags);
        containerWriter.write(encoder, container);

        return new Event(stream.toByteArray(), version, eventId, container);
    }
}
