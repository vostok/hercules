package ru.kontur.vostok.hercules.util.metrics;

/**
 * GraphiteMetricsUtil collection of utility functions for graphite metrics
 *
 * @author Kirill Sulim
 */
public final class GraphiteMetricsUtil {

    public static String sanitizeMetricName(final String metricName) {
        return metricName.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private GraphiteMetricsUtil() {
        /* static class */
    }
}
