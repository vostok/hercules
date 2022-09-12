package ru.kontur.vostok.hercules.util.metrics;

import ru.kontur.vostok.hercules.util.text.StringUtil;

/**
 * Replaces forbidden characters with underscore.
 * Allow dot {@code .} since the metric path contains them.
 * <p>
 * Valid characters satisfy {@code [-a-zA-Z0-9_:.]} regexp.
 *
 * @author Gregory Koshelev
 */
public class GraphiteMetricPathSanitizer implements GraphiteSanitizer {
    @Override
    public String sanitize(String source) {
        return StringUtil.sanitize(source, GraphiteMetricPathSanitizer::isCorrectSymbol);
    }

    private static boolean isCorrectSymbol(int ch) {
        return ch == '-' || ch == ':' || ch == '_' || ch == '.'
                || '0' <= ch && ch <= '9'
                || 'A' <= ch && ch <= 'Z'
                || 'a' <= ch && ch <= 'z';
    }
}
