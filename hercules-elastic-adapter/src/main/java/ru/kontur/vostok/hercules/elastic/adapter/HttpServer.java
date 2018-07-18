package ru.kontur.vostok.hercules.elastic.adapter;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.BlockingHandler;
import ru.kontur.vostok.hercules.gateway.client.GatewayClient;

/**
 * @author Daniil Zhenikhov
 */
public class HttpServer {
    private final Undertow undertow;
    private final GatewayClient client;

    public HttpServer(String host, int port, String stream, String url) {
        client = new GatewayClient();
        HttpHandler logHandler = new ElasticAdapterHandler((apiKey, content) -> client.send(url, apiKey, stream, content));
        HttpHandler asyncLogHandler = new ElasticAdapterHandler((apiKey, content) -> client.sendAsync(url, apiKey, stream, content));

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
        client.close();
    }
}
