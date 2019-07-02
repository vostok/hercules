package ru.kontur.vostok.hercules.stream.api;

import com.codahale.metrics.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;

/**
 * @author Vladimir Tsypaev
 */
public class StreamMetricsCollector {

    private static final String PREFIX = "byStream.";

    private MetricsCollector metricsCollector;
    private String streamName;

    public StreamMetricsCollector(MetricsCollector metricsCollector, String streamName) {
        this.metricsCollector = metricsCollector;
        this.streamName = addPrefix(streamName);
    }

    public void markReceivedEventsCount(int count) {
        meter("receivedEventsCount").mark(count);
    }

    public void markReceivedBytesCount(int count) {
        meter("receivedBytesCount").mark(count);
    }

    private Meter meter(String name) {
        return metricsCollector.meter(streamName + "." + name);
    }

    private static String addPrefix(final String streamName) {
        return PREFIX + streamName;
    }
}
