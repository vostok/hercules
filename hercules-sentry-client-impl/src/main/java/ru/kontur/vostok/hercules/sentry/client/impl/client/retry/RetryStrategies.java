package ru.kontur.vostok.hercules.sentry.client.impl.client.retry;

import ru.kontur.vostok.hercules.http.HttpStatusCodes;

/**
 * @author Aleksandr Yuferov
 */
public final class RetryStrategies {

    public static RetryStrategy noRetry() {
        return (tryCount, response) -> false;
    }

    public static RetryStrategy onTemporaryErrors(int maxTryCount) {
        return (tryCount, response) -> {
            if (tryCount > maxTryCount) {
                return false;
            }
            if (response.statusCode() >= 500) {
                return true;
            }
            switch (response.statusCode()) {
                case HttpStatusCodes.UNAUTHORIZED:
                case HttpStatusCodes.FORBIDDEN:
                case HttpStatusCodes.NOT_FOUND:
                case HttpStatusCodes.REQUEST_TIMEOUT:
                    return true;
            }
            return false;
        };
    }
}
