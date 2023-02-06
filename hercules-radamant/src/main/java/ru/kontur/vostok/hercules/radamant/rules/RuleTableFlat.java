package ru.kontur.vostok.hercules.radamant.rules;

import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.tags.MetricsTags;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Contains rules for flat metrics aggregation.
 *
 * @author Tatyana Tokmyanina
 */
public class RuleTableFlat implements RuleTable {
    private RuleFlat[] rules;
    private final MetricsCollector metricsCollector;

    public RuleTableFlat(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    public RuleFlat[] getRules() {
        return rules;
    }

    @Override
    public void updateRules(Rule[] rules) {
        this.rules = new RuleFlat[rules.length];
        for (int i = 0; i < rules.length; i++) {
            this.rules[i] = (RuleFlat) rules[i];
        }
    }

    @Override
    public RuleMatches navigate(Event event) {
        List<RuleMatch> ruleMatches = new ArrayList<>();
        for (RuleFlat rule : rules) {
            String[] groups = getGroups(rule.getPatternMatcher(), event);
            if (groups != null && groups.length != 0) {
                ruleMatches.add(new RuleMatch(rule.getName(), groups, rule.getEnrichment()));
                markEvent(rule.getName());
            }
        }
        return new RuleMatches(event, ruleMatches);
    }

    private String[] getGroups(Matcher matcher, Event event) {
        Variant tags = event.getPayload().tags().get(MetricsTags.TAGS_VECTOR_TAG.getName());
        if (tags.getType() != Type.VECTOR) {
            return null;
        }
        Vector tagsVector = (Vector) tags.getValue();
        if (tagsVector.getType() != Type.CONTAINER) {
            return null;
        }
        Container container = ((Container[]) tagsVector.getValue())[0];
        Variant nameVariant = container.tags().get(MetricsTags.TAG_VALUE_TAG.getName());
        if (nameVariant.getType() != Type.STRING) {
            return null;
        }
        String name = new String((byte[]) nameVariant.getValue(), StandardCharsets.UTF_8);
        matcher.reset(name);
        if (!matcher.matches()) {
            return null;
        }
        String[] result = new String[matcher.groupCount()];
        for (int i = 0; i < result.length; i++) {
            result[i] = matcher.group(i + 1);
        }
        return result;
    }

    private void markEvent(String ruleName) {
        String metricPath = MetricsUtil.toMetricPath("eventsByRule", ruleName);
        metricsCollector.meter(metricPath).mark();
    }
}
