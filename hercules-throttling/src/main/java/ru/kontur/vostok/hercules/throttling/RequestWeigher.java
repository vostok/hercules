package ru.kontur.vostok.hercules.throttling;

/**
 * @author Gregory Koshelev
 */
public interface RequestWeigher<R> {
    int weigh(R request);
}
