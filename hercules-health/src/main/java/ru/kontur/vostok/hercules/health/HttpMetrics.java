package ru.kontur.vostok.hercules.health;

import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * HttpMetrics - a set of metrics for http statuses reporting
 *
 * @author Kirill Sulim
 */
public class HttpMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpMetrics.class);

    private static final int[] ALWAYS_REPORTED_CODES = new int[] {
            200,
            400,
            401,
            403,
            500,
            503
    };

    private final String metricsName;
    private final MetricsCollector metricsCollector;

    /**
     * Store metric of all requests
     */
    private final Timer total;

    /**
     * store metric of request with status code between 200 and 400 (exclusive)
     */
    private final Timer r2XX;

    /**
     * store metric of request with status code between 400 and 500 (exclusive)
     */
    private final Timer r4XX;

    /**
     * store metric of request with status code between 500 and 600 (exclusive)
     */
    private final Timer r5XX;

    private final ConcurrentHashMap<Integer, Timer> statusMeters = new ConcurrentHashMap<>();

    /**
     * @param handlerName name of htt p handler as part of metric name
     * @param metricsCollector metrics collector instance
     */
    public HttpMetrics(String handlerName, MetricsCollector metricsCollector) {
        this.metricsName = "http.handlers." + handlerName + ".status.";
        this.metricsCollector = metricsCollector;

        this.total = metricsCollector.timer(metricsName + "total");
        this.r2XX = metricsCollector.timer(metricsName + "2XX");
        this.r4XX = metricsCollector.timer(metricsName + "4XX");
        this.r5XX = metricsCollector.timer(metricsName + "5XX");

        for (int code : ALWAYS_REPORTED_CODES) {
            statusMeters.computeIfAbsent(code, this::createTimerForCode);
        }
    }

    /**
     * Mark HTTP response status and time in set of metrics
     * @param statusCode HTTP status code
     * @param duration HTTP request duration
     */
    public void mark(int statusCode, long duration) {
        if (200 <= statusCode && statusCode < 400) {
            r2XX.update(duration, TimeUnit.MILLISECONDS);
        }
        else if (400 <= statusCode && statusCode < 500) {
            r4XX.update(duration, TimeUnit.MILLISECONDS);
        }
        else if (500 <= statusCode && statusCode < 600) {
            r5XX.update(duration, TimeUnit.MILLISECONDS);
        }
        else {
            LOGGER.error("Unsupported HTTP status code {}", statusCode);
            return;
        }

        Timer timer = statusMeters.computeIfAbsent(statusCode, this::createTimerForCode);
        timer.update(duration, TimeUnit.MILLISECONDS);

        total.update(duration, TimeUnit.MILLISECONDS);
    }

    private Timer createTimerForCode(int code) {
        return metricsCollector.timer(metricsName + String.valueOf(code));
    }
}
