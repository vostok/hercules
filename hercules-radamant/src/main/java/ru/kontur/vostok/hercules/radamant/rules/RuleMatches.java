package ru.kontur.vostok.hercules.radamant.rules;

import java.util.List;
import ru.kontur.vostok.hercules.protocol.Event;

/**
 * Contains {@link Event} and information about all rules that {@link Event} matches.
 *
 * @author Tatyana Tokmyanina
 */
public final class RuleMatches {

    private final Event event;
    private final List<RuleMatch> ruleMatches;

    public RuleMatches(Event event, List<RuleMatch> ruleMatches) {
        this.event = event;
        this.ruleMatches = ruleMatches;
    }

    public Event getEvent() {
        return event;
    }

    public List<RuleMatch> getRuleMatches() {
        return ruleMatches;
    }
}
