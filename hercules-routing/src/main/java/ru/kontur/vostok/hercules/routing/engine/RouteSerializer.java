package ru.kontur.vostok.hercules.routing.engine;

/**
 * Interface of routes serializer.
 *
 * @author Aleksandr Yuferov
 */
public interface RouteSerializer {
    /**
     * Do serialization of given object.
     *
     * @param route Route for deserialization.
     * @return Serialized object and mime type of result.
     * @throws Exception Exception can be thrown by serializer if serialization is failed.
     */
    byte[] serialize(Route route) throws Exception;
}
