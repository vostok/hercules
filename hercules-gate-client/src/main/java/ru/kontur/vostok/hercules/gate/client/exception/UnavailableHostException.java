package ru.kontur.vostok.hercules.gate.client.exception;

/**
 * @author Daniil Zhenikhov
 */
public class UnavailableHostException extends HerculesGateClientException{
    private static final String MESSAGE = "Host is unavailable";

    public UnavailableHostException() {
        super(MESSAGE);
    }

    public UnavailableHostException(Exception cause) {
        super(MESSAGE, cause);
    }
}
