package ru.kontur.vostok.hercules.graphite.sink.client;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GraphiteClient {

    private final String server;
    private final int port;

    public GraphiteClient(String server, int port) {
        this.server = server;
        this.port = port;
    }

    public void send(GraphiteMetricStorage metrics) {
        try (
                Socket socket = new Socket(server, port);
                OutputStream stream = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(stream, true)
        ) {
            for (Map.Entry<String, List<GraphiteMetric>> entry : metrics.getMetrics().entrySet()) {
                String metricName = entry.getKey();
                for (GraphiteMetric graphiteMetric : entry.getValue()) {
                    writer.printf(Locale.ENGLISH, "%s %f %d\n", metricName, graphiteMetric.getValue(), graphiteMetric.getTimestamp());
                }
            }
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
