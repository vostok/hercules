package ru.kontur.vostok.hercules.kafka.util.processing;

/**
 * BackendServiceFailedException - this exception should be thrown if processing of events is no longer possible.
 * {@link CommonBulkEventSink} will wait until status of underlying backends will be changed by
 * {@link CommonBulkEventSink#markBackendAlive()}
 *
 * @author Kirill Sulim
 */
public class BackendServiceFailedException extends Exception {

    public BackendServiceFailedException() {
        super("There are some problems with underlying backends.");
    }

    public BackendServiceFailedException(Throwable cause) {
        super("There are some problems with underlying backends.", cause);
    }
}
