package ru.kontur.vostok.hercules.gate;

import ru.kontur.vostok.hercules.health.Histogram;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;
import ru.kontur.vostok.hercules.health.Timer;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Gregory Koshelev
 */
public class SendRequestMetrics {
    private static final String METRICS_SCOPE = SendRequestMetrics.class.getSimpleName();

    private final ConcurrentHashMap<String, StreamMetrics> streamMetrics = new ConcurrentHashMap<>();

    private final MetricsCollector metricsCollector;

    private final boolean samplingEnabled;
    private final int samplingRequestDataSizeBytes;

    private final Timer receivingTimeMsTimer;
    private final Timer decompressionTimeMsTimer;
    private final Timer sendingEventsTimeMsTimer;

    private final Timer asyncProcessingTimeMsTimer;
    private final Timer syncProcessingTimeMsTimer;

    private final Meter throughputCompressedBytesMeter;
    private final Meter throughputUncompressedBytesMeter;

    private final Histogram requestUncompressedSizeBytesHistogram;
    private final Histogram receivedEventsHistogram;

    private Timer sampledAsyncProcessingTimeMsTimer;
    private Timer sampledSyncProcessingTimeMsTimer;

    public SendRequestMetrics(Properties properties, MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;

        this.samplingEnabled = PropertiesUtil.get(Props.SAMPLING_ENABLE, properties).get();
        this.samplingRequestDataSizeBytes = PropertiesUtil.get(Props.SAMPLING_REQUEST_DATA_SIZE_BYTES, properties).get();

        this.receivingTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(METRICS_SCOPE, "receivingTimeMs"));
        this.decompressionTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(METRICS_SCOPE, "decompressionTimeMs"));
        this.sendingEventsTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(METRICS_SCOPE, "sendingEventsTimeMs"));

        this.asyncProcessingTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(METRICS_SCOPE, "asyncProcessingTimeMs"));
        this.syncProcessingTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(METRICS_SCOPE, "syncProcessingTimeMs"));

        this.throughputCompressedBytesMeter = metricsCollector.meter(MetricsUtil.toMetricPath(METRICS_SCOPE, "throughput", "compressedBytes"));
        this.throughputUncompressedBytesMeter = metricsCollector.meter(MetricsUtil.toMetricPath(METRICS_SCOPE, "throughput", "uncompressedBytes"));

        this.requestUncompressedSizeBytesHistogram = metricsCollector.histogram(MetricsUtil.toMetricPath(METRICS_SCOPE, "requestUncompressedSizeBytes"));
        this.receivedEventsHistogram = metricsCollector.histogram(MetricsUtil.toMetricPath(METRICS_SCOPE, "receivedEvents"));

        if (samplingEnabled) {
            this.sampledAsyncProcessingTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(METRICS_SCOPE, "sampled", "asyncProcessingTimeMs"));
            this.sampledSyncProcessingTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(METRICS_SCOPE, "sampled", "syncProcessingTimeMs"));
        }
    }

    public void update(SendRequestProcessor.SendRequest request, int code) {
        receivingTimeMsTimer.update(request.receivingTimeMs());
        decompressionTimeMsTimer.update(request.decompressionTimeMs());
        sendingEventsTimeMsTimer.update(request.sendingEventsTimeMs());

        if (request.isAsync()) {
            asyncProcessingTimeMsTimer.update(request.processingTimeMs());
        } else {
            syncProcessingTimeMsTimer.update(request.processingTimeMs());
        }

        throughputCompressedBytesMeter.mark(request.requestCompressedSizeBytes());
        throughputUncompressedBytesMeter.mark(request.requestUncompressedSizeBytes());

        requestUncompressedSizeBytesHistogram.update(request.requestUncompressedSizeBytes());
        receivedEventsHistogram.update(request.totalEvents());

        if (samplingEnabled && request.requestUncompressedSizeBytes() <= samplingRequestDataSizeBytes) {
            if (request.isAsync()) {
                sampledAsyncProcessingTimeMsTimer.update(request.processingTimeMs());
            } else {
                sampledSyncProcessingTimeMsTimer.update(request.processingTimeMs());
            }
        }

        //TODO: Should protect from overflow by removing "old" (in sense of LRU cache) streams
        streamMetrics.computeIfAbsent(request.stream(), stream -> new StreamMetrics(stream, metricsCollector)).update(request);
    }

    /**
     * Per stream metrics
     */
    private static class StreamMetrics {
        private static final String METRICS_SCOPE =
                MetricsUtil.toMetricPath(SendRequestMetrics.METRICS_SCOPE, StreamMetrics.class.getSimpleName());

        private final Timer asyncProcessingTimeMsTimer;
        private final Timer syncProcessingTimeMsTimer;

        private final Histogram requestUncompressedSizeBytesHistogram;

        StreamMetrics(String stream, MetricsCollector metricsCollector) {
            this.asyncProcessingTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(METRICS_SCOPE, stream, "asyncProcessingTimeMs"));
            this.syncProcessingTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(METRICS_SCOPE, stream, "syncProcessingTimeMs"));

            this.requestUncompressedSizeBytesHistogram = metricsCollector.histogram(MetricsUtil.toMetricPath(METRICS_SCOPE, stream, "requestUncompressedSizeBytes"));
        }

        void update(SendRequestProcessor.SendRequest request) {
            if (request.isAsync()) {
                asyncProcessingTimeMsTimer.update(request.processingTimeMs());
            } else {
                syncProcessingTimeMsTimer.update(request.processingTimeMs());
            }

            requestUncompressedSizeBytesHistogram.update(request.requestUncompressedSizeBytes());
        }
    }

    private static class Props {
        static Parameter<Boolean> SAMPLING_ENABLE =
                Parameter.booleanParameter("sampling.enable").
                        withDefault(false).
                        build();

        static Parameter<Integer> SAMPLING_REQUEST_DATA_SIZE_BYTES =
                Parameter.integerParameter("sampling.request.data.size.bytes").
                        withDefault(1_048_576).// 1024 * 1024 (1 MB)
                        withValidator(IntegerValidators.positive()).
                        build();
    }
}
