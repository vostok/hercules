package ru.kontur.vostok.hercules.kafka.util.processing.bulk;

import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;

import java.util.Collection;

/**
 * Interface for bulk processing implementation
 *
 * @param <Value> Kafka record value type
 */
public interface BulkSender<Value> extends AutoCloseable {

    /**
     * Process pack of data. In case of unavailability of backend service or services
     * {@link BackendServiceFailedException} should be thrown. In that case {@link CommonBulkEventSink} will stop
     * processing and wait until {@link BulkSender#ping} return true.
     *
     * @param values pack of data
     * @return processing stats
     * @throws BackendServiceFailedException marks that backed service is unavailable
     */
    BulkSenderStat process(Collection<Value> values) throws BackendServiceFailedException;

    /**
     * Ping realization. If ping functionality is not available for service we assume that service is always available
     * until {@link BackendServiceFailedException} is thrown. In that case a significant ping rate
     * {@link CommonBulkEventSink#pingRate} is highly recommended.
     *
     * ping() should throw exception only in case of critical error, in any other cases it should return false. If any
     * exception is thrown {@link CommonBulkEventSink} will be stopped.
     *
     * @return is service available
     */
    default boolean ping() {
        return true;
    }
}
