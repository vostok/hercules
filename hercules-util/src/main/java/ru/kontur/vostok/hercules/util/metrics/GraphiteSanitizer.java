package ru.kontur.vostok.hercules.util.metrics;

/**
 * Replaces forbidden character with the allowed one.
 * <p>
 * A metric may contain
 * <ul>
 * <li>upper and lower latin characters {@code A-Z a-z}</li>
 * <li>decimal digits {@code 0-9}</li>
 * <li>the minus sign {@code -}</li>
 * <li>the underscore {@code _}</li>
 * <li>the colon {@code :}</li>
 * <li>the dot {@code .} (in the metric prefix)</li>
 * </ul>
 *
 * @author Gregory Koshelev
 */
public interface GraphiteSanitizer {
    /**
     * Replaces forbidden characters with underscore.
     * <p>
     * Valid characters satisfy {@code [-a-zA-Z0-9_:]} regexp.
     */
    GraphiteSanitizer METRIC_NAME_SANITIZER = new GraphiteMetricNameSanitizer();
    /**
     * Replaces forbidden characters with underscore.
     * Allow dot {@code .} since the metric path contains them.
     * <p>
     * Valid characters satisfy {@code [-a-zA-Z0-9_:.]} regexp.
     */
    GraphiteSanitizer METRIC_PATH_SANITIZER = new GraphiteMetricPathSanitizer();

    /**
     * Sanitize string to satisfy Graphite's restriction on metric names.
     *
     * @param source the source string
     * @return the sanitized string
     */
    String sanitize(String source);
}
