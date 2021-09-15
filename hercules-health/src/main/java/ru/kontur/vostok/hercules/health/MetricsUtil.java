package ru.kontur.vostok.hercules.health;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.util.metrics.GraphiteSanitizer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for metrics.
 *
 * @author Gregory Koshelev
 */
public final class MetricsUtil {
    /**
     * Sanitize metric name to replace forbidden characters with underscore.
     * <p>
     * Valid characters satisfy {@code [-a-zA-Z0-9_]} regexp.
     *
     * @param metricName the metric name
     * @return sanitized metric name
     */
    public static String sanitizeMetricName(@NotNull String metricName) {
        return GraphiteSanitizer.METRIC_NAME_SANITIZER.sanitize(metricName);
    }

    /**
     * Sanitize metric path to replace forbidden characters with underscore.
     * <p>
     * Valid characters satisfy {@code [-a-zA-Z0-9_.]} regexp.
     *
     * @param metricPath the metric path
     * @return sanitized metric path
     */
    public static String sanitizeMetricPath(String metricPath) {
        return GraphiteSanitizer.METRIC_PATH_SANITIZER.sanitize(metricPath);
    }

    /**
     * Build sanitized metric name from string elements.
     *
     * @param elements elements are used to build metric name
     * @return sanitized metric name
     */
    public static String toMetricName(String... elements) {
        return sanitizeMetricName(String.join("_", elements));
    }

    /**
     * Build sanitized metric path from string elements.
     * Metric path consists of elements joined by dot.
     *
     * @param elements elements are used to build metric path
     * @return sanitized metric path
     */
    public static String toMetricPath(final List<String> elements) {
        return elements.stream()
                .map(MetricsUtil::sanitizeMetricName)
                .collect(Collectors.joining("."));
    }

    /**
     * Build sanitized metric path from string elements.
     * Metric path consists of elements joined by dot.
     *
     * @param elements elements are used to build metric path
     * @return sanitized metric path
     */
    public static String toMetricPath(String... elements) {
        return Arrays.stream(elements).
                map(MetricsUtil::sanitizeMetricName).
                collect(Collectors.joining("."));
    }

    /**
     * Build metric path by joining metric prefix and metric name with a dot.
     *
     * @param prefix   the metric prefix is a metric path itself
     * @param elements the metric name
     * @return sanitized metric path
     */
    public static String toMetricPathWithPrefix(String prefix, String... elements) {
        return sanitizeMetricPath(prefix) + '.' + toMetricPath(elements);
    }

    private MetricsUtil() {
        /* static class */
    }
}
