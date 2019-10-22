package ru.kontur.vostok.hercules.undertow.util.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.health.HttpMetric;
import ru.kontur.vostok.hercules.health.MetricsCollector;

/**
 * MetricsHandler - Decorator handler with {@link HttpMetric reporter}
 *
 * @author Kirill Sulim
 */
@Deprecated
public class MetricsHandler implements HttpHandler {

    private final HttpHandler next;
    private final HttpMetric httpMetric;

    public MetricsHandler(HttpHandler next, String handlerName, MetricsCollector metricsCollector) {
        this.next = next;
        this.httpMetric = metricsCollector.http(handlerName);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (!exchange.isComplete()) {
            final long start = System.currentTimeMillis();
            exchange.addExchangeCompleteListener((exch, nextListener) -> {
                int statusCode = exch.getStatusCode();
                long duration = System.currentTimeMillis() - start;

                httpMetric.update(statusCode, duration);

                nextListener.proceed();
            });
        }
        next.handleRequest(exchange);
    }
}
