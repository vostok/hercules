package ru.kontur.vostok.hercules.radamant.configurator;

import ru.kontur.vostok.hercules.radamant.rules.Rule;

/**
 * Interface that observe changes of rules.
 *
 * @author Tatyana Tokmyanina
 */
public interface RuleConfiguratorWatcher {
    /**
     * Change table of rules.
     *
     * @param rules - array of {@link Rule}.
     */
    void updateRules(Rule[] rules);
}
