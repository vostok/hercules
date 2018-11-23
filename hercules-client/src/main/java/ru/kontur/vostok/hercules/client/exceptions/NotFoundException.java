package ru.kontur.vostok.hercules.client.exceptions;

/**
 * NotFoundException
 *
 * @author Kirill Sulim
 */
public class NotFoundException extends HerculesClientException {

    public NotFoundException(final String resourceDescription) {
        super(String.format("Resource '%s' not found", resourceDescription));
    }
}
