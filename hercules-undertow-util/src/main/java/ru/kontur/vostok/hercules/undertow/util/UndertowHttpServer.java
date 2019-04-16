package ru.kontur.vostok.hercules.undertow.util;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.http.HttpServer;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class UndertowHttpServer extends HttpServer {
    private Undertow undertow;

    public UndertowHttpServer(Properties properties, HttpHandler handler) {
        super(properties, handler);
    }

    @Override
    protected void startInternal() {
        int port = Props.PORT.extract(properties);
        String host = Props.HOST.extract(properties);

        undertow = Undertow.builder().addHttpListener(port, host).setHandler(exchange -> {
            handler.handle(wrap(exchange));
        }).build();
    }

    @Override
    protected boolean stopInternal(long timeout, TimeUnit unit) {
        undertow.stop();//TODO: replace with custom stopper with time quota
        return true;
    }

    private static HttpServerRequest wrap(HttpServerExchange exchange) {
        return new UndertowHttpServerRequest(exchange);
    }
}
