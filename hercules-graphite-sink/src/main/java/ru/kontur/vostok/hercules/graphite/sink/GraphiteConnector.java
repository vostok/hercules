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

    public GraphiteConnector(Properties properties, int retryLimit) {
        this(properties, retryLimit, TimeSource.SYSTEM);
    }

    GraphiteConnector(Properties properties, int retryLimit, TimeSource time) {
        this.localEndpoints = new EndpointPool(PropertiesUtil.ofScope(properties, "local"), retryLimit, time);
        this.remoteEndpoints = new EndpointPool(PropertiesUtil.ofScope(properties, "remote"), retryLimit, time);
        if (localEndpoints.isEmpty() && remoteEndpoints.isEmpty()) {
            throw new IllegalStateException("Endpoints must not be empty");
        }
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
    public Channel channel(boolean isRetry) {
        Channel channel = localEndpoints.channel(isRetry);
        if (remoteEndpoints.isEmpty()) {
            return channel;
        } else {
            return channel != null ? channel : remoteEndpoints.channel(isRetry);
        }
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
