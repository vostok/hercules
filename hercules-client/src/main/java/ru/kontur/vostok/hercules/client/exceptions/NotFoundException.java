package ru.kontur.vostok.hercules.client.exceptions;

import org.apache.http.HttpStatus;

/**
 * NotFoundException
 *
 * @author Kirill Sulim
 */
public class NotFoundException extends HerculesClientException {

    public NotFoundException(final String resourceDescription) {
        super(String.format("Resource '%s' not found", resourceDescription), HttpStatus.SC_NOT_FOUND);
    }
}
