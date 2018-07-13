package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.*;

public class EventReader implements Reader<Event> {

    private static final UUIDReader uuidReader = new UUIDReader();
    private static final ContainerReader containerSkipper = ContainerReader.readFields(Collections.emptySet());

    private final ContainerReader containerReader;

    public EventReader(ContainerReader containerReader) {
        this.containerReader = containerReader;
    }

    @Override
    public Event read(Decoder decoder) {
        int from = decoder.position();

        int version = decoder.readUnsignedByte();
        UUID eventId = uuidReader.read(decoder);
        Container container = processContainer(decoder);

        int to = decoder.position();
        byte[] bytes = decoder.subarray(from, to);

        return new Event(bytes, version, eventId, container);
    }

    private Container processContainer(Decoder decoder) {
        if (Objects.nonNull(containerReader)) {
            return containerReader.read(decoder);
        }
        else {
            containerSkipper.skip(decoder);
            return new Container(Collections.emptyMap());
        }
    }

    public static EventReader readNoTags() {
        return new EventReader(null);
    }

    public static EventReader readAllTags() {
        return new EventReader(ContainerReader.readAllFields());
    }

    public static EventReader readTags(Set<String> tags) {
        return new EventReader(ContainerReader.readFields(tags));
    }
}
