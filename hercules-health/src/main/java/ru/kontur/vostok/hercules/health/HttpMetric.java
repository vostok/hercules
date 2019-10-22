package ru.kontur.vostok.hercules.health;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * HTTP Metric reports request time duration grouped by status codes.
 *
 * @author Gregory Koshelev
 */
public class HttpMetric {
    private final String metricGroup;
    private final MetricsCollector metricsCollector;

    private final Timer total;
    private final Timer _2xx;
    private final Timer _3xx;
    private final Timer _4xx;
    private final Timer _5xx;
    private final ConcurrentMap<Integer, Timer> timers = new ConcurrentHashMap<>();

    HttpMetric(String handlerName, MetricsCollector metricsCollector) {
        this.metricGroup = "http.handlers." + handlerName + ".status.";
        this.metricsCollector = metricsCollector;

        this.total = metricsCollector.timer(metricGroup + "total");
        this._2xx = metricsCollector.timer(metricGroup + "2XX");
        this._3xx = metricsCollector.timer(metricGroup + "3XX");
        this._4xx = metricsCollector.timer(metricGroup + "4XX");
        this._5xx = metricsCollector.timer(metricGroup + "5XX");
    }

    /**
     * Updates request metrics with specified status code and duration in milliseconds.
     *
     * @param statusCode the status code of the request
     * @param durationMs the duration of the request in milliseconds
     */
    public void update(int statusCode, long durationMs) {
        total.update(durationMs);

        if (200 <= statusCode && statusCode <= 299) {
            _2xx.update(durationMs);
        } else if (300 <= statusCode && statusCode <= 399) {
            _3xx.update(durationMs);
        } else if (400 <= statusCode && statusCode <= 499) {
            _4xx.update(durationMs);
        } else if (500 <= statusCode && statusCode <= 599) {
            _5xx.update(durationMs);
        }
        timer(statusCode).update(durationMs);
    }

    private Timer timer(int statusCode) {
        return timers.computeIfAbsent(statusCode, x -> metricsCollector.timer(metricGroup + x));
    }
}
