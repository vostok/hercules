package ru.kontur.vostok.hercules.util.cache;

import java.util.NoSuchElementException;

/**
 * @author Gregory Koshelev
 */
public final class Cached<T> {
    private final T value;
    private final long expirationTimestamp;

    private static final Cached<?> NONE = new Cached<>();

    public Cached(T value, long expirationTimestamp) {
        this.value = value;
        this.expirationTimestamp = expirationTimestamp;
    }

    private Cached() {
        this.value = null;
        this.expirationTimestamp = Long.MAX_VALUE;
    }

    public boolean isCached() {
        return value != null;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTimestamp;
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

    public static <T> Cached<T> of(T value, long lifetime) {
        return new Cached<>(value, System.currentTimeMillis() + lifetime);
    }

    public static <T> Cached<T> none() {
        @SuppressWarnings("unchecked")
        Cached<T> c = (Cached<T>) NONE;
        return c;
    }
}
