package ru.kontur.vostok.hercules.gate;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.metrics.MetricsCollector;
import ru.kontur.vostok.hercules.throttling.CapacityThrottle;
import ru.kontur.vostok.hercules.throttling.Throttle;
import ru.kontur.vostok.hercules.undertow.util.DefaultUndertowRequestWeigher;
import ru.kontur.vostok.hercules.undertow.util.DefaultUndertowThrottledRequestProcessor;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class HttpServer {
    private final Undertow undertow;
    private final Throttle<HttpServerExchange, SendContext> throttle;

    public HttpServer(MetricsCollector metricsCollector, Properties properties, AuthManager authManager, AuthValidationManager authValidationManager, EventSender eventSender, StreamRepository streamRepository) {
        String host = properties.getProperty("host", "0.0.0.0");
        int port = PropertiesExtractor.get(properties, "port", 6306);

        Properties throttlingProperties = PropertiesUtil.ofScope(properties, "throttling");

        throttle = new CapacityThrottle<>(
                PropertiesExtractor.get(throttlingProperties, "capacity", 100_000_000),
                PropertiesExtractor.get(throttlingProperties, "threshold", 50),
                PropertiesExtractor.get(throttlingProperties, "poolSize", 4),
                PropertiesExtractor.get(throttlingProperties, "requestQueueSize", 1_000),
                PropertiesExtractor.get(throttlingProperties, "requestTimeout", 5_000L),
                new DefaultUndertowRequestWeigher(),
                new SendRequestProcessor(metricsCollector, eventSender),
                new DefaultUndertowThrottledRequestProcessor());

        HttpHandler sendAsyncHandler = new GateHandler(metricsCollector, authManager, throttle, authValidationManager, streamRepository, true);
        HttpHandler sendHandler = new GateHandler(metricsCollector, authManager, throttle, authValidationManager, streamRepository, false);

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
        throttle.shutdown(5_000, TimeUnit.MILLISECONDS);
        undertow.stop();
    }
}
