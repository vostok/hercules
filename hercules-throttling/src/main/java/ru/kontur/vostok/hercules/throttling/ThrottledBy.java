package ru.kontur.vostok.hercules.throttling;

/**
 * @author Gregory Koshelev
 */
public enum ThrottledBy {
    NONE,
    EXPIRATION,
    QUEUE_OVERFLOW,
    INTERRUPTION;
}
