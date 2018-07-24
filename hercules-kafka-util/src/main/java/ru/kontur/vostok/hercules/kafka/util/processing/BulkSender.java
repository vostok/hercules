package ru.kontur.vostok.hercules.kafka.util.processing;

import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

public interface BulkSender<Value> extends Consumer<Collection<Value>>, AutoCloseable {
}
