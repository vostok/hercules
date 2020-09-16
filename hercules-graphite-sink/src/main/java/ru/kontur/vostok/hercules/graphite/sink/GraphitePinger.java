package ru.kontur.vostok.hercules.graphite.sink;

import ru.kontur.vostok.hercules.sink.ProcessorStatus;

import java.io.IOException;
import java.net.Socket;

public class GraphitePinger {
    private final String server;
    private final int port;

    public GraphitePinger(String server, int port) {
        this.server = server;
        this.port = port;
    }

    public ProcessorStatus ping() {
        try {
            new Socket(server, port).close();
            return ProcessorStatus.AVAILABLE;
        } catch (IOException exception) {
            return ProcessorStatus.UNAVAILABLE;
        }
    }
}
