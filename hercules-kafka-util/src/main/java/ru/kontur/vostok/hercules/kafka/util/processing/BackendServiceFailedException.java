package ru.kontur.vostok.hercules.kafka.util.processing;

/**
 * BackendServiceFailedException - this exception should be thrown if processing of events is no longer possible.
 *
 * @author Kirill Sulim
 */
public class BackendServiceFailedException extends Exception {

    private static final String ERROR_MESSAGE = "There are some problems with underlying backend.";

    public BackendServiceFailedException() {
        super(ERROR_MESSAGE);
    }

    public BackendServiceFailedException(Throwable cause) {
        super(ERROR_MESSAGE, cause);
    }

    public BackendServiceFailedException(String message) {
        super(message);
    }
}
