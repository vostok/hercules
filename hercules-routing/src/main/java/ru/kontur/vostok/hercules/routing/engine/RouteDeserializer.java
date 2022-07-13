package ru.kontur.vostok.hercules.routing.engine;

import java.util.UUID;

/**
 * Interface of route deserializer.
 *
 * @author Aleksandr Yuferov
 */
public interface RouteDeserializer {

    /**
     * Do deserialization.
     *
     * @param id   Route id.
     * @param data Route body raw data.
     * @return Route constructed from the given data.
     * @throws Exception Exception can be thrown if deserialization is failed.
     */
    Route deserialize(UUID id, byte[] data) throws Exception;
}
