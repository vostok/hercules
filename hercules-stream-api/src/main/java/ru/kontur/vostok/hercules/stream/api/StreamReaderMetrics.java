package ru.kontur.vostok.hercules.stream.api;

import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Gregory Koshelev
 */
public class StreamReaderMetrics {
    private final ConcurrentMap<String, Meter> receivedEventsCountMeterByStream = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Meter> receivedBytesCountMeterByStream = new ConcurrentHashMap<>();

    private final MetricsCollector metricsCollector;

    private final Meter receivedEventsCountMeter;
    private final Meter receivedBytesCountMeter;

    public StreamReaderMetrics(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;

        this.receivedEventsCountMeter = metricsCollector.meter("receivedEventsCount");
        this.receivedBytesCountMeter = metricsCollector.meter("receivedBytesCount");
    }

    public void update(String stream, List<byte[]> events) {
        receivedEventsCountMeter.mark(events.size());
        receivedEventsCountMeterByStream.computeIfAbsent(
                stream,
                (k) -> metricsCollector.meter(MetricsUtil.toMetricPath("byStream", k, "receivedEventsCount"))
        ).mark(events.size());

        int sizeOfEvents = 0;
        for (byte[] event : events) {
            sizeOfEvents += event.length;
        }

        receivedBytesCountMeter.mark(sizeOfEvents);
        receivedBytesCountMeterByStream.computeIfAbsent(
                stream,
                (k) -> metricsCollector.meter(MetricsUtil.toMetricPath("byStream", k, "receivedBytesCount"))
        ).mark(sizeOfEvents);
    }
}