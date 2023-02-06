package ru.kontur.vostok.hercules.radamant.rules;

import java.util.Map;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;

/**
 * Contains information for enriching metrics event:
 * <p>
 * groups - groups captured from metrics name by parsing regular expression,
 * <p>
 * enrichment - additional info for aggregating.
 *
 * @author Tatyana Tokmyanina
 */
public class RuleMatch {
    private final String ruleName;
    private final String[] groups;
    private final Map<TinyString, Variant> enrichment;

    public RuleMatch(String ruleName, String[] groups, Map<TinyString, Variant> enrichment) {
        this.ruleName = ruleName;
        this.groups = groups;
        this.enrichment = enrichment;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String[] getGroups() {
        return groups;
    }

    public Map<TinyString, Variant> getEnrichment() {
        return enrichment;
    }
}
