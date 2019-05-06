package ru.kontur.vostok.hercules.graphite.client;

import java.util.concurrent.ConcurrentLinkedQueue;

public class GraphiteConnectionPool implements AutoCloseable {
    private final String server;
    private final int port;
    private final ConcurrentLinkedQueue<GraphiteConnection> connections;

    public GraphiteConnectionPool(String server, int port) {
        this.server = server;
        this.port = port;

        connections = new ConcurrentLinkedQueue<GraphiteConnection>();
    }

    public GraphiteConnection acquire() {
        GraphiteConnection cachedConnection = connections.poll();

        return cachedConnection != null ? cachedConnection : new GraphiteConnection(server, port);
    }

    public void release(GraphiteConnection connection) {
        connections.offer(connection);
    }

    @Override
    public void close() throws Exception {
        for (GraphiteConnection connection : connections) {
            connection.close();
        }
        connections.clear();
    }
}
