package ru.kontur.vostok.hercules.meta.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.cache.Cache;
import ru.kontur.vostok.hercules.util.cache.Cached;
import ru.kontur.vostok.hercules.util.functional.Result;

import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class StreamStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamStorage.class);

    private final StreamRepository repository;

    private final Cache<String, Stream> cache;

    public StreamStorage(StreamRepository repository) {
        this.repository = repository;

        this.cache = new Cache<>(30_000L /* TODO: for test usages; It should be moved to configuration */);
    }

    /**
     * Get Stream from local cache if possible or read from repository.<br>
     * If repository throws exception then return expired cached value or empty if it doesn't exist.
     *
     * @param name of the Stream
     * @return Optional of the found Stream or empty otherwise
     */
    public Optional<Stream> read(String name) {
        Cached<Stream> streamCached = cache.cacheAndGet(name, key -> {
            try {
                Optional<Stream> stream = repository.read(key);
                if (stream.isPresent()) {
                    return Result.ok(stream.get());
                }
                return Result.ok(null);
            } catch (Exception e) {
                LOGGER.warn("Cannot read from repository", e);
                return Result.error("Cannot read from repository");
            }
        });
        return streamCached.isCached() ? Optional.of(streamCached.get()) : Optional.empty();
    }
}
