package ru.kontur.vostok.hercules.throttling;

/**
 * @author Gregory Koshelev
 */
public interface ThrottledRequestProcessor<R> {
    void process(R request, ThrottledBy throttledBy);
}
