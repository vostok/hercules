package ru.kontur.vostok.hercules.stream.api;

import ru.kontur.vostok.hercules.health.MetricsCollector;

/**
 * Class for creation metrics by stream name.
 *
 * @author Vladimir Tsypaev
 */
public class StreamMetricsCollector {

    private static final String PREFIX = "byStream.";

    private final MetricsCollector metricsCollector;
    private final String receivedEventsCountMetricName;
    private final String receivedBytesCountMetricName;

    public StreamMetricsCollector(MetricsCollector metricsCollector, String streamName) {
        String metricBaseName = addPrefix(streamName);
        this.metricsCollector = metricsCollector;
        this.receivedEventsCountMetricName = metricBaseName + "receivedEventsCount";
        this.receivedBytesCountMetricName = metricBaseName + "receivedBytesCount";
    }

    public void markReceivedEventsCount(int count) {
        metricsCollector.meter(receivedEventsCountMetricName).mark(count);
    }

    public void markReceivedBytesCount(int count) {
        metricsCollector.meter(receivedBytesCountMetricName).mark(count);
    }

    private static String addPrefix(final String streamName) {
        return PREFIX + streamName + ".";
    }
}
