package ru.kontur.vostok.hercules.client.exceptions;

import org.apache.http.HttpStatus;

/**
 * ForbiddenException
 *
 * @author Kirill Sulim
 */
public class ForbiddenException extends HerculesClientException {

    public ForbiddenException(final String resource, final String credentials) {
        super(
                String.format("Resource '%s' forbidden for credentials '%s'", resource, credentials),
                HttpStatus.SC_FORBIDDEN
        );
    }
}
