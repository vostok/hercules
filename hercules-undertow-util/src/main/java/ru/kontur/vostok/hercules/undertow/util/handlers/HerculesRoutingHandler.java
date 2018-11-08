package ru.kontur.vostok.hercules.undertow.util.handlers;

import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.util.metrics.GraphiteMetricsUtil;

/**
 * HerculesRoutingHandler
 *
 * @author Kirill Sulim
 */
public class HerculesRoutingHandler implements HttpHandler {

    private final RoutingHandler routingHandler;
    private final MetricsCollector metricsCollector;

    public HerculesRoutingHandler(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
        this.routingHandler = Handlers.routing()
                .get("/ping", PingHandler.INSTANCE)
                .get("/about", AboutHandler.INSTANCE);
    }

    public HerculesRoutingHandler get(final String template, final HttpHandler handler) {
        routingHandler.get(
                template,
                new MetricsHandler(handler, createMetricName("GET", template), metricsCollector)
        );
        return this;
    }

    public HerculesRoutingHandler post(final String template, final HttpHandler handler) {
        routingHandler.post(
                template, new MetricsHandler(handler, createMetricName("POST", template), metricsCollector)
        );
        return this;
    }

    public HerculesRoutingHandler delete(final String template, final HttpHandler handler) {
        routingHandler.delete(
                template, new MetricsHandler(handler, createMetricName("DELETE", template), metricsCollector)
        );
        return this;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        routingHandler.handleRequest(exchange);
    }

    private static String createMetricName(final String httpMethodName, String template) {
        if (template.startsWith("/")) {
            template = template.substring(1);
        }
        return httpMethodName + "_" + GraphiteMetricsUtil.sanitizeMetricName(template);
    }
}
