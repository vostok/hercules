package ru.kontur.vostok.hercules.util.cache;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A container which may contain cached object and represents 3 states:<br>
 * 1. Cached object exists and has not expired,<br>
 * 2. Cached object exists but has expired,<br>
 * 3. No object was cached.
 *
 * <p>Cached object has expiration timestamp. Thus, expiration is determined by using {@link System#currentTimeMillis()}.
 *
 * @author Gregory Koshelev
 */
public final class Cached<T> {
    /**
     * Common instance for {@link #none()}.
     */
    private static final Cached<?> NONE = new Cached<>();

    /**
     * Cached object or null otherwise.
     */
    private final T value;

    /**
     * Expiration timestamp in milliseconds from Unix epoch.
     */
    private final long expirationTimestamp;

    /**
     * Constructs an instance with cached object.
     *
     * @param value               the non-null cached value
     * @param expirationTimestamp the expiration timestamp
     */
    Cached(T value, long expirationTimestamp) {
        this.value = Objects.requireNonNull(value);
        this.expirationTimestamp = expirationTimestamp;
    }

    /**
     * Constructs an empty instance.
     */
    private Cached() {
        this.value = null;
        this.expirationTimestamp = Long.MAX_VALUE;
    }

    /**
     * Return {@code true} if value has been cached, otherwise {@code false}.
     *
     * @return {@code true} if value has been cached, otherwise {@code false}
     */
    public boolean isCached() {
        return value != null;
    }

    /**
     * Return {@code true} if cached object has expired, otherwise {@code false}.
     *
     * <p>Expiration is determined by using {@link System#currentTimeMillis()}.
     *
     * @return {@code true} if cached object has expired, otherwise {@code false}
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTimestamp;
    }

    /**
     * Return {@code true} if cached object exists and has not expired, otherwise {@code false}.
     *
     * @return {@code true} if cached object exists and has not expired, otherwise {@code false}
     */
    public boolean isAlive() {
        return isCached() && !isExpired();
    }

    /**
     * If cached object exists, return the object, otherwise throws {@link NoSuchElementException}.
     *
     * @return the non-null cached object
     * @throws NoSuchElementException if there is no cached value
     */
    public T get() {
        if (value == null) {
            throw new NoSuchElementException("No cached value");
        }

        return value;
    }

    /**
     * Returns {@link Cached} with the specified cached object with lifetime defined.
     *
     * @param value    the specified cached object
     * @param lifetime the cache lifetime in milliseconds
     * @param <T>      the type of the value
     * @return {@link Cached} with the specified cached object with lifetime defined
     */
    public static <T> Cached<T> of(T value, long lifetime) {
        return new Cached<>(value, System.currentTimeMillis() + lifetime);
    }

    /**
     * Returns empty {@link Cached} instance. No value is cached for it.
     *
     * @param <T> the type of the non-existent value
     * @return empty {@link Cached}
     */
    public static <T> Cached<T> none() {
        @SuppressWarnings("unchecked")
        Cached<T> c = (Cached<T>) NONE;
        return c;
    }
}
