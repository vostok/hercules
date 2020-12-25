package ru.kontur.vostok.hercules.stream.api;

import ru.kontur.vostok.hercules.health.Histogram;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;
import ru.kontur.vostok.hercules.health.Timer;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class StreamReadRequestMetrics {
    private static final String METRICS_SCOPE = StreamReadRequestMetrics.class.getSimpleName();

    private final boolean samplingEnabled;
    private final int samplingTimeoutMs;
    private final int samplingResponseDataSizeBytes;

    private final MetricsCollector metricsCollector;

    private final Histogram timeoutMsHistogram;

    private final Timer totalTimeMsTimer;
    private final Timer readingTimeMsTimer;
    private final Timer compressionTimeMsTimer;
    private final Timer sendingTimeMsTimer;

    private final Meter throughputUncompressedBytesMeter;
    private final Meter throughputCompressedBytesMeter;

    private final Histogram uncompressedSizeBytesHistogram;

    private Timer sampledTotalTimeMsTimer;
    private Timer sampledReadingTimeMsTimer;
    private Timer sampledCompressionTimeMsTimer;
    private Timer sampledSendingTimeMsTimer;

    public StreamReadRequestMetrics(Properties properties, MetricsCollector metricsCollector) {
        this.samplingEnabled = PropertiesUtil.get(Props.SAMPLING_ENABLE, properties).get();
        this.samplingTimeoutMs = PropertiesUtil.get(Props.SAMPLING_TIMEOUT_MS, properties).get();
        this.samplingResponseDataSizeBytes = PropertiesUtil.get(Props.SAMPLING_RESPONSE_DATA_SIZE_BYTES, properties).get();

        this.metricsCollector = metricsCollector;

        this.timeoutMsHistogram = metricsCollector.histogram(MetricsUtil.toMetricPath(METRICS_SCOPE, "timeoutMs"));

        this.totalTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(METRICS_SCOPE, "totalTimeMs"));
        this.readingTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(METRICS_SCOPE, "readingTimeMs"));
        this.compressionTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(METRICS_SCOPE, "compressionTimeMs"));
        this.sendingTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(METRICS_SCOPE, "sendingTimeMs"));

        this.throughputUncompressedBytesMeter = metricsCollector.meter(MetricsUtil.toMetricPath(METRICS_SCOPE, "throughput", "uncompressedBytes"));
        this.throughputCompressedBytesMeter = metricsCollector.meter(MetricsUtil.toMetricPath(METRICS_SCOPE, "throughput", "compressedBytes"));

        this.uncompressedSizeBytesHistogram = metricsCollector.histogram(MetricsUtil.toMetricPath(METRICS_SCOPE, "uncompressedSizeBytes"));

        if (samplingEnabled) {
            this.sampledTotalTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(METRICS_SCOPE, "sampled", "totalTimeMs"));
            this.sampledReadingTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(METRICS_SCOPE, "sampled", "readingTimeMs"));
            this.sampledCompressionTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(METRICS_SCOPE, "sampled", "compressionTimeMs"));
            this.sampledSendingTimeMsTimer = metricsCollector.timer(MetricsUtil.toMetricPath(METRICS_SCOPE, "sampled", "sendingTimeMs"));
        }
    }

    public void update(StreamReadRequestProcessor.StreamReadRequest request) {
        timeoutMsHistogram.update(request.timeoutMs());

        totalTimeMsTimer.update(request.processingTimeMs());
        readingTimeMsTimer.update(request.readingTimeMs());
        compressionTimeMsTimer.update(request.compressionTimeMs());
        sendingTimeMsTimer.update(request.sendingTimeMs());

        throughputCompressedBytesMeter.mark(request.compressedSizeBytes());
        throughputUncompressedBytesMeter.mark(request.uncompressedSizeBytes());

        uncompressedSizeBytesHistogram.update(request.uncompressedSizeBytes());

        if (samplingEnabled && request.timeoutMs() <= samplingTimeoutMs && request.uncompressedSizeBytes() <= samplingResponseDataSizeBytes) {
            sampledTotalTimeMsTimer.update(request.processingTimeMs());
            sampledReadingTimeMsTimer.update(request.readingTimeMs());
            sampledCompressionTimeMsTimer.update(request.compressionTimeMs());
            sampledSendingTimeMsTimer.update(request.sendingTimeMs());
        }
    }

    private static class Props {
        static Parameter<Boolean> SAMPLING_ENABLE =
                Parameter.booleanParameter("sampling.enable").
                        withDefault(false).
                        build();

        static Parameter<Integer> SAMPLING_TIMEOUT_MS =
                Parameter.integerParameter("sampling.timeout.ms").
                        withDefault(1_000).
                        withValidator(IntegerValidators.positive()).
                        build();

        static Parameter<Integer> SAMPLING_RESPONSE_DATA_SIZE_BYTES =
                Parameter.integerParameter("sampling.response.data.size.bytes").
                        withDefault(10_485_760).// 10 * 1024 * 1024 (10 MB)
                        withValidator(IntegerValidators.positive()).
                        build();
    }
}
