package ru.kontur.vostok.hercules.gate.client.exception;

/**
 * Common class exception of Hercules Gate Client
 *
 * @author Daniil Zhenikhov
 */
public class HerculesGateClientException extends Exception {
    public HerculesGateClientException() {
        super();
    }

    public HerculesGateClientException(String message) {
        super(message);
    }

    public HerculesGateClientException(Exception cause) {
        super(cause);
    }

    public HerculesGateClientException(String message, Exception cause) {
        super(message, cause);
    }
}
