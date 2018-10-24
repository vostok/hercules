package ru.kontur.vostok.hercules.util.cache;

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

    public final Cached<V> cacheAndGet(K key, Function<K, V> provider) {
        Cached<V> element = cache.get(key);
        if (element == null) {
            V value = provider.apply(key);
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
        V value = provider.apply(key);
        if (value != null) {
            Cached<V> cached = Cached.of(value, lifetime);
            cache.put(key, cached);
            return cached;
        } else {
            cache.remove(key, element);
            return Cached.none();
        }
    }

    public final Cached<V> get(K key) {
        Cached<V> element = cache.get(key);
        if (element == null) {
            return Cached.none();
        }
        return element;
    }

    public final void put(K key, V value) {
        cache.put(key, Cached.of(value, lifetime));
    }

    public final boolean remove(K key) {
        return cache.remove(key) != null;
    }

    public final void clear() {
        cache.clear();
    }
}
