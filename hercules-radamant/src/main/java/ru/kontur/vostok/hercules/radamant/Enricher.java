package ru.kontur.vostok.hercules.radamant;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.radamant.rules.RuleMatch;
import ru.kontur.vostok.hercules.tags.MetricsTags;

/**
 * Enrich events with additional information.
 *
 * @author Tatyana Tokmyanina
 */
public class Enricher {
    private static final Logger LOGGER = LoggerFactory.getLogger(Enricher.class);

    private final Meter incorrectRule;

    public Enricher(MetricsCollector metricsCollector) {
        String metricPath = MetricsUtil.toMetricPath("incorrectRule");
        this.incorrectRule = metricsCollector.meter(metricPath);
    }

    /**
     * Enrich {@link Event} with additional info.
     *
     * @param event     original {@link Event}.
     * @param ruleMatch {@link RuleMatch} that contains additional info
     * @return enriched {@link Event}.
     */
    public Event enrich(Event event, RuleMatch ruleMatch) {
        Map<TinyString, Variant> payload = event.getPayload().tags();
        Map<TinyString, Variant> enrichment = ruleMatch.getEnrichment();
        if (enrichment.containsKey(MetricsTags.NAME_PATTERN_TAG.getName())) {
            String pattern = new String(
                    (byte[]) enrichment.get(MetricsTags.NAME_PATTERN_TAG.getName()).getValue());
            try {
                String newMetricName = formatName(pattern, ruleMatch.getGroups());
                enrichment.put(MetricsTags.NEW_NAME_TAG.getName(), Variant.ofString(newMetricName));
            } catch (IllegalArgumentException ex) {
                LOGGER.error("Illegal pattern for metric name, rule: " + ruleMatch.getRuleName());
                return null;
            }
        }
        payload.put(MetricsTags.ENRICHMENT_TAG.getName(), Variant.ofContainer(Container.of(enrichment)));
        EventBuilder result = EventBuilder.create(event.getTimestamp(), event.getUuid());
        for (Entry<TinyString, Variant> entry : payload.entrySet()) {
            result.tag(entry.getKey(), entry.getValue());
        }
        return result.build();
    }

    private String formatName(String pattern, String[] groups) throws IllegalArgumentException {
        String result = MessageFormat.format(pattern, (Object[]) groups);
        if (result.contains("{")) {
            incorrectRule.mark();
            throw new IllegalArgumentException();
        }
        return result;
    }
}
