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

    private long timestamp;
    private UUID random;
    private int version;
    ContainerBuilder containerBuilder = ContainerBuilder.create();

    private boolean wasBuild = false;

    /**
     * @deprecated Use static function create instead
     */
    @Deprecated
    public EventBuilder() {
    }

    /**
     * @deprecated Use timestamp instead
     */
    @Deprecated
    public EventBuilder setTimestamp(long timestamp) {
        return timestamp(timestamp);
    }

    /**
     * @deprecated Use random instead
     */
    @Deprecated
    public EventBuilder setRandom(UUID random) {
        return random(random);
    }

   /**
     * @deprecated Use version instead
     */
    @Deprecated
    public EventBuilder setVersion(int version) {
        return version(version);
    }

    public EventBuilder timestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public EventBuilder random(UUID random) {
        this.random = random;
        return this;
    }

    public EventBuilder version(int version) {
        this.version = version;
        return this;
    }

    /**
     * @deprecated use tag instead
     */
    @Deprecated
    public EventBuilder setTag(String key, Variant value) {
        return tag(key, value);
    }

    /**
     * @deprecated use tag instead
     */
    @Deprecated
    public <T> EventBuilder setTag(TagDescription<T> tag, Variant value) {
        return tag(tag, value);
    }

    public EventBuilder tag(String key, Variant value) {
        this.containerBuilder.tag(key, value);
        return this;
    }

    public <T> EventBuilder tag(TagDescription<T> tag, Variant value) {
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
        encoder.writeLong(timestamp);
        encoder.writeUuid(random);

        Container container = containerBuilder.build();
        CONTAINER_WRITER.write(encoder, container);

        return new Event(stream.toByteArray(), version, timestamp, random, container);
    }

    public static EventBuilder create() {
        return new EventBuilder();
    }

    public static EventBuilder create(final long timestamp, final UUID random) {
        return new EventBuilder()
                .version(1)
                .timestamp(timestamp)
                .random(random);
    }

    public static EventBuilder create(final long timestamp, final String uuidString) {
        return new EventBuilder()
                .version(1)
                .timestamp(timestamp)
                .random(UUID.fromString(uuidString));
    }
}
