package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.ContainerWriter;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

/**
 * Simple Event build. NOT thread-safe
 */
public class EventBuilder {

    private static final ContainerWriter CONTAINER_WRITER = new ContainerWriter();

    private UUID eventId;
    private int version;
    ContainerBuilder containerBuilder = ContainerBuilder.create();

    private boolean wasBuild = false;

    public EventBuilder setEventId(UUID eventId) {
        this.eventId = eventId;
        return this;
    }

    public EventBuilder setVersion(int version) {
        this.version = version;
        return this;
    }

    public EventBuilder setTag(String key, Variant value) {
        this.containerBuilder.tag(key, value);
        return this;
    }

    public <T> EventBuilder setTag(TagDescription<T> tag, Variant value) {
        this.containerBuilder.tag(tag, value);
        return this;
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

        Container container = containerBuilder.build();
        CONTAINER_WRITER.write(encoder, container);

        return new Event(stream.toByteArray(), version, eventId, container);
    }
}
