package ru.kontur.vostok.hercules.graphite.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;

/**
 * Not thread-safe, expected to be used exclusively by one thread at any given moment.
 */
public class GraphiteConnection implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphiteConnection.class);

    private final String server;
    private final int port;

    private volatile Socket socket;
    private volatile OutputStream outputStream;
    private volatile OutputStreamWriter outputWriter;
    private volatile PrintWriter printWriter;

    public GraphiteConnection(String server, int port) {
        this.server = server;
        this.port = port;
    }

    public void send(Collection<GraphiteMetricData> metrics) throws IOException {
        LOGGER.info("Sending {} metric(s) to Graphite..", metrics.size());

        if (printWriter == null) {
            LOGGER.info("Opening a connection to Graphite server at {}:{}..", server, port);

            socket = new Socket(server, port);
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);

            outputStream = new BufferedOutputStream(socket.getOutputStream(), 64 * 1024);
            outputWriter = new OutputStreamWriter(outputStream, StandardCharsets.US_ASCII);
            printWriter = new PrintWriter(outputWriter, false);
        }

        metrics.forEach(record -> printWriter.printf(Locale.ENGLISH, "%s %f %d\n", record.getMetricName(), record.getMetricValue(), record.getMetricUnixTime()));

        printWriter.flush();
        outputWriter.flush();
        outputStream.flush();

        LOGGER.info("Successfully sent {} metric(s) to Graphite.", metrics.size());
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (Exception exception) {
            LOGGER.warn("Failed to close a connection to Graphite.", exception);
        }
    }
}
