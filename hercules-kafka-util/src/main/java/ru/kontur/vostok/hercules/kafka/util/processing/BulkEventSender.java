package ru.kontur.vostok.hercules.kafka.util.processing;

import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Collection;
import java.util.UUID;

public interface BulkEventSender extends AutoCloseable {

    void send(Collection<Entry<UUID, Event>> events);
}
