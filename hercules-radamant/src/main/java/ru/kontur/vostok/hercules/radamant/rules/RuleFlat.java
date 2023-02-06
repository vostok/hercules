package ru.kontur.vostok.hercules.radamant.rules;

import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;

/**
 * Rule for aggregation of flat metrics.
 *
 * @author Tatyana Tokmyanina
 */
public class RuleFlat implements Rule {
    private String name;
    private String description;
    private ThreadLocal<Matcher> patternMatcher;
    private Map<TinyString, Variant> enrichment;

    public String getName() {
        return name;
    }

    @JsonSetter("rule_name")
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Matcher getPatternMatcher() {
        return patternMatcher.get();
    }

    @JsonSetter("pattern")
    public void setPatternMatcher(String regex) {
        Pattern pattern = Pattern.compile(regex);
        this.patternMatcher = ThreadLocal.withInitial(() -> pattern.matcher(""));
    }

    @Override
    public Map<TinyString, Variant> getEnrichment() {
        return enrichment;
    }

    @JsonSetter
    public void setEnrichment(Map<String, String> enrichment) {
        this.enrichment = new HashMap<>();
        for (Entry<String, String> entry : enrichment.entrySet()) {
            this.enrichment.put(TinyString.of(entry.getKey()), Variant.ofString(entry.getValue()));
        }
    }
}
