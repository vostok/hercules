package ru.kontur.vostok.hercules.timeline.api;

import ru.kontur.vostok.hercules.health.MetricsCollector;

import java.util.concurrent.TimeUnit;

/**
 * Class for creation metrics by timeline name
 *
 * @author Petr Demenev
 */
public class TimelineMetricsCollector {

    private static final String PREFIX = "byTimeline.";

    private final MetricsCollector metricsCollector;
    private final String receivedEventsCountMetricName;
    private final String receivedBytesCountMetricName;
    private final String readingTimerName;

    public TimelineMetricsCollector(MetricsCollector metricsCollector, String timelineName) {
        String metricBaseName = addPrefix(timelineName);
        this.metricsCollector = metricsCollector;
        this.receivedEventsCountMetricName = metricBaseName + "receivedEventsCount";
        this.receivedBytesCountMetricName = metricBaseName + "receivedBytesCount";
        this.readingTimerName = metricBaseName + "readingTimeMs";
    }

    public void markReceivedEventsCount(int count) {
        metricsCollector.meter(receivedEventsCountMetricName).mark(count);
    }

    public void markReceivedBytesCount(int count) {
        metricsCollector.meter(receivedBytesCountMetricName).mark(count);
    }

    public void updateReadingTime(long durationMs) {
        metricsCollector.timer(readingTimerName).update(durationMs, TimeUnit.MILLISECONDS);
    }

    private static String addPrefix(final String timelineName) {
        return PREFIX + timelineName + ".";
    }
}
