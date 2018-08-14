package ru.kontur.vostok.hercules.kafka.util.processing;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Interface for bulk processing implementation
 *
 * @param <Value> Kafka record value type
 */
public interface BulkSender<Value> extends Consumer<Collection<Value>>, AutoCloseable {
}
