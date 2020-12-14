package ru.kontur.vostok.hercules.stream.api;

import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;
import ru.kontur.vostok.hercules.health.Timer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * StreamReader metrics.
 *
 * @author Gregory Koshelev
 */
public class StreamReaderMetrics {
    private final String metricsScope = getClass().getSimpleName();

    private final ConcurrentMap<String, Meter> receivedEventsCountMeterByStream = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Meter> receivedBytesCountMeterByStream = new ConcurrentHashMap<>();

    private final MetricsCollector metricsCollector;

    private final Meter receivedEventsCountMeter;
    private final Meter receivedBytesCountMeter;

    private final Timer timeToWaitConsumerMsTimer;

    public StreamReaderMetrics(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;

        this.receivedEventsCountMeter = metricsCollector.meter(MetricsUtil.toMetricPath(metricsScope, "receivedEventsCount"));
        this.receivedBytesCountMeter = metricsCollector.meter(MetricsUtil.toMetricPath(metricsScope, "receivedBytesCount"));

        this.timeToWaitConsumerMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(metricsScope, "timeToWaitConsumerMs"));
    }

    /**
     * Update metrics for the stream with events were read.
     *
     * @param stream the stream
     * @param events events
     */
    public void update(String stream, List<byte[]> events) {
        receivedEventsCountMeter.mark(events.size());
        receivedEventsCountMeterByStream.computeIfAbsent(
                stream,
                (k) -> metricsCollector.meter(MetricsUtil.toMetricPath(metricsScope, "byStream", k, "receivedEventsCount"))
        ).mark(events.size());

        int sizeOfEvents = 0;
        for (byte[] event : events) {
            sizeOfEvents += event.length;
        }

        receivedBytesCountMeter.mark(sizeOfEvents);
        receivedBytesCountMeterByStream.computeIfAbsent(
                stream,
                (k) -> metricsCollector.meter(MetricsUtil.toMetricPath(metricsScope, "byStream", k, "receivedBytesCount"))
        ).mark(sizeOfEvents);
    }

    /**
     * Update metrics with consumer awaiting.
     *
     * @param waitTimeMs a time has been elapsed in millis
     */
    public void updateWaitConsumer(long waitTimeMs) {
        timeToWaitConsumerMsTimer.update(waitTimeMs);
    }
}