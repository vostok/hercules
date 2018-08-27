package ru.kontur.vostok.hercules.throttling;

import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public interface Throttle<R, C> {
    void throttleAsync(R request, C context);

    void shutdown(long timeout, TimeUnit unit);
}
