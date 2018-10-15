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

    private final ConcurrentHashMap<K, CacheElement<V>> cache;

    public Cache(long lifetime) {
        this.lifetime = lifetime;

        this.cache = new ConcurrentHashMap<>();
    }

    public final Cached<V> cacheAndGet(K key, Function<K, V> provider) {
        CacheElement<V> element = cache.get(key);
        if (element == null) {
            V value = provider.apply(key);
            if (value != null) {
                cache.put(key, new CacheElement<>(value));
                return Cached.of(value);
            } else {
                return Cached.none();
            }
        }
        if (isAlive(element)) {
            return Cached.of(element.value);
        }
        V value = provider.apply(key);
        if (value != null) {
            cache.put(key, new CacheElement<>(value));
            return Cached.of(value);
        } else {
            cache.remove(key, element);
            return Cached.none();
        }
    }

    public final Cached<V> get(K key) {
        CacheElement<V> element = cache.get(key);
        if (element == null) {
            return Cached.none();
        }
        return isAlive(element)
                ? Cached.of(element.value)
                : Cached.expired(element.value);
    }

    public final void put(K key, V value) {
        cache.put(key, new CacheElement<>(value));
    }

    public final boolean remove(K key) {
        return cache.remove(key) != null;
    }

    public final void clear() {
        cache.clear();
    }

    private boolean isAlive(CacheElement<V> element) {
        return System.currentTimeMillis() <= element.timestamp + lifetime;
    }

    private static class CacheElement<V> {
        private final V value;
        private final long timestamp;

        CacheElement(V value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
