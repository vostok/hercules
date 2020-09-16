package ru.kontur.vostok.hercules.undertow.util.handlers;

import ru.kontur.vostok.hercules.health.HttpMetric;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;
import ru.kontur.vostok.hercules.http.HttpMethod;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.handler.AboutHandler;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.handler.PingHandler;
import ru.kontur.vostok.hercules.http.handler.RouteHandlerBuilder;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class InstrumentedRouteHandlerBuilder extends RouteHandlerBuilder {
    private final MetricsCollector metricsCollector;
    private final TimeSource time;

    public InstrumentedRouteHandlerBuilder(Properties properties, MetricsCollector metricsCollector) {
        this(properties, metricsCollector, TimeSource.SYSTEM);
    }

    InstrumentedRouteHandlerBuilder(Properties properties, MetricsCollector metricsCollector, TimeSource time) {
        super(properties);
        this.metricsCollector = metricsCollector;
        this.time = time;

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
                        metricsCollector.http(
                                MetricsUtil.toMetricName(method.toString(), path))));
    }

    private static class MetricsHandler implements HttpHandler {
        private final HttpHandler handler;
        private final HttpMetric httpMetric;

        private MetricsHandler(HttpHandler handler, HttpMetric httpMetric) {
            this.handler = handler;
            this.httpMetric = httpMetric;
        }

        @Override
        public void handle(HttpServerRequest request) {
            final long start = System.currentTimeMillis();
            request.addRequestCompletionListener(r -> {
                int statusCode = r.getResponse().getStatusCode();
                httpMetric.update(statusCode, System.currentTimeMillis() - start);
            });
            handler.handle(request);
        }
    }
}
