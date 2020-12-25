package ru.kontur.vostok.hercules.sink;

import ru.kontur.vostok.hercules.util.PatternMatcher;
import ru.kontur.vostok.hercules.util.collection.CollectionUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Subscription determines {@link Pattern} for subscribing in a Kafka consumer.
 *
 * @author Gregory Koshelev
 * @see PatternMatcher
 */
public class Subscription {
    private final List<PatternMatcher> patternMatchers;
    private final List<PatternMatcher> exclusions;
    private final Pattern pattern;

    public Subscription(List<PatternMatcher> patternMatchers) {
        this(patternMatchers, Collections.emptyList());
    }

    public Subscription(List<PatternMatcher> patternMatchers, List<PatternMatcher> exclusions) {
        this.patternMatchers = patternMatchers;
        this.exclusions = exclusions;

        this.pattern = CollectionUtil.isNullOrEmpty(exclusions)
                ? PatternMatcher.matcherListToRegexp(patternMatchers)
                : Pattern.compile("(?!(" + PatternMatcher.matchersListToRegexpString(exclusions) + "))(" + PatternMatcher.matchersListToRegexpString(patternMatchers) + ")");
    }

    public Pattern toPattern() {
        return pattern;
    }

    public String toGroupId(String application) {
        return ConsumerUtil.toGroupId(application, patternMatchers);
    }

    @Override
    public String toString() {
        return pattern.pattern();
    }

    /**
     * Builder for {@link Subscription}.
     *
     * @return builder
     */
    public static SubscriptionBuilder builder() {
        return new SubscriptionBuilder();
    }

    public static final class SubscriptionBuilder {
        private final List<PatternMatcher> patternMatchers = new ArrayList<>();
        private final List<PatternMatcher> exclusions = new ArrayList<>();

        public SubscriptionBuilder include(String[] patterns) {
            for (String pattern : patterns) {
                patternMatchers.add(new PatternMatcher(pattern));
            }
            return this;
        }

        public SubscriptionBuilder exclude(String[] patterns) {
            for (String pattern : patterns) {
                exclusions.add(new PatternMatcher(pattern));
            }
            return this;
        }

        public Subscription build() {
            if (exclusions.isEmpty()) {
                return new Subscription(new ArrayList<>(patternMatchers));
            }
            return new Subscription(new ArrayList<>(patternMatchers), new ArrayList<>(exclusions));
        }
    }
}
