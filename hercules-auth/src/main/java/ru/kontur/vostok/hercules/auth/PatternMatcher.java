package ru.kontur.vostok.hercules.auth;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Gregory Koshelev
 */
public class PatternMatcher {
    private final Pattern regexp;

    public PatternMatcher(String pattern) {
        regexp = Pattern.compile(toRegexString(pattern));
    }

    public boolean matches(String value) {
        return regexp.matcher(value).matches();
    }

    public static boolean matches(String value, List<PatternMatcher> matchers) {
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

    private static String toRegexString(String pattern) {
        final int length = pattern.length();
        StringBuilder sb = new StringBuilder(length * 2);
        for (int i = 0; i < length; i++) {
            char c = pattern.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || c == '_') {
                sb.append(c);
                continue;
            }
            if (c == '*') {
                sb.append(alphanumeric).append('*');
                continue;
            }
            if (c == '?') {
                sb.append(alphanumeric);
            }
        }
        return sb.toString();
    }

    private static final String alphanumeric = "[a-z0-9_]";
}
