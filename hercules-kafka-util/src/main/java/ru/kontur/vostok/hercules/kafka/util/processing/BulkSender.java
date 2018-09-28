package ru.kontur.vostok.hercules.kafka.util.processing;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Interface for bulk processing implementation
 *
 * @param <Value> Kafka record value type
 */
public interface BulkSender<Value> extends AutoCloseable {

    BulkSenderStat process(Collection<Value> values) throws BackendServiceFailedException;

    /**
     * Ping realization. If ping functionality is not available for service we assume that service is always available
     * until {@link BackendServiceFailedException} is thrown.
     *
     * @return is service available
     */
    default boolean ping() {
        return true;
    }
}
