package ru.kontur.vostok.hercules.gate;

import ru.kontur.vostok.hercules.health.Histogram;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;
import ru.kontur.vostok.hercules.protocol.Event;

/**
 * @author Gregory Koshelev
 */
public class EventSenderMetrics {
    private static final String METRICS_SCOPE = EventSenderMetrics.class.getSimpleName();

    private final MetricsCollector metricsCollector;

    private final Meter sentEventsMeter;
    private final Meter deliveredEventsMeter;
    private final Meter failedEventsMeter;

    private final Histogram eventSizeHistogram;

    public EventSenderMetrics(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;

        this.sentEventsMeter = metricsCollector.meter(MetricsUtil.toMetricPath(METRICS_SCOPE, "sentEvents"));
        this.deliveredEventsMeter = metricsCollector.meter(MetricsUtil.toMetricPath(METRICS_SCOPE, "deliveredEvents"));
        this.failedEventsMeter = metricsCollector.meter(MetricsUtil.toMetricPath(METRICS_SCOPE, "failedEvents"));

        this.eventSizeHistogram = metricsCollector.histogram(MetricsUtil.toMetricPath(METRICS_SCOPE, "eventSize"));
    }

    public void updateSent(Event event) {
        sentEventsMeter.mark();
        eventSizeHistogram.update(event.sizeOf());
    }

    public void markDelivered() {
        deliveredEventsMeter.mark();
    }

    public void markFailed() {
        failedEventsMeter.mark();
    }
}
