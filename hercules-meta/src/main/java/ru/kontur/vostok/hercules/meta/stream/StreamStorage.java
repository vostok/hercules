package ru.kontur.vostok.hercules.meta.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.cache.Cache;
import ru.kontur.vostok.hercules.util.cache.Cached;

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
     * Get Stream from local cache if it doesn't expired. Otherwise read it from repository and persist it to cache.<br>
     * If repository throws exception then return expired cached value or empty if it doesn't exist.
     *
     * @param name of the Stream
     * @return Optional of the found Stream or empty otherwise
     */
    public Optional<Stream> read(String name) {
        Cached<Stream> streamCached = cache.get(name);
        if (streamCached.isAlive()) {
            return Optional.of(streamCached.get());
        }
        Optional<Stream> stream;
        try {
            stream = repository.read(name);
        } catch (Exception e) {
            LOGGER.warn("Cannot read from repository", e);
            return streamCached.isCached() ? Optional.of(streamCached.get()) : Optional.empty();
        }
        if (stream.isPresent()) {
            cache.put(name, stream.get());
        } else {
            cache.remove(name);
        }
        return stream;
    }
}
