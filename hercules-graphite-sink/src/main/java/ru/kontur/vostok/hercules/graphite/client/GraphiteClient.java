package ru.kontur.vostok.hercules.graphite.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

public class GraphiteClient implements GraphiteMetricDataSender, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphiteClient.class);

    private final GraphiteConnectionPool connections;
    private final int retryAttempts;

    public GraphiteClient(String server, int port, int retryAttempts) {
        this.retryAttempts = retryAttempts;
        connections = new GraphiteConnectionPool(server, port);
    }

    @Override
    public void send(Collection<GraphiteMetricData> data) throws Exception {
        Exception lastError = null;

        for (int attempt = 0; attempt < retryAttempts; attempt++) {
            try {
                GraphiteConnection connection = connections.acquire();
                connection.send(data);
                connections.release(connection);
            }
            catch (Exception error) {
                LOGGER.error("Failed to send metrics to Graphite.", error);

                if (!(error instanceof IOException))
                    throw error;

                lastError = error;
            }
        }

        throw lastError;
    }

    @Override
    public void close() throws Exception {
        connections.close();
    }
}

