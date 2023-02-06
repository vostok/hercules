package ru.kontur.vostok.hercules.radamant.configurator;

/**
 * Interface for configuration tables of rules.
 *
 * @author Tatyana Tokmyanina
 */
public interface RuleConfigurator {
    /**
     * Reports about changes to {@link RuleConfiguratorWatcher}.
     *
     * @param watcher Object that implement {@link RuleConfiguratorWatcher} interface.
     */
    void init(RuleConfiguratorWatcher watcher);
}
