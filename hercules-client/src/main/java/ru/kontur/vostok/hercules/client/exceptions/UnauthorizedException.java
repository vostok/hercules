package ru.kontur.vostok.hercules.client.exceptions;

import org.apache.http.HttpStatus;

/**
 * UnauthorizedException
 *
 * @author Kirill Sulim
 */
public class UnauthorizedException extends HerculesClientException {

    public UnauthorizedException() {
        super("Unauthorized", HttpStatus.SC_UNAUTHORIZED);
    }
}
