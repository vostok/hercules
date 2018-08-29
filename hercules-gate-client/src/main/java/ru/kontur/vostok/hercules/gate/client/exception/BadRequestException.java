package ru.kontur.vostok.hercules.gate.client.exception;

/**
 * @author Daniil Zhenikhov
 */
public class BadRequestException extends HerculesGateClientException{
    private static final String MESSAGE = "Bad request from client";
    private static final String STATUS_CODE_TEMPLATE = "Response was taken with status code %d";

    public BadRequestException() {
        super(MESSAGE);
    }

    public BadRequestException(Exception cause) {
        super(MESSAGE, cause);
    }

    public BadRequestException(String message, Exception cause) {
        super(message, cause);
    }

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(int statusCode) {
        super(String.format(STATUS_CODE_TEMPLATE, statusCode));
    }

    public BadRequestException(int statusCode, Exception cause) {
        super(String.format(STATUS_CODE_TEMPLATE, statusCode), cause);
    }
}
