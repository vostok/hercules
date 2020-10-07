package ru.kontur.vostok.hercules.graphite.sink;

import ru.kontur.vostok.hercules.graphite.sink.connection.Channel;
import ru.kontur.vostok.hercules.graphite.sink.connection.EndpointPool;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.util.Properties;

/**
 * Graphite connector provides two level of an endpoint topology: local and remote.
 * <p>
 * Local endpoints are in the same data-center, whereas remote endpoints are located in another data-centers.
 * The list of remote endpoints can be empty.
 *
 * @author Gregory Koshelev
 */
public class GraphiteConnector {
    private final EndpointPool localEndpoints;
    private final EndpointPool remoteEndpoints;

    public GraphiteConnector(Properties properties) {
        this(properties, TimeSource.SYSTEM);
    }

    GraphiteConnector(Properties properties, TimeSource time) {
        this.localEndpoints = new EndpointPool(PropertiesUtil.ofScope(properties, "local"), time);
        this.remoteEndpoints = new EndpointPool(PropertiesUtil.ofScope(properties, "remote"), time);
    }

    /**
     * Return the channel wraps a connection to the endpoint (local or remote).
     * <p>
     * A channel should be used exclusively by a thread. Normally, {@code try-with-resources} should be used.
     * No endpoint is available for sending if the method returns {@code null}.
     * <p>
     * Local endpoints have higher priority than remote endpoints.
     *
     * @return the channel if a connection has been leased, otherwise return {@code null}
     */
    public Channel channel() {
        Channel channel = localEndpoints.channel();
        return channel != null ? channel : remoteEndpoints.channel();
    }

    /**
     * Check if at least one of the endpoints MAY process metrics.
     *
     * @return {@code true} if the connector is ready
     */
    public boolean isReady() {
        return localEndpoints.isReady() || remoteEndpoints.isReady();
    }

    /**
     * Close active connections to an each endpoint (local or remote).
     */
    public void close() {
        localEndpoints.close();
        remoteEndpoints.close();
    }
}
