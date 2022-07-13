package ru.kontur.vostok.hercules.routing.config;

import ru.kontur.vostok.hercules.util.lifecycle.Stoppable;

/**
 * Interface of configuration watch task.
 * <p>
 * Objects that implement this interface observes configuration in some storage and reports about changes to
 * {@link ConfigurationObserver} object that should pass into {@link #start} method.
 *
 * @author Aleksandr Yuferov
 */
public interface ConfigurationWatchTask extends Stoppable {
    /**
     * Start the task.
     *
     * @param observer Observer, that will receive configuration changes.
     */
    void start(ConfigurationObserver observer);
}
