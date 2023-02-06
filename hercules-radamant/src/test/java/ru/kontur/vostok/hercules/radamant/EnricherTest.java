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
import ru.kontur.vostok.hercules.radamant.rules.RuleMatch;
import ru.kontur.vostok.hercules.tags.MetricsTags;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

/**
 * @author Tatyana Tokmyanina
 */
public class EnricherTest {
    private static final long EVENT_TIME = TimeUtil.millisToTicks(Instant.now().toEpochMilli());
    private static final UUID EVENT_ID = UUID.randomUUID();
    private Event flatMetricEvent;
    private MetricsCollector metricsCollector;

    @Before
    public void init() {
        metricsCollector = Mockito.mock(MetricsCollector.class);
        Mockito.when(metricsCollector.meter(Mockito.anyString()))
                .thenReturn(Mockito.mock(Meter.class));
        flatMetricEvent = EventBuilder.create(EVENT_TIME, EVENT_ID)
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("_name"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(),
                                        Variant.ofString("foo.bar.baz"))
                                .build()
                )))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(42))
                .build();
    }

    @Test
    public void shouldAddEnrichmentToFlatMetricEvent() {
        Map<TinyString, Variant> enrichment = Map.of(
                TinyString.of("function"), Variant.ofString("sum"),
                TinyString.of("period"), Variant.ofString("00:60:00")
        );
        RuleMatch ruleMatch = new RuleMatch(null, null, enrichment);

        Event resultEvent = new Enricher(metricsCollector).enrich(flatMetricEvent, ruleMatch);
        assertEquals(EVENT_TIME, resultEvent.getTimestamp());
        assertEquals(EVENT_ID, resultEvent.getUuid());
        Container enrichmentContainer = (Container) resultEvent
                .getPayload()
                .tags()
                .get(MetricsTags.ENRICHMENT_TAG.getName())
                .getValue();
        assertNotNull(enrichmentContainer);
        assertEquals(2, enrichmentContainer.tags().size());
        assertEquals("sum", new String((byte[]) enrichmentContainer.get(TinyString.of("function")).getValue()));
    }

    @Test
    public void shouldAddNewNameToEnrichment() {
        Map<TinyString, Variant> enrichment = new HashMap<>();
        enrichment.put(
                MetricsTags.NAME_PATTERN_TAG.getName(),
                Variant.ofString("asdf.asdf.{1}.asdf.asdf.{2}")
        );
        String[] groups = new String[]{"zero", "first", "second"};
        RuleMatch ruleMatch = new RuleMatch("rule", groups, enrichment);

        Event resultEvent = new Enricher(metricsCollector).enrich(flatMetricEvent, ruleMatch);
        assertEquals(EVENT_TIME, resultEvent.getTimestamp());
        assertEquals(EVENT_ID, resultEvent.getUuid());
        Container enrichmentContainer = (Container) resultEvent.getPayload().tags()
                .get(MetricsTags.ENRICHMENT_TAG.getName()).getValue();
        assertNotNull(enrichmentContainer);
        assertEquals(2, enrichmentContainer.tags().size());
        assertEquals("asdf.asdf.first.asdf.asdf.second",
                new String((byte[]) enrichmentContainer.get(TinyString.of("new_name")).getValue()));
    }

    @Test
    public void shouldReturnNullIfCannotCreateMetricName() {
        Map<TinyString, Variant> enrichment = Map.of(
                MetricsTags.NAME_PATTERN_TAG.getName(),
                Variant.ofString("asdf.asdf.{1}.asdf.asdf.{2}"));
        String[] groups = new String[0];
        RuleMatch ruleMatch = new RuleMatch("rule", groups, enrichment);

        Event resultEvent = new Enricher(metricsCollector).enrich(flatMetricEvent, ruleMatch);
        assertNull(resultEvent);
    }
}
