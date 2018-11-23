package ru.kontur.vostok.hercules.client.exceptions;

/**
 * HerculesClientException
 *
 * @author Kirill Sulim
 */
public class HerculesClientException extends Exception {

    public HerculesClientException() {
    }

    public HerculesClientException(String message) {
        super(message);
    }

    public HerculesClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
