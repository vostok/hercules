package ru.kontur.vostok.hercules.gate.client.exception;

/**
 * @author Daniil Zhenikhov
 */
public class BadRequestException extends HerculesGateClientException{
    private static final String MESSAGE = "Bad request from client";

    public BadRequestException() {
        super(MESSAGE);
    }

    public BadRequestException(Exception cause) {
        super(MESSAGE, cause);
    }
}
