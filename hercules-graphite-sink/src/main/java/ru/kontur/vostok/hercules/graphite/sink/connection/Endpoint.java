package ru.kontur.vostok.hercules.graphite.sink.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.concurrent.ThreadFactories;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.Timer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    private static final long SHUTDOWN_TIMEOUT_MS = 5_000L;

    private final ExecutorService executor;
    private final InetSocketAddress address;
    private final int connectionLimit;
    private final int socketTimeoutMs;
    private final int requestTimeoutMs;
    private final TimeSource time;

    final ConcurrentLinkedQueue<Connection> connections = new ConcurrentLinkedQueue<>();
    private final long connectionTtlMs;

    private volatile boolean frozen;
    private volatile long frozenToMs;

    public Endpoint(
            InetSocketAddress address,
            int poolSize,
            int connectionLimit,
            long connectionTtlMs,
            int socketTimeoutMs,
            int requestTimeoutMs,
            TimeSource time
    ) {
        this(Executors.newFixedThreadPool(poolSize,
                ThreadFactories.newNamedThreadFactory("endpoint_" + address, false)
        ), address, connectionLimit, connectionTtlMs, socketTimeoutMs, requestTimeoutMs, time);
    }

    public Endpoint(
            ExecutorService executor,
            InetSocketAddress address,
            int connectionLimit,
            long connectionTtlMs,
            int socketTimeoutMs,
            int requestTimeoutMs,
            TimeSource time
    ) {
        this.executor = executor;
        this.address = address;
        this.connectionLimit = connectionLimit;
        this.connectionTtlMs = connectionTtlMs;
        this.socketTimeoutMs = socketTimeoutMs;
        this.requestTimeoutMs = requestTimeoutMs;
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
        if (verifyFrozen()) {
            LOGGER.debug("Skip frozen endpoint");
            return null;
        }

        Connection connection = connections.poll();
        if (connection != null) {
            return new Channel(connection);
        }

        LOGGER.debug("Create new connection, pool is empty");
        return new Channel(newConnection());
    }

    /**
     * Return the channel wraps a connection to the endpoint.
     * <p>
     * A channel should be used exclusively by a thread. Normally, {@code try-with-resources} should be used.
     *
     * @return the channel with new connection
     * @throws EndpointException if failed to create a new connection.
     */
    public Channel channelForRetry() throws EndpointException {
        if (verifyFrozen()) {
            LOGGER.debug("Skip frozen endpoint");
            return null;
        }

        LOGGER.debug("Create new connection on retry");
        return new Channel(newConnection());
    }

    private Connection newConnection() throws EndpointException {
        try {
            return new Connection();
        } catch (Exception ex) {
            throw new EndpointException("Cannot create connection to " + address, ex);
        }
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

        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    LOGGER.warn("Thread pool did not terminate");
                }
            } catch (InterruptedException ex) {
                /* Interruption during shutdown */
                Thread.currentThread().interrupt();
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

    private void releaseConnection(Connection connection) {
        if (connection.isBroken()) {
            LOGGER.info("Close broken connection");
            connection.close();
            maintainConnections();
            return;
        }

        if (connection.isExpired()) {
            LOGGER.debug("Close expired connection");
            connection.close();
            maintainConnections();
            return;
        }

        if (connections.size() >= connectionLimit * 2) {
            LOGGER.warn("Close connection, pool is full, connection.limit.per.endpoint must be >= poolSize");
            connection.close();
        } else {
            connections.offer(connection);
            maintainConnections();
        }
    }

    private void maintainConnections() {
        if (!verifyFrozen() && connections.size() < connectionLimit) {
            LOGGER.debug("Create new connection, maintain pool");
            try {
                connections.offer(new Connection());
            } catch (IOException ex) {
                LOGGER.debug("Cannot create additional connection", ex);
            }
        }
    }

    Socket getSocket() throws IOException {
        Socket socket = new Socket(Proxy.NO_PROXY);
        // There is no reason to call #setSoTimeout since SO_TIMEOUT is only used on read from socket.
        socket.connect(address, socketTimeoutMs);
        socket.setKeepAlive(true);
        return socket;
    }

    /**
     * An internal connection to the endpoint.
     * <p>
     * A connection wraps {@link Socket}.
     */
    public class Connection {
        private final Socket socket;
        private final Writer writer;
        private boolean broken;
        private final long expiresAtMs;
        private final Timer timer;

        public Connection() throws IOException {
            this.socket = getSocket();

            this.writer =
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    socket.getOutputStream(),
                                    StandardCharsets.US_ASCII));
            this.broken = false;

            long expiresAt = time.milliseconds() + connectionTtlMs;
            this.expiresAtMs = (expiresAt > 0) ? expiresAt : Long.MAX_VALUE;
            this.timer = time.timer(requestTimeoutMs);
        }

        /**
         * Send metrics to the endpoint via the connection.
         *
         * @param metrics metrics to send
         * @throws EndpointException in case of I/O errors or timeout
         */
        public void send(List<String> metrics) throws EndpointException {
            timer.reset();

            Future<Result<Void, Exception>> asyncTask = executor.submit(() -> sendMetrics(metrics));

            try {
                Result<Void, Exception> result = asyncTask.get(timer.remainingTimeMs(), TimeUnit.MILLISECONDS);
                if (!result.isOk()) {
                    broken = true;
                    throw new EndpointException("Got I/O exception", result.getError());
                }
            } catch (ExecutionException | TimeoutException ex) {
                broken = true;
                throw new EndpointException("Cannot send metrics", ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        private Result<Void, Exception> sendMetrics(List<String> metrics) {
            try {
                for (String metric : metrics) {
                    writer.write(metric);
                }
                writer.flush();
                return Result.ok();
            } catch (IOException ex) {
                return Result.error(ex);
            }
        }

        /**
         * Release the connection.
         */
        public void release() {
            releaseConnection(this);
        }

        /**
         * Close the connection.
         */
        public void close() {
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

        /**
         * Check if the connection is expired.
         *
         * @return {@code true} if the connection is expired, otherwise return {@code false}
         *
         * <p>
         */
        public boolean isExpired() {
            return time.milliseconds() > this.expiresAtMs;
        }
    }
}
