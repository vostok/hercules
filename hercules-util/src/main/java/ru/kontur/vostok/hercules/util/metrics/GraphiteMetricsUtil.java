package ru.kontur.vostok.hercules.util.metrics;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * GraphiteMetricsUtil collection of utility functions for graphite metrics
 *
 * @author Kirill Sulim
 */
public final class GraphiteMetricsUtil {
    private static final Pattern FORBIDDEN_CHARS_PATTERN = Pattern.compile("[^-A-Za-z0-9_]");

    private GraphiteMetricsUtil() {
        /* static class */
    }

    public static String sanitizeMetricName(final String metricName) {
        return FORBIDDEN_CHARS_PATTERN.matcher(metricName).replaceAll("_");
    }

    public static String buildMetricName(final List<String> tokens) {
        return buildMetricName(tokens.toArray(new String[0]));
    }

    public static String buildMetricName(final String... tokens) {
        return Arrays.stream(tokens)
                .map(GraphiteMetricsUtil::sanitizeMetricName)
                .collect(Collectors.joining("."));
    }
}
