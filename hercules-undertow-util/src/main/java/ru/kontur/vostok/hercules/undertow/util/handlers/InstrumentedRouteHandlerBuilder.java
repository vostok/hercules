package ru.kontur.vostok.hercules.undertow.util.handlers;

import ru.kontur.vostok.hercules.health.HttpMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.http.HttpMethod;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.handler.AboutHandler;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.handler.PingHandler;
import ru.kontur.vostok.hercules.http.handler.RouteHandlerBuilder;
import ru.kontur.vostok.hercules.util.metrics.GraphiteMetricsUtil;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class InstrumentedRouteHandlerBuilder extends RouteHandlerBuilder {
    private final MetricsCollector metricsCollector;

    public InstrumentedRouteHandlerBuilder(Properties properties, MetricsCollector metricsCollector) {
        super(properties);
        this.metricsCollector = metricsCollector;

        get("/ping", new PingHandler());
        get("/about", new AboutHandler());
    }

    @Override
    public void addHandler(String path, HttpMethod method, HttpHandler handler) {
        super.addHandler(
                path,
                method,
                new MetricsHandler(
                        handler,
                        metricsCollector.httpMetrics(method + GraphiteMetricsUtil.sanitizeMetricName(path))));
    }

    private static class MetricsHandler implements HttpHandler {
        private final HttpHandler handler;
        private final HttpMetrics httpMetrics;

        private MetricsHandler(HttpHandler handler, HttpMetrics httpMetrics) {
            this.handler = handler;
            this.httpMetrics = httpMetrics;
        }

        @Override
        public void handle(HttpServerRequest request) {
            final long start = System.currentTimeMillis();
            request.addRequestCompletionListener(r -> {
                int statusCode = r.getResponse().getStatusCode();
                httpMetrics.mark(statusCode, System.currentTimeMillis() - start);
            });
            handler.handle(request);
        }
    }
}
