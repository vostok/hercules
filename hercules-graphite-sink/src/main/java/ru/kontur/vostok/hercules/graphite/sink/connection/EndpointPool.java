package ru.kontur.vostok.hercules.graphite.sink.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.concurrent.ThreadLocalTopology;
import ru.kontur.vostok.hercules.util.concurrent.Topology;
import ru.kontur.vostok.hercules.util.net.InetSocketAddressUtil;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;
import ru.kontur.vostok.hercules.util.validation.LongValidators;

import java.util.Properties;
import java.util.stream.Stream;

/**
 * A pool of endpoints.
 *
 * @author Gregory Koshelev
 */
public class EndpointPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointPool.class);

    private final long frozenTimeMs;
    private final Topology<Endpoint> topology;

    public EndpointPool(Properties properties, TimeSource time) {
        this.frozenTimeMs = PropertiesUtil.get(Props.FROZEN_TIME_MS, properties).get();

        int connectionLimitPerEndpoint = PropertiesUtil.get(Props.CONNECTION_LIMIT_PER_ENDPOINT, properties).get();

        /* After August 17, 292278994 7:12:55 AM UTC connection will be never expire.
        Just keep it in mind if this project will be alive so long */
        long connectionTtlMs = PropertiesUtil.get(Props.CONNECTION_TTL_MS, properties).get();

        int socketTimeoutMs = PropertiesUtil.get(Props.SOCKET_TIMEOUT_MS, properties).get();
        Endpoint[] endpoints =
                Stream.of(PropertiesUtil.get(Props.ENDPOINTS, properties).orEmpty(new String[0])).
                        map(hostAndPort ->
                                new Endpoint(
                                        InetSocketAddressUtil.fromString(hostAndPort, 2003),
                                        connectionLimitPerEndpoint,
                                        connectionTtlMs,
                                        socketTimeoutMs,
                                        time)).
                        toArray(Endpoint[]::new);
        this.topology = new ThreadLocalTopology<>(endpoints);
    }

    /**
     * Return the channel wraps a connection to the endpoint in the pool.
     * <p>
     * A channel should be used exclusively by a thread. Normally, {@code try-with-resources} should be used.
     * No endpoint is available for sending in the pool if the method returns {@code null}.
     *
     * @return the channel if a connection has been leased, otherwise return {@code null}
     */
    public Channel channel() {
        int attemptsLeft = topology.size();

        while (attemptsLeft-- > 0) {
            Endpoint endpoint = topology.next();
            Channel channel;
            try {
                channel = endpoint.channel();
                if (channel != null) {
                    return channel;
                }
            } catch (EndpointException ex) {
                LOGGER.warn("Cannot get channel for endpoint", ex);
                endpoint.freeze(frozenTimeMs);
            }
        }

        return null;
    }

    /**
     * Check if the endpoint pool MAY process metrics.
     *
     * @return {@code true} if the pool is ready
     */
    public boolean isReady() {
        for (Endpoint endpoint : topology.asList()) {
            if (!endpoint.verifyFrozen()) {
                return true;
            }
        }

        try (Channel channel = channel()) {
            return channel != null;
        }
    }

    /**
     * Close active connections to an each endpoint int the pool.
     */
    public void close() {
        for (Endpoint endpoint : topology.asList()) {
            endpoint.close();
        }
    }

    private static class Props {
        static final Parameter<Long> FROZEN_TIME_MS =
                Parameter.longParameter("frozen.time.ms").
                        withDefault(30_000L).
                        withValidator(LongValidators.positive()).
                        build();

        static final Parameter<Integer> CONNECTION_LIMIT_PER_ENDPOINT =
                Parameter.integerParameter("connection.limit.per.endpoint").
                        withDefault(3).
                        build();

        static final Parameter<Long> CONNECTION_TTL_MS =
                Parameter.longParameter("connection.ttl.ms").
                        withDefault(Long.MAX_VALUE).
                        withValidator(LongValidators.positive()).
                        build();

        static final Parameter<Integer> SOCKET_TIMEOUT_MS =
                Parameter.integerParameter("socket.timeout.ms").
                        withDefault(2_000).
                        withValidator(IntegerValidators.nonNegative()).
                        build();

        static final Parameter<String[]> ENDPOINTS =
                Parameter.stringArrayParameter("endpoints").
                        build();
    }
}
