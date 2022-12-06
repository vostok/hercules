package ru.kontur.vostok.hercules.sentry.client.impl.client.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.Dsn;
import ru.kontur.vostok.hercules.util.routing.SentryDestination;

/**
 * @author Tatyana Tokmyanina
 */
public class DsnCache {

    private final LoadingCache<SentryDestination, Dsn> dsnCache;

    public DsnCache(long cacheTtlMs, Function<SentryDestination, Dsn> provider) {
        CacheLoader<SentryDestination, Dsn> loader = new CacheLoader<>() {
            @Override
            public Dsn load(@NotNull SentryDestination key) {
                return provider.apply(key);
            }
        };

        this.dsnCache = CacheBuilder
                .newBuilder()
                .expireAfterAccess(cacheTtlMs, TimeUnit.MILLISECONDS)
                .build(loader);
    }

    public Dsn cacheAndGet(SentryDestination destination) throws ExecutionException {
        try {
            return dsnCache.get(destination);
        } catch (InvalidCacheLoadException e) {
            return null;
        }
    }

    public void invalidate(SentryDestination destination) {
        dsnCache.invalidate(destination);
    }
}
