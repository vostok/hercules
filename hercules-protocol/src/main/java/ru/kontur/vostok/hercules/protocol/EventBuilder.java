package ru.kontur.vostok.hercules.protocol;

import ru.kontur.vostok.hercules.protocol.encoder.ContainerWriter;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Event builder.
 *
 * @author Gregory Koshelev
 */
public class EventBuilder {
    private static final int SIZE_OF_VERSION = 1;
    private static final int SIZE_OF_TIMESTAMP = 8;

    private static final ContainerWriter CONTAINER_WRITER = new ContainerWriter();

    private long timestamp;
    private UUID uuid;
    private int version = 1;// Version is 1

    private Container.ContainerBuilder containerBuilder = Container.builder();

    private EventBuilder() {
    }

    public EventBuilder timestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public EventBuilder uuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public EventBuilder version(int version) {
        this.version = version;
        return this;
    }

    public EventBuilder tag(TinyString key, Variant value) {
        this.containerBuilder.tag(key, value);
        return this;
    }

    public EventBuilder tag(String key, Variant value) {
        return tag(TinyString.of(key), value);
    }

    public Event build() {
        Container container = containerBuilder.build();

        ByteBuffer buffer = ByteBuffer.allocate(Sizes.sizeOfVersion() + Sizes.sizeOfTimestamp() + Sizes.SIZE_OF_UUID + container.sizeOf());
        Encoder encoder = new Encoder(buffer);

        encoder.writeUnsignedByte(version);
        encoder.writeLong(timestamp);
        encoder.writeUuid(uuid);

        CONTAINER_WRITER.write(encoder, container);

        return new Event(buffer.array(), version, timestamp, uuid, container);//FIXME: If we want to reuse buffers, then we should not use buffer.array() to prevent it leaking. Also, buffer may be greater than event.
    }

    public static EventBuilder create() {
        return new EventBuilder();
    }

    public static EventBuilder create(final long timestamp, final UUID uuid) {
        return new EventBuilder()
                .timestamp(timestamp)
                .uuid(uuid);
    }

    public static EventBuilder create(final long timestamp, final String uuidString) {
        return new EventBuilder()
                .timestamp(timestamp)
                .uuid(UUID.fromString(uuidString));
    }
}
