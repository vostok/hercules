package ru.kontur.vostok.hercules.gate.client.exception;

/**
 * @author Daniil Zhenikhov
 */
public class UnavailableClusterException extends HerculesGateClientException {
    private static final String MESSAGE = "Cluster is unavailable";

    public UnavailableClusterException() {
        super(MESSAGE);
    }

    public UnavailableClusterException(Exception cause) {
        super(MESSAGE, cause);
    }
}
