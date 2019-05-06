package ru.kontur.vostok.hercules.util.metrics;

import java.util.regex.Pattern;

/**
 * GraphiteMetricsUtil collection of utility functions for graphite metrics
 *
 * @author Kirill Sulim
 */
public final class GraphiteMetricsUtil {
    private static final Pattern FORBIDDEN_CHARS_PATTERN = Pattern.compile("[^A-Za-z0-9_]");

    private GraphiteMetricsUtil() {
        /* static class */
    }

    public static String sanitizeMetricName(final String metricName) {
        return FORBIDDEN_CHARS_PATTERN.matcher(metricName).replaceAll("_");
    }
}
