package ru.kontur.vostok.hercules.gate.client.exception;

/**
 * @author Daniil Zhenikhov
 */
public class UnavailableHostException extends HerculesGateClientException{
    private static final String MESSAGE = "Host is unavailable";
    private static final String MESSAGE_TEMPLATE = "Host[%s] is unavailable";

    public UnavailableHostException() {
        super(MESSAGE);
    }

    public UnavailableHostException(Exception cause) {
        super(MESSAGE, cause);
    }

    public UnavailableHostException(String host) {
        super(String.format(MESSAGE_TEMPLATE, host));
    }

    public UnavailableHostException(String host, Exception cause) {
        super(String.format(MESSAGE_TEMPLATE, host), cause);
    }
}
