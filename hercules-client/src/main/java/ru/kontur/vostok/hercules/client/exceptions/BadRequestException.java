package ru.kontur.vostok.hercules.client.exceptions;

import org.apache.http.HttpStatus;

/**
 * BadRequestException
 *
 * @author Kirill Sulim
 */
public class BadRequestException extends HerculesClientException {

    public BadRequestException() {
        super("Bad request", HttpStatus.SC_BAD_REQUEST);
    }
}
