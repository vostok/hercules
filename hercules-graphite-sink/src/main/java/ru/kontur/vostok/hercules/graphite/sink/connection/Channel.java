package ru.kontur.vostok.hercules.graphite.sink.connection;

import ru.kontur.vostok.hercules.graphite.sink.GraphiteMetricData;

import java.io.IOException;
import java.util.List;

/**
 * A channel provides per thread sending to the {@link Endpoint endpoint}.
 * <p>
 * A channel MUST be closed after using. An underlying connection will be reused.
 *
 * @author Gregory Koshelev
 */
public class Channel implements AutoCloseable {
    private final Endpoint.Connection connection;

    Channel(Endpoint.Connection connection) {
        this.connection = connection;
    }

    /**
     * Send metrics to the endpoint via a connection.
     *
     * @param metrics metrics to send
     * @throws IOException in case of I/O errors
     */
    public void send(List<GraphiteMetricData> metrics) throws IOException {
        connection.send(metrics);
    }

    @Override
    public void close() {
        connection.release();
    }
}
