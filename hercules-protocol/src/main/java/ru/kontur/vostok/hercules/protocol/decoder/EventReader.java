package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class EventReader implements Reader<Event> {

    private static final ContainerReader CONTAINER_READER = ContainerReader.readTags(Collections.emptySet());

    private final ContainerReader containerReader;

    public EventReader(ContainerReader containerReader) {
        this.containerReader = containerReader;
    }

    @Override
    public Event read(Decoder decoder) {
        int from = decoder.position();

        int version = decoder.readUnsignedByte();
        long timestamp = decoder.readLong();
        UUID random = decoder.readUuid();
        Container container = processContainer(decoder);

        int to = decoder.position();
        byte[] bytes = decoder.subarray(from, to);

        return new Event(bytes, version, timestamp, random, container);
    }

    private Container processContainer(Decoder decoder) {
        if (Objects.nonNull(containerReader)) {
            return containerReader.read(decoder);
        } else {
            CONTAINER_READER.skip(decoder);
            return new Container(Collections.emptyMap());
        }
    }

    public static EventReader readNoTags() {
        return new EventReader(null);
    }

    public static EventReader readAllTags() {
        return new EventReader(ContainerReader.readAllTags());
    }

    public static EventReader readTags(Set<String> tags) {
        return new EventReader(ContainerReader.readTags(tags));
    }
}
