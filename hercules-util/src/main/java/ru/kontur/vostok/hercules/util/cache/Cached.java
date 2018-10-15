package ru.kontur.vostok.hercules.util.cache;

import java.util.NoSuchElementException;

/**
 * @author Gregory Koshelev
 */
public final class Cached<T> {
    private final T value;
    private final boolean expired;

    private static final Cached<?> NONE = new Cached<>();

    private Cached(T value, boolean expired) {
        this.value = value;
        this.expired = expired;
    }

    private Cached() {
        this.value = null;
        this.expired = false;
    }

    public boolean isCached() {
        return value != null;
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean isAlive() {
        return isCached() && !isExpired();
    }

    public T get() {
        if (value == null) {
            throw new NoSuchElementException("No cached value");
        }

        return value;
    }

    public static <T> Cached<T> of(T value) {
        return new Cached<>(value, false);
    }

    public static <T> Cached<T> expired(T value) {
        return new Cached<>(value, true);
    }

    public static <T> Cached<T> none() {
        @SuppressWarnings("unchecked")
        Cached<T> c = (Cached<T>) NONE;
        return c;
    }
}
