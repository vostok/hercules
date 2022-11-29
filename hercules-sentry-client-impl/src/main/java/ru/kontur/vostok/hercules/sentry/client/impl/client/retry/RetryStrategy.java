package ru.kontur.vostok.hercules.sentry.client.impl.client.retry;

import java.net.http.HttpResponse;

/**
 * @author Aleksandr Yuferov
 */
public interface RetryStrategy {

    boolean shouldRetry(int tryCount, HttpResponse<?> response);
}
