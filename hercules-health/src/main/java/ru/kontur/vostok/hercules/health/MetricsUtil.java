package ru.kontur.vostok.hercules.health;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for metrics.
 *
 * @author Gregory Koshelev
 */
public final class MetricsUtil {
    private static final Pattern METRIC_NAME_FORBIDDEN_CHARACTERS = Pattern.compile("[^a-zA-Z0-9_-]");

    /**
     * Sanitize metric name to replace forbidden characters with underscore.
     * <p>
     * Valid characters are satisfy {@code [a-zA-Z0-9_-]} regexp.
     *
     * @param metricName the metric name
     * @return sanitized metric name
     */
    public static String sanitizeMetricName(@NotNull String metricName) {
        return METRIC_NAME_FORBIDDEN_CHARACTERS.matcher(metricName).replaceAll("_");
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
     * Metric path consists of elements joined by `_`.
     *
     * @param elements elements are used to build metric path
     * @return sanitized metric path
     */
    public static String toMetricPath(final List<String> elements) {
        return elements.stream()
                .map(MetricsUtil::sanitizeMetricName)
                .collect(Collectors.joining("."));
    }

    private MetricsUtil() {
        /* static class */
    }
}
