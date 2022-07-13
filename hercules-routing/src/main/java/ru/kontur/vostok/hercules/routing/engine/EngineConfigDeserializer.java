package ru.kontur.vostok.hercules.routing.engine;

/**
 * Engine config deserializer interface.
 *
 * @author Aleksandr Yuferov
 */
public interface EngineConfigDeserializer {
    /**
     * Do deserialization.
     *
     * @param data Raw data.
     * @return Router engine config constructed from the given data.
     * @throws Exception Exception can be thrown if deserialization is failed.
     */
    EngineConfig deserialize(byte[] data) throws Exception;
}
