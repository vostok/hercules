package ru.kontur.vostok.hercules.client.exceptions;

import java.util.Optional;

/**
 * HerculesClientExceptionUtil
 *
 * @author Kirill Sulim
 */
public final class HerculesClientExceptionUtil {

    public static Optional<HerculesClientException> exceptionFromStatus(
            final int httpStatus,
            final String resourceName,
            final String apiKey
    ) {
        switch (httpStatus) {
            case 200: return Optional.empty();
            case 400: return Optional.of(new BadRequestException());
            case 401: return Optional.of(new UnauthorizedException());
            case 403: return Optional.of(new ForbiddenException(resourceName, apiKey));
            case 404: return Optional.of(new NotFoundException(resourceName));
            default: return Optional.of(new HerculesClientException("Unknown exception"));
        }
    }

    private HerculesClientExceptionUtil() {
        /* static class */
    }
}
