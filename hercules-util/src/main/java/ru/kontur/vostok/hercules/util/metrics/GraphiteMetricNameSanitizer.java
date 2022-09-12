package ru.kontur.vostok.hercules.util.metrics;

import ru.kontur.vostok.hercules.util.text.StringUtil;

/**
 * Replaces forbidden characters with underscore.
 * <p>
 * Valid characters satisfy {@code [-a-zA-Z0-9_:]} regexp.
 *
 * @author Gregory Koshelev
 */
public class GraphiteMetricNameSanitizer implements GraphiteSanitizer {

    @Override
    public String sanitize(String source) {
        return StringUtil.sanitize(source, GraphiteMetricNameSanitizer::isCorrectSymbol);
    }

    private static boolean isCorrectSymbol(int ch) {
        return ch == '-' || ch == ':' || ch == '_'
                || '0' <= ch && ch <= '9'
                || 'A' <= ch && ch <= 'Z'
                || 'a' <= ch && ch <= 'z';
    }
}
