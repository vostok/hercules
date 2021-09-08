package ru.kontur.vostok.hercules.util.metrics;

import java.util.regex.Pattern;

/**
 * Replaces forbidden characters with underscore.
 * Allow dot {@code .} since the metric path contains them.
 * <p>
 * Valid characters satisfy {@code [-a-zA-Z0-9_:.]} regexp.
 *
 * @author Gregory Koshelev
 */
public class GraphiteMetricPathSanitizer implements GraphiteSanitizer {
    private static final Pattern FORBIDDEN_CHARS_PATTERN = Pattern.compile("[^-A-Za-z0-9_:.]");

    @Override
    public String sanitize(String source) {
        return FORBIDDEN_CHARS_PATTERN.matcher(source).replaceAll("_");
    }
}
