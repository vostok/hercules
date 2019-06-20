package ru.kontur.vostok.hercules.util;

import ru.kontur.vostok.hercules.util.collection.CollectionUtil;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Gregory Koshelev
 */
public class PatternMatcher {

    private final String pattern;
    private final Pattern regexp;

    public PatternMatcher(String pattern) {
        this.pattern = pattern;
        this.regexp = Pattern.compile(toRegexString(pattern));
    }

    public boolean matches(String value) {
        return regexp.matcher(value).matches();
    }

    public static boolean matchesAnyOf(String value, List<PatternMatcher> matchers) {
        if (value == null || matchers == null || matchers.isEmpty()) {
            return false;
        }

        for (PatternMatcher matcher : matchers) {
            if (matcher.matches(value)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return pattern;
    }

    public Pattern getRegexp() {
        return regexp;
    }

    private static String toRegexString(String pattern) {
        final int length = pattern.length();
        StringBuilder sb = new StringBuilder(length * 2);
        sb.append('^');
        for (int i = 0; i < length; i++) {
            char c = pattern.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || c == '_') {
                sb.append(c);
                continue;
            }
            if (c == '*') {
                sb.append(ALPHANUMERIC).append('*');
                continue;
            }
            if (c == '?') {
                sb.append(ALPHANUMERIC);
                continue;
            }
            // Otherwise pattern is invalid thus throw illegal argument exception
            throw new IllegalArgumentException("Pattern contains prohibited chars");
        }
        sb.append('$');
        return sb.toString();
    }

    private static final String ALPHANUMERIC = "[a-z0-9_]";

    public static String matchersListToRegexpString(final List<PatternMatcher> matchers) {
        return matchers.stream()
            .map(patternMatcher -> patternMatcher.pattern)
            .map(PatternMatcher::toRegexString)
            .collect(Collectors.joining("|"));
    }

    public static Pattern matcherListToRegexp(final List<PatternMatcher> matchers) {
        return Pattern.compile(matchersListToRegexpString(matchers));
    }

    public static String toString(List<PatternMatcher> patternMatchers) {
        return CollectionUtil.isNullOrEmpty(patternMatchers)
                ? "" : patternMatchers.stream().map(x -> x.pattern).collect(Collectors.joining(","));
    }
}
