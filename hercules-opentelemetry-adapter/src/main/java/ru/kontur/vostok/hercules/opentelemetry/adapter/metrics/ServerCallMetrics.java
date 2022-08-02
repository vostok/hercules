package ru.kontur.vostok.hercules.opentelemetry.adapter.metrics;

import io.grpc.Status;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;
import ru.kontur.vostok.hercules.health.Timer;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * gRPC server call metrics
 *
 * @author Innokentiy Krivonosov
 */
public class ServerCallMetrics {
    private static final String METRICS_SCOPE = ServerCallMetrics.class.getSimpleName();

    private final String statusMetricGroup;
    private final MetricsCollector metricsCollector;
    private final TimeSource time;

    private final Timer processingTimeMsTimer;

    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    private final Timer receivingTimeMsTimer;

    public ServerCallMetrics(String serviceName, TimeSource time, MetricsCollector metricsCollector) {
        this.time = time;
        this.metricsCollector = metricsCollector;
        String metricGroup = MetricsUtil.toMetricPath(serviceName, METRICS_SCOPE);

        this.statusMetricGroup = MetricsUtil.toMetricPathWithPrefix(metricGroup, "status");
        this.processingTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPathWithPrefix(metricGroup, "processingTimeMs"));
        this.receivingTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPathWithPrefix(metricGroup, "receivingTimeMs"));
    }

    public long startMilliseconds() {
        return time.milliseconds();
    }

    public void receivingEnded(long serverCallStartedAtMs) {
        long durationMs = time.milliseconds() - serverCallStartedAtMs;
        receivingTimeMsTimer.update(durationMs);
    }

    public void serverCallEnded(Status.Code code, long serverCallStartedAtMs) {
        long serverCallDurationMs = time.milliseconds() - serverCallStartedAtMs;
        processingTimeMsTimer.update(serverCallDurationMs);
        timer(code.name()).update(serverCallDurationMs);
    }

    private Timer timer(String codeName) {
        return timers.computeIfAbsent(codeName, x ->
                metricsCollector.timer(MetricsUtil.toMetricPathWithPrefix(statusMetricGroup, x, "processingTimeMs"))
        );
    }
}
