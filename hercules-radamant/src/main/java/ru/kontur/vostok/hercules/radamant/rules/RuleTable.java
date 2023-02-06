package ru.kontur.vostok.hercules.radamant.rules;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.radamant.configurator.RuleConfiguratorWatcher;

/**
 * Interface for table of rules.
 *
 * @author Tatyana Tokmyanina
 */
public interface RuleTable extends RuleConfiguratorWatcher {
    /**
     * Looking through all rules.
     * If {@link Event} matches the rule, create {@link RuleMatch}, that contains necessary info for
     * aggregation.
     *
     * @param event event.
     * @return {@link RuleMatches}, that contains original event and array of {@link RuleMatch}.
     */
    RuleMatches navigate(Event event);
}
