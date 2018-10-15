package ru.kontur.vostok.hercules.gate;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.meta.stream.StreamStorage;
import ru.kontur.vostok.hercules.throttling.CapacityThrottle;
import ru.kontur.vostok.hercules.throttling.Throttle;
import ru.kontur.vostok.hercules.undertow.util.DefaultUndertowRequestWeigher;
import ru.kontur.vostok.hercules.undertow.util.DefaultUndertowThrottledRequestProcessor;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class HttpServer {
    private final Undertow undertow;
    private final Throttle<HttpServerExchange, SendContext> throttle;

    public HttpServer(MetricsCollector metricsCollector, Properties properties, AuthManager authManager, AuthValidationManager authValidationManager, EventSender eventSender, StreamStorage streamStorage) {
        String host = properties.getProperty("host", "0.0.0.0");
        int port = PropertiesExtractor.get(properties, "port", 6306);

        Properties throttlingProperties = PropertiesUtil.ofScope(properties, Scopes.THROTTLING);

        SendRequestProcessor sendRequestProcessor = new SendRequestProcessor(metricsCollector, eventSender);
        this.throttle = new CapacityThrottle<>(
                throttlingProperties,
                new DefaultUndertowRequestWeigher(),
                sendRequestProcessor,
                new DefaultUndertowThrottledRequestProcessor()
        );

        HttpHandler sendAsyncHandler = new GateHandler(metricsCollector, authManager, throttle, authValidationManager, streamStorage, true);
        HttpHandler sendHandler = new GateHandler(metricsCollector, authManager, throttle, authValidationManager, streamStorage, false);

        HttpHandler handler = Handlers.routing()
                .get("/ping", exchange -> {
                    exchange.setStatusCode(200);
                    exchange.endExchange();
                })
                .post("/stream/sendAsync", sendAsyncHandler)
                .post("/stream/send", sendHandler);

        undertow = Undertow
                .builder()
                .addHttpListener(port, host)
                .setHandler(handler)
                .build();
    }

    public void start() {
        undertow.start();
    }

    public void stop() {
        undertow.stop();
    }
}
