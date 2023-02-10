package ru.kontur.vostok.hercules.elastic.sink.error;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.health.IMetricsCollector;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Elastic-errors metrics store.
 *
 * @author Anton Akkuzin
 */
public class ElasticErrorMetrics {

    private static final String RETRYABLE_ERRORS = "retryableErrors";
    private static final String NON_RETRYABLE_ERRORS = "nonRetryableErrors";
    private static final String UNKNOWN_ERRORS = "unknownErrors";

    private final String metricsPrefix;
    private final IMetricsCollector metricsCollector;
    private final Map<String, IndexErrors> indexErrorsMap = new ConcurrentHashMap<>();

    // fixme: HERCULES-1042 task
    private final Meter retryableErrorsMeter;
    private final Meter nonRetryableErrorsMeter;
    private final Meter unknownErrorsMeter;
    private final ConcurrentHashMap<String, Meter> errorTypesMeter = new ConcurrentHashMap<>();


    public ElasticErrorMetrics(String metricsPrefix, @NotNull IMetricsCollector metricsCollector) {
        this.metricsPrefix = metricsPrefix;
        this.metricsCollector = metricsCollector;
        this.retryableErrorsMeter = metricsCollector.meter(MetricsUtil
                .toMetricPathWithPrefix(metricsPrefix, RETRYABLE_ERRORS));
        this.nonRetryableErrorsMeter = metricsCollector.meter(MetricsUtil
                .toMetricPathWithPrefix(metricsPrefix, NON_RETRYABLE_ERRORS));
        this.unknownErrorsMeter = metricsCollector.meter(MetricsUtil
                .toMetricPathWithPrefix(metricsPrefix, UNKNOWN_ERRORS));
    }

    /**
     * Mark Elastic error.
     *
     * @param index      index-name
     * @param errorGroup error group
     * @param errorType  type of the error
     */
    public void markError(String index, ErrorGroup errorGroup, String errorType) {
        String errorGroupName = errorGroupName(errorGroup);
        indexErrorsMap.computeIfAbsent(index, IndexErrors::new).markError(errorGroupName, errorType);
        errorTypesMeter.computeIfAbsent(errorType, this::createMeter).mark();
    }

    private String errorGroupName(@NotNull ErrorGroup errorGroup) {
        switch (errorGroup) {
            case RETRYABLE:
                retryableErrorsMeter.mark();
                return RETRYABLE_ERRORS;
            case NON_RETRYABLE:
                nonRetryableErrorsMeter.mark();
                return NON_RETRYABLE_ERRORS;
            case UNKNOWN:
                unknownErrorsMeter.mark();
                return UNKNOWN_ERRORS;
            default:
                throw new RuntimeException(String.format("Unsupported error group '%s'", errorGroup));
        }
    }

    private Meter createMeter(String errorType) {
        return metricsCollector.meter(MetricsUtil
                .toMetricPathWithPrefix(metricsPrefix, "errorTypes", MetricsUtil.sanitizeMetricName(errorType)));
    }

    private class IndexErrors {

        private final Map<String, Meter> errorTypesMeters = new ConcurrentHashMap<>();
        private final String index;

        private IndexErrors(String index) {
            this.index = index;
        }

        private void markError(String errorGroupName, String errorType) {
            errorTypesMeters.computeIfAbsent(errorType, et -> createMeter(errorGroupName, et)).mark();
        }

        private Meter createMeter(String errorGroupName, String errorType) {
            return metricsCollector.meter(MetricsUtil
                    .toMetricPathWithPrefix(metricsPrefix,
                            "byIndex", index, errorGroupName, MetricsUtil.sanitizeMetricName(errorType)));
        }
    }
}
