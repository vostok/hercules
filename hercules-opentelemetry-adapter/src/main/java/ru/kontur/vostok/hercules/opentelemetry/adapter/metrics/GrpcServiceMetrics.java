package ru.kontur.vostok.hercules.opentelemetry.adapter.metrics;

import ru.kontur.vostok.hercules.health.Histogram;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;
import ru.kontur.vostok.hercules.health.Timer;
import ru.kontur.vostok.hercules.protocol.CommonConstants;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.util.List;

/**
 * gRPC service metrics
 *
 * @author Innokentiy Krivonosov
 */
public class GrpcServiceMetrics {
    private final TimeSource time;

    private final Timer sendingEventsTimeMsTimer;
    private final Timer convertingEventsTimeMsTimer;

    private final Meter sentEventsMeter;
    private final Meter deliveredEventsMeter;
    private final Meter failedEventsMeter;
    private final Meter throughputUncompressedBytesMeter;

    private final Histogram receivedEventsHistogram;
    private final Histogram requestUncompressedSizeBytesHistogram;

    public GrpcServiceMetrics(String serviceName, TimeSource time, MetricsCollector metricsCollector) {
        this.time = time;
        this.sendingEventsTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(serviceName, "sendingEventsTimeMs"));
        this.convertingEventsTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(serviceName, "convertingEventsTimeMs"));

        this.sentEventsMeter = metricsCollector.meter(MetricsUtil.toMetricPath(serviceName, "sentEvents"));
        this.deliveredEventsMeter = metricsCollector.meter(MetricsUtil.toMetricPath(serviceName, "deliveredEvents"));
        this.failedEventsMeter = metricsCollector.meter(MetricsUtil.toMetricPath(serviceName, "failedEvents"));

        this.throughputUncompressedBytesMeter = metricsCollector.meter(MetricsUtil.toMetricPath(serviceName, "throughput", "uncompressedBytes"));

        this.receivedEventsHistogram = metricsCollector.histogram(MetricsUtil.toMetricPath(serviceName, "receivedEvents"));
        this.requestUncompressedSizeBytesHistogram = metricsCollector.histogram(MetricsUtil.toMetricPath(serviceName, "requestUncompressedSizeBytes"));
    }

    public long startMilliseconds() {
        return time.milliseconds();
    }

    public long convertingEnded(long convertingEventsStartedAtMs) {
        long convertingEventsEndedAtMs = time.milliseconds();

        convertingEventsTimeMsTimer.update(convertingEventsEndedAtMs - convertingEventsStartedAtMs);

        return convertingEventsEndedAtMs;
    }

    public void markDelivered(List<Event> events, long sendingEventsStartedAtMs) {
        sendingEventsTimeMsTimer.update(time.milliseconds() - sendingEventsStartedAtMs);

        deliveredEventsMeter.mark(events.size());
        updateCommonEventsMetric(events);
    }

    public void markFailed(List<Event> events, long sendingEventsStartedAtMs) {
        sendingEventsTimeMsTimer.update(time.milliseconds() - sendingEventsStartedAtMs);

        failedEventsMeter.mark(events.size());
        updateCommonEventsMetric(events);
    }

    private void updateCommonEventsMetric(List<Event> events) {
        sentEventsMeter.mark(events.size());
        receivedEventsHistogram.update(events.size());

        int byteSize = getByteSize(events);
        throughputUncompressedBytesMeter.mark(byteSize);
        requestUncompressedSizeBytesHistogram.update(byteSize);
    }

    private int getByteSize(List<Event> events) {
        int size = 0;

        for (Event event : events) {
            size += event.sizeOf();
        }
        return size + CommonConstants.MIN_EVENT_BATCH_SIZE_IN_BYTES;
    }
}
