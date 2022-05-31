package ru.kontur.vostok.hercules.graphite.sink.filter;

import ru.kontur.vostok.hercules.configuration.Sources;
import ru.kontur.vostok.hercules.graphite.sink.tagged.RuleCondition;
import ru.kontur.vostok.hercules.graphite.sink.tagged.TaggedFilterFileParser;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.sink.filter.EventFilter;
import ru.kontur.vostok.hercules.tags.MetricsTags;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Filter for tagged metrics.
 * <p>
 * Filter parse file specified in the {@code list.path} using {@link TaggedFilterFileParser}. For each rule from this
 * file an index assigned. An index ranges from {@code 0} to {@code rules count - 1}.
 * <p>
 * Each rule has conditions represented in code as objects of class {@link RuleCondition}. In this class array
 * {@link #wholeConditionCountsByRules} stores count of conditions in each rule (indices of this array are associated
 * to the rule indices). In each invocation {@link #test} method counts how many of conditions' predicates of each rule
 * return {@code true} for the tested {@link Event}. Counters are stored in {@link #matchingConditionCountsByRules}
 * (indices of this array are associated to the rule indices). If some counter from
 * {@link #matchingConditionCountsByRules} reached associated value from {@link #wholeConditionCountsByRules}, then
 * this rule recognized as "matched". For more information about algorithm see {@link #test} docs.
 *
 * @author Aleksandr Yuferov
 */
public class TaggedMetricFilter extends EventFilter {
    private final boolean matchResult;
    private final int[] wholeConditionCountsByRules;
    private final int[] matchingConditionCountsByRules;
    /**
     * Conditions grouped by tags name.
     * <p>
     * There are can be many condition from different rules aimed at one field with different masks, i.e. <pre>{@code
     * app=dagon
     * app=k84
     * app=some-other-app
     * }</pre>
     * That is why multiple {@link RuleCondition} can be found with the same value of {@link RuleCondition#tagKey()}
     * property.
     * <p>
     * Also, there can be many conditions from different rules aimed at one field with exactly same mask, i.e.
     * <pre>{@code
     * app=dagon;env=test
     * app=dagon;env=stage
     * }</pre>
     * These conditions become merged into one {@link RuleCondition} with all related rules indices listed in
     * {@link RuleCondition#ruleIndices()} property.
     */
    private final Map<String, List<RuleCondition>> conditionsByTagName;

    /**
     * Constructor.
     * <p>
     * Fills the filter with rules from a file specified in the {@code list.path} property using
     * {@link TaggedFilterFileParser}. Set the filter return value using the {@code list.matchResult}
     * property.
     *
     * @param properties Properties.
     */
    public TaggedMetricFilter(Properties properties) {
        super(properties);
        matchResult = PropertiesUtil.get(Props.LIST_MATCH_RESULT, properties).get();

        String listPath = PropertiesUtil.get(Props.LIST_PATH, properties).get();
        var parser = new TaggedFilterFileParser();
        TaggedFilterFileParser.ParseResult parseResult = Sources.load(listPath, parser::parse);

        wholeConditionCountsByRules = parseResult.getRuleConditionCount();
        conditionsByTagName = parseResult.getConditions().stream()
                .collect(Collectors.groupingBy(RuleCondition::tagKey));
        matchingConditionCountsByRules = new int[parseResult.getRulesCount()];
    }

    /**
     * Method checks the given event.
     * <p>
     * Using prepared conditions predicates (stored in {@link #conditionsByTagName} map) method counts how many
     * condition predicates of each rule returns {@code true}. All counters are listed in
     * {@link #matchingConditionCountsByRules}. When any rule counter reached the count of this rule condition listed
     * in filter config (stored in {@link #wholeConditionCountsByRules}), then event recognized as 'matched' to this
     * rule and method will return value of {@link #matchResult} property.
     * <p>
     * To skip rules that already will not be triggered, when condition predicate returns {@code false} value,
     * method marks counters as "unactual" setting to the counter {@link Integer#MIN_VALUE}.
     * <p>
     * If event not reached recognition as 'matched' to some rule, then method will return invert value of
     * {@link #matchResult} property.
     *
     * @param event The event to check.
     * @return Will return {@code true} if event should be passed, or {@code false} if it should be denied.
     */
    @Override
    public boolean test(Event event) {
        if (wholeConditionCountsByRules.length == 0) {
            return !matchResult;
        }
        Container[] tags = ContainerUtil
                .extract(event.getPayload(), MetricsTags.TAGS_VECTOR_TAG)
                .orElse(null);
        if (tags == null) {
            return !matchResult;
        }
        Arrays.fill(matchingConditionCountsByRules, 0);
        for (Container tag : tags) {
            // TODO: ContainerUtil.extract is expensive!
            //  (each time detecting extractor and coping values in StandardExtractors.extractString)
            String tagKey = ContainerUtil.extract(tag, MetricsTags.TAG_KEY_TAG).orElse("null");
            List<RuleCondition> conditions = conditionsByTagName.get(tagKey);
            if (conditions == null) {
                continue;
            }
            String tagValue = ContainerUtil.extract(tag, MetricsTags.TAG_VALUE_TAG).orElse("null");
            for (RuleCondition condition : conditions) {
                int[] ruleIndices = condition.ruleIndices();
                if (!anyRuleHasActualCounter(ruleIndices)) {
                    continue;
                }
                if (!condition.test(tagValue)) {
                    markRulesAsUnactual(ruleIndices);
                    continue;
                }
                increaseRulesCounters(ruleIndices);
                if (anyRuleHasReachedMaximum(ruleIndices)) {
                    return matchResult;
                }
            }
        }
        return !matchResult;
    }

    /**
     * Method checks that any of given rules has actual counter in {@link #matchingConditionCountsByRules}.
     */
    private boolean anyRuleHasActualCounter(int[] ruleIndices) {
        return Arrays.stream(ruleIndices)
                .anyMatch(rule -> matchingConditionCountsByRules[rule] >= 0);
    }

    /**
     * Method marks counters of all given rules as unactual in {@link #matchingConditionCountsByRules}.
     */
    private void markRulesAsUnactual(int[] ruleIndices) {
        for (int rule : ruleIndices) {
            matchingConditionCountsByRules[rule] = Integer.MIN_VALUE;
        }
    }

    /**
     * Method increasing counters of each given rule in {@link #matchingConditionCountsByRules}.
     */
    private void increaseRulesCounters(int[] ruleIndices) {
        for (int rule : ruleIndices) {
            matchingConditionCountsByRules[rule] += 1;
        }
    }

    /**
     * Method checks that any counter in {@link #matchingConditionCountsByRules} of given rules has reached its maximum
     * value listed in {@link #wholeConditionCountsByRules}.
     */
    private boolean anyRuleHasReachedMaximum(int[] ruleIndices) {
        return Arrays.stream(ruleIndices)
                .anyMatch(rule -> matchingConditionCountsByRules[rule] == wholeConditionCountsByRules[rule]);
    }

    static class Props {
        /**
         * Path to the file with list of rules.
         */
        static final Parameter<String> LIST_PATH = Parameter
                .stringParameter("list.path")
                .withDefault("file://filter-list.tfl")
                .build();

        /**
         * Type of the filter list.
         */
        static final Parameter<Boolean> LIST_MATCH_RESULT = Parameter
                .booleanParameter("list.matchResult")
                .withDefault(false)
                .build();
    }
}
