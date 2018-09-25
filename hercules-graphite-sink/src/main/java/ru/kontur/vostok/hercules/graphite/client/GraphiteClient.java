package ru.kontur.vostok.hercules.graphite.client;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.Locale;

public class GraphiteClient implements GraphiteMetricDataSender {

    private final String server;
    private final int port;

    public GraphiteClient(String server, int port) {
        this.server = server;
        this.port = port;
    }

    @Override
    public void send(Collection<GraphiteMetricData> data) {
        try (
                Socket socket = new Socket(server, port);
                OutputStream stream = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(stream, true)
        ) {
            data.forEach(record -> writer.printf(Locale.ENGLISH, "%s %f %d\n", record.getMetricName(), record.getMetricValue(), record.getMetricUnixTime()));
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
