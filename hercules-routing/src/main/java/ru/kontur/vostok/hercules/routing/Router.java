package ru.kontur.vostok.hercules.routing;

import ru.kontur.vostok.hercules.routing.config.ConfigurationWatchTask;
import ru.kontur.vostok.hercules.routing.engine.RouterEngine;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;
import ru.kontur.vostok.hercules.util.routing.Destination;

import java.util.concurrent.TimeUnit;

/**
 * Routing system facade.
 *
 * @param <Q> Type of queries objects.
 * @param <D> Type of destination objects.
 * @author Aleksandr Yuferov
 */
public class Router<Q, D extends Destination<D>> implements Lifecycle {
    private final ConfigurationWatchTask configWatchTask;
    private final RouterEngine<Q> engine;

    /**
     * Constructor.
     *
     * @param configWatchTask Configuration watch task.
     * @param engine          Routing engine.
     */
    public Router(ConfigurationWatchTask configWatchTask, RouterEngine<Q> engine) {
        this.configWatchTask = configWatchTask;
        this.engine = engine;
    }

    /**
     * Perform roting.
     *
     * @param query Some query.
     * @return Result route for query.
     */
    public D route(Q query) {
        @SuppressWarnings("unchecked")
        D destination = (D) engine.route(query);
        return destination;
    }

    /**
     * Starts the configuration watch task.
     */
    @Override
    public void start() {
        configWatchTask.start(engine);
    }

    /**
     * Stop configuration watch task.
     *
     * @param timeout  Timeout.
     * @param timeUnit Units of timeout parameter.
     * @return {@code true} if no errors occurred while stopping the task.
     */
    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        return configWatchTask.stop(timeout, timeUnit);
    }
}
