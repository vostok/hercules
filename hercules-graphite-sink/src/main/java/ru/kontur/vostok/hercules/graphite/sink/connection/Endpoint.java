package ru.kontur.vostok.hercules.graphite.sink.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.graphite.sink.GraphiteMetricData;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Graphite endpoint.
 * <p>
 * An endpoint abstracts a Graphite server (or any other compatible service).
 * An endpoint limits connection count.
 * Internal connections are reusable and are used through {@link Channel channel}.
 *
 * @author Gregory Koshelev
 */
public class Endpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(Endpoint.class);

    private final InetSocketAddress address;
    private final int connectionLimit;
    private final TimeSource time;

    private final ConcurrentLinkedQueue<Connection> connections = new ConcurrentLinkedQueue<>();
    private final AtomicInteger leasedConnections = new AtomicInteger(0);

    private volatile boolean frozen;
    private volatile long frozenToMs;

    public Endpoint(InetSocketAddress address, int connectionLimit, TimeSource time) {
        this.address = address;
        this.connectionLimit = connectionLimit;
        this.time = time;

        this.frozen = false;
        this.frozenToMs = 0;
    }

    /**
     * Return the channel wraps a connection to the endpoint.
     * <p>
     * A channel should be used exclusively by a thread. Normally, {@code try-with-resources} should be used.
     *
     * @return the channel if a connection has been leased, if no connection is available then return {@code null}
     * @throws EndpointException if failed to create a new connection.
     */
    public Channel channel() throws EndpointException {
        Connection connection = leaseConnection();
        if (connection != null) {
            return new Channel(connection);
        }
        return null;
    }

    /**
     * Close active connections.
     */
    public void close() {
        Connection connection;
        while ((connection = connections.poll()) != null) {
            try {
                connection.close();
            } catch (Exception ex) {
                LOGGER.warn("Closing connection to " + address + " failed", ex);
            }
        }
    }

    /**
     * Freeze the endpoint to disable creation of new connections.
     * Existing connections are still available.
     *
     * @param freezeTimeMs freeze time in millis
     */
    public void freeze(long freezeTimeMs) {
        frozenToMs = time.milliseconds() + freezeTimeMs;
        frozen = true;
    }

    /**
     * Verify if the endpoint is frozen and frozen time has not been elapsed yet.
     *
     * @return {@code true} if the endpoint is still frozen, otherwise return {@code false}
     */
    public boolean verifyFrozen() {
        return frozen = frozen && (frozenToMs >= time.milliseconds());
    }

    public int leasedConnections() {
        return leasedConnections.get();
    }

    private Connection leaseConnection() throws EndpointException {
        int currentLeasedConnections;

        do {
            currentLeasedConnections = leasedConnections.get();
            if (currentLeasedConnections >= connectionLimit) {
                return null;
            }
        } while (!leasedConnections.compareAndSet(currentLeasedConnections, currentLeasedConnections + 1));

        Connection connection = connections.poll();
        if (connection != null) {
            return connection;
        }

        if (verifyFrozen()) {
            leasedConnections.decrementAndGet();
            return null;
        }

        try {
            return new Connection();
        } catch (Exception ex) {
            leasedConnections.decrementAndGet();
            throw new EndpointException("Cannot create connection to " + address, ex);
        }
    }

    private void releaseConnection(Connection connection) {
        try {
            if (connection.isBroken()) {
                connection.close();
                return;
            }
            connections.offer(connection);
        } finally {
            leasedConnections.decrementAndGet();
        }
    }

    /**
     * An internal connection to the endpoint.
     * <p>
     * A connection wraps {@link Socket}.
     */
    class Connection {
        private final Socket socket;
        private final Writer writer;
        private boolean broken;

        private Connection() throws IOException {
            this.socket = new Socket(address.getHostName(), address.getPort());
            this.writer =
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    socket.getOutputStream(),
                                    StandardCharsets.US_ASCII));
            this.broken = false;
        }

        /**
         * Send metrics to the endpoint via the connection.
         *
         * @param metrics metrics to send
         * @throws IOException in case of I/O errors
         */
        public void send(List<GraphiteMetricData> metrics) throws IOException {
            try {
                for (GraphiteMetricData metric : metrics) {
                    writer.write(
                            String.format(
                                    Locale.ENGLISH,
                                    "%s %f %d\n",
                                    metric.getMetricName(),
                                    metric.getMetricValue(),
                                    metric.getMetricUnixTime()));
                }
                writer.flush();
            } catch (Exception ex) {
                broken = true;
                throw ex;
            }
        }

        /**
         * Release the connection.
         */
        public void release() {
            try {
                writer.flush();
            } catch (IOException ex) {
                LOGGER.warn("Got I/O exception", ex);
            } finally {
                releaseConnection(this);
            }
        }

        /**
         * Close the connection.
         */
        public void close() {
            try {
                writer.flush();
            } catch (IOException ex) {
                LOGGER.warn("Got I/O exception", ex);
            }

            try {
                socket.close();
            } catch (IOException ex) {
                LOGGER.warn("Closing socket failed with exception", ex);
            }
        }

        /**
         * Check if the connection is broken.
         * <p>
         * A connection is treated as broken if got I/O exception while send metrics.
         *
         * @return {@code true} if the connection is broken, otherwise return {@code false}
         */
        public boolean isBroken() {
            return broken;
        }
    }
}
