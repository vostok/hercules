package ru.kontur.vostok.hercules.routing.engine;

/**
 * Engine config serializer interface.
 *
 * @author Aleksandr Yuferov
 */
public interface EngineConfigSerializer {
    /**
     * Do serialization of given object.
     *
     * @param engineConfig Router engine config for deserialization.
     * @return Serialized object and mime type of result.
     * @throws Exception Exception can be thrown if serialization is failed.
     */
    byte[] serialize(EngineConfig engineConfig) throws Exception;
}
