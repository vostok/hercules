package ru.kontur.vostok.hercules.graphite;

import ru.kontur.vostok.hercules.sink.SenderStatus;

import java.io.IOException;
import java.net.Socket;

public class GraphitePinger {
    private final String server;
    private final int port;

    public GraphitePinger(String server, int port) {
        this.server = server;
        this.port = port;
    }

    public SenderStatus ping() {
        try {
            new Socket(server, port).close();
            return SenderStatus.AVAILABLE;
        } catch (IOException exception) {
            return SenderStatus.UNAVAILABLE;
        }
    }
}
