package ru.kontur.vostok.hercules.graphite.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

/**
 * GraphiteClient is designed to send batches of metric events to Graphite over a TCP connection.
 * It is thread-safe and can be safely used a singleton.
 * Underlying TCP connections are pooled and reused.
 * It also employs a simple retry policy configured by attempts count in constructor.
 *
 * @deprecated use {@link GraphiteConnector} instead
 */
@Deprecated
public class GraphiteClient implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphiteClient.class);

    private final GraphiteConnectionPool connections;
    private final int attempts;

    public GraphiteClient(String server, int port, int attempts) {
        this.attempts = attempts;
        connections = new GraphiteConnectionPool(server, port);
    }

    public void send(Collection<GraphiteMetricData> data) throws Exception {
        Exception lastException = null;

        for (int attempt = 0; attempt < attempts; attempt++) {
            GraphiteConnection connection = null;

            try {
                connection = connections.acquire();
                connection.send(data);
                connections.release(connection);
                return;
            } catch (Exception exception) {
                LOGGER.error("Failed to send metrics to Graphite.", exception);

                if (connection != null)
                    connection.close();

                if (!(exception instanceof IOException)) {
                    throw exception;
                }

                lastException = exception;
            }
        }

        throw lastException;
    }

    @Override
    public void close() {
        connections.close();
    }
}

