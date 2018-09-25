package ru.kontur.vostok.hercules.graphite.client;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collection;
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

    public void send(Collection<GraphiteMetricData> metricData) {
        try (
                Socket socket = new Socket(server, port);
                OutputStream stream = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(stream, true)
        ) {
            metricData.forEach(data -> writer.printf(Locale.ENGLISH, "%s %f %d\n", data.getMetricName(), data.getMetricValue(), data.getMetricUnixTime()));
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
