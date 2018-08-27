package ru.kontur.vostok.hercules.throttling;

/**
 * @author Gregory Koshelev
 */
public interface ThrottledRequestProcessor<R> {
    void processAsync(R request, ThrottledBy throttledBy);
}
