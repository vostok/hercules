package ru.kontur.vostok.hercules.radamant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.radamant.rules.RuleFlat;
import ru.kontur.vostok.hercules.radamant.rules.RuleMatch;
import ru.kontur.vostok.hercules.radamant.rules.RuleMatches;
import ru.kontur.vostok.hercules.radamant.rules.RuleTableFlat;
import ru.kontur.vostok.hercules.tags.MetricsTags;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

/**
 * @author Tatyana Tokmyanina
 */
public class RuleTableFlatTest {
    private RuleTableFlat ruleTableFlat;

    String[] expectedGroups;
    Map<String, String> enrichment = new HashMap<>();

    String pattern = "([A-Za-z0-9_-]+)\\.([A-Za-z0-9_-]+)\\.(Count-Requests|Rate-5-min-Requests-per-s)";

    @Before
    public void init() {
        MetricsCollector metricsCollector = Mockito.mock(MetricsCollector.class);
        Mockito.when(metricsCollector.meter(Mockito.anyString()))
                .thenReturn(Mockito.mock(Meter.class));
        ruleTableFlat = new RuleTableFlat(metricsCollector);
        enrichment.put("function", "sum");
        expectedGroups = new String[]{"firstGroup", "secondGroup", "Count-Requests"};
    }

    @Test
    public void shouldUpdateRules() {
        assertNull(ruleTableFlat.getRules());
        RuleFlat[] rules = new RuleFlat[1];
        RuleFlat ruleFlat = new RuleFlat();
        ruleFlat.setDescription("test rule for flat metrics");
        rules[0] = ruleFlat;
        ruleTableFlat.updateRules(rules);
        assertNotNull(ruleTableFlat.getRules());
        assertEquals(1, ruleTableFlat.getRules().length);
        RuleFlat actualRule = ruleTableFlat.getRules()[0];
        assertEquals("test rule for flat metrics", actualRule.getDescription());
    }

    @Test
    public void shouldReturnRuleMatchIfMatch() {
        RuleFlat rule = new RuleFlat();
        rule.setPatternMatcher(pattern);
        rule.setEnrichment(enrichment);
        rule.setName("rule_name");
        ruleTableFlat.updateRules(new RuleFlat[]{rule});
        Event flatMetricEvent = EventBuilder.create(
                        TimeUtil.millisToTicks(Instant.now().toEpochMilli()), UUID.randomUUID())
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("_name"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(),
                                        Variant.ofString("firstGroup.secondGroup.Count-Requests"))
                                .build()
                )))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(42))
                .build();
        RuleMatches ruleMatches = ruleTableFlat.navigate(flatMetricEvent);

        assertNotNull(ruleMatches);
        assertEquals(1, ruleMatches.getRuleMatches().size());
        RuleMatch actualRuleMatch = ruleMatches.getRuleMatches().get(0);

        assertNotNull(actualRuleMatch.getGroups());
        String[] actualGroups = actualRuleMatch.getGroups();
        assertEquals(3, actualGroups.length);
        for (int i = 0; i < 3; i++) {
            assertEquals(expectedGroups[i], actualGroups[i]);
        }

        assertNotNull(actualRuleMatch.getEnrichment());
        Map<TinyString, Variant> actualEnrichment = actualRuleMatch.getEnrichment();
        String function = new String((byte[]) actualEnrichment.get(TinyString.of("function")).getValue());
        assertEquals(1, actualEnrichment.size());
        assertEquals(enrichment.get("function"), function);
    }
}
