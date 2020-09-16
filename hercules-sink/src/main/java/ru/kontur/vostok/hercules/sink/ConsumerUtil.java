package ru.kontur.vostok.hercules.sink;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.util.PatternMatcher;

import java.util.List;

/**
 * @author Gregory Koshelev
 */
public final class ConsumerUtil {
    private static final String GROUP_ID_TEMPLATE = "hercules.%s.%s";

    @NotNull
    public static String toGroupId(String application, List<PatternMatcher> patternMatchers) {
        return String.format(GROUP_ID_TEMPLATE, application, PatternMatcher.toString(patternMatchers))
                .replaceAll("\\s+", "-");
    }

    private ConsumerUtil() {
        /* static class */
    }
}
