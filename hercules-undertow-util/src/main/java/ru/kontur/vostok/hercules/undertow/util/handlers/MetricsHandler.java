package ru.kontur.vostok.hercules.undertow.util.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.health.HttpMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;

/**
 * MetricsHandler - Decorator handler with {@link HttpMetrics reporter}
 *
 * @author Kirill Sulim
 */
public class MetricsHandler implements HttpHandler {

    private final HttpHandler next;
    private final HttpMetrics httpMetrics;

    public MetricsHandler(HttpHandler next, String handlerName, MetricsCollector metricsCollector) {
        this.next = next;
        this.httpMetrics = metricsCollector.httpMetrics(handlerName);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (!exchange.isComplete()) {
            final long start = System.currentTimeMillis();
            exchange.addExchangeCompleteListener((exch, nextListener) -> {
                int statusCode = exch.getStatusCode();
                long duration = System.currentTimeMillis() - start;

                httpMetrics.mark(statusCode, duration);

                nextListener.proceed();
            });
        }
        next.handleRequest(exchange);
    }
}
