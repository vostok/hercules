package ru.kontur.vostok.hercules.elastic.adapter;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.BlockingHandler;
import ru.kontur.vostok.hercules.gateway.client.GatewayClient;

public class HttpServer {
    private final Undertow undertow;

    public HttpServer(String host, int port, String stream, GatewayClient gatewayClient) {
        HttpHandler logHandler = new ElasticAdapterHandler(bytes -> gatewayClient.send(stream, bytes));
        HttpHandler asyncLogHandler = new ElasticAdapterHandler(bytes -> gatewayClient.sendAsync(stream, bytes));

        HttpHandler handler = Handlers.routing()
                .post("/logs/{index}", logHandler)
                .post("/async-logs/{index}", asyncLogHandler);

        undertow = Undertow
                .builder()
                .addHttpListener(port, host)
                .setHandler(new BlockingHandler(handler))
                .build();
    }

    public void start() {
        undertow.start();
    }

    public void stop() {
        undertow.stop();
    }
}
