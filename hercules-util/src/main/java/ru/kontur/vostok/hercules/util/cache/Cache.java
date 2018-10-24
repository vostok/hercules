package ru.kontur.vostok.hercules.util.cache;

import ru.kontur.vostok.hercules.util.functional.Result;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Thread-safe cache of objects
 *
 * @author Gregory Koshelev
 */
public class Cache<K, V> {
    private final long lifetime;

    private final ConcurrentHashMap<K, Cached<V>> cache;

    public Cache(long lifetime) {
        this.lifetime = lifetime;

        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Try to cache value by key from provider if value doesn't exist or expired. Get last cached value.<br>
     * Use old cached valued if provider returns error.
     *
     * @param key      of value
     * @param provider provides values by key
     * @param <E>      is error object
     * @return last cached value
     */
    public final <E> Cached<V> cacheAndGet(K key, Function<K, Result<V, E>> provider) {
        Cached<V> element = cache.get(key);
        if (element == null) {
            Result<V, E> result = provider.apply(key);
            if (!result.isOk()) {
                return Cached.none();
            }
            V value = result.get();
            if (value != null) {
                Cached<V> cached = Cached.of(value, lifetime);
                cache.put(key, cached);
                return cached;
            } else {
                return Cached.none();
            }
        }

        if (element.isAlive()) {
            return element;
        }

        Result<V, E> result = provider.apply(key);
        if (!result.isOk()) {
            return element;
        }
        V value = result.get();
        if (value != null) {
            Cached<V> cached = Cached.of(value, lifetime);
            cache.put(key, cached);
            return cached;
        } else {
            cache.remove(key, element);
            return Cached.none();
        }
    }

    /**
     * Get last cached value or none if it doesn't exist
     *
     * @param key of value
     * @return cached value
     */
    public final Cached<V> get(K key) {
        Cached<V> element = cache.get(key);
        if (element == null) {
            return Cached.none();
        }
        return element;
    }

    /**
     * Put value by key
     *
     * @param key   of value
     * @param value to be cached
     */
    public final void put(K key, V value) {
        cache.put(key, Cached.of(value, lifetime));
    }

    /**
     * Remove value by key from cache
     *
     * @param key of value
     * @return true if value was removed
     */
    public final boolean remove(K key) {
        return cache.remove(key) != null;
    }

    /**
     * Clear cache
     */
    public final void clear() {
        cache.clear();
    }
}
