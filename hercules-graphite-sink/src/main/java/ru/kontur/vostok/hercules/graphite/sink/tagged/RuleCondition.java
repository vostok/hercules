package ru.kontur.vostok.hercules.graphite.sink.tagged;

import ru.kontur.vostok.hercules.graphite.sink.common.PatternMatcher;

/**
 * Condition (part of the Rule).
 * <p>
 * Objects of this class represents parts of rules listed in the tagged filter file. Condition can be part of many
 * rules, to determine to which rule condition belongs use ruleIndices array.
 *
 * @author Aleksandr Yuferov
 */
public class RuleCondition {
    private final int[] ruleIndices;
    private final String tagKey;
    private final PatternMatcher matcher;

    /**
     * Factory method.
     *
     * @param tag         Tag representation.
     * @param ruleIndices Indices of rules, that will contain created condition.
     * @return New instance of the condition.
     */
    public static RuleCondition of(Tag tag, int[] ruleIndices) {
        return new RuleCondition(tag.key(), ruleIndices, PatternMatcher.of(tag.value()));
    }

    protected RuleCondition(String tagKey, int[] ruleIndices, PatternMatcher matcher) {
        this.ruleIndices = ruleIndices;
        this.tagKey = tagKey;
        this.matcher = matcher;
    }

    public boolean test(String tagValue) {
        return matcher.test(tagValue);
    }

    /**
     * Indices of the rules that contains this condition.
     *
     * @return Indices of the rules that contains this condition.
     */
    public int[] ruleIndices() {
        return ruleIndices;
    }

    /**
     * Name of the tag with which this condition associated.
     *
     * @return Name of the tag with which this condition associated.
     */
    public String tagKey() {
        return tagKey;
    }
}
