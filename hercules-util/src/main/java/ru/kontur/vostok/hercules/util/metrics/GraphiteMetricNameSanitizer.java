package ru.kontur.vostok.hercules.util.metrics;

import java.util.regex.Pattern;

/**
 * Replaces forbidden characters with underscore.
 * <p>
 * Valid characters satisfy {@code [-a-zA-Z0-9_:]} regexp.
 *
 * @author Gregory Koshelev
 */
public class GraphiteMetricNameSanitizer implements GraphiteSanitizer {
    private static final Pattern FORBIDDEN_CHARS_PATTERN = Pattern.compile("[^-A-Za-z0-9_:]");

    @Override
    public String sanitize(String source) {
        return FORBIDDEN_CHARS_PATTERN.matcher(source).replaceAll("_");
    }
}
