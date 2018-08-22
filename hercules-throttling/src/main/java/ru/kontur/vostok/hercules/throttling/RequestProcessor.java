package ru.kontur.vostok.hercules.throttling;

/**
 * @author Gregory Koshelev
 */
public interface RequestProcessor<R, C> {
    void processAsync(R request, C context, ThrottleCallback callback);
}
