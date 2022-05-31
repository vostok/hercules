package ru.kontur.vostok.hercules.graphite.sink.common;

import java.util.function.Predicate;

/**
 * Interface of predicate for preprocessing a pattern segment.
 *
 * @author Aleksandr Yuferov
 */
public interface PatternMatcher extends Predicate<String> {
    String STAR = "*";

    /**
     * Static factory method.
     * <p>
     * Will produce one of implementation for next types of patterns.
     * <ul>
     * <li>{@link AnyMatcher} will be returned if value equals to {@code *}.
     * <li>{@link PrefixMatcher} will be returned if value like {@code foo*} pattern.
     * <li>{@link SuffixMatcher} will be returned if value like {@code *foo} pattern.
     * <li>{@link PrefixAndSuffixMatcher} will be returned if value like {@code foo*bar} pattern.
     * <li>{@link ContainsMatcher} will be returned if value like {@code *foo*} pattern.
     * <li>{@link EqualsMatcher} will be returned if value doesn't have patterns but concrete value (also fallback
     * matcher).
     * </ul>
     *
     * @param value Mask of tag value.
     * @return Concrete implementation.
     */
    static PatternMatcher of(String value) {
        if (value.equals(STAR)) {
            return AnyMatcher.INSTANCE;
        } else {
            int starPosition = value.indexOf(STAR);
            int starPositionLast = value.lastIndexOf(STAR);
            if (starPosition == 0 && starPositionLast == value.length() - 1) {
                return new ContainsMatcher(value.substring(1, value.length() - 1));
            } else if (starPosition == starPositionLast) {
                if (starPosition == 0) {
                    return new SuffixMatcher(value.substring(starPosition + 1));
                } else if (starPosition == value.length() - 1) {
                    return new PrefixMatcher(value.substring(0, starPosition));
                } else if (starPosition != -1) {
                    String prefix = value.substring(0, starPosition);
                    String suffix = value.substring(starPosition + 1);
                    return new PrefixAndSuffixMatcher(prefix, suffix);
                }
            }
        }
        return new EqualsMatcher(value);
    }

    /**
     * Predicate.
     *
     * @param value Value of the tag.
     * @return Is given value matches the mask.
     */
    boolean test(String value);

    /**
     * Is any value can be accepted.
     *
     * @return Returns {@code true} if this implementation will return always true to any {@link #test} method argument.
     */
    default boolean isAny() {
        return false;
    }

    /**
     * Pattern matcher implementation that checks if given value have specified suffix.
     */
    class SuffixMatcher implements PatternMatcher {
        private final String suffix;

        private SuffixMatcher(String suffix) {
            this.suffix = suffix;
        }

        @Override
        public boolean test(String value) {
            return value.endsWith(suffix);
        }
    }

    /**
     * Pattern matcher implementation that checks if given value have specified prefix.
     */
    class PrefixMatcher implements PatternMatcher {
        private final String prefix;

        private PrefixMatcher(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public boolean test(String value) {
            return value.startsWith(prefix);
        }
    }

    /**
     * Pattern matcher implementation that checks if given value have specified prefix and suffix.
     */
    class PrefixAndSuffixMatcher implements PatternMatcher {
        private final int minLength;
        private final String prefix;
        private final String suffix;

        private PrefixAndSuffixMatcher(String prefix, String suffix) {
            this.minLength = prefix.length() + suffix.length();
            this.prefix = prefix;
            this.suffix = suffix;
        }

        @Override
        public boolean test(String value) {
            return value.length() >= minLength && value.startsWith(prefix) && value.endsWith(suffix);
        }
    }

    /**
     * Pattern matcher implementation that checks if given value is exactly equals to specified value.
     */
    class EqualsMatcher implements PatternMatcher {
        private final String expected;

        private EqualsMatcher(String expected) {
            this.expected = expected;
        }

        @Override
        public boolean test(String value) {
            return value.equals(expected);
        }
    }

    /**
     * Pattern matcher implementation that checks if given value contains specified value.
     */
    class ContainsMatcher implements PatternMatcher {
        private final String substring;

        private ContainsMatcher(String value) {
            this.substring = value;
        }

        @Override
        public boolean test(String value) {
            return value.contains(substring);
        }
    }

    /**
     * Pattern matcher implementation that will return true to any value.
     */
    class AnyMatcher implements PatternMatcher {
        private static final AnyMatcher INSTANCE = new AnyMatcher();

        private AnyMatcher() {
        }

        @Override
        public boolean test(String value) {
            return true;
        }

        @Override
        public boolean isAny() {
            return true;
        }
    }
}
